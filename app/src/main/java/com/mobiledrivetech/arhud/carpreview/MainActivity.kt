package com.mobiledrivetech.arhud.carpreview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.Surface.ROTATION_0
import android.view.Surface.ROTATION_90
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity() {

    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null

    // References for each use cases
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Set up the listener for "Take Video" button
        camera_capture_button.setOnClickListener {
            if (camera_capture_button.text == "Take Video") {
                camera_capture_button.text = "Stop Recording"
                takeVideo()
            } else {
                camera_capture_button.text = "Take Video"
                videoCapture!!.stopRecording()
            }
        }

        // Set up the listener for "Take photo" button
        camera_capture_button1.setOnClickListener { takePhoto() }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    @SuppressLint("RestrictedApi")
    private fun takeVideo() {
        // Get a stable reference of the modifiable video capture use case
        val videoCapture = videoCapture ?: return

        // Create time-stamped output file to hold the video
        val videoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.TAIWAN
            ).format(System.currentTimeMillis()) + ".mp4")

        // Create output options object which contains file + metadata
        val outputOptions = VideoCapture.OutputFileOptions.Builder(videoFile).build()

        // Set up video capture listener, which is triggered after video has
        // been taken
        videoCapture.startRecording(
            outputOptions, ContextCompat.getMainExecutor(this), object : VideoCapture.OnVideoSavedCallback {
                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                    Log.e(TAG, "Video capture failed: $message")
                }

                override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(videoFile)
                    val msg = "Video capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            })
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.TAIWAN
            ).format(System.currentTimeMillis()) + ".jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            })
    }

    @SuppressLint("RestrictedApi")
    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture!!.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture!!.get()

            // Preview use case
            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewFrame.surfaceProvider)
                }

            // Image capture use case
            imageCapture = ImageCapture.Builder()
                .build()

            // Video capture use case
            videoCapture = VideoCapture.Builder()
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera (Some devices could not support more than two use cases)
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, videoCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = filesDir.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    @SuppressLint("RestrictedApi")
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        videoCapture!!.stopRecording()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        // The lists of required permissions
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO)
    }
}