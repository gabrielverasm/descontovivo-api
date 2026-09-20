package br.com.descontovivo.promotion.inspection;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converte um link de produto da Amazon Brasil no link canônico com a tag de afiliado vigente.
 * Classe pura: só analisa a string recebida, sem rede nem banco.
 */
@ApplicationScoped
public class AmazonLinkCanonicalizer {
    private static final Logger LOG = Logger.getLogger(AmazonLinkCanonicalizer.class);
    private static final Set<String> HOSTS = Set.of("amazon.com.br", "www.amazon.com.br");
    private static final Set<String> DEAD_TAGS = Set.of("descontoviv0f-20", "descontovivo.com-20", "gatry0b-20");
    private static final Set<String> KEPT_PARAMS = Set.of("th", "psc", "smid");
    private static final Pattern PARAM_VALUE = Pattern.compile("[A-Za-z0-9]{1,32}");
    private static final Pattern ASIN_PATH = Pattern.compile(
            "(?:^|/)(?:dp|gp/product|gp/aw/d)/([A-Za-z0-9]{10})(?=/|$)");

    private final String affiliateTag;

    public AmazonLinkCanonicalizer(@ConfigProperty(name = "app.amazon.affiliate-tag") String affiliateTag) {
        if (affiliateTag == null || affiliateTag.isBlank()) {
            throw new IllegalStateException("app.amazon.affiliate-tag must not be blank");
        }
        if (DEAD_TAGS.contains(affiliateTag.strip().toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("app.amazon.affiliate-tag is a retired tag");
        }
        this.affiliateTag = affiliateTag.strip();
    }

    /** Vazio quando o link não é da Amazon Brasil ou não aponta para um produto (busca, home, etc.). */
    public Optional<CanonicalAmazonLink> canonicalize(String value) {
        URI uri;
        try {
            uri = URI.create(value == null ? "" : value.strip());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        String scheme = uri.getScheme();
        boolean web = "https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme);
        if (!web || uri.getHost() == null || !HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT))) {
            return Optional.empty();
        }
        Matcher matcher = ASIN_PATH.matcher(uri.getPath() == null ? "" : uri.getPath());
        if (!matcher.find()) return Optional.empty();
        String asin = matcher.group(1).toUpperCase(Locale.ROOT);

        var params = new TreeMap<String, String>();
        String rawQuery = uri.getRawQuery();
        if (rawQuery != null) {
            for (String pair : rawQuery.split("&")) {
                int eq = pair.indexOf('=');
                String key = (eq < 0 ? pair : pair.substring(0, eq)).toLowerCase(Locale.ROOT);
                String paramValue = eq < 0 ? "" : pair.substring(eq + 1);
                if (key.equals("tag")) {
                    if (DEAD_TAGS.contains(paramValue.toLowerCase(Locale.ROOT))) {
                        LOG.warnf("Amazon link carried retired affiliate tag %s; replaced by the current tag", paramValue);
                    }
                } else if (KEPT_PARAMS.contains(key) && PARAM_VALUE.matcher(paramValue).matches()) {
                    params.putIfAbsent(key, paramValue);
                }
            }
        }
        params.put("tag", affiliateTag);

        var url = new StringBuilder("https://www.amazon.com.br/dp/").append(asin);
        char separator = '?';
        for (var entry : params.entrySet()) {
            url.append(separator).append(entry.getKey()).append('=').append(entry.getValue());
            separator = '&';
        }
        return Optional.of(new CanonicalAmazonLink(asin, url.toString()));
    }
}
