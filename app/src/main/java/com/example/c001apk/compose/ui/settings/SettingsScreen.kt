package com.example.c001apk.compose.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Feed
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.ImageAspectRatio
import androidx.compose.material.icons.outlined.InvertColors
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.res.ResourcesCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.c001apk.compose.BuildConfig
import com.example.c001apk.compose.FollowType
import com.example.c001apk.compose.R
import com.example.c001apk.compose.ThemeMode
import com.example.c001apk.compose.ThemeType
import com.example.c001apk.compose.constant.Constants.URL_SOURCE_CODE
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.ui.blacklist.BlackListType
import com.example.c001apk.compose.ui.component.HtmlText
import com.example.c001apk.compose.ui.component.icons.Swatch
import com.example.c001apk.compose.ui.component.settings.BasicListItem
import com.example.c001apk.compose.ui.component.settings.DropdownListItem
import com.example.c001apk.compose.ui.component.settings.SelectionItem
import com.example.c001apk.compose.ui.component.settings.StateDropdownListItem
import com.example.c001apk.compose.ui.component.settings.SwitchListItem
import com.example.c001apk.compose.util.CacheDataManager.clearAllCache
import com.example.c001apk.compose.util.CacheDataManager.getTotalCacheSize
import com.example.c001apk.compose.util.TokenDeviceUtils.encode
import com.example.c001apk.compose.util.Utils.randomMacAddress
import com.example.c001apk.compose.util.openInBrowser
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.materialkolor.PaletteStyle
import java.util.Formatter

