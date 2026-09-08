package br.com.descontovivo.promotion.support;

import java.util.Locale;

/**
 * Best-effort User-Agent based bot filter for the promotion view counter.
 *
 * <p>The counter is an approximate engagement metric, not an audit trail, so this
 * intentionally does not try to catch every bot -- it filters the well-known,
 * unambiguous ones (search engine crawlers, social link-preview fetchers, uptime
 * monitors, common HTTP client libraries) by a simple substring match.
 */
public final class BotUserAgentDetector {

    private static final String[] BOT_MARKERS = {
        // Generic markers -- also match most named search engine crawlers
        // (Googlebot, Bingbot, DuckDuckBot, YandexBot, SemrushBot, AhrefsBot,
        // MJ12bot, DotBot, PetalBot, TelegramBot, Discordbot, ...)
        "bot",
        "spider",
        "crawl",
        // Named crawlers/fetchers that don't contain the markers above
        "slurp",
        "facebookexternalhit",
        "whatsapp",
        "bingpreview",
        "pingdom",
        "uptimerobot",
        // Common non-browser HTTP clients
        "curl",
        "wget",
        "python-requests",
        "headlesschrome",
        "phantomjs",
    };

    private BotUserAgentDetector() {}

    /** A missing/blank User-Agent is treated as a bot: real browsers always send one. */
    public static boolean isBot(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return true;
        }
        String lower = userAgent.toLowerCase(Locale.ROOT);
        for (String marker : BOT_MARKERS) {
            if (lower.contains(marker)) {
                return true;
            }
        }
        return false;
    }
}
