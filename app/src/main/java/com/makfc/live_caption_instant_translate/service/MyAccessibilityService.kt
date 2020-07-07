package com.makfc.live_caption_instant_translate.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.*
import android.view.accessibility.AccessibilityNodeInfo
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lzf.easyfloat.EasyFloat
import com.makfc.live_caption_instant_translate.BuildConfig
import com.makfc.live_caption_instant_translate.MainActivity
import com.makfc.live_caption_instant_translate.translate_api.Language
import com.makfc.live_caption_instant_translate.translate_api.TranslateAPI

class MyAccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = BuildConfig.APPLICATION_ID
        val ACTION_BROADCAST =
            MyAccessibilityService::class.java.name + "Broadcast"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_CAPTION_SOURCE = "extra_caption_source"
        const val EXTRA_FROM_LIVE_CAPTION = "extra_from_live_caption"
        const val EXTRA_FROM_YOUTUBE_CAPTION = "extra_from_youtube_caption"
        const val EXTRA_IS_TRANSLATED_TEXT = "extra_is_translated_text"
        const val EXTRA_ON_SERVICE_CONNECTED = "extra_on_service_connected"
        var previoustext = "Turn on Live Caption and play some media that says something..."
        var translatedText = ""
    }

    private val translateAPI = TranslateAPI()

    override fun onServiceConnected() {
        Log.v(TAG, "on Service Connected")
        LocalBroadcastManager.getInstance(this).registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    Log.d(TAG, "onReceive: ${MainActivity.ACTION_BROADCAST}")
                    sendBroadcastMessage(previoustext, EXTRA_FROM_LIVE_CAPTION, false)
                    sendBroadcastMessage(translatedText, EXTRA_FROM_LIVE_CAPTION, true)
                }
            }, IntentFilter(MainActivity.ACTION_BROADCAST)
        )
        EasyFloat.init(this.application, true)
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_ON_SERVICE_CONNECTED, "")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
/*        when (event?.packageName) {
            "com.vanced.android.youtube" -> {
//                Log.d(TAG, "onAccessibilityEvent: $event")
            }
            "com.google.android.as" -> {

            }
        }*/
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
                MyAccessibilityService.translatedText = translatedText
            }

            override fun onFailure(ErrorText: String?) {
                Log.d(MainActivity.TAG, "onFailure: $ErrorText")
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