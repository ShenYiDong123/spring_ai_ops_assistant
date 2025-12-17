package com.syd.ops.controller;

import com.alibaba.cloud.ai.advisor.DocumentRetrievalAdvisor;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import com.syd.ops.entity.MsgRequest;
import com.syd.ops.service.AlertAnalysisService;
import com.syd.ops.tools.DateTimeTools;
import com.syd.ops.tools.SystemHealthTools;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * AI智能运维助手
 */
@RestController
public class AssistantController {

    private static final Logger logger = LoggerFactory.getLogger(AssistantController.class);

    @Resource(name = "qwenChatClient")
    private ChatClient qwenChatClient;

    @Resource(name = "hunyuanChatClient")
    private ChatClient hunyuanChatClient;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private DashScopeApi dashScopeApi;

    @Autowired
    private ChatMemory chatMemory;

    @Autowired
    private AlertAnalysisService alertAnalysisService;

    @Autowired
    private SystemHealthTools systemHealthTools;

    /**
     * 智能告警分析接口
     * http://127.0.0.1:8100/sendMsg
     * {
     *     "msg":"[告警]11-20 13:33:00,vb_base_java&错误码=1621346903&错误类型=1(系统类型)&目的IP=10.150.12.35&目的Server=ao_vb_base_account_server&目的Service/CGI=handle_qaire_customer_updated的系统错误量异常,错误码1621346903(系统-call sas system error)(当前值1,大于等于阈值1)(配置ID=81)(系统错误码兜底告警，连续1次就触发，5分钟内告警一次，未发给ECC)",
     *     "modelType":"qwen",
     *     "sessionId":"user123_session",
     *     "userId":"user123",
     *     "enableRAG": true,
     *     "enableMemory": true
     * }
     */
    @PostMapping("/sendMsg")
    public Flux<String> sendMsg(@RequestBody MsgRequest msgReq) {
        try {
            logger.info("收到告警分析请求 - 用户: {}, 模型: {}, 会话: {}", 
                       msgReq.getUserId(), msgReq.getModelType(), msgReq.getSessionId());

            // 1. 预处理告警信息
            Map<String, Object> analysis = alertAnalysisService.preprocessAlert(msgReq);
            logger.info("告警预处理完成 - 错误码: {}, 级别: {}, 类型: {}", 
                       analysis.get("errorCode"), analysis.get("alertLevel"), analysis.get("alertType"));

            // 2. 根据modelType动态获取对应的ChatClient
            ChatClient chatClient = getChatClientByType(msgReq.getModelType());

            // 3. 生成增强的系统提示词
            String enhancedSystemPrompt = alertAnalysisService.generateEnhancedSystemPrompt(analysis);

            // 4. 构建ChatClient
            ChatClient.ChatClientRequestSpec requestSpec = chatClient
                    .prompt()
                    .system(enhancedSystemPrompt)
                    .user(msgReq.getMsg());

            // 5. 条件性添加Advisors
            if (msgReq.isEnableMemory()) {
                MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(msgReq.getSessionId()) // 使用sessionId隔离对话
                        .build();
                requestSpec = requestSpec.advisors(memoryAdvisor);
                logger.debug("已启用对话记忆 - 会话ID: {}", msgReq.getSessionId());
            }

            if (msgReq.isEnableRAG()) {
                DocumentRetrievalAdvisor ragAdvisor = new DocumentRetrievalAdvisor(
                        new DashScopeDocumentRetriever(dashScopeApi,
                            DashScopeDocumentRetrieverOptions.builder()
                                    .withIndexName("ops")
                                    .build()));
                requestSpec = requestSpec.advisors(ragAdvisor);
                logger.debug("已启用RAG检索");
            }

            // 6. 添加工具集
            requestSpec = requestSpec.tools(new DateTimeTools(), systemHealthTools);

            // 7. 流式调用
            return requestSpec.stream().content()
                    .doOnComplete(() -> logger.info("告警分析完成 - 会话: {}", msgReq.getSessionId()))
                    .doOnError(error -> logger.error("告警分析失败 - 会话: {}, 错误: {}", 
                                                   msgReq.getSessionId(), error.getMessage()));

        } catch (Exception e) {
            logger.error("告警分析系统异常", e);
            return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                                                        "系统内部错误: " + e.getMessage()));
        }
    }

    /**
     * 根据模型类型获取对应的ChatClient
     *
     * @param modelType 模型类型（qwen/hunyuan）
     * @return 对应的ChatClient
     */
    private ChatClient getChatClientByType(String modelType) {
        if (modelType == null || modelType.trim().isEmpty()) {
            logger.warn("模型类型为空，使用默认qwen模型");
            return qwenChatClient;
        }

        // 忽略大小写，提升用户体验
        return switch (modelType.trim().toLowerCase()) {
            case "qwen" -> {
                logger.debug("使用千问模型");
                yield qwenChatClient;
            }
            case "hunyuan" -> {
                logger.debug("使用混元模型");
                yield hunyuanChatClient;
            }
            // 处理非法模型类型，抛出友好异常
            default -> {
                logger.error("不支持的模型类型: {}", modelType);
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "不支持的模型类型：" + modelType + "，仅支持qwen和hunyuan"
                );
            }
        };
    }
}
