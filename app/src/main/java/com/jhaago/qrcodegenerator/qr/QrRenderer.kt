package com.jhaago.qrcodegenerator.qr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

object QrRenderer {
    private const val QUIET_ZONE_MODULES = 4

    fun render(
        content: String,
        style: QrStyle,
        requestedSizePx: Int = 1200,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE,
    ): Bitmap {
        require(content.isNotBlank()) { "QR content cannot be blank." }

        val qrCode = Encoder.encode(
            content,
            ErrorCorrectionLevel.H,
            mapOf(EncodeHintType.CHARACTER_SET to "UTF-8"),
        )
        val matrix = requireNotNull(qrCode.matrix) { "Unable to create QR matrix." }
        val moduleCount = matrix.width
        val totalModules = moduleCount + (QUIET_ZONE_MODULES * 2)
        val moduleSizePx = (requestedSizePx / totalModules).coerceAtLeast(1)
        val outputSizePx = moduleSizePx * totalModules

        val bitmap = Bitmap.createBitmap(outputSizePx, outputSizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(backgroundColor)

        val squarePaint = Paint().apply {
            color = foregroundColor
            isAntiAlias = false
            style = Paint.Style.FILL
        }
        val styledPaint = Paint(squarePaint).apply { isAntiAlias = true }

        for (y in 0 until moduleCount) {
            for (x in 0 until moduleCount) {
                if (matrix.get(x, y).toInt() != 1) continue

                val left = ((x + QUIET_ZONE_MODULES) * moduleSizePx).toFloat()
                val top = ((y + QUIET_ZONE_MODULES) * moduleSizePx).toFloat()
                val right = left + moduleSizePx
                val bottom = top + moduleSizePx

                // Keep the three finder patterns square for dependable scanning even
                // when the data modules use a decorative style.
                if (isFinderPatternModule(x, y, moduleCount) || style == QrStyle.CLASSIC) {
                    canvas.drawRect(left, top, right, bottom, squarePaint)
                    continue
                }

                when (style) {
                    QrStyle.CLASSIC -> Unit
                    QrStyle.ROUNDED -> {
                        val inset = moduleSizePx * 0.06f
                        val radius = moduleSizePx * 0.28f
                        canvas.drawRoundRect(
                            RectF(left + inset, top + inset, right - inset, bottom - inset),
                            radius,
                            radius,
                            styledPaint,
                        )
                    }
                    QrStyle.DOTS -> {
                        val centerX = (left + right) / 2f
                        val centerY = (top + bottom) / 2f
                        canvas.drawCircle(centerX, centerY, moduleSizePx * 0.36f, styledPaint)
                    }
                }
            }
        }

        return bitmap
    }

    private fun isFinderPatternModule(x: Int, y: Int, moduleCount: Int): Boolean {
        val inTopLeft = x <= 6 && y <= 6
        val inTopRight = x >= moduleCount - 7 && y <= 6
        val inBottomLeft = x <= 6 && y >= moduleCount - 7
        return inTopLeft || inTopRight || inBottomLeft
    }
}
