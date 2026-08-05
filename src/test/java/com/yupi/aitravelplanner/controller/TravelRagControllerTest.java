package com.yupi.aitravelplanner.controller;

import com.yupi.aitravelplanner.common.BaseResponse;
import com.yupi.aitravelplanner.model.dto.TravelRagRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TravelRagController 集成测试 —— P1：RAG 问答参数校验 + 命中/兜底
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("TravelRagController 集成测试")
class TravelRagControllerTest {

    @LocalServerPort private int port;
    @Autowired private TestRestTemplate restTemplate;

    private String baseUrl() { return "http://localhost:" + port + "/travel/rag-chat"; }

    // ======================== 正常路径 ========================

    @Test
    @DisplayName("TC-RAG-01: 正常问题 → code=0, answer 非空")
    void ragChat_validQuestion_returnsAnswer() {
        TravelRagRequest req = new TravelRagRequest();
        req.setQuestion("请介绍一个国内适合3天短途游的目的地");
        req.setConversationId("test-rag-" + System.currentTimeMillis());

        var resp = restTemplate.postForEntity(
                baseUrl(), req, BaseResponse.class);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getCode()).isEqualTo(0);
        assertThat(resp.getBody().getData()).isNotNull();
    }

    @Test
    @DisplayName("TC-RAG-02: 通用问候 → code=0（知识库兜底/通用回答）")
    void ragChat_generalQuestion_returnsOk() {
        TravelRagRequest req = new TravelRagRequest();
        req.setQuestion("你好");

        var resp = restTemplate.postForEntity(
                baseUrl(), req, BaseResponse.class);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getCode()).isEqualTo(0);
        assertThat(resp.getBody().getData()).isNotNull();
    }

    // ======================== 参数校验 ========================

    @Test
    @DisplayName("TC-RAG-03: 空 question → code≠0（参数校验）")
    void ragChat_emptyQuestion_throwsParamError() {
        TravelRagRequest req = new TravelRagRequest();
        req.setQuestion("");

        var resp = restTemplate.postForEntity(
                baseUrl(), req, BaseResponse.class);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getCode()).isNotEqualTo(0);
    }

    @Test
    @DisplayName("TC-RAG-03b: null question → code≠0")
    void ragChat_nullQuestion_throwsParamError() {
        TravelRagRequest req = new TravelRagRequest();
        req.setQuestion(null);

        var resp = restTemplate.postForEntity(
                baseUrl(), req, BaseResponse.class);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getCode()).isNotEqualTo(0);
    }
}
