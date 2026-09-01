package com.wanderwildwood.kinokocho.net

import com.wanderwildwood.kinokocho.BuildConfig
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * The only part of this app that opens a socket.
 *
 * [java.net.HttpURLConnection] and nothing else. A request builder and an HTTP client
 * would be two more dependencies and a few hundred kilobytes in an APK that is under
 * three megabytes, to make about seven calls. There is no JSON mapper either: `org.json`
 * ships with Android, and the shapes being read are small enough to read by hand — see
 * [INatPayload], where all of that lives so that it can be tested without a network.
 *
 * Every call here can fail in a way that is nobody's mistake — no signal in a valley,
 * an account that revoked this app last week — so nothing throws. [Result] is what
 * comes back, and the screen says what it says.
 */
class INatClient(private val account: INatAccount) : INatApi {

    sealed interface Result<out T> {
        data class Ok<T>(val value: T) : Result<T>

        /** Something a person can be told. Never a stack trace, never a status code alone. */
        data class Failed(val said: String, val signedOut: Boolean = false) : Result<Nothing>
    }

    /** Turns the code the browser came back with into a token worth keeping. */
    suspend fun exchange(code: String, verifier: String): Result<String> {
        val body = INatAuth.tokenRequestBody(
            clientId = INatConfig.CLIENT_ID,
            redirectUri = INatConfig.REDIRECT_URI,
            code = code,
            verifier = verifier,
        )
        return when (val r = post("${INatAuth.SITE}/oauth/token", body, FORM, auth = null)) {
            is Result.Failed -> r
            is Result.Ok -> {
                val token = runCatching { org.json.JSONObject(r.value).optString("access_token") }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
                if (token == null) {
                    Result.Failed("iNaturalist did not return a sign-in token.")
                } else {
                    Result.Ok(token)
                }
            }
        }
    }

    /**
     * A JWT for the v1 API, minted from the access token and cached for the day.
     *
     * See [INatAccount] for why these are two different things. The 401 branch is the
     * one that matters: an access token stops working when the reader revokes this
     * application on iNaturalist, and the only useful response to that is to forget it
     * and ask them to sign in again — not to retry, and not to report a network fault
     * for something that is a decision somebody made on a website.
     */
    override suspend fun apiToken(): Result<String> {
        account.apiToken()?.let { return Result.Ok(it) }
        val access = account.accessToken
            ?: return Result.Failed("Not signed in to iNaturalist.", signedOut = true)

        return when (val r = get("${INatAuth.SITE}/users/api_token", auth = "Bearer $access")) {
            is Result.Failed -> {
                if (r.signedOut) account.signOut()
                r
            }
            is Result.Ok -> {
                val jwt = runCatching { org.json.JSONObject(r.value).optString("api_token") }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
                if (jwt == null) {
                    Result.Failed("iNaturalist did not return an API token.")
                } else {
                    account.rememberApiToken(jwt)
                    Result.Ok(jwt)
                }
            }
        }
    }

    /** Who this phone is signed in as, for showing on the button. */
    suspend fun me(jwt: String): Result<String> =
        when (val r = get("$API/users/me", auth = jwt)) {
            is Result.Failed -> r
            is Result.Ok -> {
                val login = runCatching {
                    org.json.JSONObject(r.value).optJSONArray("results")
                        ?.optJSONObject(0)?.optString("login")
                }.getOrNull()?.takeIf { it.isNotBlank() }
                if (login == null) Result.Failed("Could not read the account name.")
                else Result.Ok(login)
            }
        }

    override suspend fun createObservation(jwt: String, body: String): Result<INatPayload.Created> =
        when (val r = post("$API/observations", body, JSON, auth = jwt)) {
            is Result.Failed -> r
            is Result.Ok -> INatPayload.created(r.value)
                ?.let { Result.Ok(it) }
                ?: Result.Failed("iNaturalist accepted the find but did not say where it went.")
        }

    /**
     * One photograph, as multipart.
     *
     * `observation_photo[uuid]` is not decoration. Their controller looks for an
     * existing observation photo with that uuid belonging to this user before making a
     * new one, exactly as the observation create does — so a push that dies after three
     * of five pictures can be run again and produces five photographs rather than
     * eight.
     */
    override suspend fun uploadPhoto(
        jwt: String,
        observationId: Long,
        photoUuid: String,
        file: File,
    ): Result<Long?> {
        val boundary = "----kinokocho${System.nanoTime()}"
        val body = multipart(
            boundary,
            fields = mapOf(
                "observation_photo[observation_id]" to observationId.toString(),
                "observation_photo[uuid]" to photoUuid.lowercase(),
            ),
            fileField = "file",
            file = file,
        )
        return when (
            val r = post(
                "$API/observation_photos",
                body,
                "multipart/form-data; boundary=$boundary",
                auth = jwt,
            )
        ) {
            is Result.Failed -> r
            is Result.Ok -> Result.Ok(INatPayload.photoId(r.value))
        }
    }

