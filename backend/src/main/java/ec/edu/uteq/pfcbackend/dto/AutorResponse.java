package ec.edu.uteq.pfcbackend.dto;

import java.time.LocalDateTime;

public record AutorResponse(
        Long id,
        String nombre,
        String nacionalidad,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
