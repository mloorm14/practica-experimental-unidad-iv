package ec.edu.uteq.pfcbackend.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

// Alias exigido por el enunciado del SGA (/api/docs) hacia la ruta de Swagger UI
// exigida por el PDF de la guia (/api/documentation). El JSON crudo de OpenAPI vive
// en el path por defecto de springdoc (/v3/api-docs), asi que este path queda libre.
@Hidden
@RestController
public class DocsRedirectController {

    @GetMapping("/api/docs")
    public RedirectView redirigirADocumentacion() {
        return new RedirectView("/api/documentation");
    }
}
