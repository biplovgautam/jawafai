package com.example.jawafai.repository

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

    private val usersCollection = firestore.collection("users")

    override suspend fun registerUser(user: UserModel, password: String): Boolean =
        suspendCoroutine { continuation ->
            auth.createUserWithEmailAndPassword(user.email, password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid
                    if (uid == null) {
                        continuation.resume(false)
                        return@addOnSuccessListener
                    }

                    // Create user document in Firestore
                    val userWithId = user.copy(
                        id = uid,
                        password = "" // Don't store password in Firestore
                    )

                    usersCollection.document(uid)
                        .set(userWithId.toMap())
                        .addOnSuccessListener {
                            continuation.resume(true)
                        }
                        .addOnFailureListener { e ->
                            continuation.resumeWithException(e)
                        }
                }
                .addOnFailureListener { e ->
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
