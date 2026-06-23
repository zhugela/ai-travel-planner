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

### 3.9 returnDirect 立即返回（减少一轮模型交互）

**核心作用**：工具执行完后，结果**直接返回给用户**，不再回传给大模型再生成一次回答。

**节省**：少一轮大模型交互 = 省 token + 省时间。

```java
@Tool(returnDirect = true, description = "导出用户订单为Excel")
public String exportOrders(String userId) {
    // 执行导出
    return "导出成功,文件路径: /tmp/orders.xlsx";
    // ↑ 这个字符串直接返回给用户,大模型不会再"润色"一遍
}
```

**适用场景**（工具结果就是最终结果）：
- 文件操作（保存/读取路径）
- 数据库 CRUD（操作成功提示）
- 批量数据处理（处理条数）
- PDF / Excel 生成（返回下载路径）

**不适用场景**：
- 联网搜索（需要大模型整理结果）
- 天气查询（需要大模型组织语言）
- 任何需要"AI 总结/翻译/解释"的场景

### 3.10 工具统一注册类（集中管理）

**痛点**：工具分散在各个类里，ChatClient 里一个个 `.defaultTools(工具1, 工具2, ...)` 写起来乱。

**解法**：用 `@Configuration` 配置类集中注册，统一转成 `ToolCallback[]` 数组。

```java
@Configuration
public class ToolRegistration {

    @Value("${search-api.api-key}")
    private String searchApiKey;

    @Bean
    public ToolCallback[] allTools() {
        // 1. 手动创建工具实例（需要注入配置的）
        WebSearchTool webSearchTool = new WebSearchTool();
        webSearchTool.setApiKey(searchApiKey);

        // 2. Spring 管理的工具（直接 new 或从容器取）
        FileOperationTool fileOperationTool = new FileOperationTool();
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        TerminalTool terminalTool = new TerminalTool();
        DownloadTool downloadTool = new DownloadTool();
        PdfGenerationTool pdfGenerationTool = new PdfGenerationTool();

        // 3. 统一转为 ToolCallback 数组
        return ToolCallbacks.from(
            webSearchTool,
            fileOperationTool,
            webScrapingTool,
            terminalTool,
            downloadTool,
            pdfGenerationTool
        );
    }
}
```

**业务层使用**：

```java
@Resource
private ToolCallback[] allTools;

// ChatClient 绑定全部工具
ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultAdvisors(
        MessageChatMemoryAdvisor.builder(chatMemory).build(),
        new MyLoggerAdvisor()
    )
    .defaultTools(allTools)  // ← 一行注入全部工具
    .build();
```

**设计模式（隐含 4 种）**：

| 模式 | 体现 |
|------|------|
| **工厂模式** | `ToolCallbacks.from()` 工厂方法批量创建 ToolCallback |
| **依赖注入** | `@Bean` + `@Value`，Spring 容器管理工具实例 |
| **注册模式** | 所有工具集中注册到一个地方，统一管理 |
| **适配器模式** | 把普通 Java 方法适配成 ToolCallback 接口 |

**好处**：新增/删除工具，只改 `ToolRegistration` 一个类。

---

## 四、自研主流工具完整开发（6 大工具完整版）

> 本节是教程 3 个真实工具的**完整代码 + 工程规范 + 测试指引**,
> 你写下时直接拷贝改 @Tool 描述文字即可。

### 4.0 开发前置规范（通用）

**什么情况下才自研工具**：
- 社区无现成可用工具时才自研
- AI 原生能完成的逻辑**不要封装工具**（每调用一次工具 = 多一轮大模型交互，损耗性能）

**项目统一规范**：

| 规范 | 做法 | 原因 |
|------|------|------|
| 包分层 | `tools/` 包存放工具类,`constant/` 存放常量 | 结构清晰 |
| 返回值 | 统一返回 `String` | 方便序列化传给大模型 |
| 描述清晰 | `@Tool`、`@ToolParam` 的描述文字必须清晰 | 决定大模型「何时调用该工具」 |
| 异常处理 | 全程 try-catch,返回错误文本 | 不让异常污染对话流 |
| 密钥/路径 | 放入配置文件 / 环境变量,不进代码 | 安全 |

### 4.1 文件操作工具（FileOperationTool）

**安全隔离设计**:统一隔离到项目根目录 `/tmp/file` 下,防止乱读写系统文件。

**常量接口 `constant/FileConstant.java`**:
```java
public interface FileConstant {
    String FILE_SAVE_DIR = System.getProperty("user.dir") + "/tmp/file";
}
```

