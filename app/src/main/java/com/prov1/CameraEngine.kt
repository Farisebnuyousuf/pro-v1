package com.prov1

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.RectF
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import android.view.SurfaceTexture
import android.view.TextureView
import android.view.WindowManager

class CameraEngine(
    private val context: Context,
    private val onStatus: (String) -> Unit
) {
    private val manager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val lock = Any()

    private var thread: HandlerThread? = null
    private var handler: Handler? = null

    private var cameraId: String? = null
    private var sensorOrientation = 0
    private var device: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var textureView: TextureView? = null
    private var previewSize: Size? = null

    @Volatile private var opening = false

    fun start(view: TextureView) {
        textureView = view
        synchronized(lock) {
            if (thread == null) {
                thread = HandlerThread("pro-v1-camera").also { it.start() }
                handler = Handler(thread!!.looper)
            }
        }
        open()
    }

    fun stop() {
        synchronized(lock) {
            session?.close()
            session = null
            device?.close()
            device = null
            opening = false
        }
        onStatus("Preview stopped")
    }

    fun release() {
        stop()
        thread?.quitSafely()
        thread = null
        handler = null
        textureView = null
        previewSize = null
    }

    fun updateTransform() {
        val t = textureView ?: return
        val size = previewSize ?: return
        t.post { applyTransform(t, size, displayRotationDegrees()) }
    }

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    fun requestPermission(activity: android.app.Activity) {
        activity.requestPermissions(arrayOf(Manifest.permission.CAMERA), REQ_CAMERA)
    }

    private fun open() {
        if (device != null || opening) return
        val id = cameraId ?: pickCamera() ?: run {
            onStatus("No camera found on this device")
            return
        }
        opening = true
        onStatus("Opening camera…")
        val h = handler
        if (h == null) { opening = false; return }
        try {
            @SuppressLint("MissingPermission")
            manager.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(d: CameraDevice) {
                    synchronized(lock) {
                        opening = false
                        device = d
                    }
                    onStatus("Camera open — configuring…")
                    createSession(d)
                }
                override fun onDisconnected(d: CameraDevice) {
                    synchronized(lock) { device = null }
                    d.close()
                    onStatus("Camera disconnected")
                }
                override fun onError(d: CameraDevice, err: Int) {
                    synchronized(lock) { device = null; opening = false }
                    d.close()
                    onStatus("Camera error (code $err)")
                }
            }, h)
        } catch (e: SecurityException) {
            opening = false
            onStatus("Camera blocked by permission")
        } catch (e: Exception) {
            opening = false
            onStatus("openCamera failed: ${e.localizedMessage}")
        }
    }

    private fun pickCamera(): String? {
        var fallback: String? = null
        for (id in manager.cameraIdList) {
            val chars = manager.getCameraCharacteristics(id)
            if (chars.get(CameraCharacteristics.LENS_FACING) ==
                CameraMetadata.LENS_FACING_BACK
            ) {
                cameraId = id
                sensorOrientation =
                    chars.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: sensorOrientation
                return id
            }
            if (fallback == null) fallback = id
        }
        fallback?.let {
            cameraId = it
            sensorOrientation =
                manager.getCameraCharacteristics(it).let { c ->
                    c.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
                }
        }
        return fallback
    }

    private fun createSession(d: CameraDevice) {
        val t = textureView
        if (t == null || !t.isAvailable) {
            onStatus("Waiting for preview surface…")
            return
        }
        val st = t.surfaceTexture ?: run {
            onStatus("Preview surface not ready")
            return
        }
        val sizes = getOutputSizesFor(d, SurfaceTexture::class.java)
        if (sizes.isNullOrEmpty()) {
            onStatus("No output sizes reported")
            return
        }
        previewSize = pickSize(sizes, t.width, t.height)
        val size = previewSize!!
        st.setDefaultBufferSize(size.width, size.height)
        val surface = Surface(st)

        val h = handler
        if (h == null) return
        try {
            d.createCaptureSession(listOf(surface), object
                : CameraCaptureSession.StateCallback() {
                override fun onConfigured(s: CameraCaptureSession) {
                    synchronized(lock) { session = s }
                    val req = d.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        .apply {
                            addTarget(surface)
                            set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                        }
                    s.setRepeatingRequest(req.build(), null, h)
                    onStatus("Ready — ${size.width}x${size.height}")
                    applyTransform(t, size, displayRotationDegrees())
                }
                override fun onConfigureFailed(s: CameraCaptureSession) {
                    onStatus("Session configuration failed")
                }
            }, h)
        } catch (e: Exception) {
            onStatus("Session failed: ${e.localizedMessage}")
        }
    }

    private fun getOutputSizesFor(
        d: CameraDevice,
        klass: Class<SurfaceTexture>
    ): Array<Size> {
        return if (android.os.Build.VERSION.SDK_INT >= 31) {
            d.getOutputSizes(klass)
        } else {
            val maps = manager.getCameraCharacteristics(d.id)
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?: throw IllegalStateException("Stream configuration unavailable")
            maps.getOutputSizes(klass)
        }
    }

    private fun pickSize(sizes: Array<Size>, viewW: Int, viewH: Int): Size {
        val vw = viewW.coerceAtLeast(1)
        val vh = viewH.coerceAtLeast(1)
        // Normalize both ratios to "long side / short side" so portrait/landscape
        // views select the same-shaped buffers.
        val viewRatio = maxOf(vw, vh).coerceAtLeast(1).toDouble() /
            minOf(vw, vh).coerceAtLeast(1).toDouble()
        val viewArea = vw.toDouble() * vh.toDouble()
        var best = sizes.maxByOrNull { it.width * it.height } ?: sizes[0]
        var bestScore = Double.MAX_VALUE
        for (s in sizes) {
            val area = s.width.toDouble() * s.height
            // Fit-class buffers only; skip absurd outliers like 8K still sizes.
            if (area > viewArea * 1.6) continue
            val cand = maxOf(s.width, s.height).coerceAtLeast(1).toDouble() /
                minOf(s.width, s.height).coerceAtLeast(1).toDouble()
            val score = kotlin.math.abs(cand - viewRatio) * 100.0 - area / 1e7
            if (score < bestScore) {
                bestScore = score
                best = s
            }
        }
        return best
    }

    private fun applyTransform(
        t: TextureView,
        size: Size,
        displayDegrees: Int
    ) {
        val viewW = t.width
        val viewH = t.height
        if (viewW == 0 || viewH == 0) return
        val m = Matrix()
        val view = RectF(0f, 0f, viewW.toFloat(), viewH.toFloat())
        val cx = view.centerX()
        val cy = view.centerY()
        when (displayDegrees) {
            90, 270 -> {
                val buf = RectF(0f, 0f, size.height.toFloat(), size.width.toFloat())
                buf.offset(cx - buf.centerX(), cy - buf.centerY())
                m.setRectToRect(view, buf, Matrix.ScaleToFit.FILL)
                m.postRotate(90f * (displayDegrees / 90 - 2), cx, cy)
            }
            180 -> m.postRotate(180f, cx, cy)
            else -> { }
        }
        t.setTransform(m)
    }

    @Suppress("DEPRECATION")
    private fun displayRotationDegrees(): Int {
        val rot = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay.rotation
        return when (rot) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
    }

    companion object {
        const val REQ_CAMERA = 101
    }
}
