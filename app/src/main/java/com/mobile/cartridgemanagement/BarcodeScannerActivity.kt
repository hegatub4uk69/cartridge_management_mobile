package com.mobile.cartridgemanagement

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeScannerActivity : AppCompatActivity() {
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var cameraPreview: PreviewView

    // Список поддерживаемых форматов для проверки
    private val supportedFormats = listOf(
        Barcode.FORMAT_CODE_128,
        Barcode.FORMAT_QR_CODE,
        Barcode.FORMAT_EAN_13
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_scanner)
        cameraPreview = findViewById(R.id.previewView)

        // Настройка сканера только для поддерживаемых форматов
        barcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_EAN_13
                )
                .build()
        )

        startCamera()
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(cameraPreview.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                    processImage(imageProxy)
                }
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalysis
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка камеры: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                val detectedBarcode = barcodes.firstOrNull()

                detectedBarcode?.let { barcode ->
                    if (barcode.format in supportedFormats) {
                        // Поддерживаемый формат - возвращаем результат
                        Intent().apply {
                            putExtra("SCAN_RESULT", barcode.rawValue)
                            setResult(RESULT_OK, this)
                            finish()
                        }
                    } else {
                        // Неподдерживаемый формат - показываем сообщение
                        showUnsupportedFormatMessage(barcode.format)
                    }
                }
            }
            .addOnFailureListener { e ->
                // Обработка ошибок сканирования
                Log.e("BarcodeScanner", "Ошибка сканирования", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun showUnsupportedFormatMessage(format: Int) {
        runOnUiThread {
            val formatName = getFormatName(format)
            Toast.makeText(
                this,
                "Неподдерживаемый формат штрих-кода: $formatName",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    private fun getFormatName(format: Int): String {
        return when (format) {
            Barcode.FORMAT_CODE_39 -> "Code 39"
            Barcode.FORMAT_CODE_93 -> "Code 93"
            Barcode.FORMAT_CODABAR -> "Codabar"
            Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
            Barcode.FORMAT_EAN_8 -> "EAN-8"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_UPC_A -> "UPC-A"
            Barcode.FORMAT_UPC_E -> "UPC-E"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_AZTEC -> "Aztec"
            else -> "Неизвестный формат ($format)"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        barcodeScanner.close()
    }
}