package com.encargo.microservicio.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Métricas custom del microservicio de auth (IE1).
 * Se exportan vía Micrometer a Prometheus (/actuator/prometheus) y
 * a AWS CloudWatch.
 */
@Configuration
public class MetricsConfig {

    /**
     * Contador de logins exitosos.
     * Tag "result" = "success" para diferenciarlo del de fallos.
     */
    @Bean
    public Counter loginSuccessCounter(MeterRegistry registry) {
        return Counter.builder("auth.login.attempts")
                .description("Número total de intentos de login")
                .tag("result", "success")
                .register(registry);
    }

    /**
     * Contador de logins fallidos (credenciales inválidas).
     */
    @Bean
    public Counter loginFailureCounter(MeterRegistry registry) {
        return Counter.builder("auth.login.attempts")
                .description("Número total de intentos de login")
                .tag("result", "failure")
                .register(registry);
    }

    /**
     * Timer para medir latencia del endpoint /auth/login.
     * Permite calcular p50, p95, p99 y throughput.
     */
    @Bean
    public Timer loginTimer(MeterRegistry registry) {
        return Timer.builder("auth.login.duration")
                .description("Latencia del endpoint de login")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    /**
     * Contador de tokens generados.
     */
    @Bean
    public Counter tokenIssuedCounter(MeterRegistry registry) {
        return Counter.builder("auth.tokens.issued")
                .description("Número de tokens JWT emitidos")
                .register(registry);
    }
}