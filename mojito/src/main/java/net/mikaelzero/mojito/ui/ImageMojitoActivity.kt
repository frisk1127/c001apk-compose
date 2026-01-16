package net.mikaelzero.mojito.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.Window
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
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

    fun tryDispatchLongPress(x: Float, y: Float): Boolean {
        val fragment = getCurrentFragment() as? ImageMojitoFragment ?: return false
        val originUrl = fragment.fragmentConfig.originUrl ?: ""
        val targetUrl = fragment.fragmentConfig.targetUrl ?: ""
        if (originUrl.contains(".gif", true) || targetUrl.contains(".gif", true)) {
            return false
        }
        val now = SystemClock.uptimeMillis()
        if (now - lastGlobalLongPressTime < 500L) {
            return false
        }
        val mojitoView = fragment.view?.findViewById<MojitoView>(net.mikaelzero.mojito.R.id.mojitoView)
        if (mojitoView?.isDrag == true) {
            return false
        }
        lastGlobalLongPressTime = now
        Log.d("MojitoLongPress", "Activity viewPager long-press fire pos=${fragment.fragmentConfig.position}")
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
