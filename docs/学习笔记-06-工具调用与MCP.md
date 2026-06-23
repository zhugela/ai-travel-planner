# 学习笔记 · 第 06 章：工具调用（Tool Calling）与 MCP

> 课程原型：**AI 恋爱大师** → 本项目适配：**AI 旅行规划助手**
> 前置章节：[第 05 章 · RAG 进阶与调优](./学习笔记-05-RAG进阶与调优.md)
> 下一章：第 07 章（待写）
> 深度思考：[深度思考练习手册](./深度思考练习手册.md)
> 项目仓库：`ai-travel-planner`（`com.yupi:yu-ai-agent`）
> 对应课程「本章重点」：工具调用 = 大模型原生的"借外部能力"机制；MCP = 标准化远程工具协议

**友情提示**：工具调用是 AI 从"静态问答"升级为"智能体 Agent"的关键——大模型只负责决策，工具执行完全在你的程序里。

---

## 本章目标

1. 理解「工具调用 = Tool Calling = Function Calling」是同一个概念
2. 区分**工具调用**（底层能力）和 **MCP**（标准化协议）的关系
3. 掌握 Spring AI 工具开发的 5 步流程：定义 → 注册 → 调用 → 执行 → 返回
4. 知道 6 大类工具能力（搜索 / 抓取 / 下载 / 终端 / 文件 / PDF）在旅游场景的价值

---

## 课程 → 本项目映射

| 课程（恋爱大师） | 你的项目（旅游规划助手） |
|------------------|--------------------------|
| 工具调用理论（10 张幻灯片） | 理解"决策 + 执行"分离 |
| Spring AI 工具开发 | 跟 RAG 集成，**目前未实现** |
| 6 大类工具：搜索 / 抓取 / 下载 / 终端 / 文件 / PDF | 适配旅游场景：实时天气 / 景点票价 / 行程 PDF |
| MCP 协议 | 课程不深入，**作为速查** |
| 本节作业 | 自行决定要不要实现 |

---

## 一、需求分析：RAG 的天花板

### 1.1 传统 RAG 只能"问答"

RAG 让 AI 能读私有知识库，但**没有"动手能力"**：
- 不知道今天北京天气
- 不能下载你想要的图片
- 不能保存到本地文件
- 不能生成可下载的 PDF

### 1.2 6 大类工具能力（可让 AI 升级为 Agent）

| 工具类型 | 作用 | 课程恋爱场景示例 | 你项目旅游场景示例 |
|---------|------|------------------|-------------------|
| **联网搜索** | 实时最新信息 | 查"上海小众情侣约会地" | 查"北京明天天气" / "故宫今日客流" |
| **网页抓取** | 读指定 URL 内容 | 抓 codefather.cn 案例 | 抓文旅局公告 |
| **资源下载** | 下图片/音频/视频 | 下载星空情侣壁纸 | 下载景点高清图 |
| **终端执行** | 跑 Python / JS 代码 | 生成聊天数据分析图 | 数据计算（汇率、距离） |
| **文件操作** | 读写本地文件 | 保存个人档案 | 导出 Markdown 行程单 |
| **PDF 生成** | 结构化内容 → PDF | 生成七夕方案 PDF | 生成可下载行程 PDF |

### 1.3 工具能"链式"组合

**核心亮点**：多个工具**串联**完成复杂任务。

```
联网搜"北京 3 天游" → 下载景点图片 → 整合成 Markdown → 生成 PDF → 保存到本地
   ↓           ↓            ↓              ↓            ↓
 搜索工具   下载工具    (LLM 自己整理)  PDF 工具    文件工具
   └───────────────── AI 全程自主调度 ──────────────────┘
```

**你项目价值** ⭐⭐⭐⭐：用户"帮我做一份北京 3 天攻略",AI 可以从搜索到 PDF 一条龙。

---

## 二、什么是工具调用（Tool Calling）

### 2.1 一句话定义

**工具调用 = 让大模型借用外部工具**完成它自身知识/能力做不到的任务。

