package com.snapkirin.homesecurity.network.http

import com.haroldadmin.cnradapter.NetworkResponse
import com.snapkirin.homesecurity.model.json.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface Apis {

    @POST("/auth/user/login")
    suspend fun login(
        @Body loginJsonRequest: LoginJsonRequest
    ): NetworkResponse<LoginJsonResponse, BasicJsonResponse>

    @POST("/auth/user/register")
    suspend fun registerInfo(
        @Body registerJsonRequest: RegisterJsonRequest
    ): NetworkResponse<BasicJsonResponse, BasicJsonResponse>

    @POST("/auth/user/register/verification")
    suspend fun registerVerification(
        @Body registerVerificationJsonRequest: RegisterVerificationJsonRequest
    ): NetworkResponse<BasicJsonResponse, BasicJsonResponse>

    @GET("/user/home_host/pull")
    suspend fun pullDevices(
        @Query("user_id") userId: Long
    ): NetworkResponse<PullDevicesJsonResponse, BasicJsonResponse>

    @GET("/user/alarm/pull")
    suspend fun pullAlarms(
        @Query("user_id") userId: Long,
        @Query("host_id") deviceId: Long,
        @Query("offset") offset: Int,
        @Query("page_size") pageSize: Int
    ): NetworkResponse<PullAlarmsJsonResponse, BasicJsonResponse>

    @POST("/user/monitoring/start")
    suspend fun startMonitoring(
        @Body startMonitoringJsonRequest: StartMonitoringJsonRequest
    ): NetworkResponse<BasicJsonResponse, BasicJsonResponse>

    @POST("/user/monitoring/stop")
    suspend fun stopMonitoring(
        @Body userDeviceIdPairJsonRequest: UserDeviceIdPairJsonRequest
    ): NetworkResponse<BasicJsonResponse, BasicJsonResponse>

    @POST("/user/home_host/unbinding")
    suspend fun bindingDevice(
        @Body deviceUnbindingJsonRequest: UserDeviceIdPairJsonRequest
    ): NetworkResponse<BasicJsonResponse, BasicJsonResponse>

    @POST("/user/home_host/rename")
    suspend fun renameDevice(
        @Body renameDeviceJsonRequest: RenameDeviceJsonRequest
    ): NetworkResponse<BasicJsonResponse, BasicJsonResponse>

    @POST("/user/rst_pwd")
    suspend fun resetPassword(
        @Body resetPasswordJsonRequest: ResetPasswordJsonRequest
    ): NetworkResponse<BasicJsonResponse, BasicJsonResponse>

    @POST("/user/streaming/start")
    suspend fun startStreaming(
        @Body userDeviceIdPairJsonRequest: UserDeviceIdPairJsonRequest
    ): NetworkResponse<BasicJsonResponse, BasicJsonResponse>

    @POST("/user/streaming/stop")
    suspend fun stopStreaming(
        @Body userDeviceIdPairJsonRequest: UserDeviceIdPairJsonRequest
    ): NetworkResponse<BasicJsonResponse, BasicJsonResponse>
}