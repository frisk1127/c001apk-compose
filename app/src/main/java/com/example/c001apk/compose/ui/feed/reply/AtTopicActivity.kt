package com.example.c001apk.compose.ui.feed.reply

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.ui.component.BackButton
import com.example.c001apk.compose.ui.component.cards.AppCard
import com.example.c001apk.compose.ui.component.cards.AppCardType
import com.example.c001apk.compose.ui.component.cards.LoadingCard
import com.example.c001apk.compose.ui.ffflist.FFFContentViewModel
import com.example.c001apk.compose.ui.search.SearchContentScreen
import com.example.c001apk.compose.ui.search.SearchFeedType
import com.example.c001apk.compose.ui.search.SearchOrderType
import com.example.c001apk.compose.ui.search.SearchType
import com.example.c001apk.compose.ui.theme.C001apkComposeTheme
import com.example.c001apk.compose.util.CookieUtil
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import android.util.Log
import com.example.c001apk.compose.BuildConfig

@AndroidEntryPoint
class AtTopicActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val type = intent.getStringExtra("type") ?: "user"
        setContent {
            C001apkComposeTheme(
                darkTheme = CookieUtil.isDarkMode,
                themeType = CookieUtil.themeType,
                seedColor = CookieUtil.seedColor,
                materialYou = CookieUtil.materialYou,
                pureBlack = CookieUtil.pureBlack,
                paletteStyle = CookieUtil.paletteStyle,
                fontScale = CookieUtil.fontScale,
                contentScale = CookieUtil.contentScale,
            ) {
                AtTopicScreen(
                    type = type,
                    onBack = { finish() },
                    onSelect = { selected ->
                        setResult(RESULT_OK, Intent().putExtra("data", selected))
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AtTopicScreen(
    type: String,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val isTopic = type == "topic"
    var keyword by rememberSaveable { mutableStateOf("") }
    val title = if (isTopic) "搜索话题" else "搜索用户"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton { onBack() } },
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                singleLine = true,
                placeholder = { Text(title) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )

            if (keyword.isBlank()) {
                val sectionPadding = remember {
                    PaddingValues(start = 0.dp, end = 0.dp, top = 0.dp, bottom = 0.dp)
                }
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = if (isTopic) "热门话题" else "关注用户",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium)
                    )
                    if (isTopic) {
                        HotTopicList(onSelect = onSelect)
                    } else {
                        FollowUserList(
                            uid = CookieUtil.uid,
                            paddingValues = sectionPadding,
                            onSelect = onSelect
                        )
                    }
                }
            } else {
                SearchContentScreen(
                    searchType = if (isTopic) SearchType.TOPIC else SearchType.USER,
                    keyword = keyword,
                    pageType = null,
                    pageParam = null,
                    refreshState = false,
                    resetRefreshState = {},
                    feedType = SearchFeedType.ALL,
                    orderType = SearchOrderType.DATELINE,
                    paddingValues = PaddingValues(0.dp),
                    onViewUser = {},
                    onViewFeed = { _, _ -> },
                    onOpenLink = { _, itemTitle ->
                        val targetTitle = itemTitle.orEmpty().trim()
                        if (targetTitle.isNotEmpty()) {
                            val data = if (isTopic) "#$targetTitle# " else "@$targetTitle "
                            onSelect(data)
                        }
                    },
                    onCopyText = {},
                    updateInitPage = {},
                    onReport = { _, _ -> },
                )
            }
        }
    }
}

@Composable
private fun FollowUserList(
    uid: String?,
    paddingValues: PaddingValues,
    onSelect: (String) -> Unit,
) {
    val viewModel =
        hiltViewModel<FFFContentViewModel, FFFContentViewModel.ViewModelFactory>(key = "at_follow_user_$uid") { factory ->
            factory.create(
                url = "/v6/user/followList",
                uid = uid,
                id = null,
                showDefault = null
            )
        }

    val viewModelState = viewModel.loadingState
    val dataList =
        (viewModelState as? com.example.c001apk.compose.logic.state.LoadingState.Success)?.response
            ?: emptyList()
    if (dataList.isNotEmpty()) {
        logDebug("follow list size=${dataList.size}")
        dataList.forEach { item ->
            val id = item.uid ?: item.userInfo?.uid ?: item.fUserInfo?.uid
            val name = item.username ?: item.userInfo?.username ?: item.fUserInfo?.username
            logDebug("item uid=$id name=$name")
        }
    }

    when (val state = viewModel.loadingState) {
        com.example.c001apk.compose.logic.state.LoadingState.Loading,
        com.example.c001apk.compose.logic.state.LoadingState.Empty,
        is com.example.c001apk.compose.logic.state.LoadingState.Error -> {
            LoadingCard(
                modifier = Modifier.padding(horizontal = 10.dp),
                state = state,
                onClick = if (state is com.example.c001apk.compose.logic.state.LoadingState.Loading) null
                else viewModel::loadMore
            )
        }

        is com.example.c001apk.compose.logic.state.LoadingState.Success -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 10.dp,
                    bottom = 10.dp + paddingValues.calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(state.response) { index, item ->
                    val cardType = when (item.entityType) {
                        "user" -> AppCardType.USER
                        "contacts" -> AppCardType.CONTACTS
                        else -> null
                    }
                    if (cardType != null) {
                        AppCard(
                            data = item,
                            onOpenLink = { _, _ -> },
                            appCardType = cardType,
                            isHomeFeed = false,
                            onViewUser = {
                                val name = if (item.entityType == "contacts") {
                                    item.fUserInfo?.username
                                        ?: item.userInfo?.username
                                        ?: item.username
                                } else {
                                    item.username
                                        ?: item.userInfo?.username
                                        ?: item.fUserInfo?.username
                                }
                                logDebug("itemClick uid=$it name=$name")
                                if (!name.isNullOrEmpty()) {
                                    onSelect("@$name ")
                                }
                            },
                            onFollowUser = null,
                            onHandleRecent = null
                        )
                    }
                    if (index == state.response.lastIndex && !viewModel.isEnd) {
                        viewModel.loadMore()
                    }
                }
            }
        }
    }
}

@Composable
private fun HotTopicList(
    onSelect: (String) -> Unit,
) {
    SearchContentScreen(
        searchType = SearchType.TOPIC,
        keyword = "热门",
        pageType = null,
        pageParam = null,
        refreshState = false,
        resetRefreshState = {},
        feedType = SearchFeedType.ALL,
        orderType = SearchOrderType.DATELINE,
        paddingValues = PaddingValues(0.dp),
        onViewUser = {},
        onViewFeed = { _, _ -> },
        onOpenLink = { _, itemTitle ->
            val targetTitle = itemTitle.orEmpty().trim()
            if (targetTitle.isNotEmpty()) {
                onSelect("#$targetTitle# ")
            }
        },
        onCopyText = {},
        updateInitPage = {},
        onReport = { _, _ -> },
    )
}

private fun logDebug(message: String) {
    if (BuildConfig.DEBUG) {
        Log.d("AtTopic", message)
    }
}
