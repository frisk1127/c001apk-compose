package com.example.c001apk.compose.ui.main

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.c001apk.compose.R

/**
 * Created by bggRGjQaUbCoE on 2024/5/30
 */
sealed class Router(
    val name: String,
    @param:StringRes val stringId: Int? = null,
    val unselectedIcon: ImageVector? = null,
    val selectedIcon: ImageVector? = null,
) {

    data object MAIN : Router(
        name = "MAIN"
    )

    data object HOME : Router(
        name = "HOME",
        stringId = R.string.home,
        unselectedIcon = Icons.Outlined.Home,
        selectedIcon = Icons.Default.Home
    )

    data object MESSAGE : Router(
        name = "MESSAGE",
        stringId = R.string.message,
        unselectedIcon = Icons.AutoMirrored.Outlined.Message,
        selectedIcon = Icons.AutoMirrored.Filled.Message
    )

    data object SETTINGS : Router(
        name = "SETTINGS",
        stringId = R.string.settings,
        unselectedIcon = Icons.Outlined.Settings,
        selectedIcon = Icons.Default.Settings
    )

    data object PARAMS : Router(name = "PARAMS")

    data object ABOUT : Router(name = "ABOUT")

    data object LICENSE : Router(name = "LICENSE")

    data object BLACKLIST : Router(name = "BLACKLIST")

    data object SEARCH : Router(name = "SEARCH")

    data object SEARCHRESULT : Router(name = "SEARCHRESULT")

    data object TAB : Router(name = "TAB")

    data object FEED : Router(name = "FEED")

    data object USER : Router(name = "USER")

    data object TOPIC : Router(name = "TOPIC")

    data object COPY : Router(name = "COPY")

    data object WEBVIEW : Router(name = "WEBVIEW")

    data object APP : Router(name = "APP")

    data object LOGIN : Router(name = "LOGIN")

    data object CAROUSEL : Router(name = "CAROUSEL")

    data object UPDATE : Router(name = "UPDATE")

    data object FFFLIST : Router(name = "FFFLIST")

    data object DYH : Router(name = "DYH")

    data object COOLPIC : Router(name = "COOLPIC")

    data object NOTICE : Router(name = "NOTICE")

    data object HISTORY : Router(name = "HISTORY")

    data object CHAT : Router(name = "CHAT")

    data object COLLECTION : Router(name = "COLLECTION")

}
