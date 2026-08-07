package ec.edu.uteq.pfcbackend.controller;

import ec.edu.uteq.pfcbackend.dto.ApiResponse;
import ec.edu.uteq.pfcbackend.dto.LibroRequest;
import ec.edu.uteq.pfcbackend.dto.LibroResponse;
import ec.edu.uteq.pfcbackend.service.LibroService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/libros")
@RequiredArgsConstructor
public class LibroController {

    private final LibroService libroService;

    @GetMapping
    public ApiResponse<List<LibroResponse>> listar(@RequestParam(required = false) String titulo, Pageable pageable) {
        Page<LibroResponse> pagina = libroService.listar(titulo, pageable);
        Map<String, Object> meta = Map.of(
                "current_page", pagina.getNumber(),
                "total", pagina.getTotalElements(),
                "last_page", pagina.getTotalPages()
        );
        return ApiResponse.success(pagina.getContent(), null, meta);
    }

    @GetMapping("/{id}")
    public ApiResponse<LibroResponse> obtenerPorId(@PathVariable Long id) {
        return ApiResponse.success(libroService.obtenerPorId(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LibroResponse> crear(@Valid @RequestBody LibroRequest request) {
        return ApiResponse.success(libroService.crear(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<LibroResponse> actualizar(@PathVariable Long id, @Valid @RequestBody LibroRequest request) {
        return ApiResponse.success(libroService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        libroService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
