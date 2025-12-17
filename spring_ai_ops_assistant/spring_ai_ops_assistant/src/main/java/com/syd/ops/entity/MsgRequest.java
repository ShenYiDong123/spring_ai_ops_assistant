package com.syd.ops.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
public class MsgRequest {

    private String msg;           // 消息内容（包含特殊字符）
    private String modelType;     // 模型类型
    private String sessionId;     // 会话ID，用于对话记忆隔离
    private AlertLevel alertLevel; // 告警级别
    private String source;        // 告警来源系统
    private LocalDateTime timestamp; // 告警时间戳
    private String userId;        // 用户ID
    private boolean enableRAG = true;    // 是否启用RAG
    private boolean enableMemory = true; // 是否启用对话记忆
    
    // 构造函数
    public MsgRequest() {
        this.timestamp = LocalDateTime.now();
        this.sessionId = generateSessionId();
    }
    
    private String generateSessionId() {
        return userId != null ? userId + "_" + System.currentTimeMillis() 
                             : "anonymous_" + System.currentTimeMillis();
    }
}
