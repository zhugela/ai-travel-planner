package com.yupi.aitravelplanner.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 旅行报告请求
 */
@Data
public class TravelReportRequest implements Serializable {

    private String message;

    private String conversationId;
}
