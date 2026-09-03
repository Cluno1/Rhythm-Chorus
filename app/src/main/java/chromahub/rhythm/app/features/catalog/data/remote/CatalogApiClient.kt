package chromahub.rhythm.app.features.catalog.data.remote

import com.google.gson.GsonBuilder
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

internal class CatalogApiClient(
    serverUrl: String,
    token: String,
) {
    private val origin = (CatalogEndpoint.normalize(serverUrl) + "/").toHttpUrl()
    private val auth = token.trim().also { require(it.isNotEmpty()) }

    val api: CatalogApi = Retrofit.Builder()
        .baseUrl(origin)
        .client(
            OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(Interceptor { chain ->
                    val request = chain.request()
                    val builder = request.newBuilder()
                    if (request.url.encodedPath != "/healthz" && CatalogEndpoint.sameOrigin(request.url, origin)) {
                        builder.header("Authorization", "Bearer $auth")
                    }
                    chain.proceed(builder.build())
                })
                .build(),
        )
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
        .build()
        .create(CatalogApi::class.java)

    fun resolveAssetUrl(relativeOrAbsolute: String): HttpUrl {
        val resolved = origin.resolve(relativeOrAbsolute) ?: throw IllegalArgumentException("asset URL is invalid")
        require(CatalogEndpoint.sameOrigin(origin, resolved)) { "asset URL changed origin" }
        require(resolved.encodedUsername.isEmpty() && resolved.encodedPassword.isEmpty()) { "asset URL contains userinfo" }
        require(resolved.fragment == null) { "asset URL contains fragment" }
        return resolved
    }
}
