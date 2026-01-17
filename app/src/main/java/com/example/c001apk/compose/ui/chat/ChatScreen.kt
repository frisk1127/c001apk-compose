package com.example.c001apk.compose.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.res.ResourcesCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.c001apk.compose.R
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.logic.model.OSSUploadPrepareModel
import com.example.c001apk.compose.logic.state.LoadingState
import com.example.c001apk.compose.ui.component.BackButton
import com.example.c001apk.compose.ui.component.ChatEditText
import com.example.c001apk.compose.ui.component.cards.CardIndicator
import com.example.c001apk.compose.ui.component.cards.ChatLeftCard
import com.example.c001apk.compose.ui.component.cards.ChatRightCard
import com.example.c001apk.compose.ui.component.cards.ChatTimeCard
import com.example.c001apk.compose.ui.component.cards.LoadingCard
import com.example.c001apk.compose.ui.theme.cardBg
import com.example.c001apk.compose.util.CookieUtil
import com.example.c001apk.compose.util.EmojiUtils
import com.example.c001apk.compose.util.EmojiUtils.coolBList
import com.example.c001apk.compose.util.EmojiUtils.emojiList
import com.example.c001apk.compose.util.OSSUtil.getImageDimensionsAndMD5
import com.example.c001apk.compose.util.OSSUtil.toHex
import com.example.c001apk.compose.util.OssUploadUtil.ossUpload
import com.example.c001apk.compose.util.ReportType
import com.example.c001apk.compose.util.copyText
import com.example.c001apk.compose.util.makeToast
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Created by bggRGjQaUbCoE on 2024/6/19
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBackClick: () -> Unit,
    ukey: String,
    uid: String,
    username: String,
    onViewUser: (String) -> Unit,
    onReport: (String, ReportType) -> Unit,
) {

    val viewModel =
        hiltViewModel<ChatViewModel, ChatViewModel.ViewModelFactory>(key = ukey) { factory ->
            factory.create(ukey = ukey)
        }

    val context = LocalContext.current
    var dropdownMenuExpanded by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var clearText by remember { mutableStateOf(false) }
    var clearFocus by remember { mutableStateOf(false) }
    val windowInsets = WindowInsets.navigationBars

    fun handlePickedImage(uri: android.net.Uri) {
        try {
            val result = getImageDimensionsAndMD5(context.contentResolver, uri)
            val md5Byte = result.second
            val md5 = md5Byte?.toHex() ?: ""
            val width = result.first?.first ?: 0
            val height = result.first?.second ?: 0
            val type = result.first?.third ?: ""

            viewModel.uriList = listOf(uri)
            viewModel.md5List = listOf(md5Byte)
            viewModel.typeList = listOf(type)
            val list = listOf(
                OSSUploadPrepareModel(
                    name = "${
                        UUID.randomUUID().toString().replace("-", "")
                    }.${if (type.startsWith("image/")) type.substring(6) else type}",
                    resolution = "${width}x${height}",
                    md5 = md5,
                )
            )
            viewModel.onPostOSSUploadPrepare(uid, list)
        } catch (e: Exception) {
            e.message?.let {
                context.makeToast(it)
            }
            e.printStackTrace()
        }
    }

    LaunchedEffect(key1 = viewModel.scroll) {
        if (viewModel.scroll) {
            clearText = true
            delay(150)
            lazyListState.scrollToItem(0)
            viewModel.reset()
        }
    }

    fun onClearFocus() {
        clearFocus = true
    }

    val pickContent =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { uri1 ->
                handlePickedImage(uri1)
            }
        }

    val pickDocument =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { uri1 ->
                handlePickedImage(uri1)
            }
        }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.systemBars
                    .only(WindowInsetsSides.Start + WindowInsetsSides.Top),
                navigationIcon = {
                    BackButton {
                        clearFocus = true
                        onBackClick()
                    }
                },
                title = { Text(text = username) },
                actions = {
                    Box {
                        IconButton(onClick = { dropdownMenuExpanded = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = dropdownMenuExpanded,
                            onDismissRequest = { dropdownMenuExpanded = false }) {
                            listOf(
                                stringResource(id = R.string.menu_check),
                                "拉黑",
                                stringResource(id = R.string.menu_report)
                            ).forEachIndexed { index, menu ->
                                DropdownMenuItem(
                                    text = { Text(text = menu) },
                                    onClick = {
                                        dropdownMenuExpanded = false
                                        when (index) {
                                            0 -> {
                                                onClearFocus()
                                                onViewUser(uid)
                                            }

                                            1 -> viewModel.onBlockUser(uid)
                                            2 -> {
                                                onClearFocus()
                                                onReport(uid, ReportType.USER)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        contentWindowInsets = ScaffoldDefaults
            .contentWindowInsets
            .exclude(WindowInsets.navigationBars)
            .exclude(WindowInsets.ime),
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                )
                .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Start))
        ) {
            HorizontalDivider()

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                reverseLayout = true,
                state = lazyListState,
            ) {
                when (viewModel.loadingState) {
                    LoadingState.Loading, LoadingState.Empty, is LoadingState.Error -> {
                        item(key = "loadingState") {
                            Box(modifier = Modifier.fillParentMaxSize()) {
                                LoadingCard(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(horizontal = 10.dp),
                                    state = viewModel.loadingState,
                                    onClick = if (viewModel.loadingState is LoadingState.Loading) null
                                    else viewModel::loadMore
                                )
                            }
                        }
                    }

                    is LoadingState.Success -> {
                        itemsIndexed(
                            items = (viewModel.loadingState as LoadingState.Success).response,
                            key = { _, item -> item.entityId + item.dateline },
                        ) { index, item ->
                            when (item.entityType) {
                                "message" -> when (item.fromuid) {
                                    CookieUtil.uid ->
                                        ChatRightCard(
                                            data = item,
                                            onGetImageUrl = viewModel::onGetImageUrl,
                                            onLongClick = { id, msg, url ->
                                                onClearFocus()
                                                viewModel.deleteId = id
                                                viewModel.message = msg
                                                viewModel.pic = url
                                                showDialog = true
                                            },
                                            onViewUser = { uid ->
                                                onClearFocus()
                                                onViewUser(uid)
                                            },
                                            onClearFocus = ::onClearFocus
                                        )

                                    else ->
                                        ChatLeftCard(
                                            data = item,
                                            onGetImageUrl = viewModel::onGetImageUrl,
                                            onLongClick = { id, msg, url ->
                                                onClearFocus()
                                                viewModel.deleteId = id
                                                viewModel.message = msg
                                                viewModel.pic = url
                                                showDialog = true
                                            },
                                            onViewUser = { uid ->
                                                onClearFocus()
                                                onViewUser(uid)
                                            },
                                            onClearFocus = ::onClearFocus
                                        )
                                }

                                "messageExtra" -> ChatTimeCard(title = item.title.orEmpty())
                            }

                            if (index == (viewModel.loadingState as LoadingState.Success).response.lastIndex && !viewModel.isEnd) {
                                viewModel.loadMore()
                            }
                        }
                    }
                }
            }

            ChatBottom(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg())
                    .imePadding()
                    .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Start + WindowInsetsSides.Bottom)),
                onPickImage = {
                    onClearFocus()
                    pickContent.launch("image/*")
                },
                onPickOtherImage = {
                    onClearFocus()
                    pickDocument.launch(arrayOf("image/*"))
                },
                onSendMessage = {
                    viewModel.onSendMessage(uid, it, EMPTY_STRING)
                },
                clearFocus = clearFocus,
                clearText = clearText,
                resetClear = {
                    clearText = false
                },
                resetFocus = {
                    clearFocus = false
                },
                onClearFocus = ::onClearFocus,
                viewModel = viewModel,
            )
        }

    }

    when {
        viewModel.showUploadDialog -> {
            LoadingDialog()
        }

        showDialog -> {
            Dialog(onDismissRequest = { showDialog = false }) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.elevatedCardColors()
                        .copy(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "删除",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showDialog = false
                                    viewModel.onDeleteMsg()
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (viewModel.pic.isEmpty()) {
                            Text(
                                text = "复制",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showDialog = false
                                        context.copyText(viewModel.message)
                                    }
                                    .padding(horizontal = 24.dp, vertical = 14.dp),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }
        }
    }

    viewModel.toastText?.let {
        context.makeToast(it)
        viewModel.resetToastText()
    }

    viewModel.uploadImage?.let { responseData ->
        scope.launch(Dispatchers.IO) {
            ossUpload(
                context = context,
                responseData = responseData,
                uriList = viewModel.uriList,
                typeList = viewModel.typeList,
                md5List = viewModel.md5List,
                iOnSuccess = {
                    viewModel.onSendMessage(
                        uid,
                        EMPTY_STRING,
                        "/${responseData.fileInfo.getOrNull(0)?.uploadFileName}"
                    )
                },
                iOnFailure = {
                    viewModel.showUploadDialog = false
                    context.makeToast("图片上传失败")
                },
                closeDialog = {
                    viewModel.showUploadDialog = false
                }
            )
            viewModel.resetUploadImage()
        }
    }

}

