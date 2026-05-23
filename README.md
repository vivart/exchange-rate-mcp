# Exchange Rate MCP Server

A [Model Context Protocol (MCP)](https://modelcontextprotocol.io) server written in Kotlin that exposes live currency exchange rate tools to any MCP-compatible AI client (Claude Desktop, Claude Code, etc.).

Exchange rates are fetched from [open.er-api.com](https://open.er-api.com) — **free, no API key required**, updated daily.

---

## Tools

| Tool                | Description                                         | Parameters                                        |
|---------------------|-----------------------------------------------------|---------------------------------------------------|
| `get_exchange_rate` | Get the rate between two currencies                 | `from` (string), `to` (string)                    |
| `convert_currency`  | Convert an amount from one currency to another      | `from` (string), `to` (string), `amount` (number) |
| `list_currencies`   | List all ~170 supported currencies with their rates | `base` (string, default: `USD`)                   |

### Example prompts

> "What is the exchange rate from USD to JPY?"

> "Convert 500 EUR to GBP"

> "List all currencies relative to INR"

---

## Prerequisites

| Tool   | Version | Install                               |
|--------|---------|---------------------------------------|
| JDK 21 | 21+     | `winget install Microsoft.OpenJDK.21` |

> **Note:** The project uses the Gradle wrapper (`gradlew.bat`) — no separate Gradle installation needed.

---

## Build

```bash
# Windows
.\gradlew.bat shadowJar

# macOS / Linux
./gradlew shadowJar
```

The fat JAR is produced at:

```
build/libs/exchange-rate-mcp.jar
```

---

## Run (manual test)

```bash
java -jar build/libs/exchange-rate-mcp.jar
```

The server speaks [JSON-RPC 2.0](https://www.jsonrpc.org/specification) over **stdio**. You can paste raw MCP messages to test it, but it is easier to connect it to an MCP client (see below).

---

## Connect to Claude Desktop

1. Open (or create) the Claude Desktop config file:
   - **Windows:** `%APPDATA%\Claude\claude_desktop_config.json`
   - **macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`

2. Add the server entry:

```json
{
  "mcpServers": {
    "exchange-rate": {
      "command": "java",
      "args": [
        "-jar",
        "C:\\Users\\Vivart\\Documents\\develop\\exchange-rate-mcp\\build\\libs\\exchange-rate-mcp.jar"
      ]
    }
  }
}
```

3. Restart Claude Desktop — the tools will appear automatically.

---

## Connect to Claude Code

### 1. Build the JAR (if not already done)

```bat
.\gradlew.bat shadowJar
```

### 2. Register the MCP server

**Windows (PowerShell) — use `cmd` to avoid argument-parsing issues with `-jar`:**

```powershell
cmd /c "claude mcp add exchange-rate -- java -jar C:\Users\Vivart\Documents\develop\exchange-rate-mcp\build\libs\exchange-rate-mcp.jar"
```

Replace the path with the absolute path to the JAR on your machine. Using a relative path does **not** work because Claude Code launches the server from a different working directory.

**macOS / Linux:**

```bash
claude mcp add exchange-rate -- java -jar /absolute/path/to/exchange-rate-mcp.jar
```

### 3. Verify the server is registered

```bash
claude mcp list
```

You should see `exchange-rate` in the output.

### 4. Restart Claude Code

The MCP server is loaded at startup. Restart Claude Code (or open a new session) for the tools to appear.

### 5. Test it

In a Claude Code session, ask:

> "What is the exchange rate from USD to EUR?"

Claude will use the `get_exchange_rate` tool automatically.

---

## Project Structure

```
exchange-rate-mcp/
├── build.gradle.kts                        # Gradle build (Kotlin DSL)
├── settings.gradle.kts
├── gradle/wrapper/                         # Gradle wrapper (no install needed)
└── src/main/kotlin/com/exchange/
    │
    ├── Main.kt                             # Entry point — builds server, starts stdio transport
    │
    ├── api/
    │   └── ExchangeRateClient.kt           # HTTP client + fetchRates()
    │
    ├── model/
    │   └── ExchangeRateResponse.kt         # Serializable response data class
    │
    └── tools/
        ├── ExchangeRateTools.kt            # registerExchangeRateTools() — wires all tools to the server
        ├── ToolHelpers.kt                  # Shared successResult() / errorResult() helpers
        ├── GetExchangeRateTool.kt          # get_exchange_rate tool
        ├── ConvertCurrencyTool.kt          # convert_currency tool
        └── ListCurrenciesTool.kt           # list_currencies tool
```

---

## Key Dependencies

| Library                                                                   | Version | Purpose                                                 |
|---------------------------------------------------------------------------|---------|---------------------------------------------------------|
| [MCP Kotlin SDK](https://github.com/modelcontextprotocol/kotlin-sdk)      | 0.12.0  | MCP server framework                                    |
| [Ktor Client](https://ktor.io)                                            | 3.2.0   | HTTP calls to exchange rate API                         |
| [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)  | 1.9.0   | JSON deserialization                                    |
| [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines)        | 1.10.2  | Async / suspend functions                               |
| [Logback](https://logback.qos.ch)                                         | 1.5.18  | Logging (writes to stderr to keep stdout clean for MCP) |

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
  ExchangeRateClient
        │  HTTPS GET
        ▼
  open.er-api.com/v6/latest/{BASE}
```

1. Claude calls a tool (e.g. `convert_currency`).
2. The MCP server receives the JSON-RPC request over stdin.
3. `ExchangeRateClient.fetchRates()` fetches the latest rates for the requested base currency.
4. The result is formatted and returned to Claude as a text response.

---

## Adding a New Tool

1. Create `src/main/kotlin/com/exchange/tools/MyNewTool.kt`:

```kotlin
package com.exchange.tools

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*

internal fun Server.addMyNewTool() {
    addTool(
        name = "my_new_tool",
        description = "What this tool does.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("param", buildJsonObject {
                    put("type", "string")
                    put("description", "Description of param")
                })
            },
            required = listOf("param"),
        ),
    ) { request ->
        val param = request.arguments?.get("param")?.jsonPrimitive?.content
            ?: return@addTool errorResult("Missing 'param'")
        successResult("Result for $param")
    }
}
```

2. Register it in `ExchangeRateTools.kt`:

```kotlin
fun Server.registerExchangeRateTools() {
    addGetExchangeRateTool()
    addConvertCurrencyTool()
    addListCurrenciesTool()
    addMyNewTool()          // ← add this line
}
```

3. Rebuild: `.\gradlew.bat shadowJar`
