package com.horrorpond.common.error;

import com.horrorpond.common.domain.DomainStateException;
import com.horrorpond.common.domain.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void domainValidationExceptionIs400() throws Exception {
        mvc.perform(get("/validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("bad input"));
    }

    @Test
    void domainStateExceptionIs409() throws Exception {
        mvc.perform(get("/state"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"))
                .andExpect(jsonPath("$.message").value("bad state"));
    }

    @Test
    void dataIntegrityViolationIs409WithoutSqlDetails() throws Exception {
        mvc.perform(get("/integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Request conflicts with existing data"));
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/integrity")
        void integrity() {
            throw new DataIntegrityViolationException(
                    "duplicate key value violates unique constraint \"game_slug_key\"");
        }

        @GetMapping("/validation")
        void validation() {
            throw new DomainValidationException("bad input");
        }

        @GetMapping("/state")
        void state() {
            throw new DomainStateException("bad state");
        }
    }
}
