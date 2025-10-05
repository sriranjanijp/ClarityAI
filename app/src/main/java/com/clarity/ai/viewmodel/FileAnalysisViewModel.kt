package com.clarity.ai.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.clarity.ai.data.repository.FileRepository
import com.clarity.ai.model.FileInfo
import com.clarity.ai.model.ScanState
import com.clarity.ai.model.StorageInsights
import com.clarity.ai.network.NetworkModule
import com.clarity.ai.network.DocumentRecord
import com.clarity.ai.network.ImageRecord

class FileAnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val fileRepository = FileRepository(application)
    private val apiService = NetworkModule.apiService

    private val _scanState = MutableStateFlow(ScanState.IDLE)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _analyzedFiles = MutableStateFlow<List<FileInfo>>(emptyList())
    val analyzedFiles: StateFlow<List<FileInfo>> = _analyzedFiles.asStateFlow()

    private val _storageInsights = MutableStateFlow<StorageInsights?>(null)
    val storageInsights: StateFlow<StorageInsights?> = _storageInsights.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _hasFilePermissions = MutableStateFlow(false)
    val hasFilePermissions: StateFlow<Boolean> = _hasFilePermissions.asStateFlow()

    // Backend data
    private val _backendDocuments = MutableStateFlow<List<DocumentRecord>>(emptyList())
    val backendDocuments: StateFlow<List<DocumentRecord>> = _backendDocuments.asStateFlow()

    private val _backendImages = MutableStateFlow<List<ImageRecord>>(emptyList())
    val backendImages: StateFlow<List<ImageRecord>> = _backendImages.asStateFlow()

    private val _backendConnected = MutableStateFlow(false)
    val backendConnected: StateFlow<Boolean> = _backendConnected.asStateFlow()

    private val _isLoadingBackendData = MutableStateFlow(false)
    val isLoadingBackendData: StateFlow<Boolean> = _isLoadingBackendData.asStateFlow()

    init {
        checkPermissions()
        loadBackendAnalyzedFiles()
    }

    private fun checkPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val hasPermissions = permissions.all { permission ->
            ContextCompat.checkSelfPermission(
                getApplication(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }

        _hasFilePermissions.value = hasPermissions
    }

    fun onPermissionsGranted() {
        _hasFilePermissions.value = true
    }

    fun startFileScan() {
        viewModelScope.launch {
            _scanState.value = ScanState.SCANNING
            _scanProgress.value = 0f

            try {
                // Simulate scanning progress
                for (i in 1..10) {
                    delay(200)
                    _scanProgress.value = i / 10f
                }

                val files = fileRepository.scanAllAccessibleFiles()
                _analyzedFiles.value = files

                val insights = fileRepository.generateStorageInsights(files)
                _storageInsights.value = insights

                _scanState.value = ScanState.COMPLETED

                // Also refresh backend data after scan
                loadBackendAnalyzedFiles()

            } catch (e: Exception) {
                _scanState.value = ScanState.ERROR
                e.printStackTrace()
            }
        }
    }

    fun loadBackendAnalyzedFiles() {
        viewModelScope.launch {
            _isLoadingBackendData.value = true

            try {
                // Test connection first
                val dashboardResponse = apiService.getDashboard()
                _backendConnected.value = dashboardResponse.isSuccessful

                if (dashboardResponse.isSuccessful) {
                    // Load documents
                    val documentsResponse = apiService.getAllDocuments()
                    if (documentsResponse.isSuccessful && documentsResponse.body() != null) {
                        _backendDocuments.value = documentsResponse.body()!!
                    }

                    // Load images
                    val imagesResponse = apiService.getAllImages()
                    if (imagesResponse.isSuccessful && imagesResponse.body() != null) {
                        _backendImages.value = imagesResponse.body()!!
                    }
                }
            } catch (e: Exception) {
                _backendConnected.value = false
                e.printStackTrace()
            } finally {
                _isLoadingBackendData.value = false
            }
        }
    }

    fun analyzeSpecificFile(fileInfo: FileInfo) {
        viewModelScope.launch {
            try {
                val analysis = fileRepository.performAIAnalysis(fileInfo)

                // Update the file in the list with analysis results
                val updatedFiles = _analyzedFiles.value.map { file ->
                    if (file.id == fileInfo.id) {
                        file.copy(isAnalyzed = true)
                    } else {
                        file
                    }
                }
                _analyzedFiles.value = updatedFiles

                // Refresh backend data to show newly analyzed file
                loadBackendAnalyzedFiles()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetScan() {
        _scanState.value = ScanState.IDLE
        _analyzedFiles.value = emptyList()
        _storageInsights.value = null
        _scanProgress.value = 0f
    }
}