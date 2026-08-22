package com.example.c001apk.compose.ui.feed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.logic.model.FeedArticleContentBean
import com.example.c001apk.compose.logic.model.FeedEntity
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.repository.BlackListRepo
import com.example.c001apk.compose.logic.repository.HistoryFavoriteRepo
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.logic.state.FooterState
import com.example.c001apk.compose.logic.state.LoadingState
import com.example.c001apk.compose.ui.base.BaseViewModel
import com.example.c001apk.compose.ui.base.LikeType
import com.example.c001apk.compose.ui.history.HistoryType
import com.example.c001apk.compose.util.CookieUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by bggRGjQaUbCoE on 2024/6/6
 */
@HiltViewModel(assistedFactory = FeedViewModel.ViewModelFactory::class)
class FeedViewModel @AssistedInject constructor(
    @Assisted var id: String,
    @Assisted var isViewReply: Boolean,
    networkRepo: NetworkRepo,
    blackListRepo: BlackListRepo,
    private val historyFavoriteRepo: HistoryFavoriteRepo,
) : BaseViewModel(networkRepo, blackListRepo) {

    @AssistedFactory
    interface ViewModelFactory {
        fun create(id: String, isViewReply: Boolean): FeedViewModel
    }

    var feedState by mutableStateOf<LoadingState<HomeFeedResponse.Data>>(LoadingState.Loading)
        private set

    var itemSize = 2

    lateinit var replyType: String
    private var discussMode: Int = 1
    var listType: String = "lastupdate_desc"
    var feedType: String = "feed"
    private var blockStatus = 0
    var fromFeedAuthor = 0

    private var topId: String? = null
    private var meId: String? = null

    var articleList: List<FeedArticleContentBean>? = null

    init {
        fetchFeedData()
    }

    var replyCount by mutableStateOf("0")
    var isFav by mutableStateOf(false)
    var isBlocked by mutableStateOf(false)

    private suspend fun recordHistory(
        username: String,
        avatar: String,
        device: String,
        message: String,
        pubDate: String,
        type: HistoryType = HistoryType.HISTORY
    ) {
        when (type) {
            HistoryType.FAV -> historyFavoriteRepo.insertFavorite(
                FeedEntity(id, feedUid, username, avatar, device, message, pubDate)
            )

            HistoryType.HISTORY -> historyFavoriteRepo.insertHistory(
                FeedEntity(id, feedUid, username, avatar, device, message, pubDate)
            )
        }

    }

    private fun fetchFeedData() {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.getFeedContent("/v6/feed/detail?id=$id")
                .collect { state ->
                    if (state is LoadingState.Success) {
                        val response = state.response

                        feedUid = response.uid.orEmpty()
                        feedUsername = response.username.orEmpty()
                        feedAvatar = response.userInfo?.userAvatar ?: response.userAvatar.orEmpty()
                        replyCount = response.replynum.orEmpty()
                        feedTypeName = response.feedTypeName.orEmpty()
                        feedType = response.feedType.orEmpty()

                        if (response.messageRawOutput != "null") {
                            val listType =
                                object : TypeToken<List<FeedArticleContentBean?>?>() {}.type
                            val message: List<FeedArticleContentBean> =
                                Gson().fromJson(response.messageRawOutput, listType)

                            articleList = message.filter {
                                it.type in listOf("text", "image", "shareUrl")
                            }

                            if (!response.messageCover.isNullOrEmpty()) itemSize++
                            if (!response.title.isNullOrEmpty()) itemSize++
                            if (response.targetRow != null || !response.relationRows.isNullOrEmpty())
                                itemSize++
                            itemSize += articleList?.size ?: 0
                        }

                        if (!response.topReplyRows.isNullOrEmpty()) {
                            response.topReplyRows?.getOrNull(0)?.let { reply ->
                                topId = reply.id
                                val unameTag = when (reply.uid) {
                                    feedUid -> " [楼主]"
                                    else -> EMPTY_STRING
                                }
                                reply.username = "${reply.username}$unameTag [置顶]"
                                if (!reply.replyRows.isNullOrEmpty()) {
                                    reply.replyRows = reply.replyRows?.map {
                                        it.copy(
                                            message = generateMess(
                                                it,
                                                feedUid,
                                                reply.uid
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        if (!response.replyMeRows.isNullOrEmpty()) {
                            response.replyMeRows?.getOrNull(0)?.let { reply ->
                                meId = reply.id
                                if (!reply.replyRows.isNullOrEmpty()) {
                                    reply.replyRows = reply.replyRows?.map {
                                        it.copy(
                                            message = generateMess(
                                                it,
                                                feedUid,
                                                reply.uid
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        fetchData()

                        if (CookieUtil.recordHistory && !historyFavoriteRepo.checkHistory(id)) {
                            recordHistory(
                                username = response.username.orEmpty(),
                                avatar = response.userAvatar.orEmpty(),
                                device = response.deviceTitle.orEmpty(),
                                message = with(response.message.orEmpty()) {
                                    if (this.length > 150) this.substring(0, 150) else this
                                },
                                pubDate = response.dateline.toString()
                            )
                        }
                        isFav = historyFavoriteRepo.checkFavorite(id)
                        isBlocked = blackListRepo.checkUid(feedUid)
                    }
                    feedState = state
                    isRefreshing = false
                }
        }
    }

    lateinit var feedUid: String
    lateinit var feedUsername: String
    var feedAvatar: String = EMPTY_STRING
    var feedTypeName by mutableStateOf(EMPTY_STRING)

    override suspend fun customFetchData() =
        networkRepo.getFeedContentReply(
            id, listType, page, firstItem, lastItem, discussMode,
            feedType, blockStatus, fromFeedAuthor
        )

    override fun handleResponse(response: List<HomeFeedResponse.Data>): List<HomeFeedResponse.Data> {
        response.forEach { item ->
            item.username =
                "${item.username}${if (item.uid == feedUid) " [楼主]" else EMPTY_STRING}"
            if (!item.replyRows.isNullOrEmpty()) {
                item.replyRows = item.replyRows?.map {
                    it.copy(
                        message = generateMess(
                            it,
                            feedUid,
                            item.uid
                        )
                    )
                }
            }
        }

        return response.distinctBy { it.entityId }
            .filterNot {
                if (listType == "lastupdate_desc")
                    it.id in listOf(topId, meId)
                else false
            }
    }

    var isPull = false
    override fun refresh() {
        if (!isRefreshing && !isLoadMore) {
            if (feedState is LoadingState.Success) {
                resetParams()
                isRefreshing = true
                fetchData()
            } else {
                if (isPull) {
                    isPull = false
                    viewModelScope.launch {
                        isRefreshing = true
                        delay(50)
                        isRefreshing = false
                    }
                }
                feedState = LoadingState.Loading
                fetchFeedData()
            }
        }
    }

    private fun generateMess(
        reply: HomeFeedResponse.Data,
        feedUid: String?,
        uid: String?
    ): String = run {
        val replyTag =
            when (reply.uid) {
                feedUid -> " [楼主] "
                uid -> " [层主] "
                else -> EMPTY_STRING
            }

        val rReplyTag =
            when (reply.ruid) {
                feedUid -> " [楼主] "
                uid -> " [层主] "
                else -> EMPTY_STRING
            }

        val rReplyUser =
            when (reply.ruid) {
                uid -> EMPTY_STRING
                else -> """<a class="feed-link-uname" href="/u/${reply.ruid}">${reply.rusername}${rReplyTag}</a>"""
            }

        val replyPic =
            when (reply.pic) {
                EMPTY_STRING -> EMPTY_STRING
                else -> """ <a class=\"feed-forward-pic\" href=${reply.pic}>查看图片(${reply.picArr?.size})</a>"""
            }

        """<a class="feed-link-uname" href="/u/${reply.uid}">${reply.username}${replyTag}</a>回复${rReplyUser}: ${reply.message}${replyPic}"""

    }

    var isSheet: Boolean = false
    var frid: String? = null
    lateinit var replyId: String
    lateinit var replyUid: String
    lateinit var replyName: String
    var replyAvatar: String? = null
    private var replyPage = 1
    private var replyLastItem: String? = null
    var isEndReply = false
    private var isLoadMoreReply = false
    var replyLoadingState by mutableStateOf<LoadingState<List<HomeFeedResponse.Data>>>(LoadingState.Loading)
        private set
    var replyFooterState by mutableStateOf<FooterState>(FooterState.Success)
        private set

    private fun getReplyTop(): HomeFeedResponse.Data? {
        val replyTop =
            when (frid ?: replyId) {
                topId ->
                    (feedState as LoadingState.Success).response.topReplyRows?.getOrNull(0)

                meId ->
                    (feedState as LoadingState.Success).response.replyMeRows?.getOrNull(0)

                else ->
                    (loadingState as LoadingState.Success).response.find {
                        it.id == (frid ?: replyId)
                    }
            }
        return if (!frid.isNullOrEmpty())
            replyTop?.replyRows?.find { it.id == replyId }
        else replyTop
    }

    fun fetchTotalReply() {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.getReply2Reply(replyId, replyPage, replyLastItem)
                .collect { state ->
                    when (state) {
                        LoadingState.Empty -> {
                            if (replyLoadingState is LoadingState.Success && !isRefreshing)
                                replyFooterState = FooterState.End
                            else {
                                replyLoadingState = getReplyTop()?.let {
                                    LoadingState.Success(listOf(it))
                                } ?: state
                                replyFooterState = FooterState.End
                            }
                            isEndReply = true
                        }

                        is LoadingState.Error -> {
                            if (replyLoadingState is LoadingState.Success)
                                replyFooterState = FooterState.Error(state.errMsg)
                            else
                                replyLoadingState = state
                            isEndReply = true
                        }

                        LoadingState.Loading -> {
                            if (replyLoadingState is LoadingState.Success)
                                replyFooterState = FooterState.Loading
                            else
                                replyLoadingState = state
                        }

                        is LoadingState.Success -> {
                            replyPage++
                            var response = state.response.filter {
                                it.entityType == "feed_reply"
                            }
                            response = response.map { reply ->
                                reply.copy(
                                    username = generateName(reply, replyUid)
                                )
                            }
                            if (replyPage == 2) {
                                getReplyTop()?.let {
                                    response = listOf(it) + response
                                }
                            }
                            replyLastItem = response.lastOrNull()?.id
                            replyLoadingState =
                                if (isLoadMoreReply)
                                    LoadingState.Success(
                                        (((replyLoadingState as? LoadingState.Success)?.response
                                            ?: emptyList()) + response).distinctBy { it.entityId }
                                    )
                                else
                                    LoadingState.Success(response)
                            replyFooterState = FooterState.Success
                        }
                    }
                    isLoadMoreReply = false
                }
        }
    }

    private fun generateName(data: HomeFeedResponse.Data, uid: String): String = run {
        val replyTag =
            when (data.uid) {
                feedUid -> " [楼主] "
                uid -> " [层主] "
                else -> EMPTY_STRING
            }

        val rReplyTag =
            when (data.ruid) {
                feedUid -> " [楼主] "
                uid -> " [层主] "
                else -> EMPTY_STRING
            }

        if (data.ruid == "0")
            """<a class="feed-link-uname" href="/u/${data.uid}">${data.username}$replyTag</a>"""
        else
            """<a class="feed-link-uname" href="/u/${data.uid}">${data.username}$replyTag</a>回复<a class="feed-link-uname" href="/u/${data.rusername}">${data.rusername}$rReplyTag</a>"""
    }

    fun resetReplyState() {
        replyPage = 1
        isLoadMoreReply = false
        isEndReply = false
        replyLastItem = null
        frid = null
        replyLoadingState = LoadingState.Loading
        replyFooterState = FooterState.Success
    }

    fun loadMoreReply() {
        if (!isLoadMoreReply) {
            isEndReply = false
            isLoadMoreReply = true
            fetchTotalReply()
            if (replyLoadingState is LoadingState.Success) {
                replyFooterState = FooterState.Loading
            } else {
                replyLoadingState = LoadingState.Loading
            }
        }
    }

    override fun handleLikeResponse(id: String, like: Int, count: String?): Boolean? {
        return if (id in listOf(this.id, topId, meId)) {
            var response = (feedState as LoadingState.Success).response
            val isLike = if (like == 1) 0 else 1
            when (id) {
                this.id -> {
                    response = response.copy(
                        likenum = count,
                        userAction = HomeFeedResponse.UserAction(isLike)
                    )
                }

                topId -> {
                    response.topReplyRows?.getOrNull(0)?.let {
                        response = response.copy(
                            topReplyRows = listOf(
                                it.copy(
                                    likenum = count,
                                    userAction = HomeFeedResponse.UserAction(isLike)
                                )
                            )
                        )
                    }

                }

                meId -> {
                    response.replyMeRows?.getOrNull(0)?.let {
                        response = response.copy(
                            replyMeRows = listOf(
                                it.copy(
                                    likenum = count,
                                    userAction = HomeFeedResponse.UserAction(isLike)
                                )
                            )
                        )
                    }

                }
            }
            feedState = LoadingState.Success(response)
            true
        } else null
    }

    fun blockUser() {
        viewModelScope.launch(Dispatchers.IO) {
            if (isBlocked)
                blackListRepo.deleteUid(feedUid)
            else
                blackListRepo.saveUid(feedUid)
            isBlocked = !isBlocked
        }
    }

    fun onFav() {
        viewModelScope.launch(Dispatchers.IO) {
            if (isFav) {
                historyFavoriteRepo.deleteFavorite(id)
            } else {
                val response = (feedState as LoadingState.Success).response
                recordHistory(
                    username = response.username.orEmpty(),
                    avatar = response.userAvatar.orEmpty(),
                    device = response.deviceTitle.orEmpty(),
                    message = with(response.message.orEmpty()) {
                        if (this.length > 150) this.substring(0, 150) else this
                    },
                    pubDate = response.dateline.toString(),
                    type = HistoryType.FAV
                )
            }
            isFav = !isFav
        }
    }

    override fun handleLoadMore(response: List<HomeFeedResponse.Data>): List<HomeFeedResponse.Data> {
        return response.distinctBy { it.entityId }
    }

    override fun handleBlockUser(
        uid: String,
        response: List<HomeFeedResponse.Data>
    ): List<HomeFeedResponse.Data>? {
        var feedResponse = (feedState as LoadingState.Success).response
        feedResponse.topReplyRows?.getOrNull(0)?.let { reply ->
            feedResponse = if (reply.uid == uid)
                feedResponse.copy(topReplyRows = null)
            else
                feedResponse.copy(
                    topReplyRows = listOf(
                        reply.copy(
                            replyRows = reply.replyRows?.filterNot { it.uid == uid }
                        )
                    )
                )
        }
        feedResponse.replyMeRows?.getOrNull(0)?.let { reply ->
            feedResponse = if (reply.uid == uid)
                feedResponse.copy(replyMeRows = null)
            else
                feedResponse.copy(
                    replyMeRows = listOf(
                        reply.copy(
                            replyRows = reply.replyRows?.filterNot { it.uid == uid }
                        )
                    )
                )
        }
        feedState = LoadingState.Success(feedResponse)
        return if (frid.isNullOrEmpty()) null
        else response.map { item ->
            if (item.id == frid) {
                item.copy(replyRows = item.replyRows?.filterNot { it.uid == uid })
            } else item
        }
    }

    fun onBlockReplyUser(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            blackListRepo.saveUid(uid)

            if (replyLoadingState is LoadingState.Success) {
                val response =
                    (replyLoadingState as LoadingState.Success).response.filterNot { it.uid == uid }
                replyLoadingState = LoadingState.Success(response)
            }
        }
    }

    fun onDeleteRely(id: String, deleteType: LikeType) {
        val url = if (deleteType == LikeType.FEED) "/v6/feed/deleteFeed"
        else "/v6/feed/deleteReply"
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.postDelete(url, id)
                .collect { result ->
                    val response = result.getOrNull()
                    if (response != null) {
                        if (!response.message.isNullOrEmpty()) {
                            toastText = response.message
                        } else if (response.data?.count == "删除成功") {
                            toastText = response.data.count
                            val dataList =
                                (replyLoadingState as LoadingState.Success).response.filterNot { it.id == id }
                            replyLoadingState = LoadingState.Success(dataList)
                        }
                    } else {
                        toastText = result.exceptionOrNull()?.message ?: "response is null"
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
        }
    }

    fun onLikeReply(id: String, like: Int, likeType: LikeType) {
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
                        if (response.data != null) {
                            if (!response.message.isNullOrEmpty()) {
                                toastText = response.message
                            } else if (handleLikeResponse(id, like, response.data.count) == null) {
                                val dataList =
                                    (replyLoadingState as LoadingState.Success).response.map {
                                        if (it.id == id) {
                                            it.copy(
                                                likenum = response.data.count,
                                                userAction = it.userAction?.copy(like = if (like == 1) 0 else 1)
                                            )
                                        } else it
                                    }
                                replyLoadingState = LoadingState.Success(dataList)
                            }
                        }
                    } else {
                        toastText = result.exceptionOrNull()?.message ?: "response is null"
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
        }
    }

    fun updateReply(data: HomeFeedResponse.Data) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isSheet) {
                isSheet = false
                val reply = data.copy(username = generateName(data, replyUid))
                var response = (replyLoadingState as? LoadingState.Success)?.response ?: emptyList()
                val position = response.map { it.id }.indexOf(replyId)
                response = response.toMutableList().also {
                    it.add(position + 1, reply)
                }
                replyLoadingState = LoadingState.Success(response)
            } else {
                val reply = data.copy(message = generateMess(data, feedUid, replyUid))
                if ((frid ?: replyId) in listOf(topId, meId)) {
                    var feedResponse = (feedState as LoadingState.Success).response
                    if ((frid ?: replyId) == topId)
                        feedResponse.topReplyRows?.getOrNull(0)?.let {
                            feedResponse = feedResponse.copy(
                                topReplyRows = listOf(
                                    it.copy(
                                        replyRows = (it.replyRows ?: emptyList()) + listOf(reply)
                                    )
                                )
                            )
                        }
                    else
                        feedResponse.replyMeRows?.getOrNull(0)?.let {
                            feedResponse = feedResponse.copy(
                                replyMeRows = listOf(
                                    it.copy(
                                        replyRows = (it.replyRows ?: emptyList()) + listOf(reply)
                                    )
                                )
                            )
                        }
                    feedState = LoadingState.Success(feedResponse)
                } else {
                    var response = (loadingState as? LoadingState.Success)?.response ?: emptyList()
                    response = if (replyType == "feed") {
                        listOf(data) + response
                    } else {
                        response.map { item ->
                            if (item.id == (frid ?: replyId)) {
                                item.copy(
                                    replyRows = (item.replyRows ?: emptyList()) + listOf(reply)
                                )
                            } else item
                        }
                    }
                    loadingState = LoadingState.Success(response)
                }
            }
        }
    }

    override fun handleDeleteResponse(
        id: String,
        response: List<HomeFeedResponse.Data>
    ): List<HomeFeedResponse.Data>? {
        return if (!frid.isNullOrEmpty()) {
            when (frid) {
                topId, meId -> {
                    var feedResponse = (feedState as LoadingState.Success).response
                    if (frid == topId)
                        feedResponse.topReplyRows?.getOrNull(0)?.let { reply ->
                            feedResponse = feedResponse.copy(
                                topReplyRows = listOf(
                                    reply.copy(replyRows = reply.replyRows?.filterNot { it.id == id })
                                )
                            )
                        }
                    else
                        feedResponse.replyMeRows?.getOrNull(0)?.let { reply ->
                            feedResponse = feedResponse.copy(
                                replyMeRows = listOf(
                                    reply.copy(replyRows = reply.replyRows?.filterNot { it.id == id })
                                )
                            )
                        }
                    feedState = LoadingState.Success(feedResponse)
                    null
                }

                else -> {
                    response.map { item ->
                        if (item.id == frid)
                            item.copy(replyRows = item.replyRows?.filterNot { it.id == id })
                        else item
                    }
                }
            }
        } else {
            if (id in listOf(topId, meId)) {
                var feedResponse = (feedState as LoadingState.Success).response
                feedResponse = if (id == topId)
                    feedResponse.copy(topReplyRows = null)
                else
                    feedResponse.copy(replyMeRows = null)
                feedState = LoadingState.Success(feedResponse)
                null
            } else super.handleDeleteResponse(id, response)
        }
    }

    fun refresh(id: String, isViewReply: Boolean) {
        this.id = id
        this.isViewReply = isViewReply
        resetParams()
        isRefreshing = false
        articleList = null
        itemSize = 2
        resetState()
        refresh()
    }

    private fun resetParams() {
        page = 1
        isEnd = false
        isLoadMore = false
        firstItem = null
        lastItem = null
    }

    fun resetState() {
        feedTypeName = EMPTY_STRING
        feedState = LoadingState.Loading
        loadingState = LoadingState.Loading
    }

}