    /**
     * Asks what an observation has become.
     *
     * Signed, though the observation is public and this would work without it: an
     * obscured observation shows its true coordinates only to the person who recorded
     * it, and reading back one's own find as a stranger would be a quiet way for the
     * app to see less than the reader does.
     */
    override suspend fun fetchObservation(jwt: String, uuid: String): Result<String> =
        get("$API/observations/$uuid", auth = jwt)

    // ---- the plumbing -------------------------------------------------------------

    private suspend fun get(url: String, auth: String?): Result<String> =
        send(url, "GET", null, null, auth)

    private suspend fun post(
        url: String,
        body: Any,
        contentType: String,
        auth: String?,
    ): Result<String> = send(url, "POST", body, contentType, auth)

    private suspend fun send(
        url: String,
        method: String,
        body: Any?,
        contentType: String?,
        auth: String?,
    ): Result<String> = withContext(Dispatchers.IO) {
        // iNaturalist asks for about one request a second and says they may block an
        // address that ignores it. A push is a handful of calls made once by one person,
        // so the polite thing costs nothing worth having.
        delay(COURTESY_PAUSE)

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 20_000
                readTimeout = 60_000
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
                auth?.let { setRequestProperty("Authorization", it) }
                contentType?.let { setRequestProperty("Content-Type", it) }
                if (body != null) {
                    doOutput = true
                    val bytes = when (body) {
                        is ByteArray -> body
                        else -> body.toString().toByteArray(Charsets.UTF_8)
                    }
                    setFixedLengthStreamingMode(bytes.size)
                    outputStream.use { it.write(bytes) }
                }
            }

            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.use { it.readBytes().toString(Charsets.UTF_8) }
                .orEmpty()

            when {
                code in 200..299 -> Result.Ok(text)
                code == 401 || code == 403 -> Result.Failed(
                    INatPayload.errorFrom(text)
                        ?: "iNaturalist would not accept the sign-in. Sign in again.",
                    signedOut = true,
                )
                code == 422 -> Result.Failed(
                    INatPayload.errorFrom(text) ?: "iNaturalist would not take that find."
                )
                // Their own words for a rate limit, said as a thing to wait out rather
                // than a thing that broke.
                code == 429 -> Result.Failed("iNaturalist is asking for a slower pace. Try again in a few minutes.")
                code >= 500 -> Result.Failed("iNaturalist is having trouble. Nothing was lost — try again later.")
                else -> Result.Failed(INatPayload.errorFrom(text) ?: "iNaturalist answered with $code.")
            }
        } catch (e: IOException) {
            // The ordinary case, and not an error worth a stack trace: a phone indoors
            // on a hill.
            Result.Failed("Could not reach iNaturalist. Nothing was sent.")
        } finally {
            connection?.disconnect()
        }
    }

    private fun multipart(
        boundary: String,
        fields: Map<String, String>,
        fileField: String,
        file: File,
    ): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val writer = { s: String -> out.write(s.toByteArray(Charsets.UTF_8)) }
        fields.forEach { (name, value) ->
            writer("--$boundary\r\n")
            writer("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
            writer("$value\r\n")
        }
        writer("--$boundary\r\n")
        writer("Content-Disposition: form-data; name=\"$fileField\"; filename=\"${file.name}\"\r\n")
        writer("Content-Type: image/jpeg\r\n\r\n")
        file.inputStream().use { it.copyTo(out) }
        writer("\r\n--$boundary--\r\n")
        return out.toByteArray()
    }

    private companion object {
        const val API = "https://api.inaturalist.org/v1"
        const val JSON = "application/json"
        const val FORM = "application/x-www-form-urlencoded"
        const val COURTESY_PAUSE = 1_100L

        /**
         * iNaturalist asks that software using the API identify itself, so that they can
         * tell one caller from another when something goes wrong. The name and the
         * version are the whole of it — no device, no identifier, nothing about who is
         * holding the phone.
         */
        val USER_AGENT = "kinokocho/${BuildConfig.VERSION_NAME} (Mushroom Journal, Android)"
    }
}
