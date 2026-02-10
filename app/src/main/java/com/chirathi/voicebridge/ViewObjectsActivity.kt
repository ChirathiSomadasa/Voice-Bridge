package com.chirathi.voicebridge

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.tensorflow.lite.support.common.FileUtil
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import android.speech.tts.TextToSpeech
import java.util.Locale

class ViewObjectsActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var resultText: TextView
    private lateinit var yoloDetector: YoloV5Detector
    private lateinit var labels: List<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_objects)
        previewView = findViewById(R.id.previewView)
        resultText = findViewById(R.id.resultText)

        try {
            // Load labels from assets directory
            labels = FileUtil.loadLabels(this, "labels.txt")
            Log.d("ObjectDetection", "Labels loaded: ${labels.size} classes")

            // Initialize detector with model from ml directory
            // Use the actual filename you have in ml folder
            yoloDetector = YoloV5Detector(this, "ml/object-detection-model.tflite")
            Log.d("ObjectDetection", "Model loaded successfully")

        } catch (e: Exception) {
            Log.e("ObjectDetection", "Failed to load model/labels: ${e.message}")
            e.printStackTrace()
            resultText.text = "Error loading model"
            return
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                101
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            resultText.text = "Camera permission required"
        }
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetRotation(previewView.display.rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalyzer.setAnalyzer(
                    Executors.newSingleThreadExecutor()
                ) { imageProxy ->
                    processImage(imageProxy)
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )

            } catch (e: Exception) {
                Log.e("ObjectDetection", "Camera start failed: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImage(imageProxy: ImageProxy) {
        try {
            val bitmap = imageProxy.toBitmap()

            // Convert bitmap to multipart
            val multipart = bitmapToMultipart(bitmap)

            // Send to server
            RetrofitClient.instance.uploadImage(multipart).enqueue(object : retrofit2.Callback<CaptionResponse> {
                override fun onResponse(call: retrofit2.Call<CaptionResponse>, response: retrofit2.Response<CaptionResponse>) {
                    if (response.isSuccessful) {
                        val caption = response.body()?.caption ?: "No caption"
                        runOnUiThread {
                            resultText.text = caption
                        }
                    } else {
                        runOnUiThread { resultText.text = "Server error" }
                    }
                }

                override fun onFailure(call: retrofit2.Call<CaptionResponse>, t: Throwable) {
                    runOnUiThread { resultText.text = "Request failed: ${t.message}" }
                }
            })

        } catch (e: Exception) {
            Log.e("ObjectDetection", "Process error: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }


    private fun ImageProxy.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)

        // For NV21 format: YYYY... VUVU...
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = android.graphics.YuvImage(
            nv21,
            android.graphics.ImageFormat.NV21,
            width,
            height,
            null
        )

        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 90, out)
        return BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
    }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    private fun bitmapToMultipart(bitmap: Bitmap, filename: String = "image.jpg"): MultipartBody.Part {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos)
        val byteArray = bos.toByteArray()
        val requestBody = RequestBody.create("image/*".toMediaTypeOrNull(), byteArray)
        return MultipartBody.Part.createFormData("image", filename, requestBody)
    }



}