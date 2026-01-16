package com.example.c001apk.compose.ui.collection

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.logic.state.LoadingState
import com.example.c001apk.compose.ui.component.BackButton
import com.example.c001apk.compose.ui.component.CoilLoader
import com.example.c001apk.compose.ui.component.FooterCard
import com.example.c001apk.compose.ui.component.ItemCard
import com.example.c001apk.compose.ui.component.cards.LoadingCard
import com.example.c001apk.compose.util.DateUtils.fromToday
import com.example.c001apk.compose.util.ReportType

/**
 * Created by bggRGjQaUbCoE on 2024/6/21
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    onBackClick: () -> Unit,
    id: String,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    onReport: (String, ReportType) -> Unit,
) {

    val viewModel =
        hiltViewModel<CollectionViewModel, CollectionViewModel.ViewModelFactory>(key = id) { factory ->
            factory.create(id = id)
        }
    val layoutDirection = LocalLayoutDirection.current
    val state = rememberPullToRefreshState()
    val lazyListState = rememberLazyListState()
    val shouldShowTitle by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.systemBars
                    .only(WindowInsetsSides.Start + WindowInsetsSides.Top),
                navigationIcon = {
                    BackButton {
                        onBackClick()
                    }
                },
                title = {
                    AnimatedContent(
                        shouldShowTitle,
                        label = EMPTY_STRING,
                    ) {
                        if (it) {
                            Text(text = viewModel.title)
                        } else {
                            Text(text = "收藏单")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->

        PullToRefreshBox(
            modifier = Modifier.padding(
                top = paddingValues.calculateTopPadding(),
                start = paddingValues.calculateLeftPadding(layoutDirection),
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
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 10.dp + paddingValues.calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                state = lazyListState
            ) {
                when (viewModel.collectionState) {
                    LoadingState.Loading, LoadingState.Empty, is LoadingState.Error -> {
                        item(key = "collectionState") {
                            Box(modifier = Modifier.fillParentMaxSize()) {
                                LoadingCard(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(horizontal = 10.dp),
                                    state = viewModel.collectionState,
                                    onClick = if (viewModel.collectionState is LoadingState.Loading) null
                                    else viewModel::refresh
                                )
                            }
                        }
                    }

                    is LoadingState.Success -> {
                        val response = (viewModel.collectionState as LoadingState.Success).response
                        item(key = "title") {
                            Text(
                                text = viewModel.title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 20.sp
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            )
                        }
                        item(key = "info") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CoilLoader(
                                        url = response.userAvatar,
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .clickable { onViewUser(response.uid.orEmpty()) }
                                    )
                                    Text(
                                        text = response.username.orEmpty(),
                                        color = MaterialTheme.colorScheme.outline,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(start = 10.dp),
                                    )
                                    Text(
                                        text = "${fromToday(response.lastupdate ?: 0)}更新",
                                        color = MaterialTheme.colorScheme.outline,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 10.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = response.followNum.toString() + "人关注",
                                        color = MaterialTheme.colorScheme.outline,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                response.description?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(top = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (viewModel.collectionState is LoadingState.Success) {

                    ItemCard(
                        loadingState = viewModel.loadingState,
                        loadMore = viewModel::loadMore,
                        isEnd = viewModel.isEnd,
                        onViewUser = onViewUser,
                        onViewFeed = onViewFeed,
                        onOpenLink = onOpenLink,
                        onCopyText = onCopyText,
                        onReport = onReport,
                        onLike = viewModel::onLike,
                        onDelete = { id, deleteType, _ ->
                            viewModel.onDelete(id, deleteType)
                        },
                        onBlockUser = { uid, _ ->
                            viewModel.onBlockUser(uid)
                        },
                    )

                    FooterCard(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        footerState = viewModel.footerState,
                        loadMore = viewModel::loadMore,
                    )
                }
            }
        }

    }

}
