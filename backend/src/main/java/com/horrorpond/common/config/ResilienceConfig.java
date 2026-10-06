package com.horrorpond.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

/**
 * Spring Framework 7 {@code @Retryable} / {@code @ConcurrencyLimit} 처리를 활성화한다.
 * Spring Boot는 이를 자동 구성하지 않으므로 명시적으로 선언해야 한다.
 */
@Configuration
@EnableResilientMethods
public class ResilienceConfig {
}
