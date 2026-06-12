package com.yupi.aitravelplanner.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 聊天请求
 */
@Data
public class ChatRequest implements Serializable {

    /**
     * 用户消息
     */
    private String message;

    /**
     * 会话 id，同一 id 可多轮对话；为空则服务端生成
     */
    private String conversationId;
}
