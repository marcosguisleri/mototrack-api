package br.dev.guisleri.mototrack.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RateLimitFilterTest {

    private final AtomicLong now = new AtomicLong(1_000_000);

    private MockHttpServletResponse call(
            RateLimitFilter filter,
            String method,
            String path,
            String ip
    ) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        return response;
    }

    private RateLimitFilter filter() {
        return new RateLimitFilter(true, false, now::get);
    }

    @Test
    void shouldBlockTheEleventhLoginAttemptFromTheSameIp() throws Exception {
        RateLimitFilter filter = filter();

        for (int attempt = 1; attempt <= 10; attempt++) {
            assertThat(call(filter, "POST", "/auth/login", "1.1.1.1").getStatus())
                    .isEqualTo(200);
        }

        MockHttpServletResponse blocked = call(filter, "POST", "/auth/login", "1.1.1.1");

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isEqualTo("60");
        assertThat(blocked.getContentAsString()).contains("\"status\":429");
    }

    @Test
    void shouldCountEachIpSeparately() throws Exception {
        RateLimitFilter filter = filter();

        for (int attempt = 1; attempt <= 11; attempt++) {
            call(filter, "POST", "/auth/login", "1.1.1.1");
        }

        assertThat(call(filter, "POST", "/auth/login", "2.2.2.2").getStatus()).isEqualTo(200);
    }

    @Test
    void shouldAllowAgainWhenTheWindowEnds() throws Exception {
        RateLimitFilter filter = filter();

        for (int attempt = 1; attempt <= 11; attempt++) {
            call(filter, "POST", "/auth/login", "1.1.1.1");
        }

        now.addAndGet(60_000);

        assertThat(call(filter, "POST", "/auth/login", "1.1.1.1").getStatus()).isEqualTo(200);
    }

    @Test
    void shouldNotLimitOtherEndpoints() throws Exception {
        RateLimitFilter filter = filter();

        for (int attempt = 1; attempt <= 100; attempt++) {
            assertThat(call(filter, "GET", "/trips", "1.1.1.1").getStatus()).isEqualTo(200);
        }
    }

    @Test
    void shouldNotCallTheChainWhenBlocked() throws Exception {
        RateLimitFilter filter = filter();
        FilterChain chain = mock(FilterChain.class);

        for (int attempt = 1; attempt <= 5; attempt++) {
            MockHttpServletRequest request =
                    new MockHttpServletRequest("POST", "/auth/forgot-password");
            filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));
        }

        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/auth/forgot-password");
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(chain, never()).doFilter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void shouldUseTheLastForwardedAddressWhenBehindAProxy() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, true, now::get);

        for (int attempt = 1; attempt <= 11; attempt++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
            request.setRemoteAddr("10.0.0.1");
            // The first hop is client-supplied and changes on every request.
            request.addHeader("X-Forwarded-For", "9.9.9." + attempt + ", 3.3.3.3");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, mock(FilterChain.class));

            if (attempt == 11) {
                assertThat(response.getStatus()).isEqualTo(429);
            }
        }
    }

    @Test
    void shouldDoNothingWhenDisabled() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(false, false, now::get);

        for (int attempt = 1; attempt <= 50; attempt++) {
            assertThat(call(filter, "POST", "/auth/login", "1.1.1.1").getStatus())
                    .isEqualTo(200);
        }
    }
}
