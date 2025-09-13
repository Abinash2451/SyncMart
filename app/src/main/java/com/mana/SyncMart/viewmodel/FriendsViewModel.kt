package com.mana.SyncMart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mana.SyncMart.data.model.User
import com.mana.SyncMart.data.repository.FriendsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FriendsViewModel(
    private val repository: FriendsRepository = FriendsRepository()
) : ViewModel() {

    private val _friends = MutableStateFlow<List<User>>(emptyList())
    val friends: StateFlow<List<User>> = _friends

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _searchResult = MutableStateFlow<User?>(null)
    val searchResult: StateFlow<User?> = _searchResult

    /** 🔹 Load friends list */
    fun loadFriends() {
        _isLoading.value = true
        _errorMessage.value = null

        repository.getFriends(
            onSuccess = { friendsList ->
                _friends.value = friendsList
                _isLoading.value = false
            },
            onError = { error ->
                _errorMessage.value = error
                _isLoading.value = false
            }
        )
    }

    /** 🔹 Search for user by email */
    fun searchUser(email: String) {
        if (email.isBlank()) {
            _searchResult.value = null
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        repository.searchUserByEmail(
            email = email,
            onSuccess = { user ->
                _searchResult.value = user
                _isLoading.value = false
            },
            onError = { error ->
                _errorMessage.value = error
                _searchResult.value = null
                _isLoading.value = false
            }
        )
    }

    /** 🔹 Add friend by user ID */
    fun addFriend(friendId: String) {
        _isLoading.value = true
        _errorMessage.value = null

        repository.addFriend(
            friendId = friendId,
            onSuccess = {
                loadFriends() // Refresh friends list
                _searchResult.value = null // Clear search result
                _isLoading.value = false
            },
            onError = { error ->
                _errorMessage.value = error
                _isLoading.value = false
            }
        )
    }

    /** 🔹 Remove friend */
    fun removeFriend(friendId: String) {
        _isLoading.value = true
        _errorMessage.value = null

        repository.removeFriend(
            friendId = friendId,
            onSuccess = {
                loadFriends() // Refresh friends list
                _isLoading.value = false
            },
            onError = { error ->
                _errorMessage.value = error
                _isLoading.value = false
            }
        )
    }

    /** 🔹 Clear error message */
    fun clearError() {
        _errorMessage.value = null
    }

    /** 🔹 Clear search result */
    fun clearSearchResult() {
        _searchResult.value = null
    }
}