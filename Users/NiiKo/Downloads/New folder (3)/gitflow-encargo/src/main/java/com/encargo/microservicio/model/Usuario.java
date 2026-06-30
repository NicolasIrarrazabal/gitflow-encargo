package com.encargo.microservicio.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @NotBlank(message = "El username no puede estar vacío")
    private String username;

    @NotBlank(message = "La password no puede estar vacía")
    private String password;
}
