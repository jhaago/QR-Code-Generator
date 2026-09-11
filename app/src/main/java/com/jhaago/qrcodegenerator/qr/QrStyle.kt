package com.jhaago.qrcodegenerator.qr

enum class QrStyle(
    val displayName: String,
    val description: String,
) {
    CLASSIC("Classic", "Solid square modules"),
    ROUNDED("Rounded", "Soft rounded modules"),
    DOTS("Dots", "Clean circular modules"),
    GAPPED("Gapped", "Crisp separated tiles"),
}
