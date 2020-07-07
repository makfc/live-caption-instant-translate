package com.makfc.live_caption_instant_translate.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.*
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.enums.ShowPattern
import com.makfc.live_caption_instant_translate.widget.ScaleImage
import com.lzf.easyfloat.interfaces.OnInvokeView
import com.makfc.live_caption_instant_translate.BuildConfig
import com.makfc.live_caption_instant_translate.MainActivity
import com.makfc.live_caption_instant_translate.MainActivity.Companion.scrollToBottom
import com.makfc.live_caption_instant_translate.R
import com.makfc.live_caption_instant_translate.translate_api.Language
import com.makfc.live_caption_instant_translate.translate_api.TranslateAPI
import kotlin.math.max

class MyAccessibilityService : AccessibilityService() {
    companion object {
        var instance: MyAccessibilityService? = null
        private const val TAG = BuildConfig.APPLICATION_ID
        const val TAG_SCALE_FLOAT = "scaleFloat"
        val ACTION_BROADCAST =
            MyAccessibilityService::class.java.name + "Broadcast"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_CAPTION_SOURCE = "extra_caption_source"
        const val EXTRA_FROM_LIVE_CAPTION = "extra_from_live_caption"
        const val EXTRA_FROM_YOUTUBE_CAPTION = "extra_from_youtube_caption"
        const val EXTRA_IS_TRANSLATED_TEXT = "extra_is_translated_text"
        const val EXTRA_ON_SERVICE_CONNECTED = "extra_on_service_connected"
        var previoustext = "Turn on Live Caption and play some media that says something..."
        var translatedText = "Text"
    }

    private val translateAPI = TranslateAPI()

