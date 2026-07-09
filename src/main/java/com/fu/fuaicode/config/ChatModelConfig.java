package com.fu.fuaicode.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * AI模型配置
 *
 */
@Configuration
public class ChatModelConfig {

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String chatBaseUrl;
    
    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String chatApiKey;
    
    @Value("${langchain4j.open-ai.chat-model.model-name:qwen-turbo}")
    private String chatModelName;
    
    @Value("${langchain4j.open-ai.chat-model.max-tokens:8192}")
    private Integer chatMaxTokens;
    
    @Value("${langchain4j.open-ai.streaming-chat-model.base-url}")
    private String streamingBaseUrl;
    
    @Value("${langchain4j.open-ai.streaming-chat-model.api-key}")
    private String streamingApiKey;
    
    @Value("${langchain4j.open-ai.streaming-chat-model.model-name:qwen-turbo}")
    private String streamingModelName;
    
    @Value("${langchain4j.open-ai.streaming-chat-model.max-tokens:8192}")
    private Integer streamingMaxTokens;

    @Value("${langchain4j.open-ai.tool-chat-model.base-url}")
    private String toolChatBaseUrl;

    @Value("${langchain4j.open-ai.tool-chat-model.api-key}")
    private String toolChatApiKey;

    @Value("${langchain4j.open-ai.tool-chat-model.model-name:qwen-plus}")
    private String toolChatModelName;

    @Value("${langchain4j.open-ai.tool-chat-model.max-tokens:8192}")
    private Integer toolChatMaxTokens;

    @Value("${langchain4j.open-ai.tool-streaming-chat-model.base-url}")
    private String toolStreamingChatBaseUrl;

    @Value("${langchain4j.open-ai.tool-streaming-chat-model.api-key}")
    private String toolStreamingChatApiKey;

    @Value("${langchain4j.open-ai.streaming-chat-model.model-name:qwen-turbo}")
    private String toolStreamingChatModelName;

    @Value("${langchain4j.open-ai.tool-streaming-chat-model.max-tokens:8192}")
    private Integer toolStreamingChatMaxTokens;

    /**
     * 普通对话模型（用于HTML和多文件生成）
     */
    @Bean
    @Scope("prototype")
    public ChatModel dashScopeChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(chatBaseUrl)
                .apiKey(chatApiKey)
                .modelName(chatModelName)
                .maxTokens(chatMaxTokens)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 流式对话模型（用于HTML和多文件生成）
     */
    @Bean
    @Scope("prototype")
    public StreamingChatModel dashScopeStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(streamingBaseUrl)
                .apiKey(streamingApiKey)
                .modelName(streamingModelName)
                .maxTokens(streamingMaxTokens)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 工具调用专用非流式模型（用于 VUE_PROJECT 生成，支持 function calling）
     */
    @Bean
    @Scope("prototype")
    public ChatModel toolChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(toolChatBaseUrl)
                .apiKey(toolChatApiKey)
                .modelName(toolChatModelName)
                .maxTokens(toolChatMaxTokens)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 工具调用专用流式模型（用于 VUE_PROJECT 生成，支持 function calling）
     */
    @Bean
    @Scope("prototype")
    public StreamingChatModel toolStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(toolStreamingChatBaseUrl)
                .apiKey(toolStreamingChatApiKey)
                .modelName(toolStreamingChatModelName)
                .maxTokens(toolStreamingChatMaxTokens)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 推理模型流式专用（用于vue项目生成，带工具调用）
     */
    @Bean
    @Scope("prototype")
    public StreamingChatModel reasoningStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(toolStreamingChatBaseUrl)
                .apiKey(toolStreamingChatApiKey)
                .modelName(toolStreamingChatModelName)
                .maxTokens(toolStreamingChatMaxTokens)
                .logRequests(true)
                .logResponses(true)
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}
