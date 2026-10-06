package com.horrorpond.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminKeyFilterConfig {

    /**
     * 키는 환경변수 ADMIN_API_KEY로만 주입한다 (yml 기본값 금지). 없으면 빈 값 → fail-closed.
     */
    @Bean
    public FilterRegistrationBean<AdminKeyFilter> adminKeyFilter(@Value("${ADMIN_API_KEY:}") String adminApiKey) {
        FilterRegistrationBean<AdminKeyFilter> registration = new FilterRegistrationBean<>(new AdminKeyFilter(adminApiKey));
        registration.addUrlPatterns("/api/admin/*");
        return registration;
    }
}
