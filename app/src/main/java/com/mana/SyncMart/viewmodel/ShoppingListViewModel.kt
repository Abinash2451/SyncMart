package com.mana.SyncMart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    /** 🔹 Fetch lists for current logged-in user */
    fun fetchUserLists() {
        _isLoading.value = true
        _errorMessage.value = null

        repository.getUserLists(
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

    /** 🔹 Fetch items inside a given list */
    fun fetchItems(listId: String) {
        _isLoading.value = true
        _errorMessage.value = null

        repository.getItems(
            listId = listId,
            onSuccess = { fetchedItems ->
                _items.value = fetchedItems
                _isLoading.value = false
            },
            onError = { error ->
                _errorMessage.value = error
                _isLoading.value = false
            }
        )
    }

    /** 🔹 Add new shopping list */
    fun addShoppingList(name: String) {
        if (name.isBlank()) return

        _isLoading.value = true
        _errorMessage.value = null

        repository.addShoppingList(
            name = name,
            onSuccess = {
                fetchUserLists() // refresh after adding
            },
            onError = { error ->
                _errorMessage.value = error
                _isLoading.value = false
            }
        )
    }

    /** 🔹 Add item to shopping list */
    fun addItem(listId: String, item: Item) {
        _errorMessage.value = null

        repository.addItem(
            listId = listId,
            item = item,
            onSuccess = {
                fetchItems(listId) // refresh items after adding
            },
            onError = { error ->
                _errorMessage.value = error
            }
        )
    }

    /** 🔹 Update item (e.g., mark as purchased) */
    fun updateItem(listId: String, item: Item) {
        _errorMessage.value = null

        repository.updateItem(
            listId = listId,
            item = item,
            onSuccess = {
                fetchItems(listId) // refresh items after updating
            },
            onError = { error ->
                _errorMessage.value = error
            }
        )
    }

    /** 🔹 Delete item from shopping list */
    fun deleteItem(listId: String, itemId: String) {
        _errorMessage.value = null

        repository.deleteItem(
            listId = listId,
            itemId = itemId,
            onSuccess = {
                fetchItems(listId) // refresh items after deleting
            },
            onError = { error ->
                _errorMessage.value = error
            }
        )
    }

    /** 🔹 Share list with another user */
    fun shareList(listId: String, userEmail: String) {
        if (userEmail.isBlank()) return

        _errorMessage.value = null

        repository.shareList(
            listId = listId,
            userEmail = userEmail,
            onSuccess = {
                // Could show success message or refresh data
                fetchUserLists()
            },
            onError = { error ->
                _errorMessage.value = error
            }
        )
    }

    /** 🔹 Clear error message */
    fun clearError() {
        _errorMessage.value = null
    }
}