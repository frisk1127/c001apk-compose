package com.example.c001apk.compose.ui.app

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.c001apk.compose.R
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.logic.state.LoadingState
import com.example.c001apk.compose.ui.component.BackButton
import com.example.c001apk.compose.ui.component.cards.AppInfoCard
import com.example.c001apk.compose.ui.component.cards.LoadingCard
import com.example.c001apk.compose.ui.feed.reply.ReplyActivity
import com.example.c001apk.compose.util.CookieUtil.isLogin
import com.example.c001apk.compose.util.ReportType
import com.example.c001apk.compose.util.downloadApk
import com.example.c001apk.compose.util.makeToast
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.launch
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ExperimentalToolbarApi
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState

/**
 * Created by bggRGjQaUbCoE on 2024/6/10
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalToolbarApi::class)
@Composable
fun AppScreen(
    onBackClick: () -> Unit,
    packageName: String,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    onSearch: (String, String, String) -> Unit,
    onReport: (String, ReportType) -> Unit,
) {

    val viewModel =
        hiltViewModel<AppViewModel, AppViewModel.ViewModelFactory>(key = packageName) { factory ->
            factory.create(packageName)
        }

    val context = LocalContext.current

    val tabList by lazy { listOf("最近回复", "最新发布", "热度排序") }
    var dropdownMenuExpanded by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = {
            tabList.size
        }
    )
    val scope = rememberCoroutineScope()
    var refreshState by remember { mutableStateOf(false) }

    val state = rememberCollapsingToolbarScaffoldState()

    val windowInsets = WindowInsets.systemBars
    var isScrollingUp by remember { mutableStateOf(false) }

    CollapsingToolbarScaffold(
        state = state,
        scrollStrategy = ScrollStrategy.ExitUntilCollapsed,
        modifier = Modifier.fillMaxSize(),
        toolbar = {
            TopAppBar(
                windowInsets = WindowInsets.systemBars
                    .only(WindowInsetsSides.Start + WindowInsetsSides.Top),
                navigationIcon = {
                    BackButton { onBackClick() }
                },
                title = {
                    Text(
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = if (state.toolbarState.progress == 0f) 1f else 0f
                            },
                        text = viewModel.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    if (viewModel.appState is LoadingState.Success) {
                        Row(Modifier.wrapContentSize(Alignment.TopEnd)) {
                            IconButton(
                                onClick = {
                                    onSearch(viewModel.title, "apk", viewModel.id)
                                }
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null)
                            }
                            Box {
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
                                    if (isLogin) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (viewModel.isFollowed) "UnFollow"
                                                    else "Follow"
                                                )
                                            },
                                            onClick = {
                                                dropdownMenuExpanded = false
                                                viewModel.onGetFollowApk()
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (viewModel.isBlocked) "UnBlock"
                                                else "拉黑"
                                            )
                                        },
                                        onClick = {
                                            dropdownMenuExpanded = false
                                            viewModel.blockApp()
                                        }
                                    )
                                }
                            }

                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
            if (viewModel.appState !is LoadingState.Error) {
                AppInfoCard(
                    modifier = Modifier
                        .padding(top = 58.dp)
                        .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Start + WindowInsetsSides.Top))
                        .parallax(0.5f)
                        .graphicsLayer {
                            alpha = state.toolbarState.progress
                        },
                    data = (viewModel.appState as? LoadingState.Success)?.response,
                    onDownloadApk = viewModel::onGetDownloadLink
                )
            }
        }
    ) {

        Column(modifier = Modifier.fillMaxSize()) {
            when (viewModel.appState) {
                LoadingState.Loading, LoadingState.Empty, is LoadingState.Error -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LoadingCard(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 10.dp),
                            state = viewModel.appState,
                            onClick = if (viewModel.appState is LoadingState.Loading) null
                            else viewModel::refresh
                        )
                    }
                }

                is LoadingState.Success -> {

                    if (viewModel.commentStatus == 1) {

                        SecondaryTabRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Start)),
                            selectedTabIndex = pagerState.currentPage,
                            indicator = {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier
                                        .tabIndicatorOffset(
                                            pagerState.currentPage,
                                            matchContentSize = true
                                        )
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                )
                            },
                            divider = {}
                        ) {
                            tabList.forEachIndexed { index, tab ->
                                Tab(
                                    selected = pagerState.currentPage == index,
                                    onClick = {
                                        if (pagerState.currentPage == index) {
                                            refreshState = true
                                        }
                                        scope.launch { pagerState.animateScrollToPage(index) }
                                    },
                                    text = { Text(text = tab) }
                                )
                            }
                        }

                        HorizontalDivider()

                        HorizontalPager(
                            state = pagerState
                        ) { index ->
                            AppContentScreen(
                                refreshState = refreshState,
                                resetRefreshState = {
                                    refreshState = false
                                },
                                id = viewModel.id,
                                appCommentSort = when (index) {
                                    0 -> EMPTY_STRING
                                    1 -> "&sort=dateline_desc"
                                    2 -> "&sort=popular"
                                    else -> EMPTY_STRING

                                },
                                appCommentTitle = when (index) {
                                    0 -> "最近回复"
                                    1 -> "最新发布"
                                    2 -> "热度排序"
                                    else -> "最近回复"
                                },
                                onViewUser = onViewUser,
                                onViewFeed = onViewFeed,
                                onOpenLink = onOpenLink,
                                onCopyText = onCopyText,
                                onReport = onReport,
                                isScrollingUp = {
                                    isScrollingUp = it
                                }
                            )
                        }

                    } else {
                        HorizontalDivider()
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .align(Alignment.Center),
                                text = viewModel.commentStatusText,
                            )
                        }
                    }
                }
            }
        }

        if (isLogin) {
            AnimatedVisibility(
                visible = isScrollingUp,
                enter = slideInVertically { it * 2 },
                exit = slideOutVertically { it * 2 },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        val intent = Intent(context, ReplyActivity::class.java)
                        intent.putExtra("type", "createFeed")
                        intent.putExtra("targetType", "apk")
                        intent.putExtra(
                            "targetId",
                            "${1000000000 + (viewModel.id.toIntOrNull() ?: 4599)}"
                        )
                        val animationBundle = ActivityOptionsCompat.makeCustomAnimation(
                            context,
                            R.anim.anim_bottom_sheet_slide_up,
                            R.anim.anim_bottom_sheet_slide_down
                        ).toBundle()
                        ContextCompat.startActivity(context, intent, animationBundle)
                    }
                ) {
                    Icon(
                        painter = rememberDrawablePainter(
                            ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.outline_note_alt_24,
                                context.theme
                            )
                        ),
                        contentDescription = null
                    )
                }
            }
        }

    }

    when {
        viewModel.downloadApk -> {
            viewModel.reset()
            context.downloadApk(
                viewModel.downloadUrl,
                "${viewModel.title}-${viewModel.versionName}-${viewModel.versionCode}.apk"
            )
        }
    }

    viewModel.toastText?.let {
        context.makeToast(it)
        viewModel.resetToastText()
    }

}
