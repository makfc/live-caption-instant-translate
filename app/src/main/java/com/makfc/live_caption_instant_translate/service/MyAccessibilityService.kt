package com.makfc.live_caption_instant_translate.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.*
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.anim.AppFloatFadeInOutAnimator
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.interfaces.OnInvokeView
import com.makfc.live_caption_instant_translate.BuildConfig
import com.makfc.live_caption_instant_translate.MainActivity
import com.makfc.live_caption_instant_translate.MainActivity.Companion.scrollToBottom
import com.makfc.live_caption_instant_translate.R
import com.makfc.live_caption_instant_translate.translate_api.TranslateAPI.Companion.translate
import com.makfc.live_caption_instant_translate.widget.ScaleImage
import kotlinx.coroutines.*
import java.util.*
import kotlin.math.max


class MyAccessibilityService : AccessibilityService() {
    companion object {
        var instance: MyAccessibilityService? = null
        const val TAG = BuildConfig.APPLICATION_ID
        const val TAG_SCALE_FLOAT = "scaleFloat"
        val ACTION_BROADCAST =
            MyAccessibilityService::class.java.name + "Broadcast"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_IS_TRANSLATED_TEXT = "extra_is_translated_text"
        const val START_MESSAGE = "Turn on Live Caption and play some media that says something..."
        var previoustext = ""
        var translatedDualLangText = ""
        var transcript = ""

        private fun isJapaneseKana(codePoint: Int): Boolean {
            return Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HIRAGANA ||
                    Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.KATAKANA
        }

        fun isChinese(s: String): Boolean {
            return s.codePoints().anyMatch { codePoint: Int ->
                Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN
            } && !s.codePoints().anyMatch { codePoint: Int -> isJapaneseKana(codePoint) }
        }

        fun getInsertedText(oldText: String, newText: String): String? {
            val oldTextList = oldText.split(" ")
            val newTextList = newText.split(" ")
            val firstWord = newTextList.first()

            // Get all occurrence indexes
            val indexList = oldTextList.withIndex()
                .filter { it.value == firstWord }
                .map { it.index }
            if (indexList.isEmpty()) return null

            var newTextMuList: MutableList<String>
            for (index in indexList) {
                var i = index
                newTextMuList = newTextList.toMutableList()
                while (i <= oldTextList.lastIndex) {
                    if (oldTextList[i++] == newTextMuList.first())
                        newTextMuList.removeAt(0)
                    else
                        break
                }

                // When i reach last index
                if (i == oldTextList.count()) {
                    return " " + newTextMuList.joinToString(" ")
                }
            }
            return null
        }
    }

//    val translateAPI = TranslateAPI()

    @ExperimentalCoroutinesApi
    @SuppressLint("ClickableViewAccessibility")
    override fun onServiceConnected() {
        instance = this
        Log.d(TAG, "On Service Connected")
        LocalBroadcastManager.getInstance(this).registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    Log.d(TAG, "onReceive: ${MainActivity.ACTION_BROADCAST}")
                    if (transcript.isEmpty()) {
                        sendBroadcastMessage(START_MESSAGE, false)
                        translateText(START_MESSAGE, false)
                    } else {
                        sendBroadcastMessage(transcript, false)
                        translateText(transcript, false)
                    }
                }
            }, IntentFilter(MainActivity.ACTION_BROADCAST)
        )
