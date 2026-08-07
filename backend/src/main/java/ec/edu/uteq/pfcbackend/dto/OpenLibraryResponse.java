package ec.edu.uteq.pfcbackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

// Mapea solo los campos de /isbn/{isbn}.json que el sistema realmente usa
// (title, description, number_of_pages, covers), verificados contra una respuesta
// real de Open Library (ISBN 9780451524935). El resto del payload se ignora.
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenLibraryResponse(

        String title,

        @JsonDeserialize(using = DescripcionOpenLibraryDeserializer.class)
        String description,

        @JsonProperty("number_of_pages")
        Integer numeroPaginas,

        List<Long> covers
) {
    private static final String COVER_URL_TEMPLATE = "https://covers.openlibrary.org/b/id/%d-M.jpg";

    // Open Library devuelve solo IDs numericos de portada; la URL real se compone
    // con este patron (tamaño M = mediano). Se usa la primera portada disponible.
    public String coverUrl() {
        if (covers == null || covers.isEmpty()) {
            return null;
        }
        return COVER_URL_TEMPLATE.formatted(covers.get(0));
    }
}
