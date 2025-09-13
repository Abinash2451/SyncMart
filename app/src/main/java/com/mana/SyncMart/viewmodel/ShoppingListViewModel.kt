package com.mana.SyncMart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.mana.SyncMart.data.model.Item
import com.mana.SyncMart.data.model.ShoppingList
import com.mana.SyncMart.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ShoppingListViewModel(
    private val repository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    private val _shoppingLists = MutableStateFlow<List<ShoppingList>>(emptyList())
    val shoppingLists: StateFlow<List<ShoppingList>> = _shoppingLists

    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Success message state for user feedback
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    // Store listeners for cleanup
    private var listsListener: ListenerRegistration? = null
    private var itemsListener: ListenerRegistration? = null

    /** ✅ FIXED: Fetch user's shopping lists with real-time updates */
    fun fetchUserLists() {
        _isLoading.value = true
        _errorMessage.value = null

        // Clean up existing listener
        listsListener?.remove()

        listsListener = repository.getUserLists(
            onSuccess = { lists ->
                _shoppingLists.value = lists
                _isLoading.value = false
            },
            onError = { error ->
                _errorMessage.value = error
                _isLoading.value = false
            }
        )
    }

    /** ✅ FIXED: Fetch items for a specific list with real-time updates */
    fun fetchItems(listId: String) {
        _isLoading.value = true
        _errorMessage.value = null

        // Clean up existing listener
        itemsListener?.remove()

        itemsListener = repository.getItems(
            listId = listId,
            onSuccess = { items ->
                _items.value = items
                _isLoading.value = false
            },
            onError = { error ->
                _errorMessage.value = error
                _isLoading.value = false
            }
        )
    }

    /** ✅ Add new shopping list */
    fun addShoppingList(name: String) {
        if (name.isBlank()) {
            _errorMessage.value = "List name cannot be empty"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        repository.addShoppingList(
            name = name,
            onSuccess = {
                _isLoading.value = false
                _errorMessage.value = null
                // No need to manually refresh - real-time listener will handle it
            },
            onError = { error ->
                _errorMessage.value = error
                _isLoading.value = false
            }
        )
    }

    /** ✅ Update shopping list name */
    fun updateShoppingListName(listId: String, newName: String) {
        if (newName.isBlank()) {
            _errorMessage.value = "List name cannot be empty"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        repository.updateShoppingListName(
            listId = listId,
            newName = newName,
            onSuccess = {
                _isLoading.value = false
                _errorMessage.value = null
                // Real-time listener will update the UI automatically
            },
            onError = { error ->
                _errorMessage.value = error
                _isLoading.value = false
            }
        )
    }

    /** ✅ Delete shopping list */
    fun deleteShoppingList(listId: String) {
        _isLoading.value = true
        _errorMessage.value = null

        repository.deleteShoppingList(
            listId = listId,
            onSuccess = {
                _isLoading.value = false
                _errorMessage.value = null
                // Real-time listener will update the UI automatically
            },
            onError = { error ->
                _errorMessage.value = error
                _isLoading.value = false
            }
        )
    }

    /** ✅ FIXED: Add item to shopping list */
    fun addItem(listId: String, item: Item) {
        if (item.name.isBlank()) {
            _errorMessage.value = "Item name cannot be empty"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        repository.addItem(
            listId = listId,
            item = item,
            onSuccess = {
                _isLoading.value = false
                _errorMessage.value = null
                // Real-time listener will update items automatically
            },
            onError = { error ->
                _errorMessage.value = error
                _isLoading.value = false
            }
        )
    }

    /** ✅ FIXED: Update item */
    fun updateItem(listId: String, item: Item) {
        repository.updateItem(
            listId = listId,
            item = item,
            onSuccess = {
                _errorMessage.value = null
                // Real-time listener will update items automatically
            },
            onError = { error ->
                _errorMessage.value = error
            }
        )
    }

    /** ✅ FIXED: Delete item */
    fun deleteItem(listId: String, itemId: String) {
        _isLoading.value = true
        _errorMessage.value = null

        repository.deleteItem(
            listId = listId,
            itemId = itemId,
            onSuccess = {
                _isLoading.value = false
                _errorMessage.value = null
                // Real-time listener will update items automatically
            },
            onError = { error ->
                _errorMessage.value = error
                _isLoading.value = false
            }
        )
    }

    /** ✅ Share list with friend by email */
    fun shareList(listId: String, friendEmail: String) {
        if (friendEmail.isBlank()) {
            _errorMessage.value = "Friend email cannot be empty"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        repository.shareList(
            listId = listId,
            userEmail = friendEmail,
            onSuccess = {
                _isLoading.value = false
                _errorMessage.value = null
                // Show success message
                _successMessage.value = "List shared successfully with $friendEmail"
                // Real-time listener will update lists automatically
            },
            onError = { error ->
                _errorMessage.value = error
                _isLoading.value = false
            }
        )
    }

    /** ✅ Clear error message */
    fun clearError() {
        _errorMessage.value = null
    }

    /** ✅ Clear success message */
    fun clearSuccess() {
        _successMessage.value = null
    }

    /** ✅ FIXED: Clear items and cleanup listener when navigating away */
    fun clearItems() {
        _items.value = emptyList()
        itemsListener?.remove()
        itemsListener = null
    }

    /** ✅ FIXED: Cleanup listeners when ViewModel is cleared */
    override fun onCleared() {
        super.onCleared()
        listsListener?.remove()
        itemsListener?.remove()
    }
}