**工具类 `tools/FileOperationTool.java`**:
```java
@Component
@Slf4j
public class FileOperationTool {

    private final String baseDir = FileConstant.FILE_SAVE_DIR;

    @Tool(description = "读取指定文件的文本内容,参数为文件名,如'北京攻略.md'")
    public String readFile(
        @ToolParam(description = "文件名称,如'北京攻略.md'") String fileName
    ) {
        try {
            File file = new File(baseDir, fileName);
            if (!file.exists()) {
                return "文件不存在";
            }
            String content = cn.hutool.core.io.FileUtil.readUtf8String(file);
            return content;
        } catch (Exception e) {
            return "读取文件失败: " + e.getMessage();
        }
    }

    @Tool(description = "把文本内容写入到指定文件,成功返回文件路径")
    public String writeFile(
        @ToolParam(description = "文件名称,如'东京之旅.md'") String fileName,
        @ToolParam(description = "待写入的文本内容") String content
    ) {
        try {
            File dir = new File(baseDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, fileName);
            cn.hutool.core.io.FileUtil.writeUtf8String(content, file);
            return "文件已保存到: " + file.getAbsolutePath();
        } catch (Exception e) {
            return "文件保存失败: " + e.getMessage();
        }
    }
}
```

**单元测试 `FileOperationToolTest`**:
```java
@SpringBootTest
class FileOperationToolTest {

    @Resource
    private FileOperationTool fileOperationTool;

    @Test
    void writeFile() {
        String result = fileOperationTool.writeFile("test.txt", "Hello World");
        System.out.println(result);
        assertNotNull(result);
    }

    @Test
    void readFile() {
        String result = fileOperationTool.readFile("test.txt");
        System.out.println(result);
        assertNotNull(result);
    }
}
```

**`.gitignore` 追加**（防止本地文件泄露）:
```text
# 工具文件操作隔离目录
/tmp/file/
```

### 4.2 联网搜索工具（WebSearchTool）

**实现思路**:接第三方 SearchAPI(支持百度/谷歌),搜关键词→5 条标题+链接+摘要。

**配置 `application.yml`**:
```yaml
search-api:
  api-key: ${SEARCH_API_KEY}
```

**工具类 `tools/WebSearchTool.java`**:
```java
@Component
@Slf4j
public class WebSearchTool {

    @Value("${search-api.api-key}")
    private String apiKey;

    private static final String SEARCH_URL =
        "https://serpapi.com/search?q={query}&api_key={apiKey}&engine=baidu";

    @Tool(description = "联网搜索实时信息,参数为搜索关键词,如'北京今日天气'")
    public String searchWeb(
        @ToolParam(description = "搜索关键词") String query
    ) {
        try {
            String url = StrUtil.format(SEARCH_URL,
                cn.hutool.core.map.MapUtil.of("query", query, "apiKey", apiKey));
            String json = HttpUtil.get(url);
            JSONObject obj = JSONUtil.parseObj(json);
            JSONArray results = obj.getJSONArray("organic_results");

            if (results == null || results.isEmpty()) {
                return "未找到相关搜索结果";
            }

            StringBuilder sb = new StringBuilder("搜索结果如下:\n");
            int limit = Math.min(results.size(), 5);
            for (int i = 0; i < limit; i++) {
                JSONObject item = results.getJSONObject(i);
                sb.append("标题: ").append(item.getStr("title")).append("\n");
                sb.append("链接: ").append(item.getStr("link")).append("\n");
                sb.append("摘要: ").append(item.getStr("snippet")).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "联网搜索失败: " + e.getMessage();
        }
    }
}
```

**单元测试 `WebSearchToolTest`**:
```java
@SpringBootTest
class WebSearchToolTest {

    @Value("${search-api.api-key}")
    private String apiKey;

    @Test
    void searchWeb() {
        WebSearchTool tool = new WebSearchTool();
        // 因为工具类依赖 @Value,测试里用 setter 注入
        // 或用 @Autowired 直接注入(推荐)
        String result = tool.searchWeb("北京旅游攻略");
        System.out.println(result);
        assertNotNull(result);
    }
}
```

**注意**:测试前先申请 SerpAPI 或类似平台密钥。

### 4.3 网页抓取工具（WebScrapingTool）

**依赖**:你工程 `pom.xml` 已有 `jsoup 1.19.1`,直接写工具。

**工具类 `tools/WebScrapingTool.java`**:
```java
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

@Component
@Slf4j
public class WebScrapingTool {

    @Tool(description = "抓取指定 URL 的网页纯文本内容,用于获取网页信息")
    public String scrapeWebPage(
        @ToolParam(description = "要抓取的网页 URL") String url
    ) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();
            // 只返回 body 纯文本,减少 token 消耗
            return doc.body().text();
        } catch (IOException e) {
            return "网页抓取失败: " + e.getMessage();
        }
    }
}
```

