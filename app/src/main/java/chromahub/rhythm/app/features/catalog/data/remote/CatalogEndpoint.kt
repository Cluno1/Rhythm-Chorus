package chromahub.rhythm.app.features.catalog.data.remote

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object CatalogEndpoint {
    fun normalize(raw: String): String {
        val value = raw.trim().trimEnd('/')
        val parsed = value.toHttpUrlOrNull() ?: throw IllegalArgumentException("服务器地址无效")
        require(parsed.scheme == "http" || parsed.scheme == "https") { "只支持 HTTP 或 HTTPS" }
        require(parsed.username.isEmpty() && parsed.password.isEmpty()) { "服务器地址不能包含用户信息" }
        require(parsed.query == null) { "服务器地址不能包含查询参数" }
        require(parsed.fragment == null) { "服务器地址不能包含片段" }
        require(parsed.encodedPath == "/") { "服务器地址不能包含路径" }
        return parsed.newBuilder().encodedPath("/").build().toString().trimEnd('/')
    }

    fun sameOrigin(left: HttpUrl, right: HttpUrl): Boolean =
        left.scheme.equals(right.scheme, ignoreCase = true) &&
            left.host.equals(right.host, ignoreCase = true) &&
            left.port == right.port
}
