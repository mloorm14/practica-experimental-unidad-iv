package ec.edu.uteq.pfcbackend.client;

import ec.edu.uteq.pfcbackend.dto.OpenLibraryResponse;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenLibraryClient {

    private static final String CACHE_ISBN = "openlibrary_isbn";
    private static final Duration TIMEOUT_RESPUESTA = Duration.ofSeconds(10);

    private final WebClient openLibraryWebClient;

    // Solo se cachea si el resultado es definitivo (encontrado o confirmado 404);
    // un fallo transitorio del servicio no debe quedar cacheado 24h.
    @Cacheable(value = CACHE_ISBN, key = "#isbn",
            unless = "#result.getClass().getSimpleName().equals('ServicioNoDisponible')")
    public OpenLibraryResult buscarPorIsbn(String isbn) {
        // El seed guarda el ISBN con guiones (ej. "978-0-451-52493-5"), pero Open Library
        // identifica sus recursos por el ISBN sin separadores (ej. "9780451524935").
        String isbnNormalizado = isbn.replaceAll("[^0-9Xx]", "");
        try {
            OpenLibraryResponse datos = openLibraryWebClient.get()
                    .uri("/isbn/{isbn}.json", isbnNormalizado)
                    .retrieve()
                    .bodyToMono(OpenLibraryResponse.class)
                    .timeout(TIMEOUT_RESPUESTA)
                    .block();

            return datos != null ? new OpenLibraryResult.Encontrado(datos) : new OpenLibraryResult.NoEncontrado();
        } catch (WebClientResponseException.NotFound ex) {
            log.debug("ISBN {} no encontrado en Open Library", isbn);
            return new OpenLibraryResult.NoEncontrado();
        } catch (WebClientResponseException ex) {
            log.warn("Open Library respondio con error {} para ISBN {}", ex.getStatusCode(), isbn);
            return new OpenLibraryResult.ServicioNoDisponible();
        } catch (WebClientRequestException ex) {
            log.warn("Open Library no disponible para ISBN {}: {}", isbn, ex.getMessage());
            return new OpenLibraryResult.ServicioNoDisponible();
        } catch (RuntimeException ex) {
            // Mono.timeout(Duration) propaga TimeoutException envuelta en una RuntimeException
            // (Reactor no puede relanzar una excepcion checked a traves de .block()).
            if (ex.getCause() instanceof TimeoutException) {
                log.warn("Timeout al consultar Open Library para ISBN {}", isbn);
                return new OpenLibraryResult.ServicioNoDisponible();
            }
            throw ex;
        }
    }
}