**单元测试 `WebScrapingToolTest`**:
```java
@SpringBootTest
class WebScrapingToolTest {

    @Resource
    private WebScrapingTool webScrapingTool;

    @Test
    void scrapeWebPage() {
        String result = webScrapingTool.scrapeWebPage("https://codefather.cn");
        System.out.println(result);
        assertNotNull(result);
    }
}
```

### 4.4 终端操作工具（TerminalTool）

**⚠️ 生产环境风险**：命令注入风险极高，必须加白名单校验！

**常量追加到 `FileConstant.java`：
```java
String TERMINAL_ALLOWED_COMMANDS = "python3,node,java"; // 白名单命令
```

**工具类 `tools/TerminalTool.java`：
```java
@Component
@Slf4j
public class TerminalTool {

    @Tool(description = "执行终端命令,返回命令输出结果")
    public String runCommand(
        @ToolParam(description = "要执行的命令,如'python3 script.py") String command
    ) {
        try {
            // 1. 安全校验:简单白名单校验(生产必须做更严格)
            String cmdPrefix = command.split(" ")[0];
            if (!isAllowed(cmdPrefix)) {
                return "命令不被允许,仅支持: " + FileConstant.TERMINAL_ALLOWED_COMMANDS;
            }

            // 2. 区分操作系统
            boolean isWindows = System.getProperty("os.name")
                .toLowerCase().contains("win");

            ProcessBuilder pb;
            if (isWindows) {
                pb = new ProcessBuilder("cmd", "/c", command);
            } else {
                pb = new ProcessBuilder("bash", "-c", command);
            }

            // 3. 合并标准输出和错误输出
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 4. 读取输出(设置超时防卡死)
            String output = new String(
                process.getInputStream().readAllBytes());

            // 5. 限制返回长度,防 token 爆炸
            if (output.length() > 2000) {
                output = output.substring(0, 2000) + "\n...(已截断)";
            }

            return output;
        } catch (Exception e) {
            return "命令执行失败: " + e.getMessage();
        }
    }

    private boolean isAllowed(String cmd) {
        // 简单白名单校验
        return FileConstant.TERMINAL_ALLOWED_COMMANDS.contains(cmd);
    }
}
```

**单元测试 `TerminalToolTest`：
```java
@SpringBootTest
class TerminalToolTest {

    @Resource
    private TerminalTool terminalTool;

    @Test
    void runCommand() {
        String result = terminalTool.runCommand("echo hello");
        System.out.println(result);
        assertNotNull(result);
    }
}
```

**生产安全建议**：
- ✅ 白名单校验（只允许特定命令）
- ✅ 超时机制（防止命令卡死）
- ✅ 输出长度限制（防 token 爆炸）
- ❌ 绝对不能允许 `rm`、`sudo`、`cat /etc/passwd` 等危险命令

### 4.5 资源下载工具（DownloadTool）

**基于 Hutool HttpUtil**，统一隔离到 `/tmp/download` 目录。

**常量追加到 `FileConstant.java`**：
```java
String DOWNLOAD_SAVE_DIR = System.getProperty("user.dir") + "/tmp/download";
```

**工具类 `tools/DownloadTool.java`**：
```java
@Component
@Slf4j
public class DownloadTool {

    private final String saveDir = FileConstant.DOWNLOAD_SAVE_DIR;

    @Tool(description = "下载网络资源到本地,参数为资源URL,成功返回本地文件路径")
    public String downloadResource(
        @ToolParam(description = "要下载的资源URL") String url
    ) {
        try {
            // 1. 确保目录存在
            File dir = new File(saveDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 2. 从 URL 提取文件名
            String fileName = extractFileName(url);
            File targetFile = new File(dir, fileName);

            // 3. 下载
            cn.hutool.http.HttpUtil.downloadFile(url, targetFile);

            return "下载成功,文件路径: " + targetFile.getAbsolutePath();
        } catch (Exception e) {
            return "下载失败: " + e.getMessage();
        }
    }

    private String extractFileName(String url) {
        // 从 URL 中提取文件名,取不到就用时间戳
        try {
            int lastSlash = url.lastIndexOf("/");
            if (lastSlash > 0 && lastSlash < url.length() - 1) {
                return url.substring(lastSlash + 1).split("\\?")[0];
            }
        } catch (Exception e) {
                // ignore
            }
            return "download_" + System.currentTimeMillis();
        }
    }
```

**单元测试 `DownloadToolTest`：
```java
@SpringBootTest
class DownloadToolTest {

    @Resource
    private DownloadTool downloadTool;

    @Test
    void downloadResource() {
        String result = downloadTool.downloadResource(
            "https://example.com/image.jpg");
        System.out.println(result);
        assertNotNull(result);
    }
}
```

