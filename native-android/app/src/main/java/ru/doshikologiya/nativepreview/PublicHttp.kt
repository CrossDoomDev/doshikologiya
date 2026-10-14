package ru.doshikologiya.nativepreview

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI

internal object PublicHttp {
    const val ORIGIN = "https://crossdoomdev.github.io/doshikologiya/"

    fun bytes(url: String, maxBytes: Int, timeout: Int = 30_000): ByteArray {
        val uri = URI(url)
        require(uri.scheme == "https" && uri.host == "crossdoomdev.github.io" &&
            uri.rawUserInfo == null && uri.port == -1 &&
            uri.normalize().path.startsWith("/doshikologiya/"))
        val request = uri.toURL().openConnection() as HttpURLConnection
        request.connectTimeout = timeout
        request.readTimeout = timeout
        request.instanceFollowRedirects = false
        request.useCaches = false
        try {
            check(request.responseCode == 200) { "HTTP ${request.responseCode}" }
            require(request.contentLengthLong <= maxBytes)
            val output = ByteArrayOutputStream()
            request.inputStream.use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= maxBytes) { "Превышен размер файла" }
                    output.write(buffer, 0, count)
                }
            }
            return output.toByteArray()
        } finally { request.disconnect() }
    }
}
