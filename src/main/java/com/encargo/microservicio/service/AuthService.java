package com.encargo.microservicio.service;

import com.encargo.microservicio.model.Usuario;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    // Para esta demo usamos credenciales fijas. En un proyecto real
    // esto vendría de una base de datos con contraseñas encriptadas.
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
        // Token de prueba: en producción esto sería un JWT firmado
        return "token-" + usuario.getUsername() + "-" + System.currentTimeMillis();
    }
}
