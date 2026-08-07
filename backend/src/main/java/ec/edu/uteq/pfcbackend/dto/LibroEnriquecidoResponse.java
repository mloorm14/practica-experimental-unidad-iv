package ec.edu.uteq.pfcbackend.dto;

// Campos de Open Library null si el ISBN no se encontro alli o el servicio no respondio;
// el libro base (libro) siempre esta presente, este endpoint nunca falla solo por Open Library.
public record LibroEnriquecidoResponse(
        LibroResponse libro,
        String tituloOpenLibrary,
        String coverUrl,
        Integer numeroPaginas,
        String descripcionOpenLibrary
) {
}