类比人类：人光靠手脚办不成事，就用工具；大模型没有实时联网、读写文件、跑代码的能力，就调用外部工具补齐能力。

### 2.2 别名澄清

| 叫法 | 适用 | 备注 |
|------|------|------|
| **Tool Calling** | 通俗、泛指 | 教程优先用这个 |
| **Function Calling** | 技术、代码 | 早期 OpenAI 推广 |
| **Function Calling = Tool Calling** | ✅ 同一个概念 | Spring AI 官方文档明确标注 |

### 2.3 工具调用的 6 步标准流程

```
1. 工具定义   程序提前向大模型注册工具（写明功能 + 入参）
2. 工具选择   大模型读问题，判断要调哪个工具 + 填参数
3. 返回意图   大模型输出结构化报文：调XX工具，参数XXX
4. 工具执行   本地程序解析报文，本地运行工具
5. 结果返回   工具结果回传给大模型
6. 生成回答   大模型结合工具结果，输出最终自然语言
```

### 2.4 为什么不让大模型直接执行工具？—— **安全可控**

| 风险 | 如果大模型直接执行 |
|------|------------------|
| **权限不可控** | AI 擅自调用高危工具（拆数据库、删文件） |
| **业务管控缺失** | 无法加鉴权、过滤、拦截、日志 |
| **大模型服务器压力** | 耗资源的 IO/网络调用占 GPU 算力 |

**架构原则**：AI 只负责"思考要做什么"，程序牢牢握住"动手执行"。

---

## 三、Spring AI 工具开发

### 3.1 5 步实战流程

| 步骤 | 操作 | 关键 API |
|------|------|---------|
| 1 | 定义工具 | `@Tool` 注解 |
| 2 | 注册工具 | `defaultTools(...)` 或 `MethodToolCallback` |
| 3 | 调用 | `ChatClient.prompt().tools(...).call()` |
| 4 | 执行 | Spring AI 自动拦截 + 反射调用 |
| 5 | 返回 | 大模型结合工具结果生成回答 |

### 3.2 工具定义模式（2 种）

#### 模式 A · `@Tool` 注解（推荐，最简单）

```java
@Component
public class WeatherTools {

    @Tool(description = "查询指定城市的实时天气")
    public String getWeather(
        @ToolParam(description = "城市名,如'北京'") String city
    ) {
        // 调真实 API 或返回 mock
        return city + " 晴 25℃ 北风 3级";
    }
}
```

#### 模式 B · `MethodToolCallback`（更灵活）

```java
Method method = WeatherTools.class.getMethod("getWeather", String.class);
ToolCallback callback = MethodToolCallback.builder()
    .toolDefinition(ToolDefinition.builder(method)
        .description("查询天气")
        .build())
    .toolMethod(method)
    .build();
```

### 3.3 工具注册与使用

```java
ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultTools(new WeatherTools())  // ← 注册工具
    .build();

// 用户提问时,大模型自动判断要不要调工具
String answer = chatClient.prompt()
    .user("北京今天天气怎么样?")
    .call()
    .content();
// 流程:大模型看到工具 → 输出调 getWeather("北京") → 程序执行 → 返回"北京 晴 25℃"
//      → 大模型结合结果回复"北京今天晴天,25℃,..."
```

### 3.4 Spring AI 工具调用完整流程(官方)
Spring AI 框架帮我们封装好 6 大核心自动化能力,**不用手动写多轮交互**:

| 自动化能力 | Spring AI 帮我们做什么 |
|----------|--------------------|
| **1. 工具定义与注册** | `@Tool` + `@ToolParam` 注解自动生成工具描述、JSON Schema,Java 方法直接变 AI 工具 |
| **2. 工具调用请求转发** | 自动跟大模型通信,解析模型返回的工具调用指令;**原生支持多工具链式连续调用**(搜→爬→生成 PDF 这种串联) |
| **3. 工具执行分发** | 内置 `Dispatch Tool Call Requests` 调度器,自动匹配本地方法 + 反射执行 |
| **4. 工具结果处理** | 自动对象序列化、异常捕获,POJO/集合返回值自动转模型可读文本 |
| **5. 结果回传大模型** | 自动维护对话上下文,把工具结果包装成标准 TOOL 消息塞回对话流 |
| **6. 生成最终回答** | 整合工具返回数据,让大模型输出通顺自然语言 |

