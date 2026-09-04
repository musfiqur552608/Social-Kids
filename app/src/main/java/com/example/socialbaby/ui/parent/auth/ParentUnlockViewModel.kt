package com.example.socialbaby.ui.parent.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialbaby.domain.repository.ParentAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentUnlockViewModel @Inject constructor(
    private val authRepo: ParentAuthRepository
) : ViewModel() {

    private val _pin = MutableStateFlow("")
    val pin: StateFlow<String> = _pin

    private val _isPinSet = MutableStateFlow<Boolean?>(null)
    val isPinSet: StateFlow<Boolean?> = _isPinSet

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked

    init {
        viewModelScope.launch {
            _isPinSet.value = authRepo.isPinSetFlow.first()
        }
    }

    fun onPinDigit(d: String) {
        if (_pin.value.length < 6) _pin.value += d
    }
    fun onBackspace() {
        if (_pin.value.isNotEmpty()) _pin.value = _pin.value.dropLast(1)
    }
    fun onClear() { _pin.value = ""; _error.value = null }

    fun onConfirm() {
        viewModelScope.launch {
            val isSet = authRepo.isPinSetFlow.first()
            if (!isSet) {
                if (_pin.value.length < 4) {
                    _error.value = "PIN must be at least 4 digits"
                    return@launch
                }
                authRepo.setPin(_pin.value)
                _isPinSet.value = true
                _unlocked.value = true
                _error.value = null
            } else {
                val ok = authRepo.verifyPin(_pin.value)
                if (ok) {
                    _unlocked.value = true
                    _error.value = null
                } else {
                    _error.value = "Wrong PIN"
                    _pin.value = ""
                }
            }
        }
    }

    fun resetUnlocked() { _unlocked.value = false }
}
