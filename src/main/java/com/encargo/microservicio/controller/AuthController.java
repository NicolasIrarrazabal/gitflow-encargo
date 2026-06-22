package com.encargo.microservicio.controller;

import com.encargo.microservicio.model.Usuario;
import com.encargo.microservicio.service.AuthService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * Controlador de autenticación.
 * Expone los endpoints de login y health check del microservicio.
 *
 * IE1 — Instrumentado con Micrometer (contadores + timer) para
 * observabilidad vía Prometheus / CloudWatch.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter tokenIssuedCounter;
    private final Timer loginTimer;

    @Autowired
    public AuthController(
            AuthService authService,
            Counter loginSuccessCounter,
            Counter loginFailureCounter,
            Counter tokenIssuedCounter,
            Timer loginTimer) {
        this.authService = authService;
        this.loginSuccessCounter = loginSuccessCounter;
        this.loginFailureCounter = loginFailureCounter;
        this.tokenIssuedCounter = tokenIssuedCounter;
        this.loginTimer = loginTimer;
    }

    /**
     * POST /auth/login
     * Recibe las credenciales del usuario y retorna un token si son válidas.
     * Si las credenciales son incorrectas, responde con 401 Unauthorized.
     *
     * Métricas emitidas:
     *   - auth.login.attempts{result=success|failure}  (Counter)
     *   - auth.login.duration                          (Timer)
     *   - auth.tokens.issued                           (Counter)
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody Usuario usuario) {
        Timer.Sample sample = Timer.start();
        try {
            boolean autenticado = authService.autenticar(usuario);

            if (autenticado) {
                String token = authService.generarToken(usuario);
                loginSuccessCounter.increment();
                tokenIssuedCounter.increment();
                return ResponseEntity.ok(token);
            }

            // No revelamos si el usuario existe o no — solo "credenciales inválidas"
            loginFailureCounter.increment();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales inválidas");
        } finally {
            sample.stop(loginTimer);
        }
    }

    /**
     * GET /auth/health
     * Endpoint usado por Docker y el pipeline para verificar que el servicio está vivo.
     * (K8s livenessProbe / readinessProbe también consultan /actuator/health)
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}