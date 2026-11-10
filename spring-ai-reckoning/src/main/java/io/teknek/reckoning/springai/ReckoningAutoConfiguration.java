package io.teknek.reckoning.springai;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@EnableConfigurationProperties(ReckoningConnectionProperties.class)
public class ReckoningAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.ai.reckoning", name = "mode", havingValue = "client", matchIfMissing = true)
    public ReckoningApi reckoningApi(ReckoningConnectionProperties properties,
            ObjectProvider<WebClient.Builder> webClientBuilderProvider) {
        return ReckoningApi.builder()
                .baseUrl(properties.getBaseUrl())
                .username(properties.getUsername())
                .password(properties.getPassword())
                .webClientBuilder(webClientBuilderProvider.getIfAvailable(WebClient::builder))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    @ConditionalOnProperty(prefix = "spring.ai.reckoning", name = "mode", havingValue = "client", matchIfMissing = true)
    public ReckoningChatModel reckoningChatModel(ReckoningApi reckoningApi,
            ReckoningConnectionProperties properties, ObjectProvider<ToolCallingManager> toolCallingManager,
            ObjectProvider<RetryTemplate> retryTemplate,
            ObjectProvider<ObservationRegistry> observationRegistry,
            ObjectProvider<ChatModelObservationConvention> observationConvention) {
        ReckoningChatOptions options = ReckoningChatOptions.builder()
                .model(properties.getModel())
                .build();
        ReckoningChatModel chatModel = ReckoningChatModel.builder()
                .reckoningApi(reckoningApi)
                .objectMapper(new ObjectMapper())
                .options(options)
                .toolCallingManager(toolCallingManager.getIfAvailable(() -> ToolCallingManager.builder().build()))
                .retryTemplate(retryTemplate.getIfUnique(() -> RetryUtils.DEFAULT_RETRY_TEMPLATE))
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .build();
        observationConvention.ifAvailable(chatModel::setObservationConvention);
        return chatModel;
    }
}
