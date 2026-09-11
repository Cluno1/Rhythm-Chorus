package io.github.cluno1.sonorus.features.catalog.data.remote

import io.github.cluno1.sonorus.features.catalog.data.CatalogCredentialsStore
import io.github.cluno1.sonorus.features.catalog.domain.CatalogPlaybackPolicy
import com.google.gson.GsonBuilder
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

internal class CatalogApiClient(
    serverUrl: String,
    legacyToken: String?,
    credentials: CatalogCredentialsStore?,
) {
    private val origin = (CatalogEndpoint.normalize(serverUrl) + "/").toHttpUrl()
    private val legacyAuth = legacyToken?.trim()?.also { require(it.isNotEmpty()) }
    private val deviceAuth = credentials?.loadDevice()?.let {
        CatalogDeviceAuthClient(serverUrl, credentials)
    }

    constructor(serverUrl: String, token: String) : this(serverUrl, token, null)

    constructor(serverUrl: String, credentials: CatalogCredentialsStore) : this(
        serverUrl,
        credentials.loadToken().takeIf { credentials.loadDevice() == null },
        credentials,
    )

    private val httpClient = OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .addInterceptor(Interceptor { chain ->
                    val request = chain.request()
                    val precomputedHash = request.header(INTERNAL_CONTENT_SHA256)
                    val builder = request.newBuilder().removeHeader(INTERNAL_CONTENT_SHA256)
                    if (request.url.encodedPath != "/healthz" && CatalogEndpoint.sameOrigin(request.url, origin)) {
                        if (deviceAuth != null) {
                            deviceAuth.proof(request, precomputedHash).forEach(builder::header)
                        } else {
                            builder.header("Authorization", "Bearer ${requireNotNull(legacyAuth)}")
                        }
                    }
                    chain.proceed(builder.build())
                })
                .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(origin)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
        .build()

    val api: CatalogApi = retrofit.create(CatalogApi::class.java)
    val lyricsWriteApi: CatalogLyricsWriteApi = retrofit.create(CatalogLyricsWriteApi::class.java)
    val chorusApi: CatalogChorusApi = retrofit.create(CatalogChorusApi::class.java)

    fun uploadChorusToSignedUrl(url: String, file: File, mediaType: String) {
        require(CatalogPlaybackPolicy.isSignedObjectStoreUrl(url)) {
            "chorus upload target is not a signed COS URL"
        }
        val request = Request.Builder()
            .url(url)
            .put(file.asRequestBody(mediaType.toMediaType()))
            .build()
        httpClient.newCall(request).execute().use { response ->
            require(response.isSuccessful) { "COS upload failed (${response.code})" }
        }
    }

    fun resolveAssetUrl(relativeOrAbsolute: String): HttpUrl {
        val resolved = origin.resolve(relativeOrAbsolute) ?: throw IllegalArgumentException("asset URL is invalid")
        require(CatalogEndpoint.sameOrigin(origin, resolved)) { "asset URL changed origin" }
        require(resolved.encodedUsername.isEmpty() && resolved.encodedPassword.isEmpty()) { "asset URL contains userinfo" }
        require(resolved.fragment == null) { "asset URL contains fragment" }
        return resolved
    }

    private companion object {
        const val INTERNAL_CONTENT_SHA256 = "X-Sonorus-Content-SHA256"
    }
}