    @SuppressLint("ClickableViewAccessibility")
    override fun onServiceConnected() {
        instance = this
        Log.d(TAG, "on Service Connected")
        LocalBroadcastManager.getInstance(this).registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    Log.d(TAG, "onReceive: ${MainActivity.ACTION_BROADCAST}")
                    sendBroadcastMessage(previoustext, EXTRA_FROM_LIVE_CAPTION, false)
                    translate(previoustext, EXTRA_FROM_LIVE_CAPTION)
//                    sendBroadcastMessage(translatedText, EXTRA_FROM_LIVE_CAPTION, true)
                }
            }, IntentFilter(MainActivity.ACTION_BROADCAST)
        )
        showEasyFloat(this)
    }

    fun showEasyFloat(activity: Context) {
        EasyFloat.init(this.application, true)
        EasyFloat.with(activity)
            .setTag(TAG_SCALE_FLOAT)
            .setShowPattern(ShowPattern.ALL_TIME)
            .setLocation(200, 500)
            .setAppFloatAnimator(null)
            .setFilter(MainActivity::class.java)
            .setLayout(R.layout.float_app_scale, OnInvokeView {
                val content = it.findViewById<RelativeLayout>(R.id.rlContent)
                val params = content.layoutParams as FrameLayout.LayoutParams
                it.findViewById<ScaleImage>(R.id.ivScale).onScaledListener =
                    object : ScaleImage.OnScaledListener {
                        override fun onScaled(x: Float, y: Float, event: MotionEvent) {
                            params.width = max(params.width + x.toInt(), 100)
                            params.height = max(params.height + y.toInt(), 100)
                            content.layoutParams = params
                        }
                    }

                it.findViewById<ImageView>(R.id.ivClose).setOnClickListener {
                    Log.d(MainActivity.TAG, "hideAppFloat: $TAG_SCALE_FLOAT")
                    EasyFloat.hideAppFloat(TAG_SCALE_FLOAT)
                }
                it.findViewById<TextView>(R.id.textView).setOnLongClickListener {
                    Log.d(TAG, "textView: OnLongClick")
                    val intent = Intent(this, MainActivity::class.java).apply {
                        putExtra(EXTRA_ON_SERVICE_CONNECTED, "")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                    false
                }
            })
/*            .registerCallback {
                touchEvent { view, motionEvent ->
                    Log.d(TAG, "motionEvent: $motionEvent")
                }
            }*/
            .show()
        Handler().postDelayed({
            Log.d(MainActivity.TAG, "hideAppFloat: $TAG_SCALE_FLOAT")
            EasyFloat.hideAppFloat(TAG_SCALE_FLOAT)
        }, 0)
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
//        Log.d(TAG, "onAccessibilityEvent: $event")
        when (event?.packageName) {
            "com.vanced.android.youtube" -> {
//                Log.d(TAG, "onAccessibilityEvent: $event")
            }
            "com.google.android.as" -> {
                // When Live Caption gone
                if (event.contentChangeTypes == CONTENT_CHANGE_TYPE_SUBTREE &&
                    event.className == "android.widget.RelativeLayout"
                ) {
                    EasyFloat.hideAppFloat(TAG_SCALE_FLOAT)
                }
            }
        }
        when (event?.eventType) {
            TYPE_WINDOW_CONTENT_CHANGED -> {
                val source: AccessibilityNodeInfo = event.source ?: return
                when (event.packageName) {
                    "com.vanced.android.youtube" -> {
                        val subtitleStr = getYoutubeSubtitle(source)
                        if (subtitleStr != null
                            && subtitleStr.isNotEmpty()
                            && subtitleStr != previoustext
                        ) {
                            Log.d(TAG, "sendBroadcastMessage: $subtitleStr")
                            sendBroadcastMessage(subtitleStr, EXTRA_FROM_YOUTUBE_CAPTION, false)
                            translate(subtitleStr, EXTRA_FROM_YOUTUBE_CAPTION)
                            previoustext = subtitleStr
                        }
                        return
                    }
                    "com.google.android.as" -> {
                        var text = source.text
                        if (text != null
                            && text != previoustext
                        ) {
                            // Remove TextView Title
                            text = text.substring(text.indexOf("\n") + 1)
                            sendBroadcastMessage(text.toString(), EXTRA_FROM_LIVE_CAPTION, false)
                            translate(text, EXTRA_FROM_LIVE_CAPTION)
                            previoustext = text
                        }
                        return
                    }
                }
            }
            TYPE_VIEW_SCROLLED -> {
            }
            TYPE_WINDOW_STATE_CHANGED -> {
//                Log.d(TAG, "source.childCount: ${source.childCount}")
/*                val activityNodeInfo: AccessibilityNodeInfo? = findTextViewNode(event.source)
//                val activityNodeInfo: AccessibilityNodeInfo = event.source
                val rect = Rect()
                activityNodeInfo?.getBoundsInScreen(rect)
                Log.d(TAG, "rect: $rect")*/
            }
            else -> {
//                Log.d(TAG, "onAccessibilityEvent: $event")
            }
        }
    }

    private fun translate(text: String, captionSource: String) {
        var preProcessText = text.replace("\n", "\\n")
        val maxCharLen = 1000
        if (preProcessText.length > maxCharLen) {
            val index = preProcessText
                .indexOf(" ", preProcessText.length - maxCharLen)
            preProcessText = preProcessText.takeLast(preProcessText.length - index)
        }
        translateAPI.setTranslateListener(object : TranslateAPI.TranslateListener {
            override fun onSuccess(translatedText: String?) {
//                Log.d(TAG, "onSuccess: $translatedText")
                if (translatedText == null) return
                sendBroadcastMessage(translatedText, captionSource, true)
                EasyFloat.showAppFloat(TAG_SCALE_FLOAT)
                EasyFloat.getAppFloatView(TAG_SCALE_FLOAT)?.apply {
                    findViewById<TextView>(R.id.textView).text = translatedText
                    val scrollView = findViewById<ScrollView>(R.id.scrollView)
                    scrollToBottom(scrollView)
                }
                MyAccessibilityService.translatedText = translatedText
            }

            override fun onFailure(ErrorText: String?) {
                Log.d(TAG, "onFailure: $ErrorText")
            }
        })
        translateAPI.translate(
            Language.AUTO_DETECT,
            Language.CHINESE_TRADITIONAL,
            preProcessText
        )
    }

    private fun getYoutubeSubtitle(accessibilityNodeInfo: AccessibilityNodeInfo?): String? {
        var subtitleStr = ""
        if (accessibilityNodeInfo != null) {
            val nodes =
                accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(
                    "com.vanced.android.youtube:id/subtitle_window_identifier"
                )

/*            if (nodes.count() == 0) {
                accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(
                    "com.google.android.youtube:id/subtitle_window_identifier"
                )
            }*/

//            Log.d(TAG, "nodes.count(): ${nodes.count()}")
/*            if (nodes.count() == 1){
                return nodes[0].text.toString()
            }*/

            if (nodes.count() != 2)
                return ""

            var space = ""
            for (node in nodes) {
                if (node == null) continue
                subtitleStr += space + node.text
                space = " "
            }
        }
        return subtitleStr
    }

    private fun printAllChildInfo(accessibilityNodeInfo: AccessibilityNodeInfo?) {
        if (accessibilityNodeInfo != null) {
            for (i in 0 until accessibilityNodeInfo.childCount) {
                val child = accessibilityNodeInfo.getChild(i) ?: continue
                val text = child.text
                if (text != null) {
                    Log.d(TAG, "child.childCount: ${child.childCount}")
                    Log.d(TAG, "child.viewIdResourceName: ${child.viewIdResourceName}")
                    Log.d(TAG, "child.className: ${child.className}")
                    Log.d(TAG, "text: $text")
                    Log.d(TAG, "---")
                }
            }
        }
    }

/*    fun findTextViewNode(nodeInfo: AccessibilityNodeInfo?): AccessibilityNodeInfo? {

        //I highly recommend leaving this line in! You never know when the screen content will
        //invalidate a node you're about to work on, or when a parents child will suddenly be gone!
        //Not doing this safety check is very dangerous!
        if (nodeInfo == null) return null
        Log.d(TAG, nodeInfo.toString())

        //Notice that we're searching for the TextView's simple name!
        //This allows us to find AppCompat versions of TextView as well
        //as 3rd party devs well names subclasses... though with perhaps
        //a few poorly named unintended stragglers!
        if (nodeInfo.className.toString().contains(TextView::class.java.simpleName)) {
            return nodeInfo
        }

        //Do other work!
        for (i in 0 until nodeInfo.childCount) {
            val result = findTextViewNode(nodeInfo.getChild(i))
            if (result != null) return result
        }
        return null
    }*/

    private fun sendBroadcastMessage(
        text: String?,
        captionSource: String,
        isTranslatedText: Boolean
    ) {
        val intent =
            Intent(ACTION_BROADCAST)
        intent.putExtra(
            EXTRA_TEXT,
            text
        )
        intent.putExtra(
            EXTRA_CAPTION_SOURCE,
            captionSource
        )
        intent.putExtra(
            EXTRA_IS_TRANSLATED_TEXT,
            isTranslatedText
        )
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}