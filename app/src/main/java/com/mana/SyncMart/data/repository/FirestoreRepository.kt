package com.mana.SyncMart.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.mana.SyncMart.data.model.Item
import com.mana.SyncMart.data.model.ShoppingList
import com.mana.SyncMart.data.model.User
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    /** 🔹 Get current logged-in user ID */
    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    /** 🔹 Create user document after signup */
    suspend fun createUserDocument(user: User): Result<Unit> {
        return try {
            db.collection("users")
                .document(user.uid)
                .set(mapOf(
                    "uid" to user.uid,
                    "name" to user.name,
                    "email" to user.email,
                    "lists" to emptyList<String>(),
                    "friends" to emptyList<String>()
                ))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 🔹 Fetch all lists for the current user */
    fun getUserLists(
        onSuccess: (List<ShoppingList>) -> Unit,
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
                val listIds = document.get("lists") as? List<String> ?: emptyList()
                if (listIds.isEmpty()) {
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }

                db.collection("lists")
                    .whereIn("id", listIds)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val lists = querySnapshot.documents.mapNotNull {
                            it.toObject(ShoppingList::class.java)
                        }
                        onSuccess(lists)
                    }
                    .addOnFailureListener { e ->
                        onError(e.message ?: "Error fetching lists")
                    }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error fetching user data")
            }
    }

    /** 🔹 Fetch items for a given list */
    fun getItems(
        listId: String,
        onSuccess: (List<Item>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("lists")
            .document(listId)
            .collection("items")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val items = querySnapshot.documents.mapNotNull {
                    it.toObject(Item::class.java)
                }
                onSuccess(items)
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error fetching items")
            }
    }

    /** 🔹 Add a new shopping list */
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

        val newDoc = db.collection("lists").document()
        val newList = ShoppingList(
            id = newDoc.id,
            name = name,
            ownerId = userId,
            sharedWith = emptyList()
        )

        newDoc.set(newList)
            .addOnSuccessListener {
                db.collection("users")
                    .document(userId)
                    .update("lists", FieldValue.arrayUnion(newList.id))
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        onError(e.message ?: "Error updating user lists")
                    }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error adding list")
            }
    }

    /** 🔹 Add item to a shopping list */
    fun addItem(
        listId: String,
        item: Item,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val newDoc = db.collection("lists")
            .document(listId)
            .collection("items")
            .document()

        val newItem = item.copy(id = newDoc.id)

        newDoc.set(newItem)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error adding item")
            }
    }

    /** 🔹 Update item (e.g., mark as purchased) */
    fun updateItem(
        listId: String,
        item: Item,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("lists")
            .document(listId)
            .collection("items")
            .document(item.id)
            .set(item)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error updating item")
            }
    }

    /** 🔹 Delete item from shopping list */
    fun deleteItem(
        listId: String,
        itemId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("lists")
            .document(listId)
            .collection("items")
            .document(itemId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error deleting item")
            }
    }

    /** 🔹 Share list with another user */
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
                val targetUser = querySnapshot.documents.firstOrNull()
                if (targetUser == null) {
                    onError("User not found")
                    return@addOnSuccessListener
                }

                val targetUserId = targetUser.id

                // Add user to list's sharedWith array
                db.collection("lists")
                    .document(listId)
                    .update("sharedWith", FieldValue.arrayUnion(targetUserId))
                    .addOnSuccessListener {
                        // Add list to user's lists array
                        db.collection("users")
                            .document(targetUserId)
                            .update("lists", FieldValue.arrayUnion(listId))
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { e ->
                                onError(e.message ?: "Error updating user's lists")
                            }
                    }
                    .addOnFailureListener { e ->
                        onError(e.message ?: "Error sharing list")
                    }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error finding user")
            }
    }
}