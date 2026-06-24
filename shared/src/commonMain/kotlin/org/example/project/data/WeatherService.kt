package org.example.project.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Current weather for a resolved city, from Open-Meteo (no API key required). */
data class WeatherInfo(
    val city: String,
    val country: String?,
    val temperatureC: Double,
    val windKmh: Double,
    val code: Int,
    val isDay: Boolean,
    val forecast: List<DailyForecast> = emptyList(),
) {
    val label: String get() = weatherLabel(code)
    val emoji: String get() = weatherEmoji(code, isDay)
}

/** One upcoming day in the short forecast strip. [date] is ISO yyyy-MM-dd. */
data class DailyForecast(
    val date: String,
    val code: Int,
    val tempMaxC: Double,
    val tempMinC: Double,
) {
    val emoji: String get() = weatherEmoji(code, true)
}

@Serializable
private data class GeoResponse(val results: List<GeoResult>? = null)

@Serializable
private data class GeoResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
)

@Serializable
private data class ForecastResponse(val current: CurrentWeather? = null, val daily: DailyBlock? = null)

@Serializable
private data class CurrentWeather(
    @SerialName("temperature_2m") val temperature: Double = 0.0,
    @SerialName("wind_speed_10m") val wind: Double = 0.0,
    @SerialName("weather_code") val weatherCode: Int = 0,
    @SerialName("is_day") val isDay: Int = 1,
)

@Serializable
private data class DailyBlock(
    val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int> = emptyList(),
    @SerialName("temperature_2m_max") val tMax: List<Double> = emptyList(),
    @SerialName("temperature_2m_min") val tMin: List<Double> = emptyList(),
)

/**
 * Resolves a city name to coordinates (Open-Meteo geocoding) and fetches current weather.
 * Keyless and free. Returns a [Result] so the UI can show a friendly error offline.
 */
class WeatherService(private val client: HttpClient = createHttpClient()) {

    suspend fun fetch(city: String): Result<WeatherInfo> = withContext(Dispatchers.Default) {
        runCatching {
            val geo: GeoResponse = client.get("https://geocoding-api.open-meteo.com/v1/search") {
                parameter("name", city)
                parameter("count", 1)
                parameter("language", "fr")
                parameter("format", "json")
            }.body()
            val g = geo.results?.firstOrNull() ?: error("Ville introuvable : $city")
            val forecast: ForecastResponse = client.get("https://api.open-meteo.com/v1/forecast") {
                parameter("latitude", g.latitude)
                parameter("longitude", g.longitude)
                parameter("current", "temperature_2m,weather_code,wind_speed_10m,is_day")
                parameter("daily", "weather_code,temperature_2m_max,temperature_2m_min")
                parameter("forecast_days", 4)
                parameter("timezone", "auto")
            }.body()
            val cur = forecast.current ?: error("Météo indisponible")
            val days = forecast.daily?.let { d ->
                d.time.indices.map { i ->
                    DailyForecast(
                        date = d.time.getOrElse(i) { "" },
                        code = d.weatherCode.getOrElse(i) { 0 },
                        tempMaxC = d.tMax.getOrElse(i) { 0.0 },
                        tempMinC = d.tMin.getOrElse(i) { 0.0 },
                    )
                }
            } ?: emptyList()
            WeatherInfo(
                city = g.name,
                country = g.country,
                temperatureC = cur.temperature,
                windKmh = cur.wind,
                code = cur.weatherCode,
                isDay = cur.isDay == 1,
                forecast = days.drop(1).take(3), // next 3 days
            )
        }
    }
}

/** WMO weather-interpretation code → short French label. */
fun weatherLabel(code: Int): String = when (code) {
    0 -> "Ciel dégagé"
    1 -> "Plutôt dégagé"
    2 -> "Partiellement nuageux"
    3 -> "Couvert"
    45, 48 -> "Brouillard"
    51, 53, 55 -> "Bruine"
    56, 57 -> "Bruine verglaçante"
    61, 63, 65 -> "Pluie"
    66, 67 -> "Pluie verglaçante"
    71, 73, 75 -> "Neige"
    77 -> "Grains de neige"
    80, 81, 82 -> "Averses"
    85, 86 -> "Averses de neige"
    95 -> "Orage"
    96, 99 -> "Orage et grêle"
    else -> "—"
}

/** WMO code → emoji (day/night aware for clear/partly cloudy). */
fun weatherEmoji(code: Int, isDay: Boolean): String = when (code) {
    0 -> if (isDay) "☀️" else "🌙"
    1, 2 -> if (isDay) "⛅" else "☁️"
    3 -> "☁️"
    45, 48 -> "🌫️"
    in 51..57 -> "🌦️"
    in 61..67 -> "🌧️"
    in 71..77 -> "❄️"
    in 80..82 -> "🌧️"
    85, 86 -> "🌨️"
    95, 96, 99 -> "⛈️"
    else -> "🌡️"
}
