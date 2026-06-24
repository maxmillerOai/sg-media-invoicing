package org.example.project.data

import io.ktor.client.HttpClient

internal actual fun createHttpClient(): HttpClient = HttpClient { installJson() }
