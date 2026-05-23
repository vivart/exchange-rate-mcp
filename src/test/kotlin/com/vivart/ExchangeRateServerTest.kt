package com.vivart

import io.mockk.*
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ExchangeRateServerTest {

    @BeforeTest
    fun setup() {
        mockkObject(FrankfurterClient)
    }

    @AfterTest
    fun teardown() {
        unmockkObject(FrankfurterClient)
    }

    // ── getExchangeRate ───────────────────────────────────────────────────────

    @Test
    fun `getExchangeRate returns formatted rate string`() = runTest {
        coEvery { FrankfurterClient.getRate("USD", "EUR") } returns RateResponse(
            date = "2024-01-15", base = "USD", quote = "EUR", rate = 0.92,
        )
        val result = getExchangeRate("USD", "EUR")
        assertFalse(result.isError == true)
        assertEquals("1 USD = 0.92 EUR (date: 2024-01-15)", (result.content[0] as TextContent).text)
    }

    @Test
    fun `getExchangeRate normalises currency codes to uppercase`() = runTest {
        coEvery { FrankfurterClient.getRate("USD", "EUR") } returns RateResponse(
            date = "2024-01-15", base = "USD", quote = "EUR", rate = 0.92,
        )
        val result = getExchangeRate("usd", "eur")
        assertFalse(result.isError == true)
        coVerify { FrankfurterClient.getRate("USD", "EUR") }
    }

    @Test
    fun `getExchangeRate returns error on API failure`() = runTest {
        coEvery { FrankfurterClient.getRate(any(), any()) } throws RuntimeException("Network error")
        val result = getExchangeRate("USD", "EUR")
        assertTrue(result.isError == true)
        assertTrue((result.content[0] as TextContent).text.contains("API error"))
    }

    // ── convertCurrency ──────────────────────────────────────────────────────

    @Test
    fun `convertCurrency returns formatted conversion string`() = runTest {
        coEvery { FrankfurterClient.convert("GBP", "INR", 10.0) } returns ConversionResult(
            date = "2024-01-15", base = "GBP", quote = "INR", amount = 10.0, rate = 106.05, result = 1060.5,
        )
        val result = convertCurrency("GBP", "INR", 10.0)
        assertFalse(result.isError == true)
        assertEquals(
            "10.0 GBP = 1060.5 INR  (rate: 106.05, date: 2024-01-15)",
            (result.content[0] as TextContent).text,
        )
    }

    @Test
    fun `convertCurrency normalises currency codes to uppercase`() = runTest {
        coEvery { FrankfurterClient.convert("GBP", "INR", 10.0) } returns ConversionResult(
            date = "2024-01-15", base = "GBP", quote = "INR", amount = 10.0, rate = 106.05, result = 1060.5,
        )
        val result = convertCurrency("gbp", "inr", 10.0)
        assertFalse(result.isError == true)
        coVerify { FrankfurterClient.convert("GBP", "INR", 10.0) }
    }

    @Test
    fun `convertCurrency returns error on API failure`() = runTest {
        coEvery { FrankfurterClient.convert(any(), any(), any()) } throws RuntimeException("Timeout")
        val result = convertCurrency("USD", "EUR", 50.0)
        assertTrue(result.isError == true)
        assertTrue((result.content[0] as TextContent).text.contains("API error"))
    }

    // ── listCurrenciesTool ────────────────────────────────────────────────────

    @Test
    fun `listCurrenciesTool returns currencies sorted alphabetically`() = runTest {
        coEvery { FrankfurterClient.listCurrencies() } returns mapOf(
            "USD" to "US Dollar",
            "AED" to "UAE Dirham",
            "EUR" to "Euro",
        )
        val result = listCurrenciesTool()
        assertFalse(result.isError == true)
        val lines = (result.content[0] as TextContent).text.lines()
        assertEquals("AED  UAE Dirham", lines[0])
        assertEquals("EUR  Euro", lines[1])
        assertEquals("USD  US Dollar", lines[2])
    }

    @Test
    fun `listCurrenciesTool returns error on API failure`() = runTest {
        coEvery { FrankfurterClient.listCurrencies() } throws RuntimeException("Server down")
        val result = listCurrenciesTool()
        assertTrue(result.isError == true)
        assertTrue((result.content[0] as TextContent).text.contains("API error"))
    }

    // ── buildServer ───────────────────────────────────────────────────────────

    @Test
    fun `buildServer creates server successfully`() {
        val server = buildServer()
        assertNotNull(server)
    }
}
