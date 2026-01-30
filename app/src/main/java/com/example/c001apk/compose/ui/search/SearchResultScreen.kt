package com.example.c001apk.compose.ui.search

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.c001apk.compose.ui.component.BackButton
import com.example.c001apk.compose.util.ReportType
import com.example.c001apk.compose.util.decode
import com.example.c001apk.compose.util.noRippleClickable
import kotlinx.coroutines.launch

/**
 * Created by bggRGjQaUbCoE on 2024/6/9
 */

enum class SearchType {
    FEED, APP, GAME, PRODUCT, USER, TOPIC
}

enum class SearchFeedType {
    ALL, FEED, ARTICLE, COOLPIC, COMMENT, RATING, ANSWER, QUESTION, VOTE
}

enum class SearchOrderType {
    DATELINE, HOT, REPLY
}

private fun searchTypeTitle(type: SearchType): String {
    return when (type) {
        SearchType.FEED -> "动态"
        SearchType.APP -> "应用"
        SearchType.GAME -> "游戏"
        SearchType.PRODUCT -> "设备"
        SearchType.USER -> "用户"
        SearchType.TOPIC -> "话题"
    }
}

private fun searchFeedTitle(type: SearchFeedType): String {
    return when (type) {
        SearchFeedType.ALL -> "全部"
        SearchFeedType.FEED -> "动态"
        SearchFeedType.ARTICLE -> "文章"
        SearchFeedType.COOLPIC -> "酷图"
        SearchFeedType.COMMENT -> "评论"
        SearchFeedType.RATING -> "评分"
        SearchFeedType.ANSWER -> "回答"
        SearchFeedType.QUESTION -> "问题"
        SearchFeedType.VOTE -> "投票"
    }
}

private fun searchOrderTitle(type: SearchOrderType): String {
    return when (type) {
        SearchOrderType.DATELINE -> "时间"
        SearchOrderType.HOT -> "热度"
        SearchOrderType.REPLY -> "回复"
    }
}

private fun pageTypeTitle(pageType: String?): String? {
    if (pageType.isNullOrEmpty()) return pageType
    val match = SearchType.entries.firstOrNull { it.name.equals(pageType, ignoreCase = true) }
    return match?.let { searchTypeTitle(it) } ?: pageType
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultScreen(
    onBackClick: () -> Unit,
    keyword: String,
    title: String?,
    pageType: String?,
    pageParam: String?,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    initialPage: Int = 0,
    updateInitPage: (Int) -> Unit,
    onReport: (String, ReportType) -> Unit,
) {

    val layoutDirection = LocalLayoutDirection.current
    val tabList = SearchType.entries
    var dropdownMenuExpanded by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { if (pageType.isNullOrEmpty()) tabList.size else 1 }
    )
    var refreshState by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var orderMenuExpanded by remember { mutableStateOf(false) }
    var feedType by rememberSaveable { mutableStateOf(SearchFeedType.ALL) }
    var orderType by rememberSaveable { mutableStateOf(SearchOrderType.DATELINE) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.systemBars
                    .only(WindowInsetsSides.Start + WindowInsetsSides.Top),
                navigationIcon = {
                    BackButton { onBackClick() }
                },
                title = {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .noRippleClickable { onBackClick() },
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = keyword.decode,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!title.isNullOrEmpty()) {
                            Text(
                                text = "${pageTypeTitle(pageType)}: $title",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                            )
                        }
                    }
                },
                actions = {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = pagerState.currentPage == 0,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
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
                                listOf("类型", "排序")
                                    .forEachIndexed { index, menu ->
                                        DropdownMenuItem(
                                            text = { Text(menu) },
                                            onClick = {
                                                dropdownMenuExpanded = false
                                                when (index) {
                                                    0 -> typeMenuExpanded = true
                                                    1 -> orderMenuExpanded = true
                                                }
                                            },
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                                                    contentDescription = null
                                                )
                                            }
                                        )
                                    }

                            }
                            DropdownMenu(
                                expanded = typeMenuExpanded,
                                onDismissRequest = { typeMenuExpanded = false }
                            ) {
                                SearchFeedType.entries.forEach { feed ->
                                    Row(
                                        modifier = Modifier
                                            .clickable {
                                                typeMenuExpanded = false
                                                feedType = feed
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = feedType == feed,
                                            onClick = {
                                                typeMenuExpanded = false
                                                feedType = feed
                                            }
                                        )
                                        Text(
                                            text = searchFeedTitle(feed),
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(end = 16.dp)
                                        )
                                    }
                                }
                            }
                            DropdownMenu(
                                expanded = orderMenuExpanded,
                                onDismissRequest = { orderMenuExpanded = false }
                            ) {
                                SearchOrderType.entries.forEach { order ->
                                    Row(
                                        modifier = Modifier
                                            .clickable {
                                                orderMenuExpanded = false
                                                orderType = order
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = orderType == order,
                                            onClick = {
                                                orderMenuExpanded = false
                                                orderType = order
                                            }
                                        )
                                        Text(
                                            text = searchOrderTitle(order),
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(end = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
            )
        },
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {

            if (pageType.isNullOrEmpty()) {
                SecondaryScrollableTabRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = paddingValues.calculateLeftPadding(layoutDirection),
                        ),
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
                            text = { Text(text = searchTypeTitle(tab)) }
                        )
                    }
                }
            }

            HorizontalDivider()

            HorizontalPager(
                state = pagerState
            ) { index ->
                SearchContentScreen(
                    searchType = SearchType.valueOf(SearchType.entries[index].name),
                    keyword = keyword.decode,
                    pageType = pageType,
                    pageParam = pageParam,
                    refreshState = refreshState,
                    resetRefreshState = {
                        refreshState = false
                    },
                    feedType = feedType,
                    orderType = orderType,
                    paddingValues = PaddingValues(
                        start = paddingValues.calculateLeftPadding(layoutDirection),
                        bottom = paddingValues.calculateBottomPadding(),
                    ),
                    onViewUser = onViewUser,
                    onViewFeed = onViewFeed,
                    onOpenLink = onOpenLink,
                    onCopyText = onCopyText,
                    updateInitPage = {
                        updateInitPage(pagerState.currentPage)
                    },
                    onReport = onReport,
                )
            }
        }

    }

}
