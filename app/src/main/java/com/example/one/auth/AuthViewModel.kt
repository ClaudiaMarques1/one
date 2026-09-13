package com.example.one.auth

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.one.data.FirebaseModule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val googleAuthClient = GoogleAuthClient(application)

    val currentUser: StateFlow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
        FirebaseModule.auth.addAuthStateListener(listener)
        awaitClose { FirebaseModule.auth.removeAuthStateListener(listener) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = FirebaseModule.auth.currentUser
    )

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _authError.value = null
            val result = googleAuthClient.signIn(activityContext)
            _authError.value = result.exceptionOrNull()?.message
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authError.value = null
            runCatching {
                FirebaseModule.auth.signInWithEmailAndPassword(email, password).await()
            }.onFailure { _authError.value = it.message }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authError.value = null
            runCatching {
                FirebaseModule.auth.createUserWithEmailAndPassword(email, password).await()
            }.onFailure { _authError.value = it.message }
        }
    }

    fun signOut() {
        googleAuthClient.signOut()
        FirebaseModule.auth.signOut()
    }
}
