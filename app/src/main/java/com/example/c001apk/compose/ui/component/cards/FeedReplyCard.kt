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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ThumbUpAlt
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.example.c001apk.compose.R
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.ui.base.LikeType
import com.example.c001apk.compose.ui.component.CoilLoader
import com.example.c001apk.compose.ui.component.IconText
import com.example.c001apk.compose.ui.component.LinkText
import com.example.c001apk.compose.ui.component.NineImageView
import com.example.c001apk.compose.ui.theme.cardBg
import com.example.c001apk.compose.util.CookieUtil
import com.example.c001apk.compose.util.CookieUtil.isLogin
import com.example.c001apk.compose.util.DateUtils.fromToday
import com.example.c001apk.compose.util.ReportType

/**
 * Created by bggRGjQaUbCoE on 2024/6/6
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedReplyCard(
    modifier: Modifier = Modifier,
    data: HomeFeedResponse.Data,
    onViewUser: (String) -> Unit,
    onShowTotalReply: ((String, String, String?) -> Unit)? = null, // rid, uid, frid?
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    onReport: ((String, ReportType) -> Unit)? = null,
    isTotalReply: Boolean = false,
    isTopReply: Boolean = false,
    isReply2Reply: Boolean = false,
    onLike: ((String, Int, LikeType) -> Unit)? = null,
    onDelete: ((String, LikeType, String?) -> Unit)? = null,
    onBlockUser: (String, String?) -> Unit,
    onReply: ((String, String, String, String?) -> Unit)? = null, // rid, uid/fuid, uname, frid?
) {

    var dropdownMenuExpanded by remember { mutableStateOf(false) }

    val isFeedReply by lazy { data.fetchType == "feed_reply" }
    val isLikeReply by lazy { data.likeUserInfo != null }
    val horizontal by lazy { if (isFeedReply) 16.dp else 10.dp }
    val vertical by lazy { if (isFeedReply) 12.dp else 10.dp }

    ConstraintLayout(
        modifier = modifier
            .padding(horizontal = if (isFeedReply) 0.dp else 10.dp)
            .fillMaxWidth()
            .clip(if (isFeedReply) RectangleShape else MaterialTheme.shapes.medium)
            .background(
                if ((isTotalReply && !isTopReply) || !isFeedReply)
                    cardBg()
                else
                    Color.Transparent
            )
            .combinedClickable(
                onClick = {
                    if (isLikeReply) {
                        // no action
                    } else if (isFeedReply) {
                        if (isLogin) {
                            onReply?.let {
                                it(
                                    data.id.orEmpty(),
                                    data.uid.orEmpty(),
                                    data.userInfo?.username.orEmpty(),
                                    null
                                )
                            }
                        }
                    } else {
                        onOpenLink(data.url.orEmpty(), null)
                    }
                },
                onLongClick = {
                    onCopyText(data.message)
                }
            )
            .padding(start = horizontal, bottom = vertical)
    ) {

        val (avatar, username, expand, message, image, dateLine, reply, like, replyRows, likeReply, feed) = createRefs()

        CoilLoader(
            url = data.likeUserInfo?.userAvatar ?: data.userAvatar,
            modifier = Modifier
                .padding(top = vertical)
                .size(30.dp)
                .clip(CircleShape)
                .constrainAs(avatar) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                }
                .clickable {
                    onViewUser(data.likeUserInfo?.uid ?: data.uid.orEmpty())
                }
        )

        LinkText(
            text = data.likeUserInfo?.username ?: data.username.orEmpty(),
            modifier = Modifier
                .padding(start = 10.dp, top = vertical)
                .constrainAs(username) {
                    start.linkTo(avatar.end)
                    top.linkTo(avatar.top)
                    end.linkTo(if (!isLikeReply) expand.start else dateLine.start)
                    width = Dimension.fillToConstraints
                    if (isLikeReply || data.feed != null)
                        bottom.linkTo(avatar.bottom)
                },
            maxLines = if (!isFeedReply) 1 else if (data.replyRows == null) null else 1,
            onOpenLink = onOpenLink,
        )

        LinkText(
            text = if (isReply2Reply)
                data.message?.substring(data.message.indexOfFirst { it == ':' } + 1)
            else if (isLikeReply) "赞了你的${data.infoHtml}"
            else if (!isFeedReply) {
                if (data.ruid == "0") data.message.orEmpty()
                else "回复<a class=\"feed-link-uname\" href=\"/u/" +
                        data.ruid +
                        "\">" +
                        data.rusername +
                        "</a>: " +
                        data.message
            } else data.message.orEmpty(),
            modifier = Modifier
                .padding(
                    start = if (!isLikeReply && data.feed == null) 10.dp else 0.dp,
                    top = if (!isLikeReply && data.feed == null) 5.dp else 10.dp,
                    end = horizontal
                )
                .constrainAs(message) {
                    start.linkTo(if (!isLikeReply && data.feed == null) avatar.end else parent.start)
                    top.linkTo(if (!isLikeReply && data.feed == null) username.bottom else avatar.bottom)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                },
            lineSpacingMultiplier = 1.2f,
            onOpenLink = onOpenLink,
        )

        Text(
            text = fromToday(if (isLikeReply) (data.likeTime ?: 0) else (data.dateline ?: 0)),
            modifier = Modifier
                .padding(
                    start = if (isLikeReply || data.feed != null) 0.dp else 10.dp,
                    top = 10.dp,
                    end = if (isLikeReply) 10.dp else 0.dp
                )
                .constrainAs(dateLine) {
                    if (!isLikeReply && data.feed != null)
                        start.linkTo(parent.start)
                    else if (isFeedReply)
                        start.linkTo(avatar.end)
                    top.linkTo(
                        if (isLikeReply) parent.top
                        else {
                            if (data.feed != null)
                                feed.bottom
                            else if (!data.picArr.isNullOrEmpty())
                                image.bottom
                            else
                                message.bottom
                        }
                    )
                    if (isLikeReply)
                        end.linkTo(parent.end)
                },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.outline
        )

        if (isLikeReply) {
            ConstraintLayout(
                modifier = Modifier
                    .padding(top = 10.dp, end = 10.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface)
                    .constrainAs(likeReply) {
                        start.linkTo(parent.start)
                        top.linkTo(message.bottom)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                    }
                    .clickable {
                        onOpenLink(data.url.orEmpty(), null)
                    }
                    .padding(10.dp)
            ) {
                val (pic, name, msg) = createRefs()
                if (!data.pic.isNullOrEmpty()) {
                    CoilLoader(
                        url = data.pic,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .aspectRatio(1f)
                            .constrainAs(pic) {
                                start.linkTo(parent.start)
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom)
                                height = Dimension.fillToConstraints
                            }
                    )
                }

                Text(
                    text = "@${data.username}",
                    modifier = Modifier
                        .padding(start = if (data.pic.isNullOrEmpty()) 0.dp else 10.dp)
                        .constrainAs(name) {
                            top.linkTo(parent.top)
                            start.linkTo(if (data.pic.isNullOrEmpty()) parent.start else pic.end)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                )

                LinkText(
                    text = data.message.orEmpty(),
                    onOpenLink = onOpenLink,
                    textSize = 12f,
                    color = MaterialTheme.colorScheme.outline.toArgb(),
                    maxLines = 1,
                    modifier = Modifier
                        .padding(start = if (data.pic.isNullOrEmpty()) 0.dp else 10.dp)
                        .constrainAs(msg) {
                            top.linkTo(name.bottom)
                            start.linkTo(if (data.pic.isNullOrEmpty()) parent.start else pic.end)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                        }
                )

            }
        }

        if (!isLikeReply) {
            if (!data.picArr.isNullOrEmpty()) {
                NineImageView(
                    pic = data.pic,
                    picArr = data.picArr,
                    feedType = data.feedType,
                    modifier = Modifier
                        .padding(
                            start = if (data.feed != null) 0.dp else 10.dp,
                            top = 10.dp,
                            end = horizontal
                        )
                        .clip(MaterialTheme.shapes.medium)
                        .constrainAs(image) {
                            start.linkTo(if (data.feed != null) parent.start else avatar.end)
                            top.linkTo(message.bottom)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                        }
                )
            }

            if (data.feed != null) {
                ConstraintLayout(
                    modifier = Modifier
                        .padding(top = 10.dp, end = 10.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surface)
                        .constrainAs(feed) {
                            start.linkTo(parent.start)
                            top.linkTo(
                                if (data.picArr.isNullOrEmpty())
                                    message.bottom
                                else
                                    image.bottom
                            )
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                        }
                        .clickable {
                            onOpenLink(data.feed.url.orEmpty(), null)
                        }
                        .padding(10.dp)
                ) {
                    val (pic, name, msg) = createRefs()
                    if (!data.feed.pic.isNullOrEmpty()) {
                        CoilLoader(
                            url = data.feed.pic,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .aspectRatio(1f)
                                .constrainAs(pic) {
                                    start.linkTo(parent.start)
                                    top.linkTo(parent.top)
                                    bottom.linkTo(parent.bottom)
                                    height = Dimension.fillToConstraints
                                }
                        )
                    }

                    Text(
                        text = "@${data.feed.username}",
                        modifier = Modifier
                            .padding(start = if (data.feed.pic.isNullOrEmpty()) 0.dp else 10.dp)
                            .constrainAs(name) {
                                top.linkTo(parent.top)
                                start.linkTo(if (data.feed.pic.isNullOrEmpty()) parent.start else pic.end)
                                end.linkTo(parent.end)
                                width = Dimension.fillToConstraints
                            },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                    )

                    LinkText(
                        text = data.feed.message.orEmpty(),
                        onOpenLink = onOpenLink,
                        textSize = 12f,
                        color = MaterialTheme.colorScheme.outline.toArgb(),
                        maxLines = 1,
                        modifier = Modifier
                            .padding(start = if (data.feed.pic.isNullOrEmpty()) 0.dp else 10.dp)
                            .constrainAs(msg) {
                                top.linkTo(name.bottom)
                                start.linkTo(if (data.feed.pic.isNullOrEmpty()) parent.start else pic.end)
                                end.linkTo(parent.end)
                                width = Dimension.fillToConstraints
                            }
                    )

                }
            }

            IconText(
                modifier = Modifier
                    .padding(end = 10.dp, top = 10.dp)
                    .constrainAs(reply) {
                        top.linkTo(
                            if (data.feed != null)
                                feed.bottom
                            else if (!data.picArr.isNullOrEmpty())
                                image.bottom
                            else
                                message.bottom
                        )
                        end.linkTo(like.start)
                    },
                imageVector = Icons.AutoMirrored.Outlined.Message,
                title = data.replynum.orEmpty(),
            )

            IconText(
                modifier = Modifier
                    .padding(top = 10.dp, end = horizontal)
                    .constrainAs(like) {
                        top.linkTo(
                            if (data.feed != null)
                                feed.bottom
                            else if (!data.picArr.isNullOrEmpty())
                                image.bottom
                            else
                                message.bottom
                        )
                        end.linkTo(parent.end)
                    },
                imageVector = if (data.userAction?.like == 1) Icons.Filled.ThumbUpAlt
                else Icons.Default.ThumbUpOffAlt,
                title = data.likenum.orEmpty(),
                onClick = {
                    if (isLogin) {
                        onLike?.let {
                            it(data.id.orEmpty(), data.userAction?.like ?: 0, LikeType.REPLY)
                        }
                    }
                },
                isLike = data.userAction?.like == 1,
            )

            Box(
                modifier = Modifier
                    .constrainAs(expand) {
                        top.linkTo(parent.top)
                        end.linkTo(parent.end)
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
                    listOf("拉黑", "显示回复").forEachIndexed { index, menu ->
                        DropdownMenuItem(
                            text = { Text(menu) },
                            onClick = {
                                dropdownMenuExpanded = false
                                when (index) {
                                    0 -> onBlockUser(data.uid.orEmpty(), null)

                                    1 -> onShowTotalReply?.let {
                                        it(
                                            data.id.orEmpty(),
                                            data.uid.orEmpty(),
                                            null,
                                        )
                                    }
                                }
                            }
                        )
                    }
                    if (isLogin) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.menu_report)) },
                            onClick = {
                                dropdownMenuExpanded = false
                                onReport?.let { it(data.id.orEmpty(), ReportType.REPLY) }
                            }
                        )
                    }
                    if (data.uid == CookieUtil.uid) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                dropdownMenuExpanded = false
                                onDelete?.let {
                                    it(data.id.orEmpty(), LikeType.REPLY, null)
                                }
                            }
                        )
                    }
                }

            }

            if (!isTotalReply
                && (!data.replyRows.isNullOrEmpty() || (data.replyRowsMore ?: 0) > 0)
            ) {
                ReplyRows(
                    modifier = Modifier
                        .padding(start = 10.dp, top = 10.dp, end = 16.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(cardBg())
                        .constrainAs(replyRows) {
                            top.linkTo(dateLine.bottom)
                            start.linkTo(avatar.end)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                        },
                    data = data.replyRows,
                    replyRowsMore = data.replyRowsMore ?: 0,
                    replyNum = data.replynum ?: EMPTY_STRING,
                    onShowTotalReply = { id ->
                        onShowTotalReply?.let {
                            it(
                                id ?: data.id.orEmpty(),
                                data.uid.orEmpty(),
                                if (!id.isNullOrEmpty()) data.id.orEmpty() else null
                            )
                        }
                    },
                    onOpenLink = onOpenLink,
                    onCopyText = onCopyText,
                    onReport = onReport,
                    onBlockUser = { uid ->
                        onBlockUser(uid, if (data.uid == uid) null else data.id.orEmpty())
                    },
                    onDelete = { id, likeType ->
                        onDelete?.let {
                            it(id, likeType, data.id.orEmpty())
                        }
                    },
                    onReply = { rid, runame ->
                        onReply?.let {
                            it(rid, data.uid.orEmpty(), runame, data.id.orEmpty())
                        }
                    }
                )
            }
        }

    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReplyRows(
    modifier: Modifier = Modifier,
    data: List<HomeFeedResponse.Data>?,
    replyRowsMore: Int,
    replyNum: String,
    onShowTotalReply: (String?) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    onReport: ((String, ReportType) -> Unit)? = null,
    onBlockUser: (String) -> Unit,
    onDelete: ((String, LikeType) -> Unit)? = null,
    onReply: (String, String) -> Unit
) {

    var dropdownMenuExpanded by remember { mutableIntStateOf(-1) }

    Column(
        modifier = modifier
    ) {
        data?.forEachIndexed { index, reply ->
            Box {
                LinkText(
                    text = reply.message.orEmpty(),
                    lineSpacingMultiplier = 1.2f,
                    textSize = 14f,
                    onOpenLink = onOpenLink,
                    isReply = true,
                    onShowTotalReply = {
                        onShowTotalReply(null)
                    },
                    imgList = reply.picArr,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (isLogin) {
                                    onReply(
                                        reply.id.orEmpty(),
                                        reply.username.orEmpty()
                                    )
                                }
                            },
                            onLongClick = {
                                dropdownMenuExpanded = index
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
                DropdownMenu(
                    expanded = dropdownMenuExpanded == index,
                    onDismissRequest = { dropdownMenuExpanded = -1 }
                ) {
                    listOf(
                        "复制",
                        "拉黑",
                        "显示回复"
                    ).forEachIndexed { index, menu ->
                        DropdownMenuItem(
                            text = { Text(menu) },
                            onClick = {
                                dropdownMenuExpanded = -1
                                when (index) {
                                    0 -> onCopyText(reply.message)

                                    1 -> onBlockUser(reply.uid.orEmpty())

                                    2 -> onShowTotalReply(reply.id.orEmpty())
                                }
                            }
                        )
                    }
                    if (isLogin) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.menu_report)) },
                            onClick = {
                                dropdownMenuExpanded = -1
                                onReport?.let { it(reply.id.orEmpty(), ReportType.REPLY) }
                            }
                        )
                    }
                    if (reply.uid == CookieUtil.uid) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                dropdownMenuExpanded = -1
                                onDelete?.let { it(reply.id.orEmpty(), LikeType.REPLY) }
                            }
                        )
                    }
                }
            }

        }
        if (replyRowsMore != 0) {
            LinkText(
                text = "查看更多回复(" + replyNum + ")",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onShowTotalReply(null)
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.primary.toArgb(),
                textSize = 14f,
                onOpenLink = onOpenLink,
            )
        }
    }

}
