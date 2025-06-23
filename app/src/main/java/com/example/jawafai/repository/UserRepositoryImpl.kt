package com.example.jawafai.repository

import android.util.Log
import com.example.jawafai.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

    override suspend fun registerUser(user: UserModel, password: String): Boolean =
        suspendCoroutine { continuation ->
            Log.d(TAG, "Starting user registration for email: ${user.email}")

            auth.createUserWithEmailAndPassword(user.email, password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid
                    if (uid == null) {
                        Log.e(TAG, "Auth succeeded but user ID is null")
                        continuation.resume(false)
                        return@addOnSuccessListener
                    }

                    Log.d(TAG, "Authentication successful, user created with ID: $uid")

                    // Create user document in Firestore
                    val userWithId = user.copy(
                        id = uid,
                        password = "" // Don't store password in Firestore
                    )

                    // Try to store in Firestore, but consider Auth success as overall success
                    usersCollection.document(uid)
                        .set(userWithId.toMap())
                        .addOnSuccessListener {
                            Log.d(TAG, "Firestore document created successfully")
                            continuation.resume(true)
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Firestore write failed: ${e.message}")
                            // Still consider registration successful if auth worked but Firestore failed
                            // This handles the case where Firestore API is not enabled yet
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

            usersCollection.document(currentUid)
                .update(updates)
                .addOnSuccessListener {
                    continuation.resume(true)
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
}
