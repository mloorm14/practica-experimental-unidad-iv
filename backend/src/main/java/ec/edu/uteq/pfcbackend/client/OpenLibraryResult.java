package ec.edu.uteq.pfcbackend.client;

import ec.edu.uteq.pfcbackend.dto.OpenLibraryResponse;

// Distingue explicitamente los 3 desenlaces posibles de una consulta a Open Library,
// para que el llamador pueda decidir como reaccionar a cada uno si lo necesita
// (hoy el controller trata NoEncontrado y ServicioNoDisponible igual: omite los
// datos externos sin fallar, pero la distincion queda disponible).
public sealed interface OpenLibraryResult {

    record Encontrado(OpenLibraryResponse datos) implements OpenLibraryResult {
    }

    record NoEncontrado() implements OpenLibraryResult {
    }

    record ServicioNoDisponible() implements OpenLibraryResult {
    }
}
