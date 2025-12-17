package com.syd.ops.entity;

/**
 * 告警级别枚举
 */
public enum AlertLevel {
    CRITICAL("严重", 1, "#FF4444"),
    HIGH("高", 2, "#FF8800"), 
    MEDIUM("中", 3, "#FFBB33"),
    LOW("低", 4, "#00C851"),
    INFO("信息", 5, "#33B5E5");

    private final String description;
    private final int priority;
    private final String color;

    AlertLevel(String description, int priority, String color) {
        this.description = description;
        this.priority = priority;
        this.color = color;
    }

    public String getDescription() { return description; }
    public int getPriority() { return priority; }
    public String getColor() { return color; }

    /**
     * 根据错误码判断告警级别
     */
    public static AlertLevel fromErrorCode(String errorCode) {
        if (errorCode == null) return INFO;
        
        // 系统错误类（16134xxx）- 严重
        if (errorCode.startsWith("1613456") && errorCode.endsWith("04")) {
            return CRITICAL;
        }
        // 业务错误类 - 高
        if (errorCode.startsWith("1613456")) {
            return HIGH;
        }
        // 网络超时类 - 中
        if (errorCode.startsWith("1621346")) {
            return MEDIUM;
        }
        
        return LOW;
    }
}