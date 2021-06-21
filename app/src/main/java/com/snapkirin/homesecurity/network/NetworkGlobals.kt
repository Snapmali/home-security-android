package com.snapkirin.homesecurity.network

import android.util.Base64
import com.snapkirin.homesecurity.model.json.JwtPayload
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient

object NetworkGlobals {

    const val httpBaseUrl = "https://api.sample.com"
    const val webSocketUrl = "wss://ws.sample.com/ws/user"
    const val streamingUrl = "https://stream.sample.com"

    var jwt: String = ""
    val moshi: Moshi = Moshi.Builder().build()
    val okHttpClient = OkHttpClient
        .Builder()
        .addInterceptor { chain ->
            if (jwt.isNotBlank() && chain.request().headers["Authorization"]?.isBlank() != false) {
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $jwt")
                    .build()

                chain.proceed(request)
            }
            else
                chain.proceed(chain.request())
        }
        .build()
    private val jwtDecoder = moshi.adapter(JwtPayload::class.java)

    fun getJwtPayload(): JwtPayload? {
        if (jwt.isBlank())
            return null
        return try {
            val payload = jwt.split("\\.")[1]
            val payloadBytes = Base64.decode(payload, Base64.URL_SAFE)
            jwtDecoder.fromJson(payloadBytes.decodeToString())
        } catch (e: Exception) {
            null
        }
    }
}