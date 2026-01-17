package com.example.c001apk.compose.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.c001apk.compose.R
import com.example.c001apk.compose.ui.component.BackButton
import com.example.c001apk.compose.ui.component.cards.SearchHistoryCard

/**
 * Created by bggRGjQaUbCoE on 2024/6/6
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    title: String?,
    onSearch: (String) -> Unit
) {

    val viewModel = hiltViewModel<SearchViewModel>(key = title)
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle(initialValue = emptyList())

    var textInput by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    val focusRequest = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try {
            focusRequest.requestFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    val textStyle = LocalTextStyle.current
    var showClearDialog by remember { mutableStateOf(false) }
    val layoutDirection = LocalLayoutDirection.current

    fun onCheckSearch(keyword: String = textInput.text) {
        with(keyword) {
            if (this.isNotEmpty()) {
                viewModel.saveHistory(this)
                onSearch(this)
            }
        }
    }

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
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequest),
                        singleLine = true,
                        value = textInput,
                        onValueChange = { textInput = it },
                        textStyle = textStyle.copy(fontSize = 18.sp),
                        placeholder = {
                            val hint = if (!title.isNullOrEmpty()) {
                                stringResource(id = R.string.search_hint_in, title)
                            } else {
                                stringResource(id = R.string.search_hint)
                            }
                            Text(
                                text = hint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        trailingIcon = {
                            AnimatedVisibility(
                                visible = textInput.text.isNotEmpty(),
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                IconButton(onClick = {
                                    textInput = TextFieldValue()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = null
                                    )
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                onCheckSearch()
                            }
                        )
                    )
                },
                actions = {
                    IconButton(onClick = {
                        onCheckSearch()
                    }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier.padding(
                top = paddingValues.calculateTopPadding()
            )
        ) {
            HorizontalDivider()

            androidx.compose.animation.AnimatedVisibility(
                modifier = Modifier.padding(
                    start = paddingValues.calculateLeftPadding(
                        layoutDirection
                    )
                ),
                visible = searchHistory.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "搜索历史", modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    )

                    IconButton(
                        onClick = {
                            showClearDialog = true
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ClearAll, contentDescription = null)
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .padding(start = paddingValues.calculateLeftPadding(layoutDirection))
                    .padding(horizontal = 10.dp),
            ) {
                searchHistory.forEach {
                    SearchHistoryCard(
                        data = it.data,
                        onSearch = {
                            onCheckSearch(it.data)
                        },
                        onDelete = {
                            viewModel.delete(it.data)
                        }
                    )
                }
            }
        }

    }

    when {
        showClearDialog -> {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearDialog = false
                            viewModel.clearAll()
                        }) {
                        Text(text = stringResource(id = android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showClearDialog = false
                        }) {
                        Text(text = stringResource(id = android.R.string.cancel))
                    }
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.search_clear_history_confirm),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }
    }

}
