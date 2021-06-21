package com.snapkirin.homesecurity.model.http

import com.snapkirin.homesecurity.model.json.JsonResponse

data class HttpResponse<T : JsonResponse>(
        val success: Boolean,
        val statusCode: Int,
        val code: Int,
        var data: T?
)
