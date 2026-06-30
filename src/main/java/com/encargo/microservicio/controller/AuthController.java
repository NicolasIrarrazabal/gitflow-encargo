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
 * Controlador de autenticación. Expone login y un health check propio.
 * Instrumentado con Micrometer para IE1 (Prometheus/CloudWatch).
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
     * Recibe credenciales y retorna un token si son válidas, 401 si no.
     * Emite auth.login.attempts, auth.login.duration y auth.tokens.issued.
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
     * Usado por Docker/el pipeline para chequear que el servicio está vivo.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}