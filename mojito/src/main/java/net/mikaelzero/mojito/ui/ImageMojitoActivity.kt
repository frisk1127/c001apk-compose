package net.mikaelzero.mojito.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.gyf.immersionbar.ImmersionBar
import net.mikaelzero.mojito.BuildConfig
import net.mikaelzero.mojito.MojitoView
import net.mikaelzero.mojito.Mojito
import net.mikaelzero.mojito.bean.ActivityConfig
import net.mikaelzero.mojito.bean.FragmentConfig
import net.mikaelzero.mojito.bean.ViewPagerBean
import net.mikaelzero.mojito.bean.ViewParams
import net.mikaelzero.mojito.databinding.ActivityImageBinding
import net.mikaelzero.mojito.interfaces.ActivityCoverLoader
import net.mikaelzero.mojito.interfaces.IIndicator
import net.mikaelzero.mojito.interfaces.IMojitoActivity
import net.mikaelzero.mojito.interfaces.IMojitoFragment
import net.mikaelzero.mojito.interfaces.IProgress
import net.mikaelzero.mojito.interfaces.OnMojitoListener
import net.mikaelzero.mojito.loader.FragmentCoverLoader
import net.mikaelzero.mojito.loader.InstanceLoader
import net.mikaelzero.mojito.loader.MultiContentLoader
import net.mikaelzero.mojito.tools.DataWrapUtil
import java.lang.ref.WeakReference


class ImageMojitoActivity : AppCompatActivity(), IMojitoActivity {
    private lateinit var binding: ActivityImageBinding
    private var viewParams: List<ViewParams>? = null
    lateinit var activityConfig: ActivityConfig
    private lateinit var imageViewPagerAdapter: MojitoPagerAdapter
    private var backInvokedCallback: OnBackInvokedCallback? = null
    private var backHandled = false
    private var viewPagerScrollState = ViewPager2.SCROLL_STATE_IDLE
    private var isStatusBarHidden = false
    private var finishPosted = false
    private var isFinishingPreview = false
    private val updateStatusBarRunnable = Runnable { updateStatusBarForCurrentImage() }
    private var lastSelectedPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (BuildConfig.DEBUG) {
            Log.d("MojitoStatusBar", "onCreate")
        }
        if (Mojito.mojitoConfig().transparentNavigationBar()) {
            ImmersionBar.with(this).transparentBar().init()
        } else {
            ImmersionBar.with(this).transparentStatusBar().init()
        }
        binding = ActivityImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.userCustomLayout.removeAllViews()
        activityCoverLoader?.apply {
            attach(this@ImageMojitoActivity)
            binding.userCustomLayout.addView(providerView())
        }

        if (DataWrapUtil.config == null) {
            finish()
            return
        }
        activityConfig = DataWrapUtil.get()!!
        val currentPosition = activityConfig.position
        viewParams = activityConfig.viewParams

