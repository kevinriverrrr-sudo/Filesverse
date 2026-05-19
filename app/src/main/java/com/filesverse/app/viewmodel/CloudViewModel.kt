package com.filesverse.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filesverse.app.data.model.CloudAccount
import com.filesverse.app.data.model.CloudProvider
import com.filesverse.app.data.model.UploadTask
import com.filesverse.app.data.model.UploadStatus
import com.filesverse.app.data.model.VikingFileUploadResponse
import com.filesverse.app.data.repository.CloudRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CloudState(
    val accounts: List<CloudAccount> = emptyList(),
    val uploadTasks: List<UploadTask> = emptyList(),
    val activeUploads: Int = 0,
    val showUploadDialog: Boolean = false,
    val fileToUpload: String = "",
    val uploadProgress: Float = 0f,
    val lastUploadResult: VikingFileUploadResponse? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class CloudViewModel @Inject constructor(
    private val cloudRepository: CloudRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CloudState())
    val state: StateFlow<CloudState> = _state.asStateFlow()

    val uploadTasks: StateFlow<List<UploadTask>> = cloudRepository.uploadTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        _state.value = _state.value.copy(
            accounts = cloudRepository.getAccounts()
        )
    }

    fun uploadToVikingFile(filePath: String, fileName: String) {
        viewModelScope.launch {
            val taskId = System.currentTimeMillis().toString()
            _state.value = _state.value.copy(isLoading = true, fileToUpload = filePath)

            cloudRepository.uploadToVikingFile(filePath, fileName) { progress ->
                _state.value = _state.value.copy(uploadProgress = progress)
            }.collect { response ->
                _state.value = _state.value.copy(
                    lastUploadResult = response,
                    isLoading = false,
                    uploadProgress = if (response.status) 1f else 0f
                )
            }
        }
    }

    fun uploadToGoogleDrive(filePath: String, fileName: String) {
        viewModelScope.launch {
            // Google Drive upload would be handled via GoogleDriveService
            _state.value = _state.value.copy(isLoading = true)
            // Placeholder for Google Drive integration
        }
    }

    fun addAccount(account: CloudAccount) {
        cloudRepository.addAccount(account)
        loadAccounts()
    }

    fun removeAccount(accountId: Long) {
        cloudRepository.removeAccount(accountId)
        loadAccounts()
    }

    fun showUploadDialog(filePath: String) {
        _state.value = _state.value.copy(
            showUploadDialog = true,
            fileToUpload = filePath
        )
    }

    fun hideUploadDialog() {
        _state.value = _state.value.copy(showUploadDialog = false)
    }

    fun clearUploadResult() {
        _state.value = _state.value.copy(lastUploadResult = null)
    }
}
