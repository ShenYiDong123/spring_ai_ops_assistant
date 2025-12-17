package com.syd.ops.service;

import com.syd.ops.entity.AlertLevel;
import com.syd.ops.entity.MsgRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 告警分析服务
 * 负责告警预处理、分类、优先级判断等
 */
@Service
public class AlertAnalysisService {

    // 错误码正则表达式
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("错误码[=：]?(\\d+)");
    private static final Pattern IP_PATTERN = Pattern.compile("IP[=：]?([\\d.]+)");
    private static final Pattern SERVER_PATTERN = Pattern.compile("Server[=：]?([\\w_]+)");
    private static final Pattern SERVICE_PATTERN = Pattern.compile("Service/CGI[=：]?([\\w_]+)");

    /**
     * 预处理告警消息，提取关键信息
     */
    public Map<String, Object> preprocessAlert(MsgRequest request) {
        Map<String, Object> analysis = new HashMap<>();
        String msg = request.getMsg();
        
        // 提取错误码
        String errorCode = extractErrorCode(msg);
        analysis.put("errorCode", errorCode);
        
        // 判断告警级别
        AlertLevel level = AlertLevel.fromErrorCode(errorCode);
        analysis.put("alertLevel", level);
        request.setAlertLevel(level);
        
        // 提取系统信息
        analysis.put("targetIP", extractByPattern(msg, IP_PATTERN));
        analysis.put("targetServer", extractByPattern(msg, SERVER_PATTERN));
        analysis.put("targetService", extractByPattern(msg, SERVICE_PATTERN));
        
        // 判断告警类型
        String alertType = classifyAlertType(msg, errorCode);
        analysis.put("alertType", alertType);
        
        // 提取时间信息
        String timeInfo = extractTimeInfo(msg);
        analysis.put("timeInfo", timeInfo);
        
        return analysis;
    }

    /**
     * 提取错误码
     */
    private String extractErrorCode(String msg) {
        Matcher matcher = ERROR_CODE_PATTERN.matcher(msg);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * 通用正则提取方法
     */
    private String extractByPattern(String msg, Pattern pattern) {
        Matcher matcher = pattern.matcher(msg);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * 分类告警类型
     */
    private String classifyAlertType(String msg, String errorCode) {
        if (msg.contains("超时") || msg.contains("timeout")) {
            return "网络超时";
        }
        if (msg.contains("系统错误") || msg.contains("system error")) {
            return "系统错误";
        }
        if (msg.contains("调用") && msg.contains("失败")) {
            return "服务调用失败";
        }
        if (errorCode != null && errorCode.startsWith("1621")) {
            return "第三方系统调用异常";
        }
        if (errorCode != null && errorCode.startsWith("1613")) {
            return "业务逻辑错误";
        }
        return "未知类型";
    }

    /**
     * 提取时间信息
     */
    private String extractTimeInfo(String msg) {
        Pattern timePattern = Pattern.compile("(\\d{1,2}-\\d{1,2}\\s+\\d{1,2}:\\d{2}:\\d{2})");
        Matcher matcher = timePattern.matcher(msg);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * 生成增强的系统提示词
     */
    public String generateEnhancedSystemPrompt(Map<String, Object> analysis) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("# 角色\n");
        prompt.append("你是一名资深运维工程师，专注于智能告警和故障根因分析。\n\n");
        
        prompt.append("# 当前告警分析上下文\n");
        prompt.append("- **告警级别**: ").append(analysis.get("alertLevel")).append("\n");
        prompt.append("- **告警类型**: ").append(analysis.get("alertType")).append("\n");
        prompt.append("- **错误码**: ").append(analysis.get("errorCode")).append("\n");
        prompt.append("- **目标服务器**: ").append(analysis.get("targetServer")).append("\n");
        prompt.append("- **目标服务**: ").append(analysis.get("targetService")).append("\n");
        prompt.append("- **发生时间**: ").append(analysis.get("timeInfo")).append("\n\n");
        
        prompt.append("# 主要任务\n");
        prompt.append("严格根据用户提供的错误码、故障现象以及**已知的系统故障传播路径和知识库信息**，对故障进行精准、专业的分析。\n\n");
        
        // 根据告警级别调整分析重点
        AlertLevel level = (AlertLevel) analysis.get("alertLevel");
        if (level == AlertLevel.CRITICAL) {
            prompt.append("⚠️ **严重告警**: 请优先分析业务影响和紧急处理措施！\n\n");
        }
        
        prompt.append(getBaseSystemPrompt());
        
        return prompt.toString();
    }

    /**
     * 基础系统提示词
     */
    private String getBaseSystemPrompt() {
        return """
            # 已知关键故障路径（重要背景信息）
            以下是一个典型的故障传播案例，请作为分析类似问题的重要参考：
            - **触发点**: 定时任务请求
            - **传播路径**: timer_vb_base_openacct (定时任务服务) → ao_vb_base_account_server (账户AO服务)
            - **故障点**: 
              - **根因**: ao_vb_base_account_server 在处理客户问卷更新时调用 **SAS系统超时**。
              - **直接表现**: 返回错误码 `1621346903`，内容为 `"call sas system error: java.net.SocketTimeoutException: Read timed out"`。
              - **影响传导**: AO服务将错误向上游包装为错误码 `1613486902` ("call grpc system error") 返回。
            - **核心影响**: 此故障导致无法完成关键的 **CDD（客户尽职调查）状态查询**。
            - **核心错误码**: 
              - `1621346903` (SAS系统调用超时 - 根因)
              - `1613486902` (AO服务GRPC调用错误 - 衍生现象)

            # 工作流程与输出要求
            1.  **确认信息**: 首先识别用户查询中的错误码或故障现象。
            2.  **关联分析**: 将当前问题与上述"已知关键故障路径"进行比对，分析其相似性或差异性。
            3.  **结合检索内容**: 深度分析RAG检索提供的相关文档片段，这是最重要的判断依据。
            4.  **结构化输出**: 你的回答必须严格遵循以下Markdown格式：

            ## 故障分析报告
            - **错误码/现象**: [用户输入的内容]
            - **初步判断**: 一句话简要概括故障性质（如：第三方系统调用超时、服务间通信错误等）。

            ## 推理思路
            （此处展示你的分析过程，例如：）
            - 根据已知故障路径，错误码 `16xxxxxx` 通常与...相关。
            - 当前错误码/现象与提供的"关键故障路径"案例在[某某方面]高度相似/存在不同。
            - 结合检索到的知识库文档[简要提及关键信息]...

            ## 详细解读
            1.  **根因分析**: 推断最可能的根本原因。
            2.  **影响范围**: 说明对哪些服务或业务流程可能造成影响。
            3.  **处理建议**:
                - **立即行动**: 例如，检查SAS系统健康状况、网络连通性、超时设置等。
                - **长期优化**: 例如，建议增加重试机制、优化查询逻辑、完善监控报警等。

            ## 置信度
            高/中/低（基于与已知故障模式的匹配度及检索到信息的完整程度)

            # 重要规则
            - **严格依据**: 你的分析必须严格基于RAG检索到的知识库内容、本次对话的上下文以及上面提供的"已知关键故障路径"。
            - **聚焦运维**: 专注于系统调用超时、服务通信错误、资源瓶颈等运维层面的技术分析。
            - **诚实守信**: 如果检索结果和已有知识中均找不到相关信息，务必在"初步判断"中明确说明，并将"置信度"标记为"低"。严禁臆断。
            """;
    }
}