**完整链路**(以天气查询为例):

```
用户提问"北京今天天气怎么样?"
  ↓
ChatClient 携带工具定义请求大模型
  ↓
大模型判断:要调 getWeather("北京")
  ↓
返回 FunctionCall 指令给 Spring AI
  ↓
Spring AI 执行本地 WeatherTools.getWeather("北京")
  ↓
工具结果"北京 晴 25℃"回传给大模型
  ↓
大模型整合数据,输出"北京今天晴朗,气温 25℃,..."
  ↓
返回 String 给用户
```

### 3.5 两种工具定义模式对比

| 特性 | Methods 注解模式(推荐) | Functions 函数式模式(不推荐新项目) |
|------|----------------------|----------------------------------|
| 定义方式 | `@Tool` + `@ToolParam` 注解 | `@Bean` 返回 `Function<Request,Response>` |
| 代码复杂度 | **低,一行注解** | **高,需建实体类** |
| 支持数据类型 | Java 序列化类型 + 集合 + 基础类型 | 限制多,**不支持集合/Optional** |
| 类型转换 | 框架自动 | 大量手动配置 |
| 新项目推荐 | ✅ **首选** | ❌ 仅兼容旧代码 |

**注解式 vs 编程式**(Methods 模式内部两种):

| 写法 | 用法 | 场景 |
|------|------|------|
| **注解式** | 方法上加 `@Tool` / `@ToolParam` | 日常开发,99% 场景 |
| **编程式** | 运行时通过反射构建 `MethodToolCallback` | **动态加载**(从数据库/配置读工具元数据) |

### 3.6 工具类型限制(避坑)

**不能作为工具参数 / 返回值**:
- `Optional` — 不支持
- 异步类型:`CompletableFuture` / `Future`
- 响应式:`Mono` / `Flux` / `Flow`
- 函数式接口:`Function` / `Supplier` / `Consumer`

**返回值必须可序列化**(会转 JSON 给大模型)。

### 3.7 4 种工具绑定使用方式

| 方式 | 代码 | 生效范围 | 适用 |
|------|------|---------|------|
| **按需绑定** | `.tools(new WeatherTools())` | 单次对话 | 临时专用工具,隔离性强 |
| **全局默认** | `ChatClient.builder(chatModel).defaultTools(工具1, 工具2)` | 全部对话 | 系统通用工具(天气、翻译) |
| **ChatOptions 绑定** | 操作 `ToolCallingChatOptions` | 精细控制 | 需自定义模型底层参数 |
| **动态解析** | `ToolCallbackResolver` | 运行时按需 | 企业复杂,数据库读工具配置 |

**核心**:无论哪种绑定,Spring AI 自动接管完整工具调用闭环,开发者只写业务逻辑。

### 3.8 工具生态(社区现成插件)

**不用从零开发** — 复用社区现成工具:

| 类别 | 现成工具 |
|------|---------|
| 搜索 | 百度搜索、必应搜索 |
| 爬虫 | 网页爬虫 starter |
| 翻译 | 百度翻译、阿里翻译 |
| 地图 | 百度地图、高德地图 |

**学习技巧**:官方文档不全时,直接翻 GitHub 开源仓库源码,看完整实现。

**你项目现状**: 工具生态 0% 使用,未来可加百度地图(查景点坐标)、高德天气(实时数据)等。

---

## 四、6 大类工具开发（适配旅游场景）

### 4.1 文件操作

```java
@Tool(description = "把字符串内容写入指定文件")
public String writeToFile(
    @ToolParam(description = "文件路径") String path,
    @ToolParam(description = "文件内容") String content
) {
    try {
        Files.writeString(Path.of(path), content, StandardOpenOption.CREATE);
        return "写入成功:" + path;
    } catch (IOException e) {
        return "写入失败:" + e.getMessage();
    }
}
```

