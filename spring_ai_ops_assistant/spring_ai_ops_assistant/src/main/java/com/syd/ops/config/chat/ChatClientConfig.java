package com.syd.ops.config.chat;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import io.github.studiousxiaoyu.hunyuan.api.HunYuanApi;
import io.github.studiousxiaoyu.hunyuan.chat.HunYuanChatModel;
import io.github.studiousxiaoyu.hunyuan.chat.HunYuanChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * 各种大模型的ChatClient配置
 */
@Component
public class ChatClientConfig {

    /**
     * 阿里云千问配置
     */
    @Value("${spring.ai.dashscope.api-key}")
    private String dashscopeApiKey;

    @Value("${spring.ai.dashscope.chat.options.model}")
    private String qwenModel;


    /**
     * 腾讯云混元配置
     */
    @Value("${spring.ai.hunyuan.base-url}")
    private String hunyuanBaseUrl;

    @Value("${spring.ai.hunyuan.secret-key}")
    private String hunyuanSecretKey;

    @Value("${spring.ai.hunyuan.secret-id}")
    private String hunyuanSecretId;

    @Value("${spring.ai.hunyuan.chat.options.model}")
    private String hunyuanModel;


    @Bean(name = "qwen")
    public ChatModel qwen()
    {
        return DashScopeChatModel.builder().dashScopeApi(DashScopeApi.builder()
                        .apiKey(dashscopeApiKey)
                        .build())
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                .withModel(qwenModel)
                                .build()
                )
                .build();
    }

    @Bean(name = "qwenChatClient")
    public ChatClient qwenChatClient(@Qualifier("qwen") ChatModel qwen)
    {
        return ChatClient.builder(qwen)
                .defaultOptions(ChatOptions.builder()
                        .model(qwenModel)
                        .build())
                .build();
    }

    @Bean
    public DashScopeApi dashScopeApi() {
        return DashScopeApi.builder()
                .apiKey(dashscopeApiKey)
                .workSpaceId("llm-5a43gsoax8chasqg")
                .build();
    }


    @Bean(name = "hunyuan")
    public ChatModel hunyuan() {
        // 1. 纯手动构建混元的OpenAiApi（适配多模型，避免自动配置冲突）
        HunYuanApi openAiApi = new HunYuanApi(hunyuanBaseUrl, hunyuanSecretKey);

        HunYuanChatOptions defaultOptions = HunYuanChatOptions.builder()
                .model(hunyuanModel) // 1.0.0-M2版本保留withModel方法，不要改成model
                // .withTemperature(0.7)
                .build();

        return HunYuanChatModel.builder().defaultOptions(defaultOptions).hunYuanApi(openAiApi).build();
    }

    @Bean(name = "hunyuanChatClient")
    public ChatClient hunyuanChatClient(@Qualifier("hunyuan") ChatModel hunyuan) {
        return ChatClient.builder(hunyuan)
                .defaultOptions(ChatOptions.builder()
                        .model(hunyuanModel) // ChatOptions的model方法是通用的，1.0.0-M2支持
                        .build())
                .build();
    }




}
