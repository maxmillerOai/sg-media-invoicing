package org.example.project.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

internal actual fun createHttpClient(): HttpClient = HttpClient(Darwin) { installJson() }
