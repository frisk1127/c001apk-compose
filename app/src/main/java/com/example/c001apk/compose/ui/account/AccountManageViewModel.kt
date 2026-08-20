package com.example.c001apk.compose.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.logic.model.AccountEntity
import com.example.c001apk.compose.logic.repository.AccountRepository
import com.example.c001apk.compose.logic.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountManageViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val allAccounts: Flow<List<AccountEntity>> = accountRepository.allAccounts
    val userPreferences = userPreferencesRepository.data

    fun switchAccount(uid: String) {
        viewModelScope.launch {
            accountRepository.switchAccount(uid)
        }
    }

    fun deleteAccount(uid: String) {
        viewModelScope.launch {
            accountRepository.deleteAccount(uid)
        }
    }

    fun logoutCurrent() {
        viewModelScope.launch {
            accountRepository.logoutCurrent()
        }
    }

    fun logoutAll() {
        viewModelScope.launch {
            accountRepository.logoutAll()
        }
    }
}
