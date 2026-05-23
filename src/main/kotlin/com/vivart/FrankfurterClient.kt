package com.vivart

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RateResponse(
    val date: String,
    val base: String,
    val quote: String,
    val rate: Double,
)

@Serializable
data class CurrencyInfo(
    @SerialName("iso_code") val isoCode: String,
    val name: String,
)

data class ConversionResult(
    val date: String,
    val base: String,
    val quote: String,
    val amount: Double,
    val rate: Double,
    val result: Double,
)

object FrankfurterClient {

    private const val BASE_URL = "https://api.frankfurter.dev/v2"

    internal var http: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun getRate(from: String, to: String): RateResponse =
        http.get("$BASE_URL/rate/$from/$to").body()

    suspend fun convert(from: String, to: String, amount: Double): ConversionResult {
        val r = getRate(from, to)
        return ConversionResult(r.date, r.base, r.quote, amount, r.rate, r.rate * amount)
    }

    suspend fun listCurrencies(): Map<String, String> {
        val list: List<CurrencyInfo> = http.get("$BASE_URL/currencies").body()
        return list.associate { it.isoCode to it.name }
    }
}
