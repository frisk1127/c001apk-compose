package com.example.c001apk.compose.constant

import com.example.c001apk.compose.util.CookieUtil.isDarkMode

/**
 * Created by bggRGjQaUbCoE on 2024/6/3
 */
object Constants {
    const val REQUEST_WITH = "XMLHttpRequest"
    const val LOCALE = "zh-CN"
    const val APP_ID = "com.coolapk.market"
    var DARK_MODE = if (isDarkMode) "1" else "0"
    const val CHANNEL = "coolapk"
    const val MODE = "universal"
    const val APP_LABEL = "token://com.coolapk.market/dcf01e569c1e3db93a3d0fcf191a622c"
    const val VERSION_NAME = "13.4.1"
    const val API_VERSION = "13"
    const val VERSION_CODE = "2312121"

    const val PREFIX_COOLMARKET = "coolmarket://"
    const val PREFIX_HTTP = "http"
    const val PREFIX_APP = "/apk/"
    const val PREFIX_GAME = "/game/"
    const val PREFIX_FEED = "/feed/"
    const val PREFIX_PRODUCT = "/product/"
    const val PREFIX_TOPIC = "/t/"
    const val PREFIX_USER = "/u/"
    const val PREFIX_CAROUSEL = "/page?url="
    const val PREFIX_CAROUSEL1 = "#/" // "#/feed/", "#/topic/", "#/article/"
    const val PREFIX_USER_LIST = "/user/"
    const val PREFIX_DYH = "/dyh/"
    const val PREFIX_COLLECTION = "/collection/"
    const val SUFFIX_THUMBNAIL = ".s.jpg"
    const val SUFFIX_GIF = ".gif"

    const val UTF8 = "UTF-8"
    const val EMPTY_STRING = ""
    const val LOADING_FAILED = "FAILED"
    const val WEB_LOGIN_FAILED = "网页登录失败"
    const val URL_LOGIN = "https://account.coolapk.com/auth/login?type=mobile"
    const val URL_SOURCE_CODE = "https://github.com/bggRGjQaUbCoE/c001apk-compose"
    const val URL_SOURCE_CODE_FORK = "https://github.com/frisk1127/c001apk-compose"

    val entityTypeList =
        listOf(
            "feed",
            "apk",
            "product",
            "user",
            "topic",
            "notification",
            "productBrand",
            "contacts",
            "recentHistory",
            "feed_reply",
            "message",
            "collection",
        )
    val entityTemplateList =
        listOf(
            "imageCarouselCard_1",
            "iconLinkGridCard",
            "iconMiniScrollCard",
            "iconMiniGridCard",
            "imageSquareScrollCard",
            "titleCard",
            "iconScrollCard",
            "imageTextScrollCard",
            "iconTabLinkGridCard",
            "verticalColumnsFullPageCard",
            "noMoreDataCard",
            "time",
        )

    val seedColors = listOf(
        0xFF6650A4,
        0xFFF44336,
        0xFFE91E63,
        0xFF9C27B0,
        0xFF3F51B5,
        0xFF2196F3,
        0xFF03A9F4,
        0xFF00BCD4,
        0xFF009688,
        0xFF4FAF50,
        0xFF8BC3A4,
        0xFFCDDC39,
        0xFFFFEB3B,
        0xFFFFC107,
        0xFFFF9800,
        0xFFFF5722,
        0xFF795548,
        0xFF607D8F,
        0xFFFF9CA8,
    )

}
