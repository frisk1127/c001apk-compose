package com.example.c001apk.compose.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.c001apk.compose.logic.repository.BlackListRepo
import com.example.c001apk.compose.logic.repository.HistoryFavoriteRepo
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by bggRGjQaUbCoE on 2024/6/17
 */
@HiltViewModel(assistedFactory = HistoryViewModel.ViewModelFactory::class)
class HistoryViewModel @AssistedInject constructor(
    @Assisted val type: HistoryType = HistoryType.HISTORY,
    private val blackListRepo: BlackListRepo,
    private val historyFavoriteRepo: HistoryFavoriteRepo
) : ViewModel() {

    @AssistedFactory
    interface ViewModelFactory {
        fun create(type: HistoryType): HistoryViewModel
    }

    val dataList = when (type) {
        HistoryType.FAV -> historyFavoriteRepo.loadAllFavoriteListFlow()
        HistoryType.HISTORY -> historyFavoriteRepo.loadAllHistoryListFlow()
    }

    var toastText by mutableStateOf<String?>(null)
        private set

    fun resetToastText() {
        toastText = null
    }

    fun blockUser(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!blackListRepo.checkUid(uid)) {
                blackListRepo.saveUid(uid)
            }
            when (type) {
                HistoryType.FAV -> historyFavoriteRepo.deleteFavByUid(uid)
                HistoryType.HISTORY -> historyFavoriteRepo.deleteHistoryByUid(uid)
            }
            toastText = "已拉黑并移除相关记录"
        }
    }

    fun delete(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when (type) {
                HistoryType.FAV -> historyFavoriteRepo.deleteFavorite(id)
                HistoryType.HISTORY -> historyFavoriteRepo.deleteHistory(id)
            }
            toastText = "删除成功"
        }
    }

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            when (type) {
                HistoryType.FAV -> historyFavoriteRepo.deleteAllFavorite()
                HistoryType.HISTORY -> historyFavoriteRepo.deleteAllHistory()
            }
            toastText = "清除成功"
        }
    }

}
