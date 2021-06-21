package com.snapkirin.homesecurity.network.http

import android.util.Log
import com.haroldadmin.cnradapter.NetworkResponse
import com.haroldadmin.cnradapter.NetworkResponseAdapterFactory
import com.snapkirin.homesecurity.HomeSecurity.CAPTURE_ALWAYS_SAVE
import com.snapkirin.homesecurity.HomeSecurity.CAPTURE_SAVE_WHEN_MOVING
import com.snapkirin.homesecurity.HomeSecurity.IDENTIFIER_EMAIL
import com.snapkirin.homesecurity.HomeSecurity.IDENTIFIER_USERNAME
import com.snapkirin.homesecurity.model.http.BasicRequestResult
import com.snapkirin.homesecurity.network.NetworkGlobals
import com.snapkirin.homesecurity.model.User
import com.snapkirin.homesecurity.model.http.*
import com.snapkirin.homesecurity.model.json.*
import com.snapkirin.homesecurity.network.NetworkGlobals.httpBaseUrl
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object Http {

    private val moshi = NetworkGlobals.moshi
    private val okHttpClient = NetworkGlobals.okHttpClient
    private val retrofit: Retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(httpBaseUrl)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .addCallAdapterFactory(NetworkResponseAdapterFactory())
        .build()
    private val apis: Apis = retrofit.create(Apis::class.java)

    suspend fun login(identifierType: Int, identifier: String, password: String): LoginResult {
        if (identifierType != IDENTIFIER_EMAIL && identifierType != IDENTIFIER_USERNAME) {
            throw IllegalArgumentException("Invalid identifier type")
        }
        val response = jsonResponse {
            apis.login(LoginJsonRequest(identifierType, identifier, password))
        }
        return if (response.success) {
            val data = response.data!!
            LoginResult(true, response.code, 0, User(data.userId!!, data.username!!, data.token!!))
        } else {
            LoginResult(false, response.code, 0, null)
        }
    }

    suspend fun registerInfo(username: String, email: String, password: String): BasicRequestResult {
        val response = jsonResponse {
            apis.registerInfo(RegisterJsonRequest(username, email, password))
        }
        return if (response.success) {
            BasicRequestResult(true, response.code)
        } else {
            BasicRequestResult(false, response.code)
        }
    }

    suspend fun registerVerification(
        username: String,
        verificationCode: String
    ): BasicRequestResult {
        val response = jsonResponse {
            apis.registerVerification(RegisterVerificationJsonRequest(username, verificationCode))
        }
        return if (response.success) {
            BasicRequestResult(true, response.code)
        } else {
            BasicRequestResult(false, response.code)
        }
    }

    suspend fun pullDevices(userId: Long): PullDevicesResult {
        val response = jsonResponse {
            apis.pullDevices(userId)
        }
        return if (response.success) {
            PullDevicesResult(true, response.code, 0, response.data?.hosts)
        } else {
            PullDevicesResult(false, response.code, 0)
        }
    }

    suspend fun pullAlarms(userId: Long, offset: Int, pageSize: Int, deviceId: Long = 0L): PullAlarmsResult {
        val response = jsonResponse {
            apis.pullAlarms(userId, deviceId, offset, pageSize)
        }
        return if (response.success) {
            PullAlarmsResult(
                true, response.code, 0,
                response.data?.offset, response.data?.alarms
            )
        } else {
            PullAlarmsResult(false, response.code, 0)
        }
    }

    suspend fun startMonitoring(userId: Long, deviceId: Long, savingMode: Int): BasicRequestResult {
        if (savingMode != CAPTURE_ALWAYS_SAVE && savingMode != CAPTURE_SAVE_WHEN_MOVING)
            throw IllegalArgumentException("Invalid capture saving mode")
        val response = jsonResponse {
            apis.startMonitoring(StartMonitoringJsonRequest(userId, deviceId, savingMode))
        }
        return if (response.success) {
            BasicRequestResult(true, response.code)
        } else {
            BasicRequestResult(false, response.code)
        }
    }

    suspend fun stopMonitoring(userId: Long, deviceId: Long): BasicRequestResult {
        val response = jsonResponse {
            apis.stopMonitoring(UserDeviceIdPairJsonRequest(userId, deviceId))
        }
        return if (response.success) {
            BasicRequestResult(true, response.code)
        } else {
            BasicRequestResult(false, response.code)
        }
    }

    suspend fun unbindDevice(userId: Long, deviceId: Long): BasicRequestResult {
        val response = jsonResponse {
            apis.bindingDevice(UserDeviceIdPairJsonRequest(userId, deviceId))
        }
        return if (response.success) {
            BasicRequestResult(true, response.code)
        } else {
            BasicRequestResult(false, response.code)
        }
    }

    suspend fun renameDevice(userId: Long, deviceId: Long, screenName: String): BasicRequestResult {
        val response = jsonResponse {
            apis.renameDevice(RenameDeviceJsonRequest(userId, deviceId, screenName))
        }
        return if (response.success) {
            BasicRequestResult(true, response.code)
        } else {
            BasicRequestResult(false, response.code)
        }
    }

    suspend fun resetPassword(userId: Long, oldPassword: String, newPassword: String): BasicRequestResult {
        val response = jsonResponse {
            apis.resetPassword(ResetPasswordJsonRequest(userId, oldPassword, newPassword))
        }
        return if (response.success) {
            BasicRequestResult(true, response.code)
        } else {
            BasicRequestResult(false, response.code)
        }
    }

    suspend fun startStreaming(userId: Long, deviceId: Long): BasicRequestResult {
        val response = jsonResponse {
            apis.startStreaming(UserDeviceIdPairJsonRequest(userId, deviceId))
        }
        return if (response.success) {
            BasicRequestResult(true, response.code)
        } else {
            BasicRequestResult(false, response.code)
        }
    }

    suspend fun stopStreaming(userId: Long, deviceId: Long): BasicRequestResult {
        val response = jsonResponse {
            apis.stopStreaming(UserDeviceIdPairJsonRequest(userId, deviceId))
        }
        return if (response.success) {
            BasicRequestResult(true, response.code)
        } else {
            BasicRequestResult(false, response.code)
        }
    }

    private suspend fun <T : JsonResponse, U : JsonResponse> jsonResponse(
        block: suspend () -> NetworkResponse<T, U>
    ): HttpResponse<T> {
        when (val response = block()) {
            is NetworkResponse.Success -> {
                return HttpResponse(
                    true,
                    response.code,
                    response.body.code,
                    response.body
                )
            }
            is NetworkResponse.ServerError -> {
                Log.e(
                    "HTTP", "Status Code ${response.code}, " +
                            "Message: ${response.body?.message}, Code: ${response.body?.code}"
                )
                return HttpResponse(
                    false,
                    response.code,
                    response.body?.code ?: ResponseCodes.InternalError,
                    null
                )
            }
            is NetworkResponse.NetworkError -> {
                Log.e("HTTP", response.error.message.toString())
                return HttpResponse(
                    false,
                    -1,
                    ResponseCodes.NetworkError,
                    null
                )
            }
            is NetworkResponse.UnknownError -> {
                Log.e("HTTP", response.error.message.toString())
                return HttpResponse(
                    false,
                    -1,
                    ResponseCodes.Error,
                    null
                )
            }
        }
    }

    fun getAlarmImageUrl(userId: Long, image: String): String {
        return "$httpBaseUrl/user/alarm/img?user_id=$userId&image=$image"
    }
}