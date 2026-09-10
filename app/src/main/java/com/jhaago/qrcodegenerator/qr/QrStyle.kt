package com.jhaago.qrcodegenerator.qr

enum class QrStyle(
    val displayName: String,
    val description: String,
) {
    CLASSIC("Classic", "Maximum compatibility"),
    ROUNDED("Rounded", "Soft rounded modules"),
    DOTS("Dots", "Modern dot pattern"),
}