@Composable
fun ChatBottom(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel,
    onPickImage: () -> Unit,
    onPickOtherImage: () -> Unit,
    onSendMessage: (String) -> Unit,
    clearText: Boolean,
    resetClear: () -> Unit,
    clearFocus: Boolean,
    resetFocus: () -> Unit,
    onClearFocus: () -> Unit,
) {
    val recentList by
    viewModel.recentEmojiData.collectAsStateWithLifecycle(initialValue = emptyList())
    var shouldShowSendBtn by remember { mutableStateOf(false) }
    var showEmojiPanel by remember { mutableStateOf(false) }
    var clickedEmoji by remember { mutableStateOf<String?>(null) }
    var textInput by remember { mutableStateOf(EMPTY_STRING) }
    var pagerState: PagerState
    val context = LocalContext.current

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable {
                        onClearFocus()
                        showEmojiPanel = !showEmojiPanel
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    imageVector = Icons.Outlined.EmojiEmotions,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
            ChatEditText(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 6.dp),
                showSendBtn = { shouldShowSendBtn = it },
                updateInputText = {
                    textInput = it
                },
                clearText = clearText,
                resetClear = resetClear,
                clearFocus = clearFocus,
                resetFocus = resetFocus,
                clickedEmoji = clickedEmoji,
                resetEmoji = {
                    clickedEmoji = null
                },
                onCloseEmojiPanel = {
                    showEmojiPanel = false
                },
            )
            if (!shouldShowSendBtn) {
                Row {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                onPickImage()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            imageVector = Icons.Outlined.AddPhotoAlternate,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                onPickOtherImage()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = androidx.compose.ui.res.painterResource(
                                id = R.drawable.outline_note_alt_24
                            ),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable {
                            onSendMessage(textInput)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        imageVector = Icons.AutoMirrored.Default.Send,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
        if (showEmojiPanel) {
            pagerState = rememberPagerState(
                initialPage = if (recentList.isEmpty()) 1 else 0,
                pageCount = { 3 }
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) { index ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    val pagerState1 = rememberPagerState {
                        when (index) {
                            0 -> 1
                            1 -> 4
                            2 -> 2
                            else -> 0
                        }
                    }
                    HorizontalPager(
                        state = pagerState1,
                        modifier = Modifier.fillMaxWidth()
                    ) { index0 ->
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(28) {
                                val emojiName = when (index) {
                                    0 -> recentList.getOrNull(it)?.data
                                    1 -> emojiList.getOrNull(index0)?.getOrNull(it)?.first
                                    2 -> coolBList.getOrNull(index0)?.getOrNull(it)?.first
                                    else -> null
                                }
                                val emojiRes = when (index) {
                                    0 -> emojiName?.let { name ->
                                        EmojiUtils.emojiMap.getValue(name)
                                    }

                                    1 -> emojiList.getOrNull(index0)?.getOrNull(it)?.second
                                    2 -> coolBList.getOrNull(index0)?.getOrNull(it)?.second
                                    else -> null
                                }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            emojiName?.let { name ->
                                                clickedEmoji = name
                                                if (pagerState.currentPage != 0) {
                                                    viewModel.updateRecentEmoji(
                                                        name,
                                                        recentList.size,
                                                        recentList.lastOrNull()?.data
                                                    )
                                                }
                                            }
                                            if (it == 27) clickedEmoji = "[c001apk]"
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (emojiRes != null) {
                                        Image(
                                            painter = rememberDrawablePainter(
                                                drawable = ResourcesCompat.getDrawable(
                                                    context.resources,
                                                    emojiRes,
                                                    context.theme
                                                )
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    } else if (it == 27) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                            contentDescription = null,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                            }
                        }
                    }
                    CardIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 10.dp),
                        pagerState = pagerState1,
                        defColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    )
                }
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                TextIndicator(
                    index = 0,
                    title = "最近",
                    pagerState = pagerState,
                )
                VerticalDivider()
                TextIndicator(
                    index = 1,
                    title = "默认",
                    pagerState = pagerState,
                )
                VerticalDivider()
                TextIndicator(
                    index = 2,
                    title = "酷币",
                    pagerState = pagerState,
                )
            }
        }
    }
}

@Composable
fun RowScope.TextIndicator(
    modifier: Modifier = Modifier,
    index: Int,
    title: String,
    pagerState: PagerState,
) {
    val scope = rememberCoroutineScope()
    Text(
        text = title,
        modifier = modifier
            .weight(1f)
            .background(
                if (pagerState.currentPage == index)
                    MaterialTheme.colorScheme.primary
                else
                    Color.Transparent
            )
            .clickable {
                scope.launch {
                    pagerState.scrollToPage(index)
                }
            }
            .padding(vertical = 10.dp),
        color = if (pagerState.currentPage == index)
            MaterialTheme.colorScheme.onPrimary
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.titleSmall,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun LoadingDialog() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false)
    ) {
        Surface(
            modifier = Modifier.size(100.dp), shape = RoundedCornerShape(20.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
