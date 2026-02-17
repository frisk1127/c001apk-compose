package com.example.c001apk.compose.logic.repository

import com.example.c001apk.compose.constant.Constants.LOADING_FAILED
import com.example.c001apk.compose.di.AccountService
import com.example.c001apk.compose.di.Api1Service
import com.example.c001apk.compose.di.Api1ServiceNoRedirect
import com.example.c001apk.compose.di.Api2Service
import com.example.c001apk.compose.logic.model.FeedContentResponse
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.network.ApiService
import com.example.c001apk.compose.logic.state.LoadingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Created by bggRGjQaUbCoE on 2024/6/3
 */
@Singleton
class NetworkRepo @Inject constructor(
    @param:Api1Service
    private val apiService: ApiService,
    @param:Api1ServiceNoRedirect
    private val apiServiceNoRedirect: ApiService,
    @param:Api2Service
    private val api2Service: ApiService,
    @param:AccountService
    private val accountService: ApiService,
) {

    suspend fun getHomeFeed(
        page: Int,
        firstLaunch: Int,
        installTime: String,
        firstItem: String?,
        lastItem: String?
    ) = flowList {
        api2Service.getHomeFeed(page, firstLaunch, installTime, firstItem, lastItem).await()
    }

    suspend fun getFeedContent(url: String) = flowData {
        apiService.getFeedContent(url).await()
    }

    suspend fun getFeedContentReply(
        id: String,
        listType: String,
        page: Int,
        firstItem: String?,
        lastItem: String?,
        discussMode: Int,
        feedType: String,
        blockStatus: Int,
        fromFeedAuthor: Int
    ) = flowList {
        api2Service.getFeedContentReply(
            id,
            listType,
            page,
            firstItem,
            lastItem,
            discussMode,
            feedType,
            blockStatus,
            fromFeedAuthor
        ).await()
    }

    suspend fun getSearch(
        type: String, feedType: String, sort: String, keyWord: String, pageType: String?,
        pageParam: String?, page: Int, lastItem: String?
    ) = flowList {
        apiService.getSearch(
            type, feedType, sort, keyWord, pageType, pageParam, page, lastItem
        ).await()
    }

    suspend fun getReply2Reply(id: String, page: Int, lastItem: String?) = flowList {
        apiService.getReply2Reply(id, page, lastItem).await()
    }

    suspend fun getTopicLayout(url: String, tag: String?, id: String?) = flowData {
        api2Service.getTopicLayout(url, tag, id).await()
    }

    suspend fun getProductLayout(id: String) = fire {
        Result.success(apiService.getProductLayout(id).await())
    }

    suspend fun getUserSpace(uid: String) = flowData {
        apiService.getUserSpace(uid).await()
    }

    suspend fun getUserFeed(uid: String, page: Int, lastItem: String?) = flowList {
        apiService.getUserFeed(uid, page, lastItem).await()
    }

    suspend fun getAppInfo(id: String) = flowData {
        apiService.getAppInfo(id).await()
    }

    suspend fun getAppDownloadLink(pn: String, aid: String, vc: String) = fire {
        val appResponse = apiServiceNoRedirect.getAppDownloadLink(pn, aid, vc).response()
        Result.success(appResponse.headers()["Location"])
    }

    suspend fun getAppsUpdate(pkgs: String) = flowUpdateList {
        val multipartBody = MultipartBody.Part.createFormData("pkgs", pkgs)
        apiService.getAppsUpdate(multipartBody).await()
    }

    suspend fun getProfile(uid: String) = fire {
        Result.success(api2Service.getProfile(uid).await())
    }

    suspend fun getFollowList(
        url: String,
        uid: String?,
        id: String?,
        showDefault: Int?,
        page: Int,
        lastItem: String?
    ) = flowList {
        apiService.getFollowList(url, uid, id, showDefault, page, lastItem).await()
    }

    suspend fun postLike(url: String, id: String) = fire {
        Result.success(apiService.postLike(url, id).await())
    }

    suspend fun checkLoginInfo() = fire {
        Result.success(apiService.checkLoginInfo().response())
    }

    suspend fun getLoginParam(url: String) = fire {
        Result.success(accountService.getLoginParam(url).response())
    }

    suspend fun tryLogin(data: HashMap<String, String?>) = fire {
        Result.success(accountService.tryLogin(data).response())
    }

    suspend fun getCaptcha(url: String) = fire {
        Result.success(accountService.getCaptcha(url).response())
    }

    suspend fun getValidateCaptcha(url: String) = fire {
        Result.success(apiService.getValidateCaptcha(url).response())
    }

    suspend fun postReply(data: HashMap<String, String>, id: String, type: String) = fire {
        Result.success(apiService.postReply(data, id, type).await())
    }

    suspend fun getDataList(
        url: String, title: String, subTitle: String?, lastItem: String?, page: Int
    ) = flowList {
        api2Service.getDataList(url, title, subTitle, lastItem, page).await()
    }

    suspend fun getDyhDetail(dyhId: String, type: String, page: Int, lastItem: String?) =
        flowList {
            apiService.getDyhDetail(dyhId, type, page, lastItem).await()
        }

    suspend fun getCoolPic(tag: String, type: String, page: Int, lastItem: String?) =
        flowList {
            apiService.getCoolPic(tag, type, page, lastItem).await()
        }

    suspend fun getMessage(url: String, page: Int, lastItem: String?) = flowList {
        apiService.getMessage(url, page, lastItem).await()
    }

    suspend fun postFollowUnFollow(url: String, uid: String) = fire {
        Result.success(apiService.postFollowUnFollow(url, uid).await())
    }

    suspend fun postCreateFeed(data: HashMap<String, String>) = fire {
        Result.success(apiService.postCreateFeed(data).await())
    }

    suspend fun postRequestValidate(data: HashMap<String, String?>) = fire {
        Result.success(apiService.postRequestValidate(data).await())
    }

    suspend fun getVoteComment(
        fid: String,
        extraKey: String,
        page: Int,
        firstItem: String?,
        lastItem: String?,
    ) = fire {
        Result.success(apiService.getVoteComment(fid, extraKey, page, firstItem, lastItem).await())
    }

    suspend fun getAnswerList(
        id: String,
        sort: String,
        page: Int,
        firstItem: String?,
        lastItem: String?,
    ) = fire {
        Result.success(apiService.getAnswerList(id, sort, page, firstItem, lastItem).await())
    }

    suspend fun getProductList(url: String) = flowList {
        apiService.getProductList(url).await()
    }

    suspend fun getCollectionList(
        url: String,
        uid: String?,
        id: String?,
        showDefault: Int,
        page: Int,
        lastItem: String?
    ) = fire {
        Result.success(
            apiService.getCollectionList(url, uid, id, showDefault, page, lastItem).await()
        )
    }

    suspend fun postDelete(url: String, id: String) = fire {
        Result.success(apiService.postDelete(url, id).await())
    }

    suspend fun postFollow(data: HashMap<String, String>) = fire {
        Result.success(apiService.postFollow(data).await())
    }

    suspend fun getFollow(url: String, tag: String?, id: String?) = fire {
        Result.success(apiService.getFollow(url, tag, id).await())
    }

    suspend fun postOSSUploadPrepare(data: HashMap<String, String>) = fire {
        Result.success(apiService.postOSSUploadPrepare(data).await())
    }

    suspend fun getSearchTag(
        query: String,
        page: Int,
        recentIds: String?,
        firstItem: String?,
        lastItem: String?,
    ) = fire {
        Result.success(apiService.getSearchTag(query, page, recentIds, firstItem, lastItem).await())
    }

    suspend fun loadShareUrl(url: String) = fire {
        Result.success(apiService.loadShareUrl(url).await())
    }

    suspend fun messageOperation(
        url: String,
        ukey: String?,
        uid: String?,
        page: Int?,
        firstItem: String?,
        lastItem: String?,
    ) = flowList {
        apiService.messageOperation(url, ukey, uid, page, firstItem, lastItem).await()
    }

    suspend fun sendMessage(uid: String, message: MultipartBody.Part, pic: MultipartBody.Part) =
        fire {
            Result.success(apiService.sendMessage(uid, message, pic).await())
        }

    suspend fun deleteMessage(url: String, ukey: String, id: String?) = fire {
        Result.success(apiService.deleteMessage(url, ukey, id).await())
    }

    suspend fun getImageUrl(id: String) = fire {
        val response = apiServiceNoRedirect.getImageUrl(id).response()
        Result.success(response.headers()["Location"])
    }

    suspend fun checkCount() = fire {
        Result.success(apiService.checkCount().await())
    }


    suspend fun postDelete(url: String, data: Map<String, String>?) = fire {
        Result.success(apiService.postDelete(url, data).await())
    }

    private suspend fun <T> Call<T>.await(): T {
        return suspendCoroutine { continuation ->
            enqueue(object : Callback<T> {
                override fun onResponse(call: Call<T>, response: Response<T>) {
                    val body = response.body()
                    if (body != null) continuation.resume(body)
                    else continuation.resumeWithException(
                        RuntimeException("response body is null")
                    )
                }

                override fun onFailure(call: Call<T>, t: Throwable) {
                    continuation.resumeWithException(t)
                }
            })
        }
    }

    private suspend fun <T> Call<T>.response(): Response<T> {
        return suspendCoroutine { continuation ->
            enqueue(object : Callback<T> {
                override fun onResponse(call: Call<T>, response: Response<T>) {
                    continuation.resume(response)
                }

                override fun onFailure(call: Call<T>, t: Throwable) {
                    continuation.resumeWithException(t)
                }
            })
        }
    }

    private fun <T> fire(block: suspend () -> Result<T>) = flow {
        val result = try {
            block()
        } catch (e: Exception) {
            Result.failure(e)
        }
        emit(result)
    }.flowOn(Dispatchers.IO)

    private fun flowList(block: suspend () -> HomeFeedResponse) = flow {
        val result = try {
            val response = block()
            if (!response.message.isNullOrEmpty()) {
                LoadingState.Error(mapServerMessage(response.message))
            } else if (!response.data.isNullOrEmpty()) {
                LoadingState.Success(response.data)
            } else if (response.data?.isEmpty() == true) {
                LoadingState.Empty
            } else {
                LoadingState.Error(LOADING_FAILED)
            }
        } catch (e: Exception) {
            LoadingState.Error(mapErrorMessage(e))
        }
        emit(result)
    }.flowOn(Dispatchers.IO)

    private fun flowUpdateList(block: suspend () -> List<HomeFeedResponse.Data>) = flow {
        val result = try {
            val response = block()
            if (response.isNotEmpty()) {
                LoadingState.Success(response)
            } else {
                LoadingState.Empty
            }
        } catch (e: Exception) {
            LoadingState.Error(mapErrorMessage(e))
        }
        emit(result)
    }.flowOn(Dispatchers.IO)

    private fun flowData(block: suspend () -> FeedContentResponse) = flow {
        val result = try {
            val response = block()
            if (!response.message.isNullOrEmpty()) {
                LoadingState.Error(mapServerMessage(response.message))
            } else if (response.data != null) {
                LoadingState.Success(response.data)
            } else {
                LoadingState.Error(LOADING_FAILED)
            }
        } catch (e: Exception) {
            LoadingState.Error(mapErrorMessage(e))
        }
        emit(result)
    }.flowOn(Dispatchers.IO)

    private fun mapServerMessage(message: String?): String {
        val normalized = message?.trim().orEmpty()
        if (normalized.equals("timeout", ignoreCase = true)) {
            return "超时"
        }
        return normalized.ifEmpty { LOADING_FAILED }
    }

    private fun mapErrorMessage(e: Exception): String {
        if (e is java.net.SocketTimeoutException) {
            return "超时"
        }
        val message = e.message?.trim().orEmpty()
        if (message.contains("timeout", ignoreCase = true)) {
            return "超时"
        }
        return message.ifEmpty { "unknown error" }
    }

}
