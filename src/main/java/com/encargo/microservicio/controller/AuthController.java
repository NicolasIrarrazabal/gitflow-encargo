package com.encargo.microservicio.controller;

import com.encargo.microservicio.model.Usuario;
import com.encargo.microservicio.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    public ResponseEntity<Map<String, String>> registro(@Valid @RequestBody Usuario usuario) {
        boolean exito = authService.registrar(usuario);
        if (!exito) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("mensaje", "El usuario ya existe"));
        }
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Usuario registrado correctamente"));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody Usuario usuario) {
        boolean exito = authService.login(usuario);
        if (!exito) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Credenciales invalidas"));
        }
        return ResponseEntity
                .ok(Map.of("mensaje", "Login exitoso", "usuario", usuario.getUsername()));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("estado", "UP", "servicio", "microservicio-auth"));
    }
}
