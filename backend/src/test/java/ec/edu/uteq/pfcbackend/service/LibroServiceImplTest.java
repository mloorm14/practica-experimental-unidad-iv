package ec.edu.uteq.pfcbackend.service;

import ec.edu.uteq.pfcbackend.client.OpenLibraryClient;
import ec.edu.uteq.pfcbackend.client.OpenLibraryResult;
import ec.edu.uteq.pfcbackend.dto.LibroEnriquecidoResponse;
import ec.edu.uteq.pfcbackend.dto.LibroRequest;
import ec.edu.uteq.pfcbackend.dto.LibroResponse;
import ec.edu.uteq.pfcbackend.dto.OpenLibraryResponse;
import ec.edu.uteq.pfcbackend.entity.Editorial;
import ec.edu.uteq.pfcbackend.entity.EstadoLibro;
import ec.edu.uteq.pfcbackend.entity.Idioma;
import ec.edu.uteq.pfcbackend.entity.Libro;
import ec.edu.uteq.pfcbackend.exception.BusinessException;
import ec.edu.uteq.pfcbackend.exception.ResourceNotFoundException;
import ec.edu.uteq.pfcbackend.repository.EditorialRepository;
import ec.edu.uteq.pfcbackend.repository.EstadoLibroRepository;
import ec.edu.uteq.pfcbackend.repository.IdiomaRepository;
import ec.edu.uteq.pfcbackend.repository.LibroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibroServiceImplTest {

    @Mock
    private LibroRepository libroRepository;

    @Mock
    private EditorialRepository editorialRepository;

    @Mock
    private IdiomaRepository idiomaRepository;

    @Mock
    private EstadoLibroRepository estadoLibroRepository;

    @Mock
    private OpenLibraryClient openLibraryClient;

    @InjectMocks
    private LibroServiceImpl libroService;

    private Libro libroExistente;
    private LibroRequest requestValido;
    private Editorial editorial;
    private Idioma idioma;
    private EstadoLibro estado;

    @BeforeEach
    void setUp() {
        editorial = Editorial.builder().id(1L).nombre("Sudamericana").build();
        idioma = Idioma.builder().id(1L).nombre("Español").build();
        estado = EstadoLibro.builder().id(1L).nombre("Disponible").build();

        libroExistente = Libro.builder()
                .id(1L)
                .titulo("Cien años de soledad")
                .descripcion("Novela")
                .isbn("978-0307474728")
                .genero("Realismo mágico")
                .autor("Gabriel García Márquez")
                .anioPublicacion(1967)
                .editorial(editorial)
                .idioma(idioma)
                .estado(estado)
                .stock(10)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        requestValido = new LibroRequest(
                "Rayuela", "Novela experimental", "978-8437604572",
                "Vanguardia", "Julio Cortázar", 1963,
                editorial.getId(), idioma.getId(), estado.getId(), 5
        );
    }

    @Test
    void listaLibrosPaginados() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Libro> pagina = new PageImpl<>(List.of(libroExistente), pageable, 1);
        when(libroRepository.findAll(pageable)).thenReturn(pagina);

        Page<LibroResponse> resultado = libroService.listar(null, pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).titulo()).isEqualTo("Cien años de soledad");
    }

    @Test
    void obtenerPorIdDevuelveElLibroSiExiste() {
        when(libroRepository.findById(1L)).thenReturn(Optional.of(libroExistente));

        LibroResponse resultado = libroService.obtenerPorId(1L);

        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.titulo()).isEqualTo("Cien años de soledad");
    }

    @Test
    void obtenerPorIdLanzaExcepcionSiNoExiste() {
        when(libroRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> libroService.obtenerPorId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void obtenerEnriquecidoCombinaLibroYDatosDeOpenLibrarySiSeEncuentra() {
        OpenLibraryResponse datos = new OpenLibraryResponse(
                "Cien años de soledad", "Descripcion de prueba", 471, List.of(123L));

        when(libroRepository.findById(1L)).thenReturn(Optional.of(libroExistente));
        when(openLibraryClient.buscarPorIsbn(libroExistente.getIsbn()))
                .thenReturn(new OpenLibraryResult.Encontrado(datos));

        LibroEnriquecidoResponse resultado = libroService.obtenerEnriquecido(1L);

        assertThat(resultado.libro().id()).isEqualTo(1L);
        assertThat(resultado.tituloOpenLibrary()).isEqualTo("Cien años de soledad");
        assertThat(resultado.numeroPaginas()).isEqualTo(471);
        assertThat(resultado.coverUrl()).isEqualTo("https://covers.openlibrary.org/b/id/123-M.jpg");
    }

    @Test
    void obtenerEnriquecidoDevuelveSoloElLibroSiOpenLibraryNoEncuentraElIsbn() {
        when(libroRepository.findById(1L)).thenReturn(Optional.of(libroExistente));
        when(openLibraryClient.buscarPorIsbn(libroExistente.getIsbn()))
                .thenReturn(new OpenLibraryResult.NoEncontrado());

        LibroEnriquecidoResponse resultado = libroService.obtenerEnriquecido(1L);

        assertThat(resultado.libro().id()).isEqualTo(1L);
        assertThat(resultado.tituloOpenLibrary()).isNull();
        assertThat(resultado.coverUrl()).isNull();
    }

    @Test
    void obtenerEnriquecidoDevuelveSoloElLibroSiOpenLibraryNoResponde() {
        when(libroRepository.findById(1L)).thenReturn(Optional.of(libroExistente));
        when(openLibraryClient.buscarPorIsbn(libroExistente.getIsbn()))
                .thenReturn(new OpenLibraryResult.ServicioNoDisponible());

        LibroEnriquecidoResponse resultado = libroService.obtenerEnriquecido(1L);

        assertThat(resultado.libro().id()).isEqualTo(1L);
        assertThat(resultado.descripcionOpenLibrary()).isNull();
    }

    @Test
    void obtenerEnriquecidoLanzaExcepcionSiElLibroNoExiste() {
        when(libroRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> libroService.obtenerEnriquecido(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crearGuardaElLibroSiElIsbnNoExiste() {
        when(libroRepository.findByIsbnIgnoreCase(requestValido.isbn())).thenReturn(Optional.empty());
        when(editorialRepository.findById(editorial.getId())).thenReturn(Optional.of(editorial));
        when(idiomaRepository.findById(idioma.getId())).thenReturn(Optional.of(idioma));
        when(estadoLibroRepository.findById(estado.getId())).thenReturn(Optional.of(estado));
        when(libroRepository.save(any(Libro.class))).thenAnswer(invocacion -> {
            Libro l = invocacion.getArgument(0);
            l.setId(2L);
            l.setCreatedAt(LocalDateTime.now());
            l.setUpdatedAt(LocalDateTime.now());
            return l;
        });

        LibroResponse resultado = libroService.crear(requestValido);

        assertThat(resultado.id()).isEqualTo(2L);
        assertThat(resultado.titulo()).isEqualTo("Rayuela");
        verify(libroRepository, times(1)).save(any(Libro.class));
    }

    @Test
    void crearLanzaExcepcionSiElIsbnYaExiste() {
        when(libroRepository.findByIsbnIgnoreCase(requestValido.isbn()))
                .thenReturn(Optional.of(libroExistente));

        assertThatThrownBy(() -> libroService.crear(requestValido))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(requestValido.isbn());

        verify(libroRepository, never()).save(any(Libro.class));
    }

    @Test
    void actualizarModificaElLibroSiExiste() {
        when(libroRepository.findById(1L)).thenReturn(Optional.of(libroExistente));
        when(libroRepository.findByIsbnIgnoreCase(requestValido.isbn())).thenReturn(Optional.empty());
        when(editorialRepository.findById(editorial.getId())).thenReturn(Optional.of(editorial));
        when(idiomaRepository.findById(idioma.getId())).thenReturn(Optional.of(idioma));
        when(estadoLibroRepository.findById(estado.getId())).thenReturn(Optional.of(estado));
        when(libroRepository.save(any(Libro.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        LibroResponse resultado = libroService.actualizar(1L, requestValido);

        assertThat(resultado.titulo()).isEqualTo("Rayuela");
        assertThat(resultado.autor()).isEqualTo("Julio Cortázar");
    }

    @Test
    void actualizarLanzaExcepcionSiElLibroNoExiste() {
        when(libroRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> libroService.actualizar(999L, requestValido))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(libroRepository, never()).save(any(Libro.class));
    }

    @Test
    void actualizarLanzaExcepcionSiElNuevoIsbnPerteneceAOtroLibro() {
        Libro otroLibro = Libro.builder().id(2L).isbn(requestValido.isbn()).build();

        when(libroRepository.findById(1L)).thenReturn(Optional.of(libroExistente));
        when(libroRepository.findByIsbnIgnoreCase(requestValido.isbn())).thenReturn(Optional.of(otroLibro));

        assertThatThrownBy(() -> libroService.actualizar(1L, requestValido))
                .isInstanceOf(BusinessException.class);

        verify(libroRepository, never()).save(any(Libro.class));
    }

    @Test
    void eliminarBorraElLibroSiExiste() {
        when(libroRepository.findById(1L)).thenReturn(Optional.of(libroExistente));

        libroService.eliminar(1L);

        verify(libroRepository, times(1)).delete(libroExistente);
    }

    @Test
    void eliminarLanzaExcepcionSiElLibroNoExiste() {
        when(libroRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> libroService.eliminar(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(libroRepository, never()).delete(any(Libro.class));
    }
}