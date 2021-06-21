package com.snapkirin.homesecurity.model.json

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JwtPayload(
    val aud: String,
    val exp: Long,
    val jti: String,
    val iat: Long,
    @Json(name = "user_id") val userId: Long
)