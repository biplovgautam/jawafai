package com.example.jawafai.repository

import android.net.Uri
import android.util.Log
import com.example.jawafai.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import javax.inject.Inject
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File

class UserRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : UserRepository {

    private val TAG = "UserRepositoryImpl"
    private val usersCollection = firestore.collection("users")

    /**
     * Checks if a username already exists in Firestore
     * @param username The username to check
     * @return Boolean true if username exists, false otherwise
     */
    private suspend fun isUsernameExists(username: String): Boolean = suspendCoroutine { continuation ->
        if (username.isEmpty()) {
            continuation.resume(false)
            return@suspendCoroutine
        }

        usersCollection
            .whereEqualTo("username", username)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                continuation.resume(!documents.isEmpty)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking username existence: ${e.message}")
                continuation.resume(false) // Assume it doesn't exist on error
            }
    }

    /**
     * Checks if an email already exists in Firestore
     * Note: Firebase Auth already enforces unique emails, but this is a double-check
     * @param email The email to check
     * @return Boolean true if email exists, false otherwise
     */
    private suspend fun isEmailExists(email: String): Boolean = suspendCoroutine { continuation ->
        if (email.isEmpty()) {
            continuation.resume(false)
            return@suspendCoroutine
        }

        usersCollection
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                continuation.resume(!documents.isEmpty)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking email existence: ${e.message}")
                continuation.resume(false) // Assume it doesn't exist on error
            }
    }

    override suspend fun registerUser(user: UserModel, password: String): Boolean =
        suspendCoroutine { continuation ->
            Log.d(TAG, "Starting user registration for email: ${user.email}")
            try {
                if (user.username.isNotEmpty()) {
                    usersCollection
                        .whereEqualTo("username", user.username)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { documents ->
                            if (!documents.isEmpty) {
                                Log.w(TAG, "Username already exists: ${user.username}")
                                continuation.resumeWithException(Exception("Username already exists"))
                                return@addOnSuccessListener
                            }
                            // Username doesn't exist, proceed with registration
                            createUserWithAuthAndFirestore(user, password, continuation)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error checking username: ${e.message}")
                            continuation.resumeWithException(e)
                        }
                } else {
                    createUserWithAuthAndFirestore(user, password, continuation)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in registration process: ${e.message}")
                continuation.resumeWithException(e)
            }
        }

    private fun createUserWithAuthAndFirestore(user: UserModel, password: String, continuation: kotlin.coroutines.Continuation<Boolean>) {
        auth.createUserWithEmailAndPassword(user.email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                if (uid == null) {
                    Log.e(TAG, "Auth succeeded but user ID is null")
                    continuation.resume(false)
                    return@addOnSuccessListener
                }
                Log.d(TAG, "Authentication successful, user created with ID: $uid")
                val userWithId = user.copy(id = uid, password = "")
                usersCollection.document(uid)
                    .set(userWithId.toMap())
                    .addOnSuccessListener {
                        Log.d(TAG, "Firestore document created successfully")
                        continuation.resume(true)
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Firestore write failed: ${e.message}")
                        if (e.message?.contains("PERMISSION_DENIED") == true ||
                            e.message?.contains("API has not been used") == true) {
                            Log.d(TAG, "Firestore permission denied but auth succeeded, returning success")
                            continuation.resume(true)
                        } else {
                            continuation.resumeWithException(e)
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Authentication failed: ${e.message}")
                continuation.resumeWithException(e)
            }
    }

    override suspend fun updateUser(user: UserModel): Boolean =
        suspendCoroutine { continuation ->
            val currentUid = auth.currentUser?.uid
            if (currentUid == null) {
                continuation.resume(false)
                return@suspendCoroutine
            }

            // Only update allowed fields
            val updates = mapOf(
                "firstName" to user.firstName,
                "lastName" to user.lastName,
                "dateOfBirth" to user.dateOfBirth,
                "imageUrl" to user.imageUrl
            )

            val docRef = usersCollection.document(currentUid)
            docRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        docRef.update(updates)
                            .addOnSuccessListener {
                                continuation.resume(true)
                            }
                            .addOnFailureListener { e ->
                                continuation.resumeWithException(e)
                            }
                    } else {
                        docRef.set(updates, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener {
                                continuation.resume(true)
                            }
                            .addOnFailureListener { e ->
                                continuation.resumeWithException(e)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }

    override suspend fun deleteUser(userId: String): Boolean =
        suspendCoroutine { continuation ->
            // First delete from Firestore
            usersCollection.document(userId)
                .delete()
                .addOnSuccessListener {
                    // Then delete the Firebase Auth user
                    auth.currentUser?.delete()
                        ?.addOnSuccessListener {
                            continuation.resume(true)
                        }
                        ?.addOnFailureListener { e ->
                            continuation.resumeWithException(e)
                        }
                        ?: continuation.resume(false)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }

    override fun logout() {
        auth.signOut()
    }

    override suspend fun uploadProfileImage(imageUri: Uri): String? {
        // Replace with your actual Cloudinary details
        val cloudName = "ddahczkbf"
        val uploadPreset = "jawafai" // For unsigned uploads
        val apiUrl = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"

        // Convert Uri to File (assumes you have a way to get a File from Uri)
        val file = File(imageUri.path ?: return null)
        val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, requestBody)
            .addFormDataPart("upload_preset", uploadPreset)
            .build()

        val request = Request.Builder()
            .url(apiUrl)
            .post(multipartBody)
            .build()

        val client = OkHttpClient()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null
        val responseBody = response.body?.string() ?: return null
        val json = JSONObject(responseBody)
        return json.optString("secure_url", null)
    }
}