**`.gitignore` 追加**：
```text
/tmp/download/
```

### 4.6 PDF 生成工具（PdfGenerationTool）

**基于 iText 9**，内置中文字体解决乱码问题。

**pom.xml 加依赖**：
```xml
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itext</artifactId>
    <version>9.0.0</version>
</dependency>
```

**常量追加到 `FileConstant.java`**：
```java
String PDF_SAVE_DIR = System.getProperty("user.dir") + "/tmp/pdf";
```

**工具类 `tools/PdfGenerationTool.java`**：
```java
@Component
@Slf4j
public class PdfGenerationTool {

    private final String saveDir = FileConstant.PDF_SAVE_DIR;

    @Tool(returnDirect = true, description = "把文本内容生成为PDF文件,返回PDF文件路径")
    public String generatePdf(
        @ToolParam(description = "PDF文件名,如'北京攻略.pdf'") String fileName,
        @ToolParam(description = "PDF内容文本") String content
    ) {
        try {
            // 1. 确保目录
            File dir = new File(saveDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File pdfFile = new File(dir, fileName);

            // 2. 生成 PDF
            try (FileOutputStream fos = new FileOutputStream(pdfFile);
                 PdfWriter writer = new PdfWriter(fos);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {

                // 3. 中文字体支持(解决中文乱码)
                // 方式一:用系统自带字体
                Font font = PdfFontFactory.createFont(
                    "C:/Windows/Fonts/simhei.ttf",
                    PdfEncodings.IDENTITY_H);
                document.setFont(font);

                // 4. 写入内容(按换行符分段)
                String[] paragraphs = content.split("\n");
                for (String para : paragraphs) {
                    if (!para.trim().isEmpty()) {
                        document.add(new Paragraph(para));
                    }
                }
            }

            return "PDF生成成功: " + pdfFile.getAbsolutePath();
        } catch (Exception e) {
            return "PDF生成失败: " + e.getMessage();
        }
    }
}
```

**单元测试 `PdfGenerationToolTest`：
```java
@SpringBootTest
class PdfGenerationToolTest {

    @Resource
    private PdfGenerationTool pdfGenerationTool;

    @Test
    void generatePdf() {
        String content = "北京三日游攻略\n\n第一天:天安门 + 故宫\n第二天:长城\n第三天:颐和园";
        String result = pdfGenerationTool.generatePdf("北京攻略.pdf", content);
        System.out.println(result);
        assertNotNull(result);
    }
}
```

**生产优化方向**：
- 生成后上传 OSS（对象存储），返回公网访问链接
- 支持更丰富的排版（标题、列表、图片）
- 支持模板渲染

**`.gitignore` 追加**：
```text
/tmp/pdf/
```

### 4.7 工具组合链式调用（搜索 → 下载 → 生成 PDF）



```java
// RAG Service 里注册所有工具
String answer = chatClient.prompt()
    .user("请帮我搜一下北京5天游攻略,下载一些图片,然后生成PDF给我")
    .tools(new WebSearchTool(), new WebScrapingTool(),
           new FileOperationTool(), new PdfGenerationTool())
    .call()
    .content();
```

**AI 内部自动调度**:
```
联网搜"北京5天游" → 抓取攻略网页 → 存为 .md 文件 → 生成 PDF
  ↓                ↓               ↓              ↓
 搜索工具         爬虫工具        文件操作        PDF 工具
  └──────────────── AI 自动链式调用 ──────────────┘
```

### 4.6 完整开发流程模板(自研通用)

1. **规划**:判断是否必须自研,定义工具能力、入参、返回格式
2. **分层**:常量→ `constant/`,工具类→ `tools/`,隔离文件/网络资源
3. **封装**:`@Tool` + `@ToolParam` 注解,统一返回 `String`,全局 try-catch
4. **配置**:密钥、路径放进配置文件,不硬编码
5. **测试**:先跑单元测试验证工具逻辑,再接 ChatClient
6. **优化**:过滤第三方 API 多余字段,只保留大模型需要的核心数据

---

## 五、工具进阶核心 API

### 5.1 底层核心结构

#### 5.1.1 ToolCallback — 所有工具的顶层接口

`ToolCallback` 是 Spring AI 中所有工具的**统一抽象**，定义了工具的元数据和执行能力：