/**
 * Created by bggRGjQaUbCoE on 2024/5/30
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onParamsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onViewBlackList: (String) -> Unit,
) {

    val prefs = LocalUserPreferences.current
    val rememberScrollState = rememberScrollState()
    val layoutDirection = LocalLayoutDirection.current
    val context = LocalContext.current

    val imageQualityList by lazy { listOf("网络自适应", "原图", "普清") }
    val themeList by lazy { listOf("跟随系统", "总是开启", "总是关闭") }
    val followList by lazy { listOf("全部", "好友", "话题", "数码", "应用") }

    var dropdownMenuExpanded by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSZLMIDDialog by remember { mutableStateOf(false) }
    var showFontScaleDialog by remember { mutableStateOf(false) }
    var showContentScaleDialog by remember { mutableStateOf(false) }
    var showImageQualityDialog by remember { mutableStateOf(false) }
    var showCleanCacheDialog by remember { mutableStateOf(false) }
    var showThemeTypeDialog by remember { mutableStateOf(false) }
    var cacheSize by remember { mutableStateOf(getTotalCacheSize(context)) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "设置") },
                actions = {
                    Box(Modifier.wrapContentSize(Alignment.TopEnd)) {
                        IconButton(onClick = { dropdownMenuExpanded = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = Icons.Default.MoreVert.name
                            )
                        }

                        DropdownMenu(
                            expanded = dropdownMenuExpanded,
                            onDismissRequest = { dropdownMenuExpanded = false }
                        ) {
                            listOf(
                                stringResource(id = R.string.menu_feedback),
                                stringResource(id = R.string.menu_about)
                            ).forEachIndexed { index, menu ->
                                DropdownMenuItem(
                                    text = { Text(menu) },
                                    onClick = {
                                        dropdownMenuExpanded = false
                                        when (index) {
                                            0 -> context.openInBrowser("$URL_SOURCE_CODE/issues")

                                            1 -> showAboutDialog = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateLeftPadding(layoutDirection),
                    end = paddingValues.calculateRightPadding(layoutDirection)
                )
                .verticalScroll(rememberScrollState)
        ) {

            BasicListItem(leadingText = stringResource(id = R.string.app_name))
            BasicListItem(
                leadingImageVector = Icons.Outlined.Smartphone,
                headlineText = "数字联盟ID",
                supportingText = prefs.szlmId.ifEmpty { null }
            ) {
                showSZLMIDDialog = true
            }
            BasicListItem(
                leadingImageVector = Icons.Outlined.DeveloperMode,
                headlineText = "机型参数",
            ) {
                onParamsClick()
            }

            BasicListItem(leadingText = "主题")
            SwitchListItem(
                value = prefs.materialYou,
                leadingImageVector = Icons.Outlined.Palette,
                headlineText = "系统主题色",
            ) {
                viewModel.setMaterialYou(it)
            }
            StateDropdownListItem(
                isEnable = !prefs.materialYou,
                leadingImageVector = Icons.Outlined.FormatColorFill,
                headlineText = "主题颜色",
                value = prefs.themeType,
                selections = ThemeType.entries.toMutableList()
                    .also { it.remove(ThemeType.UNRECOGNIZED) }.map {
                        SelectionItem(stringResource(id = themeTypeLabelRes(it)), it)
                    },
                onValueChanged = { _, value ->
                    if (value == ThemeType.Custom)
                        showThemeTypeDialog = true
                    viewModel.setThemeType(value)
                }
            )
            StateDropdownListItem(
                isEnable = !prefs.materialYou,
                leadingImageVector = Icons.Rounded.Swatch,
                headlineText = "调色版风格",
                value = prefs.paletteStyle,
                selections = PaletteStyle.entries.mapIndexed { index, label ->
                    SelectionItem(stringResource(id = paletteStyleLabelRes(label)), index)
                },
                onValueChanged = { index, _ ->
                    viewModel.setPaletteStyle(index)
                }
            )
            DropdownListItem(
                leadingImageVector = Icons.Outlined.DarkMode,
                headlineText = "深色主题",
                value = prefs.themeMode.name,
                selections = (0..2).map {
                    SelectionItem(
                        themeList[it],
                        ThemeMode.entries[it].name
                    )
                }/*ThemeMode.entries.map {
                    SelectionItem(it.name, it.name)
                }*/,
                onValueChanged = { index, _ ->
                    viewModel.setDarkTheme(ThemeMode.forNumber(index))
                }
            )
            SwitchListItem(
                value = prefs.pureBlack,
                leadingImageVector = Icons.Outlined.InvertColors,
                headlineText = "纯黑主题",
            ) {
                viewModel.setPureBlack(it)
            }

            BasicListItem(leadingText = "显示")
            BasicListItem(
                leadingImageVector = Icons.Outlined.Block,
                headlineText = "用户黑名单",
            ) {
                onViewBlackList(BlackListType.USER.name)
            }
            BasicListItem(
                leadingImageVector = Icons.Outlined.Block,
                headlineText = "话题黑名单",
            ) {
                onViewBlackList(BlackListType.TOPIC.name)
            }
            BasicListItem(
                leadingImageVector = Icons.Outlined.TextFields,
                headlineText = "字体大小",
                supportingText = "${Formatter().format("%.2f", prefs.fontScale)}x"
            ) {
                showFontScaleDialog = true
            }
            BasicListItem(
                leadingImageVector = Icons.Outlined.ImageAspectRatio,
                headlineText = "内容大小",
                supportingText = "${Formatter().format("%.2f", prefs.contentScale)}x"
            ) {
                showContentScaleDialog = true
            }
            DropdownListItem(
                leadingImageVector = Icons.Outlined.AddCircleOutline,
                headlineText = "关注分组",
                value = prefs.followType.name,
                selections = followList.mapIndexed { index, label ->
                    SelectionItem(label, FollowType.entries[index].name)
                },
                onValueChanged = { index, _ ->
                    viewModel.setFollowType(FollowType.forNumber(index))
                }
            )
            DropdownListItem(
                leadingImageVector = Icons.Outlined.Image,
                headlineText = "图片画质",
                value = prefs.imageQuality,
                selections = imageQualityList.mapIndexed { index, label ->
                    SelectionItem(label, index)
                },
                onValueChanged = { index, _ ->
                    viewModel.setImageQuality(index)
                }
            )
            /*BasicListItem(
                leadingImageVector = Icons.Outlined.Image,
                headlineText = "Image Quality",
                supportingText = imageQualityList[userPreferences.imageQuality]
            ) {
                showImageQualityDialog = true
            }*/
            SwitchListItem(
                value = prefs.imageFilter,
                leadingImageVector = Icons.Outlined.PhotoLibrary,
                headlineText = "压暗图片",
            ) {
                viewModel.setImageFilter(it)
            }
            SwitchListItem(
                value = prefs.openInBrowser,
                leadingImageVector = Icons.Outlined.TravelExplore,
                headlineText = "使用外部浏览器打开链接",
            ) {
                viewModel.setOpenInBrowser(it)
            }
            SwitchListItem(
                value = prefs.showSquare,
                leadingImageVector = Icons.AutoMirrored.Outlined.Feed,
                headlineText = "头条显示广场",
            ) {
                viewModel.setShowSquare(it)
            }
            SwitchListItem(
                value = prefs.recordHistory,
                leadingImageVector = Icons.Outlined.History,
                headlineText = "记录浏览历史",
            ) {
                viewModel.setRecordHistory(it)
            }
            SwitchListItem(
                value = prefs.showEmoji,
                leadingImageVector = Icons.Outlined.EmojiEmotions,
                headlineText = "显示表情",
            ) {
                viewModel.setShowEmoji(it)
            }
            SwitchListItem(
                value = prefs.checkUpdate,
                leadingImageVector = Icons.Outlined.SystemUpdate,
                headlineText = "检查更新",
            ) {
                viewModel.setCheckUpdate(it)
            }
            /*SwitchListItem(
                value = prefs.checkCount,
                leadingImageVector = Icons.Outlined.Notifications,
                headlineText = "检查通知",
            ) {
                viewModel.setCheckCount(it)
            }*/

            BasicListItem(leadingText = "其他")
            BasicListItem(
                leadingImageVector = Icons.Outlined.AllInclusive,
                headlineText = "关于",
                supportingText = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
            ) {
                onAboutClick()
            }
            BasicListItem(
                leadingImageVector = Icons.Outlined.CleaningServices,
                headlineText = "清理缓存",
                supportingText = cacheSize
            ) {
                cacheSize = getTotalCacheSize(context)
                showCleanCacheDialog = true
            }

        }

        when {
            showAboutDialog -> {
                AboutDialog {
                    showAboutDialog = false
                }
            }

            showImageQualityDialog -> {
                ListItemDialog(
                    title = "Image Quality",
                    list = imageQualityList,
                    selected = prefs.imageQuality,
                    onDismiss = {
                        showImageQualityDialog = false
                    },
                    setData = {
                        showImageQualityDialog = false
                        viewModel.setImageQuality(it)
                    }
                )
            }

            showThemeTypeDialog -> {
                EditTextDialog(
                    hint = "6650A4",
                    maxLength = 6,
                    data = prefs.seedColor,
                    title = "自定义主题颜色",
                    onDismiss = {
                        showThemeTypeDialog = false
                    },
                    setData = {
                        viewModel.setSeedColor(it)
                    }
                )
            }

            showSZLMIDDialog -> {
                EditTextDialog(
                    data = prefs.szlmId,
                    title = "数字联盟 ID",
                    onDismiss = {
                        showSZLMIDDialog = false
                    },
                    setData = {
                        viewModel.setSZLMId(it)
                        viewModel.setXAppDevice(
                            encode("$it; ; ; ${randomMacAddress()}; ${prefs.manufacturer}; ${prefs.brand}; ${prefs.model}; ${prefs.buildNumber}; null")
                        )
                    }
                )
            }

            showFontScaleDialog -> {
                SliderDialog(
                    data = prefs.fontScale,
                    title = "内容缩放",
                    hint = "比例",
                    onDismiss = {
                        showFontScaleDialog = false
                    },
                    setData = {
                        viewModel.setFontScale(it)
                    }
                )
            }

            showContentScaleDialog -> {
                SliderDialog(
                    data = prefs.contentScale,
                    title = "内容缩放",
                    hint = "比例",
                    onDismiss = {
                        showContentScaleDialog = false
                    },
                    setData = {
                        viewModel.setContentScale(it)
                    }
                )
            }

            showCleanCacheDialog -> {
                CleanCacheDialog(
                    cacheSize = cacheSize,
                    onDismiss = {
                        showCleanCacheDialog = false
                    },
                    onClean = {
                        clearAllCache(context)
                        cacheSize = "0 B"
                    }
                )
            }

        }
    }

}

