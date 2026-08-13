package com.studysphere.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

//  UI State 

data class UpdateUiState(
    val isChecking: Boolean = false,
    val showDialog: Boolean = false,
    val availableRelease: GithubRelease? = null,      // null = up to date (after a check)
    val hasChecked: Boolean = false,                  // distinguishes "never checked" from "up to date"
    val downloadState: DownloadState = DownloadState.Idle,
    val checkError: String? = null,
    val readyToInstall: File? = null
)

//  ViewModel 

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val manager = UpdateManager(application)

    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    //  Triggered by "Check for updates" row tap 

    fun checkForUpdate() {
        if (_state.value.isChecking) return
        _state.value = _state.value.copy(
            isChecking    = true,
            checkError    = null,
            showDialog    = false,
            availableRelease = null,
            downloadState = DownloadState.Idle,
            readyToInstall = null
        )
        viewModelScope.launch {
            val result = manager.checkForUpdate()
            _state.value = when (result) {
                is UpdateCheckResult.UpdateAvailable -> _state.value.copy(
                    isChecking       = false,
                    hasChecked       = true,
                    availableRelease = result.release,
                    showDialog       = true
                )
                is UpdateCheckResult.UpToDate -> _state.value.copy(
                    isChecking = false,
                    hasChecked = true,
                    showDialog = true
                )
                is UpdateCheckResult.Error -> _state.value.copy(
                    isChecking  = false,
                    hasChecked  = true,
                    checkError  = result.message,
                    showDialog  = true
                )
            }
        }
    }

    //  Download (called from dialog "Update" button) 
    // Download runs in background even if the dialog is dismissed.

    fun startDownload() {
        val release = _state.value.availableRelease ?: return
        viewModelScope.launch {
            manager.downloadUpdate(release).collect { dlState ->
                _state.value = _state.value.copy(downloadState = dlState)
                if (dlState is DownloadState.Downloaded) {
                    // Re-surface dialog for "Install now"
                    _state.value = _state.value.copy(
                        readyToInstall = dlState.apkFile,
                        showDialog     = true
                    )
                }
            }
        }
    }

    fun installAndCleanup(apkFile: File) {
        // Keep the downloaded APK available until Android's installer has started.
        // Deleting it immediately can break the install flow on some devices.
        manager.installApk(apkFile)
        _state.value = _state.value.copy(
            readyToInstall = null,
            downloadState  = DownloadState.Idle,
            showDialog     = false
        )
    }

    fun reopenDialog() {
        if (_state.value.availableRelease != null || _state.value.checkError != null || _state.value.hasChecked) {
            _state.value = _state.value.copy(showDialog = true)
        }
    }

    fun dismissDialog() {
        _state.value = _state.value.copy(showDialog = false)
    }
}