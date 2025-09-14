package com.mana.SyncMart.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

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

    // Get current user display name
    fun getCurrentUserDisplayName(): String? {
        return auth.currentUser?.displayName
    }

    // Initialize Google Sign-In client
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("832583157197-4rc33tfftri1b3cgm8n4v886mpmvoba5.apps.googleusercontent.com") // Replace with your web client ID from Firebase Console
            .requestEmail()
            .build()

        return GoogleSignIn.getClient(context, gso)
    }

    // Handle Google Sign-In result
    fun handleGoogleSignInResult(data: android.content.Intent?, onResult: (Boolean) -> Unit) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)

            val credential = GoogleAuthProvider.getCredential(account.idToken, null)

            _isLoading.value = true
            _errorMessage.value = null

            auth.signInWithCredential(credential)
                .addOnCompleteListener { authTask ->
                    _isLoading.value = false
                    if (authTask.isSuccessful) {
                        val firebaseUser = authTask.result?.user
                        if (firebaseUser != null) {
                            // Create or update user document
                            val user = User(
                                uid = firebaseUser.uid,
                                name = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "User",
                                email = firebaseUser.email ?: "",
                                displayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "User",
                                profilePicture = firebaseUser.photoUrl?.toString() ?: "",
                                isEmailVerified = firebaseUser.isEmailVerified,
                                signInProvider = "google"
                            )

                            viewModelScope.launch {
                                val result = firestoreRepository.createOrUpdateUserDocument(user)
                                if (result.isSuccess) {
                                    _errorMessage.value = null
                                    onResult(true)
                                } else {
                                    _errorMessage.value = "Failed to create user profile"
                                    onResult(false)
                                }
                            }
                        } else {
                            _errorMessage.value = "Failed to get user information"
                            onResult(false)
                        }
                    } else {
                        _errorMessage.value = authTask.exception?.message ?: "Google sign-in failed"
                        onResult(false)
                    }
                }
        } catch (e: ApiException) {
            _isLoading.value = false
            _errorMessage.value = "Google sign-in failed: ${e.message}"
            onResult(false)
        }
    }

    // LOGIN with email/password (with email verification check)
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
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user?.isEmailVerified == true) {
                        _isLoading.value = false
                        _errorMessage.value = null
                        onResult(true)
                    } else {
                        // Email not verified, send verification email
                        user?.sendEmailVerification()
                            ?.addOnCompleteListener { verificationTask ->
                                _isLoading.value = false
                                if (verificationTask.isSuccessful) {
                                    auth.signOut() // Sign out until verified
                                    _errorMessage.value = "Please check your email and verify your account before signing in. Verification email sent."
                                } else {
                                    _errorMessage.value = "Please verify your email address before signing in."
                                }
                                onResult(false)
                            }
                    }
                } else {
                    _isLoading.value = false
                    _errorMessage.value = task.exception?.message ?: "Login failed"
                    onResult(false)
                }
            }
    }

    // SIGNUP with email/password (with automatic email verification)
    fun signup(email: String, password: String, name: String = "", displayName: String = "", onResult: (Boolean) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Email and password are required"
            onResult(false)
            return
        }

        if (name.isBlank() || displayName.isBlank()) {
            _errorMessage.value = "Name and display name are required"
            onResult(false)
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    if (firebaseUser != null) {
                        // Send email verification
                        firebaseUser.sendEmailVerification()
                            .addOnCompleteListener { verificationTask ->
                                if (verificationTask.isSuccessful) {
                                    // Create user document
                                    val user = User(
                                        uid = firebaseUser.uid,
                                        name = name,
                                        email = firebaseUser.email ?: email,
                                        displayName = displayName,
                                        profilePicture = "",
                                        isEmailVerified = false,
                                        signInProvider = "email"
                                    )

                                    viewModelScope.launch {
                                        val result = firestoreRepository.createOrUpdateUserDocument(user)
                                        _isLoading.value = false

                                        if (result.isSuccess) {
                                            auth.signOut() // Sign out until email is verified
                                            _successMessage.value = "Account created! Please check your email and verify your account before signing in."
                                            onResult(true)
                                        } else {
                                            _errorMessage.value = "Failed to create user profile"
                                            onResult(false)
                                        }
                                    }
                                } else {
                                    _isLoading.value = false
                                    _errorMessage.value = "Failed to send verification email"
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
        clearMessages()
    }

    // Clear error message
    fun clearError() {
        _errorMessage.value = null
    }

    // Clear success message
    fun clearSuccess() {
        _successMessage.value = null
    }

    // Clear all messages
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    // Update user display name
    fun updateDisplayName(newDisplayName: String, onResult: (Boolean) -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            _errorMessage.value = "User not logged in"
            onResult(false)
            return
        }

        if (newDisplayName.isBlank()) {
            _errorMessage.value = "Display name cannot be empty"
            onResult(false)
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = firestoreRepository.updateUserDisplayName(userId, newDisplayName)
            _isLoading.value = false

            if (result.isSuccess) {
                _successMessage.value = "Display name updated successfully"
                onResult(true)
            } else {
                _errorMessage.value = "Failed to update display name"
                onResult(false)
            }
        }
    }
}