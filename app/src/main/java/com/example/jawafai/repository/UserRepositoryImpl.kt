package com.example.jawafai.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.jawafai.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import javax.inject.Inject
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
            val currentFirebaseUser = auth.currentUser
            if (currentFirebaseUser == null) {
                continuation.resume(false)
                return@suspendCoroutine
            }
            val currentUid = currentFirebaseUser.uid

            val updates = mapOf(
                "firstName" to user.firstName,
                "lastName" to user.lastName,
                "username" to user.username,
                "dateOfBirth" to user.dateOfBirth,
                "bio" to user.bio,
                "imageUrl" to user.imageUrl
            )

            val docRef = usersCollection.document(currentUid)

            val firestoreUpdateAction = {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName("${user.firstName} ${user.lastName}".trim().ifEmpty { user.username })
                    .setPhotoUri(user.imageUrl?.let { Uri.parse(it) })
                    .build()

                currentFirebaseUser.updateProfile(profileUpdates)
                    .addOnSuccessListener {
                        continuation.resume(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to update Firebase Auth profile", e)
                        continuation.resume(true) // Firestore succeeded, so we count it as success
                    }
            }

            docRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    val task = if (documentSnapshot.exists()) {
                        docRef.update(updates)
                    } else {
                        docRef.set(updates, SetOptions.merge())
                    }
                    task.addOnSuccessListener { firestoreUpdateAction() }
                        .addOnFailureListener { e -> continuation.resumeWithException(e) }
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

    // Upload a profile image using content resolver to handle various Uri schemes
    override suspend fun uploadProfileImage(context: Context, imageUri: Uri): String? {
        Log.d(TAG, "-------------------- CLOUDINARY UPLOAD START --------------------")
        Log.d(TAG, "Starting image upload for URI: $imageUri")
        Log.d(TAG, "Cloud name: ddahczkbf, Upload preset: jawafai")

        // Print network status
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        val isConnected = activeNetwork?.isConnectedOrConnecting == true
        Log.d(TAG, "Network status - Connected: $isConnected, Type: ${activeNetwork?.typeName}")

        val cloudName = "ddahczkbf"
        val uploadPreset = "jawafai"
        val apiUrl = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"
        var tempFile: File? = null

        try {
            // Read image data from URI into temporary file
            val inputStream = context.contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for URI: $imageUri")
                return null
            }

            tempFile = File.createTempFile("upload", ".jpg", context.cacheDir)
            Log.d(TAG, "Created temp file: ${tempFile.absolutePath}")

            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    val bytesTransferred = input.copyTo(output)
                    Log.d(TAG, "Bytes copied from URI to temp file: $bytesTransferred")
                }
            }

            // Verify file exists and has content
            if (!tempFile.exists() || tempFile.length() == 0L) {
                Log.e(TAG, "Temp file is empty or doesn't exist")
                return null
            }

            Log.d(TAG, "File ready for upload, size: ${tempFile.length()} bytes")

            // Prepare multipart request
            val requestBody = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", tempFile.name, requestBody)
                .addFormDataPart("upload_preset", uploadPreset)
                .build()

            val request = Request.Builder()
                .url(apiUrl)
                .post(multipartBody)
                .build()

            Log.d(TAG, "Request details:")
            Log.d(TAG, "- URL: ${request.url}")
            Log.d(TAG, "- Method: ${request.method}")
            Log.d(TAG, "- Headers: ${request.headers}")

            val client = OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            Log.d(TAG, "Sending upload request to Cloudinary...")

            client.newCall(request).execute().use { response ->
                val responseTime = System.currentTimeMillis()
                Log.d(TAG, "Received response in ${responseTime - System.currentTimeMillis()}ms")
                Log.d(TAG, "Response code: ${response.code}")
                Log.d(TAG, "Response message: ${response.message}")
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "Full response headers: ${response.headers}")
                    Log.e(TAG, "Upload failed with code ${response.code}: $errorBody")
                    return null
                }

                val responseBody = response.body?.string()
                if (responseBody == null) {
                    Log.e(TAG, "Upload response body is null")
                    return null
                }

                // Log only first 500 chars of response to avoid log overflow
                val truncatedResponse = if (responseBody.length > 500)
                    "${responseBody.substring(0, 500)}..."
                else
                    responseBody
                Log.d(TAG, "Upload response received: $truncatedResponse")

                val json = JSONObject(responseBody)
                val secureUrl = json.optString("secure_url")

                if (secureUrl.isNullOrEmpty()) {
                    Log.e(TAG, "No secure_url in response. Available fields: ${json.keys().asSequence().toList()}")
                    Log.e(TAG, "No secure_url in response")
                    return null
                }

                Log.d(TAG, "Upload successful, URL: $secureUrl")
                Log.d(TAG, "-------------------- CLOUDINARY UPLOAD SUCCESS --------------------")
                return secureUrl
            }
        } catch (e: Exception) {
            Log.e(TAG, "-------------------- CLOUDINARY UPLOAD ERROR --------------------")
            Log.e(TAG, "Error during image upload", e)
            Log.e(TAG, "Exception class: ${e.javaClass.name}")
            Log.e(TAG, "Exception message: ${e.message}")
            e.printStackTrace()
            return null
        } finally {
            // Clean up temp file
            tempFile?.let {
                if (it.exists()) {
                    it.delete()
                    Log.d(TAG, "Deleted temp file: ${it.absolutePath}")
                }
            }
            Log.d(TAG, "-------------------- CLOUDINARY UPLOAD END --------------------")
        }
    }
}