@Composable
fun CleanCacheDialog(
    cacheSize: String,
    onDismiss: () -> Unit,
    onClean: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                }) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onClean()
                }
            ) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        text = {
            Text(text = "当前缓存大小: " + cacheSize)
        },
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "清理缓存",
                textAlign = TextAlign.Center
            )
        }
    )
}

@Composable
fun ListItemDialog(
    title: String,
    list: List<String>,
    selected: Int,
    onDismiss: () -> Unit,
    setData: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {},
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                textAlign = TextAlign.Center
            )
        },
        text = {
            LazyColumn {
                itemsIndexed(list) { index, title ->
                    ListItemDialogItem(title, selected == index) {
                        setData(index)
                    }
                }
            }
        },
    )
}

@Composable
fun ListItemDialogItem(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable { onClick() }
    ) {
        RadioButton(selected = selected, onClick = { onClick() })
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterVertically),
            text = title,
        )
    }
}

@Composable
fun EditTextDialog(
    data: String,
    title: String,
    onDismiss: () -> Unit,
    setData: (String) -> Unit,
    hint: String? = null,
    maxLength: Int? = null,
) {
    var text by remember {
        mutableStateOf(
            TextFieldValue(
                text = data,
                selection = TextRange(data.length)
            )
        )
    }
    val focusRequest = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try {
            focusRequest.requestFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    AlertDialog(
        onDismissRequest = { onDismiss() },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                }) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    setData(text.text)
                }) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                textAlign = TextAlign.Center
            )
        },
        text = {
            OutlinedTextField(
                modifier = Modifier.focusRequester(focusRequest),
                value = text,
                onValueChange = {
                    if (maxLength != null) {
                        if (it.text.matches(Regex("^[0-9a-fA-F]{0,6}$")))
                            text = it
                    } else {
                        text = it
                    }
                },
                placeholder = {
                    hint?.let {
                        Text(text = it)
                    }
                }
            )
        },
    )
}