/*        showEasyFloat()
        Handler().postDelayed({
            Log.d(MainActivity.TAG, "hideAppFloat: $TAG_SCALE_FLOAT")
            EasyFloat.hideAppFloat(TAG_SCALE_FLOAT)
        }, 0)*/
    }

    private fun showEasyFloat() {
        val displayMetrics: DisplayMetrics = resources.displayMetrics
        val dpHeight: Float = displayMetrics.heightPixels / displayMetrics.density
        val dpWidth: Float = displayMetrics.widthPixels / displayMetrics.density
        println("dpHeight : $dpHeight")
        println("dpWidth : $dpWidth")
        println("heightPixels : ${displayMetrics.heightPixels}")
        println("widthPixels : ${displayMetrics.widthPixels}")
        EasyFloat.init(this.application, true)
        EasyFloat.with(MainActivity.instance?.applicationContext ?: this)
            .setTag(TAG_SCALE_FLOAT)
            .setShowPattern(ShowPattern.ALL_TIME)
            .setLocation(0, 500)
            .setAppFloatAnimator(AppFloatFadeInOutAnimator())
            .setFilter(MainActivity::class.java)
            .setLayout(R.layout.float_app_scale, OnInvokeView {
                val content = it.findViewById<RelativeLayout>(R.id.rlContent)
                val params = content.layoutParams as FrameLayout.LayoutParams
                params.width = displayMetrics.widthPixels.coerceAtMost(displayMetrics.heightPixels)
                content.layoutParams = params
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
                    transcript = ""
                }
                val textView = it.findViewById<TextView>(R.id.textView)
                textView.setOnLongClickListener {
//                    Log.d(TAG, "textView: OnLongClick")
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                    false
                }
                var isTranslucent = false
                textView.setOnClickListener {
//                    Log.d(TAG, "textView: OnClick")
                    EasyFloat.getAppFloatView(TAG_SCALE_FLOAT)?.apply {
//                        Log.d(TAG, "isTranslucent: $isTranslucent")
                        findViewById<RelativeLayout>(R.id.rlContent)
                            .setBackgroundResource(if (!isTranslucent) R.drawable.floating_window_bg_top else 0)
                        isTranslucent = !isTranslucent
                    }
                }
            })
            .show()
    }

    override fun onInterrupt() {}

    @ExperimentalCoroutinesApi
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
//        Log.d(TAG, "onAccessibilityEvent: $event")
        when (event?.packageName) {
            "com.google.android.youtube",
            "com.vanced.android.youtube" -> {
//                Log.d(TAG, "onAccessibilityEvent: $event")
/*                Log.d(TAG, "event.contentChangeTypes: ${event.contentChangeTypes}")
//                Log.d(TAG, "event.source: ${event.source}")
                Log.d(
                    TAG, "getYoutubeSubtitle: ${getYoutubeSubtitle(
                        event.source,
                        event.packageName as String
                    )}"
                )*/
//                if (event.contentChangeTypes == 7){
//                if (event.contentChangeTypes == 5){
/*                if (event.contentChangeTypes == CONTENT_CHANGE_TYPE_SUBTREE) {
//                    Log.d(TAG, "onAccessibilityEvent: $event")
                    Log.d(TAG, "event.source: ${getYoutubeSubtitle(event.source)}")
//                    printAllChildInfo(event.source)
                }*/
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
                    "com.google.android.youtube",
                    "com.vanced.android.youtube" -> {
                        if (event.contentChangeTypes != CONTENT_CHANGE_TYPE_SUBTREE) return
                        var subtitleStr: String? =
                            getYoutubeSubtitle(source, event.packageName.toString()) ?: return

                        // Remove unnecessary text
                        if (subtitleStr!!.startsWith("♪")) {
                            subtitleStr = subtitleStr
                                .replace("♪", "").toLowerCase(Locale.ROOT)
                        }

                        // Ignore unnecessary text
                        if (isChinese(subtitleStr)) return
                        if (subtitleStr.isEmpty() ||
                            subtitleStr == previoustext ||
                            previoustext.endsWith(subtitleStr)
                        ) return

                        val insertText = getInsertedText(previoustext, subtitleStr)

                        // When it is the automatic subtitle
                        if (insertText != null) {
                            transcript += insertText
                            translateText(transcript)
                        } else {
                            if (previoustext.isNotEmpty())
                                transcript += "\n"
                            transcript += subtitleStr
                            translateText(
                                subtitleStr,
                                isSetFloatText = true,
                                isSendBroadcast = false
                            )
                            translateText(transcript, false)
                        }

                        // For debug
//                        if (insertText == null) {
//                            Log.d(TAG, "previoustext: $previoustext")
//                            Log.d(TAG, "subtitleStr: $subtitleStr")
//                            Log.d(TAG, "insertText: $insertText")
//                            Log.d(TAG, "transcript: $transcript")
//                        }

//                        Log.d(TAG, "transcript: $transcript")
                        sendBroadcastMessage(transcript, false)
                        previoustext = subtitleStr
                        return
                    }
                    "com.google.android.as" -> {
                        var text = source.text ?: return
                        // Ignore unnecessary text
                        if (text.isEmpty() ||
                            text == previoustext
                        ) return
                        // Remove TextView Title
                        text = text.substring(text.indexOf("\n") + 1)
                        translateText(text)
                        transcript = text
                        sendBroadcastMessage(transcript, false)
                        previoustext = text
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


    @ExperimentalCoroutinesApi
    private fun translateText(
        text: String,
        isSetFloatText: Boolean = true,
        isSendBroadcast: Boolean = true
    ) {

        var preProcessText = text.replace("\n", "\\n")
        val maxCharLen = 500
        if (preProcessText.length > maxCharLen) {
            val index = preProcessText
                .indexOf(" ", preProcessText.length - maxCharLen)
            preProcessText = preProcessText.takeLast(preProcessText.length - index)
        }
        GlobalScope.launch(Dispatchers.Main) {
            val translateResult =
                withContext(Dispatchers.IO) { translate(preProcessText) } ?: return@launch
//            val translatedText = translate(preProcessText) ?: return@launch
//            Log.d(TAG, "preProcessText: ${preProcessText}")
//            Log.d(TAG, "translatedText: ${translateResult.translatedText}")
            if (isSetFloatText && MainActivity.isOnPause) {
                if (EasyFloat.getAppFloatView(TAG_SCALE_FLOAT) == null) {
                    showEasyFloat()
                    Handler().postDelayed({
                        setFloatText(translateResult.translatedText)
                    }, 0)
                } else {
                    setFloatText(translateResult.translatedText)
                }
            }
            if (isSendBroadcast) {
                translatedDualLangText = translateResult.dualLangText
//                Log.d(TAG, "sendBroadcastMessage: $translatedText")
                sendBroadcastMessage(translatedDualLangText, true)
            }
        }
    }


    private fun setFloatText(text: String) {
        EasyFloat.getAppFloatView(TAG_SCALE_FLOAT)?.apply {
            if (!isShown) EasyFloat.showAppFloat(TAG_SCALE_FLOAT)
            val scrollView = findViewById<ScrollView>(R.id.scrollView)
            findViewById<TextView>(R.id.textView).text = text
            scrollView.post { scrollToBottom(scrollView) }
        }
    }

    private fun getYoutubeSubtitle(
        accessibilityNodeInfo: AccessibilityNodeInfo?,
        packageName: String
    ): String? {
        var subtitleStr = ""
        if (accessibilityNodeInfo != null) {
            val nodes =
                accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(
                    "$packageName:id/subtitle_window_identifier"
                )
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
                val contentDescription = child.contentDescription
                val text = child.text
                if (text != null || contentDescription != null) {
                    Log.d(TAG, "child.childCount: ${child.childCount}")
                    Log.d(TAG, "child.viewIdResourceName: ${child.viewIdResourceName}")
                    Log.d(TAG, "child.className: ${child.className}")
                    Log.d(TAG, "text: $text")
                    Log.d(TAG, "contentDescription: $contentDescription")
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
        isTranslatedText: Boolean
    ) {
        val intent =
            Intent(ACTION_BROADCAST)
        intent.putExtra(
            EXTRA_TEXT,
            text
        )
        intent.putExtra(
            EXTRA_IS_TRANSLATED_TEXT,
            isTranslatedText
        )
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}