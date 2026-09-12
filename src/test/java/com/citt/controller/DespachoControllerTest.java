package com.citt.controller;

import com.citt.persistence.entity.Despacho;
import com.citt.persistence.services.DespachoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DespachoController.class)
class DespachoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DespachoService despachoService;

    @Test
    @DisplayName("POST debe responder 201 con el encabezado Location apuntando al ID generado")
    void crearDespachoDebeDevolverLocationConElIdGenerado() throws Exception {
        Despacho despachoGuardado = new Despacho();
        despachoGuardado.setIdDespacho(7L);
        despachoGuardado.setPatenteCamion("ABCD12");
        when(despachoService.saveDespacho(any(Despacho.class))).thenReturn(despachoGuardado);

        mockMvc.perform(post("/api/v1/despachos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patenteCamion\":\"ABCD12\",\"direccionCompra\":\"Calle Falsa 123\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/despachos/7")))
                .andExpect(jsonPath("$.idDespacho").value(7));
    }
}
