package ec.edu.uteq.pfcbackend.service;

import ec.edu.uteq.pfcbackend.dto.AutorRequest;
import ec.edu.uteq.pfcbackend.dto.AutorResponse;
import ec.edu.uteq.pfcbackend.entity.Autor;
import ec.edu.uteq.pfcbackend.exception.BusinessException;
import ec.edu.uteq.pfcbackend.exception.ResourceNotFoundException;
import ec.edu.uteq.pfcbackend.repository.AutorRepository;
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
class AutorServiceImplTest {

    @Mock
    private AutorRepository autorRepository;

    @InjectMocks
    private AutorServiceImpl autorService;

    private Autor autorExistente;
    private AutorRequest requestValido;

    @BeforeEach
    void setUp() {
        autorExistente = Autor.builder()
                .id(1L)
                .nombre("Gabriel García Márquez")
                .nacionalidad("Colombiana")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        requestValido = new AutorRequest("Julio Cortázar", "Argentina");
    }

    @Test
    void listaAutoresPaginados() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Autor> pagina = new PageImpl<>(List.of(autorExistente), pageable, 1);
        when(autorRepository.findAll(pageable)).thenReturn(pagina);

        Page<AutorResponse> resultado = autorService.listar(pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).nombre()).isEqualTo("Gabriel García Márquez");
    }

    @Test
    void obtenerPorIdDevuelveElAutorSiExiste() {
        when(autorRepository.findById(1L)).thenReturn(Optional.of(autorExistente));

        AutorResponse resultado = autorService.obtenerPorId(1L);

        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.nacionalidad()).isEqualTo("Colombiana");
    }

    @Test
    void obtenerPorIdLanzaExcepcionSiNoExiste() {
        when(autorRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> autorService.obtenerPorId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void crearGuardaElAutorSiElNombreNoExiste() {
        when(autorRepository.findByNombreIgnoreCase(requestValido.nombre())).thenReturn(Optional.empty());
        when(autorRepository.save(any(Autor.class))).thenAnswer(invocacion -> {
            Autor a = invocacion.getArgument(0);
            a.setId(2L);
            a.setCreatedAt(LocalDateTime.now());
            a.setUpdatedAt(LocalDateTime.now());
            return a;
        });

        AutorResponse resultado = autorService.crear(requestValido);

        assertThat(resultado.id()).isEqualTo(2L);
        assertThat(resultado.nombre()).isEqualTo("Julio Cortázar");
        verify(autorRepository, times(1)).save(any(Autor.class));
    }

    @Test
    void crearLanzaExcepcionSiElNombreYaExiste() {
        when(autorRepository.findByNombreIgnoreCase(requestValido.nombre()))
                .thenReturn(Optional.of(autorExistente));

        assertThatThrownBy(() -> autorService.crear(requestValido))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(requestValido.nombre());

        verify(autorRepository, never()).save(any(Autor.class));
    }

    @Test
    void actualizarModificaElAutorSiExiste() {
        when(autorRepository.findById(1L)).thenReturn(Optional.of(autorExistente));
        when(autorRepository.findByNombreIgnoreCase(requestValido.nombre())).thenReturn(Optional.empty());
        when(autorRepository.save(any(Autor.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        AutorResponse resultado = autorService.actualizar(1L, requestValido);

        assertThat(resultado.nombre()).isEqualTo("Julio Cortázar");
        assertThat(resultado.nacionalidad()).isEqualTo("Argentina");
    }

    @Test
    void actualizarLanzaExcepcionSiElAutorNoExiste() {
        when(autorRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> autorService.actualizar(999L, requestValido))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(autorRepository, never()).save(any(Autor.class));
    }

    @Test
    void actualizarLanzaExcepcionSiElNuevoNombrePerteneceAOtroAutor() {
        Autor otroAutor = Autor.builder().id(2L).nombre(requestValido.nombre()).build();

        when(autorRepository.findById(1L)).thenReturn(Optional.of(autorExistente));
        when(autorRepository.findByNombreIgnoreCase(requestValido.nombre())).thenReturn(Optional.of(otroAutor));

        assertThatThrownBy(() -> autorService.actualizar(1L, requestValido))
                .isInstanceOf(BusinessException.class);

        verify(autorRepository, never()).save(any(Autor.class));
    }

    @Test
    void eliminarBorraElAutorSiExiste() {
        when(autorRepository.findById(1L)).thenReturn(Optional.of(autorExistente));

        autorService.eliminar(1L);

        verify(autorRepository, times(1)).delete(autorExistente);
    }

    @Test
    void eliminarLanzaExcepcionSiElAutorNoExiste() {
        when(autorRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> autorService.eliminar(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(autorRepository, never()).delete(any(Autor.class));
    }
}
