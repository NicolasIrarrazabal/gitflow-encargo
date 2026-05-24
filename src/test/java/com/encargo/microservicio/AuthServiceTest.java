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
    void registrar_usuarioNuevo_retornaTrue() {
        Usuario usuario = new Usuario("juan", "pass123");
        assertTrue(authService.registrar(usuario));
    }

    @Test
    void registrar_usuarioDuplicado_retornaFalse() {
        Usuario usuario = new Usuario("juan", "pass123");
        authService.registrar(usuario);
        assertFalse(authService.registrar(usuario));
    }

    @Test
    void login_credencialesCorrectas_retornaTrue() {
        Usuario usuario = new Usuario("juan", "pass123");
        authService.registrar(usuario);
        assertTrue(authService.login(usuario));
    }

    @Test
    void login_passwordIncorrecta_retornaFalse() {
        authService.registrar(new Usuario("juan", "pass123"));
        assertFalse(authService.login(new Usuario("juan", "wrongpass")));
    }

    @Test
    void login_usuarioInexistente_retornaFalse() {
        assertFalse(authService.login(new Usuario("noexiste", "pass123")));
    }

    @Test
    void existeUsuario_registrado_retornaTrue() {
        authService.registrar(new Usuario("maria", "clave456"));
        assertTrue(authService.existeUsuario("maria"));
    }

    @Test
    void existeUsuario_noRegistrado_retornaFalse() {
        assertFalse(authService.existeUsuario("fantasma"));
    }

    @Test
    void cantidadUsuarios_refleja_totalCorrecto() {
        assertEquals(0, authService.cantidadUsuarios());
        authService.registrar(new Usuario("u1", "pass111"));
        authService.registrar(new Usuario("u2", "pass222"));
        assertEquals(2, authService.cantidadUsuarios());
    }
}
