package com.filesverse.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filesverse.app.data.model.FileItem
import com.filesverse.app.data.model.StorageInfo
import com.filesverse.app.data.model.UsageCategory
import com.filesverse.app.data.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FileBrowserState(
    val currentPath: String = "",
    val files: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val selectedFiles: Set<FileItem> = emptySet(),
    val showContextMenu: FileItem? = null,
    val isMultiSelectMode: Boolean = false
)

@HiltViewModel
class FileViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _browserState = MutableStateFlow(FileBrowserState())
    val browserState: StateFlow<FileBrowserState> = _browserState.asStateFlow()

    val storageInfo: StateFlow<StorageInfo?> = fileRepository.getStorageInfo()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val usageCategories: StateFlow<List<UsageCategory>?> = fileRepository.getUsageCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        loadRootDirectories()
    }

    fun loadRootDirectories() {
        viewModelScope.launch {
            _browserState.value = _browserState.value.copy(isLoading = true)
            fileRepository.getRootDirectories().collect { dirs ->
                _browserState.value = _browserState.value.copy(
                    files = dirs,
                    isLoading = false,
                    currentPath = ""
                )
            }
        }
    }

    fun navigateToDirectory(path: String) {
        viewModelScope.launch {
            _browserState.value = _browserState.value.copy(
                isLoading = true,
                currentPath = path,
                selectedFiles = emptySet(),
                isMultiSelectMode = false
            )
            fileRepository.getFilesInDirectory(path).collect { files ->
                _browserState.value = _browserState.value.copy(
                    files = files,
                    isLoading = false
                )
            }
        }
    }

    fun navigateUp() {
        val currentPath = _browserState.value.currentPath
        if (currentPath.isEmpty()) return

        val parentFile = java.io.File(currentPath).parentFile
        if (parentFile != null) {
            navigateToDirectory(parentFile.absolutePath)
        } else {
            loadRootDirectories()
        }
    }

    fun searchFiles(query: String) {
        if (query.isBlank()) {
            _browserState.value = _browserState.value.copy(
                isSearching = false,
                searchQuery = ""
            )
            return
        }

        viewModelScope.launch {
            _browserState.value = _browserState.value.copy(
                isSearching = true,
                searchQuery = query,
                isLoading = true
            )
            fileRepository.searchFiles(query).collect { results ->
                _browserState.value = _browserState.value.copy(
                    files = results,
                    isLoading = false
                )
            }
        }
    }

    fun createFolder(name: String) {
        val currentPath = _browserState.value.currentPath
        if (currentPath.isEmpty()) return

        viewModelScope.launch {
            val success = fileRepository.createDirectory(currentPath, name)
            if (success) {
                navigateToDirectory(currentPath)
            }
        }
    }

    fun deleteFile(file: FileItem) {
        viewModelScope.launch {
            val success = fileRepository.deleteFile(file.path)
            if (success) {
                _browserState.value = _browserState.value.copy(
                    files = _browserState.value.files.filter { it.path != file.path }
                )
            }
        }
    }

    fun renameFile(file: FileItem, newName: String) {
        viewModelScope.launch {
            val success = fileRepository.renameFile(file.path, newName)
            if (success) {
                navigateToDirectory(_browserState.value.currentPath)
            }
        }
    }

    fun toggleFileSelection(file: FileItem) {
        val currentSelection = _browserState.value.selectedFiles.toMutableSet()
        if (currentSelection.contains(file)) {
            currentSelection.remove(file)
        } else {
            currentSelection.add(file)
        }

        val isMultiSelect = currentSelection.isNotEmpty()
        _browserState.value = _browserState.value.copy(
            selectedFiles = currentSelection,
            isMultiSelectMode = isMultiSelect
        )
    }

    fun exitMultiSelectMode() {
        _browserState.value = _browserState.value.copy(
            selectedFiles = emptySet(),
            isMultiSelectMode = false
        )
    }

    fun showContextMenu(file: FileItem) {
        _browserState.value = _browserState.value.copy(showContextMenu = file)
    }

    fun dismissContextMenu() {
        _browserState.value = _browserState.value.copy(showContextMenu = null)
    }

    fun refreshUsageCategories() {
        viewModelScope.launch {
            fileRepository.getUsageCategories().collect { categories ->
                // Categories are observed via StateFlow
            }
        }
    }
}
