package ec.edu.uteq.pfcbackend.service;

import ec.edu.uteq.pfcbackend.dto.LibroEnriquecidoResponse;
import ec.edu.uteq.pfcbackend.dto.LibroRequest;
import ec.edu.uteq.pfcbackend.dto.LibroResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LibroService {

    Page<LibroResponse> listar(String titulo, Pageable pageable);

    LibroResponse obtenerPorId(Long id);

    LibroEnriquecidoResponse obtenerEnriquecido(Long id);

    LibroResponse crear(LibroRequest request);

    LibroResponse actualizar(Long id, LibroRequest request);

    void eliminar(Long id);
}
