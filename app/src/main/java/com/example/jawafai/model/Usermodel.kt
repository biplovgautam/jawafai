package com.example.jawafai.model

data class UserModel(
    val id: String = "", // Firebase UID
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "", // Added username field
    val email: String = "",
    val password: String = "", // Note: This won't be stored in Firestore
    val dateOfBirth: String = "",
    val imageUrl: String? = null, // For future Cloudinary integration
    val bio: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    // Convert to map for Firestore
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "firstName" to firstName,
            "lastName" to lastName,
            "username" to username, // Added username to map
            "email" to email,
            "dateOfBirth" to dateOfBirth,
            "imageUrl" to imageUrl,
            "bio" to bio,
            "createdAt" to createdAt
        )
    }

    companion object {
        // Create from Firestore document
        fun fromMap(map: Map<String, Any>): UserModel {
            return UserModel(
                id = map["id"] as? String ?: "",
                firstName = map["firstName"] as? String ?: "",
                lastName = map["lastName"] as? String ?: "",
                username = map["username"] as? String ?: "", // Added username to fromMap
                email = map["email"] as? String ?: "",
                dateOfBirth = map["dateOfBirth"] as? String ?: "",
                imageUrl = map["imageUrl"] as? String,
                bio = map["bio"] as? String ?: "",
                createdAt = map["createdAt"] as? Long ?: System.currentTimeMillis()
            )
        }
    }
}