```java
public interface ToolCallback {
    // 1. 给AI看的工具元数据(名称、描述、JSON参数Schema)
    ToolDefinition getToolDefinition();

    // 2. 控制属性,核心是 returnDirect
    ToolMetadata getToolMetadata();

    // 3. 执行工具(不带上下文)
    String call(String toolInput);

    // 4. 执行工具(带 ToolContext 上下文)
    default String call(String toolInput, ToolContext toolContext) {
        return call(toolInput);
    }
}
```

**关键方法解析**：

| 方法 | 作用 | 谁会调用 |
|------|------|---------|
| `getToolDefinition()` | 返回工具元数据（name、description、inputSchema） | 框架请求大模型时，把工具列表发给 AI |
| `getToolMetadata()` | 返回控制属性（`returnDirect` 等） | 框架判断是否直接返回结果 |
| `call(...)` | 实际执行工具逻辑 | `ToolCallingManager` 调度执行 |

#### 5.1.2 ToolDefinition — 工具定义

包含工具的完整描述信息，**AI 通过这个判断"要不要调这个工具"**：

```java
public class ToolDefinition {
    private String name;                    // 工具名
    private String description;             // 工具描述(AI 据此判断是否调用)
    private JsonSchema inputSchema;         // 入参 JSON Schema
    private String outputSchema;            // 出参描述(可选)
}
```

**注解模式下**，框架自动扫描 `@Tool` + `@ToolParam` 注解，**自动生成 JSON Schema**，无需手写。

#### 5.1.3 ToolContext — 工具上下文

底层是 `Map<String, Object>`，存放**不会传给大模型**的私有数据，用于鉴权、链路追踪等：

```java
public interface ToolContext {
    Map<String, Object> getContext();
}
```

**典型用途**：
- 登录用户 ID、角色、权限
- 请求 ID（链路追踪）
- 租户 ID（多租户隔离）
- Token 等敏感信息

**代码示例**：
```java
@Tool(description = "查询用户订单")
public String getOrders(
    @ToolParam(description = "页数") int page,
    ToolContext context  // ← 框架自动注入
) {
    String userId = (String) context.getContext().get("userId");
    // 只能查自己的订单,鉴权
    return orderService.queryByUserId(userId, page);
}
```

> ⚠️ **关键特性**：ToolContext 的内容**不会发送给大模型**，只在本地工具执行时可用，安全。

### 5.2 调度核心：ToolCallingManager

`ToolCallingManager` 是全局工具调度管理器，负责工具调用的全流程控制。

**两个核心方法**：

```java
public interface ToolCallingManager {
    // 1. 解析全部工具定义(发给大模型看)
    List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions options);

    // 2. 匹配并执行 AI 下发的工具调用
    List<ToolExecutionResult> executeToolCalls(
        List<ToolCall> toolCalls,
        ToolCallingChatOptions options
    );
}
```

**内置默认实现**：`DefaultToolCallingManager`

**核心职责**：
- 从 ToolCallback 列表中提取 ToolDefinition
- 解析大模型返回的 ToolCall 请求
- 根据 toolName 匹配对应的 ToolCallback
- 调用 ToolCallback.call() 执行工具
- 处理执行结果和异常

**自定义替换**：可以自己实现 `ToolCallingManager` 接口，注册为 Spring Bean 替换默认实现：
```java
@Bean
public ToolCallingManager myToolCallingManager() {
    return new MyCustomToolCallingManager();
}
```

### 5.3 两种工具执行模式

#### 模式 A：框架自动控制（默认，日常开发）

**默认行为**：Spring AI 自动循环处理工具调用，直到大模型输出纯文本回答。

```
用户提问
  ↓
请求大模型(带工具定义)
  ↓
大模型返回 ToolCall? ──否──→ 返回纯文本答案
  ↓ 是
执行工具
  ↓
工具结果回传大模型
  ↓
再次请求大模型
  ↓
(循环...)
```

**优点**：业务代码极简，一行 `.call()` 搞定全部
**适用**：99% 日常开发场景

#### 模式 B：用户手动控制（复杂定制场景）

关闭自动循环，手动写 while 循环处理工具调用，可插入自定义逻辑。

**配置关闭自动执行**：
```java
// 方式一:ChatOptions 配置
ToolCallingChatOptions options = ToolCallingChatOptions.builder()
    .internalToolExecutionEnabled(false)  // ← 关闭自动循环
    .build();

// 方式二:application.yml 全局配置
// spring.ai.chat.tool-calling.internal-tool-execution-enabled=false
```

