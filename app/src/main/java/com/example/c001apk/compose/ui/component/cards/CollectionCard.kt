package com.example.c001apk.compose.ui.component.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.ui.component.CoilLoader
import com.example.c001apk.compose.ui.ffflist.FFFListType
import com.example.c001apk.compose.ui.theme.cardBg

/**
 * Created by bggRGjQaUbCoE on 2024/6/15
 */
@Composable
fun CollectionCard(
    modifier: Modifier = Modifier,
    data: HomeFeedResponse.Data,
    onViewFFFList: ((String?, String, String?, String?) -> Unit)? = null,
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(cardBg())
            .clickable {
                onViewFFFList?.let {
                    it(
                        null,
                        FFFListType.COLLECTION_ITEM.name,
                        data.id.orEmpty(),
                        data.title.orEmpty()
                    )
                }
            }
            .padding(10.dp)
    ) {
        val (cover, name, type, follow, size) = createRefs()

        CoilLoader(
            url = data.coverPic,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .aspectRatio(4f / 3f)
                .constrainAs(cover) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    height = Dimension.fillToConstraints
                })

        Text(
            text = data.title.orEmpty(),
            modifier = Modifier
                .padding(start = 10.dp)
                .constrainAs(name) {
                    start.linkTo(cover.end)
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall
        )

        Text(
            text = data.isOpenTitle.orEmpty(),
            modifier = Modifier
                .padding(start = 10.dp)
                .constrainAs(type) {
                    start.linkTo(cover.end)
                    top.linkTo(name.bottom)
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = data.followNum.toString() + "人关注",
            modifier = Modifier
                .padding(start = 10.dp)
                .constrainAs(follow) {
                    start.linkTo(type.end)
                    top.linkTo(name.bottom)
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = data.itemNum.toString() + "个内容",
            modifier = Modifier
                .padding(start = 10.dp)
                .constrainAs(size) {
                    start.linkTo(follow.end)
                    top.linkTo(name.bottom)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )

    }
}
