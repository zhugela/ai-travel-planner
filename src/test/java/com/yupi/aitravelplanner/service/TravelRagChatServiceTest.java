package com.yupi.aitravelplanner.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 云知识库版 RAG 问答测试
 *
 * 跟之前本地版的差异:
 *   - 不用 similaritySearch 预检
 *   - 云 advisor 内部检索 + 调模型,直接看返回值
 *   - 命中:返回**不是**兜底话术即可
 *   - 兜底:用通用问题验证也能正常返回
 */
@SpringBootTest
class TravelRagChatServiceTest {

    @Autowired
    private TravelRagChatService travelRagChatService;

    /**
     * 用例 1:命中
     * 问云知识库"旅游规划"里有内容的话题
     * 期望:模型返回基于知识库的回答(不能是兜底话术)
     */
    @Test
    void ragChat_hit() {
        // TODO: 改成你云知识库里确实有的问题(等你告诉我云端有什么)
        // 临时用一个旅游相关的通用问题,先验证链路通
        String question = "请介绍一个国内适合3天短途游的目的地";
        System.out.println("========== 测试 1: 命中场景 ==========");
        System.out.println("[Q] " + question);

        String answer = travelRagChatService.chatWithRag(question);

        System.out.println("[A] " + answer);
        assertNotNull(answer, "命中场景的 answer 不应为 null");
        assertTrue(!TravelRagChatService.NO_MATCH_REPLY.equals(answer),
                "命中场景不应返回兜底话术");
    }

    /**
     * 用例 2:通用问答
     * 问一个云知识库大概率没有的通用问题
     * 期望:模型给通用回答(云 advisor 不强制兜底,模型会自己答)
     */
    @Test
    void ragChat_general() {
        String question = "你好,请用一句话介绍你自己";
        System.out.println("========== 测试 2: 通用问答 ==========");
        System.out.println("[Q] " + question);

        String answer = travelRagChatService.chatWithRag(question);

        System.out.println("[A] " + answer);
        assertNotNull(answer);
    }

    /**
     * 用例 3:目的地推荐(扩展思路)
     * 给一个用户需求 → 模型按 prompt 规则列 3~5 个候选目的地
     */
    @Test
    void recommendDestinations() {
        String need = "3 天短途,带老人,不想太累";
        System.out.println("========== 测试 3: 目的地推荐 ==========");
        System.out.println("[需求] " + need);

        String result = travelRagChatService.recommendDestinations(need);

        System.out.println("[推荐]\n" + result);
        assertNotNull(result);
        assertTrue(result.length() > 5, "推荐结果应有内容");
    }
}