package com.syd.ops.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 系统健康检查工具
 */
@Component
public class SystemHealthTools {

    private final Random random = new Random();

    @Tool(description = "检查指定服务器的健康状态", returnDirect = false)
    public String checkServerHealth(String serverName) {
        // 模拟健康检查逻辑
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("serverName", serverName);
        healthStatus.put("status", random.nextBoolean() ? "健康" : "异常");
        healthStatus.put("cpuUsage", random.nextInt(100) + "%");
        healthStatus.put("memoryUsage", random.nextInt(100) + "%");
        healthStatus.put("diskUsage", random.nextInt(100) + "%");
        healthStatus.put("checkTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return String.format("服务器 %s 健康检查结果：状态=%s，CPU使用率=%s，内存使用率=%s，磁盘使用率=%s，检查时间=%s",
                healthStatus.get("serverName"), healthStatus.get("status"), 
                healthStatus.get("cpuUsage"), healthStatus.get("memoryUsage"),
                healthStatus.get("diskUsage"), healthStatus.get("checkTime"));
    }

    @Tool(description = "检查网络连通性", returnDirect = false)
    public String checkNetworkConnectivity(String targetIP) {
        // 模拟网络检查
        boolean isReachable = random.nextBoolean();
        int latency = random.nextInt(200) + 10; // 10-210ms
        
        return String.format("网络连通性检查 - 目标IP: %s, 状态: %s, 延迟: %dms, 检查时间: %s",
                targetIP, 
                isReachable ? "可达" : "不可达",
                isReachable ? latency : 0,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    @Tool(description = "查询错误码详细信息", returnDirect = false)
    public String queryErrorCodeDetails(String errorCode) {
        // 模拟错误码查询
        Map<String, String> errorCodeMap = new HashMap<>();
        errorCodeMap.put("1621346903", "SAS系统调用超时 - 通常由网络延迟或SAS系统负载过高引起");
        errorCodeMap.put("1613486902", "GRPC调用错误 - 服务间通信异常，可能是下游服务不可用");
        errorCodeMap.put("1613456001", "参数错误 - 请求参数格式不正确或缺少必要参数");
        errorCodeMap.put("1613456904", "Redis系统异常 - 缓存服务不可用或连接超时");
        
        String description = errorCodeMap.getOrDefault(errorCode, "未知错误码，请查阅详细文档");
        
        return String.format("错误码 %s 详情: %s", errorCode, description);
    }

    @Tool(description = "获取系统负载信息", returnDirect = false)
    public String getSystemLoad() {
        return String.format("当前系统负载 - CPU: %d%%, 内存: %d%%, 磁盘IO: %d%%, 网络IO: %d%%, 时间: %s",
                random.nextInt(100), random.nextInt(100), random.nextInt(100), random.nextInt(100),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
}