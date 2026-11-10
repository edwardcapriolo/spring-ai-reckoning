package io.teknek.reckoning.springai;

import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import java.time.Duration;

import io.teknek.reckoning.client.spring.api.ModelsApi;
import io.teknek.reckoning.client.spring.core.ApiClient;
import io.teknek.reckoning.client.spring.model.FetchRequest;
import io.teknek.reckoning.client.spring.model.ServeRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Docker-backed smoke test for model fetch, serve, and chat generation.
 *
 * <p>Enable explicitly with {@code RECKONING_RUN_DOCKER_IT=true}. The host
 * {@code ~/.reckoning} directory is mounted so model downloads survive test runs.</p>
 */
@Testcontainers
@EnabledIfEnvironmentVariable(named = "RECKONING_RUN_DOCKER_IT", matches = "true")
class ReckoningDockerChatIT {

    private static final String OWNER = System.getenv().getOrDefault("RECKONING_TEST_MODEL_OWNER", "edwardcapriolo");

    private static final String MODEL = System.getenv().getOrDefault("RECKONING_TEST_MODEL", "Qwen3-0.6B-JQ4");

    private static GenericContainer<?> container;

    private static ReckoningChatModel chatModel;

    @BeforeAll
    static void startReckoning() throws Exception {
        Path primaryCache = Path.of(System.getProperty("user.home"), ".reckoning");
        Files.createDirectories(primaryCache);
        Path cache = Path.of(System.getenv().getOrDefault("RECKONING_MODEL_CACHE",
                Path.of(System.getProperty("user.home"), ".deliverance").toString()));
        if (!Files.isDirectory(cache)) {
            throw new IllegalStateException("Reckoning model cache does not exist: " + cache);
        }
        String image = System.getenv().getOrDefault("RECKONING_DOCKER_IMAGE", "ecapriolo/reckoning:0.0.17");
        int hostPort = Integer.parseInt(System.getenv().getOrDefault("RECKONING_DOCKER_PORT", "18080"));
        container = new GenericContainer<>(DockerImageName.parse(image))
                .withFileSystemBind(primaryCache.toString(), "/home/reckoning/.reckoning", BindMode.READ_WRITE)
                .withFileSystemBind(cache.toString(), "/home/reckoning/.deliverance", BindMode.READ_ONLY)
                .withCreateContainerCmdModifier(command -> command.getHostConfig().withPortBindings(
                        new PortBinding(Ports.Binding.bindPort(hostPort), new ExposedPort(8080))));
        container.start();

        String baseUrl = "http://localhost:" + hostPort;
        awaitReady(baseUrl);
        ModelsApi modelsApi = new ModelsApi(new ApiClient().setBasePath(baseUrl));
        modelsApi.fetchModel(new FetchRequest().owner(OWNER).name(MODEL)).block(Duration.ofMinutes(30));
        modelsApi.serveModel(new ServeRequest().owner(OWNER).name(MODEL)
                .type(ServeRequest.TypeEnum.GENERATION)).block(Duration.ofMinutes(5));
        chatModel = new ReckoningChatModel(ReckoningApi.create(baseUrl, null, null),
                new tools.jackson.databind.ObjectMapper(),
                ReckoningChatOptions.builder().model(MODEL).maxTokens(8).build());
    }

    @AfterAll
    static void stopReckoning() {
        if (container != null) {
            container.stop();
        }
    }

    @Test
    void fetchesServesAndGenerates() {
        ChatResponse response = chatModel.call(new Prompt("Reply with one short sentence about Java."));

        String text = response.getResult().getOutput().getText();
        assertTrue(text != null && !text.isBlank());
    }

    private static void awaitReady(String baseUrl) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/models"))
                .header("Content-Type", "application/json")
                .GET()
                .build();
        long deadline = System.nanoTime() + Duration.ofMinutes(3).toNanos();
        while (System.nanoTime() < deadline) {
            try {
                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() == 200) {
                    return;
                }
            }
            catch (Exception ignored) {
                // The server may still be starting.
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException("Reckoning did not become ready at " + baseUrl);
    }

}
