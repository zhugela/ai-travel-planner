package com.yupi.aitravelplanner.demo.prompt;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step 2：PromptTemplate 最小 Demo（render 占位符）
 */
@ActiveProfiles("test")
class PromptTemplateDemoTest {

    @Test
    void renderWithDestination() {
        String template = "请为 {destination} 规划 {days} 天行程，预算 {budget} 元，偏好 {preferences}。";
        PromptTemplate promptTemplate = new PromptTemplate(template);

        String result = promptTemplate.render(Map.of(
                "destination", "杭州",
                "days", "3",
                "budget", "2000",
                "preferences", "美食"
        ));

        System.out.println(result);
        assertTrue(result.contains("杭州"));
        assertTrue(result.contains("2000"));
    }
}
