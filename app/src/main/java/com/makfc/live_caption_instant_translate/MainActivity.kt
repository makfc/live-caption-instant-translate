package com.makfc.live_caption_instant_translate

import android.content.*
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.interfaces.OnInvokeView
import com.makfc.live_caption_instant_translate.service.AccessibilityServiceTool
import com.makfc.live_caption_instant_translate.service.GlobalAppContext
import com.makfc.live_caption_instant_translate.service.MyAccessibilityService
import com.makfc.live_caption_instant_translate.service.MyAccessibilityService.Companion.EXTRA_IS_TRANSLATED_TEXT
import com.makfc.live_caption_instant_translate.service.MyAccessibilityService.Companion.EXTRA_TEXT
import com.makfc.live_caption_instant_translate.service.MyAccessibilityService.Companion.TAG_SCALE_FLOAT
import com.makfc.live_caption_instant_translate.translate_api.Language
import com.makfc.live_caption_instant_translate.translate_api.TranslateAPI
import com.makfc.live_caption_instant_translate.widget.ScaleImage
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.max

class MainActivity : AppCompatActivity() {
    companion object {
        var instance: MainActivity? = null
        const val TAG = BuildConfig.APPLICATION_ID
        val ACTION_BROADCAST =
            MainActivity::class.java.name + "Broadcast"

        fun getScrollViewBottomDelta(scrollView: ScrollView): Int {
            val lastChild = scrollView.getChildAt(scrollView.childCount - 1)
            val bottom = lastChild.bottom + scrollView.paddingBottom
            val sy = scrollView.scrollY
            val sh = scrollView.height
            return bottom - (sy + sh)
        }

        fun scrollToBottom(scrollView: ScrollView) {
            val delta = getScrollViewBottomDelta(scrollView)
            scrollView.smoothScrollBy(0, delta)
        }
    }

    var text: String? = ""
    var translatedText: String? = ""
    var captionSource: String? = null

    //    var isStarted = false
//    var previousSubtitleStr = ""
    private val translateAPI = TranslateAPI()

    override fun onCreate(savedInstanceState: Bundle?) {
        instance = this
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.activity_main)
        GlobalAppContext.set(this.application)
        checkAccessibility()

/*        textView_transcript.setOnClickListener { v ->
            if (!textView_transcript.hasSelection()) {
                setText()
            }
        }

        textView_transcript2.setOnClickListener { v ->
            if (!textView_transcript.hasSelection()) {
                setText()
            }
        }*/

/*        scrollView.viewTreeObserver
            .addOnScrollChangedListener {
                val scrollY: Int = scrollView.scrollY
//                Log.d(TAG, "scrollY: $scrollY")
//                Log.d(TAG, "delta: ${getScrollViewBottomDelta(scrollView)}")
            }*/

        val intent =
            Intent(ACTION_BROADCAST)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

        LocalBroadcastManager.getInstance(this).registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    captionSource =
                        intent.getStringExtra(MyAccessibilityService.EXTRA_CAPTION_SOURCE)
                    val isTranslatedText = intent.getBooleanExtra(
                        EXTRA_IS_TRANSLATED_TEXT,
                        false
                    )
                    if (isTranslatedText)
                        translatedText = intent.getStringExtra(EXTRA_TEXT)
                    else
                        text = intent.getStringExtra(EXTRA_TEXT)
                    setText()
                }
            }, IntentFilter(MyAccessibilityService.ACTION_BROADCAST)
        )

        translateAPI.setTranslateListener(object : TranslateAPI.TranslateListener {
            override fun onSuccess(translatedText: String?) {
//                Log.d(TAG, "onSuccess: $translatedText")
                if (translatedText == null) return
                textView_transcript2.text = translatedText
                scrollView2.post { scrollToBottom(scrollView2) }
            }

            override fun onFailure(ErrorText: String?) {
                Log.d(TAG, "onFailure: $ErrorText")
            }
        })
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        EasyFloat.dismissAppFloat(TAG_SCALE_FLOAT)
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
//        MyAccessibilityService.instance?.showEasyFloat()
        super.onPause()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }

/*    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "onSaveInstanceState")
        outState.putString("text", text)
        outState.putString("translatedText", translatedText)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.d(TAG, "onRestoreInstanceState")
        text = savedInstanceState.getString("text")
        translatedText = savedInstanceState.getString("translatedText")
    }*/

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

    private fun setText() {
        val startSelection2: Int = textView_transcript2.selectionStart
        val endSelection2: Int = textView_transcript2.selectionEnd
        if (text != null
            && startSelection2 == endSelection2
            && getScrollViewBottomDelta(scrollView2) < 10
        ) {
/*            if (captionSource == EXTRA_FROM_LIVE_CAPTION) {
                textView_transcript.text = text
            } else if (captionSource == EXTRA_FROM_YOUTUBE_CAPTION) {
                textView_transcript.append("\n\n" + text)
            }*/
            textView_transcript.text = text
            scrollView.post { scrollToBottom(scrollView) }
            textView_transcript2.text = translatedText
            scrollView2.post { scrollToBottom(scrollView2) }
        }
    }

}
