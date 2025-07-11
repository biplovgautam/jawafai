package com.example.jawafai.model

data class UserModel(
    val id: String = "", // Firebase UID
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "", // Added username field
    val email: String = "",
    val password: String = "", // Note: This won't be stored in Firestore
    val dateOfBirth: String = "",
    val imageUrl: String? = null, // Optional - users can add later in profile
    val bio: String = "", // Optional - users can add later in profile
    val createdAt: Long = System.currentTimeMillis()
) {
    // Convert to map for Firestore
    fun toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "id" to id,
            "firstName" to firstName,
            "lastName" to lastName,
            "username" to username,
            "email" to email,
            "dateOfBirth" to dateOfBirth,
            "createdAt" to createdAt
        )

        // Only add these fields if they're not empty
        if (!bio.isNullOrBlank()) map["bio"] = bio
        if (!imageUrl.isNullOrBlank()) map["imageUrl"] = imageUrl

        return map
    }

    companion object {
        // Create from Firestore document
        fun fromMap(map: Map<String, Any>): UserModel {
            return UserModel(
                id = map["id"] as? String ?: "",
                firstName = map["firstName"] as? String ?: "",
                lastName = map["lastName"] as? String ?: "",
                username = map["username"] as? String ?: "",
                email = map["email"] as? String ?: "",
                dateOfBirth = map["dateOfBirth"] as? String ?: "",
                imageUrl = map["imageUrl"] as? String,
                bio = map["bio"] as? String ?: "",
                createdAt = map["createdAt"] as? Long ?: System.currentTimeMillis()
            )
        }
    }
}
