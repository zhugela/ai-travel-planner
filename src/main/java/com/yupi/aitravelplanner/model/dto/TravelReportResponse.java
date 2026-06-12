package com.yupi.aitravelplanner.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 旅行报告响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelReportResponse implements Serializable {

    private String conversationId;

    private String title;

    private List<String> suggestions;
}
