package com.yupi.aitravelplanner.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherToolTest {

    @Test
    void getWeather_validCity() {
        WeatherTool tool = new WeatherTool();
        String result = tool.getWeather("Hangzhou");

        assertNotNull(result);
        System.out.println("getWeather('Hangzhou') = " + result);

        assertTrue(
                result.contains("°C") || result.contains("天气") || result.contains("杭州"),
                "返回应包含温度/天气/城市标识,实际:" + result
        );
    }

    @Test
    void getWeather_emptyCity() {
        WeatherTool tool = new WeatherTool();
        String result = tool.getWeather("");

        assertNotNull(result);
        assertTrue(result.contains("错误") || result.contains("不能为空"));
    }

    @Test
    void getWeather_nullCity() {
        WeatherTool tool = new WeatherTool();
        String result = tool.getWeather(null);

        assertNotNull(result);
        assertTrue(result.contains("错误") || result.contains("不能为空"));
    }
}
