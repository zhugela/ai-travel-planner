# 改动验收清单

> **为什么需要这份文档**
> 本项目出现过"curl 通了就报完成,实际页面是坏的"的情况。
> 后来发现是 SSE 多行 data 被拆碎、双重编码导致 500、前端渲染空数组崩溃这类
> **只有真正打开页面才能发现**的问题。
>
> 规则:**接口通过 ≠ 功能可用**。改完必须走完下面四层。
>
> 最后更新:2026-11-10

---

## 一、验收四层(从下往上,不许跳级)

```
第 4 层  端到端体验   页面真实点击、看视觉效果、看交互流畅度
第 3 层  前端渲染     控制台无报错、空态/错误态正常、长内容不撑爆
第 2 层  接口契约     入参出参字段对得上、SSE 事件名对得上
第 1 层  接口连通     curl 返回 200 且内容正确
```

很多问题的形态:
- 第 1 层过,第 2 层挂(字段名对不上,前端读到 `undefined`)
- 第 2 层过,第 3 层挂(SSE 多行 data 被拆碎,最终回答丢失)
- 第 3 层过,第 4 层挂(能用但布局崩了)

---

## 二、第 1 层:接口连通

```bash
# 后端存活
curl -s -o /dev/null -w "%{http_code}" http://localhost:8123/api/doc.html
# 期望 200

# 问答 SSE(注意要 --get --data-urlencode,不能手拼中文)
curl -s -N --get --data-urlencode "message=你好" \
  http://localhost:8123/api/travel/chat/sse | head -c 200
# 期望看到 data: 帧

# Agent SSE
curl -s -N --get --data-urlencode "message=查杭州天气" \
  http://localhost:8123/api/agent/sse | head -c 500
# 期望 step 帧 + 最后 event:done

# RAG
curl -s -X POST http://localhost:8123/api/travel/rag-chat \
  -H "Content-Type: application/json" \
  -d '{"question":"杭州有什么好玩的"}'
# 期望 matched:true 或明确兜底
```

**坑**:手拼 `?message=中文` 会乱码,必须 `--get --data-urlencode`。

---

## 三、第 2 层:接口契约

逐字段比对,不能只看"有返回"。

### 结构化报告

```bash
curl -s -X POST http://localhost:8123/api/travel/report \
  -H "Content-Type: application/json" \
  -d '{"message":"成都 5 天，带爸妈，节奏慢"}' \
  | python -c "import sys,json; d=json.load(sys.stdin)['data']; print(list(d.keys())); print('days:', len(d.get('days') or []))"
```

**检查项**:
- [ ] `days` 数量 === 用户要求的天数
- [ ] 每个 day 有 `theme` / `morning` / `afternoon` / `evening`
- [ ] `budget.total` 接近用户预算,四项之和 ≈ total
- [ ] `assumptions` 非空(信息残缺时)
- [ ] 没有旧的 `suggestions` 字段残留

### SSE 事件名

| 端点 | 事件名 | 谁消费 |
|---|---|---|
| `/travel/chat/sse` | 默认 message | ChatView |
| `/agent/sse` | `step` / `loop` / `error` / `done` | AgentView |

**坑(踩过)**:后端把一步的内容用多行 `data:` 推送,按 SSE 规范每行触发一次回调,
且行间无分隔符。前端若逐条处理,内容会被拆碎丢失。
必须按 `Step N:` 分块聚合。

**检查项**:
- [ ] 前端监听的事件名 === 后端 `event:` 里的名字
- [ ] 后端只推 4 种事件时,前端不要等第 5 种(如 `tool`)

---

## 四、第 3 层:前端渲染

打开浏览器 F12,逐项确认。

**检查项**:
- [ ] Console 无 error(除了 `favicon.ico` 404,这个是正常的)
- [ ] Network 面板:同一请求**只发一次**(EventSource 未正确 close 会重连)
- [ ] 空数组不报错(如 `report.days = []` 时页面不白屏)
- [ ] 错误态有提示(后端 500 时页面有友好文案,不是空白)
- [ ] 长内容不撑爆布局(工具结果 2000 字时界面不崩)
- [ ] 加载态有反馈(按钮变"执行中",不卡死)

**踩过的坑**:
1. `URLSearchParams` 又套 `encodeURIComponent` → 双重编码 → 500
2. EventSource 收到连接关闭不 `close()` → 自动重连 → 重复请求、内容重复追加
3. 工具结果整段平铺 → 5000 字撑爆页面

---

## 五、第 4 层:端到端体验

**必须真的点一遍**,不能靠 curl 推断。

| 页面 | 必测动作 |
|---|---|
| 旅行对话 | 发一条消息 → 看打字机 → 看结束按钮复位 → 再发一条能续上上下文 |
| 智能行程 | 输入完整需求 → 看分天卡片 + 预算 + 提示 → 换个残缺需求看默认值补齐 |
| 旅行工具 | 发一个需要工具的任务 → 看工具卡片 → 看最终回答 → 长结果能折叠展开 |
| 灵感探索 | 问一个知识库有的问题 → 命中;问一个无关问题 → 明确拒绝 |

**检查项**:
- [ ] 视觉对齐(卡片间距、字号、颜色一致)
- [ ] 交互流畅(无卡顿、无闪烁)
- [ ] 移动端 375px 宽度下布局不塌

---

## 六、快速自查命令

```bash
# 两端存活
curl -s -o /dev/null -w "backend:%{http_code}\n" http://localhost:8123/api/doc.html
curl -s -o /dev/null -w "frontend:%{http_code}\n" http://localhost:3000/

# 后端错误日志
grep -E "ERROR|Exception" tmp/run.log | tail -5

# 前端构建能否通过
cd D:/code2/ai-travel-planner-frontend && npx vite build --logLevel error 2>&1 | grep -i error
```

---

## 七、报告"完成"时的强制格式

每次报完成,必须给出:

```
✅ 第 1 层接口: 通过 / 失败(附实际输出)
✅ 第 2 层契约: 通过 / 失败(附字段对比)
✅ 第 3 层渲染: 通过 / 失败(附 console 截图或 DOM 查询结果)
✅ 第 4 层体验: 通过 / 失败(附操作路径)
⚠️ 已知问题: xxx(不阻塞)
```

**没有第 3、4 层的验证记录,不许说"改好了"。**
