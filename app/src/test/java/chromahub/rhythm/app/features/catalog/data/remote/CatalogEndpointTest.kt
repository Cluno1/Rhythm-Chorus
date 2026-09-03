package chromahub.rhythm.app.features.catalog.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogEndpointTest {
    @Test
    fun normalizesOriginOnlyServerUrl() {
        assertEquals("http://10.88.0.1:8010", CatalogEndpoint.normalize(" http://10.88.0.1:8010/ "))
        assertEquals("https://music.example", CatalogEndpoint.normalize("https://music.example"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUserInfo() {
        CatalogEndpoint.normalize("https://token@music.example")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPath() {
        CatalogEndpoint.normalize("https://music.example/api")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsQuery() {
        CatalogEndpoint.normalize("https://music.example/?token=secret")
    }

    @Test
    fun comparesSchemeHostAndEffectivePort() {
        assertTrue(CatalogEndpoint.sameOrigin("https://music.example/a".toHttpUrl(), "https://MUSIC.example/b".toHttpUrl()))
        assertFalse(CatalogEndpoint.sameOrigin("http://music.example/a".toHttpUrl(), "https://music.example/a".toHttpUrl()))
        assertFalse(CatalogEndpoint.sameOrigin("https://music.example:8443/a".toHttpUrl(), "https://music.example/a".toHttpUrl()))
    }
}
