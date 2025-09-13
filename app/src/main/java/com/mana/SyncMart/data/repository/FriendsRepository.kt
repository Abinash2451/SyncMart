package com.mana.SyncMart.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.mana.SyncMart.data.model.User
import kotlinx.coroutines.tasks.await

class FriendsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    /** 🔹 Get current logged-in user ID */
    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    /** 🔹 Get friends list for current user */
    fun getFriends(
        onSuccess: (List<User>) -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = getCurrentUserId()
        if (userId == null) {
            onError("User not logged in")
            return
        }

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val friendIds = document.get("friends") as? List<String> ?: emptyList()
                if (friendIds.isEmpty()) {
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }

                // Fetch friend details
                db.collection("users")
                    .whereIn("uid", friendIds)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val friends = querySnapshot.documents.mapNotNull {
                            it.toObject(User::class.java)
                        }
                        onSuccess(friends)
                    }
                    .addOnFailureListener { e ->
                        onError(e.message ?: "Error fetching friends")
                    }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error fetching user data")
            }
    }

    /** 🔹 Search for user by email */
    fun searchUserByEmail(
        email: String,
        onSuccess: (User?) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val user = querySnapshot.documents.firstOrNull()?.toObject(User::class.java)
                onSuccess(user)
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error searching user")
            }
    }

    /** 🔹 Add friend by user ID */
    fun addFriend(
        friendId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = getCurrentUserId()
        if (userId == null) {
            onError("User not logged in")
            return
        }

        // Add friend to current user's friends list
        db.collection("users")
            .document(userId)
            .update("friends", FieldValue.arrayUnion(friendId))
            .addOnSuccessListener {
                // Add current user to friend's friends list (mutual friendship)
                db.collection("users")
                    .document(friendId)
                    .update("friends", FieldValue.arrayUnion(userId))
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        onError(e.message ?: "Error adding mutual friendship")
                    }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error adding friend")
            }
    }

    /** 🔹 Remove friend */
    fun removeFriend(
        friendId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = getCurrentUserId()
        if (userId == null) {
            onError("User not logged in")
            return
        }

        // Remove friend from current user's friends list
        db.collection("users")
            .document(userId)
            .update("friends", FieldValue.arrayRemove(friendId))
            .addOnSuccessListener {
                // Remove current user from friend's friends list
                db.collection("users")
                    .document(friendId)
                    .update("friends", FieldValue.arrayRemove(userId))
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        onError(e.message ?: "Error removing mutual friendship")
                    }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error removing friend")
            }
    }
}