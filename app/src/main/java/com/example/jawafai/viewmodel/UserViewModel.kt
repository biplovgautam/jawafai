package com.example.jawafai.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
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
                    val userDocRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(firebaseUser.uid)
                    val userDoc = userDocRef.get().await()

                    if (userDoc.exists()) {
                        // Profile exists, load it
                        val userMap = userDoc.data ?: emptyMap<String, Any>()
                        _userProfile.value = com.example.jawafai.model.UserModel.fromMap(userMap)
                        _userState.value = UserOperationResult.Success()
                    } else {
                        // Profile doesn't exist, create a default one
                        Log.w("UserViewModel", "User document not found for UID: ${firebaseUser.uid}. Creating a default one.")
                        val defaultUsername = firebaseUser.email?.substringBefore('@') ?: "user${firebaseUser.uid.take(6)}"
                        val displayName = firebaseUser.displayName
                        val firstName = displayName?.split(" ")?.getOrNull(0) ?: ""
                        val lastName = displayName?.split(" ")?.getOrNull(1) ?: ""

                        val defaultUser = UserModel(
                            id = firebaseUser.uid,
                            email = firebaseUser.email ?: "",
                            username = defaultUsername,
                            firstName = firstName,
                            lastName = lastName,
                            imageUrl = firebaseUser.photoUrl?.toString()
                        )

                        // Save the new default profile to Firestore and update local state
                        userDocRef.set(defaultUser.toMap()).await()
                        _userProfile.value = defaultUser
                        _userState.value = UserOperationResult.Success("Default profile created.")
                    }
                } else {
                    _userProfile.value = null
                    _userState.value = UserOperationResult.Error("User not logged in.")
                }
            } catch (e: Exception) {
                _userProfile.value = null
                _userState.value = UserOperationResult.Error(e.message ?: "Failed to fetch user profile.")
                Log.e("UserViewModel", "Error fetching user profile", e)
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

    fun uploadProfileImage(context: Context, imageUri: Uri, onResult: (String?) -> Unit) {
        Log.d("UserViewModel", "Starting uploadProfileImage in ViewModel: $imageUri")
        viewModelScope.launch {
            _userState.value = UserOperationResult.Loading
            try {
                Log.d("UserViewModel", "About to call repository.uploadProfileImage")
                val url = repository.uploadProfileImage(context, imageUri)
                Log.d("UserViewModel", "Repository returned URL: $url")
                if (url != null) {
                    Log.d("UserViewModel", "Upload successful, updating profile with URL: $url")
                    onResult(url)
                    // Update local profile with new image URL
                    _userProfile.value = _userProfile.value?.copy(imageUrl = url)
                    _userState.value = UserOperationResult.Success("Profile image uploaded")
                } else {
                    Log.e("UserViewModel", "Repository returned null URL")
                    _userState.value = UserOperationResult.Error("Image upload failed")
                    onResult(null)
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Exception during image upload", e)
                _userState.value = UserOperationResult.Error(e.message ?: "Image upload failed")
                onResult(null)
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
