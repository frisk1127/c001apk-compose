package com.example.c001apk.compose

import android.app.Application
import coil.Coil
import coil.ImageLoader
import com.example.c001apk.compose.util.CookieUtil.materialYou
import com.example.c001apk.compose.util.AddCookiesInterceptor
import com.example.c001apk.compose.util.dp
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import me.zhanghai.android.appiconloader.coil.AppIconKeyer
import com.example.c001apk.compose.util.MojitoOkHttpImageLoader
import net.mikaelzero.mojito.Mojito
import net.mikaelzero.mojito.view.sketch.SketchImageLoadFactory
import okhttp3.OkHttpClient

/**
 * Created by bggRGjQaUbCoE on 2024/5/29
 */
lateinit var c001Application: C001Application

@HiltAndroidApp
class C001Application : Application() {

    override fun onCreate() {
        super.onCreate()

        c001Application = this
        if (materialYou) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }

        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .crossfade(true)
                .okHttpClient {
                    OkHttpClient.Builder()
                        .addInterceptor(AddCookiesInterceptor)
                        .build()
                }
                .components {
                    add(AppIconKeyer())
                    add(AppIconFetcher.Factory(48.dp, false, this@C001Application))
                }
                .build()
        )

        Mojito.initialize(
            MojitoOkHttpImageLoader(this),
            SketchImageLoadFactory()
        )

    }

}
