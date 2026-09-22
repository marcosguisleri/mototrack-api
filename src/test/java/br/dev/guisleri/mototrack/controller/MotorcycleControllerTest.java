package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.exception.MotorcycleInUseException;
import br.dev.guisleri.mototrack.exception.MotorcycleNotFoundException;
import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.service.MotorcycleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MotorcycleController.class)
class MotorcycleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MotorcycleService motorcycleService;

    @Test
    void shouldCreateMotorcycleWithColor() throws Exception {
        Motorcycle savedMotorcycle = Motorcycle.restore(
                1L,
                "Honda",
                "NX 500",
                "Black",
                2025,
                471
        );
        when(motorcycleService.registerMotorcycle(
                "Honda",
                "NX 500",
                "Black",
                2025,
                471
        )).thenReturn(savedMotorcycle);

        mockMvc.perform(post("/motorcycles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "brand": "Honda",
                                  "model": "NX 500",
                                  "color": "Black",
                                  "year": 2025,
                                  "engineCapacity": 471
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.brand").value("Honda"))
                .andExpect(jsonPath("$.model").value("NX 500"))
                .andExpect(jsonPath("$.color").value("Black"))
                .andExpect(jsonPath("$.year").value(2025))
                .andExpect(jsonPath("$.engineCapacity").value(471));

        verify(motorcycleService).registerMotorcycle(
                "Honda",
                "NX 500",
                "Black",
                2025,
                471
        );
    }

    @Test
    void shouldRejectBlankMotorcycleColor() throws Exception {
        mockMvc.perform(post("/motorcycles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "brand": "Honda",
                                  "model": "NX 500",
                                  "color": " ",
                                  "year": 2025,
                                  "engineCapacity": 471
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(motorcycleService);
    }

    @Test
    void shouldFindAllMotorcycles() throws Exception {
        Motorcycle honda = Motorcycle.restore(
                1L,
                "Honda",
                "NX 500",
                "Black",
                2025,
                471
        );
        Motorcycle yamaha = Motorcycle.restore(
                2L,
                "Yamaha",
                "Tenere 700",
                "Blue",
                2024,
                689
        );
        when(motorcycleService.findAllMotorcycles())
                .thenReturn(List.of(honda, yamaha));

        mockMvc.perform(get("/motorcycles"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].brand").value("Honda"))
                .andExpect(jsonPath("$[0].model").value("NX 500"))
                .andExpect(jsonPath("$[0].color").value("Black"))
                .andExpect(jsonPath("$[0].year").value(2025))
                .andExpect(jsonPath("$[0].engineCapacity").value(471))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].brand").value("Yamaha"));

        verify(motorcycleService).findAllMotorcycles();
    }

    @Test
    void shouldFindMotorcycleById() throws Exception {
        Motorcycle motorcycle = Motorcycle.restore(
                1L,
                "Honda",
                "NX 500",
                "Black",
                2025,
                471
        );
        when(motorcycleService.findMotorcycleById(1L)).thenReturn(motorcycle);

        mockMvc.perform(get("/motorcycles/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.brand").value("Honda"))
                .andExpect(jsonPath("$.model").value("NX 500"))
                .andExpect(jsonPath("$.color").value("Black"))
                .andExpect(jsonPath("$.year").value(2025))
                .andExpect(jsonPath("$.engineCapacity").value(471));

        verify(motorcycleService).findMotorcycleById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenMotorcycleDoesNotExist() throws Exception {
        when(motorcycleService.findMotorcycleById(999L)).thenThrow(
                new MotorcycleNotFoundException(
                        "Motocicleta com id 999 não encontrada"
                )
        );

        mockMvc.perform(get("/motorcycles/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(
                        "Motocicleta com id 999 não encontrada"
                ));

        verify(motorcycleService).findMotorcycleById(999L);
    }

    @Test
    void shouldDeleteMotorcycleById() throws Exception {
        mockMvc.perform(delete("/motorcycles/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(motorcycleService).deleteMotorcycleById(1L);
    }

    @Test
    void shouldReturnConflictWhenDeletingMotorcycleWithTrips() throws Exception {
        String message =
                "A motocicleta não pode ser excluída enquanto possuir viagens associadas.";
        doThrow(new MotorcycleInUseException(message))
                .when(motorcycleService)
                .deleteMotorcycleById(1L);

        mockMvc.perform(delete("/motorcycles/1"))
                .andExpect(status().isConflict())
                .andExpect(content().string(message));

        verify(motorcycleService).deleteMotorcycleById(1L);
    }
}
