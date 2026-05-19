package com.filesverse.app.data.model

import java.io.Serializable

enum class FileType {
    FOLDER, IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE, CODE, APK, PDF,
    SPREADSHEET, PRESENTATION, UNKNOWN;

    companion object {
        fun fromExtension(ext: String): FileType = when (ext.lowercase()) {
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "heic", "raw" -> IMAGE
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp" -> VIDEO
            "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a" -> AUDIO
            "pdf" -> PDF
            "doc", "docx", "odt", "rtf", "txt", "md" -> DOCUMENT
            "xls", "xlsx", "csv", "ods" -> SPREADSHEET
            "ppt", "pptx", "odp" -> PRESENTATION
            "zip", "rar", "7z", "tar", "gz", "bz2" -> ARCHIVE
            "kt", "java", "py", "js", "ts", "html", "css", "json", "xml", "c", "cpp", "h", "sh" -> CODE
            "apk", "aab", "xapk" -> APK
            else -> UNKNOWN
        }
    }
}

data class FileItem(
    val id: Long = 0,
    val name: String,
    val path: String,
    val size: Long = 0,
    val lastModified: Long = System.currentTimeMillis(),
    val isDirectory: Boolean = false,
    val extension: String = "",
    val mimeType: String = "",
    val fileType: FileType = FileType.UNKNOWN,
    val isProtected: Boolean = false,
    val isInContainer: Boolean = false,
    val containerName: String = "",
    val cloudSynced: Boolean = false,
    val cloudProvider: String = "",
    val thumbnailPath: String? = null
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

data class StorageInfo(
    val totalSpace: Long,
    val usedSpace: Long,
    val freeSpace: Long
) {
    val usedPercentage: Float
        get() = if (totalSpace > 0) (usedSpace.toFloat() / totalSpace.toFloat()) * 100f else 0f

    val freePercentage: Float
        get() = if (totalSpace > 0) (freeSpace.toFloat() / totalSpace.toFloat()) * 100f else 0f
}

data class UsageCategory(
    val type: FileType,
    val label: String,
    val totalSize: Long,
    val fileCount: Int,
    val color: Long
)
