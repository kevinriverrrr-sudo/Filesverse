package com.filesverse.app.data.repository

import android.content.Context
import com.filesverse.app.data.model.CloudAccount
import com.filesverse.app.data.model.CloudProvider
import com.filesverse.app.data.model.UploadTask
import com.filesverse.app.data.model.UploadStatus
import com.filesverse.app.data.model.VikingFileUploadResponse
import com.filesverse.app.service.VikingFileService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vikingFileService: VikingFileService
) {
    private val _accounts = MutableStateFlow<List<CloudAccount>>(emptyList())
    val accounts: Flow<List<CloudAccount>> = _accounts

    private val _uploadTasks = MutableStateFlow<List<UploadTask>>(emptyList())
    val uploadTasks: Flow<List<UploadTask>> = _uploadTasks

    init {
        loadAccounts()
    }

    fun getAccounts(): List<CloudAccount> = _accounts.value

    fun getAccount(provider: CloudProvider): CloudAccount? {
        return _accounts.value.find { it.provider == provider }
    }

    fun addAccount(account: CloudAccount) {
        _accounts.update { current ->
            current + account
        }
    }

    fun removeAccount(accountId: Long) {
        _accounts.update { current ->
            current.filter { it.id != accountId }
        }
    }

    fun updateAccount(account: CloudAccount) {
        _accounts.update { current ->
            current.map { if (it.id == account.id) account else it }
        }
    }

    fun uploadToVikingFile(
        filePath: String,
        fileName: String,
        onProgress: (Float) -> Unit
    ): Flow<VikingFileUploadResponse> = flow {
        emit(VikingFileUploadResponse(status = false, message = "Uploading..."))
        val response = vikingFileService.uploadFile(filePath, fileName) { progress ->
            onProgress(progress)
        }
        emit(response)
    }.flowOn(Dispatchers.IO)

    fun addUploadTask(task: UploadTask) {
        _uploadTasks.update { current ->
            current + task
        }
    }

    fun updateUploadTask(task: UploadTask) {
        _uploadTasks.update { current ->
            current.map { if (it.id == task.id) task else it }
        }
    }

    fun removeCompletedTasks() {
        _uploadTasks.update { current ->
            current.filter { it.status != UploadStatus.COMPLETED && it.status != UploadStatus.FAILED }
        }
    }

    private fun loadAccounts() {
        // Load saved accounts from DataStore (simplified for initial implementation)
        _accounts.value = listOf(
            CloudAccount(
                id = 1,
                provider = CloudProvider.VIKINGFILE,
                accountName = "VikingFile",
                isAuthenticated = false,
                autoSyncEnabled = false
            ),
            CloudAccount(
                id = 2,
                provider = CloudProvider.GOOGLE_DRIVE,
                accountName = "Google Drive",
                isAuthenticated = false,
                autoSyncEnabled = false
            )
        )
    }

    companion object {
        const val VIKINGFILE_API_BASE = "https://vikingfile.com/api"
    }
}
