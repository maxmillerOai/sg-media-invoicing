package org.example.project.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

// OkHttp (blocking I/O) is used on desktop instead of CIO: CIO relies on a JDK NIO Selector,
// whose AF_UNIX self-pipe can fail in sandboxed/restricted environments. OkHttp also honours
// system proxy settings.
internal actual fun createHttpClient(): HttpClient = HttpClient(OkHttp) { installJson() }
