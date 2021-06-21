package com.snapkirin.homesecurity.ui.player

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.utils.OrientationUtils
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import com.snapkirin.homesecurity.HomeSecurity.DEVICE_ID
import com.snapkirin.homesecurity.HomeSecurity.USER_ID
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.model.DeviceList
import com.snapkirin.homesecurity.network.NetworkGlobals


class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var videoPlayer: StandardGSYVideoPlayer
    private lateinit var orientationUtils: OrientationUtils

    private lateinit var playerViewModel: VideoPlayerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        val userId = intent.getLongExtra(USER_ID, 0L)
        val deviceId = intent.getLongExtra(DEVICE_ID, 0L)

        playerViewModel = ViewModelProvider(
            this, VideoPlayerViewModelFactory(userId, deviceId)
        ).get(VideoPlayerViewModel::class.java)

        val url = "${NetworkGlobals.streamingUrl}/live/$deviceId.flv"
        val header = mapOf(Pair("Authorization", "Bearer ${NetworkGlobals.jwt}"))
        initPlayer(deviceId, url, header)
    }

    private fun initPlayer(deviceId: Long, url: String, header: Map<String, String>) {

        videoPlayer = findViewById(R.id.video_player)

        videoPlayer.setUp(url, false, DeviceList.getDeviceName(deviceId))
        videoPlayer.mapHeadData = header
        // 显示title
        videoPlayer.titleTextView.visibility = View.VISIBLE
        // 显示返回键
        videoPlayer.backButton.visibility = View.VISIBLE
        // 隐藏全屏键
        videoPlayer.fullscreenButton.visibility = View.INVISIBLE
        // 设置旋转
        orientationUtils = OrientationUtils(this, videoPlayer)
        orientationUtils.isOnlyRotateLand = true
        orientationUtils.isRotateWithSystem = false
        // 是否可以滑动调控制
        videoPlayer.setIsTouchWiget(true)
        // 设置返回按键功能
        videoPlayer.backButton.setOnClickListener { onBackPressed() }
        videoPlayer.startPlayLogic()
    }


    override fun onPause() {
        super.onPause()
        videoPlayer.onVideoPause()
    }

    override fun onResume() {
        super.onResume()
        videoPlayer.onVideoResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        GSYVideoManager.releaseAllVideos()
        orientationUtils.releaseListener()
    }

    override fun onBackPressed() {
        //释放所有
        videoPlayer.setVideoAllCallBack(null)
        super.onBackPressed()
    }
}