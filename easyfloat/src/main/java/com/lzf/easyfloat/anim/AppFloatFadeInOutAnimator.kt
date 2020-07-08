package com.lzf.easyfloat.anim

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import android.view.WindowManager
import com.lzf.easyfloat.enums.SidePattern
import com.lzf.easyfloat.interfaces.OnAppFloatAnimator

open class AppFloatFadeInOutAnimator : OnAppFloatAnimator {

    override fun enterAnim(
        view: View,
        params: WindowManager.LayoutParams,
        windowManager: WindowManager,
        sidePattern: SidePattern
    ): Animator? = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 500
        addUpdateListener {
            view.alpha = it.animatedValue as Float
            windowManager.updateViewLayout(view, params)
        }
    }

    override fun exitAnim(
        view: View,
        params: WindowManager.LayoutParams,
        windowManager: WindowManager,
        sidePattern: SidePattern
    ): Animator? = ValueAnimator.ofFloat(1f, 0f).apply {
        duration = 500
        addUpdateListener {
            view.alpha = it.animatedValue as Float
            windowManager.updateViewLayout(view, params)
        }
    }
}