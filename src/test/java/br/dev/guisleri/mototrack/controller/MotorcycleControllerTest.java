package br.dev.guisleri.mototrack.controller;

import br.dev.guisleri.mototrack.model.Motorcycle;
import br.dev.guisleri.mototrack.service.MotorcycleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
