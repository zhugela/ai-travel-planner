# 安全文件路径

> 状态:提议
> 来源:文件工具与文件会话记忆代码审查
> 最后验证:2026-11-10

## 问题

把用户提供的文件名直接拼到固定目录下,并不能保证结果仍在该目录。`../`、绝对路径、混合分隔符、符号链接和编码差异都可能造成路径穿越或越权覆盖。

核心不变量:**最终实际访问的路径必须位于预先批准的根目录内。**

## 统一解析流程

所有文件读、写、删、下载、PDF 生成和会话存储都应复用同一个安全路径解析器:

1. 固定并创建允许根目录,将根目录转为绝对规范路径。
2. 拒绝空值、NUL 字符、绝对路径和不允许的扩展名。
3. 使用 `root.resolve(userInput).normalize()` 得到候选路径。
4. 校验 `candidate.startsWith(root)`;失败立即拒绝。
5. 需要防符号链接逃逸时,对已存在的父目录使用真实路径再校验。
6. 写入采用临时文件 + 原子移动,避免半写入和并发覆盖。
7. 返回逻辑文件名或相对路径,尽量不向用户暴露主机绝对路径。

伪代码:

```java
Path root = configuredRoot.toAbsolutePath().normalize();
Path candidate = root.resolve(userInput).normalize();
if (candidate.isAbsolute() && !candidate.startsWith(root)) reject();
if (!candidate.startsWith(root)) reject();
return candidate;
```

仅靠以下做法不够:
- 只替换字符串中的 `../`。
- 只检查扩展名。
- 使用 `new File(baseDir, fileName)` 后直接读写。
- 认为 Windows 只有 `/` 或只有 `\` 一种分隔符。

## 会话 ID 也属于路径输入

当 `conversationId` 被用作文件名时,它必须满足:
- 使用服务端生成的 UUID,或只允许固定字符集与长度。
- 不接受路径分隔符、点段和设备名。
- 同一会话的并发写入需要锁或单线程队列。
- 序列化组件必须线程安全;非线程安全实例不能全局共享。

## 验收用例

至少覆盖:
- 正常:`trip.md`、合法 UUID。
- 点段:`../secret`、`a/../../secret`。
- 绝对路径:`C:\Windows\win.ini`、`/etc/passwd`。
- 混合分隔符和 URL 编码后的点段。
- 超长文件名、保留设备名、空文件名。
- 符号链接指向允许目录外。
- 两个线程同时写同一会话。

验收标准:
- 所有越界输入被拒绝且没有创建/读取根目录外文件。
- 拒绝日志只记录安全摘要,不回显完整敏感路径。

## 本项目现状

当前 `FileOperationTool`、`PdfGenerationTool`、`DownloadTool` 和 `FileBasedChatMemory` 都使用用户可影响的文件名或会话 ID,尚未共享统一的安全路径解析器。

## 项目证据

- `src/main/java/com/yupi/aitravelplanner/tools/FileOperationTool.java`
- `src/main/java/com/yupi/aitravelplanner/tools/PdfGenerationTool.java`
- `src/main/java/com/yupi/aitravelplanner/tools/DownloadTool.java`
- `src/main/java/com/yupi/aitravelplanner/chatmemory/FileBasedChatMemory.java`

## Related

- [[security/agent-tool-security]]
- [[security/api-security-baseline]]
- [[engineering/ai-testing-and-delivery]]
