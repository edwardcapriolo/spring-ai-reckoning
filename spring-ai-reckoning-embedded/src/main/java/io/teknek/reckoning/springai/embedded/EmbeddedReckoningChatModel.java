package io.teknek.reckoning.springai.embedded;

import io.teknek.reckoning.generator.Response;
import io.teknek.reckoning.model.CausalLanguageModel;
import io.teknek.reckoning.model.DoNothingGenerateEvent;
import io.teknek.reckoning.safetensors.prompt.PromptContext;
import io.teknek.reckoning.safetensors.prompt.PromptSupport;
import io.teknek.reckoning.springai.ReckoningChatOptions;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EmbeddedReckoningChatModel implements ChatModel {
    private final CausalLanguageModel model;
    private final ReckoningChatOptions options;

    public EmbeddedReckoningChatModel(CausalLanguageModel model, ReckoningChatOptions options) {
        this.model = model;
        this.options = options;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        PromptContext promptContext = toPromptContext(prompt);
        Response response = model.generate(UUID.randomUUID(), promptContext,
                EmbeddedReckoningOptionsMapper.toGeneratorParameters(mergeOptions(prompt.getOptions())),
                new DoNothingGenerateEvent());
        return new ChatResponse(List.of(new Generation(new AssistantMessage(response.responseText))));
    }

    @Override
    public ChatOptions getOptions() {
        return options;
    }

    private PromptContext toPromptContext(Prompt prompt) {
        PromptSupport promptSupport = model.promptSupport()
                .orElseThrow(() -> new IllegalStateException("Embedded Reckoning model has no PromptSupport"));
        PromptSupport.Builder builder = promptSupport.builder();
        prompt.getInstructions().forEach(message -> {
            switch (message.getMessageType()) {
                case SYSTEM -> builder.addSystemMessage(message.getText());
                case ASSISTANT -> builder.addAssistantMessage(message.getText());
                case TOOL -> builder.addToolResult(io.teknek.reckoning.safetensors.prompt.ToolResult.from("tool",
                        "tool", message.getText()));
                default -> builder.addUserMessage(message.getText());
            }
        });
        return builder.build();
    }

    private ReckoningChatOptions mergeOptions(ChatOptions promptOptions) {
        if (promptOptions == null) {
            return options;
        }
        if (promptOptions instanceof ReckoningChatOptions reckoningOptions) {
            return reckoningOptions;
        }
        return ReckoningChatOptions.builder()
                .model(promptOptions.getModel() == null ? options.getModel() : promptOptions.getModel())
                .temperature(promptOptions.getTemperature() == null ? options.getTemperature() : promptOptions.getTemperature())
                .maxTokens(promptOptions.getMaxTokens() == null ? options.getMaxTokens() : promptOptions.getMaxTokens())
                .topP(promptOptions.getTopP() == null ? options.getTopP() : promptOptions.getTopP())
                .topK(promptOptions.getTopK() == null ? options.getTopK() : promptOptions.getTopK())
                .stopSequences(promptOptions.getStopSequences() == null ? options.getStopSequences() : promptOptions.getStopSequences())
                .build();
    }
}
