package com.snapkirin.homesecurity.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.*

@Parcelize
@JsonClass(generateAdapter = true)
data class Device(
        @Json(name = "host_id") val id: Long,
        @Json(name = "created_at") val bindingTime: Double,
        @Json(name = "screen_name") var name: String = "",
        var online: Boolean = false,
        var streaming: Boolean = false,
        var monitoring: Boolean = false
) : Parcelable {

    @IgnoredOnParcel
    @Transient
    var bindingTimeString: String = SimpleDateFormat(
            "yyyy/MM/dd HH:mm:ss",
            Locale.getDefault()
    ).format(Date((bindingTime * 1000).toLong()))

    fun updateBindingTimeString(): String {
        bindingTimeString = SimpleDateFormat(
                "yyyy/MM/dd HH:mm:ss",
                Locale.getDefault()
        ).format(Date((bindingTime * 1000).toLong()))
        return bindingTimeString
    }
}