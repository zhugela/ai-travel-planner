package com.yupi.aitravelplanner;

import com.yupi.aitravelplanner.config.TestMockConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestMockConfig.class)
class AiTravelPlannerApplicationTests {

    @Test
    void contextLoads() {
    }

}
