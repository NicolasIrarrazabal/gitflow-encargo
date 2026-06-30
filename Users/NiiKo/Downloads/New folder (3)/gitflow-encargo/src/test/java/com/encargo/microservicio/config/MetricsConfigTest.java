package com.encargo.microservicio.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para MetricsConfig.
 * Verifica que los beans de métricas se registran correctamente
 * en el MeterRegistry con sus nombres y tags esperados.
 */
class MetricsConfigTest {

    private MetricsConfig metricsConfig;
    private SimpleMeterRegistry registry;

    @BeforeEach
    void setUp() {
        metricsConfig = new MetricsConfig();
        registry = new SimpleMeterRegistry();
    }

    @Test
    void loginSuccessCounter_debeRegistrarseConTagSuccess() {
        Counter counter = metricsConfig.loginSuccessCounter(registry);

        assertThat(counter).isNotNull();
        assertThat(counter.getId().getName()).isEqualTo("auth.login.attempts");
        assertThat(counter.getId().getTag("result")).isEqualTo("success");
    }

    @Test
    void loginFailureCounter_debeRegistrarseConTagFailure() {
        Counter counter = metricsConfig.loginFailureCounter(registry);

        assertThat(counter).isNotNull();
        assertThat(counter.getId().getName()).isEqualTo("auth.login.attempts");
        assertThat(counter.getId().getTag("result")).isEqualTo("failure");
    }

    @Test
    void loginTimer_debeRegistrarseConNombreCorrecto() {
        Timer timer = metricsConfig.loginTimer(registry);

        assertThat(timer).isNotNull();
        assertThat(timer.getId().getName()).isEqualTo("auth.login.duration");
    }

    @Test
    void tokenIssuedCounter_debeRegistrarseConNombreCorrecto() {
        Counter counter = metricsConfig.tokenIssuedCounter(registry);

        assertThat(counter).isNotNull();
        assertThat(counter.getId().getName()).isEqualTo("auth.tokens.issued");
    }
}
