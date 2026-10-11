package com.horrorpond.ingestion.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AlertMailerTest {

    private static final AlertProperties PROPS =
            new AlertProperties("https://resend.test", "keeper@example.com", "lurkpond <onboarding@resend.dev>");

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    private AlertMailer mailer(AlertProperties props, String key) {
        return new AlertMailer(SteamClientConfig.configureResend(builder, props, key).build(), props, key);
    }

    @Test
    void sendsOneMailWithBearerKey() {
        server.expect(requestTo("https://resend.test/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer re_test"))
                .andExpect(jsonPath("$.to[0]").value("keeper@example.com"))
                .andExpect(jsonPath("$.from").value("lurkpond <onboarding@resend.dev>"))
                .andExpect(jsonPath("$.subject").value("[lurkpond] 수집 실패: ITCH"))
                .andExpect(jsonPath("$.text").value("본문"))
                .andRespond(withSuccess("{\"id\":\"1\"}", MediaType.APPLICATION_JSON));

        assertThat(mailer(PROPS, "re_test").send("[lurkpond] 수집 실패: ITCH", "본문")).isTrue();
        server.verify();
    }

    @Test
    void withoutKeyOrRecipientNothingIsSent() {
        assertThat(mailer(PROPS, "").isConfigured()).isFalse();
        assertThat(mailer(PROPS, "").send("s", "t")).isFalse();
        AlertProperties noRecipient = new AlertProperties("https://resend.test", null, "x <a@b.c>");
        assertThat(mailer(noRecipient, "re_test").send("s", "t")).isFalse();
        server.verify();
    }

    @Test
    void failureIsLoggedNotThrown() {
        server.expect(requestTo("https://resend.test/emails")).andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT));

        assertThat(mailer(PROPS, "re_test").send("s", "t")).isFalse();
        server.verify();
    }
}
