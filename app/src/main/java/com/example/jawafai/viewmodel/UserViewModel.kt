package com.example.jawafai.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jawafai.model.UserModel
import com.example.jawafai.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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
        object PasswordResetSent : UserOperationResult() // Added for password reset
    }

    // Added login function for Firebase authentication
    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                _userState.value = UserOperationResult.Loading

                // Use suspendCoroutine to handle the Firebase callback-based auth
                val user = suspendCoroutine<FirebaseUser?> { continuation ->
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener { authResult ->
                            continuation.resume(authResult.user)
                        }
                        .addOnFailureListener { e ->
                            continuation.resumeWithException(e)
                        }
                }

                if (user != null) {
                    _userState.value = UserOperationResult.Success("Login successful")
                } else {
                    _userState.value = UserOperationResult.Error("Login failed: Unknown error")
                }
            } catch (e: Exception) {
                _userState.value = UserOperationResult.Error("Login failed: ${e.message}")
            }
        }
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

    fun fetchUserProfile() {
        viewModelScope.launch {
            _userState.value = UserOperationResult.Loading
            try {
                val firebaseUser = auth.currentUser
                if (firebaseUser != null) {
                    val userDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(firebaseUser.uid)
                        .get()
                        .await()
                    if (userDoc.exists()) {
                        val userMap = userDoc.data ?: emptyMap<String, Any>()

                        // Check if persona is completed by fetching persona data
                        // This line had the error - reference doesn't exist, replaced with correct path
                        val personaRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(firebaseUser.uid)
                            .collection("persona")

                        // Create UserModel with personaCompleted status
                        val userModel = com.example.jawafai.model.UserModel.fromMap(userMap)

                        try {
                            // Try to fetch persona data to determine completion
                            val personaSnapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(firebaseUser.uid)
                                .collection("persona")
                                .get()
                                .await()

                            // Consider persona completed if there are at least 5 answers
                            val personaCompleted = !personaSnapshot.isEmpty && personaSnapshot.size() >= 5

                            // Update the user model with persona completion status
                            _userProfile.value = userModel.copy(personaCompleted = personaCompleted)
                        } catch (e: Exception) {
                            // If there's an error fetching persona data, just use the user model as is
                            _userProfile.value = userModel
                        }

                        _userState.value = UserOperationResult.Success()
                    } else {
                        _userProfile.value = null
                        _userState.value = UserOperationResult.Error("User profile not found.")
                    }
                } else {
                    _userProfile.value = null
                    _userState.value = UserOperationResult.Error("User not logged in.")
                }
            } catch (e: Exception) {
                _userProfile.value = null
                _userState.value = UserOperationResult.Error(e.message ?: "Failed to fetch user profile.")
            }
        }
    }

    fun updateUser(user: UserModel) {
        viewModelScope.launch {
            _userState.value = UserOperationResult.Loading
            try {
                val result = repository.updateUser(user)
                if (result) {
                    // Update the local profile directly for a smoother experience
                    _userProfile.value = user
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

    fun uploadProfileImage(imageUri: Uri, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _userState.value = UserOperationResult.Loading
            try {
                val url = repository.uploadProfileImage(imageUri)
                if (url != null) {
                    onResult(url)
                    // Update local profile with new image URL
                    _userProfile.value = _userProfile.value?.copy(imageUrl = url)
                    _userState.value = UserOperationResult.Success("Profile image uploaded")
                } else {
                    _userState.value = UserOperationResult.Error("Image upload failed")
                }
            } catch (e: Exception) {
                _userState.value = UserOperationResult.Error(e.message ?: "Image upload failed")
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _userState.value = UserOperationResult.Loading
            try {
                suspendCoroutine<Unit> { continuation ->
                    auth.sendPasswordResetEmail(email)
                        .addOnSuccessListener { continuation.resume(Unit) }
                        .addOnFailureListener { e ->
                            android.util.Log.e("UserViewModel", "Password reset failed: ${e.message}", e)
                            continuation.resumeWithException(e)
                        }
                }
                _userState.value = UserOperationResult.PasswordResetSent
            } catch (e: Exception) {
                android.util.Log.e("UserViewModel", "Password reset exception: ${e.message}", e)
                _userState.value = UserOperationResult.Error(e.message ?: "Failed to send reset email.")
            }
        }
    }
}
