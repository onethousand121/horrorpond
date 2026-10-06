package com.horrorpond.common.security;

import com.horrorpond.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ADMIN_API_KEY가 비어 있으면 어떤 키를 보내도 거부한다 (fail-closed).
 * 실행 환경에 같은 이름의 환경변수가 있어도 테스트 속성(빈 값)이 우선한다.
 */
@SpringBootTest(properties = "ADMIN_API_KEY=")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AdminKeyNotConfiguredTest {

    @Autowired
    MockMvc mvc;

    @Test
    void everyAdminRequestIs403() throws Exception {
        mvc.perform(get("/api/admin/ingestion/jobs")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/ingestion/jobs").header("X-Admin-Key", "")).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/ingestion/run").header("X-Admin-Key", "anything")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"steps\":[\"DISCOVERY\"]}"))
                .andExpect(status().isForbidden());
    }
}
