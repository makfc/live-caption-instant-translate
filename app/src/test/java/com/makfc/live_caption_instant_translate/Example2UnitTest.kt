package com.makfc.live_caption_instant_translate

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class Example2UnitTest {

    @Test
    fun testString() {
        val bodyStr = ")]}'\n" +
                "1b;[\"PKK8YKO7Odnm-Abm1ZK4Ag\"]\n" +
                "4;[2]\n" +
                "46a;<div jsname=\"Vinbg\" class=\"bbVIQb\"><div jsname=\"U8S5sf\" class=\"ujudUb WRZytc OULBYb\"><span jsname=\"UVGAte\"><span>將金屬棒彈回。</span><br></span><span jsname=\"YS01Ge\" class=\"gOGZsb\">bring pops the metal rod back up.</span><br><br><span jsname=\"UVGAte\"><span>所以你可以看到按下品絲和彈奏吉他是多麼方便。</span><br></span><span jsname=\"YS01Ge\" class=\"gOGZsb\">And so you can see how convenient these are to press the frets and strum the guitar.</span><br><br><span jsname=\"UVGAte\"><span>所有這些螺線管都連接到一個 8 通道 5 伏繼電器模塊，該模塊基本上可以通過編程非常快速地打開和關閉螺線管。</span><br></span><span jsname=\"YS01Ge\" class=\"gOGZsb\">All these solenoids are connected to an 8 channel 5 volt relay module which can basically programmatically turn the solenoids on and off very quickly.</span><br><br><span jsname=\"UVGAte\"><span>所有這些都連接到 Raspberry Pi。</span><br></span><span jsname=\"YS01Ge\" class=\"gOGZsb\">And all of that is connected to a Raspberry Pi with.</span><span>&hellip; </span></div></div><div jsname=\"WbKHeb\" class=\"bbVIQb\"><div jsname=\"U8S5sf\" class=\"ujudUb WRZytc u7wWjf\" data-mh=\"-1\"><span jsname=\"UVGAte\"><span> </span><br></span><span jsname=\"YS01Ge\" class=\"gOGZsb\"> </span></div></div>4;[9]\n" +
                "0;"
        val result = bodyStr
            .split(';')[3]
            .replace("<br>", "<br></br>")
        val doc: Document = Jsoup.parse(result)
        val spanElements: Elements = doc
            .select("div > div > span[jsname]:nth-child(odd) > span")
        val joinToString = spanElements.joinToString("") { e -> e.text() }
        print(joinToString)
    }
}
