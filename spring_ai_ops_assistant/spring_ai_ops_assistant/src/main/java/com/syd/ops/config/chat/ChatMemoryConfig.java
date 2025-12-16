package com.syd.ops.config.chat;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class ChatMemoryConfig {

    // 1. 注册 Redis 版的 ChatMemoryRepository bean
    @Bean
    public ChatMemoryRepository chatMemoryRepository() {
        // RedisChatMemoryRepository 需要依赖 RedisTemplate
        return new InMemoryChatMemoryRepository();
    }

    // 2. 注入 ChatMemoryRepository 并创建 ChatMemory bean
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();
    }
}
