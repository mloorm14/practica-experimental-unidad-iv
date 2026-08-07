package ec.edu.uteq.pfcbackend.controller;

import ec.edu.uteq.pfcbackend.dto.ApiResponse;
import ec.edu.uteq.pfcbackend.dto.EditorialRequest;
import ec.edu.uteq.pfcbackend.dto.EditorialResponse;
import ec.edu.uteq.pfcbackend.service.EditorialService;
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
@RequestMapping("/api/v1/editoriales")
@RequiredArgsConstructor
public class EditorialController {

    private final EditorialService editorialService;

    @GetMapping
    public ApiResponse<List<EditorialResponse>> listar(Pageable pageable) {
        Page<EditorialResponse> pagina = editorialService.listar(pageable);
        Map<String, Object> meta = Map.of(
                "current_page", pagina.getNumber(),
                "total", pagina.getTotalElements(),
                "last_page", pagina.getTotalPages()
        );
        return ApiResponse.success(pagina.getContent(), null, meta);
    }

    @GetMapping("/{id}")
    public ApiResponse<EditorialResponse> obtenerPorId(@PathVariable Long id) {
        return ApiResponse.success(editorialService.obtenerPorId(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EditorialResponse> crear(@Valid @RequestBody EditorialRequest request) {
        return ApiResponse.success(editorialService.crear(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<EditorialResponse> actualizar(@PathVariable Long id, @Valid @RequestBody EditorialRequest request) {
        return ApiResponse.success(editorialService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        editorialService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
