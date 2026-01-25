package com.example.c001apk.compose.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.logic.repository.UserPreferencesRepository
import com.example.c001apk.compose.ui.theme.C001apkComposeTheme
import com.example.c001apk.compose.util.CookieUtil.apiVersion
import com.example.c001apk.compose.util.CookieUtil.imageFilter
import com.example.c001apk.compose.util.CookieUtil.imageQuality
import com.example.c001apk.compose.util.CookieUtil.isDarkMode
import com.example.c001apk.compose.util.CookieUtil.isLogin
import com.example.c001apk.compose.util.CookieUtil.materialYou
import com.example.c001apk.compose.util.CookieUtil.openInBrowser
import com.example.c001apk.compose.util.CookieUtil.recordHistory
import com.example.c001apk.compose.util.CookieUtil.sdkInt
import com.example.c001apk.compose.util.CookieUtil.showEmoji
import com.example.c001apk.compose.util.CookieUtil.showSquare
import com.example.c001apk.compose.util.CookieUtil.szlmId
import com.example.c001apk.compose.util.CookieUtil.token
import com.example.c001apk.compose.util.CookieUtil.uid
import com.example.c001apk.compose.util.CookieUtil.userAgent
import com.example.c001apk.compose.util.CookieUtil.username
import com.example.c001apk.compose.util.CookieUtil.versionCode
import com.example.c001apk.compose.util.CookieUtil.versionName
import com.example.c001apk.compose.util.CookieUtil.xAppDevice
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Created by bggRGjQaUbCoE on 2024/5/29
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var navController: NavHostController
    private val viewModel by viewModels<MainViewModel>()
    private var stableStatusBarInset: Int? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        intent.data?.let {
            navController.onOpenLink(
                context = this,
                url = it.toString(),
                title = null,
                needConvert = true
            )
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupStableStatusBarInsets()

        setContent {
            navController = rememberNavController()

            val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass

            val userPreferences by userPreferencesRepository.data
                .collectAsStateWithLifecycle(
                    initialValue = null,
                    lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                )

            val preferences = if (userPreferences == null) {
                return@setContent
            } else {
                checkNotNull(userPreferences)
            }

            CompositionLocalProvider(
                LocalUserPreferences provides preferences
            ) {
                if (preferences.xAppDevice.isEmpty())
                    viewModel.regenerateParams()

                isLogin = preferences.isLogin
                szlmId = preferences.szlmId
                xAppDevice = preferences.xAppDevice
                uid = preferences.uid
                username = preferences.username
                token = preferences.token
                userAgent = preferences.userAgent
                sdkInt = preferences.sdkInt
                versionName = preferences.versionName
                versionCode = preferences.versionCode
                apiVersion = preferences.apiVersion
                imageQuality = preferences.imageQuality
                showEmoji = preferences.showEmoji
                showSquare = preferences.showSquare
                openInBrowser = preferences.openInBrowser
                imageFilter = preferences.imageFilter
                recordHistory = preferences.recordHistory
                materialYou = preferences.materialYou
                isDarkMode = preferences.isDarkMode()

                C001apkComposeTheme(
                    darkTheme = preferences.isDarkMode(),
                    themeType = preferences.themeType,
                    seedColor = preferences.seedColor,
                    materialYou = preferences.materialYou,
                    pureBlack = preferences.pureBlack,
                    paletteStyle = preferences.paletteStyle,
                    fontScale = preferences.fontScale,
                    contentScale = preferences.contentScale,
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        MainNavigation(
                            navController = navController,
                            badge = viewModel.badge,
                            resetBadge = viewModel::resetBadge,
                            widthSizeClass = widthSizeClass,
                        )
                    }
                }
            }

            LaunchedEffect(key1 = navController) {
                if (savedInstanceState == null) {
                    handleIntent(intent)
                }
            }

        }

    }

    private fun setupStableStatusBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val currentTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val stableTop = stableStatusBarInset
                ?: currentTop.takeIf { it > 0 }?.also { stableStatusBarInset = it }
                ?: 0
            if (stableTop == 0) {
                return@setOnApplyWindowInsetsListener insets
            }
            WindowInsetsCompat.Builder(insets)
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, stableTop, 0, 0))
                .build()
        }
    }
}

