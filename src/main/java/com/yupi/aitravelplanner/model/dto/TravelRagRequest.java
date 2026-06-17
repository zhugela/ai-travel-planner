package com.yupi.aitravelplanner.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 旅游 RAG 问答请求 DTO
 *
 * 与 ChatRequest 的差别:
 *   - ChatRequest 用 "message" 字段(原有多轮对话接口)
 *   - TravelRagRequest 用 "question" 字段(RAG 问答专用,语义更准确)
 */
@Data
public class TravelRagRequest implements Serializable {

    /**
     * 用户提问(必填,非空)
     */
    private String question;

    /**
     * 会话标识(可选)
     * 留空则不参与多轮,Service 无状态(每次都是全新检索)
     * 不为空时,Controller 会原样回显到 Response,便于前端做会话关联
     */
    private String conversationId;
}
