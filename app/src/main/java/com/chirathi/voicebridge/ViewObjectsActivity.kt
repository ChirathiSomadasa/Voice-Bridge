package com.chirathi.voicebridge

import android.Manifest
import android.content.Intent
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
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import java.util.Locale

class ViewObjectsActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView //show the live camera
    private lateinit var resultText: TextView //display status
    private lateinit var yoloDetector: YoloV5Detector //load yolov5 model
    private lateinit var labels: List<String> //load labels
    private var detectedImageBytes: ByteArray? = null //store image byte to sent to phraseactivity
    private lateinit var buttonContainer: LinearLayout
    private lateinit var btnIWant: Button
    private lateinit var btnISee: Button
    private lateinit var btnThisIs: Button
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var imageAnalyzer: ImageAnalysis
    private var isObjectDetected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_objects)
        previewView = findViewById(R.id.previewView)
        resultText = findViewById(R.id.resultText)
        buttonContainer = findViewById(R.id.buttonContainer)
        btnIWant = findViewById(R.id.btnIWant)
        btnISee = findViewById(R.id.btnISee)
        btnThisIs = findViewById(R.id.btnThisIs)

        try {
            // Load labels from assets directory
            labels = FileUtil.loadLabels(this, "labels.txt")
            Log.d("ObjectDetection", "Labels loaded: ${labels.size} classes")

            // Initialize detector with model from ml directory
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
                // REMOVE 'val' here to use the class-level variable
                cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // REMOVE 'val' here to use the class-level variable
                imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetRotation(previewView.display.rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                    if (!isObjectDetected) {
                        processImage(imageProxy)
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            } catch (e: Exception) {
                Log.e("ObjectDetection", "Camera start failed: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }
    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
    }

    private fun processImage(imageProxy: ImageProxy) {
        try {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val rawBitmap = imageProxy.toBitmap()
            val bitmap = rotateBitmap(rawBitmap, rotation)
            detectedImageBytes = bitmapToByteArray(bitmap)

            val multipart = bitmapToMultipart(bitmap)

            // CREATE THE HINT (Pass an empty string or the YOLO detected label)
            val hintText = "object" // You could pass yoloDetector results here if you want
            val hintBody = RequestBody.create(MultipartBody.FORM, hintText)

            // UPDATED CALL WITH HINT
            RetrofitClient.instance.uploadImage(multipart, hintBody).enqueue(object : retrofit2.Callback<CaptionResponse> {
                override fun onResponse(call: retrofit2.Call<CaptionResponse>, response: retrofit2.Response<CaptionResponse>) {
                    if (response.isSuccessful && !isObjectDetected) {
                        isObjectDetected = true
                        val objectName = response.body()?.caption ?: "object"

                        runOnUiThread {
                            stopCamera()

                            // Show toast only
                            Toast.makeText(this@ViewObjectsActivity, "Object Detected", Toast.LENGTH_SHORT).show()

                            // Do not show sentence here
                            resultText.text = "Object detected"

                            showButtons(objectName)
                        }
                    }
                }

                override fun onFailure(call: retrofit2.Call<CaptionResponse>, t: Throwable) {
//                    runOnUiThread {
//                        resultText.text = "Server connecting... please wait"
//                    }
                }
            })
        } catch (e: Exception) {
            Log.e("ObjectDetection", "Process error: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }
    private fun stopCamera() {
        try {
            imageAnalyzer.clearAnalyzer()
            cameraProvider.unbindAll()
        } catch (e: Exception) {
            Log.e("CameraX", "Failed to stop camera: ${e.message}")
        }
    }


    private fun showButtons(objectName: String) {
        buttonContainer.visibility = View.VISIBLE

        btnIWant.setOnClickListener {
            navigateToPhraseActivity("I want $objectName")
        }

        btnISee.setOnClickListener {
            navigateToPhraseActivity("I see $objectName")
        }

        btnThisIs.setOnClickListener {
            navigateToPhraseActivity("This is $objectName")
        }
    }


    private fun navigateToPhraseActivity(detectedSentence: String) {

        val intent = Intent(this, PhraseActivity::class.java)

        // Pass detected sentence
        intent.putExtra("SELECTED_PHRASE", detectedSentence)

        detectedImageBytes?.let {
            intent.putExtra("DETECTED_IMAGE", it)
        }
        //  default icon
        intent.putExtra("SELECTED_ICON_DRAWABLE", R.drawable.play)

        startActivity(intent)
        finish() // stop camera screen
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

    private fun bitmapToMultipart(
        bitmap: Bitmap,
        filename: String = "image.jpg"
    ): MultipartBody.Part {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos)
        val byteArray = bos.toByteArray()
        val requestBody = RequestBody.create("image/*".toMediaTypeOrNull(), byteArray)
        return MultipartBody.Part.createFormData("image", filename, requestBody)
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return stream.toByteArray()
    }




}