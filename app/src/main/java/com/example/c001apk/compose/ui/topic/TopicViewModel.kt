package com.example.c001apk.compose.ui.topic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 * Created by bggRGjQaUbCoE on 2024/6/9
 */
@HiltViewModel(assistedFactory = TopicViewModel.ViewModelFactory::class)
class TopicViewModel @AssistedInject constructor(
    @Assisted("url") val url: String,
    @Assisted("tag") val tag: String?,
    @Assisted("id") var id: String?,
    private val networkRepo: NetworkRepo,
    private val blackListRepo: BlackListRepo,
) : ViewModel() {

    @AssistedFactory
    interface ViewModelFactory {
        fun create(
            @Assisted("url") url: String,
            @Assisted("tag") tag: String?,
            @Assisted("id") id: String?,
        ): TopicViewModel
    }

    var topicState by mutableStateOf<LoadingState<HomeFeedResponse.Data>>(LoadingState.Loading)
        private set

    init {
        fetchTopicLayout()
    }

    lateinit var entityType: String
    lateinit var title: String
    var selectedTab: String? = null
    var tabList: List<HomeFeedResponse.TabList>? = null
    var productConfigRows: List<HomeFeedResponse.ProductConfig> = emptyList()
    var productLogo: String? = null

    var isFollowed by mutableStateOf(false)
    var isBlocked by mutableStateOf(false)
        private set

    private fun fetchTopicLayout() {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.getTopicLayout(url, tag, id)
                .collect { state ->
                    if (state is LoadingState.Success) {
                        val response = state.response
                        id = response.id
                        entityType = response.entityType.orEmpty()
                        title = response.title.orEmpty()
                        tabList = response.tabList
                        productConfigRows = response.configRows.orEmpty()
                        productLogo = response.logo
                        selectedTab = response.selectedTab
                        isFollowed = response.userAction?.follow == 1
                        checkIsBlocked(title)
                    }
                    topicState = state
                }
        }
    }

    fun refresh() {
        topicState = LoadingState.Loading
        fetchTopicLayout()
    }

    var toastText by mutableStateOf<String?>(null)
        private set

    fun resetToastText() {
        toastText = null
    }

    fun onGetFollow() {
        val followUrl = if (isFollowed) "/v6/feed/unFollowTag"
        else "/v6/feed/followTag"
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.getFollow(followUrl, title, null)
                .collect { result ->
                    val response = result.getOrNull()
                    if (response != null) {
                        if (!response.message.isNullOrEmpty()) {
                            if (response.message.contains("关注成功"))
                                isFollowed = !isFollowed
                            toastText = response.message
                        }
                    } else {
                        toastText = result.exceptionOrNull()?.message ?: "response is null"
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
        }
    }

    private var postFollowData: HashMap<String, String>? = null
    fun onPostFollow() {
        if (postFollowData.isNullOrEmpty()) postFollowData = HashMap()
        postFollowData?.let { map ->
            map["id"] = id.orEmpty()
            map["status"] = if (isFollowed) "0" else "1"
        }
        viewModelScope.launch(Dispatchers.IO) {
            postFollowData?.let {
                networkRepo.postFollow(it)
                    .collect { result ->
                        val response = result.getOrNull()
                        if (response != null) {
                            if (!response.message.isNullOrEmpty()) {
                                if (response.message.contains("成功"))
                                    isFollowed = !isFollowed
                                toastText = response.message
                            }
                        } else {
                            toastText = result.exceptionOrNull()?.message ?: "response is null"
                            result.exceptionOrNull()?.printStackTrace()
                        }
                    }
            }

        }
    }

    fun blockTopic() {
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
