# AI Travel Planner · Spring AI Agent

> A ReAct-style travel planning agent built on Spring AI — a 4-layer agent core, 9 callable tools, SSE streaming and RAG-grounded retrieval.

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen?logo=springboot&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0-6DB33F)
![Vue](https://img.shields.io/badge/Vue-3-42b883?logo=vuedotjs&logoColor=white)

---

## What this is

Most "LLM demos" stop at `prompt -> text`. This project is about the engineering **around** the model:

- an agent loop with explicit **state**, **step budget** and **loop detection**
- a **tool layer** the model can call, including a tool that deliberately stops and asks the user a question
- **streaming** every agent step to the browser over SSE without occupying a Tomcat thread
- **RAG** retrieval that grounds the plan in a local travel knowledge base
- **file-based chat memory** so a conversation survives restarts, with no database

The domain is travel planning: the agent asks for missing constraints (days, budget, travellers), searches the web for current facts, reads/writes note files, and finally produces a day-by-day itinerary report.

## Architecture

```
POST /agent/sse
      │
      ▼
AgentController ──► YuTravelAgent ──────────────┐
                     (Spring @Component)        │
      ┌──────────────────────────────────────────┘
      │  BaseAgent            run() / runStream()  ← state machine + step budget
      │      │
      │  ReActAgent           think() + act()
      │      │
      │  ToolCallAgent        think: call ChatModel + parse tool calls
      │                       act  : invoke ToolCallback[]
      │
      ├── LoopGuard            repeats a tool / no progress → abort
      ├── StateContext         auto IDLE→RUNNING→IDLE on close
      ├── ToolCallback[]       9 tools
      └── ChatMemory           Kryo-serialised file store
```

| Layer | Responsibility | Why the extra layer exists |
|---|---|---|
| `BaseAgent` | state, step budget, `run()` sync loop, `runStream()` SSE | one loop implementation shared by every agent |
| `ReActAgent` | splits a step into `think()` / `act()` | keeps reasoning and side effects separable |
| `ToolCallAgent` | model call → tool-call parsing → tool execution | the only place that knows Spring AI's `ChatModel` API |
| `YuTravelAgent` | wiring + domain system prompt | business layer stays thin |

## The agent loop

`BaseAgent.run()` executes, in order:

1. **Busy guard** — reject a second call while state ≠ `IDLE`
2. **Append** the `UserMessage` to `messageList`
3. **New `LoopGuard`** per task
4. **`StateContext`** (try-with-resources) drives `RUNNING`, and `close()` resolves it back to `IDLE`
5. `while (RUNNING && currentStep < maxSteps)` → `step()`, then check loop guard
6. **`cleanup()`** hook, then return the accumulated per-step output

## Streaming design (SSE)

`runStream()` returns an `SseEmitter` with a 5-minute timeout and moves the loop onto a background thread via `CompletableFuture.runAsync`, so the Tomcat request thread is released immediately. Each iteration emits one `step` event; a detected loop emits a `loop` event; failures emit `error`.

Three callbacks are always registered — `onTimeout`, `onCompletion`, `onError` — so a dead client never leaves a dangling emitter on the server.

## Tools exposed to the model

| Tool | Purpose |
|---|---|
| `WeatherTool` | real-time city weather (wttr.in, no API key) |
| `WebSearchTool` | web search |
| `WebScrapingTool` | fetch and extract page text |
| `AskHumanTool` | stop and ask the user for missing constraints |
| `FileOperationTool` | read / write local note files |
| `DownloadTool` | download a URL to a working directory |
| `PdfGenerationTool` | generate a PDF from text |
| `TerminalTool` | execute whitelisted commands |
| `TerminateTool` | end the task with a final answer |

## RAG

`TravelDocumentLoader` ingests markdown travel notes → `SimpleVectorStore` (in-memory) → `RagRetrieverEnhancer` performs retrieval before the model call, injecting the retrieved context as an advisor. `TravelRagChatService` / `TravelRagController` expose `/travel/rag-chat` as a separate grounded endpoint so RAG can be evaluated independently from the agent.

## Quick start

```bash
export DASHSCOPE_API_KEY=your_key           # never hard-coded
./mvnw spring-boot:run                      # backend :8123
```

```bash
cd ai-travel-planner-frontend && npm install && npm run dev   # frontend :3000
```

Key endpoints:

| Method | Path | Description |
|---|---|---|
| GET | `/agent/sse` | agent loop streamed step by step |
| POST | `/travel/chat` | blocking chat |
| GET | `/travel/chat/sse` | chat streaming |
| GET | `/travel/chat/sse/emitter` | chat streaming via `SseEmitter` |
| POST | `/travel/report` | generate the itinerary report |
| POST | `/travel/rag-chat` | RAG-grounded answer |

## Project structure

```
src/main/java/com/yupi/aitravelplanner/
├── agent/        BaseAgent · ReActAgent · ToolCallAgent · YuTravelAgent
│   ├── context/  StateContext
│   ├── enums/    AgentState
│   └── workflow/ ChainAgent
├── tools/        9 @Tool implementations
├── rag/          TravelDocumentLoader · RagRetrieverEnhancer
├── service/      TravelRagChatService
├── chatmemory/   FileBasedChatMemory (Kryo)
├── advisor/      MyLoggerAdvisor
├── controller/   AgentController · AiController · TravelRagController
└── config/       vector store · CORS · tool registration
```

## Status

Educational project, actively evolving. Known limitations:

- vector store is in-memory, rebuilt on restart
- conversation memory is file-based — no multi-instance support by design
- remote MCP tools are disabled in `application.yml`
- end-to-end SSE against the frontend is the current testing focus

## License

Educational use. Built on top of a Spring AI course project and heavily extended.
