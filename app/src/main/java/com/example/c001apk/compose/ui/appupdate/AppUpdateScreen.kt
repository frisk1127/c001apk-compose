package com.example.c001apk.compose.ui.appupdate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.c001apk.compose.R
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.logic.model.UpdateCheckItem
import com.example.c001apk.compose.logic.state.LoadingState
import com.example.c001apk.compose.ui.app.AppViewModel
import com.example.c001apk.compose.ui.component.BackButton
import com.example.c001apk.compose.ui.component.cards.AppUpdateCard
import com.example.c001apk.compose.ui.component.cards.LoadingCard
import com.example.c001apk.compose.util.Utils.getBase64
import com.example.c001apk.compose.util.downloadApk
import org.json.JSONObject

/**
 * Created by bggRGjQaUbCoE on 2024/6/12
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateScreen(
    onBackClick: () -> Unit,
    data: List<UpdateCheckItem>?,
    onViewApp: (String) -> Unit,
) {

    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val viewModel =
        hiltViewModel<AppViewModel, AppViewModel.ViewModelFactory> { factory ->
            factory.create(EMPTY_STRING)
        }
    var count by remember { mutableIntStateOf(0) }

    fun checkUpdate() {
        data?.let { data ->
            val updateCheckJsonObject = JSONObject()
            data.forEach {
                updateCheckJsonObject.put(it.key, it.value)
            }
            viewModel.fetchAppsUpdate(updateCheckJsonObject.toString().getBase64(false))
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.updateState is LoadingState.Loading) {
            checkUpdate()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    BackButton { onBackClick() }
                },
                title = {
                    val title = if (count == 0) {
                        stringResource(id = R.string.update_title)
                    } else {
                        stringResource(id = R.string.update_title_with_count, count)
                    }
                    Text(text = title)
                },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateLeftPadding(layoutDirection),
                ),
            contentPadding = PaddingValues(
                start = 10.dp, end = 10.dp, top = 10.dp,
                bottom = 10.dp + paddingValues.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            when (viewModel.updateState) {
                LoadingState.Loading, LoadingState.Empty, is LoadingState.Error -> {
                    item(key = "updateState") {
                        Box(modifier = Modifier.fillParentMaxSize()) {
                            LoadingCard(
                                modifier = Modifier
                                    .align(Alignment.Center),
                                state = viewModel.updateState,
                                onClick = if (viewModel.updateState is LoadingState.Loading) null
                                else ::checkUpdate
                            )
                        }
                    }
                }

                is LoadingState.Success -> {
                    (viewModel.updateState as? LoadingState.Success)?.response?.let {
                        count = it.size
                        itemsIndexed(it,
                            key = { _, item -> item.packageName + item.id }) { _, item ->
                            AppUpdateCard(
                                data = item,
                                onViewApp = onViewApp,
                                onDownloadApk = { packageName, id, title, versionName, versionCode ->
                                    viewModel.packageName = packageName
                                    viewModel.id = id
                                    viewModel.title = title
                                    viewModel.versionName = versionName
                                    viewModel.versionCode = versionCode
                                    viewModel.onGetDownloadLink()
                                }
                            )
                        }
                    }
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

}
