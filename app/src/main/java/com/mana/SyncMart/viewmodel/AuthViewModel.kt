package com.mana.SyncMart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.mana.SyncMart.data.model.User
import com.mana.SyncMart.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Check if user is already logged in
    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    // Get current user ID
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // Get current user email
    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    // LOGIN
    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Email and password are required"
            onResult(false)
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    _errorMessage.value = null
                    onResult(true)
                } else {
                    _errorMessage.value = task.exception?.message ?: "Login failed"
                    onResult(false)
                }
            }
    }

    // SIGNUP
    fun signup(email: String, password: String, name: String = "", onResult: (Boolean) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Email and password are required"
            onResult(false)
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Create user document in Firestore
                    val firebaseUser = task.result.user
                    if (firebaseUser != null) {
                        val user = User(
                            uid = firebaseUser.uid,
                            name = name.ifBlank { firebaseUser.email?.substringBefore("@") ?: "User" },
                            email = firebaseUser.email ?: email
                        )

                        viewModelScope.launch {
                            val result = firestoreRepository.createUserDocument(user)
                            _isLoading.value = false

                            if (result.isSuccess) {
                                _errorMessage.value = null
                                onResult(true)
                            } else {
                                _errorMessage.value = "Failed to create user profile"
                                onResult(false)
                            }
                        }
                    } else {
                        _isLoading.value = false
                        _errorMessage.value = "Failed to create user"
                        onResult(false)
                    }
                } else {
                    _isLoading.value = false
                    _errorMessage.value = task.exception?.message ?: "Signup failed"
                    onResult(false)
                }
            }
    }

    // FORGOT PASSWORD
    fun forgotPassword(email: String, onResult: (Boolean) -> Unit) {
        if (email.isBlank()) {
            _errorMessage.value = "Email is required"
            onResult(false)
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    _errorMessage.value = null
                    onResult(true)
                } else {
                    _errorMessage.value = task.exception?.message ?: "Failed to send reset email"
                    onResult(false)
                }
            }
    }

    // LOGOUT
    fun logout() {
        auth.signOut()
        clearError()
    }

    // Clear error message
    fun clearError() {
        _errorMessage.value = null
    }
}