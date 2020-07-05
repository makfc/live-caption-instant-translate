package com.makfc.live_caption_instant_translate.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.*
import android.view.accessibility.AccessibilityNodeInfo
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.makfc.live_caption_instant_translate.BuildConfig

class MyAccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = BuildConfig.APPLICATION_ID
        val ACTION_BROADCAST =
            MyAccessibilityService::class.java.name + "Broadcast"
        const val EXTRA_TEXT_VIEW_TEXT = "extra_text_view_text"
        const val EXTRA_CAPTION_SOURCE = "extra_caption_source"
        const val EXTRA_FROM_LIVE_CAPTION = "extra_from_live_caption"
        const val EXTRA_FROM_YOUTUBE_CAPTION = "extra_from_youtube_caption"
    }

    var previousSubtitleStr = ""

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
                val source: AccessibilityNodeInfo? = event.source
                if (source != null) {
                    when (event.packageName) {
                        "com.vanced.android.youtube" -> {
                            val subtitleStr = getYoutubeSubtitle(source)
                            if (subtitleStr != null
                                && subtitleStr.isNotEmpty()
                                && subtitleStr != previousSubtitleStr
                            ) {
                                previousSubtitleStr = subtitleStr
                                Log.d(TAG, "sendBroadcastMessage: $subtitleStr")
                                sendBroadcastMessage(subtitleStr, EXTRA_FROM_YOUTUBE_CAPTION)
                            }
                            return
                        }
                        "com.google.android.as" -> {
                            val text = source.text
                            if (text != null) {
                                sendBroadcastMessage(text.toString(), EXTRA_FROM_LIVE_CAPTION)
                            }
                            return
                        }
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

            var space = ""
            for (node in nodes) {
                if (node != null) {
                    subtitleStr += space + node.text
                    space = " "
                }
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

    private fun sendBroadcastMessage(text: String?, captionSource: String) {
        val intent =
            Intent(ACTION_BROADCAST)
        intent.putExtra(
            EXTRA_TEXT_VIEW_TEXT,
            text
        )
        intent.putExtra(
            EXTRA_CAPTION_SOURCE,
            captionSource
        )
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}