**你项目价值**：保存 AI 生成的行程单为 Markdown。

### 4.2 联网搜索

```java
@Tool(description = "联网搜索实时信息")
public String webSearch(
    @ToolParam(description = "搜索关键词") String query
) {
    // 接第三方 API:百度/谷歌/秘塔
    return "搜索结果...";
}
```

**你项目价值**：实时天气 / 实时景点客流 / 突发新闻。

### 4.3 网页抓取（jsoup）

```java
@Tool(description = "抓取指定 URL 的网页纯文本")
public String fetchUrl(
    @ToolParam(description = "URL") String url
) {
    return Jsoup.connect(url).get().body().text();
}
```

**你项目价值**：从文旅局 / 景点官网抓最新公告。

### 4.4 终端操作

```java
@Tool(description = "执行 shell 命令")
public String runCommand(
    @ToolParam(description = "命令") String command
) {
    // ⚠️ 危险:需加白名单/超时
    return new ProcessBuilder("bash", "-c", command)
        .redirectErrorStream(true).start().inputStream().toString();
}
```

**你项目价值** ⭐:危险,不加(AI 可能误删文件)。

### 4.5 资源下载

```java
@Tool(description = "下载 URL 指向的资源到本地")
public String downloadFile(
    @ToolParam(description = "URL") String url,
    @ToolParam(description = "本地保存路径") String path
) {
    // 用 Hutool HttpUtil.downloadFile
    HttpUtil.downloadFile(url, new File(path));
    return "已下载到 " + path;
}
```

**你项目价值**：下载景点高清图 / 行程模板。

### 4.6 PDF 生成（iText）

```java
@Tool(description = "把内容导出为 PDF 文件")
public String generatePdf(
    @ToolParam(description = "PDF 路径") String path,
    @ToolParam(description = "内容") String content
) {
    try (PdfWriter writer = new PdfWriter(path);
         PdfDocument pdf = new PdfDocument(writer);
         Document doc = new Document(pdf)) {
        doc.add(new Paragraph(content));
        return "已生成 PDF: " + path;
    } catch (Exception e) {
        return "生成失败:" + e.getMessage();
    }
}
```

**你项目价值** ⭐⭐⭐⭐:导出可下载的行程 PDF(用户最常用功能之一)。

---

## 五、工具进阶知识

### 5.1 工具底层数据结构

Spring AI 内部用 `ToolDefinition`(工具元数据) + `ToolCall`(调用请求) + `ToolResponse`(执行结果) 三种对象。

### 5.2 工具调用完整链路

```
用户问题
  ↓
ChatClient.prompt().user(q).tools(toolCallbacks).call()
  ↓
大模型看到工具列表,判断要不要用
  ↓ 输出 ToolCall(toolName, args)
Spring AI ToolCallingManager
  ↓ 根据 toolName 找 ToolCallback
MethodToolCallback.invoke()
  ↓ 反射调用 @Tool 方法
工具执行结果 ToolResponse
  ↓
大模型结合 ToolResponse 生成最终回答
  ↓
返回 String 给用户
```

### 5.3 工具上下文（Context）

Spring AI 用 `ToolContext` 传额外信息:
```java
@Tool(description = "查天气")
public String getWeather(String city, ToolContext context) {
    String userId = (String) context.getContext().get("userId");
    // 知道是谁在问
}
```

### 5.4 ToolCallingManager

Spring AI 中央调度器,负责:
- 拦截大模型的 ToolCall 输出
- 路由到对应 ToolCallback
- 处理异常 + 重试

### 5.5 异常处理

```java
try {
    ChatResponse response = chatClient.prompt().user(q).tools(...).call().chatResponse();
} catch (ToolExecutionException e) {
    // 工具执行失败
    log.error("工具执行失败", e);
    return "抱歉,工具出错了";
}
```

### 5.6 工具解析（Parsers）

Spring AI 解析大模型输出:
- OpenAI: 解析 `tool_calls` 字段
- Qwen: 解析 `function_call` 字段
- 不同模型格式不同 → 框架帮我们处理

