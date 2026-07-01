package com.yupi.aitravelplanner.test.agent;

import com.yupi.aitravelplanner.agent.YuTravelAgent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class BaseAgentTest {

    @Autowired
    private YuTravelAgent yuTravelAgent;

    @Test
    public void testFullReActTravelPlan() {
        // 1. 用户提问
        String userQuestion = "帮我规划北京3日游";  // TODO: 填写测试问题

        // 2. 调用 run
        String result = yuTravelAgent.run(userQuestion);

        // 3. 打印日志
        System.out.println("===== Agent 执行结果 =====");
        System.out.println(result);

        // 4. 基础断言
        assert result != null;
        assert !result.isEmpty();
        // TODO: 补充更多断言
    }
}
