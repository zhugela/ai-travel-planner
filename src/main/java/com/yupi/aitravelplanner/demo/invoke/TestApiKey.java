package com.yupi.aitravelplanner.demo.invoke;

/**
 * 仅用于测试获取 API Key。
 *
 * 密钥不入库:从环境变量 DASHSCOPE_API_KEY 读取,
 * 未设置时为空串(调用方自行处理),不会把真实密钥写进代码。
 */
public interface TestApiKey {

    String API_KEY = System.getenv().getOrDefault("DASHSCOPE_API_KEY", "");
}
