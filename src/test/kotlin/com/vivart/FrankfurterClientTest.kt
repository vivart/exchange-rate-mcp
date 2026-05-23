package com.vivart

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.*

class FrankfurterClientTest {

    private fun mockHttp(body: String): HttpClient =
        HttpClient(MockEngine {
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

    @Test
    fun `getRate calls correct v2 path and parses response`() = runTest {
        var capturedUrl = ""
        FrankfurterClient.http = HttpClient(MockEngine { request ->
            capturedUrl = request.url.toString()
            respond(
                content = """{"date":"2024-01-15","base":"USD","quote":"EUR","rate":0.92}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val result = FrankfurterClient.getRate("USD", "EUR")

        assertTrue(capturedUrl.contains("/v2/rate/USD/EUR"), "Expected /v2/rate/USD/EUR in $capturedUrl")
        assertEquals("USD", result.base)
        assertEquals("EUR", result.quote)
        assertEquals(0.92, result.rate)
        assertEquals("2024-01-15", result.date)
    }

    @Test
    fun `convert calls getRate and computes result correctly`() = runTest {
        FrankfurterClient.http = mockHttp(
            """{"date":"2024-01-15","base":"GBP","quote":"INR","rate":105.0}"""
        )

        val result = FrankfurterClient.convert("GBP", "INR", 100.0)

        assertEquals("GBP", result.base)
        assertEquals("INR", result.quote)
        assertEquals(100.0, result.amount)
        assertEquals(105.0, result.rate)
        assertEquals(10500.0, result.result, absoluteTolerance = 0.001)
        assertEquals("2024-01-15", result.date)
    }

    @Test
    fun `listCurrencies calls v2 currencies endpoint and maps array to code-name pairs`() = runTest {
        var capturedUrl = ""
        FrankfurterClient.http = HttpClient(MockEngine { request ->
            capturedUrl = request.url.toString()
            respond(
                content = """[
                    {"iso_code":"USD","iso_numeric":"840","name":"US Dollar","symbol":"$","start_date":"1946-01-01","end_date":"2026-01-01"},
                    {"iso_code":"EUR","iso_numeric":"978","name":"Euro","symbol":"€","start_date":"1999-01-04","end_date":"2026-01-01"},
                    {"iso_code":"GBP","iso_numeric":"826","name":"British Pound","symbol":"£","start_date":"1946-01-01","end_date":"2026-01-01"}
                ]""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val result = FrankfurterClient.listCurrencies()

        assertTrue(capturedUrl.contains("/v2/currencies"), "Expected /v2/currencies in $capturedUrl")
        assertEquals("US Dollar", result["USD"])
        assertEquals("Euro", result["EUR"])
        assertEquals("British Pound", result["GBP"])
        assertEquals(3, result.size)
    }
}
