package com.clarity.ai.viewmodel

import android.app.Application
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

class FileAnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val fileRepository = FileRepository(application)

    private val _scanState = MutableStateFlow(ScanState.IDLE)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _analyzedFiles = MutableStateFlow<List<FileInfo>>(emptyList())
    val analyzedFiles: StateFlow<List<FileInfo>> = _analyzedFiles.asStateFlow()

    private val _storageInsights = MutableStateFlow<StorageInsights?>(null)
    val storageInsights: StateFlow<StorageInsights?> = _storageInsights.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    fun startFileScan() {
        viewModelScope.launch {
            _scanState.value = ScanState.SCANNING
            _scanProgress.value = 0f

            try {
                // Simulate scanning progress
                for (i in 1..10) {
                    delay(200) // Simulate processing time
                    _scanProgress.value = i / 10f
                }

                val files = fileRepository.scanAllAccessibleFiles()
                _analyzedFiles.value = files

                val insights = fileRepository.generateStorageInsights(files)
                _storageInsights.value = insights

                _scanState.value = ScanState.COMPLETED

            } catch (e: Exception) {
                _scanState.value = ScanState.ERROR
                e.printStackTrace()
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