package com.makfc.live_caption_instant_translate.translate_api;

import android.util.Log;

import com.makfc.live_caption_instant_translate.MainActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Token {

    private String[] tkk;
    private static final String COOKIES_HEADER = "Set-Cookie";
    static CookieManager msCookieManager = new CookieManager();

    public Token() {
        getTKK();
    }

    private boolean getTKK() {
        StringBuilder response = new StringBuilder();
        try {
            URL url = new URL("https://translate.google.com/");
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestProperty("User-Agent",
                    "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36");
            int responseCode = c.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                // set cookies
                Map<String, List<String>> headerFields = c.getHeaderFields();
                List<String> cookiesHeader = headerFields.get(COOKIES_HEADER);
                if (cookiesHeader != null) {
                    for (String cookie : cookiesHeader) {
                        msCookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
                    }
                }

                BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                int index = response.indexOf("tkk:'") + 5;
                tkk = response.substring(index, response.indexOf("'", index)).split("\\.");
                Log.d(MainActivity.TAG, "tkk: "+ String.join(".", tkk));
                Log.d(MainActivity.TAG, "cookies: "+ msCookieManager.getCookieStore().getCookies());
                return true;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getToken(String str) {
        if (tkk == null && !getTKK()) return null;

        Date date = new Date();
        long time = date.getTime();
        long now = (long) Math.floor((double) time / 3600000L);
        if (!tkk[0].equals(String.valueOf(now)) && !getTKK()) return null;

        long b = Long.parseLong(tkk[0]);

        ArrayList<Long> e = new ArrayList<>();

        for (int g = 0; g < str.length(); g++) {
            long k = str.charAt(g);
            if (k < 128) {
                e.add(k);
            } else {
                if (k < 2048) {
                    e.add(k >> 6 | 192);
                } else {
                    if (55296 == (k & 64512) && g + 1 < str.length() && 56320 == (str.charAt(g + 1) & 64512)) {
                        k = 65536 + ((k & 1023) << 10) + (str.charAt(++g) & 1023);
                        e.add(k >> 18 | 240);
                        e.add(k >> 12 & 63 | 128);
                    } else {
                        e.add(k >> 12 | 224);
                    }
                    e.add(k >> 6 & 63 | 128);
                }
                e.add(k & 63 | 128);
            }
        }
        int a = (int) b;
        for (int f = 0; f < e.size(); f++) {
            a += e.get(f);
            a = go(a, "+-a^+6");
        }
        a = go(a, "+-3^+b+-f");
        a ^= (int) Long.parseLong(tkk[1]);
        if (0 > a) {
            long aL = a & Integer.MAX_VALUE + 0x80000000L;
            aL %= 1e6;
            return aL + "." + (aL ^ b);
        }
        a %= 1000000;

        return a + "." + (a ^ b);
    }

    private int go(long in, String b) {
        int out = (int) in;
        for (int c = 0; c < b.length() - 2; c += 3) {
            int d = b.charAt(c + 2);
            d = 'a' <= d ? d - 87 : Integer.parseInt(String.valueOf((char) d));
            d = '+' == b.charAt(c + 1) ? out >>> d : out << d;
            out = '+' == b.charAt(c) ? out + d : out ^ d;
        }
        return out;
    }
}
