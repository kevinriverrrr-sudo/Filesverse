package com.filesverse.app.service

import android.content.Context
import com.filesverse.app.data.model.CloudAccount
import com.filesverse.app.data.model.CloudProvider
import com.filesverse.app.data.model.UploadTask
import com.filesverse.app.data.model.UploadStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var account: CloudAccount? = null

    fun isAuthenticated(): Boolean = account?.isAuthenticated == true

    fun setAccount(acc: CloudAccount) {
        account = acc
    }

    fun uploadFile(
        filePath: String,
        fileName: String,
        mimeType: String,
        onProgress: (Float) -> Unit
    ): Flow<UploadTask> = flow {
        if (!isAuthenticated()) {
            emit(UploadTask(
                fileName = fileName,
                filePath = filePath,
                fileSize = File(filePath).length(),
                provider = CloudProvider.GOOGLE_DRIVE,
                status = UploadStatus.FAILED,
                error = "Not authenticated with Google Drive"
            ))
            return@flow
        }

        // Simulate upload progress for structure
        // In production, this would use Google Drive REST API v3
        val fileSize = File(filePath).length()
        var uploaded = 0L
        val chunkSize = fileSize / 10

        while (uploaded < fileSize) {
            uploaded = minOf(uploaded + chunkSize, fileSize)
            onProgress(uploaded.toFloat() / fileSize)
            emit(UploadTask(
                id = System.currentTimeMillis().toString(),
                fileName = fileName,
                filePath = filePath,
                fileSize = fileSize,
                provider = CloudProvider.GOOGLE_DRIVE,
                progress = uploaded.toFloat() / fileSize,
                status = UploadStatus.UPLOADING
            ))
            kotlinx.coroutines.delay(100)
        }

        emit(UploadTask(
            id = System.currentTimeMillis().toString(),
            fileName = fileName,
            filePath = filePath,
            fileSize = fileSize,
            provider = CloudProvider.GOOGLE_DRIVE,
            progress = 1f,
            status = UploadStatus.COMPLETED,
            remoteUrl = "https://drive.google.com/file/d/mock-id"
        ))
    }.flowOn(Dispatchers.IO)
}