### 5.7 可观测性

Spring AI 工具调用集成 Micrometer Observation:
- 每次工具调用都有 `observation`
- 可导出到 Prometheus / Zipkin
- 生产环境必备

### 5.8 MCP 是什么?(对比 Tool Calling)

| 维度 | Tool Calling | MCP |
|------|--------------|-----|
| 定位 | 大模型内置的"意图识别" | 跨系统**标准化通信协议**(JSON-RPC) |
| 工具位置 | 工具函数写在你的应用**本地** | 工具独立部署为**远程 MCP Server** |
| 适用场景 | 单体小项目 / 快速 Demo | 微服务 / 多外部系统 / 多模型混用 |
| 依赖关系 | **基础必备** | **可选扩展**(原 Tool Calling 不需要 MCP) |
| 你项目 | ❌ 未用 | ❌ 未用 |

**关键**:
- **不能**说"工具调用就是 MCP"
- 教程教的是**原生 Tool Calling,完全不需要 MCP**
- MCP 是"企业级远程工具集成"方案,不是入门必修

### 5.9 立即返回 vs 流式返回

| 模式 | 适合 |
|------|------|
| 立即返回 (`.call().content()`) | 普通问答 |
| 流式返回 (`.stream().content()`) | 长答案(实时显示) |

**你项目**:目前用立即返回。

---

## 六、扩展思路（你项目的可能扩展）

| 思路 | 实现难度 | 价值 |
|------|---------|------|
| 加"实时天气"工具 | ⭐⭐(调免费 API) | ⭐⭐⭐⭐ |
| 加"生成 PDF 行程单"工具 | ⭐⭐(用 iText) | ⭐⭐⭐⭐ |
| 加"网页抓取景点"工具 | ⭐⭐(用 jsoup) | ⭐⭐⭐ |
| 加"汇率/距离计算"工具 | ⭐(纯函数) | ⭐⭐ |
| 加 MCP Client 接外部工具服务 | ⭐⭐⭐ | ⭐⭐(过设计) |
| 加"终端命令"工具 | ⭐ | ⛔ 危险 |

**推荐先做**:"生成 PDF 行程单" + "实时天气" — 用户最直接受益的 2 个。

---

## 七、本节作业（你项目对标）

教程 Ch 06 作业:
> 1. 自行整理笔记,学会通过结构化的方式,理解工具调用的原理
> 2. 编写代码,实现 1-2 个工具(联网搜索 / PDF 生成 / 资源下载)
> 3. 工具组合:多工具链式完成复杂任务(比如"搜索+下载+PDF")
> 4. 了解 MCP 协议,作为知识储备

| 作业 | 你的完成度 | 备注 |
|------|----------|------|
| 1. 笔记 | **100%** | 本章(§一~§六) |
| 2. 实现 1-2 个工具 | ⏸️ 0% | **跳过**(本节作业,以后再做) |
| 3. 工具组合链式 | ⏸️ 0% | 同上 |
| 4. 了解 MCP | ✅ 100% | §五.8 |

**作业 1(笔记) 100%**,其他 0%(MCP 概念掌握)。**Ch 06 教程 100% 笔记化**,代码落地待以后。

---

## 八、本章小结

| 要点 | 一句话 |
|------|--------|
| 工具调用 | AI 借用外部工具补齐能力,大模型只决策,程序执行 |
| Tool Calling = Function Calling | 同义词,Spring AI 统一叫 Tool Calling |
| 6 步流程 | 定义 → 选择 → 意图 → 执行 → 返回 → 生成 |
| MCP | 企业级远程工具**通信标准**,跟 Tool Calling 互补不替代 |
| 安全原则 | 工具执行权永远在本地程序,不在大模型 |
| 你项目 Ch 06 | 0 工具实现,5 项工程实现在 4.1~4.6 节模板,以后直接抄 |

---

*最后更新：2026-06-22 · 完整踩坑见 [深度思考练习手册](./深度思考练习手册.md)*