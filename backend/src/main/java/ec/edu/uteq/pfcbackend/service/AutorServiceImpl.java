package ec.edu.uteq.pfcbackend.service;

import ec.edu.uteq.pfcbackend.dto.AutorRequest;
import ec.edu.uteq.pfcbackend.dto.AutorResponse;
import ec.edu.uteq.pfcbackend.entity.Autor;
import ec.edu.uteq.pfcbackend.exception.BusinessException;
import ec.edu.uteq.pfcbackend.exception.ResourceNotFoundException;
import ec.edu.uteq.pfcbackend.repository.AutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AutorServiceImpl implements AutorService {

    private final AutorRepository autorRepository;

    @Override
    public Page<AutorResponse> listar(Pageable pageable) {
        return autorRepository.findAll(pageable).map(this::aResponse);
    }

    @Override
    public AutorResponse obtenerPorId(Long id) {
        return aResponse(buscarOFallar(id));
    }

    @Override
    @Transactional
    public AutorResponse crear(AutorRequest request) {
        autorRepository.findByNombreIgnoreCase(request.nombre()).ifPresent(a -> {
            throw new BusinessException("Ya existe un autor con el nombre: " + request.nombre());
        });

        Autor autor = Autor.builder()
                .nombre(request.nombre())
                .nacionalidad(request.nacionalidad())
                .build();

        return aResponse(autorRepository.save(autor));
    }

    @Override
    @Transactional
    public AutorResponse actualizar(Long id, AutorRequest request) {
        Autor autor = buscarOFallar(id);

        autorRepository.findByNombreIgnoreCase(request.nombre())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(a -> {
                    throw new BusinessException("Ya existe un autor con el nombre: " + request.nombre());
                });

        autor.setNombre(request.nombre());
        autor.setNacionalidad(request.nacionalidad());

        return aResponse(autorRepository.save(autor));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Autor autor = buscarOFallar(id);
        autorRepository.delete(autor);
    }

    private Autor buscarOFallar(Long id) {
        return autorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Autor no encontrado con id: " + id));
    }

    private AutorResponse aResponse(Autor autor) {
        return new AutorResponse(
                autor.getId(),
                autor.getNombre(),
                autor.getNacionalidad(),
                autor.getCreatedAt(),
                autor.getUpdatedAt()
        );
    }
}
