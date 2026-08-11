package ec.edu.uteq.pfcbackend.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
abstract class BaseIntegracionTest {

    static final String JWT_SECRET =
            "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyAhIiMkJSYnKCkqKywtLi8w";

    @Autowired
    protected MockMvc mockMvc;

    protected Cookie login(String username, String password) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return resultado.getResponse().getCookie("access_token");
    }

    protected static String cuerpoLogin(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    protected static String cuerpoLibroValido(String isbn) {
        return "{\"titulo\":\"Cien anos de soledad\",\"autor\":\"Gabriel Garcia Marquez\","
                + "\"isbn\":\"" + isbn + "\",\"genero\":\"Novela\","
                + "\"descripcion\":\"Novela clasica de la literatura hispanoamericana\","
                + "\"anioPublicacion\":1967,\"editorialId\":1,\"idiomaId\":1,\"estadoId\":1,\"stock\":5}";
    }
}
