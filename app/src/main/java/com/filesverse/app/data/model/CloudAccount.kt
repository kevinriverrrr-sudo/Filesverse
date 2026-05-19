package com.filesverse.app.data.model

enum class CloudProvider(val displayName: String, val packageId: String) {
    GOOGLE_DRIVE("Google Drive", "com.google.android.apps.docs"),
    VIKINGFILE("VikingFile", "com.vikingfile"),
    DROPBOX("Dropbox", "com.dropbox.android"),
    ONE_DRIVE("OneDrive", "com.microsoft.skydrive"),
    MEGA("MEGA", "mega.privacy.android.app"),
    YANDEX_DISK("Yandex Disk", "ru.yandex.disk")
}

data class CloudAccount(
    val id: Long = 0,
    val provider: CloudProvider,
    val accountName: String,
    val accountEmail: String = "",
    val isAuthenticated: Boolean = false,
    val authToken: String = "",
    val refreshToken: String = "",
    val totalSpace: Long = 0,
    val usedSpace: Long = 0,
    val lastSyncTime: Long = 0,
    val autoSyncEnabled: Boolean = false
)

data class UploadTask(
    val id: String = "",
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val provider: CloudProvider,
    val progress: Float = 0f,
    val status: UploadStatus = UploadStatus.PENDING,
    val remoteUrl: String = "",
    val error: String = ""
)

enum class UploadStatus {
    PENDING, UPLOADING, PAUSED, COMPLETED, FAILED, CANCELLED
}

data class VikingFileUploadResponse(
    val status: Boolean = false,
    val fileCode: String = "",
    val url: String = "",
    val deleteUrl: String = "",
    val message: String = ""
)
