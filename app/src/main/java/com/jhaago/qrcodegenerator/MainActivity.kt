package com.jhaago.qrcodegenerator

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jhaago.qrcodegenerator.qr.QrRenderer
import com.jhaago.qrcodegenerator.qr.QrStyle
import com.jhaago.qrcodegenerator.ui.theme.QrGeneratorTheme
import com.jhaago.qrcodegenerator.util.ImageExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

private data class ColorOption(
    val name: String,
    val value: Int,
)

private val foregroundOptions = listOf(
    ColorOption("Black", 0xFF111111.toInt()),
    ColorOption("Navy", 0xFF16345B.toInt()),
    ColorOption("Forest", 0xFF174D3B.toInt()),
    ColorOption("Plum", 0xFF51345D.toInt()),
    ColorOption("Charcoal", 0xFF3A3A3A.toInt()),
)

private val backgroundOptions = listOf(
    ColorOption("White", 0xFFFFFFFF.toInt()),
    ColorOption("Cream", 0xFFFFF7E8.toInt()),
    ColorOption("Ice", 0xFFF0F7FF.toInt()),
    ColorOption("Mint", 0xFFEFFAF3.toInt()),
    ColorOption("Blush", 0xFFFFF2F4.toInt()),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QrGeneratorTheme {
                QrGeneratorScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrGeneratorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var content by rememberSaveable { mutableStateOf("") }
    var selectedStyleName by rememberSaveable { mutableStateOf(QrStyle.CLASSIC.name) }
    var foregroundColor by rememberSaveable { mutableIntStateOf(foregroundOptions.first().value) }
    var backgroundColor by rememberSaveable { mutableIntStateOf(backgroundOptions.first().value) }
    var foregroundHex by rememberSaveable { mutableStateOf(formatColorHex(foregroundColor)) }
    var backgroundHex by rememberSaveable { mutableStateOf(formatColorHex(backgroundColor)) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val selectedStyle = QrStyle.valueOf(selectedStyleName)

    fun invalidateQr() {
        qrBitmap = null
        errorMessage = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "QR Code Generator",
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Spacer(Modifier.height(2.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Create a QR in seconds",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Paste a link, choose a look, then save or share it. Everything is generated on this phone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                    )
                }
            }

            SectionTitle("1. Add your link")

            OutlinedTextField(
                value = content,
                onValueChange = {
                    content = it
                    invalidateQr()
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://example.com") },
                label = { Text("Link or text") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                supportingText = {
                    Text(errorMessage ?: "Links, plain text and other QR-compatible content are supported.")
                },
                isError = errorMessage != null,
            )

            SectionTitle("2. Choose a style")

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(QrStyle.entries) { style ->
                    StyleCard(
                        style = style,
                        selected = style == selectedStyle,
                        foregroundColor = foregroundColor,
                        backgroundColor = backgroundColor,
                        onClick = {
                            selectedStyleName = style.name
                            invalidateQr()
                        },
                    )
                }
            }

            SectionTitle("3. Pick your colours")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ColorSwatchRow(
                        title = "QR colour",
                        options = foregroundOptions,
                        selectedColor = foregroundColor,
                        onColorSelected = { color ->
                            foregroundColor = color
                            foregroundHex = formatColorHex(color)
                            invalidateQr()
                        },
                    )

                    ColorSwatchRow(
                        title = "Background",
                        options = backgroundOptions,
                        selectedColor = backgroundColor,
                        onColorSelected = { color ->
                            backgroundColor = color
                            backgroundHex = formatColorHex(color)
                            invalidateQr()
                        },
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = foregroundHex,
                            onValueChange = { value ->
                                foregroundHex = value.uppercase(Locale.ROOT)
                                parseHexColor(value)?.let {
                                    foregroundColor = it
                                    invalidateQr()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("QR hex") },
                            singleLine = true,
                        )

                        OutlinedTextField(
                            value = backgroundHex,
                            onValueChange = { value ->
                                backgroundHex = value.uppercase(Locale.ROOT)
                                parseHexColor(value)?.let {
                                    backgroundColor = it
                                    invalidateQr()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Background hex") },
                            singleLine = true,
                        )
                    }

                    Text(
                        text = "For dependable scanning, the app requires a dark QR colour on a lighter high-contrast background.",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Button(
                onClick = {
                    val trimmed = content.trim()
                    when {
                        trimmed.isEmpty() -> {
                            errorMessage = "Enter a link or some text first."
                        }

                        !isScanSafe(foregroundColor, backgroundColor) -> {
                            errorMessage = "Choose a darker QR colour and a lighter, higher-contrast background."
                        }

                        else -> {
                            runCatching {
                                QrRenderer.render(
                                    content = trimmed,
                                    qrStyle = selectedStyle,
                                    foregroundColor = foregroundColor,
                                    backgroundColor = backgroundColor,
                                )
                            }.onSuccess {
                                qrBitmap = it
                                errorMessage = null
                            }.onFailure {
                                errorMessage = it.message ?: "Could not generate this QR code."
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = "Generate QR",
                    fontWeight = FontWeight.SemiBold,
                )
            }

            qrBitmap?.let { bitmap ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Your QR code",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${selectedStyle.displayName} • ${formatColorHex(foregroundColor)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(backgroundColor))
                                .padding(12.dp),
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Generated QR code",
                                modifier = Modifier.size(260.dp),
                                contentScale = ContentScale.Fit,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        runCatching {
                                            withContext(Dispatchers.IO) {
                                                ImageExporter.saveToGallery(context, bitmap)
                                            }
                                        }.onSuccess {
                                            Toast.makeText(
                                                context,
                                                "Saved to Pictures / QR Code Generator",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }.onFailure {
                                            Toast.makeText(
                                                context,
                                                "Save failed: ${it.message}",
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Save PNG")
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        runCatching {
                                            val shareIntent = withContext(Dispatchers.IO) {
                                                ImageExporter.createShareIntent(context, bitmap)
                                            }
                                            context.startActivity(shareIntent)
                                        }.onFailure {
                                            Toast.makeText(
                                                context,
                                                "Share failed: ${it.message}",
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Share")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun StyleCard(
    style: QrStyle,
    selected: Boolean,
    foregroundColor: Int,
    backgroundColor: Int,
    onClick: () -> Unit,
) {
    val preview = remember(style, foregroundColor, backgroundColor) {
        runCatching {
            QrRenderer.render(
                content = "https://qr.example/style",
                qrStyle = style,
                requestedSizePx = 240,
                foregroundColor = foregroundColor,
                backgroundColor = backgroundColor,
            )
        }.getOrNull()
    }

    val border = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    Card(
        modifier = Modifier
            .width(148.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
        border = border,
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(backgroundColor)),
                contentAlignment = Alignment.Center,
            ) {
                preview?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "${style.displayName} style preview",
                        modifier = Modifier.size(104.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }

            Text(
                text = style.displayName,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = style.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ColorSwatchRow(
    title: String,
    options: List<ColorOption>,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        ) {
            items(options) { option ->
                Column(
                    modifier = Modifier
                        .width(58.dp)
                        .clickable { onColorSelected(option.value) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(option.value))
                            .border(
                                width = if (option.value == selectedColor) 3.dp else 1.dp,
                                color = if (option.value == selectedColor) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (option.value == selectedColor) {
                            Text(
                                text = "✓",
                                color = if (relativeLuminance(option.value) > 0.55) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Text(
                        text = option.name,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private fun parseHexColor(value: String): Int? {
    val clean = value.trim().removePrefix("#")
    if (clean.length != 6 || clean.any { it !in "0123456789abcdefABCDEF" }) return null
    return runCatching { AndroidColor.parseColor("#$clean") }.getOrNull()
}

private fun formatColorHex(color: Int): String = String.format(
    Locale.ROOT,
    "#%06X",
    color and 0xFFFFFF,
)

private fun isScanSafe(foreground: Int, background: Int): Boolean {
    val foregroundLuminance = relativeLuminance(foreground)
    val backgroundLuminance = relativeLuminance(background)
    val contrast = (max(foregroundLuminance, backgroundLuminance) + 0.05) /
        (min(foregroundLuminance, backgroundLuminance) + 0.05)

    return backgroundLuminance > foregroundLuminance && contrast >= 4.0
}

private fun relativeLuminance(color: Int): Double {
    fun channel(component: Int): Double {
        val value = component / 255.0
        return if (value <= 0.04045) {
            value / 12.92
        } else {
            ((value + 0.055) / 1.055).pow(2.4)
        }
    }

    val red = channel(AndroidColor.red(color))
    val green = channel(AndroidColor.green(color))
    val blue = channel(AndroidColor.blue(color))
    return (0.2126 * red) + (0.7152 * green) + (0.0722 * blue)
}
