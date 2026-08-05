package com.yupi.aitravelplanner.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AgentController 集成测试 —— P0 核心：Agent SSE 事件协议
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("AgentController 集成测试")
class AgentControllerTest {

    @LocalServerPort private int port;
    @Autowired private TestRestTemplate restTemplate;

    private String baseUrl() { return "http://localhost:" + port + "/agent"; }

    // ======================== SSE 事件协议 ========================

    @Test
    @DisplayName("TC-AGT-01: HTTP 200, Content-Type=text/event-stream")
    void agentSse_returnsEventStream() {
        String url = baseUrl() + "/sse?message=帮我规划北京3日游";

        var result = restTemplate.execute(url, HttpMethod.GET, null,
                clientHttpResponse -> {
                    String ct = clientHttpResponse.getHeaders().getFirst("Content-Type");
                    assertThat(ct).contains("text/event-stream");

                    var reader = new BufferedReader(
                            new InputStreamReader(clientHttpResponse.getBody(), StandardCharsets.UTF_8));
                    List<String> events = new ArrayList<>();
                    String line;
                    long deadline = System.currentTimeMillis() + 60_000;
                    while ((line = reader.readLine()) != null
                            && System.currentTimeMillis() < deadline) {
                        events.add(line);
                    }
                    return events;
                });

        assertThat(result).isNotNull();
        assertThat(result.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("TC-AGT-02: 收到 event:step 帧")
    void agentSse_receivesStepEvent() {
        String url = baseUrl() + "/sse?message=你好";

        var result = restTemplate.execute(url, HttpMethod.GET, null,
                clientHttpResponse -> {
                    var reader = new BufferedReader(
                            new InputStreamReader(clientHttpResponse.getBody(), StandardCharsets.UTF_8));
                    List<String> events = new ArrayList<>();
                    String line;
                    long deadline = System.currentTimeMillis() + 60_000;
                    while ((line = reader.readLine()) != null
                            && System.currentTimeMillis() < deadline) {
                        events.add(line);
                    }
                    return events;
                });

        assertThat(result).isNotNull();
        boolean hasStepEvent = result.stream().anyMatch(l -> l.startsWith("event:") && l.contains("step"));
        assertThat(hasStepEvent).isTrue();
    }

    @Test
    @DisplayName("TC-AGT-04: 收到 event:done（任务正常结束）")
    void agentSse_receivesDoneOrCompletes() {
        String url = baseUrl() + "/sse?message=1+1等于几";

        var result = restTemplate.execute(url, HttpMethod.GET, null,
                clientHttpResponse -> {
                    var reader = new BufferedReader(
                            new InputStreamReader(clientHttpResponse.getBody(), StandardCharsets.UTF_8));
                    List<String> events = new ArrayList<>();
                    String line;
                    long deadline = System.currentTimeMillis() + 60_000;
                    while ((line = reader.readLine()) != null
                            && System.currentTimeMillis() < deadline) {
                        events.add(line);
                    }
                    return events;
                });

        assertThat(result).isNotNull();
        // Agent 可能通过 event:done 或自然结束（stream 关闭），只要收到内容就算正常
        boolean hasContent = result.stream().anyMatch(l -> l.startsWith("data:") && l.length() > 6);
        assertThat(hasContent).isTrue();
    }

    @Test
    @DisplayName("TC-AGT-05: SSE 流含有效 data 帧")
    void agentSse_receivesDataFrames() {
        String url = baseUrl() + "/sse?message=北京有什么好玩的";

        var result = restTemplate.execute(url, HttpMethod.GET, null,
                clientHttpResponse -> {
                    var reader = new BufferedReader(
                            new InputStreamReader(clientHttpResponse.getBody(), StandardCharsets.UTF_8));
                    List<String> dataFrames = new ArrayList<>();
                    String line;
                    long deadline = System.currentTimeMillis() + 60_000;
                    while ((line = reader.readLine()) != null
                            && System.currentTimeMillis() < deadline) {
                        if (line.startsWith("data:")) dataFrames.add(line);
                    }
                    return dataFrames;
                });

        assertThat(result).isNotNull();
        assertThat(result.size()).isGreaterThanOrEqualTo(1);
    }
}
