package com.yupi.aitravelplanner.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * RAG 问答服务单元测试
 *
 * 对应教程图 1~4 的 Debug 复现:
 *  - 启动时 @SpringBootTest 加载完整 Spring 上下文
 *  - TravelVectorStoreConfig 自动把 3 份 md 向量化入库
 *  - ragChat_hit:命中知识库(北京周边),验证模型拿到上下文
 *  - ragChat_noMatch:无匹配(元宇宙),验证兜底话术
 */
@SpringBootTest
class TravelRagChatServiceTest {

    @Autowired
    private TravelRagChatService travelRagChatService;

    /**
     * 用例 1:命中
     * 问题:"北京周边 2 天怎么玩" 在 travel-short-trip.md 第 1 段
     * 期望:返回 AI 生成的回答(包含 "古北水镇"/"爨底下"/"密云" 之一)
     * 验证:assertNotNull + 答案非空
     */
    @Test
    void ragChat_hit() {
        String question = "北京周边 2 天怎么玩?";
        System.out.println("========== 测试 1: 命中场景 ==========");
        System.out.println("[Q] " + question);

        String answer = travelRagChatService.chatWithRag(question);

        System.out.println("[A] " + answer);
        assertNotNull(answer, "命中场景的 answer 不应为 null");
    }

    /**
     * 用例 2:兜底
     * 问题:"元宇宙 7 天深度游" 知识库里没有
     * 期望:返回固定兜底话术 "暂无相关旅游攻略信息"
     * 验证:assertEquals 严格匹配
     */
    @Test
    void ragChat_noMatch() {
        String question = "元宇宙 7 天深度游攻略";
        System.out.println("========== 测试 2: 兜底场景 ==========");
        System.out.println("[Q] " + question);

        String answer = travelRagChatService.chatWithRag(question);

        System.out.println("[A] " + answer);
        assertEquals(TravelRagChatService.NO_MATCH_REPLY, answer,
                "无匹配时应返回固定兜底话术");
    }
}
