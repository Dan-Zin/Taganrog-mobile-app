package com.example.taganrog_map.data

import android.util.Log
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "InitiativeRepository"

class InitiativeRepository {
    private val apiService = ApiClient.service
    
    private val _initiatives = MutableStateFlow<List<Initiative>>(emptyList())
    val initiatives: StateFlow<List<Initiative>> = _initiatives.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    suspend fun loadInitiatives(status: String? = null, category: String? = null) {
        try {
            _isLoading.value = true
            _error.value = null
            
            val response = apiService.getInitiatives(status, category)
            _initiatives.value = response.map { it.toInitiative() }
            
            Log.d(TAG, "Loaded ${_initiatives.value.size} initiatives")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading initiatives", e)
            _error.value = e.message ?: "Unknown error"
            // Fallback to mock data on error
            _initiatives.value = MockInitiatives.items
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun getInitiative(id: String): Initiative? {
        return try {
            _isLoading.value = true
            _error.value = null
            
            val response = apiService.getInitiative(id)
            response.toInitiative()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading initiative $id", e)
            _error.value = e.message ?: "Unknown error"
            // Fallback to mock data
            MockInitiatives.items.find { it.id == id }
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun createInitiative(
        initiative: InitiativeCreateRequest,
        files: List<MultipartBody.Part>
    ): Initiative? {
        return try {
            _isLoading.value = true
            _error.value = null

            val json = Gson().toJson(initiative)
            val body = json.toRequestBody("application/json".toMediaType())
            Log.d(TAG, "Sending initiative create: files=${files.size}")
            val response = apiService.createInitiative(body, files)
            val newInitiative = response.toInitiative()
            
            // Обновляем список
            _initiatives.value = _initiatives.value + newInitiative
            
            newInitiative
        } catch (e: Exception) {
            Log.e(TAG, "Error creating initiative", e)
            _error.value = e.message ?: "Unknown error"
            null
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun updateInitiative(id: String, initiative: InitiativeCreateRequest): Initiative? {
        return try {
            _isLoading.value = true
            _error.value = null
            
            val response = apiService.updateInitiative(id, initiative)
            val updatedInitiative = response.toInitiative()
            
            // Обновляем список
            _initiatives.value = _initiatives.value.map {
                if (it.id == id) updatedInitiative else it
            }
            
            updatedInitiative
        } catch (e: Exception) {
            Log.e(TAG, "Error updating initiative", e)
            _error.value = e.message ?: "Unknown error"
            null
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun deleteInitiative(id: String): Boolean {
        return try {
            _isLoading.value = true
            _error.value = null
            
            apiService.deleteInitiative(id)
            
            // Удаляем из списка
            _initiatives.value = _initiatives.value.filter { it.id != id }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting initiative", e)
            _error.value = e.message ?: "Unknown error"
            false
        } finally {
            _isLoading.value = false
        }
    }
}
