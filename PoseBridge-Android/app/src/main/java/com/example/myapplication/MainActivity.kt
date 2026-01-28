package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.SurfaceHolder
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import net.majorkernelpanic.streaming.Session
import net.majorkernelpanic.streaming.SessionBuilder
import net.majorkernelpanic.streaming.gl.SurfaceView

class MainActivity : AppCompatActivity(),
    Session.Callback,
    SurfaceHolder.Callback {

    private val REQ_PERMS = 1001

    private lateinit var surfaceView: SurfaceView
    private lateinit var etIp: EditText
    private lateinit var tvSdp: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    private var session: Session? = null
    private var surfaceReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        surfaceView = findViewById(R.id.surface)
        etIp = findViewById(R.id.etIp)
        tvSdp = findViewById(R.id.tvSdp)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)

        // 让文字能盖在相机预览上
        surfaceView.setZOrderMediaOverlay(true)
        surfaceView.holder.addCallback(this)

        tvSdp.apply {
            text = "SDP 会显示在这里~"
            setBackgroundColor(Color.parseColor("#AA000000"))
            setTextColor(Color.WHITE)
            textSize = 14f
        }

        ensurePermissions()

        btnStart.setOnClickListener { startStreaming() }
        btnStop.setOnClickListener {
            session?.stop()
            session?.stopPreview()
            tvSdp.text = "Stopped"
        }
    }

    private fun startStreaming() {
        val ip = etIp.text.toString().trim()
        if (ip.isEmpty()) {
            tvSdp.text = "请输入接收端 IP（电脑 IP）"
            return
        }
        if (!surfaceReady) {
            tvSdp.text = "Surface 还没就绪，稍等 1 秒再点开始"
            return
        }

        tvSdp.text = "Configuring session..."

        // 每次开始前先停掉旧的，避免状态乱
        session?.stop()
        session?.release()

        session = SessionBuilder.getInstance()
            .setCallback(this)
            .setSurfaceView(surfaceView)
            .setPreviewOrientation(90)
            .setContext(this) // ✅ 用 Activity context，不要用 applicationContext
            .setVideoEncoder(SessionBuilder.VIDEO_H264)
            .setAudioEncoder(SessionBuilder.AUDIO_NONE) // ✅ 先别碰音频，减少坑
            .build()

        session?.setDestination(ip)

        // ✅ 关键：必须先 configure()，成功后才会回调 onSessionConfigured()
        try {
            session?.configure()
        } catch (e: Exception) {
            tvSdp.text = "configure() failed:\n${e.message}"
            e.printStackTrace()
        }
    }

    // ===== 权限 =====

    private fun ensurePermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val perms = arrayOf(
            Manifest.permission.CAMERA
            // 音频我们先禁用，所以不需要 RECORD_AUDIO
        )

        val need = perms.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (need) {
            ActivityCompat.requestPermissions(this, perms, REQ_PERMS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // ===== Session.Callback =====

    override fun onSessionConfigured() {
        // ✅ 这里一定会拿到 SDP
        runOnUiThread {
            val sdp = session?.sessionDescription ?: "No SDP"
            tvSdp.text = sdp
        }

        // ✅ configure 完成后再 start
        try {
            session?.start()
        } catch (e: Exception) {
            runOnUiThread { tvSdp.append("\n\nstart() failed: ${e.message}") }
            e.printStackTrace()
        }
    }

    override fun onSessionStarted() {
        runOnUiThread {
            tvSdp.append("\n\nSession started OK")
        }
    }

    override fun onSessionStopped() {}

    override fun onPreviewStarted() {}
    override fun onBitrateUpdate(bitrate: Long) {}

    override fun onSessionError(message: Int, streamType: Int, e: Exception?) {
        runOnUiThread {
            tvSdp.text = "Error: $message\n${e?.message ?: "unknown"}"
        }
        e?.printStackTrace()
    }

    // ===== Surface.Callback =====

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceReady = true
        // 预览只负责显示相机，推流的 start/stop 由按钮控制
        session?.startPreview()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceReady = false
        session?.stopPreview()
        session?.stop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
}
