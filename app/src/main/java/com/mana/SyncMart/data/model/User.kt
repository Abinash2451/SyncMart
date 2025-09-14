package com.mana.SyncMart.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val displayName: String = "", // New field for user-friendly display name
    val profilePicture: String = "", // For Google profile picture URL
    val isEmailVerified: Boolean = false, // Track email verification status
    val signInProvider: String = "" // Track how user signed in (google, email, etc.)
)