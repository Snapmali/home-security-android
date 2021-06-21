package com.snapkirin.homesecurity.model

data class WifiAuthentication(
    val keyManagement: List<String>,
    val cipher: String
)