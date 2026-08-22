package com.example.c001apk.compose.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.logic.repository.AccountRepository
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.logic.repository.UserPreferencesRepository
import com.example.c001apk.compose.ui.base.PrefsViewModel
import com.example.c001apk.compose.util.CookieUtil
import com.example.c001apk.compose.util.CookieUtil.SESSID
import com.example.c001apk.compose.util.encode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by bggRGjQaUbCoE on 2024/6/3
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val networkRepo: NetworkRepo,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val accountRepository: AccountRepository,
) : PrefsViewModel(userPreferencesRepository) {

    init {
        getCheckLoginInfo()
    }

    var badge by mutableIntStateOf(0)
        private set

    fun getCheckLoginInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.checkLoginInfo()
                .collect { result ->
                    val response = result.getOrNull()
                    if (response != null) {
                        response.body()?.data?.let { login ->
                            badge = login.notifyCount.badge
                            CookieUtil.atme = login.notifyCount.atme
                            CookieUtil.atcommentme = login.notifyCount.atcommentme
                            CookieUtil.feedlike = login.notifyCount.feedlike
                            CookieUtil.contacts_follow = login.notifyCount.contactsFollow
                            CookieUtil.message = login.notifyCount.message

                            userPreferencesRepository.apply {
                                setUid(login.uid)
                                setUserAvatar(login.userAvatar)
                                setUsername(login.username.encode)
                                setToken(login.token)
                                setIsLogin(true)
                            }
                            accountRepository.saveOrUpdateAccount(
                                uid = login.uid,
                                username = login.username.encode,
                                token = login.token,
                                userAvatar = login.userAvatar
                            )
                        }

                        if (response.body()?.message == "登录信息有误") {
                            userPreferencesRepository.apply {
                                setUid(EMPTY_STRING)
                                setUserAvatar(EMPTY_STRING)
                                setUsername(EMPTY_STRING)
                                setToken(EMPTY_STRING)
                                setIsLogin(false)
                            }
                        }

                        try {
                            val session = response.headers().values("Set-Cookie")[0]
                            SESSID = session.substring(0, session.indexOf(";"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
        }
    }

    fun resetBadge() {
        badge = 0
    }

}