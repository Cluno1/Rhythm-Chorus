package chromahub.rhythm.app.features.catalog.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class CatalogApiClientTest {
    private val server = MockWebServer()

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun sendsTokenOnlyToAuthenticatedCatalogRequests() = runBlocking {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("{\"status\":\"ok\"}"))
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("{\"items\":[],\"next_cursor\":null}"))
        val client = CatalogApiClient(server.url("/").toString(), "top-secret")

        client.api.health()
        client.api.works(limit = 1)

        assertNull(server.takeRequest().headers["Authorization"])
        assertEquals("Bearer top-secret", server.takeRequest().headers["Authorization"])
    }

    @Test
    fun refusesCrossOriginAssetUrl() {
        val client = CatalogApiClient(server.url("/").toString(), "token")
        assertThrows(IllegalArgumentException::class.java) {
            client.resolveAssetUrl("https://evil.example/v2/assets/11111111-1111-4111-8111-111111111111/content")
        }
    }

    @Test
    fun doesNotFollowRedirects() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "https://evil.example/collect"))
        val response = CatalogApiClient(server.url("/").toString(), "token").api.works(limit = 1)
        assertEquals(302, response.code())
        assertEquals(1, server.requestCount)
    }
}
