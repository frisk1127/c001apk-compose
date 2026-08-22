package com.example.c001apk.compose.ui.home

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.c001apk.compose.R
import com.example.c001apk.compose.logic.model.HomeMenu
import com.example.c001apk.compose.logic.model.UpdateCheckItem
import com.example.c001apk.compose.ui.component.rememberHapticClick
import com.example.c001apk.compose.ui.feed.reply.ReplyActivity
import com.example.c001apk.compose.ui.home.app.AppListScreen
import com.example.c001apk.compose.ui.home.feed.HomeFeedScreen
import com.example.c001apk.compose.ui.home.topic.HomeTopicScreen
import com.example.c001apk.compose.util.CookieUtil.isLogin
import com.example.c001apk.compose.util.ReportType
import kotlinx.coroutines.launch

/**
 * Created by bggRGjQaUbCoE on 2024/6/5
 */

enum class TabType {
    FOLLOW, APP, FEED, HOT, TOPIC, PRODUCT, COOLPIC
}

private fun tabTitle(type: TabType): String {
    return when (type) {
        TabType.FOLLOW -> "关注"
        TabType.APP -> "应用"
        TabType.FEED -> "动态"
        TabType.HOT -> "热榜"
        TabType.TOPIC -> "话题"
        TabType.PRODUCT -> "数码"
        TabType.COOLPIC -> "酷图"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTabEditorDialog(
    orderedTabs: List<TabType>,
    enabledTabs: Set<TabType>,
    onDismiss: () -> Unit,
    onConfirm: (List<TabType>, Set<TabType>) -> Unit,
) {
    var selectedTabs by remember(enabledTabs) { mutableStateOf(enabledTabs) }
    val tabs = remember(orderedTabs) {
        mutableStateListOf<TabType>().apply { addAll(orderedTabs) }
    }
    val listState = rememberLazyListState()
    var draggedItemIndex by remember { mutableIntStateOf(-1) }
    var draggedItemOffset by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("编辑首页板块") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回",
                                )
                            }
                        },
                        actions = {
                            TextButton(onClick = { onConfirm(tabs.toList(), selectedTabs) }) {
                                Text("完成")
                            }
                        },
                    )
                },
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "首页板块",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = "已启用 ${selectedTabs.size} / ${tabs.size}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }

                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp)
                            .pointerInput(tabs) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    draggedItemIndex = listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { item ->
                                            offset.y.toInt() in item.offset..(item.offset + item.size)
                                        }
                                        ?.index
                                        ?: -1
                                    draggedItemOffset = 0f
                                },
                                onDragCancel = {
                                    draggedItemIndex = -1
                                    draggedItemOffset = 0f
                                },
                                onDragEnd = {
                                    draggedItemIndex = -1
                                    draggedItemOffset = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    val currentIndex = draggedItemIndex
                                    if (currentIndex == -1) return@detectDragGesturesAfterLongPress

                                    change.consume()
                                    draggedItemOffset += dragAmount.y
                                    val currentItem = listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.index == currentIndex }
                                        ?: return@detectDragGesturesAfterLongPress
                                    val draggedCenter = currentItem.offset + draggedItemOffset + currentItem.size / 2f
                                    val targetItem = listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { item ->
                                            item.index != currentIndex &&
                                                draggedCenter.toInt() in item.offset..(item.offset + item.size)
                                        }
                                        ?: return@detectDragGesturesAfterLongPress

                                    tabs.add(targetItem.index, tabs.removeAt(currentIndex))
                                    draggedItemIndex = targetItem.index
                                    draggedItemOffset += currentItem.offset - targetItem.offset
                                },
                            )
                            },
                    ) {
                        itemsIndexed(tabs, key = { _, type -> type.name }) { index, type ->
                            val checked = type in selectedTabs
                            val canToggle = !checked || selectedTabs.size > 1
                            ListItem(
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "长按拖动排序",
                                        tint = MaterialTheme.colorScheme.outline,
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        translationY = if (index == draggedItemIndex) draggedItemOffset else 0f
                                    }
                                    .clickable(enabled = canToggle) {
                                        selectedTabs = if (checked) {
                                            selectedTabs - type
                                        } else {
                                            selectedTabs + type
                                        }
                                    },
                                headlineContent = { Text(tabTitle(type)) },
                                supportingContent = {
                                    Text(
                                        text = if (checked) "已显示" else "已隐藏",
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                },
                                trailingContent = {
                                    Switch(
                                        checked = checked,
                                        enabled = canToggle,
                                        onCheckedChange = { enabled ->
                                            selectedTabs = if (enabled) {
                                                selectedTabs + type
                                            } else {
                                                selectedTabs - type
                                            }
                                        },
                                    )
                                },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 56.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    refreshState: Boolean,
    resetRefreshState: () -> Unit,
    onRefresh: () -> Unit,
    onSearch: () -> Unit,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    onViewApp: (String) -> Unit,
    onCheckUpdate: (List<UpdateCheckItem>) -> Unit,
    onReport: (String, ReportType) -> Unit,
) {

    val scope = rememberCoroutineScope()

    val storedMenus by viewModel.homeMenus.collectAsStateWithLifecycle(initialValue = emptyList())
    val orderedTabs = remember(storedMenus) {
        if (storedMenus.isEmpty()) {
            TabType.entries
        } else {
            val storedOrder = storedMenus
                .asSequence()
                .sortedBy(HomeMenu::position)
                .mapNotNull { menu ->
                    runCatching { TabType.valueOf(menu.title) }.getOrNull()
                }
                .distinct()
                .toList()
            storedOrder + TabType.entries.filterNot(storedOrder::contains)
        }
    }
    val enabledTabs = remember(storedMenus) {
        if (storedMenus.isEmpty()) {
            TabType.entries.toSet()
        } else {
            storedMenus
                .asSequence()
                .filter(HomeMenu::isEnable)
                .mapNotNull { menu -> runCatching { TabType.valueOf(menu.title) }.getOrNull() }
                .toSet()
        }
    }
    val tabList = remember(orderedTabs, enabledTabs) {
        orderedTabs.filter(enabledTabs::contains).ifEmpty { listOf(TabType.FEED) }
    }
    var selectedTabType by rememberSaveable { mutableStateOf(TabType.FEED) }
    val initialPage = tabList.indexOf(selectedTabType).coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = {
            tabList.size
        }
    )
    val context = LocalContext.current
    var isScrollingUp by remember { mutableStateOf(false) }
    var showTabEditor by rememberSaveable { mutableStateOf(false) }
    val selectedTabIndex = pagerState.currentPage.coerceIn(tabList.indices)

    LaunchedEffect(tabList) {
        val targetType = selectedTabType.takeIf(tabList::contains)
            ?: TabType.FEED.takeIf(tabList::contains)
            ?: tabList.first()
        selectedTabType = targetType
        val targetIndex = tabList.indexOf(targetType)
        if (pagerState.currentPage != targetIndex) {
            pagerState.scrollToPage(targetIndex)
        }
    }

    LaunchedEffect(pagerState, tabList) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            tabList.getOrNull(page)?.let { selectedTabType = it }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if (isLogin && tabList.getOrNull(selectedTabIndex) == TabType.FEED) {
                AnimatedVisibility(
                    visible = isScrollingUp,
                    enter = slideInVertically { it * 2 },
                    exit = slideOutVertically { it * 2 }
                ) {
                    FloatingActionButton(
                        onClick = rememberHapticClick {
                            val intent = Intent(context, ReplyActivity::class.java)
                            intent.putExtra("type", "createFeed")
                            val animationBundle = ActivityOptionsCompat.makeCustomAnimation(
                                context,
                                R.anim.anim_bottom_sheet_slide_up,
                                R.anim.anim_bottom_sheet_slide_down
                            ).toBundle()
                            ContextCompat.startActivity(context, intent, animationBundle)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                    }
                }
            }
        },
        contentWindowInsets = ScaffoldDefaults
            .contentWindowInsets
            .exclude(WindowInsets.navigationBars)
    ) { paddingValues ->

        Column(
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SecondaryScrollableTabRow(
                    modifier = Modifier.weight(1f),
                    selectedTabIndex = selectedTabIndex,
                    indicator = {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier
                                .tabIndicatorOffset(selectedTabIndex, matchContentSize = true)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        )
                    },
                    divider = {}
                ) {
                    tabList.forEachIndexed { index, tab ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = rememberHapticClick {
                                if (pagerState.currentPage == index) {
                                    onRefresh()
                                }
                                selectedTabType = tab
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = { Text(text = tabTitle(tab)) }
                        )
                    }
                }
                IconButton(
                    modifier = Modifier.size(40.dp),
                    onClick = { onSearch() },
                ) {
                    Icon(
                        modifier = Modifier.size(21.dp),
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索",
                    )
                }
                IconButton(
                    modifier = Modifier.size(40.dp),
                    onClick = rememberHapticClick { showTabEditor = true },
                ) {
                    Icon(
                        modifier = Modifier.size(21.dp),
                        painter = painterResource(R.drawable.ic_menu),
                        contentDescription = "编辑首页板块",
                    )
                }
            }

            HorizontalDivider()

            HorizontalPager(
                state = pagerState,
            ) { index ->

                when (val type = tabList[index]) {
                    TabType.FOLLOW, TabType.FEED, TabType.HOT, TabType.COOLPIC ->
                        HomeFeedScreen(
                            refreshState = refreshState,
                            resetRefreshState = resetRefreshState,
                            type = type,
                            onViewUser = onViewUser,
                            onViewFeed = onViewFeed,
                            onOpenLink = onOpenLink,
                            onCopyText = onCopyText,
                            onReport = onReport,
                            isScrollingUp = {
                                isScrollingUp = it
                            }
                        )

                    TabType.APP -> AppListScreen(
                        refreshState = refreshState,
                        resetRefreshState = resetRefreshState,
                        onViewApp = onViewApp,
                        onCheckUpdate = onCheckUpdate,
                    )

                    TabType.TOPIC, TabType.PRODUCT -> HomeTopicScreen(
                        type = type,
                        onViewUser = onViewUser,
                        onViewFeed = onViewFeed,
                        onOpenLink = onOpenLink,
                        onCopyText = onCopyText
                    )
                }

            }

        }

    }

    if (showTabEditor) {
        HomeTabEditorDialog(
            orderedTabs = orderedTabs,
            enabledTabs = enabledTabs,
            onDismiss = { showTabEditor = false },
            onConfirm = { newOrder, newEnabledTabs ->
                viewModel.setTabs(newOrder, newEnabledTabs)
                showTabEditor = false
            },
        )
    }

}
