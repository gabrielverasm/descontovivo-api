package br.com.descontovivo.promotion.inspection;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/**
 * Resolve links curtos da Amazon (link.amazon, amzn.to) seguindo os redirecionamentos um a um e
 * lendo apenas o cabeçalho Location. O corpo da resposta nunca é lido e a página do produto nunca é
 * requisitada: ao chegar em amazon.com.br a resolução termina.
 */
@ApplicationScoped
public class AmazonShortLinkResolver {
    static final int MAX_HOPS = 6;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Set<String> SHORT_HOSTS = Set.of("link.amazon", "amzlinks.in", "amzn.to");
    private static final Set<String> FINAL_HOSTS = Set.of("amazon.com.br", "www.amazon.com.br");

    private final HttpClient client;
    private final Set<String> shortHosts;
    private final Set<String> finalHosts;
    private final Set<String> schemes;
    private final int maxHops;
    private final Duration timeout;

    @Inject
    public AmazonShortLinkResolver() {
        this(SHORT_HOSTS, FINAL_HOSTS, Set.of("https"), MAX_HOPS, TIMEOUT);
    }

    AmazonShortLinkResolver(Set<String> shortHosts, Set<String> finalHosts, Set<String> schemes,
                            int maxHops, Duration timeout) {
        this.shortHosts = shortHosts;
        this.finalHosts = finalHosts;
        this.schemes = schemes;
        this.maxHops = maxHops;
        this.timeout = timeout;
        this.client = HttpClient.newBuilder().connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    public boolean isShortLink(String value) {
        try {
            String host = URI.create(value.strip()).getHost();
            return host != null && SHORT_HOSTS.contains(host.toLowerCase(Locale.ROOT));
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Devolve a URL final em amazon.com.br; falha com INSPECTION_FAILED se não for possível chegar lá. */
    public String resolve(String value) {
        URI current = parse(value);
        for (int hop = 0; hop <= maxHops; hop++) {
            String host = requireAllowed(current);
            if (finalHosts.contains(host)) return current.toString();
            if (hop == maxHops) break;
            current = nextLocation(current);
        }
        throw failure("Link curto com redirecionamentos demais");
    }

    private URI nextLocation(URI current) {
        HttpRequest request = HttpRequest.newBuilder().uri(current).timeout(timeout).GET().build();
        HttpResponse<java.io.InputStream> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw failure("Resolução do link curto interrompida");
        } catch (IOException | RuntimeException e) {
            throw failure("Não foi possível resolver o link curto");
        }
        try (var body = response.body()) {
            int status = response.statusCode();
            String location = response.headers().firstValue("Location").orElse(null);
            if (status < 300 || status >= 400 || location == null || location.isBlank()) {
                throw failure("O link curto não redireciona para um produto");
            }
            return current.resolve(parse(location.strip()));
        } catch (IOException e) {
            throw failure("Não foi possível resolver o link curto");
        }
    }

    private URI parse(String value) {
        try {
            return URI.create(value == null ? "" : value.strip());
        } catch (IllegalArgumentException e) {
            throw failure("Link inválido");
        }
    }

    private String requireAllowed(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!schemes.contains(scheme) || !(shortHosts.contains(host) || finalHosts.contains(host))) {
            throw failure("O link curto redireciona para um endereço não permitido");
        }
        return host;
    }

    private static MarketplaceInspectionException failure(String message) {
        return new MarketplaceInspectionException("INSPECTION_FAILED", message);
    }
}
