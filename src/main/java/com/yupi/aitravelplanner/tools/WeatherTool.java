package com.yupi.aitravelplanner.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 天气查询工具（自研）
 * <p>
 * 基于 wttr.in 公共 API，免 key、无需注册。
 * 数据源：https://wttr.in/{city}?format=j1
 * <p>
 * 关键坑：wttr.in 强制要求 User-Agent header，否则返回 403。
 * 这里通过 Hutool 的 HttpRequest 显式设置 UA。
 */
@Component
@Slf4j
public class WeatherTool {

    private static final String WTTR_BASE = "https://wttr.in/";

    private static final String USER_AGENT = "Mozilla/5.0 (compatible; AiTravelPlanner/1.0)";

    /**
     * 查询指定城市的实时天气
     *
     * @param city 城市名，支持中文或英文，如"杭州"、"Hangzhou"、"Tokyo"
     * @return 格式化天气信息，例：杭州:晴,15°C,湿度 60%,东北风 12km/h
     */
    @Tool(description = "查询指定城市的实时天气。基于 wttr.in 公共 API,免 key。参数为城市名,如'杭州'、'Beijing'")
    public String getWeather(
            @ToolParam(description = "城市名,支持中英文,例如:杭州、北京、Tokyo") String city
    ) {
        if (city == null || city.trim().isEmpty()) {
            return "错误:城市名不能为空";
        }

        try {
            String encodedCity = URLEncoder.encode(city.trim(), StandardCharsets.UTF_8);
            String url = WTTR_BASE + encodedCity + "?format=j1";

            log.info("查询天气: {}", city);

            String json = HttpUtil.createGet(url)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .timeout(10_000)
                    .execute()
                    .body();

            JSONObject obj = JSONUtil.parseObj(json);

            JSONArray currentArr = obj.getJSONArray("current_condition");
            if (currentArr == null || currentArr.isEmpty()) {
                return "未获取到 " + city + " 的天气数据";
            }
            JSONObject current = currentArr.getJSONObject(0);

            String tempC = current.getStr("temp_C", "?");
            String humidity = current.getStr("humidity", "?");

            String desc;
            JSONArray descArr = current.getJSONArray("weatherDesc");
            if (descArr != null && !descArr.isEmpty()) {
                desc = descArr.getJSONObject(0).getStr("value", "未知");
            } else {
                desc = "未知";
            }

            String windSpeed = current.getStr("windspeedKmph", "?");
            String windDir = current.getStr("winddir16Point", "?");

            String areaName = city;
            JSONArray nearestArr = obj.getJSONArray("nearest_area");
            if (nearestArr != null && !nearestArr.isEmpty()) {
                JSONObject area = nearestArr.getJSONObject(0);
                JSONArray nameArr = area.getJSONArray("areaName");
                if (nameArr != null && !nameArr.isEmpty()) {
                    areaName = nameArr.getJSONObject(0).getStr("value", city);
                }
            }

            return String.format(
                    "%s:%s,%s°C,湿度 %s%%,风速 %skm/h %s",
                    areaName, desc, tempC, humidity, windSpeed, windDir
            );

        } catch (Exception e) {
            log.error("天气查询失败: {}", city, e);
            return "天气查询失败: " + e.getMessage();
        }
    }
}
