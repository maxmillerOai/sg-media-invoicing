package org.example.project.data

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Platform HTTP engine (CIO on desktop, OkHttp on Android, Darwin on iOS). */
internal expect fun createHttpClient(): HttpClient

internal val networkJson = Json { ignoreUnknownKeys = true; isLenient = true }

internal fun HttpClientConfig<*>.installJson() {
    install(ContentNegotiation) { json(networkJson) }
}
