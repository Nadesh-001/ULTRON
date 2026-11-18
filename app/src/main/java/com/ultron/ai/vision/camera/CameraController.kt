package com.ultron.ai.vision.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.ultron.ai.vision.image.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * CameraX controller for ULTRON Vision AI.
 * Coordinates hardware preview, capture, torch/flash, and lens orientation.
 */
class CameraController(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null

    private val _isFlashOn = MutableStateFlow(false)
    val isFlashOn: StateFlow<Boolean> = _isFlashOn.asStateFlow()

    private val _isFrontFacing = MutableStateFlow(false)
    val isFrontFacing: StateFlow<Boolean> = _isFrontFacing.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    /**
     * Binds CameraX preview and image capture to the provided [lifecycleOwner] and [previewView].
     */
    suspend fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ): Unit = withContext(Dispatchers.Main) {
        val provider = getCameraProvider()
        cameraProvider = provider

        val cameraSelector = if (_isFrontFacing.value) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            _isReady.value = true
            // Restore flash if was enabled
            if (_isFlashOn.value && !_isFrontFacing.value) {
                camera?.cameraControl?.enableTorch(true)
            }
        } catch (e: Exception) {
            _isReady.value = false
        }
    }

    /**
     * Toggles hardware torch / flash.
     */
    fun toggleFlash() {
        val next = !_isFlashOn.value
        _isFlashOn.value = next
        camera?.cameraControl?.enableTorch(next)
    }

    /**
     * Switches between front and back camera lenses.
     */
    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        _isFrontFacing.value = !_isFrontFacing.value
        if (_isFrontFacing.value) {
            _isFlashOn.value = false
        }
        lifecycleOwner.let {
            // Re-bind with new lens
            androidx.lifecycle.lifecycleScope.let {
                // Trigger re-start via caller or direct call
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    bindCameraSafely(lifecycleOwner, previewView)
                }
            }
        }
    }

    private fun bindCameraSafely(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val provider = cameraProvider ?: return
        val cameraSelector = if (_isFrontFacing.value) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (_: Exception) {}
    }

    /**
     * Captures a single frame as a normalized, rotated [Bitmap].
     */
    suspend fun captureImage(): Bitmap = suspendCancellableCoroutine { continuation ->
        val capture = imageCapture
        if (capture == null) {
            continuation.resumeWithException(IllegalStateException("Camera capture is not initialized"))
            return@suspendCancellableCoroutine
        }

        val executor: Executor = ContextCompat.getMainExecutor(context)
        capture.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val rotation = image.imageInfo.rotationDegrees
                        val bitmap = imageProxyToBitmap(image)
                        image.close()

                        val orientedBitmap = ImageProcessor.rotateBitmap(bitmap, rotation)
                        val scaled = ImageProcessor.scaleBitmap(orientedBitmap)
                        continuation.resume(scaled)
                    } catch (e: Exception) {
                        image.close()
                        continuation.resumeWithException(e)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    continuation.resumeWithException(exception)
                }
            }
        )
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private suspend fun getCameraProvider(): ProcessCameraProvider =
        suspendCancellableCoroutine { continuation ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                {
                    try {
                        continuation.resume(future.get())
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                },
                ContextCompat.getMainExecutor(context)
            )
        }

    /**
     * Safely unbinds and releases CameraX hardware resources.
     */
    fun stopCamera() {
        try {
            camera?.cameraControl?.enableTorch(false)
            cameraProvider?.unbindAll()
            _isReady.value = false
            _isFlashOn.value = false
        } catch (_: Exception) {}
    }
}
