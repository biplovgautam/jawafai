package com.example.jawafai.repository

import android.net.Uri
import android.util.Log
import com.example.jawafai.managers.CloudinaryManager
import com.example.jawafai.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import javax.inject.Inject

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
    override suspend fun isUsernameExists(username: String): Boolean = suspendCoroutine { continuation ->
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
    override suspend fun isEmailExists(email: String): Boolean = suspendCoroutine { continuation ->
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

                // Log what we're trying to write to Firestore
                Log.d(TAG, "Attempting to write user data to Firestore: ${userWithId.toMap()}")

                usersCollection.document(uid)
                    .set(userWithId.toMap())
                    .addOnSuccessListener {
                        Log.d(TAG, "Firestore document created successfully")
                        continuation.resume(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Firestore write failed with error: ${e.message}", e)

                        // We'll consider registration successful even if Firestore write fails
                        // This way the user can at least log in, even if their profile data isn't saved
                        continuation.resume(true)

                        // This allows registration to "succeed" even though Firestore write failed
                        // The user can update their profile later
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

    /**
     * Uploads a profile image using the CloudinaryManager.
     * @param imageUri The URI of the image to upload.
     * @return The secure URL of the uploaded image, or null on failure.
     */
    override suspend fun uploadProfileImage(imageUri: Uri): String? {
        Log.d(TAG, "Uploading image through CloudinaryManager for URI: $imageUri")
        return CloudinaryManager.uploadImage(imageUri)
    }
}
