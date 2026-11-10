package io.teknek.reckoning.springai.embedded;

import io.teknek.reckoning.generator.GeneratorParameters;
import io.teknek.reckoning.springai.ReckoningChatOptions;

import org.springframework.ai.chat.prompt.ChatOptions;

final class EmbeddedReckoningOptionsMapper {
    private EmbeddedReckoningOptionsMapper() {
    }

    static GeneratorParameters toGeneratorParameters(ChatOptions options) {
        GeneratorParameters parameters = new GeneratorParameters();
        if (options == null) {
            return parameters;
        }
        if (options.getTemperature() != null) {
            parameters.withTemperature(options.getTemperature().floatValue());
        }
        if (options.getMaxTokens() != null) {
            parameters.withMaxTokens(options.getMaxTokens());
        }
        if (options.getTopP() != null) {
            parameters.withTopP(options.getTopP().floatValue());
        }
        if (options.getTopK() != null) {
            parameters.withTopK(options.getTopK());
        }
        if (options.getStopSequences() != null) {
            parameters.withStopWords(options.getStopSequences());
        }
        if (options instanceof ReckoningChatOptions reckoningOptions) {
            if (reckoningOptions.getSeed() != null) {
                parameters.withSeed(reckoningOptions.getSeed());
            }
            if (reckoningOptions.getLogprobs() != null) {
                parameters.withLogProbs(reckoningOptions.getLogprobs());
            }
            if (reckoningOptions.getTopLogprobs() != null) {
                parameters.withTopLogProbs(reckoningOptions.getTopLogprobs());
            }
            if (reckoningOptions.getXtcThreshold() != null) {
                parameters.withXtcThreshold(reckoningOptions.getXtcThreshold().floatValue());
            }
            if (reckoningOptions.getXtcProbability() != null) {
                parameters.withXtcProbability(reckoningOptions.getXtcProbability().floatValue());
            }
            if (reckoningOptions.getGuidedChoice() != null) {
                parameters.withGuidedChoice(reckoningOptions.getGuidedChoice());
            }
            if (reckoningOptions.getGuidedRegex() != null) {
                parameters.withGuidedRegex(reckoningOptions.getGuidedRegex());
            }
            if (reckoningOptions.getGuidedJson() != null) {
                parameters.withGuidedJson(reckoningOptions.getGuidedJson());
            }
        }
        return parameters;
    }
}
