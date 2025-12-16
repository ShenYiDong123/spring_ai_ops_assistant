package com.syd.ops.controller;

import com.alibaba.cloud.ai.advisor.DocumentRetrievalAdvisor;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import com.syd.ops.entity.MsgRequest;
import com.syd.ops.tools.DateTimeTools;
import jakarta.annotation.Resource;
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

/**
 * AI智能运维助手
 */
@RestController
public class AssistantController {

    @Resource(name = "qwenChatClient")
    private ChatClient qwenChatClient;

    @Resource(name = "hunyuanChatClient")
    private ChatClient hunyuanChatClient;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private DashScopeApi dashScopeApi;

    @Autowired
    ChatMemory chatMemory;

    /**
     * http://localhost:8100/sendMsg?msg=404是什么意思&&modelType=qwen
     *
     * @param
     * @return
     */
    @PostMapping("/sendMsg")
    public Flux<String> sendMsg(@RequestBody MsgRequest msgReq) {
        // 1. 根据modelType动态获取对应的ChatClient
        ChatClient chatClient = getChatClientByType(msgReq.getModelType());

        String systemInfo = """
            # 角色
            你是一名资深运维工程师，专注于智能告警和故障根因分析。
    
            # 主要任务
            严格根据用户提供的错误码、故障现象以及**已知的系统故障传播路径和知识库信息**，对故障进行精准、专业的分析。
    
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
            2.  **关联分析**: 将当前问题与上述“已知关键故障路径”进行比对，分析其相似性或差异性。
            3.  **结合检索内容**: 深度分析RAG检索提供的相关文档片段，这是最重要的判断依据。
            4.  **结构化输出**: 你的回答必须严格遵循以下Markdown格式：
    
            ## 故障分析报告
            - **错误码/现象**: [用户输入的内容]
            - **初步判断**: 一句话简要概括故障性质（如：第三方系统调用超时、服务间通信错误等）。
    
            ## 推理思路
            （此处展示你的分析过程，例如：）
            - 根据已知故障路径，错误码 `16xxxxxx` 通常与...相关。
            - 当前错误码/现象与提供的“关键故障路径”案例在[某某方面]高度相似/存在不同。
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
            - **严格依据**: 你的分析必须严格基于RAG检索到的知识库内容、本次对话的上下文以及上面提供的“已知关键故障路径”。
            - **聚焦运维**: 专注于系统调用超时、服务通信错误、资源瓶颈等运维层面的技术分析。
            - **诚实守信**: 如果检索结果和已有知识中均找不到相关信息，务必在“初步判断”中明确说明，并将“置信度”标记为“低”。严禁臆断。
        """;


        // 构建 Memory Advisor
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        //开启RAG(本地)
        /*RetrievalAugmentationAdvisor ragAdvisor = RetrievalAugmentationAdvisor.builder().documentRetriever(
                                                    VectorStoreDocumentRetriever.builder().vectorStore(vectorStore).build()
                                                ).build();*/

        //开启RAG(百炼)
        DocumentRetrievalAdvisor ragAdvisor = new DocumentRetrievalAdvisor(
                new DashScopeDocumentRetriever(dashScopeApi,
                    DashScopeDocumentRetrieverOptions.builder()
                            .withIndexName("ops")
                            .build()));

        // 流式调用
        return chatClient
                .prompt()
                .system(systemInfo)
                .advisors(ragAdvisor,memoryAdvisor) // 注入RAG Advisor, ChatMemory对话记忆
                .tools(new DateTimeTools()) // 注入工具类
                .user(msgReq.getMsg())
                .stream()
                .content();

    }

    /**
     * 根据模型类型获取对应的ChatClient
     *
     * @param modelType 模型类型（qwen/hunyuan）
     * @return 对应的ChatClient
     */
    private ChatClient getChatClientByType(String modelType) {
        // 忽略大小写，提升用户体验
        return switch (modelType.trim().toLowerCase()) {
            case "qwen" -> qwenChatClient;
            case "hunyuan" -> hunyuanChatClient;
            // 处理非法模型类型，抛出友好异常
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "不支持的模型类型：" + modelType + "，仅支持qwen和hunyuan"
            );
        };
    }
}
