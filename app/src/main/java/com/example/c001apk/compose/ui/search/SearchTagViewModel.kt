package com.example.c001apk.compose.ui.search

import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.repository.BlackListRepo
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.logic.state.LoadingState
import com.example.c001apk.compose.ui.base.BaseViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 用于热门/搜索话题列表（/v6/feed/searchTag）
 */
@HiltViewModel(assistedFactory = SearchTagViewModel.ViewModelFactory::class)
class SearchTagViewModel @AssistedInject constructor(
    @Assisted("query") private val query: String,
    networkRepo: NetworkRepo,
    blackListRepo: BlackListRepo,
) : BaseViewModel(networkRepo, blackListRepo) {

    @AssistedFactory
    interface ViewModelFactory {
        fun create(@Assisted("query") query: String): SearchTagViewModel
    }

    override suspend fun customFetchData(): Flow<LoadingState<List<HomeFeedResponse.Data>>> {
        return networkRepo.getSearchTag(
            query = query,
            page = page,
            recentIds = null,
            firstItem = firstItem,
            lastItem = lastItem
        ).map { result ->
            val response = result.getOrNull()
            if (response == null) {
                LoadingState.Error(result.exceptionOrNull()?.message ?: "response is null")
            } else if (!response.message.isNullOrEmpty()) {
                LoadingState.Error(response.message)
            } else if (!response.data.isNullOrEmpty()) {
                LoadingState.Success(response.data)
            } else if (response.data?.isEmpty() == true) {
                LoadingState.Empty
            } else {
                LoadingState.Error("unknown error")
            }
        }
    }
}
