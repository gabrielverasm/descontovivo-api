package br.com.descontovivo.promotion.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotUserAgentDetectorTest {

    @Test void treatsMissingOrBlankUserAgentAsBot() {
        assertTrue(BotUserAgentDetector.isBot(null));
        assertTrue(BotUserAgentDetector.isBot(""));
        assertTrue(BotUserAgentDetector.isBot("   "));
    }

    @Test void detectsKnownSearchEngineCrawlers() {
        assertTrue(BotUserAgentDetector.isBot("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"));
        assertTrue(BotUserAgentDetector.isBot("Mozilla/5.0 (compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm)"));
        assertTrue(BotUserAgentDetector.isBot("DuckDuckBot/1.1"));
        assertTrue(BotUserAgentDetector.isBot("Mozilla/5.0 (compatible; YandexBot/3.0)"));
        assertTrue(BotUserAgentDetector.isBot("Baiduspider+(+http://www.baidu.com/search/spider.htm)"));
        assertTrue(BotUserAgentDetector.isBot("Mozilla/5.0 (compatible; SemrushBot/7~bl)"));
        assertTrue(BotUserAgentDetector.isBot("Mozilla/5.0 (compatible; AhrefsBot/7.0)"));
    }

    @Test void detectsSocialLinkPreviewFetchers() {
        assertTrue(BotUserAgentDetector.isBot("facebookexternalhit/1.1"));
        assertTrue(BotUserAgentDetector.isBot("WhatsApp/2.23.20.0"));
        assertTrue(BotUserAgentDetector.isBot("TelegramBot (like TwitterBot)"));
        assertTrue(BotUserAgentDetector.isBot("Mozilla/5.0 (compatible; Discordbot/2.0; +https://discordapp.com)"));
    }

    @Test void detectsCommonHttpClientsAndHeadlessBrowsers() {
        assertTrue(BotUserAgentDetector.isBot("curl/8.4.0"));
        assertTrue(BotUserAgentDetector.isBot("Wget/1.21.3"));
        assertTrue(BotUserAgentDetector.isBot("python-requests/2.31.0"));
        assertTrue(BotUserAgentDetector.isBot("Mozilla/5.0 HeadlessChrome/120.0.0.0"));
        assertTrue(BotUserAgentDetector.isBot("Mozilla/5.0 (compatible; PhantomJS/2.1.1)"));
    }

    @Test void isCaseInsensitive() {
        assertTrue(BotUserAgentDetector.isBot("MOZILLA/5.0 (COMPATIBLE; GOOGLEBOT/2.1)"));
    }

    @Test void doesNotFlagRealBrowsers() {
        assertFalse(BotUserAgentDetector.isBot(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/120.0.0.0 Safari/537.36"));
        assertFalse(BotUserAgentDetector.isBot(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X) AppleWebKit/605.1.15 "
                        + "(KHTML, like Gecko) Version/17.1 Mobile/15E148 Safari/604.1"));
        assertFalse(BotUserAgentDetector.isBot(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 "
                        + "(KHTML, like Gecko) Version/17.1 Safari/605.1.15"));
    }
}
