package org.sjbtimdan.linden.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.FxRates

@Serializable
data class FrankfurterRates(
    val base: String,
    val date: String,
    val rates: Map<String, Double>,
)

private val json = Json { ignoreUnknownKeys = true }

fun parseFxRatesResponse(text: String): FxRates {
    val parsed = json.decodeFromString<FrankfurterRates>(text)
    return FxRates(
        base = Currency.fromCode(parsed.base),
        date = parsed.date,
        rates = parsed.rates.mapKeys { (code, _) -> Currency.fromCode(code) },
    )
}

class FxRatesFetcher(
    private val client: HttpClient = HttpClient(),
) {
    suspend fun fetchLatestRates(base: Currency, symbols: List<Currency>): FxRates {
        val response = client.get(FRANKFURTER_LATEST_URL) {
            parameter("base", base.name)
            parameter("symbols", symbols.joinToString(",") { it.name })
        }
        check(response.status.isSuccess()) { "Frankfurter error: ${response.status}" }
        return parseFxRatesResponse(response.bodyAsText())
    }
}

private const val FRANKFURTER_LATEST_URL = "https://api.frankfurter.dev/v1/latest"
