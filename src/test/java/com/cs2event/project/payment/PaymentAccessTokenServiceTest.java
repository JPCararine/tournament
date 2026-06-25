package com.cs2event.project.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PaymentAccessTokenServiceTest {

    private final PaymentAccessTokenService service = new PaymentAccessTokenService();

    @Test
    void generatesUrlSafeOpaqueToken() {
        String token = service.generateToken();

        assertThat(token).hasSize(43);
        assertThat(token).matches("^[A-Za-z0-9_-]+$");
    }

    @Test
    void hashesTokenDeterministicallyWithoutStoringRawValue() {
        String token = service.generateToken();

        String hash = service.hashToken(token);

        assertThat(hash).isEqualTo(service.hashToken(token));
        assertThat(hash).isNotEqualTo(token);
        assertThat(hash).hasSize(43);
    }
}
