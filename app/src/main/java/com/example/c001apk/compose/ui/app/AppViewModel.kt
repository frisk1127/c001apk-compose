package com.example.c001apk.compose.ui.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.repository.BlackListRepo
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.logic.state.LoadingState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by bggRGjQaUbCoE on 2024/6/10
 */
@HiltViewModel(assistedFactory = AppViewModel.ViewModelFactory::class)
class AppViewModel @AssistedInject constructor(
    @Assisted var packageName: String,
    private val networkRepo: NetworkRepo,
    private val blackListRepo: BlackListRepo,
) : ViewModel() {

    @AssistedFactory
    interface ViewModelFactory {
        fun create(packageName: String): AppViewModel
    }

    lateinit var id: String
    var title: String = EMPTY_STRING
    lateinit var versionName: String
    lateinit var versionCode: String
    lateinit var commentStatusText: String
    var commentStatus: Int = -1

    var isFollowed by mutableStateOf(false)
    var isBlocked by mutableStateOf(false)
        private set

    var appState by mutableStateOf<LoadingState<HomeFeedResponse.Data>>(LoadingState.Loading)
        private set

    init {
        if (packageName.isNotEmpty()) {
            fetchAppInfo()
        }
    }

    private fun fetchAppInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.getAppInfo(packageName)
                .collect { state ->
                    if (state is LoadingState.Success) {
                        val response = state.response
                        id = response.id.orEmpty()
                        title = response.title.orEmpty()
                        versionName = response.apkversionname.orEmpty()
                        versionCode = response.apkversioncode.orEmpty()
                        commentStatus = response.commentStatus ?: -1
                        commentStatusText = response.commentStatusText.orEmpty()
                        isFollowed = response.userAction?.follow == 1
                        checkIsBlocked(response.title.orEmpty())
                    }
                    appState = state
                }
        }
    }

    fun refresh() {
        appState = LoadingState.Loading
        fetchAppInfo()
    }

    var downloadApk by mutableStateOf(false)
        private set
    lateinit var downloadUrl: String
    fun onGetDownloadLink() {
        if (::downloadUrl.isInitialized) {
            downloadApk = true
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                networkRepo.getAppDownloadLink(packageName, id, versionCode)
                    .collect { result ->
                        val link = result.getOrNull()
                        if (link != null) {
                            downloadUrl = link
                            downloadApk = true
                        } else {
                            toastText =
                                result.exceptionOrNull()?.message ?: "failed to get download url"
                            result.exceptionOrNull()?.printStackTrace()
                        }
                    }
            }
        }
    }

    fun reset() {
        downloadApk = false
    }

    var updateState by mutableStateOf<LoadingState<List<HomeFeedResponse.Data>>>(LoadingState.Loading)
        private set

    fun fetchAppsUpdate(pkg: String) {
        viewModelScope.launch(Dispatchers.IO) {
            updateState = LoadingState.Loading
            networkRepo.getAppsUpdate(pkg)
                .collect { state ->
                    updateState = state
                }
        }
    }

    var toastText by mutableStateOf<String?>(null)
        private set

    fun resetToastText() {
        toastText = null
    }

    fun onGetFollowApk() {
        val followUrl = if (isFollowed) "/v6/apk/unFollow"
        else "/v6/apk/follow"
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.getFollow(followUrl, null, id)
                .collect { result ->
                    val response = result.getOrNull()
                    if (response != null) {
                        if (!response.message.isNullOrEmpty()) {
                            toastText = response.message
                        } else if (response.data?.follow != null) {
                            toastText = if (isFollowed) "取消关注成功"
                            else "关注成功"
                            isFollowed = !isFollowed
                        }
                    } else {
                        toastText = result.exceptionOrNull()?.message ?: "response is null"
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
        }
    }

    fun blockApp() {
        viewModelScope.launch(Dispatchers.IO) {
            if (isBlocked) {
                blackListRepo.deleteTopic(title)
                toastText = "取消拉黑成功"
            } else {
                blackListRepo.saveTopic(title)
                toastText = "拉黑成功"
            }
            isBlocked = !isBlocked
        }
    }

    private fun checkIsBlocked(title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            isBlocked = blackListRepo.checkTopic(title)
        }
    }

}
