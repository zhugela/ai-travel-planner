package com.yupi.aitravelplanner;

import com.yupi.aitravelplanner.app.TravelApp;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class TravelAppTest {

    @Resource
    private TravelApp travelApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();

        String answer1 = travelApp.doChat("你好，我想去杭州玩3天", chatId);
        assertNotNull(answer1);

        String answer2 = travelApp.doChat("预算2000元，2个人，喜欢美食", chatId);
        assertNotNull(answer2);

        String answer3 = travelApp.doChat("我刚才说想去哪里？帮我回忆一下", chatId);
        assertNotNull(answer3);

        System.out.println("第三轮回复: " + answer3);
    }

    @Test
    void testChatWithTravelReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是小明，我想去杭州玩3天，预算2000元，喜欢美食";
        TravelApp.TravelReport report = travelApp.doChatWithTravelReport(message, chatId);
        assertNotNull(report);
        assertNotNull(report.title());
        assertNotNull(report.suggestions());
        System.out.println("报告标题: " + report.title());
        System.out.println("建议列表: " + report.suggestions());
    }
}