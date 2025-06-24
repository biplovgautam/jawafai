package com.example.jawafai.repository

import android.net.Uri
import com.example.jawafai.model.UserModel

/**
 * Repository interface for handling user authentication and Firestore operations
 */
interface UserRepository {
    /**
     * Registers a new user with Firebase Authentication and stores their data in Firestore
     * @param user The user model containing registration information
     * @param password The user's password for authentication
     * @return Boolean indicating success or failure
     */
    suspend fun registerUser(user: UserModel, password: String): Boolean

    /**
     * Updates an existing user's information in Firestore
     * @param user The updated user model
     * @return Boolean indicating success or failure
     */
    suspend fun updateUser(user: UserModel): Boolean

    /**
     * Deletes a user's account and their associated data
     * @param userId The Firebase UID of the user to delete
     * @return Boolean indicating success or failure
     */
    suspend fun deleteUser(userId: String): Boolean

    /**
     * Signs out the current user from Firebase Authentication
     */
    fun logout()

    /**
     * Uploads a profile image to Cloudinary and returns the image URL
     */
    suspend fun uploadProfileImage(imageUri: Uri): String?
}
