package com.makfc.live_caption_instant_translate.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
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
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
//        Log.d(TAG, "onAccessibilityEvent: $event")
/*        when (event?.packageName) {
            "com.google.android.as" -> {

            }
        }*/
        when (event?.eventType) {
            TYPE_WINDOW_CONTENT_CHANGED -> {

                val activityNodeInfo: AccessibilityNodeInfo? = event.source
                if (activityNodeInfo != null) {
                    val text = activityNodeInfo.text
//                    Log.d(TAG, "activityNodeInfo: $text")
                    if (text != null)
                        sendBroadcastMessage(text.toString())
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

    private fun sendBroadcastMessage(text: String?) {
        val intent =
            Intent(ACTION_BROADCAST)
        intent.putExtra(
            EXTRA_TEXT_VIEW_TEXT,
            text
        )
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}