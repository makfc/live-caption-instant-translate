package com.makfc.live_caption_instant_translate.service

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.RequiresApi

object GlobalAppContext {
    @SuppressLint("StaticFieldLeak")
    private var sApplicationContext: Context? = null
    private var sHandler: Handler? = null
    fun set(a: Application) {
        sHandler = Handler(Looper.getMainLooper())
        sApplicationContext = a.applicationContext
    }

    fun get(): Context? {
        checkNotNull(sApplicationContext) { "Call GlobalAppContext.set() to set a application context" }
        return sApplicationContext
    }

    fun getString(resId: Int): String {
        return get()!!.getString(resId)
    }

    fun getString(resId: Int, vararg formatArgs: Any?): String {
        return get()!!.getString(resId, *formatArgs)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun getColor(id: Int): Int {
        return get()!!.getColor(id)
    }

    fun toast(message: String?) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(get(), message, Toast.LENGTH_SHORT).show()
            return
        }
        sHandler!!.post { Toast.makeText(get(), message, Toast.LENGTH_SHORT).show() }
    }

    fun toast(resId: Int) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(get(), resId, Toast.LENGTH_SHORT).show()
            return
        }
        sHandler!!.post { Toast.makeText(get(), resId, Toast.LENGTH_SHORT).show() }
    }

    fun toast(resId: Int, vararg args: Any?) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(
                get(),
                getString(resId, *args),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        sHandler!!.post {
            Toast.makeText(
                get(),
                getString(resId, *args),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun post(r: Runnable?) {
        sHandler!!.post(r)
    }

    fun postDelayed(r: Runnable?, m: Long) {
        sHandler!!.postDelayed(r, m)
    }
}