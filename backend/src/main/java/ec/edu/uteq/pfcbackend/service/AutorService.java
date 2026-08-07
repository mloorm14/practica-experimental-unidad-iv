package ec.edu.uteq.pfcbackend.service;

import ec.edu.uteq.pfcbackend.dto.AutorRequest;
import ec.edu.uteq.pfcbackend.dto.AutorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AutorService {

    Page<AutorResponse> listar(Pageable pageable);

    AutorResponse obtenerPorId(Long id);

    AutorResponse crear(AutorRequest request);

    AutorResponse actualizar(Long id, AutorRequest request);

    void eliminar(Long id);
}
