package com.mana.SyncMart.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.mana.SyncMart.data.model.User

class FriendsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Get friends list for current user
     * Handles batching for large friend lists (Firestore whereIn limit is 10)
     */
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
                try {
                    val friendIds = document.get("friends") as? List<String> ?: emptyList()
                    if (friendIds.isEmpty()) {
                        onSuccess(emptyList())
                        return@addOnSuccessListener
                    }

                    // Handle Firestore's whereIn limitation of 10 items
                    if (friendIds.size <= 10) {
                        fetchFriendsBatch(friendIds, onSuccess, onError)
                    } else {
                        fetchFriendsInBatches(friendIds, onSuccess, onError)
                    }
                } catch (e: Exception) {
                    onError("Error processing friend data: ${e.message}")
                }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error fetching user data")
            }
    }

    private fun fetchFriendsBatch(
        friendIds: List<String>,
        onSuccess: (List<User>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users")
            .whereIn("uid", friendIds)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val friends = querySnapshot.documents.mapNotNull {
                    try {
                        it.toObject(User::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
                onSuccess(friends)
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error fetching friends")
            }
    }

    private fun fetchFriendsInBatches(
        friendIds: List<String>,
        onSuccess: (List<User>) -> Unit,
        onError: (String) -> Unit
    ) {
        val batches = friendIds.chunked(10)
        val allFriends = mutableListOf<User>()
        var completedBatches = 0

        batches.forEach { batch ->
            db.collection("users")
                .whereIn("uid", batch)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val batchFriends = querySnapshot.documents.mapNotNull {
                        try {
                            it.toObject(User::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    allFriends.addAll(batchFriends)
                    completedBatches++
                    if (completedBatches == batches.size) {
                        onSuccess(allFriends)
                    }
                }
                .addOnFailureListener { e ->
                    onError(e.message ?: "Error fetching friends batch")
                }
        }
    }

    /**
     * FIXED: Improved search with better error handling and retry logic
     */
    fun searchUserByEmail(
        email: String,
        onSuccess: (User?) -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            onError("User not logged in")
            return
        }

        val currentUserEmail = auth.currentUser?.email
        if (email.equals(currentUserEmail, ignoreCase = true)) {
            onError("Cannot add yourself as a friend")
            return
        }

        val searchEmail = email.trim().lowercase() // Normalize to lowercase

        // IMPROVED: Search with multiple strategies
        searchWithMultipleStrategies(searchEmail, currentUserId, onSuccess, onError)
    }

    /**
     * FIXED: Multiple search strategies to handle Firebase inconsistencies
     */
    private fun searchWithMultipleStrategies(
        searchEmail: String,
        currentUserId: String,
        onSuccess: (User?) -> Unit,
        onError: (String) -> Unit
    ) {
        // Strategy 1: Direct lowercase search
        db.collection("users")
            .whereEqualTo("email", searchEmail)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    handleSearchResult(querySnapshot, currentUserId, onSuccess, onError)
                } else {
                    // Strategy 2: Try original case search
                    searchOriginalCase(searchEmail, currentUserId, onSuccess, onError)
                }
            }
            .addOnFailureListener {
                // Strategy 2: Fallback on network error
                searchOriginalCase(searchEmail, currentUserId, onSuccess, onError)
            }
    }

    private fun searchOriginalCase(
        searchEmail: String,
        currentUserId: String,
        onSuccess: (User?) -> Unit,
        onError: (String) -> Unit
    ) {
        // Try with different case variations
        val emailVariations = listOf(
            searchEmail,
            searchEmail.lowercase(),
            searchEmail.uppercase(),
            searchEmail.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        ).distinct()

        var attemptCount = 0
        var foundUser: User? = null

        emailVariations.forEach { emailVariation ->
            db.collection("users")
                .whereEqualTo("email", emailVariation)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    attemptCount++
                    if (!querySnapshot.isEmpty && foundUser == null) {
                        foundUser = querySnapshot.documents.firstOrNull()?.toObject(User::class.java)
                    }

                    // Check if all attempts completed
                    if (attemptCount >= emailVariations.size) {
                        if (foundUser != null) {
                            checkIfAlreadyFriend(currentUserId, foundUser!!.uid) { isAlreadyFriend ->
                                if (isAlreadyFriend) {
                                    onError("This user is already your friend")
                                } else {
                                    onSuccess(foundUser)
                                }
                            }
                        } else {
                            // Strategy 3: Fallback to full collection scan
                            fallbackFullScan(searchEmail, currentUserId, onSuccess, onError)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    attemptCount++
                    if (attemptCount >= emailVariations.size && foundUser == null) {
                        fallbackFullScan(searchEmail, currentUserId, onSuccess, onError)
                    }
                }
        }
    }

    /**
     * FIXED: Fallback method for comprehensive search
     */
    private fun fallbackFullScan(
        searchEmail: String,
        currentUserId: String,
        onSuccess: (User?) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users")
            .get()
            .addOnSuccessListener { querySnapshot ->
                try {
                    val user = querySnapshot.documents
                        .mapNotNull { document ->
                            try {
                                document.toObject(User::class.java)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        .firstOrNull { user ->
                            user.email.equals(searchEmail, ignoreCase = true)
                        }

                    if (user != null) {
                        checkIfAlreadyFriend(currentUserId, user.uid) { isAlreadyFriend ->
                            if (isAlreadyFriend) {
                                onError("This user is already your friend")
                            } else {
                                onSuccess(user)
                            }
                        }
                    } else {
                        onSuccess(null) // No user found
                    }
                } catch (e: Exception) {
                    onError("Error processing search result: ${e.message}")
                }
            }
            .addOnFailureListener { e ->
                onError("Search failed: ${e.message}")
            }
    }

    private fun handleSearchResult(
        querySnapshot: com.google.firebase.firestore.QuerySnapshot,
        currentUserId: String,
        onSuccess: (User?) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val user = querySnapshot.documents.firstOrNull()?.toObject(User::class.java)
            if (user != null) {
                checkIfAlreadyFriend(currentUserId, user.uid) { isAlreadyFriend ->
                    if (isAlreadyFriend) {
                        onError("This user is already your friend")
                    } else {
                        onSuccess(user)
                    }
                }
            } else {
                onSuccess(null)
            }
        } catch (e: Exception) {
            onError("Error processing search result: ${e.message}")
        }
    }

    private fun checkIfAlreadyFriend(
        currentUserId: String,
        targetUserId: String,
        callback: (Boolean) -> Unit
    ) {
        db.collection("users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                try {
                    val friendIds = document.get("friends") as? List<String> ?: emptyList()
                    callback(friendIds.contains(targetUserId))
                } catch (e: Exception) {
                    callback(false)
                }
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    /**
     * Add friend with comprehensive validation
     * Creates mutual friendship (bidirectional)
     */
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

        if (userId == friendId) {
            onError("Cannot add yourself as a friend")
            return
        }

        // Check if already friends
        checkIfAlreadyFriend(userId, friendId) { isAlreadyFriend ->
            if (isAlreadyFriend) {
                onError("This user is already your friend")
                return@checkIfAlreadyFriend
            }

            // Verify friend exists
            db.collection("users")
                .document(friendId)
                .get()
                .addOnSuccessListener { friendDoc ->
                    if (!friendDoc.exists()) {
                        onError("User not found")
                        return@addOnSuccessListener
                    }

                    // Add mutual friendship
                    db.collection("users")
                        .document(userId)
                        .update("friends", FieldValue.arrayUnion(friendId))
                        .addOnSuccessListener {
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
                .addOnFailureListener { e ->
                    onError(e.message ?: "Error verifying friend exists")
                }
        }
    }

    /**
     * Remove friend - removes mutual friendship
     */
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

        // Remove mutual friendship
        db.collection("users")
            .document(userId)
            .update("friends", FieldValue.arrayRemove(friendId))
            .addOnSuccessListener {
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