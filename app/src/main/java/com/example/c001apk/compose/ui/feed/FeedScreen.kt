package com.example.c001apk.compose.ui.feed

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring.StiffnessLow
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.c001apk.compose.R
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.state.LoadingState
import com.example.c001apk.compose.ui.base.LikeType
import com.example.c001apk.compose.ui.component.ArticleItem
import com.example.c001apk.compose.ui.component.BackButton
import com.example.c001apk.compose.ui.component.FooterCard
import com.example.c001apk.compose.ui.component.ItemCard
import com.example.c001apk.compose.ui.component.cards.FeedCard
import com.example.c001apk.compose.ui.component.cards.FeedHeader
import com.example.c001apk.compose.ui.component.cards.FeedReplyCard
import com.example.c001apk.compose.ui.component.cards.FeedReplySortCard
import com.example.c001apk.compose.ui.component.cards.LoadingCard
import com.example.c001apk.compose.ui.feed.reply.ReplyActivity
import com.example.c001apk.compose.util.CookieUtil.isLogin
import com.example.c001apk.compose.util.ReportType
import com.example.c001apk.compose.util.ShareType
import com.example.c001apk.compose.util.Utils.richToString
import com.example.c001apk.compose.util.copyText
import com.example.c001apk.compose.util.getAllLinkAndText
import com.example.c001apk.compose.util.getShareText
import com.example.c001apk.compose.util.isScrollingUp
import com.example.c001apk.compose.util.makeToast
import com.example.c001apk.compose.util.noRippleClickable
import com.example.c001apk.compose.util.shareText
import kotlinx.coroutines.launch

