package com.makfc.live_caption_instant_translate

import android.content.*
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.makfc.live_caption_instant_translate.service.MyAccessibilityService
import com.makfc.live_caption_instant_translate.service.MyAccessibilityService.Companion.EXTRA_TEXT_VIEW_TEXT
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = BuildConfig.APPLICATION_ID
    }

    var text: String? = ""

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_actions, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.translate -> {
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
                "com.google.android.apps.translate.TranslateActivity")
/*            component = ComponentName(
                "com.google.android.apps.translate",
                "com.google.android.apps.translate.copydrop.CopyDropActivity")*/
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView_transcript.setOnClickListener { v ->
            if (!textView_transcript.hasSelection()) {
                setText()
            }
        }

        scrollView.viewTreeObserver
            .addOnScrollChangedListener {
                val scrollY: Int = scrollView.scrollY
//                Log.i(TAG, "scrollY: $scrollY")
//                Log.i(TAG, "delta: ${getScrollViewBottomDelta(scrollView)}")
            }

        LocalBroadcastManager.getInstance(this).registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    text = intent.getStringExtra(EXTRA_TEXT_VIEW_TEXT)
                    setText()
                }
            }, IntentFilter(MyAccessibilityService.ACTION_BROADCAST)
        )

        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Log.d("WebView", consoleMessage.message())
                return true
            }
        }
//        webView.loadUrl("https://translate.google.com.hk/?hl=zh-TW")
        webView.loadUrl("https://www.bing.com/translator")
    }

    private fun setText() {
        val startSelection: Int = textView_transcript.selectionStart
        val endSelection: Int = textView_transcript.selectionEnd
        if (startSelection == endSelection && getScrollViewBottomDelta(scrollView) == 0) {
            textView_transcript.text = text
            scrollView.post { scrollToBottom(scrollView) }
            webView.evaluateJavascript("javascript:" +
                    "document.getElementById(\"tta_input_ta\").value = " +
                    "\"${text?.replace("\n","\\n")}\";" +
                    "document.getElementById(\"tta_input_ta\").click();" +
                    "window.scrollTo(0,document.body.scrollHeight);") {}
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
