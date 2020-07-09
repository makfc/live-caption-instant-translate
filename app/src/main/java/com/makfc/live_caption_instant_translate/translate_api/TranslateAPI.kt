package com.makfc.live_caption_instant_translate.translate_api

import android.annotation.SuppressLint
import android.os.AsyncTask
import android.text.TextUtils
import com.makfc.live_caption_instant_translate.translate_api.Token.msCookieManager
import org.json.JSONArray
import org.json.JSONException
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class TranslateAPI {
    companion object {
        var token: Token? = null
    }
    var resp: String? = null
    lateinit var langFrom: String
    lateinit var langTo: String
    lateinit var word: String

    fun translate(langFrom: String, langTo: String, text: String) {
        this.langFrom = langFrom
        this.langTo = langTo
        word = text
        val async = Async()
        async.execute()
    }

    @SuppressLint("StaticFieldLeak")
    internal inner class Async : AsyncTask<String?, String?, String?>() {
        override fun doInBackground(vararg params: String?): String? {
            if (token == null)
                token = Token()

//            Log.d(MainActivity.TAG, "token: ${token!!.tkk[0]}")
            try {
                val url = "https://translate.googleapis.com/translate_a/single?" +
                        "anno=3" +
                        "&client=webapp" +
//                        "&format=text" +
                        "&v=1.0" +
                        "&key=" +
                        "&logld=" + URLEncoder.encode("vTE_20200506_00", "UTF-8") +
                        "&sl=" + langFrom +
                        "&tl=" + langTo +
                        "&hl=zh-TW" +
                        "&sp=nmt" +
                        "&tc=2" +
                        "&sr=1" +
                        "&tk=" + token!!.getToken(word) +
                        "&mode=1" +
                        "&dt=at&dt=bd&dt=ex&dt=ld&dt=md&dt=qca&dt=rw&dt=rm&dt=sos&dt=ss&dt=t" +
                        "&otf=1" +
                        "&pc=1&ssel=0&tsel=0&kc=2" //+
//                        "&q=" + URLEncoder.encode(word, "UTF-8")
                val q = "q=" + URLEncoder.encode(word, "UTF-8")
//                Log.d(TAG, "url: $url")
                val obj = URL(url)
                val con =
                    obj.openConnection() as HttpURLConnection
                con.requestMethod = "POST"
//                con.setRequestProperty("content-type", "application/x-www-form-urlencoded")
                con.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36"
                )
                con.setRequestProperty("Accept", "*/*")
//                con.setRequestProperty("X-Requested-With", "XMLHttpRequest")
//                con.setRequestProperty("Referer", "https://translate.google.com/?source=gtx")
//                con.setRequestProperty("Sec-Fetch-Site", "same-origin")
//                con.setRequestProperty("Sec-Fetch-Mode", "cors")
//                con.setRequestProperty("Sec-Fetch-Dest", "empty")
//                Log.d(MainActivity.TAG, "cookies: ${msCookieManager.cookieStore.cookies}")
                con.setRequestProperty(
                    "Cookie",
                    TextUtils.join(";", msCookieManager.cookieStore.cookies)
                )
                con.doOutput = true
                try {
                    val postData: ByteArray = q.toByteArray(StandardCharsets.UTF_8)
                    val outputStream = DataOutputStream(con.outputStream)
                    outputStream.write(postData)
                    outputStream.flush()
                } catch (exception: Exception) {
                }
                resp = con.inputStream.bufferedReader().readText()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(s: String?) {
            var temp = ""
            if (resp == null) {
                listener!!.onFailure("Network Error")
            } else {
                try {
//                    Log.d(MainActivity.TAG, "resp: $resp")
                    val main = JSONArray(resp)
                    val total = main[0] as JSONArray
                    var doubleNewLine = ""
                    for (i in 0 until total.length()) {
                        val currentLine = total[i] as JSONArray
                        if (currentLine[0].toString() == "null") continue
                        temp += doubleNewLine + currentLine[1].toString()
                            .replace("\\ n", "\n")
                            .replace("\\n", "\n")
                            .replace("\\", "")
                        temp += "\n" + currentLine[0].toString()
                            .replace("\\ n", "\n")
                            .replace("\\n", "\n")
                            .replace("\\", "")
                        doubleNewLine = "\n\n"
                    }
//                    Log.d(ContentValues.TAG, "onPostExecute: $temp")
                    if (temp.length > 2) {
                        listener!!.onSuccess(temp)
                    } else {
                        listener!!.onFailure("Invalid Input String")
                    }
                } catch (e: JSONException) {
                    listener!!.onFailure(e.localizedMessage ?: e.toString())
                    e.printStackTrace()
                }
            }
            super.onPostExecute(s)
        }
    }

    private var listener: TranslateListener? = null
    fun setTranslateListener(listener: TranslateListener?) {
        this.listener = listener
    }

    interface TranslateListener {
        fun onSuccess(translatedText: String)
        fun onFailure(ErrorText: String)
    }
}