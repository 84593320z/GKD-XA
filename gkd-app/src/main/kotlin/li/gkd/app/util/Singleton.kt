package li.gkd.app.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import java.util.concurrent.TimeUnit
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.text.Collator
import java.util.Locale


val json by lazy {
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
}

val keepNullJson by lazy {
    Json(from = json) {
        explicitNulls = true
    }
}

val client by lazy {
    HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json, ContentType.Any)
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
            requestTimeoutMillis = 120_000
        }
        engine {
            clientCacheSize = 8
            config {
                connectTimeout(15, TimeUnit.SECONDS)
                readTimeout(30, TimeUnit.SECONDS)
                writeTimeout(30, TimeUnit.SECONDS)
            }
        }
    }
}

val collator by lazy { Collator.getInstance(Locale.CHINESE)!! }
