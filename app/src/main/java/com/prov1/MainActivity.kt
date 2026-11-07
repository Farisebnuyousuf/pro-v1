package com.prov1

import android.Manifest
import android.app.Activity
import android.content.Intent
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
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import kotlin.math.exp
import kotlin.math.ln

class MainActivity : Activity() {

    private lateinit var engine: CameraEngine
    private lateinit var textureView: TextureView
    private lateinit var statusView: TextView
    private lateinit var recordBtn: View
    private lateinit var proToggle: TextView
    private lateinit var infoBtn: TextView
    private lateinit var panel: LinearLayout

    private var panelBuilt = false

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
            textSize = 13f
            setPadding(24, 48, 24, 8)
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            alpha = 0.9f
        }
        frame.addView(
            statusView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP
            )
        )

        // --- PRO/AUTO chip (top-left) ---
        proToggle = TextView(this).apply {
            text = "PRO"
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(dp(16), dp(6), dp(16), dp(6))
            background = GradientDrawable().apply {
                cornerRadius = dpF(16f)
                setColor(Color.argb(140, 0, 0, 0))
                setStroke(dp(1), Color.argb(255, 255, 255, 90))
            }
            alpha = 0.6f
        }
        proToggle.setOnClickListener { onProToggle() }
        val chipParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = dp(144)
        }
        frame.addView(proToggle, chipParams)

        // --- device capability report (top-right) ---
        infoBtn = TextView(this).apply {
            text = "ℹ"
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(dp(14), dp(4), dp(14), dp(4))
            background = GradientDrawable().apply { cornerRadius = dpF(16f); setColor(Color.argb(140, 0, 0, 0)) }
            alpha = 0.6f
        }
        infoBtn.setOnClickListener {
            startActivity(Intent(this, CapReportActivity::class.java))
        }
        val infoParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = dp(144)
            marginEnd = dp(16)
        }
        frame.addView(infoBtn, infoParams)

        // --- manual panel (bottom-left) ---
        panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dpF(14f)
                setColor(Color.argb(170, 0, 0, 0))
            }
            setPadding(dp(12), dp(8), dp(12), dp(10))
        }
        val panelParams = FrameLayout.LayoutParams(
            dp(360), FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            bottomMargin = dp(16)
            marginStart = dp(12)
        }
        frame.addView(panel, panelParams)
        panel.visibility = View.GONE

        // --- record button (bottom-right) ---
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
        btnParams.gravity = Gravity.BOTTOM or Gravity.END
        btnParams.bottomMargin = dp(64)
        btnParams.marginEnd = dp(24)
        frame.addView(recordBtn, btnParams)
        recordBtn.setOnClickListener { onRecordPressed() }

        setContentView(frame)

        engine = CameraEngine(this) { msg ->
            runOnUiThread {
                statusView.text = msg
                syncPanel()
            }
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

        statusView.text = "Pro V1 — M2 manual build"
    }

    override fun onResume() {
        super.onResume()
        if (!engine.isRecording) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
        maybeStart()
        runOnUiThread { syncPanel() }
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

    // ---------- PRO panel ----------

    private fun syncPanel() {
        val caps = engine.manualCaps()
        if (caps == null || !caps.manualPossible) {
            proToggle.visibility = View.GONE
            panel.visibility = View.GONE
            return
        }
        proToggle.visibility = View.VISIBLE
        if (!engine.isManual()) {
            panel.visibility = View.GONE
            return
        }
        panel.visibility = View.VISIBLE
        if (!panelBuilt) {
            panelBuilt = true
            buildPanel(caps)
        }
    }

    private fun buildPanel(caps: CameraEngine.ManualCaps) {
        panel.removeAllViews()
        val title = TextView(this).apply {
            text = "MANUAL"
            setTextColor(Color.argb(255, 255, 255, 90))
            textSize = 12f
            setPadding(dp(4), dp(2), dp(4), dp(6))
        }
        panel.addView(title)

        if (caps.isoMax > caps.isoMin) {
            panel.addView(sliderRow("ISO", "%,d".format(caps.isoMax)) { p ->
                val iso = caps.isoMin +
                    (((caps.isoMax - caps.isoMin).toLong() * p) / 100L).toInt()
                engine.setManualIso(iso)
                "%,d".format(iso)
            })
        }

        if (caps.shutterMaxNs > caps.shutterMinNs) {
            panel.addView(sliderRow("SHUTTER", "16 ms") { p ->
                val ns = exp(
                    ln(caps.shutterMinNs.toDouble()) +
                        (ln(caps.shutterMaxNs.toDouble()) - ln(caps.shutterMinNs.toDouble())) * p / 100.0
                ).toLong()
                engine.setManualShutterNs(ns)
                if (ns >= 1_000_000_000L) "%.1f s".format(ns / 1e9)
                else "%d ms".format(ns / 1_000_000L)
            })
        }

        if (caps.focusMaxDiopter > 0f) {
            panel.addView(sliderRow("FOCUS", "∞") { p ->
                if (p == 0) {
                    engine.setManualFocusDiopter(0.0f)
                    "∞"
                } else {
                    val d = caps.focusMaxDiopter * p / 100f
                    engine.setManualFocusDiopter(d)
                    "%.1f m".format(1.0f / d)
                }
            })
        }

        if (caps.wbOffSupported) {
            panel.addView(sliderRow("WB", "5500K") { p ->
                val k = 2000 + p * 80
                engine.setManualWbTemp(k)
                "%dK".format(k)
            })
        }
    }

    private fun sliderRow(
        label: String,
        initialText: String,
        onProgress: (Int) -> String
    ): LinearLayout {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        val name = TextView(this).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 11f
            width = dp(72)
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(name)

        val bar = SeekBar(this).apply {
            max = 100
            progress = 50
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        row.addView(bar)

        val value = TextView(this).apply {
            text = initialText
            setTextColor(Color.argb(255, 255, 255, 90))
            textSize = 11f
            width = dp(64)
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
        }
        row.addView(value)

        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) value.text = onProgress(p)
            }
            override fun onStartTrackingTouch(s: SeekBar?) { }
            override fun onStopTrackingTouch(s: SeekBar?) { }
        })
        return row
    }

    private fun onProToggle() {
        engine.setManualMode(!engine.isManual())
        if (engine.isManual()) {
            proToggle.text = "AUTO"
            proToggle.alpha = 1f
            syncPanel()
        } else {
            proToggle.text = "PRO"
            proToggle.alpha = 0.6f
            syncPanel()
        }
    }

    // ---------- recording ----------

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

    private fun dpF(v: Float): Float = v * resources.displayMetrics.density

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
