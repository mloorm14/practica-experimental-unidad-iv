package ec.edu.uteq.pfcbackend.controller;

import ec.edu.uteq.pfcbackend.dto.ApiResponse;
import ec.edu.uteq.pfcbackend.dto.LibroEnriquecidoResponse;
import ec.edu.uteq.pfcbackend.dto.LibroRequest;
import ec.edu.uteq.pfcbackend.dto.LibroResponse;
import ec.edu.uteq.pfcbackend.service.LibroService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/libros")
@RequiredArgsConstructor
@Tag(name = "Libros", description = "Gestion del inventario de libros")
public class LibroController {

    private final LibroService libroService;

    @GetMapping
    @Operation(summary = "Listar libros, con filtro opcional por titulo")
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
    @Operation(summary = "Obtener un libro por id")
    public ApiResponse<LibroResponse> obtenerPorId(@PathVariable Long id) {
        return ApiResponse.success(libroService.obtenerPorId(id));
    }

    @GetMapping("/{id}/enriquecido")
    @Operation(
            summary = "Obtener un libro con metadatos externos de Open Library",
            description = "Combina el libro con portada, numero de paginas y descripcion obtenidos de "
                    + "Open Library buscando por ISBN. Si Open Library no tiene el ISBN o no responde "
                    + "(caido, timeout), la respuesta sigue siendo 200 con el libro base y los campos "
                    + "externos en null - este endpoint nunca falla solo por Open Library."
    )
    public ApiResponse<LibroEnriquecidoResponse> obtenerEnriquecido(@PathVariable Long id) {
        return ApiResponse.success(libroService.obtenerEnriquecido(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un libro")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LibroResponse> crear(@Valid @RequestBody LibroRequest request) {
        return ApiResponse.success(libroService.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un libro existente")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LibroResponse> actualizar(@PathVariable Long id, @Valid @RequestBody LibroRequest request) {
        return ApiResponse.success(libroService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un libro")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        libroService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
