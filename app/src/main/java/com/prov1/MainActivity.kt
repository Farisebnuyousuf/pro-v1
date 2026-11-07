package com.prov1

import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.Gravity
import android.view.TextureView
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var engine: CameraEngine
    private lateinit var textureView: TextureView
    private lateinit var statusView: TextView

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

        statusView.text = "Pro V1 — M0 foundation build"
    }

    override fun onResume() {
        super.onResume()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CameraEngine.REQ_CAMERA) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                if (surfaceReady) engine.start(textureView)
            } else {
                statusView.setTextColor(Color.RED)
                statusView.text = "Camera permission is required to use Pro V1"
            }
        }
    }
}
