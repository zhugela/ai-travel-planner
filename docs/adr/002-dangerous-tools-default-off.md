# ADR-002:高风险 Agent 工具默认关闭

- 状态:已接受,待完全落地
- 日期:2026-11-10
- 决策人:项目组

## 背景

Agent 会把模型输出转成真实系统操作。终端执行、任意文件读写、网页抓取和下载一旦直接暴露,风险高于普通聊天接口。现有“首个命令白名单”不足以阻止 shell 连接符、重定向和子命令绕过,文件名拼接也未形成完整路径边界。

## 决策

高风险工具采用“默认关闭 + 显式启用 + 能力分级”:
- 生产环境默认不注册终端工具。
- 文件工具只能访问固定根目录,路径必须规范化后再校验包含关系。
- 下载和抓取工具必须阻止内网地址、限制协议、大小、超时和重定向。
- 工具注册前检查环境、权限和依赖;不可用或不安全的工具不暴露给模型。
- 每次工具调用记录操作者、工具、目标、结果和耗时,但不记录密钥和敏感正文。

## 备选方案

| 方案 | 优点 | 缺点 | 未选择原因 |
|---|---|---|---|
| 只靠 Prompt 禁止危险操作 | 改动最小 | 模型约束不是安全边界 | 无法抵抗提示注入和误调用 |
| shell 命令字符串白名单 | 保留灵活性 | 容易被连接符、参数和解释器绕过 | 当前实现仍存在绕过面 |
| 结构化专用工具 | 参数可校验、权限清晰 | 工具数量增加 | 作为长期推荐方案 |
| 沙箱执行通用终端 | 兼顾通用性和隔离 | 部署复杂、资源成本高 | 规模扩大后再评估 |

## 影响

### 正向
- 模型误判和提示注入不再直接等价于主机命令执行。
- 能力边界可以测试、审计和分环境配置。

### 代价与风险
- 演示环境若要启用高风险工具,必须额外配置。
- 一些“万能 Agent”体验会被限制,需要更多专用工具替代。

## 落地与验收

- [x] 部分外部依赖工具已有可用性门控。
- [ ] TerminalTool 默认不注册,仅在明确的开发配置中启用。
- [ ] 文件类工具统一调用安全路径解析器。
- [ ] 下载和抓取增加 SSRF、大小、超时、重定向限制。
- [ ] 增加鉴权、限流和工具调用审计。
- [ ] 增加绕过测试,覆盖 `..`、绝对路径、shell 连接符和内网 URL。

回退方式:只关闭对应工具注册,不影响普通聊天、RAG 和结构化报告。

## 项目证据

- `src/main/java/com/yupi/aitravelplanner/controller/AgentController.java`
- `src/main/java/com/yupi/aitravelplanner/tools/TerminalTool.java`
- `src/main/java/com/yupi/aitravelplanner/tools/FileOperationTool.java`
- `src/main/java/com/yupi/aitravelplanner/tools/DownloadTool.java`

## Related

- [[knowledge/security/agent-tool-security]]
- [[knowledge/security/safe-file-paths]]
- [[knowledge/security/api-security-baseline]]
