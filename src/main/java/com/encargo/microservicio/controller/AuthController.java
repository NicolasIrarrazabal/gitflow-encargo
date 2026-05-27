package com.encargo.microservicio.controller;

import com.encargo.microservicio.model.Usuario;
import com.encargo.microservicio.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de autenticación.
 * Expone los endpoints de login y health check del microservicio.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    /**
     * POST /auth/login
     * Recibe las credenciales del usuario y retorna un token si son válidas.
     * Si las credenciales son incorrectas, responde con 401 Unauthorized.
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody Usuario usuario) {
        boolean autenticado = authService.autenticar(usuario);

        if (autenticado) {
            String token = authService.generarToken(usuario);
            return ResponseEntity.ok(token);
        }

        // No revelamos si el usuario existe o no — solo "credenciales inválidas"
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales inválidas");
    }

    /**
     * GET /auth/health
     * Endpoint usado por Docker y el pipeline para verificar que el servicio está vivo.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
