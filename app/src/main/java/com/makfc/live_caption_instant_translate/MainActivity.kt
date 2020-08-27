package com.makfc.live_caption_instant_translate

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lzf.easyfloat.EasyFloat
import com.makfc.live_caption_instant_translate.service.AccessibilityServiceTool
import com.makfc.live_caption_instant_translate.service.GlobalAppContext
import com.makfc.live_caption_instant_translate.service.MyAccessibilityService
import com.makfc.live_caption_instant_translate.service.MyAccessibilityService.Companion.EXTRA_IS_TRANSLATED_TEXT
import com.makfc.live_caption_instant_translate.service.MyAccessibilityService.Companion.EXTRA_TEXT
import com.makfc.live_caption_instant_translate.service.MyAccessibilityService.Companion.TAG_SCALE_FLOAT
import com.makfc.live_caption_instant_translate.translate_api.TranslateAPI
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity() {
    companion object {
        var instance: MainActivity? = null
        const val TAG = BuildConfig.APPLICATION_ID
        val ACTION_BROADCAST =
            MainActivity::class.java.name + "Broadcast"
        var isOnPause: Boolean = true

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

    var fullTranscript: String = ""
    var translatedText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        instance = this
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.activity_main)
        GlobalAppContext.set(this.application)
        checkAccessibility()
        checkCanDrawOverlays()

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
//                Log.d(TAG, "scrollY: $scrollY")
//                Log.d(TAG, "delta: ${getScrollViewBottomDelta(scrollView)}")
            }*/

        LocalBroadcastManager.getInstance(this).registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val isTranslatedText = intent.getBooleanExtra(
                        EXTRA_IS_TRANSLATED_TEXT,
                        false
                    )
                    val extraText = intent.getStringExtra(EXTRA_TEXT) ?: ""
//                    Log.d(TAG, "EXTRA_IS_TRANSLATED_TEXT: $isTranslatedText")
//                    Log.d(TAG, "EXTRA_TEXT: $extraText")
                    if (isTranslatedText) {
                        if (extraText == translatedText) return
                        translatedText = extraText
                        setText()
                    } else {
                        fullTranscript = extraText
                    }
                }
            }, IntentFilter(MyAccessibilityService.ACTION_BROADCAST)
        )
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        isOnPause = false
        EasyFloat.dismissAppFloat(TAG_SCALE_FLOAT)
        val intent = Intent(ACTION_BROADCAST)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        isOnPause = true
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

    @ExperimentalCoroutinesApi
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.translateAll -> {
                if (fullTranscript.isEmpty()) return true
                val preProcessText = fullTranscript.replace("\n", "\\n")
                scrollView.post { scrollToBottom(scrollView) }
                GlobalScope.launch(Dispatchers.Main) {
                    val translateResult =
                        withContext(Dispatchers.IO) {
                            TranslateAPI.translate(preProcessText)
                        } ?: return@launch
                    textView_transcript2.text = translateResult.dualLangText
                    scrollView2.post { scrollToBottom(scrollView2) }
                }
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
            putExtra(Intent.EXTRA_TEXT, fullTranscript)
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

    private fun checkCanDrawOverlays() {
        if (!Settings.canDrawOverlays(this)) {
            val builder = AlertDialog.Builder(this)

            builder.setMessage("Open Draw Overlays Permission?")
                .setTitle("Permission Request")
                .setPositiveButton("OK") { dialog, which ->
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                    )
                }
            val dialog = builder.create()
            dialog.show()
        }
    }

    private fun setText() {
        val startSelection2: Int = textView_transcript2.selectionStart
        val endSelection2: Int = textView_transcript2.selectionEnd
        if (translatedText.isNotEmpty()
            && startSelection2 == endSelection2
            && getScrollViewBottomDelta(scrollView2) < 100
        ) {
//            Log.d(TAG, "translatedText: $translatedText")
//            textView_transcript.text = text
//            scrollView.post { scrollToBottom(scrollView) }
            textView_transcript2.text = translatedText
            scrollView2.post { scrollToBottom(scrollView2) }
        }
    }

}
