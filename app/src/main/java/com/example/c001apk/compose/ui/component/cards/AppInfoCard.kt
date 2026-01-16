package com.example.c001apk.compose.ui.component.cards

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.ui.component.CoilLoader
import com.example.c001apk.compose.util.DateUtils.fromToday

/**
 * Created by bggRGjQaUbCoE on 2024/6/4
 */
@Composable
fun AppInfoCard(
    modifier: Modifier = Modifier,
    data: HomeFeedResponse.Data?,
    onDownloadApk: () -> Unit,
) {

    ConstraintLayout(
        modifier = modifier.fillMaxWidth()
    ) {

        val (logo, appName, version, size, updateTime, downloadBtn) = createRefs()

        CoilLoader(
            url = data?.logo,
            modifier = Modifier
                .padding(start = 20.dp)
                .height(80.dp)
                .width(80.dp)
                .clip(RoundedCornerShape(18.dp))
                .constrainAs(logo) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
        )

        Text(
            text = data?.title.orEmpty(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier
                .padding(start = 10.dp, top = 20.dp, end = 10.dp)
                .constrainAs(appName) {
                    start.linkTo(logo.end)
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
        )

        Text(
            text = data?.apkversionname?.let { "版本: ${data.apkversionname}(${data.apkversioncode})" }
                ?: EMPTY_STRING,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
            modifier = Modifier
                .padding(start = 10.dp, top = 5.dp, end = 10.dp)
                .constrainAs(version) {
                    start.linkTo(logo.end)
                    top.linkTo(appName.bottom)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
        )

        Text(
            text = data?.apksize?.let { "大小: " + data.apksize } ?: EMPTY_STRING,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = 10.dp, top = 5.dp, end = 10.dp)
                .constrainAs(size) {
                    start.linkTo(logo.end)
                    top.linkTo(version.bottom)
                    end.linkTo(if (data?.entityType == "apk") downloadBtn.start else parent.end)
                    width = Dimension.fillToConstraints
                }
        )

        Text(
            text = data?.let {
                "更新时间: ${if (data.lastupdate != null) fromToday(data.lastupdate) else "null"}"
            } ?: EMPTY_STRING,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = 10.dp, top = 5.dp, end = 10.dp, bottom = 20.dp)
                .constrainAs(updateTime) {
                    start.linkTo(logo.end)
                    top.linkTo(size.bottom)
                    end.linkTo(if (data?.entityType == "apk") downloadBtn.start else parent.end)
                    width = Dimension.fillToConstraints
                }
        )

        if (data?.entityType == "apk") {
            FilledTonalButton(
                onClick = {
                    onDownloadApk()
                },
                modifier = Modifier
                    .padding(end = 10.dp, bottom = 20.dp)
                    .constrainAs(downloadBtn) {
                        bottom.linkTo(parent.bottom)
                        end.linkTo(parent.end)
                    },
            ) {
                Text(text = "下载")
            }

        }

    }

}
