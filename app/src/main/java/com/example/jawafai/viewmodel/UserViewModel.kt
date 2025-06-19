package com.example.jawafai.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jawafai.model.UserModel
import com.example.jawafai.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class UserViewModel(
    private val repository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    // States
    private val _userState = MutableLiveData<UserOperationResult>(UserOperationResult.Initial)
    val userState: LiveData<UserOperationResult> = _userState

    private val _userProfile = MutableLiveData<UserModel?>()
    val userProfile: LiveData<UserModel?> = _userProfile

    sealed class UserOperationResult {
        object Initial : UserOperationResult()
        object Loading : UserOperationResult()
        data class Success(val message: String = "") : UserOperationResult()
        data class Error(val message: String) : UserOperationResult()
    }

    fun register(email: String, password: String, user: UserModel) {
        viewModelScope.launch {
            try {
                _userState.value = UserOperationResult.Loading
                val result = repository.registerUser(user, password)
                if (result) {
                    _userState.value = UserOperationResult.Success("Registration successful")
                } else {
                    _userState.value = UserOperationResult.Error("Registration failed")
                }
            } catch (e: Exception) {
                _userState.value = UserOperationResult.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun updateUser(user: UserModel) {
        viewModelScope.launch {
            try {
                _userState.value = UserOperationResult.Loading
                val result = repository.updateUser(user)
                if (result) {
                    _userState.value = UserOperationResult.Success("Profile updated successfully")
                } else {
                    _userState.value = UserOperationResult.Error("Update failed")
                }
            } catch (e: Exception) {
                _userState.value = UserOperationResult.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            try {
                _userState.value = UserOperationResult.Loading
                val result = repository.deleteUser(userId)
                if (result) {
                    _userState.value = UserOperationResult.Success("Account deleted successfully")
                } else {
                    _userState.value = UserOperationResult.Error("Delete failed")
                }
            } catch (e: Exception) {
                _userState.value = UserOperationResult.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                repository.logout()
                _userState.value = UserOperationResult.Success("Logged out successfully")
            } catch (e: Exception) {
                _userState.value = UserOperationResult.Error(e.message ?: "Logout failed")
            }
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun resetState() {
        _userState.value = UserOperationResult.Initial
        _userProfile.value = null
    }
}
