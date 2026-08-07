package ec.edu.uteq.pfcbackend.controller;

import ec.edu.uteq.pfcbackend.dto.ApiResponse;
import ec.edu.uteq.pfcbackend.dto.EstadoLibroRequest;
import ec.edu.uteq.pfcbackend.dto.EstadoLibroResponse;
import ec.edu.uteq.pfcbackend.service.EstadoLibroService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/estados-libro")
@RequiredArgsConstructor
public class EstadoLibroController {

    private final EstadoLibroService estadoLibroService;

    @GetMapping
    public ApiResponse<List<EstadoLibroResponse>> listar(Pageable pageable) {
        Page<EstadoLibroResponse> pagina = estadoLibroService.listar(pageable);
        Map<String, Object> meta = Map.of(
                "current_page", pagina.getNumber(),
                "total", pagina.getTotalElements(),
                "last_page", pagina.getTotalPages()
        );
        return ApiResponse.success(pagina.getContent(), null, meta);
    }

    @GetMapping("/{id}")
    public ApiResponse<EstadoLibroResponse> obtenerPorId(@PathVariable Long id) {
        return ApiResponse.success(estadoLibroService.obtenerPorId(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EstadoLibroResponse> crear(@Valid @RequestBody EstadoLibroRequest request) {
        return ApiResponse.success(estadoLibroService.crear(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<EstadoLibroResponse> actualizar(@PathVariable Long id, @Valid @RequestBody EstadoLibroRequest request) {
        return ApiResponse.success(estadoLibroService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        estadoLibroService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
