package com.yupi.aitravelplanner.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 聊天响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse implements Serializable {

    /**
     * 会话 id，客户端下次请求带上即可续聊
     */
    private String conversationId;

    /**
     * AI 回复内容
     */
    private String reply;
}
