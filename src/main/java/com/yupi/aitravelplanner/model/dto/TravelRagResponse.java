package com.yupi.aitravelplanner.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 旅游 RAG 问答响应 DTO
 *
 * 关键字段:
 *   - matched:true = 命中知识库,answer 是 AI 生成的回答
 *           false = 没命中,answer 是固定兜底话术
 *   - code 永远是 0(成功),"无匹配"不算业务异常
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelRagResponse implements Serializable {

    /**
     * AI 回答(命中时)或兜底话术(未命中时)
     */
    private String answer;

    /**
     * 是否命中知识库
     *  true  → 命中,answer 是基于知识库的回答
     *  false → 未命中,answer 是固定话术 "暂无相关旅游攻略信息"
     */
    private boolean matched;

    /**
     * 会话标识(原样回显,无状态)
     */
    private String conversationId;
}
