package com.yupi.aitravelplanner.controller;

import com.yupi.aitravelplanner.common.BaseResponse;
import com.yupi.aitravelplanner.model.dto.ChatRequest;
import com.yupi.aitravelplanner.model.dto.TravelReportRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AiController 集成测试 —— P0 核心：SSE 流式 + REST 接口
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("AiController 集成测试")
class AiControllerTest {

    @LocalServerPort private int port;
    @Autowired private TestRestTemplate restTemplate;

    private String baseUrl() { return "http://localhost:" + port + "/travel"; }

    // ======================== POST /travel/chat ========================

    @Test
    @DisplayName("TC-AI-01: POST /travel/chat — 正常请求返回 code=0, reply 非空, conversationId 回显")
    void chat_validRequest_returnsReply() {
        ChatRequest req = new ChatRequest();
        req.setMessage("北京3月天气怎么样？");
        req.setConversationId("test-" + System.currentTimeMillis());

        var resp = restTemplate.postForEntity(
                baseUrl() + "/chat", req, BaseResponse.class);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getCode()).isEqualTo(0);
        assertThat(resp.getBody().getData()).isNotNull();
    }

    @Test
    @DisplayName("TC-AI-02: POST /travel/chat — 空消息返回 code≠0")
    void chat_emptyMessage_throwsParamError() {
        ChatRequest req = new ChatRequest();
        req.setMessage("");

        var resp = restTemplate.postForEntity(
                baseUrl() + "/chat", req, BaseResponse.class);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody().getCode()).isNotEqualTo(0);
    }

    // ======================== POST /travel/report ========================

    @Test
    @DisplayName("TC-AI-03: POST /travel/report — title 非空, suggestions[] 非空")
    void report_validRequest_returnsReport() {
        TravelReportRequest req = new TravelReportRequest();
        req.setMessage("帮我规划杭州3日游，预算2000元");
        req.setConversationId("test-" + System.currentTimeMillis());

        var resp = restTemplate.postForEntity(
                baseUrl() + "/report", req, BaseResponse.class);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getCode()).isEqualTo(0);
    }

    // ======================== GET /travel/chat/sse ========================

    @Test
    @DisplayName("TC-AI-04: GET /travel/chat/sse — Content-Type=text/event-stream, 至少收到3帧")
    void chatSse_returnsStream() {
        String url = baseUrl() + "/chat/sse?message=你好&chatId=t1";

        var frames = restTemplate.execute(url, HttpMethod.GET, null,
                clientHttpResponse -> {
                    String ct = clientHttpResponse.getHeaders().getFirst("Content-Type");
                    assertThat(ct).contains("text/event-stream");

                    var reader = new BufferedReader(
                            new InputStreamReader(clientHttpResponse.getBody(), StandardCharsets.UTF_8));
                    List<String> dataFrames = new ArrayList<>();
                    String line;
                    long deadline = System.currentTimeMillis() + 30_000;
                    while ((line = reader.readLine()) != null
                            && System.currentTimeMillis() < deadline) {
                        if (line.startsWith("data:")) dataFrames.add(line);
                    }
                    assertThat(dataFrames.size()).isGreaterThanOrEqualTo(3);
                    return dataFrames;
                });

        assertThat(frames).isNotNull();
        assertThat(frames.size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("TC-AI-07: GET /travel/chat/sse — 中文参数 URL encode 后正常返回")
    void chatSse_chineseParam_returnsStream() {
        String url = baseUrl() + "/chat/sse?message=%E4%BD%A0%E5%A5%BD&chatId=t2";

        var frames = restTemplate.execute(url, HttpMethod.GET, null,
                clientHttpResponse -> {
                    var reader = new BufferedReader(
                            new InputStreamReader(clientHttpResponse.getBody(), StandardCharsets.UTF_8));
                    List<String> dataFrames = new ArrayList<>();
                    String line;
                    long deadline = System.currentTimeMillis() + 30_000;
                    while ((line = reader.readLine()) != null
                            && System.currentTimeMillis() < deadline) {
                        if (line.startsWith("data:")) dataFrames.add(line);
                    }
                    return dataFrames;
                });

        assertThat(frames).isNotNull();
        assertThat(frames.size()).isGreaterThanOrEqualTo(1);
    }

    // ======================== GET /travel/chat/sse/emitter ========================

    @Test
    @DisplayName("TC-AI-05: GET /travel/chat/sse/emitter — SseEmitter 正常推送")
    void chatSseEmitter_returnsStream() {
        String url = baseUrl() + "/chat/sse/emitter?message=你好&chatId=t3";

        var frames = restTemplate.execute(url, HttpMethod.GET, null,
                clientHttpResponse -> {
                    String ct = clientHttpResponse.getHeaders().getFirst("Content-Type");
                    assertThat(ct).contains("text/event-stream");

                    var reader = new BufferedReader(
                            new InputStreamReader(clientHttpResponse.getBody(), StandardCharsets.UTF_8));
                    List<String> lines = new ArrayList<>();
                    String line;
                    long deadline = System.currentTimeMillis() + 30_000;
                    while ((line = reader.readLine()) != null
                            && System.currentTimeMillis() < deadline) {
                        lines.add(line);
                    }
                    return lines;
                });

        assertThat(frames).isNotNull();
        assertThat(frames.size()).isGreaterThanOrEqualTo(1);
    }
}
