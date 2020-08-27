package com.makfc.live_caption_instant_translate

import com.makfc.live_caption_instant_translate.service.MyAccessibilityService.Companion.getInsertedText
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun test() {
//        val text1 = "let's take a look at what we can do in this example"
//        val text1 = "let's take a look at what we can do chat message in this example"
//        val text2 = "this example the chat message"
//        val text1 = "the value of the text field won't actually"
//        val text2 = "the value of the text field won't actually change"
        val text1 = "isn't an initial value it's actually the value of the text field"
        val text2 = "value of the text field composable"

        val insertedText = getInsertedText(text1, text2)
        println(insertedText)
    }
}
