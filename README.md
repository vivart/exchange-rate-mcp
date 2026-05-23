# Exchange Rate MCP Server

A [Model Context Protocol (MCP)](https://modelcontextprotocol.io) server written in Kotlin that exposes live currency exchange rate tools to any MCP-compatible AI client (Claude Desktop, Claude Code, etc.).

Exchange rates are fetched from [Frankfurter](https://frankfurter.dev) (`api.frankfurter.dev/v2`) — **free, no API key required**, sourced from the European Central Bank.

---

## Tools

| Tool                | Description                                    | Parameters                                        |
|---------------------|------------------------------------------------|---------------------------------------------------|
| `get_exchange_rate` | Get the current rate between two currencies    | `from` (string), `to` (string)                    |
| `convert_currency`  | Convert an amount from one currency to another | `from` (string), `to` (string), `amount` (number) |
| `list_currencies`   | List all supported currencies                  | *(none)*                                          |

### Example prompts

> "What is the exchange rate from USD to JPY?"

> "Convert 500 EUR to GBP"

> "List all supported currencies"

---

## Prerequisites

| Tool   | Version | Install                               |
|--------|---------|---------------------------------------|
| JDK 21 | 21+     | `winget install Microsoft.OpenJDK.21` |

> **Note:** The project uses the Gradle wrapper (`gradlew.bat`) — no separate Gradle installation needed.

---

## Build

```bat
# Windows
.\gradlew.bat jar

# macOS / Linux
./gradlew jar
```

The fat JAR (includes all dependencies) is produced at:

```
build/libs/exchange-rate-mcp-1.0.0.jar
```

---

## Test

```bat
.\gradlew.bat test
```

---

## Inspect with MCP Inspector

```bat
npx @modelcontextprotocol/inspector -- java -jar build/libs/exchange-rate-mcp-1.0.0.jar
```

Opens a browser UI at `http://localhost:6274` where you can browse and invoke all tools interactively.

---

## Connect to Claude Desktop

1. Build the JAR (see above).

2. Open (or create) the Claude Desktop config file:
   - **Windows:** `%APPDATA%\Claude\claude_desktop_config.json`
   - **macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`

3. Add the server entry:

```json
{
  "mcpServers": {
    "exchange-rate": {
      "command": "java",
      "args": [
        "-jar",
        "C:\\path\\to\\exchange-rate-mcp\\build\\libs\\exchange-rate-mcp-1.0.0.jar"
      ]
    }
  }
}
```

4. Restart Claude Desktop — the tools will appear automatically.

---

## Connect to Claude Code

### 1. Build the JAR

```bat
.\gradlew.bat jar
```

### 2. Register the MCP server

**Windows (PowerShell):**

```powershell
cmd /c "claude mcp add exchange-rate -- java -jar C:\path\to\exchange-rate-mcp\build\libs\exchange-rate-mcp-1.0.0.jar"
```

**macOS / Linux:**

```bash
claude mcp add exchange-rate -- java -jar /path/to/exchange-rate-mcp/build/libs/exchange-rate-mcp-1.0.0.jar
```

Use the **absolute path** to the JAR — relative paths do not work because Claude Code launches the server from a different working directory.

### 3. Verify

```bash
claude mcp list
```

### 4. Restart Claude Code

The MCP server is loaded at startup. Open a new session for the tools to appear.

---

## Project Structure

```
exchange-rate-mcp/
├── build.gradle.kts                              # Gradle build (Kotlin DSL)
├── settings.gradle.kts
├── gradle/wrapper/                               # Gradle wrapper (no install needed)
└── src/
    ├── main/kotlin/com/vivart/
    │   ├── Main.kt                               # Entry point — starts stdio transport
    │   ├── FrankfurterClient.kt                  # Ktor HTTP client + data classes
    │   └── ExchangeRateServer.kt                 # MCP server + tool handlers
    └── test/kotlin/com/vivart/
        ├── FrankfurterClientTest.kt              # HTTP layer tests (Ktor MockEngine)
        └── ExchangeRateServerTest.kt             # Tool handler tests (MockK)
```

---

## Key Dependencies

| Library                                                                   | Version | Purpose                                                 |
|---------------------------------------------------------------------------|---------|---------------------------------------------------------|
| [MCP Kotlin SDK](https://github.com/modelcontextprotocol/kotlin-sdk)      | 0.12.0  | MCP server framework                                    |
| [Ktor Client](https://ktor.io)                                            | 3.3.3   | HTTP calls to Frankfurter API                           |
| [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)  | (bundled with Kotlin 2.3.21) | JSON deserialization          |
| [Logback](https://logback.qos.ch)                                         | 1.5.32  | Logging (writes to stderr to keep stdout clean for MCP) |
| Kotlin                                                                    | 2.3.21  | Language                                                |
| Gradle                                                                    | 9.5.1   | Build tool                                              |

---

## How It Works

```
Claude Desktop / Claude Code
        │  JSON-RPC 2.0 over stdio
        ▼
  StdioServerTransport
        │
      Server  (MCP Kotlin SDK)
        │  dispatches tool calls
        ▼
  FrankfurterClient
        │  HTTPS GET
        ▼
  api.frankfurter.dev/v2
```

1. Claude calls a tool (e.g. `convert_currency`).
2. The MCP server receives the JSON-RPC request over stdin.
3. `FrankfurterClient` fetches live rates from `api.frankfurter.dev/v2`.
4. The result is formatted and returned to Claude as a text response.

---

## Adding a New Tool

1. Add a handler function in `ExchangeRateServer.kt`:

```kotlin
internal suspend fun myNewTool(param: String): CallToolResult =
    runCatching { /* call FrankfurterClient or other logic */ }
        .fold(
            onSuccess = { toolSuccess("Result: $it") },
            onFailure = { toolError("Error: ${it.message}") },
        )
```

2. Register it in `buildServer()`:

```kotlin
server.addTool(
    name = "my_new_tool",
    description = "What this tool does.",
    inputSchema = ToolSchema(
        properties = buildJsonObject {
            putJsonObject("param") {
                put("type", "string")
                put("description", "Description of param")
            }
        },
        required = listOf("param"),
    ),
) { req ->
    val param = req.arguments?.get("param")?.jsonPrimitive?.content
        ?: return@addTool toolError("Missing 'param'")
    myNewTool(param)
}
```

3. Rebuild: `.\gradlew.bat jar`
