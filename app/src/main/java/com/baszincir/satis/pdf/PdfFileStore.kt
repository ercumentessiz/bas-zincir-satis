package com.baszincir.satis.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * PDF dosyasını hem uygulamaya özel klasöre (paylaşım için) hem de mümkünse
 * telefonun genel "İndirilenler" klasörüne kaydeder.
 */
object PdfFileStore {

    fun save(context: Context, document: PdfDocument, fileName: String): File {
        val dir = File(context.getExternalFilesDir(null), "pdf").apply { mkdirs() }
        val file = File(dir, fileName)
        file.outputStream().use { document.writeTo(it) }
        document.close()

        // Ayrıca telefonun "İndirilenler" klasörüne de kopyalamayı dene.
        try {
            copyToPublicDownloads(context, file, fileName)
        } catch (_: Exception) {
            // Herhangi bir sorun olursa uygulama klasöründeki kopya yeterli; paylaşım
            // ekranından yine erişilip müşteriye gönderilebilir.
        }
        return file
    }

    private fun copyToPublicDownloads(context: Context, sourceFile: File, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
            resolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { input -> input.copyTo(out) }
            }
        } else {
            @Suppress("DEPRECATION")
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloads.mkdirs()
            val dest = File(downloads, fileName)
            sourceFile.copyTo(dest, overwrite = true)
        }
    }
}
