package com.syd.ops.config.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis实现的ChatMemoryRepository
 * 用于将对话记忆持久化存储到Redis中
 */
@Component
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final String CHAT_MEMORY_KEY_PREFIX = "spring_ai:chat_memory:";
    private static final long DEFAULT_TTL_HOURS = 24; // 默认24小时过期

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisChatMemoryRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        String key = buildKey(conversationId);
        try {
            // 序列化并存储到Redis
            String serializedMessages = objectMapper.writeValueAsString(messages);
            redisTemplate.opsForValue().set(key, serializedMessages, DEFAULT_TTL_HOURS, TimeUnit.HOURS);
            
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize messages for conversation: " + conversationId, e);
        }
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = buildKey(conversationId);
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return new ArrayList<>();
            }
            
            String serializedMessages = value.toString();
            Message[] messages = objectMapper.readValue(serializedMessages, Message[].class);
            return List.of(messages);
            
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize messages for conversation: " + conversationId, e);
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        String key = buildKey(conversationId);
        redisTemplate.delete(key);
    }

    @Override
    public List<String> findConversationIds() {
        String pattern = CHAT_MEMORY_KEY_PREFIX + "*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null) {
            return new ArrayList<>();
        }
        
        return keys.stream()
                .map(key -> key.substring(CHAT_MEMORY_KEY_PREFIX.length()))
                .toList();
    }

    /**
     * 构建Redis key
     */
    private String buildKey(String conversationId) {
        return CHAT_MEMORY_KEY_PREFIX + conversationId;
    }

    /**
     * 设置对话记忆的过期时间
     */
    public void setTtl(String conversationId, long ttl, TimeUnit timeUnit) {
        String key = buildKey(conversationId);
        redisTemplate.expire(key, ttl, timeUnit);
    }

    /**
     * 检查对话记忆是否存在
     */
    public boolean exists(String conversationId) {
        String key = buildKey(conversationId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}