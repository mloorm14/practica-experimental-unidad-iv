package ec.edu.uteq.pfcbackend.integration;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
class LibroIntegracionTest extends BaseIntegracionTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void configurarPropiedades(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("jwt.secret", () -> JWT_SECRET);
        registry.add("app.cookie.secure", () -> "false");
    }

    @Test
    void listarLibrosConRolUser_devuelve200() throws Exception {
        Cookie token = login("usuario_prueba", "User123!");

        mockMvc.perform(get("/api/v1/libros")
                        .cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void crearLibroConRolUser_devuelve403() throws Exception {
        Cookie token = login("usuario_prueba", "User123!");

        mockMvc.perform(post("/api/v1/libros")
                        .cookie(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLibroValido("978-987-654321-0")))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearLibroConRolAdmin_devuelve201() throws Exception {
        Cookie token = login("admin", "Admin123!");

        mockMvc.perform(post("/api/v1/libros")
                        .cookie(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLibroValido("978-987-654321-1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isbn").value("978-987-654321-1"));
    }

    @Test
    void obtenerLibroInexistente_devuelve404ConProblemDetail() throws Exception {
        Cookie token = login("usuario_prueba", "User123!");

        mockMvc.perform(get("/api/v1/libros/999999")
                        .cookie(token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void crearLibroConBodyInvalido_devuelve400ConErrors() throws Exception {
        Cookie token = login("admin", "Admin123!");

        String cuerpoInvalido = "{\"titulo\":\"\",\"autor\":\"\",\"isbn\":\"abc\"}";

        mockMvc.perform(post("/api/v1/libros")
                        .cookie(token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void eliminarLibroConRolUser_devuelve403() throws Exception {
        Cookie token = login("usuario_prueba", "User123!");

        mockMvc.perform(delete("/api/v1/libros/1")
                        .cookie(token))
                .andExpect(status().isForbidden());
    }

    @Test
    void respuestaExitosaTraeEnvelopeApiResponse() throws Exception {
        Cookie token = login("admin", "Admin123!");

        mockMvc.perform(get("/api/v1/libros")
                        .cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.current_page").value(0))
                .andExpect(jsonPath("$.meta.total").isNumber());
    }
}
