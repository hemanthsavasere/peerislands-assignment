package com.peerislands.orders.common;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorTest {
    @Test
    void constructorPopulatesAllFields() {
        Instant t = Instant.parse("2026-07-06T12:00:00Z");
        ApiError err = new ApiError(t, 409, "Conflict", "msg", "/api/v1/products");
        assertThat(err.getTimestamp()).isEqualTo(t);
        assertThat(err.getStatus()).isEqualTo(409);
        assertThat(err.getError()).isEqualTo("Conflict");
        assertThat(err.getMessage()).isEqualTo("msg");
        assertThat(err.getPath()).isEqualTo("/api/v1/products");
    }
}
