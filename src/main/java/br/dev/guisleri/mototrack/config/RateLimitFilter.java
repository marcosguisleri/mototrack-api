package br.dev.guisleri.mototrack.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Limita quantas vezes um mesmo IP pode chamar os endpoints sensíveis (login, cadastro,
 * recuperação de senha, troca de e-mail), para frear força bruta e criação de contas em massa.
 *
 * <p>Os contadores ficam em memória, por instância: basta para uma única instância da API.
 * Com várias instâncias seria preciso um armazenamento compartilhado (ex.: Redis).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private record Rule(String method, String path, int maxRequests, Duration window) {

        boolean matches(HttpServletRequest request) {
            return method.equals(request.getMethod()) && path.equals(request.getRequestURI());
        }

        String key() {
            return method + " " + path;
        }
    }

    private static final class Counter {

        private final long windowEnd;
        private int requests;

        private Counter(long windowEnd) {
            this.windowEnd = windowEnd;
        }
    }

    private static final List<Rule> RULES = List.of(
            new Rule("POST", "/auth/login", 10, Duration.ofMinutes(1)),
            new Rule("POST", "/auth/refresh", 60, Duration.ofMinutes(1)),
            new Rule("POST", "/users", 10, Duration.ofHours(1)),
            new Rule("POST", "/auth/forgot-password", 5, Duration.ofMinutes(10)),
            new Rule("POST", "/auth/reset-password", 10, Duration.ofMinutes(10)),
            new Rule("POST", "/users/me/email-change", 5, Duration.ofMinutes(10)),
            new Rule("POST", "/users/me/email-change/confirm", 10, Duration.ofMinutes(10))
    );

    private final boolean enabled;
    private final boolean trustForwardedFor;
    private final LongSupplier clockMillis;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitFilter(
            @Value("${mototrack.rate-limit.enabled:true}") boolean enabled,
            @Value("${mototrack.rate-limit.trust-forwarded-for:false}") boolean trustForwardedFor
    ) {
        this(enabled, trustForwardedFor, System::currentTimeMillis);
    }

    RateLimitFilter(boolean enabled, boolean trustForwardedFor, LongSupplier clockMillis) {
        this.enabled = enabled;
        this.trustForwardedFor = trustForwardedFor;
        this.clockMillis = clockMillis;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Rule rule = enabled ? ruleFor(request) : null;

        if (rule != null) {
            long retryAfterSeconds = registerAndGetRetryAfter(rule, clientIp(request));

            if (retryAfterSeconds > 0) {
                LOGGER.warn(
                        "Limite de requisições excedido: {} (IP {})",
                        rule.key(),
                        clientIp(request)
                );
                reject(response, retryAfterSeconds);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Rule ruleFor(HttpServletRequest request) {
        for (Rule rule : RULES) {
            if (rule.matches(request)) {
                return rule;
            }
        }

        return null;
    }

    /** Conta a requisição; devolve 0 se pode passar ou os segundos de espera se estourou. */
    private long registerAndGetRetryAfter(Rule rule, String ip) {
        long now = clockMillis.getAsLong();

        if (counters.size() > MAX_TRACKED_CLIENTS) {
            counters.values().removeIf(counter -> counter.windowEnd <= now);
        }

        long[] retryAfterMillis = {0};

        counters.compute(rule.key() + "|" + ip, (key, current) -> {
            Counter counter = current == null || current.windowEnd <= now
                    ? new Counter(now + rule.window().toMillis())
                    : current;

            if (counter.requests >= rule.maxRequests()) {
                retryAfterMillis[0] = counter.windowEnd - now;
            } else {
                counter.requests++;
            }

            return counter;
        });

        return retryAfterMillis[0] > 0 ? Math.max(1, (retryAfterMillis[0] + 999) / 1000) : 0;
    }

    /**
     * Atrás de um proxy (Railway, Render, nginx) o IP de origem vem em X-Forwarded-For. O proxy
     * acrescenta o IP real ao fim da lista, então só o último valor é confiável; os anteriores
     * podem ter sido forjados pelo cliente.
     */
    private String clientIp(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");

            if (forwarded != null && !forwarded.isBlank()) {
                String[] hops = forwarded.split(",");
                return hops[hops.length - 1].trim();
            }
        }

        return request.getRemoteAddr();
    }

    private void reject(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\","
                        + "\"message\":\"Muitas tentativas. Aguarde um pouco e tente de novo.\","
                        + "\"errors\":{}}"
        );
    }
}
