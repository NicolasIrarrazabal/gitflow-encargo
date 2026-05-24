package com.encargo.microservicio.service;

import com.encargo.microservicio.model.Usuario;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final Map<String, String> usuarios = new HashMap<>();

    public boolean registrar(Usuario usuario) {
        if (usuarios.containsKey(usuario.getUsername())) {
            return false;
        }
        usuarios.put(usuario.getUsername(), usuario.getPassword());
        return true;
    }

    public boolean login(Usuario usuario) {
        String passwordGuardada = usuarios.get(usuario.getUsername());
        return passwordGuardada != null && passwordGuardada.equals(usuario.getPassword());
    }

    public boolean existeUsuario(String username) {
        return usuarios.containsKey(username);
    }

    public int cantidadUsuarios() {
        return usuarios.size();
    }
}
