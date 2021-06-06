package com.makfc.live_caption_instant_translate.translate_api

import android.util.Log
import com.makfc.live_caption_instant_translate.MainActivity.Companion.TAG
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.io.IOException
import java.net.URLEncoder
import kotlin.coroutines.resume

class TranslateAPI {
    companion object {
        fun replaceChar(text: String): String {
            return text
                .replace("\\ n", "\n")
                .replace("\\n", "\n")
                .replace("\\", "")
                .replace("，", "，\n")
                .replace("。", "。\n")
                .replace("\n\n", "\n")
        }

        @ExperimentalCoroutinesApi
        suspend fun translate(text: String): TranslateResult? =
            suspendCancellableCoroutine { cont ->
                val preProcessText = URLEncoder.encode(text, "UTF-8")

                val langCodeTo = "zh-Hant"

                val logging = HttpLoggingInterceptor()
                logging.setLevel(HttpLoggingInterceptor.Level.BASIC)
                val client = OkHttpClient()
                    .newBuilder()
                    .addInterceptor(logging)
                    .build()
                val request = Request.Builder()
                    .addHeader(
                        "user-agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.125 Safari/537.36"
                    )
                    .url("https://www.google.com/async/lyrics_translate?async=lyrics_partial:${preProcessText},lyrics_full:%20,title:%20,lang_code_from:en,lang_code_to:${langCodeTo},exp_ui_ctx:2,_id:gws-plugins-knowledge-verticals-music__translated-lyrics-container,_pms:s,_fmt:pc")
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        val bodyStr = response.body?.string()
//                        Log.d("HKT", "response: $bodyStr")

                        val result = bodyStr?.split(';')?.get(3)
                        val doc: Document = Jsoup.parse(result)
                        val spanElements: Elements = doc
                            .select("div > div > span[jsname]:nth-child(odd) > span")
                        val translatedText =
                            spanElements.joinToString("") { e -> replaceChar(e.text()) }
                        cont.resume(TranslateResult(translatedText, "$text\n$translatedText"))
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        Log.d(TAG, "onFailure: $e")
                        cont.resume(null)
                    }
                })
            }
    }

    data class TranslateResult(val translatedText: String, val dualLangText: String)
}