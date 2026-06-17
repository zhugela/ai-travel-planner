package com.yupi.aitravelplanner.controller;

// ==== 通用响应工具 ====
import com.yupi.aitravelplanner.common.BaseResponse;
import com.yupi.aitravelplanner.common.ResultUtils;
import com.yupi.aitravelplanner.exception.ErrorCode;
import com.yupi.aitravelplanner.exception.ThrowUtils;

// ==== DTO ====
import com.yupi.aitravelplanner.model.dto.TravelRagRequest;
import com.yupi.aitravelplanner.model.dto.TravelRagResponse;

// ==== 业务 Service ====
import com.yupi.aitravelplanner.service.TravelRagChatService;

// ==== Spring 框架 ====
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 旅游 RAG 问答 HTTP 接口
 *
 * 路径:POST /api/travel/rag-chat
 *  (yml 里 context-path=/api,所以 Controller 里只写 /travel/rag-chat)
 *
 * 设计要点:
 *   1) 用 @RequestBody 接 JSON,沿用项目现有 AiController 风格
 *   2) 真正的客户端错误(参数为空)→ 抛 BusinessException → code != 0
 *   3) 业务正常路径(包括无匹配)→ code=0,前端看 matched 字段
 *   4) conversationId 只回显,不做服务端多轮(本步骤先打通链路)
 */
@Slf4j
@RestController
@RequestMapping("/travel/rag-chat")
@Tag(name = "旅游 RAG 问答")
public class TravelRagController {

    @Resource
    private TravelRagChatService travelRagChatService;

    /**
     * RAG 问答统一入口
     *
     * @param travelRagRequest 包含 question(必填)+ conversationId(可选)
     * @return BaseResponse<TravelRagResponse> {code:0, data:{answer, matched, conversationId}}
     */
    @PostMapping
    @Operation(summary = "基于旅游知识库的 RAG 问答")
    public BaseResponse<TravelRagResponse> ragChat(@RequestBody TravelRagRequest travelRagRequest) {

        // 1) 参数校验(真正的客户端错误 → code != 0)
        ThrowUtils.throwIf(travelRagRequest == null, ErrorCode.PARAMS_ERROR, "请求体不能为空");
        String question = travelRagRequest.getQuestion();
        ThrowUtils.throwIf(!StringUtils.hasText(question),
                ErrorCode.PARAMS_ERROR, "question 不能为空");

        // 2) 打印入参日志(便于排查请求内容,排查阈值/切片效果)
        log.info("[RAG 接口入参] question={} | conversationId={}",
                question, travelRagRequest.getConversationId());

        // 3) 调用 Service 走 RAG 全流程
        //    Service 内部:检索 → 命中调模型 / 未命中返回兜底
        String answer = travelRagChatService.chatWithRag(question);

        // 4) 判断是否命中:比对 Service 的 NO_MATCH_REPLY 常量
        //    (NO_MATCH_REPLY 在 Service 里已改为 public,这里直接引用)
        boolean matched = !TravelRagChatService.NO_MATCH_REPLY.equals(answer);

        // 5) 构造响应(命中 → matched=true,未命中 → matched=false,但 code 永远是 0)
        TravelRagResponse response = new TravelRagResponse(
                answer,
                matched,
                travelRagRequest.getConversationId()
        );

        // 6) 统一返回 code=0 的标准结构
        return ResultUtils.success(response);
    }
}
