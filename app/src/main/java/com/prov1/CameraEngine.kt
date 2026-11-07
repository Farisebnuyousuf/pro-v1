package com.prov1

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private var sensorOrientation = 90
    private var backCamera = true
    private var device: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var textureView: TextureView? = null
    private var previewSize: Size? = null
    private var videoSize: Size = Size(1920, 1080)

    private var previewSurface: Surface? = null

    // M1 recorder state
    private var recorder: MediaRecorder? = null
    private var recorderSurface: Surface? = null
    private var outputDescriptor: ParcelFileDescriptor? = null
    private var outputUri: Uri? = null
    private var outputPath: File? = null
    private var onRecordDone: ((String) -> Unit)? = null

    @Volatile var isRecording = false
        private set

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
        abandonRecording()
        synchronized(lock) {
            session?.close()
            session = null
            device?.close()
            device = null
            opening = false
        }
        // Surface may be destroyed with the activity; drop refs so resume rebuilds.
        previewSurface = null
        previewSize = null
    }

    fun release() {
        stop()
        thread?.quitSafely()
        thread = null
        handler = null
        textureView = null
        previewSurface = null
        previewSize = null
    }

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    fun hasAudioPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun requestPermission(activity: android.app.Activity) {
        activity.requestPermissions(arrayOf(Manifest.permission.CAMERA), REQ_CAMERA)
    }

    fun updateTransform() {
        val t = textureView ?: return
        val size = previewSize ?: return
        t.post { applyTransform(t, size, displayRotationDegrees()) }
    }

    // ---------- camera open / preview ----------

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
                    buildSession(withRecorder = false)
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
                backCamera = true
                sensorOrientation =
                    chars.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: sensorOrientation
                return id
            }
            if (fallback == null) fallback = id
        }
        fallback?.let {
            cameraId = it
            backCamera = false
            sensorOrientation =
                manager.getCameraCharacteristics(it).get(CameraCharacteristics.SENSOR_ORIENTATION)
                    ?: sensorOrientation
        }
        return fallback
    }

    private fun streamMap(): StreamConfigurationMap {
        val id = cameraId ?: throw IllegalStateException("Camera not selected")
        val chars = manager.getCameraCharacteristics(id)
        return chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: throw IllegalStateException("Stream configuration unavailable")
    }

    private fun getOutputSizesFor(d: CameraDevice, klass: Class<SurfaceTexture>): Array<Size> =
        streamMap().getOutputSizes(klass)

    private fun buildSession(withRecorder: Boolean) {
        val d = device
        if (d == null) {
            onStatus("Camera not ready")
            return
        }
        val t = textureView
        if (t == null || !t.isAvailable) {
            onStatus("Waiting for preview surface…")
            return
        }
        val st = t.surfaceTexture ?: run {
            onStatus("Preview surface not ready")
            return
        }
        if (previewSize == null) {
            val sizes = getOutputSizesFor(d, SurfaceTexture::class.java)
            previewSize = pickSize(sizes, t.width, t.height)
        }
        val size = previewSize!!
        st.setDefaultBufferSize(size.width, size.height)
        val surface = previewSurface ?: Surface(st).also { previewSurface = it }

        val outputs = mutableListOf(surface)
        val recSurf = recorderSurface
        if (withRecorder && isRecording) {
            recSurf?.let { outputs.add(it) }
        }

        val h = handler
        if (h == null) return
        try {
            d.createCaptureSession(outputs, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(s: CameraCaptureSession) {
                    synchronized(lock) { session = s }
                    val template =
                        if (withRecorder && isRecording) CameraDevice.TEMPLATE_RECORD
                        else CameraDevice.TEMPLATE_PREVIEW
                    val req = d.createCaptureRequest(template)
                        .apply {
                            addTarget(surface)
                            if (withRecorder && isRecording) recSurf?.let { addTarget(it) }
                            set(
                                CaptureRequest.CONTROL_AF_MODE,
                                if (withRecorder && isRecording)
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                                else
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                        }
                    s.setRepeatingRequest(req.build(), null, h)
                    if (withRecorder && isRecording) {
                        recorder?.start()
                        onStatus("REC — recording…")
                        applyTransform(t, size, displayRotationDegrees())
                    } else {
                        onStatus("Ready — ${size.width}x${size.height}")
                        applyTransform(t, size, displayRotationDegrees())
                    }
                }
                override fun onConfigureFailed(s: CameraCaptureSession) {
                    onStatus("Session configuration failed")
                }
            }, h)
        } catch (e: Exception) {
            onStatus("Session failed: ${e.localizedMessage}")
        }
    }

    // ---------- recording ----------

    fun startRecording(onDone: (String) -> Unit) {
        val h = handler
        if (h == null) { onDone("Engine stopped"); return }
        val d = device
        if (d == null) { onDone("Camera not ready"); return }
        if (isRecording) { onDone("Already recording"); return }
        onRecordDone = onDone
        val hint = computeVideoOrientation()
        try {
            val rec = createRecorder(hint)
            rec.prepare()
            recorder = rec
            recorderSurface = rec.surface
            isRecording = true // intent flag; actual start happens in onConfigured
            log("recorder prepared ${videoSize.width}x${videoSize.height}")
        } catch (e: Exception) {
            cleanupRecorder()
            onDone("Recorder failed: ${e.localizedMessage}")
            return
        }
        h.post { buildSession(withRecorder = true) }
    }

    fun stopRecording(onDone: (String) -> Unit) {
        val h = handler
        if (h == null) { onDone("Engine stopped"); return }
        if (recorder == null) { onDone("Not recording"); return }
        onRecordDone = onDone
        h.post { finishRecording() }
    }

    private fun finishRecording() {
        val rec = recorder ?: return
        isRecording = false
        val ok = try {
            rec.stop()
            true
        } catch (e: Exception) {
            log("stop failed: ${e.localizedMessage}")
            false
        }
        try { rec.reset(); rec.release() } catch (_: Exception) { }
        recorder = null
        recorderSurface = null
        val saved = finalizeOutput(ok)
        rebuildPreviewSession()
        onRecordDone?.invoke(
            when {
                ok && saved.startsWith("Saved") -> saved
                ok -> "Recording discarded"
                else -> "Recording was empty — discarded"
            }
        )
        onRecordDone = null
    }

    private fun rebuildPreviewSession() {
        val d = device
        if (d == null) return
        buildSession(withRecorder = false)
    }

    private fun abandonRecording() {
        if (recorder != null) {
            val rec = recorder
            isRecording = false
            try { rec?.stop() } catch (_: Exception) { }
            try { rec?.reset(); rec?.release() } catch (_: Exception) { }
            recorder = null
            recorderSurface = null
            finalizeOutput(false)
        }
        outputDescriptor = null
        outputUri = null
        outputPath = null
    }

    @Suppress("DEPRECATION")
    private fun createRecorder(hintDeg: Int): MediaRecorder {
        val rec = MediaRecorder() // legacy ctor: works on every Android for minSdk 26
        rec.setAudioSource(MediaRecorder.AudioSource.MIC)
        rec.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        rec.setAudioEncodingBitRate(128_000)
        rec.setAudioSamplingRate(44_100)
        rec.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        rec.setVideoEncodingBitRate(12_000_000)
        rec.setVideoFrameRate(30)
        videoSize = pickVideoSize()
        rec.setVideoSize(videoSize.width, videoSize.height)
        rec.setOrientationHint(hintDeg)
        openOutput(rec)
        return rec
    }

    private fun pickVideoSize(): Size {
        val sizes = streamMap().getOutputSizes(MediaRecorder::class.java)
        val target = 16.0 / 9.0
        var best: Size? = null
        var bestArea = 0
        for (s in sizes) {
            val ratio = maxOf(s.width, s.height).toDouble() /
                minOf(s.width, s.height).coerceAtLeast(1)
            if (ratio < 1.70 || ratio > 1.86) continue
            val area = s.width * s.height
            // M1 default: keep to 1080p-class; 4K comes with the quality milestone.
            if (area > 2_200_000) continue
            if (area > bestArea) {
                bestArea = area
                best = s
            }
        }
        val intent = best
        if (intent != null) return intent
        return sizes.maxByOrNull { it.width * it.height } ?: Size(1920, 1080)
    }

    private fun computeVideoOrientation(): Int {
        val display = displayRotationDegrees()
        return if (backCamera) {
            (sensorOrientation - display + 360) % 360
        } else {
            (sensorOrientation + display + 360) % 360
        }
    }

    private fun openOutput(rec: MediaRecorder) {
        val name = "ProV1_" +
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".mp4"
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Pro V1")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values
            )
            val fd = context.contentResolver.openFileDescriptor(uri!!, "rw")
            outputDescriptor = fd
            outputUri = uri
            rec.setOutputFile(fd!!.fileDescriptor)
        } else {
            val dir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "Pro V1"
            )
            dir.mkdirs()
            val file = File(dir, name)
            outputPath = file
            rec.setOutputFile(file.absolutePath)
        }
    }

    private fun finalizeOutput(ok: Boolean): String {
        val uri = outputUri
        if (uri != null) {
            val values = ContentValues()
            if (ok) {
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                outputDescriptor?.close()
                outputUri = null
                outputDescriptor = null
                return "Saved to gallery"
            } else {
                context.contentResolver.delete(uri, null, null)
                outputDescriptor?.close()
                outputUri = null
                outputDescriptor = null
                return "Failed"
            }
        }
        val file = outputPath
        if (file != null) {
            if (ok) return "Saved to ${file.parentFile?.name}"
            file.delete()
        }
        outputPath = null
        return "Failed"
    }

    // ---------- utils ----------

    private fun pickSize(sizes: Array<Size>, viewW: Int, viewH: Int): Size {
        val vw = viewW.coerceAtLeast(1)
        val vh = viewH.coerceAtLeast(1)
        val viewRatio = maxOf(vw, vh).coerceAtLeast(1).toDouble() /
            minOf(vw, vh).coerceAtLeast(1).toDouble()
        val viewArea = vw.toDouble() * vh.toDouble()
        var best = sizes.maxByOrNull { it.width * it.height } ?: sizes[0]
        var bestScore = Double.MAX_VALUE
        for (s in sizes) {
            val area = s.width.toDouble() * s.height
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

    private fun cleanupRecorder() {
        try { recorder?.reset(); recorder?.release() } catch (_: Exception) { }
        recorder = null
        recorderSurface = null
        outputDescriptor?.close()
        outputDescriptor = null
        outputUri = null
        outputPath = null
        isRecording = false
    }

    private fun log(msg: String) {
        android.util.Log.i("pro-v1", msg)
        onStatus(msg)
    }

    companion object {
        const val REQ_CAMERA = 101
        const val REQ_AUDIO = 202
    }
}
