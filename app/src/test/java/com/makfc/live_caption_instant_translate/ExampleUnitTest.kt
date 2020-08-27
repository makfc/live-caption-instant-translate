package com.makfc.live_caption_instant_translate

import com.makfc.live_caption_instant_translate.service.MyAccessibilityService.Companion.getInsertedText
import org.junit.Assert
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun testGetInsertedText() {
        data class TestCase(val oldText: String, val newText: String, val expected: String)
        val textList = listOf(
            TestCase(
                "let's take a look at what we can do in this example",
                "this example the chat message",
                " the chat message"
            ),
            TestCase(
                "let's take a look at what we can do chat message in this example",
                "this example the chat message",
                " the chat message"
            ),
            TestCase(
                "the value of the text field won't actually",
                "the value of the text field won't actually change",
                " change"
            ),
            TestCase(
                "isn't an initial value it's actually the value of the text field",
                "value of the text field composable",
                " composable"
            )
        )
        for (strPair in textList) {
            val insertedText = getInsertedText(strPair.oldText, strPair.newText)
            println(insertedText)
            Assert.assertEquals(strPair.expected, insertedText)
        }
//        val text1 = "let's take a look at what we can do in this example"
//        val text2 = "this example the chat message"
//        val text1 = "let's take a look at what we can do chat message in this example"
//        val text2 = "this example the chat message"
//        val text1 = "the value of the text field won't actually"
//        val text2 = "the value of the text field won't actually change"
//        val text1 = "isn't an initial value it's actually the value of the text field"
//        val text2 = "value of the text field composable"
//        val insertedText = getInsertedText(text1, text2)
//        println(insertedText)
//        Assert.assertEquals(" composable", insertedText)
    }
}
