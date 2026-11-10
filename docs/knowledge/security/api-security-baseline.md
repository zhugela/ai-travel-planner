# API 安全基线

> 状态:提议
> 来源:Controller 与部署配置审查
> 最后验证:2026-11-10

## 问题

AI 接口通常连接付费模型、文件工具和外部服务。没有身份、额度和输入边界时,匿名请求可以消耗费用、占满线程、读取会话或触发高风险工具。

## 推荐权限分级

| 能力 | 最低要求 | 额外限制 |
|---|---|---|
| 健康检查 | 可匿名 | 只返回状态,不泄露配置 |
| 普通聊天/RAG | 已认证用户 | 用户级限流、输入长度、会话归属校验 |
| 结构化报告/Agent | 已认证用户 | 更低并发和额度,任务超时 |
| 文件写入/下载/抓取 | 特定权限 | 目标限制、审计、必要时确认 |
| 终端/部署/删除 | 管理权限 | 默认关闭、隔离环境、强确认 |

## 可复用规则

### 身份与会话
- 服务端从认证信息确定用户,不要信任客户端传来的 owner/userId。
- `conversationId` 必须校验属于当前用户,不能作为全局可猜测 ID。
- 内部服务调用使用独立服务身份,不要复用个人密钥。

### 输入与资源边界
- 为 message、question、conversationId、文件内容设置长度与字符边界。
- 限制每用户并发 SSE/Agent 数量、请求频率和每日模型额度。
- 后端统一设置模型、HTTP、工具和整体任务超时。
- 对请求体、下载体、工具输出和日志分别设大小上限。

### CORS 与文档
- 生产环境只允许明确的前端来源,不要用宽泛通配符。
- Swagger/Knife4j 在生产默认关闭或要求认证。
- 错误响应不返回堆栈、主机路径、密钥或第三方完整原文。

### 日志与隐私
- 生产默认不记录完整 Prompt、模型回答、知识片段和聊天历史。
- 使用 requestId/traceId 关联请求,敏感字段脱敏。
- 设定日志保留期限和访问权限。

## 最小落地顺序

1. 关闭或下线高风险工具。
2. 给所有业务 API 增加认证,校验会话归属。
3. 增加输入长度、并发、频率和超时限制。
4. 收紧 CORS,关闭生产接口文档和 DEBUG 日志。
5. 增加用户/工具级授权、额度和审计。

## 验收清单

- [ ] 未认证请求访问业务 API 返回 401。
- [ ] 普通用户调用高风险工具返回 403。
- [ ] 猜测他人的 conversationId 无法读取或续写会话。
- [ ] 超长输入、超额频率和超并发分别返回明确错误。
- [ ] SSE 断开后释放后台任务与并发配额。
- [ ] 生产接口文档不可匿名访问。
- [ ] 日志扫描不包含密钥、完整隐私正文和本机绝对路径。

## 本项目现状

当前接口已具备基本参数校验和统一错误结构,但业务 API 尚未建立完整鉴权、会话归属、限流和工具级授权。生产配置还需关闭调试级日志并限制接口文档访问。

## 项目证据

- `src/main/java/com/yupi/aitravelplanner/controller/AiController.java`
- `src/main/java/com/yupi/aitravelplanner/controller/AgentController.java`
- `src/main/java/com/yupi/aitravelplanner/config/CorsConfig.java`
- `src/main/resources/application.yml`
- `src/main/resources/application-prod.yml`

## Related

- [[security/secret-management]]
- [[security/agent-tool-security]]
- [[agent/sse-agent-lifecycle]]
