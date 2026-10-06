package com.horrorpond;

import com.horrorpond.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class HorrorpondApplicationTests {

    @Test
    void contextLoads() {
        // Flyway V1 적용 + ddl-auto=validate 통과 확인
    }
}
