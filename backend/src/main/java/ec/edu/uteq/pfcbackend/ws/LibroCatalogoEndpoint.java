package ec.edu.uteq.pfcbackend.ws;

import ec.edu.uteq.pfcbackend.entity.Autor;
import ec.edu.uteq.pfcbackend.entity.Libro;
import ec.edu.uteq.pfcbackend.repository.LibroRepository;
import ec.edu.uteq.pfcbackend.ws.gen.ConsultarLibroPorIsbnRequest;
import ec.edu.uteq.pfcbackend.ws.gen.ConsultarLibroPorIsbnResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

// Paquete separado de "controller" a proposito: distingue el endpoint SOAP (contract-first,
// via libro-catalogo.xsd) de los controllers REST existentes.
@Endpoint
@RequiredArgsConstructor
public class LibroCatalogoEndpoint {

    private static final String NAMESPACE_URI = "http://uteq.edu.ec/pfc/libro-catalogo";

    private final LibroRepository libroRepository;

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "ConsultarLibroPorIsbnRequest")
    @ResponsePayload
    @Transactional(readOnly = true)
    public ConsultarLibroPorIsbnResponse consultarPorIsbn(@RequestPayload ConsultarLibroPorIsbnRequest request) {
        Libro libro = libroRepository.findByIsbnIgnoreCase(request.getIsbn())
                .orElseThrow(() -> new LibroNoEncontradoSoapException(
                        "No existe un libro con el ISBN: " + request.getIsbn()));

        ConsultarLibroPorIsbnResponse response = new ConsultarLibroPorIsbnResponse();
        response.setId(libro.getId());
        response.setTitulo(libro.getTitulo());
        response.setAutor(primerAutor(libro));
        response.setIsbn(libro.getIsbn());
        response.setEditorial(libro.getEditorial().getNombre());
        response.setIdioma(libro.getIdioma().getNombre());
        response.setEstado(libro.getEstado().getNombre());
        response.setStock(libro.getStock());
        return response;
    }

    // Prefiere el primer autor de la relacion N:M real; cae al String legacy si el libro
    // aun no tiene autores migrados en libro_autor.
    private String primerAutor(Libro libro) {
        return libro.getAutores().stream()
                .findFirst()
                .map(Autor::getNombre)
                .orElse(libro.getAutor());
    }
}
