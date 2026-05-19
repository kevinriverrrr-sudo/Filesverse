package com.filesverse.app.service

import android.util.Log
import com.filesverse.app.data.model.VikingFileUploadResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VikingFileService @Inject constructor(
    @ApplicationContext private val context: android.content.Context
) {
    companion object {
        private const val TAG = "VikingFileService"
        private const val BASE_URL = "https://vikingfile.com/api/1/upload"
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    suspend fun uploadFile(
        filePath: String,
        fileName: String,
        onProgress: (Float) -> Unit
    ): VikingFileUploadResponse {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return VikingFileUploadResponse(
                    status = false,
                    message = "File not found: $filePath"
                )
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    fileName,
                    file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                )
                .build()

            val progressRequestBody = ProgressRequestBody(requestBody) { progress ->
                onProgress(progress)
            }

            val request = okhttp3.Request.Builder()
                .url(BASE_URL)
                .post(progressRequestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                parseUploadResponse(responseBody)
            } else {
                VikingFileUploadResponse(
                    status = false,
                    message = "Upload failed: ${response.code} - ${response.message}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload error", e)
            VikingFileUploadResponse(
                status = false,
                message = "Upload error: ${e.message}"
            )
        }
    }

    private fun parseUploadResponse(responseBody: String): VikingFileUploadResponse {
        return try {
            val json = JSONObject(responseBody)

            when {
                json.optBoolean("status", false) || json.optString("status") == "200" -> {
                    VikingFileUploadResponse(
                        status = true,
                        fileCode = json.optString("file_code", json.optString("code", "")),
                        url = json.optString("url", ""),
                        deleteUrl = json.optString("delete_url", ""),
                        message = "Upload successful"
                    )
                }
                else -> {
                    VikingFileUploadResponse(
                        status = false,
                        message = json.optString("msg", json.optString("error", "Unknown error"))
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse error", e)
            // Try to extract URL from HTML response (some APIs return HTML)
            val urlRegex = Regex("""https?://vikingfile\.com/\w+""")
            val url = urlRegex.find(responseBody)?.value ?: ""
            if (url.isNotEmpty()) {
                VikingFileUploadResponse(
                    status = true,
                    url = url,
                    message = "Upload successful"
                )
            } else {
                VikingFileUploadResponse(
                    status = false,
                    message = "Failed to parse upload response"
                )
            }
        }
    }

    /**
     * Progress tracking wrapper for request body
     */
    class ProgressRequestBody(
        private val requestBody: MultipartBody,
        private val progressListener: (Float) -> Unit
    ) : okhttp3.RequestBody() {

        override fun contentType() = requestBody.contentType()

        override fun contentLength() = requestBody.contentLength()

        override fun writeTo(sink: okio.BufferedSink) {
            val bufferedSink = ProgressSink(sink).buffer()
            requestBody.writeTo(bufferedSink)
            bufferedSink.flush()
        }

        inner class ProgressSink(
            private val delegate: okio.Sink
        ) : okio.Sink by delegate {

            private var bytesWritten = 0L
            private var totalBytes = 0L

            override fun write(source: okio.Buffer, byteCount: Long) {
                delegate.write(source, byteCount)
                if (totalBytes == 0L) {
                    totalBytes = contentLength()
                }
                bytesWritten += byteCount
                if (totalBytes > 0) {
                    progressListener(bytesWritten.toFloat() / totalBytes)
                }
            }
        }
    }

    suspend fun getFileInfo(fileCode: String): VikingFileUploadResponse {
        return try {
            val url = "https://vikingfile.com/api/1/file/info?file_code=$fileCode"
            val request = okhttp3.Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                val json = JSONObject(responseBody)
                VikingFileUploadResponse(
                    status = json.optBoolean("status", false),
                    url = json.optString("url", ""),
                    message = json.optString("msg", "")
                )
            } else {
                VikingFileUploadResponse(status = false, message = "Failed to get file info")
            }
        } catch (e: Exception) {
            VikingFileUploadResponse(status = false, message = e.message ?: "Unknown error")
        }
    }
}