**手动控制示例**：
```java
// 手动循环处理工具调用
String userMessage = "帮我搜北京天气并生成PDF";
ChatResponse response = chatClient.prompt()
    .user(userMessage)
    .options(ToolCallingChatOptions.builder()
        .internalToolExecutionEnabled(false)
        .build())
    .tools(allTools)
    .call()
    .chatResponse();

while (hasToolCalls(response)) {
    // 1. 提取工具调用
    List<ToolCall> toolCalls = response.getResults().get(0)
        .getOutput().getToolCalls();

    // 2. 自定义前置逻辑(鉴权、日志、限流...)
    log.info("准备执行工具: {}", toolCalls);

    // 3. 执行工具
    List<ToolExecutionResult> results = toolCallingManager
        .executeToolCalls(toolCalls, options);

    // 4. 自定义后置逻辑(监控、告警、结果过滤...)
    log.info("工具执行结果: {}", results);

    // 5. 结果回传大模型,继续下一轮
    response = chatClient.prompt()
        .user(userMessage)
        .toolResults(results)
        .call()
        .chatResponse();
}

String finalAnswer = response.getResults().get(0).getOutput().getText();
```

**适用场景**：
- 需要自定义工具执行前/后逻辑
- 需要重试、熔断、降级
- 需要复杂的分支判断
- 需要精细化监控埋点

### 5.4 异常处理体系

Spring AI 提供 `ToolExecutionExceptionProcessor` 统一处理工具报错。

#### 三种策略

| 策略 | 配置 | 行为 | 适用 |
|------|------|------|------|
| **默认策略** | `alwaysThrow=false` | 异常文本返回给 AI，模型可自行重试 | 日常开发，AI 能自我纠错 |
| **直接抛出** | `alwaysThrow=true` | 异常直接抛出，中断整个对话 | 关键业务，不容错 |
| **自定义处理器** | 实现接口 | 按异常类型分支处理 | 生产环境，精细化管控 |

#### 默认策略（alwaysThrow=false）

```java
// 工具执行失败后,异常信息会被包装成 ToolResult 返回给 AI
// AI 看到错误信息,可能调整参数重试,或者告诉你失败了
```

**优点**：AI 可能自己解决问题（比如参数不对就重调）
**缺点**：可能反复调用失败，浪费 token

#### 直接抛出（alwaysThrow=true）

```java
ToolCallingChatOptions options = ToolCallingChatOptions.builder()
    .toolExecutionExceptionProcessor(
        new ToolExecutionExceptionProcessor(true)  // alwaysThrow=true
    )
    .build();
```

**适用**：工具调用是关键路径，失败了就别继续了

#### 自定义异常处理器

```java
public class MyToolExceptionProcessor 
    implements ToolExecutionExceptionProcessor {

    @Override
    public ToolExecutionResult process(
        ToolCall toolCall, 
        Exception exception
    ) {
        // 按异常类型分支处理
        if (exception instanceof SecurityException) {
            // 权限异常:直接阻断,不给AI重试
            throw new RuntimeException("权限不足", exception);
        } else if (exception instanceof IOException) {
            // 网络异常:友好提示,AI可能换个方式
            return ToolExecutionResult.builder()
                .toolCall(toolCall)
                .resultData("网络繁忙,请稍后重试")
                .build();
        } else {
            // 其他异常:返回错误信息给AI
            return ToolExecutionResult.builder()
                .toolCall(toolCall)
                .resultData("工具执行失败: " + exception.getMessage())
                .build();
        }
    }
}
```

### 5.5 动态工具解析：ToolCallbackResolver

通过工具名称字符串匹配工具实例，支持动态加载和轻量化绑定。

**核心接口**：
```java
public interface ToolCallbackResolver {
    ToolCallback resolveToolCallback(String toolName);
    List<ToolCallback> resolveToolCallbacks(Set<String> toolNames);
}
```

**默认实现**：多级委托解析器，先从本地注册的工具里找，找不到再委托下一级。

**使用场景**：
```java
// 轻量化绑定:只绑定需要的几个工具,不用全量注册
String answer = chatClient.prompt()
    .user("帮我查天气")
    .toolNames("getWeather")  // ← 按名称绑定,轻量
    .call()
    .content();
```

**自定义扩展**：可以实现从数据库/配置中心动态加载工具：
```java
public class DynamicToolCallbackResolver implements ToolCallbackResolver {
    @Override
    public ToolCallback resolveToolCallback(String toolName) {
        // 从数据库查工具配置,动态构建 ToolCallback
        ToolConfig config = toolConfigMapper.selectByName(toolName);
        if (config != null) {
            return buildToolCallbackFromConfig(config);
        }
        return null;
    }
}
```

### 5.6 returnDirect 立即返回（深度解析）

第三章已介绍基础用法，这里补充底层原理。

**执行流程对比**：

