# Agent 设计说明

> 本文记录 `agent/` 包的架构决策和踩过的坑。
> 改 Agent 相关代码前先读这份,避免重犯。
>
> 最后更新:2026-11-10

---

## 一、四层继承链路

```
BaseAgent         状态机 + run/runStream + maxSteps
  └─ ReActAgent   step() = think() → act()
       └─ ToolCallAgent   think() 调模型 / act() 执行工具
            └─ YuTravelAgent   成品:systemPrompt + maxSteps=20
```

---

## 二、必修的 5 个坑

### 坑 1:think() 存历史必须带 toolCalls

```java
// ❌ 只存文本 —— 模型下一轮看不到"我已经调过 getWeather",
//    会反复调同一个工具直到 maxSteps(实测连续 20 步调天气)
getMessageList().add(new AssistantMessage(assistantOutput.getText()));

// ✅ 带上 toolCalls
getMessageList().add(new AssistantMessage(
        assistantOutput.getText() == null ? "" : assistantOutput.getText(),
        assistantOutput.getMetadata(),
        assistantOutput.getToolCalls()));
```

### 坑 2:act() 必须回填 ToolResponseMessage

```java
// ❌ 只把结果拼进返回字符串,模型看不到工具结果
result.append("【工具执行】").append(toolName).append(" => ").append(toolResult);

// ✅ 同时写进消息历史
List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
responses.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolName, toolResult));
getMessageList().add(new ToolResponseMessage(responses));
```

**坑 1 + 坑 2 是同一类错误**:历史里只有"我调了工具"或只有"结果是啥",模型无法形成完整因果链。

### 坑 3:必须注册 TerminateTool

`TerminateTool.finishTask()` 是模型结束任务的唯一途径。
`AgentController.availableLocalTools()` 里**必须包含** `terminateTool`,
否则模型永远无法置 FINISHED,只能跑满 maxSteps。

```java
tools.add(terminateTool);  // 漏了这行 = 模型永远停不下来
```

### 坑 4:无工具调用 = 任务结束

```java
// ❌ 返回一句"思考完成"就继续下一轮 → 空转到 maxSteps
if (!needRunTool) return "思考完成，无需调用工具";

// ✅ 置 FINISHED,并把模型这段文本作为最终答案
if (!needRunTool) {
    setState(AgentState.FINISHED);
    return finalText.isBlank() ? "思考完成，无需调用工具" : finalText;
}
```

### 坑 5:runStream 结束必须推 done

```java
// ❌ 直接 complete() —— 浏览器 EventSource 会按规范自动重连,重复执行整个 Agent
emitter.complete();

// ✅ 先推 done 帧,前端收到后主动 close
try {
    emitter.send(SseEmitter.event().name("done").data("finished"));
} catch (Exception ignored) {}
try { emitter.complete(); } catch (Exception ignored) {}
```

---

## 三、能力门控(2026-11-10 新增)

### 原则
**不可用的工具不注册给模型。**

模型无法区分"工具存在但配置错了"和"这个能力本就没有",
把所有工具都塞给它,结果是它先调用一次撞错、再调 `getStatus` 确认、最后输出道歉。

### 实现

每个有外部依赖的工具实现 `health()`:

```java
public record Health(boolean available, String reason) {}

public Health health() {
    if (apiKey == null || apiKey.trim().isEmpty()) {
        return new Health(false, "未配置 SEARCH_API_KEY 环境变量");
    }
    return new Health(true, "已配置");
}
```

`AgentController` 注册前过滤 + 把边界写进 system prompt:

```java
private List<Object> availableLocalTools() {
    List<Object> tools = new ArrayList<>();
    tools.add(fileOperationTool);
    // ...
    tools.add(terminateTool);

    WebSearchTool.Health search = webSearchTool.health();
    if (search.available()) tools.add(webSearchTool);
    else log.warn("[AgentController] 跳过联网搜索工具: {}", search.reason());

    return tools;
}
```

```java
private String buildCapabilityNote() {
    // 生成 "【当前不可用的能力】联网搜索(未配置 SEARCH_API_KEY)..." 附加到 prompt
}
```

### 当前工具可用性

| 工具 | 依赖 | 检查方式 |
|---|---|---|
| `fileOperationTool` | 无 | 恒可用 |
| `webScrapingTool` | 无(公网可达即可) | 恒可用 |
| `terminalTool` | 无(白名单) | 恒可用 |
| `downloadTool` | 无 | 恒可用 |
| `pdfGenerationTool` | 无 | 恒可用 |
| `weatherTool` | wttr.in(免 key) | 恒可用 |
| `terminateTool` | 无 | 恒可用,**不能漏** |
| `webSearchTool` | SerpAPI | `SEARCH_API_KEY` 是否为空 |

---

## 四、SSE 输出格式

`BaseAgent.runStream()` 推送的事件:

| 事件 | 内容 | 说明 |
|---|---|---|
| `step` | `Step N:\n<step() 返回值>` | 每轮一步 |
| `loop` | `[LoopGuard] ...` | 死循环 |
| `error` | 异常信息 | |
| `done` | `finished` | 必须推,前端据此 close |

**注意**:`step` 的内容含换行,Spring 会拆成多行 `data:`。
按 SSE 规范每行触发一次前端回调、行间无分隔符,
所以**前端必须按 `Step N:` 分块聚合**,不能逐条处理。

详见 [API_CONTRACT.md](./API_CONTRACT.md) 第四节。

---

## 五、LoopGuard 阈值

```java
SAME_TOOL_THRESHOLD = 3;    // 连续 3 次调同一工具 → 判定死循环
NO_PROGRESS_THRESHOLD = 5;  // 连续 5 次无进展 → 判定死循环
```

有了坑 1/2/4 的修复后,LoopGuard 基本不会触发;
它已是兜底而非主要手段。

---

## 六、调试方法

```bash
# 直接看 SSE 事件流(注意 --get --data-urlencode 防中文乱码)
curl -s -N --get --data-urlencode "message=查杭州天气" \
  http://localhost:8123/api/agent/sse

# 看后端日志里模型实际收到的工具列表
grep "最终工具数量\|跳过联网搜索" tmp/run.log
```