        val viewPagerBeans = mutableListOf<ViewPagerBean>()
        if (activityConfig.originImageUrls == null) {
            finish()
            return
        }
        for (i in activityConfig.originImageUrls!!.indices) {
            var targetImageUrl: String? = null
            if (activityConfig.targetImageUrls != null) {
                if (i < activityConfig.targetImageUrls!!.size) {
                    targetImageUrl = activityConfig.targetImageUrls!![i]
                }
            }

            val model = when {
                viewParams == null -> {
                    null
                }

                i >= viewParams!!.size -> {
                    null
                }

                else -> {
                    viewParams?.get(i)
                }
            }
            viewPagerBeans.add(
                ViewPagerBean(
                    activityConfig.originImageUrls!![i],
                    targetImageUrl, i,
                    currentPosition != i,
                    model
                )
            )
        }
        imageViewPagerAdapter = MojitoPagerAdapter(this, viewPagerBeans)
        binding.viewPager.adapter = imageViewPagerAdapter
        binding.viewPager.setCurrentItem(currentPosition, false)
        lastSelectedPosition = currentPosition
        binding.viewPager.post {
            findFragment(currentPosition)?.onPageHidden(false)
        }
        binding.viewPager.post(updateStatusBarRunnable)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backHandled) {
                    return
                }
                backHandled = true
                backToMin()
            }
        })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backInvokedCallback = OnBackInvokedCallback {
                if (backHandled) {
                    return@OnBackInvokedCallback
                }
                backHandled = true
                backToMin()
            }
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                backInvokedCallback!!
            )
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                viewPagerScrollState = state
                if (BuildConfig.DEBUG) {
                    Log.d("MojitoPager", "onPageScrollStateChanged state=$state current=${binding.viewPager.currentItem}")
                }
            }

            override fun onPageSelected(position: Int) {
                activityCoverLoader?.pageChange(viewPagerBeans.size, position)
                onMojitoListener?.onViewPageSelected(position)
                if (lastSelectedPosition != position) {
                    if (BuildConfig.DEBUG) {
                        Log.d(
                            "MojitoPager",
                            "onPageSelected from=$lastSelectedPosition to=$position"
                        )
                    }
                    findFragment(lastSelectedPosition)?.onPageHidden(true)
                    findFragment(position)?.onPageHidden(false)
                    lastSelectedPosition = position
                }
                if (BuildConfig.DEBUG) {
                    Log.d("MojitoStatusBar", "onPageSelected: position=$position")
                }
                binding.viewPager.removeCallbacks(updateStatusBarRunnable)
                binding.viewPager.post(updateStatusBarRunnable)
            }
        })
        activityCoverLoader?.pageChange(viewPagerBeans.size, activityConfig.position)
        if (!activityConfig.originImageUrls.isNullOrEmpty()) {
            iIndicator?.attach(binding.indicatorLayout)
            iIndicator?.onShow(binding.viewPager.viewPager)
        }
        Mojito.currentActivity = WeakReference<ImageMojitoActivity>(this)
    }

    fun setViewPagerLock(isLock: Boolean) {
        binding.viewPager.setLocked(isLock)
    }

    fun finishView() {
        if (finishPosted) {
            return
        }
        finishPosted = true
        isFinishingPreview = true
        if (BuildConfig.DEBUG) {
            Log.d("MojitoStatusBar", "finishView")
        }
        binding.viewPager.removeCallbacks(updateStatusBarRunnable)
        restoreSystemBars()
        binding.root.post { finishInternal() }
    }

    private fun finishInternal() {
        if (BuildConfig.DEBUG) {
            Log.d("MojitoStatusBar", "finishInternal")
        }
        progressLoader = null
        fragmentCoverLoader = null
        multiContentLoader = null
        iIndicator = null
        activityCoverLoader = null
        onMojitoListener = null
        viewParams = null
        Mojito.clean()
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    fun backToMin() {
        // Block status bar toggles during exit animation to avoid flicker.
        beginExitIfNeeded()
        (getCurrentFragment() as? ImageMojitoFragment)?.backToMin()
    }

    fun updateStatusBarForCurrentImage() {
        if (isFinishingPreview) {
            return
        }
        val fragment = getCurrentFragment() as? ImageMojitoFragment ?: return
        val shouldHide = fragment.shouldHideStatusBar()
        if (BuildConfig.DEBUG) {
            Log.d(
                "MojitoStatusBar",
                "updateStatusBarForCurrentImage: shouldHide=$shouldHide hidden=$isStatusBarHidden info=${fragment.getStatusBarOverlapInfo()}"
            )
        }
        if (shouldHide == isStatusBarHidden) {
            return
        }
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (shouldHide) {
            controller.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            controller.show(WindowInsetsCompat.Type.statusBars())
        }
        isStatusBarHidden = shouldHide
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK) {
            (getCurrentFragment() as? ImageMojitoFragment)?.backToMin()
            true
        } else super.onKeyDown(keyCode, event)
    }

    private inner class MojitoPagerAdapter(
        fa: FragmentActivity,
        private val items: List<ViewPagerBean>
    ) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = items.size

        override fun createFragment(position: Int): Fragment {
            val item = items[position]
            val fragmentConfig = FragmentConfig(
                item.url,
                item.targetUrl,
                item.viewParams,
                position,
                activityConfig.autoLoadTarget,
                item.showImmediately,
                activityConfig.errorDrawableResIdList[position] ?: 0
            )
            return ImageMojitoFragment.newInstance(fragmentConfig)
        }
    }

    private fun findFragment(position: Int): ImageMojitoFragment? {
        val itemId = try {
            imageViewPagerAdapter.getItemId(position)
        } catch (_: Exception) {
            position.toLong()
        }
        val tag = "f$itemId"
        return supportFragmentManager.findFragmentByTag(tag) as? ImageMojitoFragment
    }

    companion object {
        var hasShowedAnimMap = hashMapOf<Int, Boolean>()
        var progressLoader: InstanceLoader<IProgress>? = null
        var fragmentCoverLoader: InstanceLoader<FragmentCoverLoader>? = null
        var multiContentLoader: MultiContentLoader? = null
        var iIndicator: IIndicator? = null
        var activityCoverLoader: ActivityCoverLoader? = null
        var lastGlobalLongPressTime: Long = 0L
        var onMojitoListener: OnMojitoListener? = null
    }

    override fun getCurrentFragment(): IMojitoFragment? {
        return findFragment(binding.viewPager.currentItem)
    }

    override fun getContext(): Context {
        return this
    }

    override fun onDestroy() {
        if (BuildConfig.DEBUG) {
            Log.d("MojitoStatusBar", "onDestroy")
        }
        super.onDestroy()
        binding.viewPager.removeCallbacks(updateStatusBarRunnable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backInvokedCallback?.let { onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it) }
            backInvokedCallback = null
        }
        if (isStatusBarHidden) {
            restoreSystemBars()
        }
        if (isFinishing && onMojitoListener != null) {
            onMojitoListener?.onMojitoViewFinish(binding.viewPager.currentItem)
        }

        progressLoader = null
        fragmentCoverLoader = null
        multiContentLoader = null
        iIndicator = null
        activityCoverLoader = null
        onMojitoListener = null
        viewParams = null

    }

    fun restoreSystemBars() {
        if (BuildConfig.DEBUG) {
            Log.d("MojitoStatusBar", "restoreSystemBars")
        }
        WindowInsetsControllerCompat(window, window.decorView)
            .show(WindowInsetsCompat.Type.statusBars())
        isStatusBarHidden = false
    }

    fun beginExitIfNeeded() {
        if (isFinishingPreview) {
            return
        }
        isFinishingPreview = true
        binding.viewPager.removeCallbacks(updateStatusBarRunnable)
    }

    fun tryDispatchLongPress(x: Float, y: Float): Boolean {
        val fragment = getCurrentFragment() as? ImageMojitoFragment ?: return false
        val originUrl = fragment.fragmentConfig.originUrl ?: ""
        val targetUrl = fragment.fragmentConfig.targetUrl ?: ""
        if (originUrl.contains(".gif", true) || targetUrl.contains(".gif", true)) {
            if (BuildConfig.DEBUG) {
                Log.d("MojitoLongPress", "tryDispatchLongPress skipped gif x=$x y=$y")
            }
            return false
        }
        if (viewPagerScrollState != ViewPager2.SCROLL_STATE_IDLE) {
            if (BuildConfig.DEBUG) {
                Log.d("MojitoLongPress", "tryDispatchLongPress blocked by scroll state x=$x y=$y")
            }
            return false
        }
        val now = SystemClock.uptimeMillis()
        if (now - lastGlobalLongPressTime < 500L) {
            if (BuildConfig.DEBUG) {
                Log.d("MojitoLongPress", "tryDispatchLongPress throttled x=$x y=$y")
            }
            return false
        }
        val mojitoView = fragment.view?.findViewById<MojitoView>(net.mikaelzero.mojito.R.id.mojitoView)
        if (mojitoView?.isDrag == true) {
            if (BuildConfig.DEBUG) {
                Log.d("MojitoLongPress", "tryDispatchLongPress ignored drag x=$x y=$y")
            }
            return false
        }
        lastGlobalLongPressTime = now
        if (BuildConfig.DEBUG) {
            Log.d("MojitoLongPress", "tryDispatchLongPress dispatch x=$x y=$y")
        }
        onMojitoListener?.onLongClick(
            this@ImageMojitoActivity,
            fragment.view ?: binding.viewPager,
            x,
            y,
            fragment.fragmentConfig.position
        )
        return true
    }

}
