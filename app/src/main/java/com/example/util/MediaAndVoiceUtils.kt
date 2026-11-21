package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.speech.tts.TextToSpeech
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val mime = context.contentResolver.getType(uri).orEmpty()
        if (mime.contains("pdf")) {
            renderPdfFirstPage(context, uri)
        } else {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        }
    } catch (_: Exception) {
        null
    }
}

fun renderPdfFirstPage(context: Context, uri: Uri): Bitmap? {
    return try {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        pfd.use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                if (renderer.pageCount <= 0) return null
                renderer.openPage(0).use { page ->
                    val width = (page.width * 2).coerceIn(600, 1400)
                    val height = ((page.height.toFloat() / page.width.toFloat()) * width).toInt().coerceAtLeast(600)
                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    canvas.drawColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bmp
                }
            }
        }
    } catch (_: Exception) {
        null
    }
}

fun readTextFromUri(context: Context, uri: Uri, maxChars: Int = 12000): String? {
    return try {
        val mime = context.contentResolver.getType(uri).orEmpty()
        if (mime.contains("pdf")) return null
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                val sb = StringBuilder()
                var line = reader.readLine()
                while (line != null && sb.length < maxChars) {
                    sb.appendLine(line)
                    line = reader.readLine()
                }
                sb.toString().take(maxChars)
            }
        }
    } catch (_: Exception) {
        null
    }
}

class NexoraTtsHelper(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = (status == TextToSpeech.SUCCESS)
        }
    }

    fun speak(text: String, localeTag: String) {
        if (!ready) return
        val locale = Locale.forLanguageTag(localeTag)
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.language = Locale.US
        }
        tts?.speak(text.take(3500), TextToSpeech.QUEUE_FLUSH, null, "nexora_utterance")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}

fun formatBdt(amount: Double): String {
    val rounded = amount.toLong()
    return if (amount % 1.0 == 0.0) {
        "৳%,d".format(Locale.US, rounded)
    } else {
        "৳%,.2f".format(Locale.US, amount)
    }
}
