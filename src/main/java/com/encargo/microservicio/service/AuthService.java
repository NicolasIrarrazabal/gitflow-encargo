package com.encargo.microservicio.service;

import com.encargo.microservicio.model.Usuario;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    // Usuario hardcodeado para fines de demostracion y comprobacion.
    // En un entorno real las credenciales se validarian con una base de datos.
    private static final String USUARIO = "admin";
    private static final String PASSWORD = "1234";

    public boolean autenticar(Usuario usuario) {
        if (usuario == null || usuario.getUsername() == null || usuario.getPassword() == null) {
            return false;
        }
        return usuario.getUsername().equals(USUARIO) && usuario.getPassword().equals(PASSWORD);
    }

    public String generarToken(Usuario usuario) {
        if (usuario == null || usuario.getUsername() == null) {
            return null;
        }
        // Token simulado basado en username
        return "token-" + usuario.getUsername() + "-" + System.currentTimeMillis();
    }
}
