package ec.edu.uteq.pfcbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AutorRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,

        @Size(max = 100, message = "La nacionalidad no puede superar los 100 caracteres")
        String nacionalidad
) {
}
