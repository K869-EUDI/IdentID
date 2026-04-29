package com.k689.identid.ui.dashboard.loyaltycards.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.EnumMap

@Composable
fun BarcodeVisual(
    barcodeValue: String,
    barcodeFormat: String,
    modifier: Modifier = Modifier,
) {
    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, barcodeValue, barcodeFormat) {
        value =
            withContext(Dispatchers.Default) {
                runCatching {
                    createBarcodeBitmap(barcodeValue, barcodeFormat)
                }.getOrNull()
            }
    }
    val renderedBitmap = imageBitmap

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(20.dp),
                ).padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (renderedBitmap != null) {
            Image(
                bitmap = renderedBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(if (barcodeFormat == BarcodeFormat.QR_CODE.name) 260.dp else 140.dp),
            )
        } else {
            Text(
                text = barcodeValue,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun createBarcodeBitmap(
    value: String,
    formatName: String,
): ImageBitmap {
    val format = barcodeFormatFromName(formatName)
    val width = if (format == BarcodeFormat.QR_CODE) 720 else 960
    val height = if (format == BarcodeFormat.QR_CODE) 720 else 320
    val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
        put(EncodeHintType.MARGIN, 1)
    }
    val matrix = MultiFormatWriter().encode(value, format, width, height, hints)
    return matrix.toImageBitmap()
}

private fun barcodeFormatFromName(value: String): BarcodeFormat =
    runCatching { BarcodeFormat.valueOf(value) }.getOrElse { BarcodeFormat.QR_CODE }

private fun BitMatrix.toImageBitmap(): ImageBitmap {
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        val offset = y * width
        for (x in 0 until width) {
            pixels[offset + x] = if (get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
    }
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap.asImageBitmap()
}