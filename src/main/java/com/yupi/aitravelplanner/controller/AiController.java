package com.yupi.aitravelplanner.controller;

import com.yupi.aitravelplanner.app.TravelApp;
import com.yupi.aitravelplanner.common.BaseResponse;
import com.yupi.aitravelplanner.common.ResultUtils;
import com.yupi.aitravelplanner.exception.ErrorCode;
import com.yupi.aitravelplanner.exception.ThrowUtils;
import com.yupi.aitravelplanner.model.dto.ChatRequest;
import com.yupi.aitravelplanner.model.dto.ChatResponse;
import com.yupi.aitravelplanner.model.dto.TravelReportRequest;
import com.yupi.aitravelplanner.model.dto.TravelReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

/**
 * AI 接口（对应课程 AiController）
 */
@RestController
@RequestMapping("/travel")
@Tag(name = "AI 旅行规划")
public class AiController {

    @Resource
    private TravelApp travelApp;

    /**
     * 多轮对话 - 旅行规划
     */
    @PostMapping("/chat")
    @Operation(summary = "旅行规划多轮对话")
    public BaseResponse<ChatResponse> chat(@RequestBody ChatRequest chatRequest) {
        ThrowUtils.throwIf(chatRequest == null, ErrorCode.PARAMS_ERROR);
        String message = chatRequest.getMessage();
        ThrowUtils.throwIf(!org.springframework.util.StringUtils.hasText(message),
                ErrorCode.PARAMS_ERROR, "消息不能为空");
        TravelApp.ChatResult result = travelApp.doChatWithId(message, chatRequest.getConversationId());
        ChatResponse chatResponse = new ChatResponse(result.conversationId(), result.reply());
        return ResultUtils.success(chatResponse);
    }

    /**
     * 旅行规划结构化报告
     */
    @PostMapping("/report")
    @Operation(summary = "旅行规划结构化报告（JSON）")
    public BaseResponse<TravelReportResponse> report(@RequestBody TravelReportRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        String message = request.getMessage();
        ThrowUtils.throwIf(!org.springframework.util.StringUtils.hasText(message),
                ErrorCode.PARAMS_ERROR, "消息不能为空");
        TravelApp.TravelReportResult result = travelApp.doChatWithTravelReportAndId(
                message, request.getConversationId());
        TravelReportResponse response = new TravelReportResponse(
                result.conversationId(),
                result.report().title(),
                result.report().suggestions());
        return ResultUtils.success(response);
    }

    /**
     * 旅行问答 SSE 流式接口(Ch 09 §2.3 实现)。
     * 返回 Flux<String>,前段 EventSource 监听逐字渲染打字机效果。
     */
    @GetMapping(value = "/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "旅行规划问答(SSE 流式,逐字返回)")
    public Flux<String> doChatWithSSE(@RequestParam String message,
                                      @RequestParam(required = false) String chatId) {
        return travelApp.doChatByStream(message, chatId);
    }

    /**
     * 旅行问答 SseEmitter 备用接口(老式 Servlet 栈兼容)。
     * 与 /chat/sse 二选一使用,效果一致。
     */
    @GetMapping(value = "/chat/sse/emitter", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "旅行规划问答(SseEmitter 备用,Servlet 异步)")
    public SseEmitter doChatWithSseEmitter(@RequestParam String message,
                                          @RequestParam(required = false) String chatId) {
        SseEmitter emitter = new SseEmitter(180_000L);
        travelApp.doChatByStream(message, chatId)
                .subscribe(
                        chunk -> {
                            try {
                                emitter.send(chunk);
                            } catch (java.io.IOException ignored) {
                                // 客户端断网时常见异常,静默忽略即可
                            }
                        },
                        err -> emitter.completeWithError(err),
                        () -> emitter.complete()
                );
        return emitter;
    }
}
