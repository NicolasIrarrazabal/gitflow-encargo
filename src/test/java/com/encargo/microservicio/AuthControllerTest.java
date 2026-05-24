package com.encargo.microservicio;

import com.encargo.microservicio.controller.AuthController;
import com.encargo.microservicio.model.Usuario;
import com.encargo.microservicio.service.AuthService;
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

    @BeforeEach
    void setUp() {
        authService = Mockito.mock(AuthService.class);
        authController = new AuthController(authService);
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
}
