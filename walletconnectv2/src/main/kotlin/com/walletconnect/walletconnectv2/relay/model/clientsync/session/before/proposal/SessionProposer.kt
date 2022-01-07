package com.walletconnect.walletconnectv2.relay.model.clientsync.session.before.proposal

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SessionProposer(
    @Json(name = "publicKey")
    val publicKey: String,
    @Json(name = "controller")
    val controller: Boolean,
    @Json(name = "metadata")
    val metadata: AppMetaData?
)