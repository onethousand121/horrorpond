package com.horrorpond.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * /api/admin/** 요청의 X-Admin-Key를 환경변수 ADMIN_API_KEY와 상수 시간 비교한다.
 * 키가 설정되지 않았으면 모든 admin 요청을 거부한다 (fail-closed). 키 값은 로그에 남기지 않는다.
 */
@Slf4j
public class AdminKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Admin-Key";

    private static final String FORBIDDEN_BODY = "{\"code\":\"FORBIDDEN\",\"message\":\"Admin key required\"}";

    private final byte[] expectedKey;

    public AdminKeyFilter(String adminApiKey) {
        this.expectedKey = adminApiKey == null || adminApiKey.isBlank()
                ? null
                : adminApiKey.getBytes(StandardCharsets.UTF_8);
        if (expectedKey == null) {
            log.warn("ADMIN_API_KEY is not set; all /api/admin requests will be rejected");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (expectedKey != null && provided != null
                && MessageDigest.isEqual(expectedKey, provided.getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(request, response);
            return;
        }
        log.warn("Rejected admin request: {} {} (reason={})", request.getMethod(), request.getRequestURI(),
                expectedKey == null ? "key not configured" : provided == null ? "missing header" : "invalid key");
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(FORBIDDEN_BODY);
    }
}
