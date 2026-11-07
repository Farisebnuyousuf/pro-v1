package com.prov1

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.TextureView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * M2 device capability report: the honest "what can THIS phone actually do" screen.
 * Shows sensor-level limits straight from the hardware — no invented numbers.
 */
class CapReportActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val sb = StringBuilder()

        sb.append("=== DEVICE ===\n")
        sb.append("${Build.MANUFACTURER} ${Build.MODEL}\n")
        sb.append("Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
        sb.append("logical cameras: ${cm.cameraIdList.size}\n\n")

        for (id in cm.cameraIdList) {
            sb.append("=== CAMERA $id ===\n")
            try {
                val c = cm.getCameraCharacteristics(id)
                val facing = when (c.get(CameraCharacteristics.LENS_FACING)) {
                    CameraMetadata.LENS_FACING_BACK -> "back"
                    CameraMetadata.LENS_FACING_FRONT -> "front"
                    else -> "external"
                }
                sb.append("facing: $facing\n")
                val active = c.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                if (active != null) {
                    sb.append("active sensor: ${active.width()}x${active.height()} (~" +
                        "%.1f MP)".format(active.width().toLong() * active.height().toLong() / 1e6) + "\n")
                }
                val orientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION)
                if (orientation != null) sb.append("sensor orientation: ${orientation}°\n")

                val iso = c.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
                sb.append("manual ISO: " + if (iso != null) "yes — ${iso.lower}..${iso.upper}\n" else "NOT EXPOSED\n")

                val exp = c.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
                if (exp != null) {
                    sb.append(
                        "manual shutter: yes — %s µs .. %s µs\n".format(
                            exp.lower / 1000, exp.upper / 1000
                        )
                    )
                } else sb.append("manual shutter: NOT EXPOSED\n")

                val focus = c.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
                if (focus != null && focus > 0f) {
                    sb.append(
                        "manual focus: yes — 0.0..%.1f D (nearest %.1f cm)\n".format(
                            focus, 100f / focus
                        )
                    )
                } else sb.append("manual focus: NOT EXPOSED\n")

                val awb = c.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)
                sb.append(
                    "AWB off (manual WB): " +
                        if (awb != null && awb.contains(CameraMetadata.CONTROL_AWB_MODE_OFF)) "yes\n" else "no\n"
                )
                val ae = c.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)
                sb.append(
                    "AE off (manual exposure): " +
                        if (ae != null && ae.contains(CameraMetadata.CONTROL_AE_MODE_OFF)) "yes\n" else "no\n"
                )

                val zoom = c.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
                if (zoom != null) sb.append("zoom range: %.2fx..%.2fx\n".format(zoom.lower, zoom.upper))

                val regions = c.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) ?: 0
                if (regions > 0) sb.append("AE metering regions: $regions\n")
                val ecr = c.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
                if (ecr != null && ecr.lower != ecr.upper) {
                    sb.append("EV compensation: ${ecr.lower}..${ecr.upper} steps\n")
                }

                val fps = c.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                if (fps != null && fps.isNotEmpty()) {
                    val exact = fps.filter { it.lower == it.upper }
                        .joinToString(", ") { "${it.lower}" }
                    sb.append(
                        "fps lockable: " +
                            if (exact.isEmpty()) "none (exact)" else exact
                    )
                    sb.append("\n")
                    sb.append(
                        "fps ranges: " +
                            fps.joinToString(", ") { "${it.lower}-${it.upper}" }
                    )
                    sb.append("\n")
                }
                val eis = c.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
                if (eis != null) {
                    sb.append(
                        "video EIS: " +
                            if (eis.contains(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON)) "yes"
                            else "no"
                    )
                    sb.append("\n")
                }
                val ois = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                if (ois != null) {
                    sb.append(
                        "optical OIS: " +
                            if (ois.contains(CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON)) "yes"
                            else "no"
                    )
                    sb.append("\n")
                }

                val tm = c.get(CameraCharacteristics.TONEMAP_AVAILABLE_TONE_MAP_MODES)
                if (tm != null) {
                    val names = tm.map { m ->
                        when (m) {
                            CameraMetadata.TONEMAP_MODE_CONTRAST_CURVE -> "contrast-curve"
                            CameraMetadata.TONEMAP_MODE_FAST -> "fast"
                            CameraMetadata.TONEMAP_MODE_HIGH_QUALITY -> "high-quality"
                            CameraMetadata.TONEMAP_MODE_GAMMA_VALUE -> "gamma"
                            CameraMetadata.TONEMAP_MODE_PRESET_CURVE -> "preset"
                            else -> "mode$m"
                        }
                    }
                    sb.append("tone-map modes: ${names.joinToString(", ")}\n")
                    c.get(CameraCharacteristics.TONEMAP_MAX_CURVE_POINTS)?.let {
                        sb.append("tone-map max curve points: $it\n")
                    }
                }

                val caps = (c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                    ?: IntArray(0)).toList()
                val capNames = caps.mapNotNull { capName(it) }
                sb.append("capabilities: ${capNames.joinToString(", ")}\n")

                val map = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                if (map != null) {
                    val sizes = map.getOutputSizes(android.media.MediaRecorder::class.java)
                    if (sizes != null && sizes.isNotEmpty()) {
                        val max = sizes.maxByOrNull { it.width * it.height }!!
                        val sorted = sizes.sortedByDescending { it.width * it.height }.take(6)
                        sb.append("video max: ${max.width}x${max.height}\n")
                        sb.append("video modes: ${sorted.joinToString(", ") { "${it.width}x${it.height}" }}\n")
                        val highSpeed = map.highSpeedVideoSizes.size
                        sb.append("high-speed video modes (slow-mo capable): $highSpeed\n")
                    }
                }
            } catch (e: Exception) {
                sb.append("report failed: ${e.localizedMessage}\n")
            }
            sb.append("\n")
        }

        val report = sb.toString()

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.argb(255, 12, 12, 12))
        }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val reportView = TextView(this).apply {
            text = report
            setTextColor(Color.argb(255, 220, 220, 220))
            textSize = 13f
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(32, 96, 32, 64)
        }
        box.addView(reportView)
        // back to camera
        val done = TextView(this).apply {
            text = "⟨ back to camera"
            setTextColor(Color.argb(255, 255, 255, 90))
            textSize = 15f
            setPadding(32, 8, 32, 32)
        }
        done.setOnClickListener {
            enginelessFinish()
        }
        box.addView(done)
        scroll.addView(box)
        setContentView(scroll)
    }

    private fun enginelessFinish() {
        finish()
    }

    private fun capName(code: Int): String? =
        try {
            CameraMetadata::class.java.fields
                .filter { it.type == Int::class.javaPrimitiveType }
                .map { Pair(it.getInt(null), it.name) }
                .firstOrNull { it.first == code }?.second
        } catch (_: Exception) {
            null
        }
}
