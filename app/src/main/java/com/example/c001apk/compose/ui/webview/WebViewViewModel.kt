package com.example.c001apk.compose.ui.webview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.logic.repository.AccountRepository
import com.example.c001apk.compose.logic.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by bggRGjQaUbCoE on 2024/6/11
 */
@HiltViewModel
class WebViewViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    var isLoginSuccess by mutableStateOf(false)
        private set

    fun setIsLogin(uid: String, username: String, token: String) {
        viewModelScope.launch {
            userPreferencesRepository.apply {
                setUid(uid)
                setUsername(username)
                setToken(token)
                setIsLogin(true)
            }
            accountRepository.saveOrUpdateAccount(
                uid = uid,
                username = username,
                token = token
            )
            isLoginSuccess = true
        }
    }

}