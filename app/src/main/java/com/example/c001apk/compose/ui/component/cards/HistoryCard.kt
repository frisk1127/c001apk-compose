package com.example.c001apk.compose.ui.component.cards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.example.c001apk.compose.R
import com.example.c001apk.compose.logic.model.FeedEntity
import com.example.c001apk.compose.ui.component.CoilLoader
import com.example.c001apk.compose.ui.component.IconText
import com.example.c001apk.compose.ui.component.LinkText
import com.example.c001apk.compose.ui.theme.cardBg
import com.example.c001apk.compose.util.CookieUtil.isLogin
import com.example.c001apk.compose.util.DateUtils.fromToday
import com.example.c001apk.compose.util.ReportType
import com.example.c001apk.compose.util.ShareType
import com.example.c001apk.compose.util.Utils.richToString
import com.example.c001apk.compose.util.copyText
import com.example.c001apk.compose.util.getShareText

/**
 * Created by bggRGjQaUbCoE on 2024/6/17
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryCard(
    modifier: Modifier = Modifier,
    data: FeedEntity,
    onViewUser: (String) -> Unit,
    onReport: (String, ReportType) -> Unit,
    onDelete: (String) -> Unit,
    onBlockUser: (String) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onCopyText: (String?) -> Unit,
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(cardBg())
            .combinedClickable(
                onClick = {
                    onViewFeed(data.id, false)
                },
                onLongClick = {
                    onCopyText(data.message)
                }
            )
            .padding(start = 10.dp, bottom = 10.dp)
    ) {

        HistoryHeader(
            data = data,
            onViewUser = onViewUser,
            onReport = onReport,
            onDelete = onDelete,
            onBlockUser = onBlockUser
        )

        LinkText(
            text = data.message,
            textSize = 16f,
            lineSpacingMultiplier = 1.3f,
            modifier = modifier.padding(top = 10.dp, end = 10.dp),
            onOpenLink = onOpenLink
        )

    }

}

@Composable
fun HistoryHeader(
    modifier: Modifier = Modifier,
    data: FeedEntity,
    onViewUser: (String) -> Unit,
    onReport: (String, ReportType) -> Unit,
    onDelete: (String) -> Unit,
    onBlockUser: (String) -> Unit,
) {

    var dropdownMenuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        val (avatar, username, dateline, device, expand) = createRefs()

        CoilLoader(
            url = data.avatar,
            modifier = Modifier
                .padding(top = 10.dp)
                .clip(CircleShape)
                .aspectRatio(1f, false)
                .constrainAs(avatar) {
                    top.linkTo(username.top)
                    bottom.linkTo(device.bottom)
                    start.linkTo(parent.start)
                    height = Dimension.fillToConstraints
                }
                .clickable {
                    onViewUser(data.uid)
                },
        )

        Text(
            modifier = Modifier
                .padding(start = 10.dp, top = 10.dp, end = 10.dp)
                .constrainAs(username) {
                    start.linkTo(avatar.end)
                    top.linkTo(parent.top)
                    end.linkTo(expand.start)
                    width = Dimension.fillToConstraints
                },
            text = data.uname,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = fromToday(data.pubDate.toLongOrNull() ?: 0),
            modifier = Modifier
                .padding(start = 10.dp)
                .constrainAs(dateline) {
                    start.linkTo(avatar.end)
                    top.linkTo(username.bottom)
                },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        IconText(
            modifier = Modifier
                .padding(start = 10.dp, end = 10.dp)
                .constrainAs(device) {
                    start.linkTo(dateline.end)
                    bottom.linkTo(dateline.bottom)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                },
            imageVector = Icons.Default.Smartphone,
            title = data.device.richToString(),
            textSize = 13f,
        )

        Box(
            modifier = Modifier
                .aspectRatio(1f, false)
                .constrainAs(expand) {
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                    bottom.linkTo(device.bottom)
                    height = Dimension.fillToConstraints
                }) {

            IconButton(
                onClick = {
                    dropdownMenuExpanded = true
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }

            DropdownMenu(
                expanded = dropdownMenuExpanded,
                onDismissRequest = {
                    dropdownMenuExpanded = false
                },
            ) {
                listOf("复制", "拉黑").forEachIndexed { index, menu ->
                    DropdownMenuItem(
                        text = { Text(menu) },
                        onClick = {
                            dropdownMenuExpanded = false
                            when (index) {
                                0 -> context.copyText(getShareText(ShareType.FEED, data.id))

                                1 -> onBlockUser(data.uid)
                            }
                        }
                    )
                }
                if (isLogin) {
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.menu_report)) },
                        onClick = {
                            dropdownMenuExpanded = false
                            onReport(data.id, ReportType.FEED)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.menu_delete)) },
                    onClick = {
                        dropdownMenuExpanded = false
                        onDelete(data.id)
                    }
                )
            }
        }
    }

}
