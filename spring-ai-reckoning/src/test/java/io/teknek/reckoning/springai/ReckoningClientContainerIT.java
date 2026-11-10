package io.teknek.reckoning.springai;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class ReckoningClientContainerIT {

    @Test
    void clientModeCanCallReleasedReckoningContainer() {
        Assumptions.assumeTrue(Boolean.getBoolean("reckoning.springai.it.container"));
        String image = System.getProperty("reckoning.springai.testcontainer.image", "ecapriolo/reckoning:latest");

        try (GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(image)).withExposedPorts(8080)) {
            container.start();
            String baseUrl = "http://" + container.getHost() + ":" + container.getMappedPort(8080);
            ReckoningChatModel model = new ReckoningChatModel(
                    ReckoningApi.create(baseUrl, null, null),
                    new ObjectMapper(),
                    ReckoningChatOptions.builder()
                            .model(System.getProperty("reckoning.springai.testcontainer.model", "test-model"))
                            .temperature(0.0)
                            .maxTokens(16)
                            .build());

            String response = model.call("Say hello in one short sentence.");
            assertTrue(response != null && !response.isBlank());
        }
    }

    @Test
    void clientModeCanCallLocalReckoningImage() {
        Assumptions.assumeTrue(Boolean.getBoolean("reckoning.springai.it.local-image"));

        ImageFromDockerfile image = new ImageFromDockerfile("reckoning:spring-ai-it", false)
                .withDockerfile(Path.of("docker", "Dockerfile"));
        try (GenericContainer<?> container = new GenericContainer<>(image).withExposedPorts(8080)) {
            container.start();
            assertTrue(container.isRunning());
        }
    }
}
