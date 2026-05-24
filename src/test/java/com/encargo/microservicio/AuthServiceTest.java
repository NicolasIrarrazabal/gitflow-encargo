package com.encargo.microservicio;

import com.encargo.microservicio.model.Usuario;
import com.encargo.microservicio.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
    }

    @Test
    void autenticar_conCredencialesValidas_retornaTrue() {
        Usuario usuario = new Usuario("admin", "1234");
        assertTrue(authService.autenticar(usuario));
    }

    @Test
    void autenticar_conUsernameVacio_retornaFalse() {
        Usuario usuario = new Usuario("", "1234");
        assertFalse(authService.autenticar(usuario));
    }

    @Test
    void autenticar_conPasswordVacio_retornaFalse() {
        Usuario usuario = new Usuario("admin", "");
        assertFalse(authService.autenticar(usuario));
    }

    @Test
    void autenticar_conUsuarioNull_retornaFalse() {
        assertFalse(authService.autenticar(null));
    }

    @Test
    void generarToken_conUsuarioValido_retornaTokenNoNulo() {
        Usuario usuario = new Usuario("admin", "1234");
        String token = authService.generarToken(usuario);
        assertNotNull(token);
        assertTrue(token.startsWith("token-admin-"));
    }

    @Test
    void generarToken_conUsuarioNull_retornaNull() {
        assertNull(authService.generarToken(null));
    }
}
