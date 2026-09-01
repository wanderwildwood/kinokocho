package com.wanderwildwood.kinokocho.net

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketException
import kotlin.concurrent.thread

/**
 * A very small HTTP server, so the client can be tested against a real socket.
 *
 * Written out rather than taken off a shelf. `com.sun.net.httpserver` is not on the
 * Android unit-test classpath — the same absence as `javax.imageio`, and it fails as an
 * unresolved reference rather than as anything that explains itself — and the alternative
 * was adding MockWebServer to an app whose whole networking layer is
 * `HttpURLConnection` precisely so that it has no such dependency. Fifty lines of socket
 * is the cheaper of the two.
 *
 * It speaks only the sliver of HTTP/1.1 that [INatClient] speaks: a request line, headers,
 * a `Content-Length` body, and one response with `Connection: close`. Closing every
 * connection is deliberate — it keeps keep-alive out of the tests, so each one starts from
 * the same place.
 */
class TestHttpServer {

    /** What the last request carried, for a test to assert on. */
    @Volatile var method: String? = null
    @Volatile var path: String? = null
    @Volatile var headers: Map<String, String> = emptyMap()
    @Volatile var body: ByteArray = ByteArray(0)
    @Volatile var hits = 0

    /** What to answer with. */
    @Volatile var status = 200
    @Volatile var response = "{}"
    @Volatile var location: String? = null

    private val socket = ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))
    private val loop: Thread

    val base: String get() = "http://127.0.0.1:${socket.localPort}"

    init {
        loop = thread(isDaemon = true) {
            while (!socket.isClosed) {
                try {
                    socket.accept().use { client ->
                        val input = client.getInputStream()
                        val request = readRequest(input) ?: return@use
                        hits++
                        method = request.method
                        path = request.path
                        headers = request.headers
                        body = request.body

                        val bytes = response.toByteArray()
                        val head = buildString {
                            append("HTTP/1.1 $status ${reason(status)}\r\n")
                            append("Content-Length: ${bytes.size}\r\n")
                            location?.let { append("Location: $it\r\n") }
                            append("Connection: close\r\n\r\n")
                        }
                        client.getOutputStream().apply {
                            write(head.toByteArray())
                            write(bytes)
                            flush()
                        }
                    }
                } catch (_: SocketException) {
                    // The socket was closed while waiting, which is how this stops.
                } catch (_: Exception) {
                    // A test tearing down mid-request is not a failure of the test.
                }
            }
        }
    }

    fun stop() {
        socket.close()
        loop.join(2_000)
    }

    private class Request(
        val method: String,
        val path: String,
        val headers: Map<String, String>,
        val body: ByteArray,
    )

    /**
     * Reads bytes rather than lines.
     *
     * A `BufferedReader` over the stream would decode the body as text, and the body here
     * can be a photograph. So the head is read one byte at a time to the blank line, and
     * the rest is taken raw.
     */
    private fun readRequest(input: InputStream): Request? {
        val head = ByteArrayOutputStream()
        var last4 = 0
        while (true) {
            val b = input.read()
            if (b == -1) return null
            head.write(b)
            last4 = (last4 shl 8) or b
            if (last4 == 0x0D0A0D0A) break
        }

        val lines = head.toByteArray().toString(Charsets.ISO_8859_1).split("\r\n")
        val start = lines.firstOrNull()?.split(" ") ?: return null
        if (start.size < 2) return null

        val headers = lines.drop(1)
            .filter { it.contains(":") }
            .associate { line ->
                line.substringBefore(":").trim().lowercase() to line.substringAfter(":").trim()
            }

        val length = headers["content-length"]?.toIntOrNull() ?: 0
        val body = ByteArray(length)
        var read = 0
        while (read < length) {
            val n = input.read(body, read, length - read)
            if (n <= 0) break
            read += n
        }

        return Request(start[0], start[1].substringBefore("?"), headers, body)
    }

    private fun reason(code: Int) = when (code) {
        200 -> "OK"
        302 -> "Found"
        401 -> "Unauthorized"
        422 -> "Unprocessable Entity"
        429 -> "Too Many Requests"
        503 -> "Service Unavailable"
        else -> "Status"
    }
}
