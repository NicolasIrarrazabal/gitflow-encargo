package com.encargo.microservicio;

import com.encargo.microservicio.controller.AuthController;
import com.encargo.microservicio.model.Usuario;
import com.encargo.microservicio.service.AuthService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthControllerTest {

    private AuthController authController;
    private AuthService authService;
    private Counter loginSuccessCounter;
    private Counter loginFailureCounter;
    private Counter tokenIssuedCounter;
    private Timer loginTimer;

    @BeforeEach
    void setUp() {
        authService = Mockito.mock(AuthService.class);

        // SimpleMeterRegistry: registry en memoria para tests
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        loginSuccessCounter = Counter.builder("auth.login.attempts")
                .tag("result", "success").register(registry);
        loginFailureCounter = Counter.builder("auth.login.attempts")
                .tag("result", "failure").register(registry);
        tokenIssuedCounter = Counter.builder("auth.tokens.issued")
                .register(registry);
        loginTimer = Timer.builder("auth.login.duration")
                .register(registry);

        authController = new AuthController(
                authService,
                loginSuccessCounter,
                loginFailureCounter,
                tokenIssuedCounter,
                loginTimer);
    }

    @Test
    void login_conCredencialesValidas_retornaOkConToken() {
        Usuario usuario = new Usuario("admin", "1234");
        when(authService.autenticar(usuario)).thenReturn(true);
        when(authService.generarToken(usuario)).thenReturn("token-admin-123");

        ResponseEntity<String> response = authController.login(usuario);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("token-admin-123", response.getBody());
    }

    @Test
    void login_conCredencialesInvalidas_retornaUnauthorized() {
        Usuario usuario = new Usuario("admin", "wrong");
        when(authService.autenticar(usuario)).thenReturn(false);

        ResponseEntity<String> response = authController.login(usuario);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Credenciales inválidas", response.getBody());
    }

    @Test
    void health_retornaOk() {
        ResponseEntity<String> response = authController.health();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OK", response.getBody());
    }

    @Test
    void login_autenticarEsLlamado() {
        Usuario usuario = new Usuario("admin", "1234");
        when(authService.autenticar(usuario)).thenReturn(true);
        when(authService.generarToken(usuario)).thenReturn("token-admin-123");

        authController.login(usuario);

        verify(authService, times(1)).autenticar(usuario);
    }

    /**
     * IE1 — Verifica que los contadores de Micrometer se incrementan
     * correctamente tras un login exitoso.
     */
    @Test
    void login_exitoso_incrementaContadores() {
        Usuario usuario = new Usuario("admin", "1234");
        when(authService.autenticar(usuario)).thenReturn(true);
        when(authService.generarToken(usuario)).thenReturn("token-admin-123");

        double successBefore = loginSuccessCounter.count();
        double tokensBefore = tokenIssuedCounter.count();

        authController.login(usuario);

        assertEquals(successBefore + 1.0, loginSuccessCounter.count(), 0.0001);
        assertEquals(tokensBefore + 1.0, tokenIssuedCounter.count(), 0.0001);
    }

    /**
     * IE1 — Verifica que el contador de fallos se incrementa tras
     * un login con credenciales inválidas.
     */
    @Test
    void login_fallido_incrementaContadorFallos() {
        Usuario usuario = new Usuario("admin", "wrong");
        when(authService.autenticar(usuario)).thenReturn(false);

        double failureBefore = loginFailureCounter.count();

        authController.login(usuario);

        assertEquals(failureBefore + 1.0, loginFailureCounter.count(), 0.0001);
    }
}