package com.horrorpond.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 모든 테스트 컨텍스트가 PostgreSQL 컨테이너 1개를 공유한다.
 * 컨텍스트가 닫혀도 컨테이너를 멈추지 않으며(destroyMethod = ""), JVM 종료 시 Ryuk가 정리한다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @Bean(destroyMethod = "")
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return POSTGRES;
    }
}
