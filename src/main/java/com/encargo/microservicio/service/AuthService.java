package com.encargo.microservicio.service;

import com.encargo.microservicio.model.Usuario;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public boolean autenticar(Usuario usuario) {
        if (usuario == null || usuario.getUsername() == null || usuario.getPassword() == null) {
            return false;
        }
        // Lógica de autenticación básica
        return !usuario.getUsername().isBlank() && !usuario.getPassword().isBlank();
    }

    public String generarToken(Usuario usuario) {
        if (usuario == null || usuario.getUsername() == null) {
            return null;
        }
        // Token simulado basado en username
        return "token-" + usuario.getUsername() + "-" + System.currentTimeMillis();
    }
}
