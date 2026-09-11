package io.github.cluno1.sonorus.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrclibHeadersInterceptorTest {
    private val server = MockWebServer()

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `identifies Sonorus to LRCLIB exactly once`() {
        server.enqueue(MockResponse().setBody("[]"))
        val client = OkHttpClient.Builder()
            .addInterceptor(NetworkClient.lrclibHeadersInterceptor("1.2.3-test"))
            .build()

        client.newCall(Request.Builder().url(server.url("/api/search?q=test")).build())
            .execute()
            .close()

        val request = server.takeRequest()
        assertEquals(
            listOf("Sonorus/1.2.3-test (https://github.com/Cluno1/Rhythm-Chorus)"),
            request.headers.values("User-Agent"),
        )
        assertEquals("application/json", request.headers["Accept"])
    }

    @Test
    fun `keeps an explicitly supplied User-Agent`() {
        server.enqueue(MockResponse().setBody("[]"))
        val client = OkHttpClient.Builder()
            .addInterceptor(NetworkClient.lrclibHeadersInterceptor("ignored"))
            .build()

        client.newCall(
            Request.Builder()
                .url(server.url("/api/search?q=test"))
                .header("User-Agent", "ExplicitClient/7")
                .build(),
        ).execute().close()

        assertEquals(
            listOf("ExplicitClient/7"),
            server.takeRequest().headers.values("User-Agent"),
        )
    }

    @Test
    fun `honors Retry-After before retrying a rate limited request`() {
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "2"))
        server.enqueue(MockResponse().setBody("[]"))
        val delays = mutableListOf<Long>()
        val client = OkHttpClient.Builder()
            .addInterceptor(NetworkClient.lrclibHeadersInterceptor("1.2.3-test"))
            .addInterceptor(NetworkClient.lrclibRetryAfterInterceptor(sleep = delays::add))
            .build()

        val response = client.newCall(
            Request.Builder().url(server.url("/api/search?q=test")).build(),
        ).execute()

        response.use { assertTrue(it.isSuccessful) }
        assertEquals(2, server.requestCount)
        assertEquals(listOf(2_000L), delays)
    }
}
