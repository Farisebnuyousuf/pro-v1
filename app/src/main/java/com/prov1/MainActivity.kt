package com.prov1

import android.Manifest
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var engine: CameraEngine
    private lateinit var textureView: TextureView
    private lateinit var statusView: TextView
    private lateinit var recordBtn: View

    private var surfaceReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val frame = FrameLayout(this)

        textureView = TextureView(this)
        frame.addView(
            textureView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        statusView = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(24, 24, 24, 24)
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }
        frame.addView(
            statusView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        )

        recordBtn = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(235, 255, 255, 255))
                setStroke(dp(4), Color.argb(255, 235, 235, 235))
            }
            contentDescription = "Record"
        }
        val btnSize = dp(72)
        val btnParams = FrameLayout.LayoutParams(btnSize, btnSize)
        btnParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        btnParams.bottomMargin = dp(64)
        frame.addView(recordBtn, btnParams)
        recordBtn.setOnClickListener { onRecordPressed() }

        setContentView(frame)

        engine = CameraEngine(this) { msg ->
            runOnUiThread { statusView.text = msg }
        }

        textureView.surfaceTextureListener = object
            : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                st: SurfaceTexture, w: Int, h: Int
            ) {
                surfaceReady = true
                maybeStart()
            }
            override fun onSurfaceTextureSizeChanged(
                st: SurfaceTexture, w: Int, h: Int
            ) {
                engine.updateTransform()
            }
            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                engine.stop()
                surfaceReady = false
                return true
            }
            override fun onSurfaceTextureUpdated(st: SurfaceTexture) { }
        }

        statusView.text = "Pro V1 — M1 record build"
    }

    override fun onResume() {
        super.onResume()
        if (!engine.isRecording) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
        maybeStart()
    }

    override fun onPause() {
        engine.stop()
        super.onPause()
    }

    override fun onDestroy() {
        engine.release()
        super.onDestroy()
    }

    override fun onConfigurationChanged(rc: android.content.res.Configuration) {
        super.onConfigurationChanged(rc)
        engine.updateTransform()
    }

    private fun maybeStart() {
        if (!engine.hasPermission()) {
            engine.requestPermission(this)
            statusView.text = "Grant camera permission to start"
            return
        }
        if (surfaceReady) engine.start(textureView)
    }

    private fun onRecordPressed() {
        if (engine.isRecording) {
            engine.stopRecording { msg ->
                runOnUiThread {
                    statusView.text = msg
                    setRecordBtnIdle()
                }
            }
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            return
        }
        if (!engine.hasAudioPermission()) {
            statusView.text = "Microphone permission needed for audio recording"
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO), CameraEngine.REQ_AUDIO
            )
            return
        }
        if (!surfaceReady || !engine.hasPermission()) return
        // Lock orientation so the saved clip metadata matches one fixed rotation.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        engine.startRecording { msg ->
            runOnUiThread {
                statusView.text = msg
                if (engine.isRecording) setRecordBtnRecording()
            }
        }
    }

    private fun setRecordBtnIdle() {
        (recordBtn.background as? GradientDrawable)?.apply {
            setColor(Color.argb(235, 255, 255, 255))
        }
    }

    private fun setRecordBtnRecording() {
        (recordBtn.background as? GradientDrawable)?.apply {
            setColor(Color.argb(235, 255, 90, 90))
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CameraEngine.REQ_CAMERA -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                    if (surfaceReady) engine.start(textureView)
                } else {
                    statusView.setTextColor(Color.RED)
                    statusView.text = "Camera permission is required to use Pro V1"
                }
            }
            CameraEngine.REQ_AUDIO -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                    onRecordPressed()
                } else {
                    statusView.text = "Audio denied — recording needs mic access"
                }
            }
        }
    }
}
