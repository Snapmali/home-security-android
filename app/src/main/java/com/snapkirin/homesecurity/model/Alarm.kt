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
data class Alarm(
    @Json(name = "alarm_id") val id: Long,
    @Json(name = "host_id") val deviceId: Long,
    val type: Int,
    val time: Double,
    var viewed: Boolean = false,
    var desc: String = "",
    var img: String = ""
) : Parcelable {

    companion object {
        const val MOTION_ALARM = 1
        const val SMOKE_ALARM = 2
    }

    @IgnoredOnParcel
    @Transient
    var dateTimeString: String = SimpleDateFormat(
        "yyyy/MM/dd HH:mm:ss",
        Locale.getDefault()
    ).format(Date((time * 1000).toLong()))

    @IgnoredOnParcel
    @Transient
    var timeString: String = SimpleDateFormat(
        "HH:mm:ss",
        Locale.getDefault()
    ).format(Date((time * 1000).toLong()))

    fun refreshTimeString() {
        dateTimeString = SimpleDateFormat(
                "yyyy/MM/dd HH:mm:ss",
                Locale.getDefault()
        ).format(Date((time * 1000).toLong()))
        timeString = SimpleDateFormat(
            "HH:mm:ss",
            Locale.getDefault()
        ).format(Date((time * 1000).toLong()))
    }
}