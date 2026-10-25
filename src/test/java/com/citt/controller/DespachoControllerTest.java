package com.citt.controller;

import com.citt.exceptions.DespachoNotFoundException;
import com.citt.persistence.entity.Despacho;
import com.citt.persistence.services.DespachoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Test
    @DisplayName("GET de la lista debe responder 200 con los despachos del servicio")
    void getAllDespachosDebeDevolver200ConLaLista() throws Exception {
        // Arrange
        Despacho primerDespacho = new Despacho();
        primerDespacho.setIdDespacho(1L);
        primerDespacho.setPatenteCamion("AAAA11");
        Despacho segundoDespacho = new Despacho();
        segundoDespacho.setIdDespacho(2L);
        segundoDespacho.setPatenteCamion("BBBB22");
        when(despachoService.findAllDespachos()).thenReturn(List.of(primerDespacho, segundoDespacho));

        // Act
        var resultado = mockMvc.perform(get("/api/v1/despachos"));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].idDespacho").value(1))
                .andExpect(jsonPath("$[0].patenteCamion").value("AAAA11"))
                .andExpect(jsonPath("$[1].patenteCamion").value("BBBB22"));
    }

    @Test
    @DisplayName("GET por ID existente debe responder 200 con el despacho")
    void obtenerDespachoPorIdDebeDevolver200() throws Exception {
        // Arrange
        Despacho despachoEncontrado = new Despacho();
        despachoEncontrado.setIdDespacho(3L);
        despachoEncontrado.setPatenteCamion("CCCC33");
        despachoEncontrado.setDireccionCompra("Av. Siempre Viva 742");
        when(despachoService.findById(3L)).thenReturn(despachoEncontrado);

        // Act
        var resultado = mockMvc.perform(get("/api/v1/despachos/3"));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.idDespacho").value(3))
                .andExpect(jsonPath("$.patenteCamion").value("CCCC33"))
                .andExpect(jsonPath("$.direccionCompra").value("Av. Siempre Viva 742"));
    }

    @Test
    @DisplayName("GET por ID inexistente debe responder 404")
    void obtenerDespachoPorIdInexistenteDebeDevolver404() throws Exception {
        // Arrange
        when(despachoService.findById(99L))
                .thenThrow(new DespachoNotFoundException("¡No existe despacho con el ID:99"));

        // Act
        var resultado = mockMvc.perform(get("/api/v1/despachos/99"));

        // Assert
        resultado.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.idDespacho").value("ID de despacho no existe"));
    }

    @Test
    @DisplayName("PUT sobre un ID existente debe responder 200 con el despacho actualizado")
    void actualizarDespachoDebeDevolver200() throws Exception {
        // Arrange
        Despacho despachoActualizado = new Despacho();
        despachoActualizado.setIdDespacho(5L);
        despachoActualizado.setPatenteCamion("NUEVA22");
        despachoActualizado.setIntento(3);
        despachoActualizado.setDespachado(true);
        when(despachoService.updateDespacho(eq(5L), any(Despacho.class))).thenReturn(despachoActualizado);

        // Act
        var resultado = mockMvc.perform(put("/api/v1/despachos/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"patenteCamion\":\"NUEVA22\",\"intento\":3,\"despachado\":true}"));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.idDespacho").value(5))
                .andExpect(jsonPath("$.patenteCamion").value("NUEVA22"))
                .andExpect(jsonPath("$.intento").value(3))
                .andExpect(jsonPath("$.despachado").value(true));
    }

    @Test
    @DisplayName("PUT sobre un ID inexistente debe responder 404")
    void actualizarDespachoInexistenteDebeDevolver404() throws Exception {
        // Arrange
        when(despachoService.updateDespacho(eq(99L), any(Despacho.class)))
                .thenThrow(new DespachoNotFoundException("Despacho no encontrado con ID: 99"));

        // Act
        var resultado = mockMvc.perform(put("/api/v1/despachos/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"patenteCamion\":\"ZZZZ99\"}"));

        // Assert
        resultado.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Despacho no encontrado con ID: 99"));
    }

    @Test
    @DisplayName("DELETE sobre un ID existente debe responder 204 sin contenido")
    void eliminarDespachoDebeDevolver204() throws Exception {
        // Arrange
        doNothing().when(despachoService).deleteDespacho(4L);

        // Act
        var resultado = mockMvc.perform(delete("/api/v1/despachos/4"));

        // Assert
        resultado.andExpect(status().isNoContent());
        verify(despachoService).deleteDespacho(4L);
    }

    @Test
    @DisplayName("DELETE sobre un ID inexistente debe responder 404")
    void eliminarDespachoInexistenteDebeDevolver404() throws Exception {
        // Arrange
        doThrow(new DespachoNotFoundException("¡No es posible eliminar! No existe despacho con el ID:99"))
                .when(despachoService).deleteDespacho(99L);

        // Act
        var resultado = mockMvc.perform(delete("/api/v1/despachos/99"));

        // Assert
        resultado.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors.idDespacho").value("ID de despacho no existe"));
    }
}
