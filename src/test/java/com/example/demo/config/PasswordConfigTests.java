package com.example.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordConfigTests {

    private final PasswordEncoder passwordEncoder = new PasswordConfig().passwordEncoder();

    @Test
    void shouldHashAndVerifyPasswordWithBcrypt() {
        String hash = passwordEncoder.encode("correct-password");

        assertThat(hash).startsWith("$2");
        assertThat(passwordEncoder.matches("correct-password", hash)).isTrue();
        assertThat(passwordEncoder.matches("wrong-password", hash)).isFalse();
    }
}
