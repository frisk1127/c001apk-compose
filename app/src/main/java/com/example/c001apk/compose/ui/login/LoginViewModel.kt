package com.example.c001apk.compose.ui.login

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.logic.model.LoginResponse
import com.example.c001apk.compose.logic.repository.AccountRepository
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.logic.repository.UserPreferencesRepository
import com.example.c001apk.compose.util.CookieUtil.SESSID
import com.example.c001apk.compose.util.CookieUtil.isGetCaptcha
import com.example.c001apk.compose.util.CookieUtil.isGetLoginParam
import com.example.c001apk.compose.util.CookieUtil.isPreGetLoginParam
import com.example.c001apk.compose.util.CookieUtil.isTryLogin
import com.example.c001apk.compose.util.createRequestHash
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import javax.inject.Inject

/**
 * Created by bggRGjQaUbCoE on 2024/6/10
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val networkRepo: NetworkRepo,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    var requestHash by mutableStateOf(EMPTY_STRING)
    var captchaImg by mutableStateOf<Bitmap?>(null)
        private set
    var toastText by mutableStateOf<String?>(null)
        private set
    var isLoginSuccess by mutableStateOf(false)
        private set

    private val urlPreGetParam = "/auth/login?type=mobile"
    private val urlGetParam = "/auth/loginByCoolApk"

    init {
        isPreGetLoginParam = true
        viewModelScope.launch(Dispatchers.IO) {
            onGetLoginParam(urlPreGetParam)
        }
    }

    private suspend fun onGetLoginParam(url: String) {
        networkRepo.getLoginParam(url)
            .collect { result ->
                val response = result.getOrNull()
                if (response != null) {
                    if (url == urlGetParam) {
                        response.body()?.string()?.let {
                            requestHash = Jsoup.parse(it).createRequestHash()
                        }
                    }
                    try {
                        val session = response.headers().values("Set-Cookie")[0]
                        SESSID = session.substring(0, session.indexOf(";"))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        toastText = "无法获取Cookie"
                        return@collect
                    }
                    if (url == urlPreGetParam) {
                        isGetLoginParam = true
                        onGetLoginParam(urlGetParam)
                    }
                } else {
                    toastText = result.exceptionOrNull()?.message ?: "response is null"
                    result.exceptionOrNull()?.printStackTrace()
                }
            }
    }

    fun onGetCaptcha() {
        isGetCaptcha = true
        val timeStamp = System.currentTimeMillis().toString()
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.getCaptcha("/auth/showCaptchaImage?$timeStamp")
                .collect { result ->
                    val response = result.getOrNull()
                    if (response != null) {
                        captchaImg = BitmapFactory.decodeStream(response.body()?.byteStream())
                    } else {
                        toastText = result.exceptionOrNull()?.message ?: "response is null"
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
        }
    }

    var loginData = HashMap<String, String?>()
    fun onLogin() {
        isTryLogin = true
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.tryLogin(loginData)
                .collect { result ->
                    val response = result.getOrNull()
                    response?.body()?.let {
                        val login: LoginResponse = Gson().fromJson(
                            response.body()?.string(),
                            LoginResponse::class.java
                        )
                        if (login.status == 1) {
                            val cookies = response.headers().values("Set-Cookie")
                            val uid =
                                cookies.find { it.startsWith("uid=") }?.split(";")
                                    ?.getOrNull(0)
                                    ?.replace("uid=", EMPTY_STRING)?.trim()
                            val username =
                                cookies.find { it.startsWith("username=") }?.split(";")
                                    ?.getOrNull(0)?.replace("username=", EMPTY_STRING)?.trim()
                            val token =
                                cookies.findLast { it.startsWith("token=") }?.split(";")
                                    ?.getOrNull(0)
                                    ?.replace("token=", EMPTY_STRING)?.trim()
                            if (!uid.isNullOrEmpty() && !username.isNullOrEmpty() && !token.isNullOrEmpty()) {
                                userPreferencesRepository.apply {
                                    setIsLogin(true)
                                    setUid(uid)
                                    setUsername(username)
                                    setToken(token)
                                }
                                accountRepository.saveOrUpdateAccount(
                                    uid = uid,
                                    username = username,
                                    token = token
                                )
                                isLoginSuccess = true
                            }
                        } else {
                            login.message?.let {
                                toastText = login.message
                                when (login.message) {
                                    "图形验证码不能为空", "图形验证码错误" -> onGetCaptcha()

                                    "密码错误" -> if (captchaImg != null) onGetCaptcha()
                                }
                            }
                        }
                    }
                }
        }
    }

    fun reset() {
        toastText = null
    }

}