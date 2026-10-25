package com.citt.persistence.services;

import com.citt.exceptions.DespachoNotFoundException;
import com.citt.persistence.entity.Despacho;
import com.citt.persistence.repository.DespachoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DespachoServiceTest {

    @Mock
    private DespachoRepository despachoRepository;

    @InjectMocks
    private DespachoServiceImpl despachoService;

    @Test
    @DisplayName("findAllDespachos debe devolver la lista entregada por el repositorio")
    void findAllDespachosDebeDevolverLaListaDelRepositorio() {
        // Arrange
        Despacho primerDespacho = new Despacho();
        primerDespacho.setIdDespacho(1L);
        primerDespacho.setPatenteCamion("AAAA11");
        Despacho segundoDespacho = new Despacho();
        segundoDespacho.setIdDespacho(2L);
        segundoDespacho.setPatenteCamion("BBBB22");
        when(despachoRepository.findAll()).thenReturn(List.of(primerDespacho, segundoDespacho));

        // Act
        List<Despacho> despachosEncontrados = despachoService.findAllDespachos();

        // Assert
        assertEquals(2, despachosEncontrados.size());
        assertEquals("AAAA11", despachosEncontrados.get(0).getPatenteCamion());
        assertEquals("BBBB22", despachosEncontrados.get(1).getPatenteCamion());
        verify(despachoRepository).findAll();
    }

    @Test
    @DisplayName("saveDespacho debe persistir el despacho y devolver el resultado del repositorio")
    void saveDespachoDebePersistirYDevolverElDespacho() {
        // Arrange
        Despacho despachoNuevo = new Despacho();
        despachoNuevo.setPatenteCamion("CCCC33");
        despachoNuevo.setDireccionCompra("Av. Siempre Viva 742");
        Despacho despachoPersistido = new Despacho();
        despachoPersistido.setIdDespacho(10L);
        despachoPersistido.setPatenteCamion("CCCC33");
        despachoPersistido.setDireccionCompra("Av. Siempre Viva 742");
        when(despachoRepository.save(despachoNuevo)).thenReturn(despachoPersistido);

        // Act
        Despacho despachoGuardado = despachoService.saveDespacho(despachoNuevo);

        // Assert
        assertSame(despachoPersistido, despachoGuardado);
        assertEquals(10L, despachoGuardado.getIdDespacho());
        verify(despachoRepository).save(despachoNuevo);
    }

    @Test
    @DisplayName("updateDespacho con ID existente debe actualizar todos los campos y guardar")
    void updateDespachoConIdExistenteDebeActualizarTodosLosCampos() throws DespachoNotFoundException {
        // Arrange
        Despacho despachoExistente = new Despacho();
        despachoExistente.setIdDespacho(5L);
        despachoExistente.setFechaDespacho(LocalDate.of(2024, 1, 1));
        despachoExistente.setPatenteCamion("VIEJA11");
        despachoExistente.setIntento(1);
        despachoExistente.setIdCompra(100L);
        despachoExistente.setDireccionCompra("Direccion antigua 1");
        despachoExistente.setValorCompra(5000L);
        despachoExistente.setDespachado(false);
        Despacho despachoConCambios = new Despacho();
        despachoConCambios.setFechaDespacho(LocalDate.of(2025, 6, 15));
        despachoConCambios.setPatenteCamion("NUEVA22");
        despachoConCambios.setIntento(3);
        despachoConCambios.setIdCompra(200L);
        despachoConCambios.setDireccionCompra("Direccion nueva 2");
        despachoConCambios.setValorCompra(9900L);
        despachoConCambios.setDespachado(true);
        when(despachoRepository.findById(5L)).thenReturn(Optional.of(despachoExistente));
        when(despachoRepository.save(any(Despacho.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // Act
        Despacho despachoActualizado = despachoService.updateDespacho(5L, despachoConCambios);

        // Assert
        assertEquals(5L, despachoActualizado.getIdDespacho());
        assertEquals(LocalDate.of(2025, 6, 15), despachoActualizado.getFechaDespacho());
        assertEquals("NUEVA22", despachoActualizado.getPatenteCamion());
        assertEquals(3, despachoActualizado.getIntento());
        assertEquals(200L, despachoActualizado.getIdCompra());
        assertEquals("Direccion nueva 2", despachoActualizado.getDireccionCompra());
        assertEquals(9900L, despachoActualizado.getValorCompra());
        assertTrue(despachoActualizado.isDespachado());
        verify(despachoRepository).save(despachoExistente);
    }

    @Test
    @DisplayName("updateDespacho con ID inexistente debe lanzar DespachoNotFoundException y no guardar")
    void updateDespachoConIdInexistenteDebeLanzarExcepcion() {
        // Arrange
        Despacho despachoConCambios = new Despacho();
        despachoConCambios.setPatenteCamion("ZZZZ99");
        when(despachoRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        Executable actualizacion = () -> despachoService.updateDespacho(99L, despachoConCambios);

        // Assert
        DespachoNotFoundException excepcion = assertThrows(DespachoNotFoundException.class, actualizacion);
        assertEquals("Despacho no encontrado con ID: 99", excepcion.getMessage());
        verify(despachoRepository, never()).save(any(Despacho.class));
    }

    @Test
    @DisplayName("deleteDespacho con ID existente debe llamar a deleteById del repositorio")
    void deleteDespachoConIdExistenteDebeLlamarDeleteById() throws DespachoNotFoundException {
        // Arrange
        Despacho despachoExistente = new Despacho();
        despachoExistente.setIdDespacho(4L);
        when(despachoRepository.findById(4L)).thenReturn(Optional.of(despachoExistente));

        // Act
        despachoService.deleteDespacho(4L);

        // Assert
        verify(despachoRepository).findById(4L);
        verify(despachoRepository).deleteById(4L);
    }

    @Test
    @DisplayName("deleteDespacho con ID inexistente debe lanzar DespachoNotFoundException y no eliminar")
    void deleteDespachoConIdInexistenteDebeLanzarExcepcion() {
        // Arrange
        when(despachoRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        Executable eliminacion = () -> despachoService.deleteDespacho(99L);

        // Assert
        DespachoNotFoundException excepcion = assertThrows(DespachoNotFoundException.class, eliminacion);
        assertTrue(excepcion.getMessage().contains("99"));
        verify(despachoRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("findById con ID existente debe devolver el despacho encontrado")
    void findByIdConIdExistenteDebeDevolverElDespacho() throws DespachoNotFoundException {
        // Arrange
        Despacho despachoExistente = new Despacho();
        despachoExistente.setIdDespacho(8L);
        despachoExistente.setPatenteCamion("DDDD44");
        when(despachoRepository.findById(8L)).thenReturn(Optional.of(despachoExistente));

        // Act
        Despacho despachoEncontrado = despachoService.findById(8L);

        // Assert
        assertSame(despachoExistente, despachoEncontrado);
        assertEquals("DDDD44", despachoEncontrado.getPatenteCamion());
        verify(despachoRepository).findById(8L);
    }

    @Test
    @DisplayName("findById con ID inexistente debe lanzar DespachoNotFoundException")
    void findByIdConIdInexistenteDebeLanzarExcepcion() {
        // Arrange
        when(despachoRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        Executable busqueda = () -> despachoService.findById(99L);

        // Assert
        DespachoNotFoundException excepcion = assertThrows(DespachoNotFoundException.class, busqueda);
        assertTrue(excepcion.getMessage().contains("99"));
        verify(despachoRepository).findById(99L);
    }
}
