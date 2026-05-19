package com.filesverse.app.data.repository

import android.content.Context
import android.os.Environment
import com.filesverse.app.data.model.FileItem
import com.filesverse.app.data.model.FileType
import com.filesverse.app.data.model.StorageInfo
import com.filesverse.app.data.model.UsageCategory
import com.filesverse.app.ui.theme.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getStorageInfo(): Flow<StorageInfo> = flow {
        val stat = Environment.getExternalStorageDirectory()
        val total = stat.totalSpace
        val free = stat.freeSpace
        emit(StorageInfo(totalSpace = total, usedSpace = total - free, freeSpace = free))
    }.flowOn(Dispatchers.IO)

    fun getFilesInDirectory(directoryPath: String): Flow<List<FileItem>> = flow {
        val dir = File(directoryPath)
        if (!dir.exists() || !dir.isDirectory) {
            emit(emptyList())
            return@flow
        }

        val files = dir.listFiles()?.mapNotNull { file ->
            file.toFileItem()
        }?.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() })
            ?: emptyList()

        emit(files)
    }.flowOn(Dispatchers.IO)

    fun getRootDirectories(): Flow<List<FileItem>> = flow {
        val roots = mutableListOf<FileItem>()

        // Internal storage
        roots.add(
            FileItem(
                name = "Internal Storage",
                path = Environment.getExternalStorageDirectory().absolutePath,
                isDirectory = true,
                fileType = FileType.FOLDER
            )
        )

        // External SD cards
        val externalDirs = context.getExternalFilesDirs(null)
        externalDirs.filterNotNull().forEach { dir ->
            val path = dir.absolutePath
            if (!path.contains("emulated")) {
                val storageDir = File(path).parentFile?.parentFile?.parentFile
                storageDir?.let {
                    roots.add(
                        FileItem(
                            name = it.name,
                            path = it.absolutePath,
                            isDirectory = true,
                            fileType = FileType.FOLDER
                        )
                    )
                }
            }
        }

        // Downloads
        roots.add(
            FileItem(
                name = "Downloads",
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
                isDirectory = true,
                fileType = FileType.FOLDER
            )
        )

        // Pictures
        roots.add(
            FileItem(
                name = "Pictures",
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath,
                isDirectory = true,
                fileType = FileType.FOLDER
            )
        )

        // Music
        roots.add(
            FileItem(
                name = "Music",
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath,
                isDirectory = true,
                fileType = FileType.FOLDER
            )
        )

        // Videos
        roots.add(
            FileItem(
                name = "Videos",
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath,
                isDirectory = true,
                fileType = FileType.FOLDER
            )
        )

        // Documents
        roots.add(
            FileItem(
                name = "Documents",
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath,
                isDirectory = true,
                fileType = FileType.FOLDER
            )
        )

        emit(roots)
    }.flowOn(Dispatchers.IO)

    fun getUsageCategories(): Flow<List<UsageCategory>> = flow {
        val root = Environment.getExternalStorageDirectory()
        val categories = mutableMapOf<FileType, Pair<Long, Int>>()

        scanDirectory(root, categories)

        val result = categories.map { (type, data) ->
            UsageCategory(
                type = type,
                label = type.name.replace("_", " "),
                totalSize = data.first,
                fileCount = data.second,
                color = getColorForType(type)
            )
        }.sortedByDescending { it.totalSize }

        emit(result)
    }.flowOn(Dispatchers.IO)

    fun searchFiles(query: String): Flow<List<FileItem>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }

        val results = mutableListOf<FileItem>()
        val root = Environment.getExternalStorageDirectory()
        searchInDirectory(root, query.lowercase(), results, maxDepth = 5)

        emit(results)
    }.flowOn(Dispatchers.IO)

    private fun searchInDirectory(
        dir: File,
        query: String,
        results: MutableList<FileItem>,
        currentDepth: Int = 0,
        maxDepth: Int = 5
    ) {
        if (currentDepth >= maxDepth || results.size >= 100) return

        dir.listFiles()?.forEach { file ->
            if (file.name.lowercase().contains(query)) {
                results.add(file.toFileItem())
            }
            if (file.isDirectory && file.name !startsWith "." && currentDepth < maxDepth) {
                searchInDirectory(file, query, results, currentDepth + 1, maxDepth)
            }
        }
    }

    private fun scanDirectory(dir: File, categories: MutableMap<FileType, Pair<Long, Int>>) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                if (!file.name.startsWith(".")) {
                    scanDirectory(file, categories)
                }
            } else {
                val ext = file.extension
                val type = FileType.fromExtension(ext)
                val current = categories[type] ?: Pair(0L, 0)
                categories[type] = Pair(current.first + file.length(), current.second + 1)
            }
        }
    }

    private fun File.toFileItem(): FileItem? {
        return try {
            FileItem(
                name = this.name,
                path = this.absolutePath,
                size = if (this.isFile) this.length() else 0,
                lastModified = this.lastModified(),
                isDirectory = this.isDirectory,
                extension = if (this.isFile) this.extension else "",
                fileType = if (this.isDirectory) FileType.FOLDER else FileType.fromExtension(this.extension)
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun getColorForType(type: FileType): Long = when (type) {
        FileType.IMAGE -> ColorImage.value.toLong()
        FileType.VIDEO -> ColorVideo.value.toLong()
        FileType.AUDIO -> ColorAudio.value.toLong()
        FileType.DOCUMENT -> ColorDocument.value.toLong()
        FileType.PDF -> ColorPdf.value.toLong()
        FileType.ARCHIVE -> ColorArchive.value.toLong()
        FileType.CODE -> ColorCode.value.toLong()
        FileType.APK -> ColorApk.value.toLong()
        else -> TextSecondary.value.toLong()
    }

    fun createDirectory(parentPath: String, name: String): Boolean {
        return File(parentPath, name).mkdirs()
    }

    fun deleteFile(path: String): Boolean {
        val file = File(path)
        return if (file.isDirectory) file.deleteRecursively() else file.delete()
    }

    fun renameFile(oldPath: String, newName: String): Boolean {
        val oldFile = File(oldPath)
        val newFile = File(oldFile.parent, newName)
        return oldFile.renameTo(newFile)
    }

    fun getFileInfo(path: String): FileItem? {
        val file = File(path)
        return file.toFileItem()
    }
}
