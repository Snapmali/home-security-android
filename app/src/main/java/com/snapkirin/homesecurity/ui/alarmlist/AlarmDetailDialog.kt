package com.snapkirin.homesecurity.ui.alarmlist

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.snapkirin.homesecurity.HomeSecurity.ALARM
import com.snapkirin.homesecurity.HomeSecurity.DEVICE_NAME
import com.snapkirin.homesecurity.HomeSecurity.IMAGE_URL
import com.snapkirin.homesecurity.HomeSecurity.RECT
import com.snapkirin.homesecurity.HomeSecurity.SCALE_TYPE
import com.snapkirin.homesecurity.HomeSecurity.USER_ID
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.model.Alarm
import com.snapkirin.homesecurity.network.http.Http
import com.snapkirin.homesecurity.ui.picview.PicViewActivity


class AlarmDetailDialog : DialogFragment() {

    private var userId: Long? = null
    private var alarm: Alarm? = null
    private var deviceName: String? = null

    private lateinit var imageView: ImageView
    private lateinit var typeText: TextView
    private lateinit var deviceNameText: TextView
    private lateinit var timeText: TextView
    private lateinit var descriptionText: TextView

    private lateinit var parentActivity: AppCompatActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getLong(USER_ID)
            alarm = it.getParcelable(ALARM)
            deviceName = it.getString(DEVICE_NAME)
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.dialog_alarm_detail, container, false)
        dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog!!.window!!.requestFeature(Window.FEATURE_NO_TITLE)

        parentActivity = activity as AppCompatActivity

        imageView = root.findViewById(R.id.alarmDetailImageView)
        typeText = root.findViewById(R.id.alarmDetailTypeText)
        deviceNameText = root.findViewById(R.id.alarmDetailDeviceNameText)
        timeText = root.findViewById(R.id.alarmDetailTimeText)
        descriptionText = root.findViewById(R.id.alarmDetailDescriptionText)

        typeText.apply {
            when (alarm?.type) {
                Alarm.MOTION_ALARM -> {
                    setText(R.string.motion_alarm)
                    setCompoundDrawablesRelativeWithIntrinsicBounds(
                        root.context.getDrawable(R.drawable.ic_baseline_directions_walk_24),
                        null, null, null
                    )
                }
                Alarm.SMOKE_ALARM -> {
                    setText(R.string.smoke_alarm)
                    setCompoundDrawablesRelativeWithIntrinsicBounds(
                        root.context.getDrawable(R.drawable.ic_baseline_local_fire_department_24),
                        null, null, null
                    )
                }
            }
        }
        val progressDrawable = CircularProgressDrawable(requireContext()).apply {
            setStyle(CircularProgressDrawable.LARGE)
            setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.design_default_color_primary))
        }

        deviceNameText.text = deviceName
        timeText.text = alarm?.dateTimeString
        descriptionText.text = alarm?.desc
        var width = imageView.width
        var height = imageView.height
        val imageUrl = userId?.let { alarm?.img?.let { it1 -> Http.getAlarmImageUrl(it, it1) } }
        Glide.with(this)
            .asBitmap()
            .placeholder(progressDrawable.apply { start() })
            .load(imageUrl)
            .error(R.drawable.ic_baseline_error_24_red)
            .listener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<Bitmap>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Bitmap?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<Bitmap>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    if (resource != null) {
                        width = resource.width
                        height = resource.height
                    }
                    return false
                }
            })
            .into(imageView)
        imageView.setOnClickListener {
            val rect = Rect()
            val coordinates = IntArray(2)
            imageView.getLocationOnScreen(coordinates)

            // calculate resized image coordinates
            rect.left = coordinates[0] + imageView.width / 2 - width / 2
            rect.right = coordinates[0] + imageView.width / 2 + width / 2
            rect.top = coordinates[1] + imageView.height / 2 - height / 2
            rect.bottom = coordinates[1] + imageView.height / 2 + height / 2

            startPicViewActivity(rect, imageUrl, imageView.scaleType)
        }

        return root
    }

    private fun startPicViewActivity(
        rect: Rect, imageUrl: String?, scaleType: ImageView.ScaleType, bundle: Bundle? = null
    ) {
        val intent = Intent(parentActivity, PicViewActivity::class.java)
        intent.putExtra(RECT, rect)
        intent.putExtra(IMAGE_URL, imageUrl)
        intent.putExtra(SCALE_TYPE, scaleType)
        if (bundle != null) {
            intent.putExtras(bundle)
        }
        startActivity(intent)
    }

    companion object {
        @JvmStatic
        fun newInstance(userId: Long, alarm: Alarm, deviceName: String) =
            AlarmDetailDialog().apply {
                arguments = Bundle().apply {
                    putLong(USER_ID, userId)
                    putParcelable(ALARM, alarm)
                    putString(DEVICE_NAME, deviceName)
                }
            }
    }
}