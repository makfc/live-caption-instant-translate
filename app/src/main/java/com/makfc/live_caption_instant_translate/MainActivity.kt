package com.makfc.live_caption_instant_translate

import android.content.*
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.makfc.live_caption_instant_translate.service.AccessibilityServiceTool
import com.makfc.live_caption_instant_translate.service.GlobalAppContext
import com.makfc.live_caption_instant_translate.service.MyAccessibilityService
import com.makfc.live_caption_instant_translate.service.MyAccessibilityService.Companion.EXTRA_FROM_LIVE_CAPTION
import com.makfc.live_caption_instant_translate.service.MyAccessibilityService.Companion.EXTRA_FROM_YOUTUBE_CAPTION
import com.makfc.live_caption_instant_translate.service.MyAccessibilityService.Companion.EXTRA_TEXT_VIEW_TEXT
import com.makfc.live_caption_instant_translate.translate_api.Language
import com.makfc.live_caption_instant_translate.translate_api.TranslateAPI
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = BuildConfig.APPLICATION_ID
    }

    var text: String? = null
    var captionSource: String? = null
    var isStarted = false
    private val translateAPI = TranslateAPI()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_actions, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.translateAll -> {
                if (text == null || text!!.isEmpty()) return true
                val preProcessText = text!!.replace("\n", "\\n")
                scrollView.post { scrollToBottom(scrollView) }
                translateAPI.translate(
                    Language.AUTO_DETECT,
                    Language.CHINESE_TRADITIONAL,
                    preProcessText
                )
                true
            }
            R.id.googleTranslate -> {
                translate()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun translate() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
//            action = "android.intent.action.PROCESS_TEXT"
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
            component = ComponentName(
                "com.google.android.apps.translate",
                "com.google.android.apps.translate.TranslateActivity"
            )
/*            component = ComponentName(
                "com.google.android.apps.translate",
                "com.google.android.apps.translate.copydrop.CopyDropActivity")*/
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private fun checkAccessibility() {
        val enableAccessibility = AccessibilityServiceTool.isAccessibilityServiceEnabled(this)
        if (!enableAccessibility) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("無障礙服務未開啟，去啟動無障礙服務？")
                .setTitle("提示")
                .setPositiveButton("確定") { dialog, which ->
                    AccessibilityServiceTool.goToAccessibilitySetting()
                }
            val dialog = builder.create()
            dialog.show()

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        GlobalAppContext.set(this.application)
        checkAccessibility()

        textView_transcript.setOnClickListener { v ->
            if (!textView_transcript.hasSelection()) {
                setText()
            }
        }

        textView_transcript2.setOnClickListener { v ->
            if (!textView_transcript.hasSelection()) {
                setText()
            }
        }

/*        scrollView.viewTreeObserver
            .addOnScrollChangedListener {
                val scrollY: Int = scrollView.scrollY
//                Log.i(TAG, "scrollY: $scrollY")
//                Log.i(TAG, "delta: ${getScrollViewBottomDelta(scrollView)}")
            }*/

        LocalBroadcastManager.getInstance(this).registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    text = intent.getStringExtra(EXTRA_TEXT_VIEW_TEXT)
                    captionSource =
                        intent.getStringExtra(MyAccessibilityService.EXTRA_CAPTION_SOURCE)
                    setText()
                }
            }, IntentFilter(MyAccessibilityService.ACTION_BROADCAST)
        )

        translateAPI.setTranslateListener(object : TranslateAPI.TranslateListener {
            override fun onSuccess(translatedText: String?) {
//                Log.d(TAG, "onSuccess: $translatedText")
                if (captionSource == null ||
                    captionSource == EXTRA_FROM_LIVE_CAPTION
                ) {
                    textView_transcript2.text = translatedText
                } else if (captionSource == EXTRA_FROM_YOUTUBE_CAPTION) {
                    if (isStarted) {
                        textView_transcript2.append("\n\n")
                        textView_transcript2.append(translatedText)
                    } else {
                        textView_transcript2.text = translatedText
                        isStarted = true
                    }
                }
                scrollView2.post { scrollToBottom(scrollView2) }
            }

            override fun onFailure(ErrorText: String?) {
                Log.d(TAG, "onFailure: $ErrorText")
            }
        })

        translateAPI.translate(
            Language.AUTO_DETECT,
            Language.CHINESE_TRADITIONAL,
            textView_transcript.text.toString()
        )
    }

    private fun setText() {
        val startSelection2: Int = textView_transcript2.selectionStart
        val endSelection2: Int = textView_transcript2.selectionEnd
        if (text != null
            && startSelection2 == endSelection2
            && getScrollViewBottomDelta(scrollView2) < 10
        ) {
            var preProcessText = text!!.replace("\n", "\\n")
            if (captionSource == EXTRA_FROM_LIVE_CAPTION) {
                textView_transcript.text = text
                val maxCharLen = 1000
                if (preProcessText.length > maxCharLen) {
                    val index = preProcessText
                        .indexOf(" ", preProcessText.length - maxCharLen)
                    preProcessText = preProcessText.takeLast(preProcessText.length - index)
                }
            } else if (captionSource == EXTRA_FROM_YOUTUBE_CAPTION) {
                textView_transcript.append("\n\n" + text)
            }
            scrollView.post { scrollToBottom(scrollView) }
            translateAPI.translate(
                Language.AUTO_DETECT,
                Language.CHINESE_TRADITIONAL,
                preProcessText
            )
        }
    }

    private fun getScrollViewBottomDelta(scrollView: ScrollView): Int {
        val lastChild = scrollView.getChildAt(scrollView.childCount - 1)
        val bottom = lastChild.bottom + scrollView.paddingBottom
        val sy = scrollView.scrollY
        val sh = scrollView.height
        return bottom - (sy + sh)
    }

    private fun scrollToBottom(scrollView: ScrollView) {
        val delta = getScrollViewBottomDelta(scrollView)
        scrollView.smoothScrollBy(0, delta)
    }
}
