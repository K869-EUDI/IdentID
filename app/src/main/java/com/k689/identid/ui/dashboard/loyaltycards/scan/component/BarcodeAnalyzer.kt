package com.k689.identid.ui.dashboard.loyaltycards.scan.component

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

data class BarcodeScanResult(
    val value: String,
    val format: String,
)

class BarcodeAnalyzer(
    private val onBarcodeScanned: (BarcodeScanResult) -> Unit,
) : ImageAnalysis.Analyzer {
    private val scanner =
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_QR_CODE,
                ).build(),
        )

    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image
        if (mediaImage == null) {
            image.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
        scanner
            .process(inputImage)
            .addOnSuccessListener { barcodes ->
                val firstSupported =
                    barcodes.firstOrNull { barcode ->
                        !barcode.rawValue.isNullOrBlank() && barcode.format != Barcode.FORMAT_UNKNOWN
                    }

                if (firstSupported != null) {
                    onBarcodeScanned(
                        BarcodeScanResult(
                            value = firstSupported.rawValue.orEmpty(),
                            format = firstSupported.format.toFormatName(),
                        ),
                    )
                }
            }.addOnCompleteListener {
                image.close()
            }
    }
}

private fun Int.toFormatName(): String =
    when (this) {
        Barcode.FORMAT_EAN_13 -> "EAN_13"
        Barcode.FORMAT_EAN_8 -> "EAN_8"
        Barcode.FORMAT_CODE_128 -> "CODE_128"
        Barcode.FORMAT_CODE_39 -> "CODE_39"
        Barcode.FORMAT_UPC_A -> "UPC_A"
        Barcode.FORMAT_QR_CODE -> "QR_CODE"
        else -> "UNKNOWN"
    }