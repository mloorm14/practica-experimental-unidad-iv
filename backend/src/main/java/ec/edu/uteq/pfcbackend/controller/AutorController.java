package ec.edu.uteq.pfcbackend.controller;

import ec.edu.uteq.pfcbackend.dto.ApiResponse;
import ec.edu.uteq.pfcbackend.dto.AutorRequest;
import ec.edu.uteq.pfcbackend.dto.AutorResponse;
import ec.edu.uteq.pfcbackend.service.AutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/autores")
@RequiredArgsConstructor
@Tag(name = "Autores", description = "Catalogo de autores")
public class AutorController {

    private final AutorService autorService;

    @GetMapping
    @Operation(summary = "Listar autores")
    public ApiResponse<List<AutorResponse>> listar(Pageable pageable) {
        Page<AutorResponse> pagina = autorService.listar(pageable);
        Map<String, Object> meta = Map.of(
                "current_page", pagina.getNumber(),
                "total", pagina.getTotalElements(),
                "last_page", pagina.getTotalPages()
        );
        return ApiResponse.success(pagina.getContent(), null, meta);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un autor por id")
    public ApiResponse<AutorResponse> obtenerPorId(@PathVariable Long id) {
        return ApiResponse.success(autorService.obtenerPorId(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un autor")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AutorResponse> crear(@Valid @RequestBody AutorRequest request) {
        return ApiResponse.success(autorService.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un autor existente")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AutorResponse> actualizar(@PathVariable Long id, @Valid @RequestBody AutorRequest request) {
        return ApiResponse.success(autorService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un autor")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        autorService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