@Composable
fun SliderDialog(
    data: Float,
    title: String,
    hint: String,
    onDismiss: () -> Unit,
    setData: (Float) -> Unit
) {
    var progress by remember { mutableFloatStateOf(data) }
    AlertDialog(
        onDismissRequest = {
            onDismiss()
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    setData(1.0f)
                }) {
                Text(text = stringResource(id = R.string.menu_reset))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    setData(progress)
                }) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                Modifier.fillMaxWidth()
            ) {
                Slider(
                    value = progress,
                    onValueChange = {
                        progress = it
                    },
                    valueRange = 0.8f..1.3f
                )
                Text(
                    text = "$hint: ${Formatter().format("%.2f", progress)}",
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 15.sp * progress,
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = { onDismiss() }) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors()
                .copy(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                val drawable = ResourcesCompat.getDrawable(
                    context.resources,
                    R.mipmap.ic_launcher,
                    context.theme
                )
                Image(
                    painter = rememberDrawablePainter(drawable),
                    contentDescription = "icon",
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {

                    Text(
                        stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    HtmlText(
                        html = stringResource(
                            id = R.string.about_source_code,
                            "<b><a href=\"${URL_SOURCE_CODE}\">GitHub</a></b>"
                        )
                    )
                }
            }
        }
    }
}

private fun themeTypeLabelRes(themeType: ThemeType): Int = when (themeType) {
    ThemeType.Default -> R.string.theme_type_default
    ThemeType.Red -> R.string.theme_type_red
    ThemeType.Pink -> R.string.theme_type_pink
    ThemeType.Purple -> R.string.theme_type_purple
    ThemeType.Indigo -> R.string.theme_type_indigo
    ThemeType.Blue -> R.string.theme_type_blue
    ThemeType.LightBlue -> R.string.theme_type_light_blue
    ThemeType.Cyan -> R.string.theme_type_cyan
    ThemeType.Teal -> R.string.theme_type_teal
    ThemeType.Green -> R.string.theme_type_green
    ThemeType.LightGreen -> R.string.theme_type_light_green
    ThemeType.Lime -> R.string.theme_type_lime
    ThemeType.Yellow -> R.string.theme_type_yellow
    ThemeType.Amber -> R.string.theme_type_amber
    ThemeType.Orange -> R.string.theme_type_orange
    ThemeType.DeepOrange -> R.string.theme_type_deep_orange
    ThemeType.Brown -> R.string.theme_type_brown
    ThemeType.BlueGrey -> R.string.theme_type_blue_grey
    ThemeType.Sakura -> R.string.theme_type_sakura
    ThemeType.Custom -> R.string.theme_type_custom
    ThemeType.UNRECOGNIZED -> R.string.theme_type_default
}

private fun paletteStyleLabelRes(style: PaletteStyle): Int = when (style) {
    PaletteStyle.TonalSpot -> R.string.palette_style_tonal_spot
    PaletteStyle.Neutral -> R.string.palette_style_neutral
    PaletteStyle.Vibrant -> R.string.palette_style_vibrant
    PaletteStyle.Expressive -> R.string.palette_style_expressive
    PaletteStyle.Rainbow -> R.string.palette_style_rainbow
    PaletteStyle.FruitSalad -> R.string.palette_style_fruit_salad
    PaletteStyle.Monochrome -> R.string.palette_style_monochrome
    PaletteStyle.Fidelity -> R.string.palette_style_fidelity
    PaletteStyle.Content -> R.string.palette_style_content
}
