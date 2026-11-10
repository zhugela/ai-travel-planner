package com.yupi.aitravelplanner;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeChatAutoConfiguration;
import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeEmbeddingAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类。
 *
 * Chat 走 DeepSeek(OpenAI 兼容),百炼账户欠费期间排除其 Chat/Embedding 自动配置,
 * 否则会额外注册 DashScopeChatModel Bean 抢占注入。需要切回百炼时删掉 exclude 即可。
 */
@SpringBootApplication(exclude = {
        DashScopeChatAutoConfiguration.class,
        DashScopeEmbeddingAutoConfiguration.class
})
public class AiTravelPlannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiTravelPlannerApplication.class, args);
    }

}
