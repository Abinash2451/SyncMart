package com.mana.SyncMart.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.mana.SyncMart.data.model.Item
import com.mana.SyncMart.data.model.ShoppingList
import com.mana.SyncMart.data.model.User
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    // Helper function to get current user ID
    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    /** ✅ Create user document in Firestore */
    suspend fun createUserDocument(user: User): Result<Unit> {
        return try {
            db.collection("users")
                .document(user.uid)
                .set(
                    mapOf(
                        "uid" to user.uid,
                        "name" to user.name,
                        "email" to user.email,
                        "friends" to emptyList<String>()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** ✅ FIXED: Get user's shopping lists (both owned and shared) with real-time updates */
    fun getUserLists(
        onSuccess: (List<ShoppingList>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val userId = getCurrentUserId()
        if (userId == null) {
            onError("User not logged in")
            return null
        }

        // Use real-time listener for immediate updates
        val listener = db.collection("shopping_lists")
            .whereArrayContains("allUsers", userId) // New field to track all users with access
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Error fetching lists")
                    return@addSnapshotListener
                }

                querySnapshot?.let { snapshot ->
                    val lists = snapshot.documents.mapNotNull { document ->
                        try {
                            document.toObject(ShoppingList::class.java)?.copy(id = document.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onSuccess(lists)
                } ?: onError("No data received")
            }

        return listener
    }

    /** ✅ FIXED: Get items in a shopping list with real-time updates */
    fun getItems(
        listId: String,
        onSuccess: (List<Item>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        // Use real-time listener for immediate item updates
        val listener = db.collection("shopping_lists")
            .document(listId)
            .collection("items")
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Error fetching items")
                    return@addSnapshotListener
                }

                querySnapshot?.let { snapshot ->
                    val items = snapshot.documents.mapNotNull { document ->
                        try {
                            document.toObject(Item::class.java)?.copy(id = document.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onSuccess(items)
                } ?: onError("No items data received")
            }

        return listener
    }

    /** ✅ FIXED: Add new shopping list with proper user tracking */
    fun addShoppingList(
        name: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = getCurrentUserId()
        if (userId == null) {
            onError("User not logged in")
            return
        }

        val listData = mapOf(
            "name" to name,
            "ownerId" to userId,
            "sharedWith" to emptyList<String>(),
            "allUsers" to listOf(userId), // Track all users with access
            "createdAt" to FieldValue.serverTimestamp(),
            "lastModified" to FieldValue.serverTimestamp()
        )

        db.collection("shopping_lists")
            .add(listData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error adding list")
            }
    }

    /** ✅ Update shopping list name with timestamp */
    fun updateShoppingListName(
        listId: String,
        newName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val updates = mapOf(
            "name" to newName,
            "lastModified" to FieldValue.serverTimestamp()
        )

        db.collection("shopping_lists")
            .document(listId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error updating list name")
            }
    }

    /** ✅ Delete shopping list */
    fun deleteShoppingList(
        listId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // First delete all items in the list
        db.collection("shopping_lists")
            .document(listId)
            .collection("items")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                querySnapshot.documents.forEach { document ->
                    batch.delete(document.reference)
                }
                batch.commit()
                    .addOnSuccessListener {
                        // Then delete the list itself
                        db.collection("shopping_lists")
                            .document(listId)
                            .delete()
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { e ->
                                onError(e.message ?: "Error deleting list")
                            }
                    }
                    .addOnFailureListener { e ->
                        onError(e.message ?: "Error deleting list items")
                    }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error fetching list items for deletion")
            }
    }

    /** ✅ FIXED: Add item to shopping list with timestamp */
    fun addItem(
        listId: String,
        item: Item,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val itemData = mapOf(
            "name" to item.name,
            "quantity" to item.quantity,
            "purchased" to item.purchased,
            "createdAt" to FieldValue.serverTimestamp(),
            "lastModified" to FieldValue.serverTimestamp()
        )

        val batch = db.batch()

        // Add the item
        val itemRef = db.collection("shopping_lists")
            .document(listId)
            .collection("items")
            .document()

        batch.set(itemRef, itemData)

        // Update list's last modified timestamp
        val listRef = db.collection("shopping_lists").document(listId)
        batch.update(listRef, "lastModified", FieldValue.serverTimestamp())

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error adding item")
            }
    }

    /** ✅ FIXED: Update item with timestamp */
    fun updateItem(
        listId: String,
        item: Item,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val itemData = mapOf(
            "name" to item.name,
            "quantity" to item.quantity,
            "purchased" to item.purchased,
            "lastModified" to FieldValue.serverTimestamp()
        )

        val batch = db.batch()

        // Update the item
        val itemRef = db.collection("shopping_lists")
            .document(listId)
            .collection("items")
            .document(item.id)

        batch.update(itemRef, itemData)

        // Update list's last modified timestamp
        val listRef = db.collection("shopping_lists").document(listId)
        batch.update(listRef, "lastModified", FieldValue.serverTimestamp())

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error updating item")
            }
    }

    /** ✅ FIXED: Delete item with timestamp update */
    fun deleteItem(
        listId: String,
        itemId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val batch = db.batch()

        // Delete the item
        val itemRef = db.collection("shopping_lists")
            .document(listId)
            .collection("items")
            .document(itemId)

        batch.delete(itemRef)

        // Update list's last modified timestamp
        val listRef = db.collection("shopping_lists").document(listId)
        batch.update(listRef, "lastModified", FieldValue.serverTimestamp())

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error deleting item")
            }
    }

    /** ✅ FIXED: Share list with another user - now properly tracks all users */
    fun shareList(
        listId: String,
        userEmail: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // First find the user by email
        db.collection("users")
            .whereEqualTo("email", userEmail)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    onError("User not found with email: $userEmail")
                    return@addOnSuccessListener
                }

                val userDoc = querySnapshot.documents.first()
                val userId = userDoc.getString("uid")
                if (userId != null) {
                    // Update the list with new shared user
                    val updates = mapOf(
                        "sharedWith" to FieldValue.arrayUnion(userId),
                        "allUsers" to FieldValue.arrayUnion(userId), // Critical: Add to allUsers for querying
                        "lastModified" to FieldValue.serverTimestamp()
                    )

                    db.collection("shopping_lists")
                        .document(listId)
                        .update(updates)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e ->
                            onError(e.message ?: "Error sharing list")
                        }
                } else {
                    onError("Invalid user data")
                }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error finding user")
            }
    }

    /** 🔄 CRITICAL: One-time database migration to fix existing lists */
    fun migrateExistingLists(
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        db.collection("shopping_lists")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                var updatedCount = 0

                querySnapshot.documents.forEach { document ->
                    val data = document.data
                    val ownerId = data?.get("ownerId") as? String
                    val sharedWith = data?.get("sharedWith") as? List<String> ?: emptyList()

                    if (ownerId != null && !data.containsKey("allUsers")) {
                        // Create allUsers field with owner + shared users
                        val allUsers = mutableListOf(ownerId).apply {
                            addAll(sharedWith)
                        }.distinct()

                        batch.update(document.reference, mapOf(
                            "allUsers" to allUsers,
                            "lastModified" to FieldValue.serverTimestamp()
                        ))
                        updatedCount++
                    }
                }

                if (updatedCount > 0) {
                    batch.commit()
                        .addOnSuccessListener {
                            onComplete(true, "Migration completed: $updatedCount lists updated")
                        }
                        .addOnFailureListener { e ->
                            onComplete(false, "Migration failed: ${e.message}")
                        }
                } else {
                    onComplete(true, "No migration needed - all lists already updated")
                }
            }
            .addOnFailureListener { e ->
                onComplete(false, "Migration failed: ${e.message}")
            }
    }
}