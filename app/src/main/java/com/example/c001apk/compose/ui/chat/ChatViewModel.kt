package com.example.c001apk.compose.ui.chat

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.model.OSSUploadPrepareModel
import com.example.c001apk.compose.logic.model.OSSUploadPrepareResponse
import com.example.c001apk.compose.logic.model.StringEntity
import com.example.c001apk.compose.logic.repository.BlackListRepo
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.logic.repository.RecentEmojiRepo
import com.example.c001apk.compose.logic.state.LoadingState
import com.example.c001apk.compose.ui.base.BaseViewModel
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

/**
 * Created by bggRGjQaUbCoE on 2024/6/19
 */
@HiltViewModel(assistedFactory = ChatViewModel.ViewModelFactory::class)
class ChatViewModel @AssistedInject constructor(
    @Assisted val ukey: String,
    networkRepo: NetworkRepo,
    blackListRepo: BlackListRepo,
    private val recentEmojiRepo: RecentEmojiRepo
) : BaseViewModel(networkRepo, blackListRepo) {

    @AssistedFactory
    interface ViewModelFactory {
        fun create(ukey: String): ChatViewModel
    }

    init {
        fetchData()
    }

    override suspend fun customFetchData() =
        networkRepo.messageOperation("/v6/message/chat", ukey, null, page, firstItem, lastItem)

    override fun onBlockUser(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            blackListRepo.saveUid(uid)
            toastText = "拉黑成功"
        }
    }

    override fun handleResponse(response: List<HomeFeedResponse.Data>): List<HomeFeedResponse.Data> {
        return response.reversed()
    }

    fun onGetImageUrl(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.getImageUrl(id)
                .collect { result ->
                    val imageUrl = result.getOrNull()
                    if (!imageUrl.isNullOrEmpty()) {
                        var response = (loadingState as LoadingState.Success).response
                        response = response.map { item ->
                            if (item.id == id)
                                item.copy(messagePic = imageUrl)
                            else item
                        }
                        loadingState = LoadingState.Success(response)
                    } else {
                        toastText = result.exceptionOrNull()?.message ?: "response is null"
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
        }
    }

    fun onDeleteMsg() {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.deleteMessage("/v6/message/delete", ukey, deleteId)
                .collect { result ->
                    val data = result.getOrNull()
                    if (data != null) {
                        if (!data.message.isNullOrEmpty()) {
                            toastText = data.message
                        } else if (data.data?.count != null) {
                            var response = (loadingState as LoadingState.Success).response
                            response = response.filterNot { it.id == deleteId }
                            loadingState = LoadingState.Success(response)
                            toastText = data.data.count
                        }
                    } else {
                        toastText = result.exceptionOrNull()?.message ?: "response is null"
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
        }
    }

    lateinit var deleteId: String
    lateinit var message: String
    lateinit var pic: String
    var scroll by mutableStateOf(false)
        private set

    fun reset() {
        scroll = false
    }

    fun onSendMessage(uid: String, text: String, url: String) {
        showUploadDialog = true
        val message = MultipartBody.Part.createFormData("message", text)
        val pic = MultipartBody.Part.createFormData("message_pic", url)
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.sendMessage(uid, message, pic)
                .collect { result ->
                    val data = result.getOrNull()
                    if (data != null) {
                        if (!data.message.isNullOrEmpty()) {
                            toastText = data.message
                        } else if (data.data != null) {
                            val response =
                                (loadingState as LoadingState.Success).response.toMutableList()
                                    .also {
                                        it.addAll(0, data.data)
                                    }
                            loadingState = LoadingState.Success(response)
                            scroll = true
                        }
                    } else {
                        toastText = result.exceptionOrNull()?.message ?: "failed to send message"
                    }
                    showUploadDialog = false
                }
        }
    }

    var uploadImage by mutableStateOf<OSSUploadPrepareResponse.Data?>(null)
        private set

    fun resetUploadImage() {
        uploadImage = null
    }

    lateinit var uriList: List<Uri>
    lateinit var typeList: List<String>
    lateinit var md5List: List<ByteArray?>
    var showUploadDialog by mutableStateOf(false)

    fun onPostOSSUploadPrepare(uid: String, imageList: List<OSSUploadPrepareModel>) {
        showUploadDialog = true
        val ossUploadPrepareData = hashMapOf(
            "uploadBucket" to "message",
            "uploadDir" to "message",
            "is_anonymous" to "0",
            "uploadFileList" to Gson().toJson(imageList),
            "toUid" to uid,
        )
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.postOSSUploadPrepare(ossUploadPrepareData)
                .collect { result ->
                    val data = result.getOrNull()
                    if (data != null) {
                        if (!data.message.isNullOrEmpty()) {
                            toastText = data.message
                            showUploadDialog = false
                        } else if (data.data != null) {
                            uploadImage = data.data
                        }
                    } else {
                        showUploadDialog = false
                        toastText = result.exceptionOrNull()?.message ?: "upload prepare failed"
                    }
                }
        }
    }

    val recentEmojiData = recentEmojiRepo.loadAllListFlow()

    fun updateRecentEmoji(data: String, size: Int, last: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (recentEmojiRepo.checkEmoji(data)) {
                recentEmojiRepo.updateEmoji(data)
            } else {
                if (size == 27)
                    last?.let {
                        recentEmojiRepo.updateEmoji(it, data)
                    }
                else
                    recentEmojiRepo.insertEmoji(StringEntity(data))
            }
        }
    }

}