/**
 * Created by bggRGjQaUbCoE on 2024/6/4
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    modifier: Modifier = Modifier,
    isCompat: Boolean = true,
    onBackClick: () -> Unit,
    id: String,
    isViewReply: Boolean,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    onReport: (String, ReportType) -> Unit,
) {

    val viewModel =
        hiltViewModel<FeedViewModel, FeedViewModel.ViewModelFactory>(key = if (isCompat) id else "KEY") { factory ->
            factory.create(id, isViewReply)
        }


    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val state = rememberPullToRefreshState()
    val lazyListState = rememberLazyListState()
    var dropdownMenuExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(true)
    var openBottomSheet by remember { mutableStateOf(false) }
    var viewReply by remember { mutableStateOf(false) }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val shouldShowSortCard by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > viewModel.itemSize - 1 }
    }
    val shouldShowTopCard by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
    }

    fun onShowTotalReply(id: String, uid: String, frid: String?) {
        openBottomSheet = true
        viewModel.replyId = id
        viewModel.replyUid = uid
        viewModel.frid = frid
        viewModel.resetReplyState()
        viewModel.fetchTotalReply()
    }

    fun setReplyType(index: Int) {
        selected = index
        viewModel.listType = when (index) {
            0 -> "lastupdate_desc"
            1 -> "dateline_desc"
            2 -> "popular"
            else -> EMPTY_STRING
        }
        viewModel.fromFeedAuthor = if (index == 3) 1 else 0
    }

    LaunchedEffect(id) {
        if (id != viewModel.id) {
            viewReply = false
            setReplyType(0)
            viewModel.refresh(id, isViewReply)
        }
    }

    val articleList = remember(key1 = viewModel.feedState) { viewModel.articleList }

    val replyLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data = if (SDK_INT >= 33)
                    result.data?.getParcelableExtra(
                        "response_data", HomeFeedResponse.Data::class.java
                    )
                else
                    result.data?.getParcelableExtra("response_data")
                data?.let {
                    viewModel.updateReply(it)
                    context.makeToast("回复成功")
                    if (viewModel.replyType == "feed" && shouldShowSortCard) {
                        scope.launch {
                            lazyListState.scrollToItem(viewModel.itemSize)
                        }
                    }
                }
            }
        }

    fun launchReply() {
        val intent = Intent(context, ReplyActivity::class.java)
        intent.putExtra("type", viewModel.replyType)
        intent.putExtra("rid", viewModel.replyId)
        intent.putExtra("username", viewModel.replyName)
        val options = ActivityOptionsCompat.makeCustomAnimation(
            context, R.anim.anim_bottom_sheet_slide_up, R.anim.anim_bottom_sheet_slide_down
        )
        replyLauncher.launch(intent, options)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.systemBars
                    .only(
                        WindowInsetsSides.Top
                                + if (isCompat) WindowInsetsSides.Start else WindowInsetsSides.End
                    ),
                navigationIcon = {
                    BackButton {
                        if (!isCompat) {
                            viewModel.resetState()
                        }
                        onBackClick()
                    }
                },
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .noRippleClickable {
                                scope.launch {
                                    lazyListState.scrollToItem(
                                        if (shouldShowSortCard || viewReply) {
                                            viewReply = false
                                            0
                                        } else {
                                            viewReply = true
                                            viewModel.itemSize
                                        }
                                    )
                                }
                            },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        AnimatedVisibility(
                            visible = !shouldShowTopCard,
                            enter = fadeIn(animationSpec = spring(stiffness = StiffnessLow)),
                            exit = fadeOut(animationSpec = spring(stiffness = StiffnessLow))
                        ) {
                            Text(text = viewModel.feedTypeName)
                        }
                    }
                    if (viewModel.feedState is LoadingState.Success) {
                        AnimatedVisibility(
                            visible = shouldShowTopCard,
                            enter = fadeIn(animationSpec = spring(stiffness = StiffnessLow)),
                            exit = fadeOut(animationSpec = spring(stiffness = StiffnessLow))
                        ) {
                            FeedHeader(
                                data = (viewModel.feedState as LoadingState.Success).response,
                                onViewUser = onViewUser,
                                isFeedContent = true,
                                onReport = onReport,
                                isFeedTop = true,
                                onBlockUser = {},
                            )
                        }
                    }
                },
                actions = {
                    if (viewModel.feedState is LoadingState.Success) {
                        Box(Modifier.wrapContentSize(Alignment.TopEnd)) {
                            IconButton(onClick = { dropdownMenuExpanded = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = null
                                )
                            }
                            DropdownMenu(
                                expanded = dropdownMenuExpanded,
                                onDismissRequest = { dropdownMenuExpanded = false }
                            ) {
                                listOf("复制", "分享")
                                    .forEachIndexed { index, menu ->
                                        DropdownMenuItem(
                                            text = { Text(menu) },
                                            onClick = {
                                                dropdownMenuExpanded = false
                                                when (index) {
                                                    0 -> context.copyText(
                                                        getShareText(ShareType.FEED, id)
                                                    )

                                                    1 -> context.shareText(
                                                        getShareText(ShareType.FEED, id)
                                                    )
                                                }
                                            }
                                        )
                                    }
                                DropdownMenuItem(
                                    text = { Text(if (viewModel.isFav) "取消收藏" else "收藏") },
                                    onClick = {
                                        dropdownMenuExpanded = false
                                        viewModel.onFav()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (viewModel.isBlocked) {
                                                stringResource(id = R.string.menu_unblock)
                                            } else {
                                                "拉黑"
                                            }
                                        )
                                    },
                                    onClick = {
                                        dropdownMenuExpanded = false
                                        viewModel.blockUser()
                                    }
                                )
                                if (isLogin) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(id = R.string.menu_report)) },
                                        onClick = {
                                            dropdownMenuExpanded = false
                                            onReport(id, ReportType.FEED)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isLogin && viewModel.feedState is LoadingState.Success) {
                AnimatedVisibility(
                    modifier = if (!isCompat) Modifier.navigationBarsPadding() else Modifier,
                    visible = lazyListState.isScrollingUp(),
                    enter = slideInVertically { it * 2 },
                    exit = slideOutVertically { it * 2 }
                ) {
                    FloatingActionButton(
                        onClick = {
                            viewModel.replyId = viewModel.id
                            viewModel.replyUid = viewModel.feedUid
                            viewModel.replyName = viewModel.feedUsername
                            viewModel.replyType = "feed"
                            launchReply()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null
                        )
                    }
                }
            }
        },
    ) { paddingValues ->

        PullToRefreshBox(
            modifier = Modifier.padding(
                top = paddingValues.calculateTopPadding(),
                end = if (isCompat) 0.dp else paddingValues.calculateRightPadding(layoutDirection),
                start = if (isCompat) paddingValues.calculateLeftPadding(layoutDirection) else 0.dp
            ),
            state = state,
            isRefreshing = viewModel.isRefreshing,
            onRefresh = {
                viewModel.isPull = true
                viewModel.refresh()
            },
            indicator = {
                PullToRefreshDefaults.Indicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    isRefreshing = viewModel.isRefreshing,
                    state = state,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding()),
                state = lazyListState
            ) {
                when (viewModel.feedState) {
                    LoadingState.Loading, LoadingState.Empty, is LoadingState.Error -> {
                        item(key = "feedState") {
                            Box(modifier = Modifier.fillParentMaxSize()) {
                                LoadingCard(
                                    modifier = Modifier.align(Alignment.Center),
                                    state = viewModel.feedState,
                                    onClick = if (viewModel.feedState is LoadingState.Loading) null
                                    else viewModel::refresh,
                                    isFeed = true,
                                )
                            }
                        }
                    }

                    is LoadingState.Success -> {
                        val response = (viewModel.feedState as LoadingState.Success).response
                        if (!articleList.isNullOrEmpty()) {
                            ArticleItem(
                                response = response,
                                articleList = articleList,
                                onOpenLink = onOpenLink,
                                onCopyText = onCopyText,
                                onLike = {
                                    if (isLogin) {
                                        viewModel.onLike(
                                            response.id.orEmpty(),
                                            response.userAction?.like ?: 0,
                                            LikeType.FEED
                                        )
                                    }
                                },
                                onViewUser = onViewUser,
                            )
                        } else {
                            item(key = "header") {
                                FeedHeader(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    data = response,
                                    onViewUser = onViewUser,
                                    isFeedContent = true,
                                    isFeedTop = false,
                                )
                            }
                            item(key = "feed") {
                                FeedCard(
                                    isFeedContent = true,
                                    data = response,
                                    onViewUser = onViewUser,
                                    onViewFeed = onViewFeed,
                                    onOpenLink = onOpenLink,
                                    onCopyText = onCopyText,
                                    onReport = onReport,
                                    onLike = viewModel::onLike,
                                    onDelete = { id, deleteType, frid ->
                                        viewModel.frid = frid
                                        viewModel.onDelete(id, deleteType)
                                    },
                                    onBlockUser = {},
                                )
                            }
                        }

                        item(key = "sort") {
                            FeedReplySortCard(
                                replyCount = viewModel.replyCount,
                                selected = selected,
                                updateSortReply = { index ->
                                    setReplyType(index)
                                    if (shouldShowSortCard)
                                        viewModel.isViewReply = true
                                    viewModel.refresh()
                                }
                            )
                            HorizontalDivider()
                        }

                        if (viewModel.listType == "lastupdate_desc") {
                            if (!response.topReplyRows.isNullOrEmpty()) {
                                response.topReplyRows?.getOrNull(0)?.let { reply ->
                                    item(key = "topReplyRows") {
                                        FeedReplyCard(
                                            data = reply,
                                            onViewUser = onViewUser,
                                            onShowTotalReply = ::onShowTotalReply,
                                            onOpenLink = onOpenLink,
                                            onCopyText = onCopyText,
                                            onReport = onReport,
                                            onLike = viewModel::onLike,
                                            onDelete = { id, deleteType, frid ->
                                                viewModel.frid = frid
                                                viewModel.onDelete(id, deleteType)
                                            },
                                            onBlockUser = { uid, frid ->
                                                viewModel.frid = frid
                                                viewModel.onBlockUser(uid)
                                            },
                                            onReply = { rid, uid, username, frid ->
                                                viewModel.replyId = rid
                                                viewModel.replyUid = uid
                                                viewModel.replyName = username
                                                viewModel.frid = frid
                                                viewModel.replyType = "reply"
                                                launchReply()
                                            },
                                        )
                                        HorizontalDivider()
                                    }
                                }
                            }

                            if (!response.replyMeRows.isNullOrEmpty()) {
                                response.replyMeRows?.getOrNull(0)?.let { reply ->
                                    item(key = "replyMeRows") {
                                        FeedReplyCard(
                                            data = reply,
                                            onViewUser = onViewUser,
                                            onShowTotalReply = ::onShowTotalReply,
                                            onOpenLink = onOpenLink,
                                            onCopyText = onCopyText,
                                            onReport = onReport,
                                            onLike = viewModel::onLike,
                                            onDelete = { id, deleteType, frid ->
                                                viewModel.frid = frid
                                                viewModel.onDelete(id, deleteType)
                                            },
                                            onBlockUser = { uid, frid ->
                                                viewModel.frid = frid
                                                viewModel.onBlockUser(uid)
                                            },
                                            onReply = { rid, uid, username, frid ->
                                                viewModel.replyId = rid
                                                viewModel.replyUid = uid
                                                viewModel.replyName = username
                                                viewModel.frid = frid
                                                viewModel.replyType = "reply"
                                                launchReply()
                                            },
                                        )
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                        if (viewModel.isViewReply) {
                            viewModel.isViewReply = false
                            scope.launch {
                                lazyListState.scrollToItem(viewModel.itemSize)
                            }
                        }
                    }
                }

                if (viewModel.feedState is LoadingState.Success) {

                    ItemCard(
                        loadingState = viewModel.loadingState,
                        loadMore = viewModel::loadMore,
                        isEnd = viewModel.isEnd,
                        onViewUser = onViewUser,
                        onViewFeed = onViewFeed,
                        onOpenLink = onOpenLink,
                        onCopyText = onCopyText,
                        onShowTotalReply = ::onShowTotalReply,
                        onReport = onReport,
                        onLike = viewModel::onLike,
                        onDelete = { id, deleteType, frid ->
                            viewModel.frid = frid
                            viewModel.onDelete(id, deleteType)
                        },
                        onBlockUser = { uid, frid ->
                            viewModel.frid = frid
                            viewModel.onBlockUser(uid)
                        },
                        onFollowUser = viewModel::onFollowUser,
                        onReply = { rid, uid, username, frid ->
                            viewModel.replyId = rid
                            viewModel.replyUid = uid
                            viewModel.replyName = username
                            viewModel.frid = frid
                            viewModel.replyType = "reply"
                            launchReply()
                        },
                    )

                    FooterCard(
                        footerState = viewModel.footerState,
                        loadMore = viewModel::loadMore,
                        isFeed = true
                    )

                }

            }
            if (shouldShowSortCard) {
                FeedReplySortCard(
                    replyCount = viewModel.replyCount,
                    selected = selected,
                    updateSortReply = { index ->
                        setReplyType(index)
                        viewModel.isViewReply = true
                        viewModel.refresh()
                    }
                )
            }

        }

    }

    fun resetBottomSheet() {
        openBottomSheet = false
        viewModel.resetReplyState()
    }

    if (openBottomSheet) {

        ModalBottomSheet(
            onDismissRequest = {
                resetBottomSheet()
            },
            sheetState = bottomSheetState,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
            ) {

                ItemCard(
                    loadingState = viewModel.replyLoadingState,
                    loadMore = viewModel::loadMoreReply,
                    isEnd = viewModel.isEndReply,
                    onViewUser = { uid ->
                        resetBottomSheet()
                        onViewUser(uid)
                    },
                    onViewFeed = { id, isViewReply ->
                        resetBottomSheet()
                        onViewFeed(id, isViewReply)
                    },
                    onOpenLink = { url, title ->
                        resetBottomSheet()
                        onOpenLink(url, title)
                    },
                    onCopyText = {
                        context.copyText(it?.getAllLinkAndText?.richToString())
                    },
                    onReport = { id, type ->
                        resetBottomSheet()
                        onReport(id, type)
                    },
                    isTotalReply = true,
                    onLike = { id, like, likeType ->
                        viewModel.onLikeReply(id, like, likeType)
                    },
                    onDelete = { id, deleteType, _ ->
                        viewModel.onDeleteRely(id, deleteType)
                    },
                    onBlockUser = { uid, _ ->
                        viewModel.onBlockReplyUser(uid)
                    },
                    isReply2Reply = !viewModel.frid.isNullOrEmpty(),
                    onShowTotalReply = { id, uid, frid ->
                        viewModel.replyId = id
                        viewModel.replyUid = uid
                        viewModel.frid = frid
                        viewModel.resetReplyState()
                        viewModel.fetchTotalReply()
                    },
                    onReply = { rid, uid, username, _ ->
                        viewModel.isSheet = true
                        viewModel.replyId = rid
                        viewModel.replyUid = uid
                        viewModel.replyName = username
                        viewModel.frid = null
                        viewModel.replyType = "reply"
                        launchReply()
                    },
                )

                FooterCard(
                    footerState = viewModel.replyFooterState,
                    loadMore = viewModel::loadMoreReply,
                    isFeed = true
                )
            }
        }
    }

    viewModel.toastText?.let {
        viewModel.resetToastText()
        context.makeToast(it)
    }

}
