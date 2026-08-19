package com.example.c001apk.compose.ui.base

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.constant.Constants.entityTemplateList
import com.example.c001apk.compose.constant.Constants.entityTypeList
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.repository.BlackListRepo
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.logic.state.FooterState
import com.example.c001apk.compose.logic.state.LoadingState
import com.example.c001apk.compose.util.CookieUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Created by bggRGjQaUbCoE on 2024/6/10
 */

enum class LikeType {
    FEED, REPLY
}

abstract class BaseViewModel(
    val networkRepo: NetworkRepo,
    val blackListRepo: BlackListRepo,
) : ViewModel() {

    var isRefreshing by mutableStateOf(false)

    var loadingState by mutableStateOf<LoadingState<List<HomeFeedResponse.Data>>>(LoadingState.Loading)

    var footerState by mutableStateOf<FooterState>(FooterState.Success)

    var page = 1
    var firstLaunch = 1
    var isLoadMore = false
    var isEnd = false
    var firstItem: String? = null
    var lastItem: String? = null

    abstract suspend fun customFetchData(): Flow<LoadingState<List<HomeFeedResponse.Data>>>

    fun fetchData() {
        viewModelScope.launch(Dispatchers.IO) {
            customFetchData().collect { state ->
                when (state) {
                    LoadingState.Empty -> {
                        if (loadingState is LoadingState.Success && !isRefreshing)
                            footerState = FooterState.End
                        else {
                            loadingState = state
                            footerState = FooterState.Success
                        }
                        isEnd = true
                    }

                    is LoadingState.Error -> {
                        if (loadingState is LoadingState.Success)
                            footerState = FooterState.Error(state.errMsg)
                        else
                            loadingState = state
                        isEnd = true
                    }

                    LoadingState.Loading -> {
                        if (loadingState is LoadingState.Success)
                            footerState = FooterState.Loading
                        else
                            loadingState = state
                    }

                    is LoadingState.Success -> {
                        page++
                        var response = state.response.filter {
                            ((it.entityType in entityTypeList
                                    || it.entityTemplate in
                                    if (CookieUtil.showSquare) entityTemplateList
                                    else entityTemplateList.toMutableList()
                                        .also { list ->
                                            list.removeAll(
                                                listOf(
                                                    "iconMiniScrollCard",
                                                    "iconMiniGridCard"
                                                )
                                            )
                                        })
                                    || isAdditionalItemSupported(it))
                                    && !blackListRepo.checkUid(
                                if (!it.fromuid.isNullOrEmpty()) it.fromuid
                                else it.uid.orEmpty()
                            )
                                    && !blackListRepo.checkTopic(
                                it.tags + it.ttitle +
                                        it.relationRows?.getOrNull(0)?.title
                            )
                        }
                        firstItem = response.firstOrNull()?.id
                        lastItem = response.lastOrNull()?.id

                        handleResponse(response)?.let {
                            response = it
                        }

                        if (isLoadMore) {
                            response =
                                ((loadingState as? LoadingState.Success)?.response ?: emptyList()) +
                                        response
                        }

                        handleLoadMore(response)?.let {
                            response = it
                        }

                        loadingState = LoadingState.Success(response)
                        footerState = FooterState.Success
                        if (response.isEmpty()) {
                            isLoadMore = false
                            isRefreshing = false
                            loadMore()
                        }
                    }
                }
                isLoadMore = false
                isRefreshing = false
            }
        }
    }

    open fun handleResponse(response: List<HomeFeedResponse.Data>): List<HomeFeedResponse.Data>? {
        return null
    }

    open fun isAdditionalItemSupported(item: HomeFeedResponse.Data): Boolean = false

    open fun handleLoadMore(response: List<HomeFeedResponse.Data>): List<HomeFeedResponse.Data>? {
        return null
    }

    open fun refresh() {
        if (!isRefreshing && !isLoadMore) {
            page = 1
            isEnd = false
            isLoadMore = false
            isRefreshing = true
            firstItem = null
            lastItem = null
            fetchData()
        }
    }

    open fun loadMore() {
        if (!isRefreshing && !isLoadMore) {
            isEnd = false
            isLoadMore = true
            fetchData()
            if (loadingState is LoadingState.Success) {
                footerState = FooterState.Loading
            } else {
                loadingState = LoadingState.Loading
            }
        }
    }

    var toastText by mutableStateOf<String?>(null)

    fun resetToastText() {
        toastText = null
    }

    fun onLike(id: String, like: Int, likeType: LikeType) {
        val isLike = when (likeType) {
            LikeType.FEED -> if (like == 1) "unlike" else "like"
            LikeType.REPLY -> if (like == 1) "unLikeReply" else "likeReply"
        }
        val likeUrl = "/v6/feed/$isLike"
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.postLike(likeUrl, id)
                .collect { result ->
                    val response = result.getOrNull()
                    if (response != null) {
                        if (!response.message.isNullOrEmpty()) {
                            toastText = response.message
                        } else if (response.data != null) {
                            if (handleLikeResponse(id, like, response.data.count) == null) {
                                val dataList = (loadingState as LoadingState.Success).response.map {
                                    if (it.id == id) {
                                        it.copy(
                                            likenum = response.data.count,
                                            userAction = it.userAction?.copy(like = if (like == 1) 0 else 1)
                                        )
                                    } else it
                                }
                                loadingState = LoadingState.Success(dataList)
                            }
                        }
                    } else {
                        toastText = result.exceptionOrNull()?.message ?: "response is null"
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
        }
    }

    open fun handleLikeResponse(id: String, like: Int, count: String?): Boolean? {
        return null
    }

    fun onDelete(id: String, deleteType: LikeType) {
        val url = if (deleteType == LikeType.FEED) "/v6/feed/deleteFeed"
        else "/v6/feed/deleteReply"
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.postDelete(url, id)
                .collect { result ->
                    val data = result.getOrNull()
                    if (data != null) {
                        if (!data.message.isNullOrEmpty()) {
                            toastText = data.message
                        } else if (data.data?.count == "删除成功") {
                            var response = (loadingState as LoadingState.Success).response
                            handleDeleteResponse(id, response)?.let {
                                response = it
                                loadingState = LoadingState.Success(response)
                            }
                            toastText = data.data.count
                        }
                    } else {
                        toastText = result.exceptionOrNull()?.message ?: "response is null"
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
        }
    }

    open fun handleDeleteResponse(
        id: String,
        response: List<HomeFeedResponse.Data>
    ): List<HomeFeedResponse.Data>? {
        return response.filterNot { it.id == id }
    }

    open fun onBlockUser(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            blackListRepo.saveUid(uid)
            toastText = "拉黑成功"

            if (loadingState is LoadingState.Success) {
                var response =
                    (loadingState as LoadingState.Success).response.filterNot { it.uid == uid }
                handleBlockUser(uid, response)?.let {
                    response = it
                }
                loadingState = LoadingState.Success(response)
            }
        }
    }

    open fun handleBlockUser(
        uid: String,
        response: List<HomeFeedResponse.Data>
    ): List<HomeFeedResponse.Data>? {
        return null
    }

    fun onFollowUser(uid: String, isFollow: Int) {
        val url = if (isFollow == 1) "/v6/user/unfollow" else "/v6/user/follow"
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.postFollowUnFollow(url, uid)
                .collect { result ->
                    val response = result.getOrNull()
                    if (response != null) {
                        if (!response.message.isNullOrEmpty()) {
                            toastText = response.message
                        } else {
                            val follow = if (isFollow == 1) 0 else 1
                            if (handleFollowResponse(follow) == null) {
                                val dataList = (loadingState as LoadingState.Success).response.map {
                                    if (it.uid == uid)
                                        it.copy(isFollow = follow)
                                    else it
                                }
                                loadingState = LoadingState.Success(dataList)
                            }
                            toastText = if (follow == 1) "关注成功"
                            else "取消关注成功"
                        }
                    } else {
                        toastText = result.exceptionOrNull()?.message ?: "response is null"
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
        }
    }

    open fun handleFollowResponse(follow: Int): Boolean? {
        return null
    }

}
