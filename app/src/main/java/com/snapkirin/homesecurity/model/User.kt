package com.snapkirin.homesecurity.model

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
        val userId: Long,
        val username: String,
        var jwt: String
        ) : Parcelable {
    @IgnoredOnParcel
    var email: String? = null
}