```
【普通模式】                            【returnDirect 模式】
用户提问                                用户提问
  ↓                                      ↓
大模型决定调工具X                       大模型决定调工具X
  ↓                                      ↓
执行工具X                               执行工具X
  ↓                                      ↓
结果回传大模型                            结果直接返回给用户 ✓
  ↓                                   (少一轮模型交互)
大模型再组织语言回答
  ↓
返回给用户
```

**配置位置**：
- 注解方式：`@Tool(returnDirect = true)`
- 编程方式：`ToolMetadata.builder().returnDirect(true).build()`

**使用判断口诀**：
> 工具结果是不是"用户最终想要的东西"？
> - 是 → 用 `returnDirect=true`
> - 不是（还需要 AI 加工）→ 用默认 false

### 5.7 MCP 是什么？（对比 Tool Calling）

| 维度 | Tool Calling | MCP |
|------|--------------|-----|
| 定位 | 大模型内置的"意图识别" | 跨系统**标准化通信协议**（JSON-RPC） |
| 工具位置 | 工具函数写在应用**本地** | 工具独立部署为**远程 MCP Server** |
| 适用场景 | 单体小项目 / 快速 Demo | 微服务 / 多外部系统 / 多模型混用 |
| 依赖关系 | **基础必备** | **可选扩展**（Tool Calling 不需要 MCP） |
| 你项目 | ❌ 未用 | ❌ 未用 |

**关键结论**：
- **不能**说"工具调用就是 MCP"
- 教程教的是**原生 Tool Calling，完全不需要 MCP**
- MCP 是"企业级远程工具集成"方案，不是入门必修

### 5.8 立即返回 vs 流式返回

| 模式 | API | 适合 |
|------|-----|------|
| 立即返回 | `.call().content()` | 普通问答、短答案 |
| 流式返回 | `.stream().content()` | 长答案、需要实时显示 |

**你项目**：目前用立即返回。

---

## 六、可观测性（日志监控）

### 6.1 当前官方能力

**现状**：Spring AI 工具调用目前**没有完整的指标/链路监控**，只有 DEBUG 级日志。

**开启方法**：
```yaml
# application.yml 开启 DEBUG 日志
logging:
  level:
    org.springframework.ai: DEBUG
```

**默认日志内容**（`DefaultToolCallingManager` 打印）：
- 工具调用前：准备执行哪些工具
- 工具执行后：执行结果、耗时
- 异常时：错误堆栈

**调试必备**：开发阶段建议开启，能看到完整的工具调用链路。

### 6.2 未来规划能力

Spring AI 官方 roadmap 中的可观测性能力：
- 调用指标（成功率、耗时、调用次数）
- 分布式追踪（Trace ID 贯穿整个调用链）
- 可视化面板（Grafana 仪表盘）
- 性能监控（P99、P95 耗时）

> 目前（2026 年中）尚未完全落地，生产环境需要自己实现。

### 6.3 自定义监控方案

**思路**：用**代理模式**包装 `ToolCallback` 或 `ToolCallingManager`，手动埋点。

#### 方案 A：代理 ToolCallback（推荐，粒度细）

```java
// 包装类,给每个工具加上监控
public class MonitoredToolCallback implements ToolCallback {
    private final ToolCallback delegate;  // 原始工具
    private final MeterRegistry meterRegistry;

    @Override
    public String call(String toolInput, ToolContext context) {
        // 1. 计时开始
        Timer.Sample sample = Timer.start(meterRegistry);
        String toolName = getToolDefinition().getName();

        try {
            // 2. 执行原工具
            String result = delegate.call(toolInput, context);

            // 3. 成功计数
            Counter.builder("tool.calls")
                .tag("tool", toolName)
                .tag("status", "success")
                .register(meterRegistry)
                .increment();

            return result;
        } catch (Exception e) {
            // 4. 失败计数
            Counter.builder("tool.calls")
                .tag("tool", toolName)
                .tag("status", "error")
                .tag("error", e.getClass().getSimpleName())
                .register(meterRegistry)
                .increment();
            throw e;
        } finally {
            // 5. 记录耗时
            sample.stop(Timer.builder("tool.duration")
                .tag("tool", toolName)
                .register(meterRegistry));
        }
    }

    // 其他方法委托给 delegate...
}
```

#### 方案 B：代理 ToolCallingManager（全局入口）

```java
public class MonitoredToolCallingManager implements ToolCallingManager {
    private final ToolCallingManager delegate;
    private final MeterRegistry meterRegistry;

    @Override
    public List<ToolExecutionResult> executeToolCalls(
        List<ToolCall> toolCalls,
        ToolCallingChatOptions options
    ) {
        // 全局埋点
        log.info("开始执行 {} 个工具调用", toolCalls.size());
        long start = System.currentTimeMillis();

        try {
            List<ToolExecutionResult> results = delegate.executeToolCalls(toolCalls, options);
            log.info("工具调用完成,耗时 {}ms", System.currentTimeMillis() - start);
            return results;
        } catch (Exception e) {
            log.error("工具调用失败", e);
            throw e;
        }
    }
}
```

