package com.vivart

import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import kotlinx.serialization.json.*

fun buildServer(): Server {
    val server = Server(
        serverInfo = Implementation(name = "exchange-rate-mcp", version = "1.0.0"),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = false),
            ),
        ),
    )

    server.addTool(
        name = "get_exchange_rate",
        description = "Get the current exchange rate between two currencies.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("from") {
                    put("type", "string")
                    put("description", "Source currency code, e.g. USD")
                }
                putJsonObject("to") {
                    put("type", "string")
                    put("description", "Target currency code, e.g. EUR")
                }
            },
            required = listOf("from", "to"),
        ),
    ) { req ->
        val from = req.arguments?.get("from")?.jsonPrimitive?.content
            ?: return@addTool toolError("Missing parameter: from")
        val to = req.arguments?.get("to")?.jsonPrimitive?.content
            ?: return@addTool toolError("Missing parameter: to")
        getExchangeRate(from, to)
    }

    server.addTool(
        name = "convert_currency",
        description = "Convert a monetary amount from one currency to another using the live exchange rate.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("from") {
                    put("type", "string")
                    put("description", "Source currency code, e.g. USD")
                }
                putJsonObject("to") {
                    put("type", "string")
                    put("description", "Target currency code, e.g. EUR")
                }
                putJsonObject("amount") {
                    put("type", "number")
                    put("description", "Amount to convert (must be > 0)")
                }
            },
            required = listOf("from", "to", "amount"),
        ),
    ) { req ->
        val from = req.arguments?.get("from")?.jsonPrimitive?.content
            ?: return@addTool toolError("Missing parameter: from")
        val to = req.arguments?.get("to")?.jsonPrimitive?.content
            ?: return@addTool toolError("Missing parameter: to")
        val amount = req.arguments?.get("amount")?.jsonPrimitive?.doubleOrNull
            ?: return@addTool toolError("Missing or invalid parameter: amount")
        convertCurrency(from, to, amount)
    }

    server.addTool(
        name = "list_currencies",
        description = "List all currencies supported by the Frankfurter API.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {},
            required = emptyList(),
        ),
    ) { _ ->
        listCurrenciesTool()
    }

    return server
}

internal suspend fun getExchangeRate(from: String, to: String): CallToolResult =
    runCatching { FrankfurterClient.getRate(from.uppercase(), to.uppercase()) }
        .fold(
            onSuccess = { r -> toolSuccess("1 ${r.base} = ${r.rate} ${r.quote} (date: ${r.date})") },
            onFailure = { toolError("API error: ${it.message}") },
        )

internal suspend fun convertCurrency(from: String, to: String, amount: Double): CallToolResult =
    runCatching { FrankfurterClient.convert(from.uppercase(), to.uppercase(), amount) }
        .fold(
            onSuccess = { r ->
                toolSuccess("$amount ${r.base} = ${r.result} ${r.quote}  (rate: ${r.rate}, date: ${r.date})")
            },
            onFailure = { toolError("API error: ${it.message}") },
        )

internal suspend fun listCurrenciesTool(): CallToolResult =
    runCatching { FrankfurterClient.listCurrencies() }
        .fold(
            onSuccess = { currencies ->
                val text = currencies.entries
                    .sortedBy { it.key }
                    .joinToString("\n") { (code, name) -> "$code  $name" }
                toolSuccess(text)
            },
            onFailure = { toolError("API error: ${it.message}") },
        )

internal fun toolSuccess(text: String) = CallToolResult(content = listOf(TextContent(text)))
internal fun toolError(message: String) = CallToolResult(content = listOf(TextContent(message)), isError = true)
