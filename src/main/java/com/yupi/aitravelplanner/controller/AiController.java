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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
