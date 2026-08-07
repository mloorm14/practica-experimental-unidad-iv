package ec.edu.uteq.pfcbackend.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

// Usa MockWebServer (OkHttp) en vez de WireMock: es mas liviano para simular respuestas
// HTTP simples (200/404/500/timeout) y es la opcion mas usada en la documentacion oficial
// de Spring para probar WebClient sin necesitar un servidor real ni un DSL de matching mas
// pesado del que hace falta aqui.
class OpenLibraryClientTest {

    private static final String JSON_1984_REAL = """
            {
              "title": "Nineteen Eighty-Four",
              "description": {"type": "/type/text", "value": "1984 has come and gone, but George Orwell's prophetic, nightmarish vision in 1949 of the world we were becoming is timelier than ever."},
              "number_of_pages": 328,
              "covers": [12054527],
              "isbn_13": ["9780451524935"]
            }
            """;

    private MockWebServer servidor;

    @AfterEach
    void tearDown() throws IOException {
        if (servidor != null) {
            servidor.shutdown();
        }
    }

    private OpenLibraryClient crearCliente(Duration timeout) {
        HttpClient httpClient = HttpClient.create().responseTimeout(timeout);
        WebClient webClient = WebClient.builder()
                .baseUrl(servidor.url("/").toString())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        return new OpenLibraryClient(webClient);
    }

    @Test
    void devuelveEncontradoConLaFormaRealDeOpenLibrary() throws IOException {
        servidor = new MockWebServer();
        servidor.start();
        servidor.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(JSON_1984_REAL));
        OpenLibraryClient cliente = crearCliente(Duration.ofSeconds(10));

        OpenLibraryResult resultado = cliente.buscarPorIsbn("9780451524935");

        assertThat(resultado).isInstanceOf(OpenLibraryResult.Encontrado.class);
        var datos = ((OpenLibraryResult.Encontrado) resultado).datos();
        assertThat(datos.title()).isEqualTo("Nineteen Eighty-Four");
        assertThat(datos.numeroPaginas()).isEqualTo(328);
        // "description" llego como objeto {type, value}: confirma que el deserializador
        // custom extrajo el texto plano, no el objeto ni null.
        assertThat(datos.description()).contains("1984 has come and gone");
        assertThat(datos.coverUrl()).isEqualTo("https://covers.openlibrary.org/b/id/12054527-M.jpg");
    }

    @Test
    void devuelveEncontradoCuandoDescriptionEsUnStringPlano() throws IOException {
        servidor = new MockWebServer();
        servidor.start();
        servidor.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"title": "El Principito", "description": "Un cuento sobre un principito.", "number_of_pages": 96, "covers": []}
                        """));
        OpenLibraryClient cliente = crearCliente(Duration.ofSeconds(10));

        OpenLibraryResult resultado = cliente.buscarPorIsbn("9780156013987");

        assertThat(resultado).isInstanceOf(OpenLibraryResult.Encontrado.class);
        var datos = ((OpenLibraryResult.Encontrado) resultado).datos();
        assertThat(datos.description()).isEqualTo("Un cuento sobre un principito.");
        assertThat(datos.coverUrl()).isNull();
    }

    @Test
    void devuelveNoEncontradoCuandoOpenLibraryResponde404() throws IOException {
        servidor = new MockWebServer();
        servidor.start();
        servidor.enqueue(new MockResponse().setResponseCode(404));
        OpenLibraryClient cliente = crearCliente(Duration.ofSeconds(10));

        OpenLibraryResult resultado = cliente.buscarPorIsbn("0000000000000");

        assertThat(resultado).isInstanceOf(OpenLibraryResult.NoEncontrado.class);
    }

    @Test
    void devuelveServicioNoDisponibleCuandoOpenLibraryResponde500() throws IOException {
        servidor = new MockWebServer();
        servidor.start();
        servidor.enqueue(new MockResponse().setResponseCode(500));
        OpenLibraryClient cliente = crearCliente(Duration.ofSeconds(10));

        OpenLibraryResult resultado = cliente.buscarPorIsbn("9780451524935");

        assertThat(resultado).isInstanceOf(OpenLibraryResult.ServicioNoDisponible.class);
    }

    @Test
    void devuelveServicioNoDisponibleCuandoHayTimeout() throws IOException {
        servidor = new MockWebServer();
        servidor.start();
        servidor.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(JSON_1984_REAL)
                .setBodyDelay(2, TimeUnit.SECONDS));
        // Timeout del cliente deliberadamente mas corto que el delay del servidor mock.
        OpenLibraryClient cliente = crearCliente(Duration.ofMillis(200));

        OpenLibraryResult resultado = cliente.buscarPorIsbn("9780451524935");

        assertThat(resultado).isInstanceOf(OpenLibraryResult.ServicioNoDisponible.class);
    }
}
