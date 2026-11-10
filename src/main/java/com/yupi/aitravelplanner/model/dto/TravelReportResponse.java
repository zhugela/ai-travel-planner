package com.yupi.aitravelplanner.model.dto;

import com.yupi.aitravelplanner.app.TravelApp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 旅行报告响应(分天行程 + 预算拆分)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelReportResponse implements Serializable {

    private String conversationId;

    private String title;

    /** 一句话概述行程主线 */
    private String summary;

    /** 分天行程 */
    private List<TravelApp.DayPlan> days;

    /** 预算拆分 */
    private TravelApp.Budget budget;

    /** 为缺失信息补的默认值 */
    private List<String> assumptions;

    /** 注意事项 */
    private List<String> tips;
}
