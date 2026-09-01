package com.wanderwildwood.kinokocho.net

import java.io.File

/**
 * The four calls publishing actually makes.
 *
 * An interface over [INatClient] purely so that [INatPush] can be tested without a
 * network. That is not a ceremonial reason: the ordering [INatPush] guarantees — the
 * observation written down as pushed before any photograph goes up, a photograph's uuid
 * written down before the photograph — is invisible in the source and only observable in
 * the sequence of calls and writes. It is also exactly what somebody tidying up later
 * would reorder, because the tidier order is the wrong one.
 */
interface INatApi {
    suspend fun apiToken(): INatClient.Result<String>

    suspend fun createObservation(jwt: String, body: String): INatClient.Result<INatPayload.Created>

    suspend fun uploadPhoto(
        jwt: String,
        observationId: Long,
        photoUuid: String,
        file: File,
    ): INatClient.Result<Long?>

    suspend fun fetchObservation(jwt: String, uuid: String): INatClient.Result<String>
}
