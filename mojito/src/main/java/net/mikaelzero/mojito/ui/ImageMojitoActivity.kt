package net.mikaelzero.mojito.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.Window
import android.view.WindowManager
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.gyf.immersionbar.ImmersionBar
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
    private lateinit var imageViewPagerAdapter: FragmentPagerAdapter
    val fragmentMap = hashMapOf<Int, ImageMojitoFragment?>()
    private var backInvokedCallback: OnBackInvokedCallback? = null
    private var backHandled = false
    private var viewPagerScrollState = ViewPager.SCROLL_STATE_IDLE
    private var isStatusBarHidden = false
    private var statusBarRetryCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        Log.d("MojitoStatusBar", "onCreate: setDecorFitsSystemWindows(false)")
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        if (Mojito.mojitoConfig().transparentNavigationBar()) {
            ImmersionBar.with(this).transparentBar().init()
        } else {
            ImmersionBar.with(this).transparentStatusBar().init()
        }
        binding = ActivityImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val topInset = ImmersionBar.getStatusBarHeight(this)
        Log.d("MojitoStatusBar", "onCreate: statusBarHeight=$topInset")
        binding.indicatorLayout.setPadding(
            binding.indicatorLayout.paddingLeft,
            topInset,
            binding.indicatorLayout.paddingRight,
            binding.indicatorLayout.paddingBottom
        )
        binding.userCustomLayout.setPadding(
            binding.userCustomLayout.paddingLeft,
            topInset,
            binding.userCustomLayout.paddingRight,
            binding.userCustomLayout.paddingBottom
        )

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
        imageViewPagerAdapter = object :
            FragmentPagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getItem(position: Int): Fragment {
                val fragment = fragmentMap[position]
                return if (fragment == null) {
                    val fragmentConfig = FragmentConfig(
                        viewPagerBeans[position].url,
                        viewPagerBeans[position].targetUrl,
                        viewPagerBeans[position].viewParams,
                        position,
                        activityConfig.autoLoadTarget,
                        viewPagerBeans[position].showImmediately,
                        if (activityConfig.errorDrawableResIdList[position] != null) {
                            activityConfig.errorDrawableResIdList[position]!!
                        } else {
                            0
                        }
                    )
                    val imageFragment = ImageMojitoFragment.newInstance(fragmentConfig)
                    fragmentMap[position] = imageFragment
                    imageFragment
                } else {
                    fragment
                }
            }

            override fun getCount(): Int = viewPagerBeans.size
        }
        binding.viewPager.adapter = imageViewPagerAdapter
        binding.viewPager.setCurrentItem(currentPosition, false)
        binding.viewPager.post { updateStatusBarForCurrentImage() }
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

        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                viewPagerScrollState = state
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                activityCoverLoader?.pageChange(viewPagerBeans.size, position)
                onMojitoListener?.onViewPageSelected(position)
                Log.d("MojitoStatusBar", "onPageSelected: position=$position")
                statusBarRetryCount = 0
                binding.viewPager.post { updateStatusBarForCurrentImage() }
            }
        })
        activityCoverLoader?.pageChange(viewPagerBeans.size, activityConfig.position)
        if (!activityConfig.originImageUrls.isNullOrEmpty()) {
            iIndicator?.attach(binding.indicatorLayout)
            iIndicator?.onShow(binding.viewPager)
        }
        Mojito.currentActivity = WeakReference<ImageMojitoActivity>(this)
    }

    fun setViewPagerLock(isLock: Boolean) {
        binding.viewPager.isLocked = isLock
    }

    fun finishView() {
        restoreSystemBars()
        progressLoader = null
        fragmentCoverLoader = null
        multiContentLoader = null
        iIndicator = null
        activityCoverLoader = null
        onMojitoListener = null
        viewParams = null
        fragmentMap.clear()
        Mojito.clean()
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    fun backToMin() {
        (imageViewPagerAdapter.getItem(binding.viewPager.currentItem) as ImageMojitoFragment).backToMin()
    }

    fun updateStatusBarForCurrentImage() {
        val fragment = getCurrentFragment() as? ImageMojitoFragment ?: return
        val shouldHide = fragment.shouldHideStatusBar()
        if (!fragment.hasValidBounds()) {
            if (statusBarRetryCount++ < 6) {
                binding.viewPager.postDelayed({ updateStatusBarForCurrentImage() }, 60L)
            }
            return
        }
        Log.d(
            "MojitoStatusBar",
            "updateStatusBarForCurrentImage: shouldHide=$shouldHide hidden=$isStatusBarHidden info=${fragment.getStatusBarOverlapInfo()}"
        )
        if (shouldHide == isStatusBarHidden) {
            if (!shouldHide && statusBarRetryCount++ < 6) {
                binding.viewPager.postDelayed({ updateStatusBarForCurrentImage() }, 60L)
            }
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
            (imageViewPagerAdapter.getItem(binding.viewPager.currentItem) as ImageMojitoFragment).backToMin()
            true
        } else super.onKeyDown(keyCode, event)
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
        return imageViewPagerAdapter.getItem(binding.viewPager.currentItem) as IMojitoFragment?
    }

    override fun getContext(): Context {
        return this
    }

    override fun onDestroy() {
        super.onDestroy()
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
        WindowInsetsControllerCompat(window, window.decorView)
            .show(WindowInsetsCompat.Type.statusBars())
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        isStatusBarHidden = false
        Log.d("MojitoStatusBar", "restoreSystemBars")
    }

    

    fun tryDispatchLongPress(x: Float, y: Float): Boolean {
        val fragment = getCurrentFragment() as? ImageMojitoFragment ?: return false
        val originUrl = fragment.fragmentConfig.originUrl ?: ""
        val targetUrl = fragment.fragmentConfig.targetUrl ?: ""
        if (originUrl.contains(".gif", true) || targetUrl.contains(".gif", true)) {
            Log.d("MojitoLongPress", "tryDispatchLongPress skipped gif x=$x y=$y")
            return false
        }
        if (viewPagerScrollState != ViewPager.SCROLL_STATE_IDLE) {
            Log.d("MojitoLongPress", "tryDispatchLongPress blocked by scroll state x=$x y=$y")
            return false
        }
        val now = SystemClock.uptimeMillis()
        if (now - lastGlobalLongPressTime < 500L) {
            Log.d("MojitoLongPress", "tryDispatchLongPress throttled x=$x y=$y")
            return false
        }
        val mojitoView = fragment.view?.findViewById<MojitoView>(net.mikaelzero.mojito.R.id.mojitoView)
        if (mojitoView?.isDrag == true) {
            Log.d("MojitoLongPress", "tryDispatchLongPress ignored drag x=$x y=$y")
            return false
        }
        lastGlobalLongPressTime = now
        Log.d("MojitoLongPress", "tryDispatchLongPress dispatch x=$x y=$y")
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
