# SOP：Chrome DevTools UI 验证闭环

## 适用场景

- 修改 Vue 页面、SSE 展示、错误态或移动端布局。
- 准备声称“前端完成”。
- 接口正常但用户页面不可用。

## 前置条件

1. 后端在 `8123` 启动。
2. 前端在 `3000` 启动。
3. Vite 代理 `/api` 指向 `http://localhost:8123`。
4. 打开 DevTools，清空 Console 和 Network 旧记录。

## 步骤

### 1. 记录起点

- URL、视口尺寸、当前构建版本。
- Console 是否有历史错误。
- Network 是否已有未结束的 SSE。

### 2. 验证接口连通

Network 过滤 `/api`：

- 请求路径正确。
- 状态码符合预期。
- JSON 字段与 `API_CONTRACT.md` 一致。
- SSE `Content-Type: text/event-stream`。

### 3. 验证流式行为

对聊天和 Agent：

- 是否持续出现 data/event。
- `done` 是否到达。
- 到达 `done` 后是否断开，而不是自动重连。
- 页面卸载或点停止后是否关闭连接。
- 控制台是否有解析错误。

### 4. 验证渲染状态

- loading、正常、空态、错误态、超时。
- 长文本是否溢出。
- Agent 多行 `step` 是否被正确聚合。
- RAG `matched=false` 是否显示兜底样式。
- Report 空数组是否不白屏。

### 5. 验证真实交互

按 `ACCEPTANCE_CHECKLIST.md` 四条主流程实际点击：

1. 旅行问答。
2. 智能行程。
3. 灵感探索。
4. 旅行工具。

不是只看 curl。

### 6. 验证移动端

切换 375px 宽度：

- 输入框和按钮可操作。
- 时间线/卡片不横向溢出。
- 长错误信息可读。

### 7. 保存证据

至少保留：

- Console 结果。
- 关键 Network 请求/SSE 截图。
- 页面主流程截图。
- 操作路径和发现的边界。

证据写进 `feature_list.json` / `claude-progress.md`，不要只留在聊天中。
