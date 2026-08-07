package ec.edu.uteq.pfcbackend.controller;

import ec.edu.uteq.pfcbackend.dto.ApiResponse;
import ec.edu.uteq.pfcbackend.dto.IdiomaRequest;
import ec.edu.uteq.pfcbackend.dto.IdiomaResponse;
import ec.edu.uteq.pfcbackend.service.IdiomaService;
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
@RequestMapping("/api/v1/idiomas")
@RequiredArgsConstructor
public class IdiomaController {

    private final IdiomaService idiomaService;

    @GetMapping
    public ApiResponse<List<IdiomaResponse>> listar(Pageable pageable) {
        Page<IdiomaResponse> pagina = idiomaService.listar(pageable);
        Map<String, Object> meta = Map.of(
                "current_page", pagina.getNumber(),
                "total", pagina.getTotalElements(),
                "last_page", pagina.getTotalPages()
        );
        return ApiResponse.success(pagina.getContent(), null, meta);
    }

    @GetMapping("/{id}")
    public ApiResponse<IdiomaResponse> obtenerPorId(@PathVariable Long id) {
        return ApiResponse.success(idiomaService.obtenerPorId(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IdiomaResponse> crear(@Valid @RequestBody IdiomaRequest request) {
        return ApiResponse.success(idiomaService.crear(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<IdiomaResponse> actualizar(@PathVariable Long id, @Valid @RequestBody IdiomaRequest request) {
        return ApiResponse.success(idiomaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        idiomaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