**可采集的指标**：

| 指标 | 类型 | 用途 |
|------|------|------|
| 工具调用次数 | Counter | 统计各工具使用频率 |
| 工具成功率 | Counter | 监控工具稳定性 |
| 工具耗时 | Timer | 性能分析，发现慢工具 |
| 错误类型分布 | Counter | 定位常见错误原因 |
| 入参大小 | DistributionSummary | 防止 token 爆炸 |

---

## 七、扩展思路（你项目的可能扩展）

| 思路 | 实现难度 | 价值 |
|------|---------|------|
| 加"实时天气"工具 | ⭐⭐（调免费 API） | ⭐⭐⭐⭐ |
| 加"生成 PDF 行程单"工具 | ⭐⭐（用 iText） | ⭐⭐⭐⭐ |
| 加"网页抓取景点"工具 | ⭐⭐（用 jsoup） | ⭐⭐⭐ |
| 加"汇率/距离计算"工具 | ⭐（纯函数） | ⭐⭐ |
| 加 MCP Client 接外部工具服务 | ⭐⭐⭐ | ⭐⭐（过设计） |
| 加"终端命令"工具 | ⭐ | ⛔ 危险 |

**推荐先做**："生成 PDF 行程单" + "实时天气" — 用户最直接受益的 2 个。

### 7.1 新增通用工具建议

| 工具类型 | 具体工具 | 用途 |
|---------|---------|------|
| 时间工具 | 获取当前时间、日期计算 | 行程规划中计算日期差 |
| 邮件工具 | 发送邮件 | 把行程 PDF 发到用户邮箱 |
| 数据库工具 | 通用 CRUD | 操作行程、订单等数据 |

### 7.2 PDF 工具生产优化

当前实现：生成本地文件 → 返回路径
生产优化：生成 → 上传 OSS → 返回公网 URL

```
本地生成 PDF
  ↓
上传阿里云 OSS / 腾讯云 COS
  ↓
返回公网访问链接 (https://xxx.oss.com/beijing.pdf)
  ↓
returnDirect 直接返回给用户
```

**搭配 `returnDirect=true`**：用户点链接就能下载，体验更好。

### 7.3 自主扩展能力

- PDF / Word 文档解析（读用户上传的行程单）
- 自动注入会话上下文（用户偏好、历史行程）
- 工具权限控制（不同用户能调用的工具不同）

---

## 八、本节作业（你项目对标）

教程 Ch 06 作业：
> 1. 复现全部 6 类工具代码，额外自研 1 个全新工具
> 2. 梳理 Spring AI 工具调用底层完整原理（笔记 / 流程图）

| 作业 | 你的完成度 | 备注 |
|------|----------|------|
| 1. 6 类工具复现 | ⏸️ 0% | 代码模板在 §四，以后直接抄 |
| 2. 自研 1 个新工具 | ⏸️ 0% | 建议做"天气查询"工具 |
| 3. 底层原理笔记 | **100%** | §三~§六 完整覆盖 |
| 4. 了解 MCP | ✅ 100% | §五.7 |

**原理笔记 100%**，代码实现待以后落地。**Ch 06 教程 100% 笔记化**。

---

## 九、本章小结

| 要点 | 一句话 |
|------|--------|
| 工具调用 | AI 借用外部工具补齐能力，大模型只决策，程序执行 |
| Tool Calling = Function Calling | 同义词，Spring AI 统一叫 Tool Calling |
| 6 步流程 | 定义 → 选择 → 意图 → 执行 → 返回 → 生成 |
| 6 大自研工具 | 文件 / 搜索 / 抓取 / 终端 / 下载 / PDF |
| ToolCallback | 所有工具顶层接口，元数据 + 执行 |
| ToolCallingManager | 全局调度器，解析 + 匹配 + 执行 |
| 两种执行模式 | 框架自动（默认）vs 手动控制（定制） |
| returnDirect | 工具结果直接返回用户，省一轮交互 |
| 异常处理 | 三种策略：默认返回 AI / 直接抛出 / 自定义 |
| MCP | 企业级远程工具通信标准，跟 Tool Calling 互补不替代 |
| 安全原则 | 工具执行权永远在本地程序，不在大模型 |
| 可观测性 | 目前只有 DEBUG 日志，生产需自定义监控 |

---

*最后更新：2026-06-23 · 完整踩坑见 [深度思考练习手册](./深度思考练习手册.md)*