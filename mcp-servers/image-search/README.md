# yu-image-search-mcp-server

基于 Spring AI 的图片搜索 MCP(Model Context Protocol)服务端,通过 Pexels 公共 API 提供关键词图片搜索能力。

## 功能

- 工具名:`searchImage`
- 输入:关键词(String,如"computer"、"beach"、"food")
- 输出:逗号分隔的图片 URL 列表(中等尺寸 medium)
- 协议:MCP 2024-11-05
- 传输:Stdio(默认)+ SSE(可选)

## 快速开始

### 前置条件

- Java 21
- Maven 3.9+
- Pexels API Key(免费):https://www.pexels.com/api/

### 1. 申请 Pexels Key

到 https://www.pexels.com/api/ 申请免费 API Key(200 次/小时,20000 次/月)。

### 2. 打包

```bash
mvn clean package -DskipTests
```

产出:`target/yu-image-search-mcp-server-0.0.1-SNAPSHOT.jar`

### 3. 设置环境变量

**Windows PowerShell**:
```powershell
$env:PEXELS_API_KEY = "你的Pexels key"
```

**Linux/Mac**:
```bash
export PEXELS_API_KEY=你的Pexels key
```

### 4. 启动

**Stdio 模式**(默认,本地子进程,适合 Client 集成):
```bash
java -jar target/yu-image-search-mcp-server-0.0.1-SNAPSHOT.jar
```

**SSE 模式**(远程 HTTP,适合多客户端共享):
```bash
java -jar target/yu-image-search-mcp-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=sse
```

SSE 端点:`http://localhost:8127/sse`

## 配置

修改 `src/main/resources/application.yml` 或传环境变量:

```yaml
pexels:
  api-key: ${PEXELS_API_KEY}
```

## MCP 客户端配置示例

在 Client 项目 `mcp-servers.json`:

```json
{
  "mcpServers": {
    "image-search": {
      "command": "java",
      "args": [
        "-Dspring.profiles.active=stdio",
        "-jar",
        "/path/to/yu-image-search-mcp-server-0.0.1-SNAPSHOT.jar"
      ],
      "env": {
        "PEXELS_API_KEY": "你的Pexels key"
      }
    }
  }
}
```

## 协议

[MCP 协议 2024-11-05](https://modelcontextprotocol.io)

## License

MIT
