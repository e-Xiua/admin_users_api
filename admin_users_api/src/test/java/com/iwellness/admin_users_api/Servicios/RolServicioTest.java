package com.iwellness.admin_users_api.Servicios;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.iwellness.admin_users_api.Entidades.Rol;
import com.iwellness.admin_users_api.Repositorios.RolRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de RolServicio")
public class RolServicioTest {

    @InjectMocks
    private RolServicio rolServicio;

    @Mock
    private RolRepositorio rolRepositorio;

    private Rol rolAdmin;
    private Rol rolTurista;
    private Rol rolProveedor;

    @BeforeEach
    void setUp() {
        rolAdmin = new Rol();
        rolAdmin.setId(1L);
        rolAdmin.setNombre("ADMIN");

        rolTurista = new Rol();
        rolTurista.setId(2L);
        rolTurista.setNombre("TURISTA");

        rolProveedor = new Rol();
        rolProveedor.setId(3L);
        rolProveedor.setNombre("PROVEEDOR");
    }

    @Test
    @DisplayName("findByNombre - Debería encontrar rol existente por nombre")
    void testFindByNombre_Found() {
        // Arrange
        when(rolRepositorio.findByNombre("ADMIN")).thenReturn(Optional.of(rolAdmin));

        // Act
        Optional<Rol> result = rolServicio.findByNombre("ADMIN");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("ADMIN", result.get().getNombre());
        assertEquals(1L, result.get().getId());
        verify(rolRepositorio).findByNombre("ADMIN");
    }

    @Test
    @DisplayName("findByNombre - No debería encontrar rol inexistente")
    void testFindByNombre_NotFound() {
        // Arrange
        when(rolRepositorio.findByNombre("INEXISTENTE")).thenReturn(Optional.empty());

        // Act
        Optional<Rol> result = rolServicio.findByNombre("INEXISTENTE");

        // Assert
        assertFalse(result.isPresent());
        verify(rolRepositorio).findByNombre("INEXISTENTE");
    }

    @Test
    @DisplayName("findByNombre - Debería manejar nombre nulo")
    void testFindByNombre_NullName() {
        // Arrange
        when(rolRepositorio.findByNombre(null)).thenReturn(Optional.empty());

        // Act
        Optional<Rol> result = rolServicio.findByNombre(null);

        // Assert
        assertFalse(result.isPresent());
        verify(rolRepositorio).findByNombre(null);
    }

    @Test
    @DisplayName("findByNombre - Debería ser case-sensitive")
    void testFindByNombre_CaseSensitive() {
        // Arrange
        when(rolRepositorio.findByNombre("admin")).thenReturn(Optional.empty());
        when(rolRepositorio.findByNombre("ADMIN")).thenReturn(Optional.of(rolAdmin));

        // Act
        Optional<Rol> resultLowerCase = rolServicio.findByNombre("admin");
        Optional<Rol> resultUpperCase = rolServicio.findByNombre("ADMIN");

        // Assert
        assertFalse(resultLowerCase.isPresent());
        assertTrue(resultUpperCase.isPresent());
    }

    @Test
    @DisplayName("existsByName - Debería retornar true cuando el rol existe")
    void testExistsByName_True() {
        // Arrange
        when(rolRepositorio.existsByNombre("ADMIN")).thenReturn(true);

        // Act
        boolean result = rolServicio.existsByName("ADMIN");

        // Assert
        assertTrue(result);
        verify(rolRepositorio).existsByNombre("ADMIN");
    }

    @Test
    @DisplayName("existsByName - Debería retornar false cuando el rol no existe")
    void testExistsByName_False() {
        // Arrange
        when(rolRepositorio.existsByNombre("MODERADOR")).thenReturn(false);

        // Act
        boolean result = rolServicio.existsByName("MODERADOR");

        // Assert
        assertFalse(result);
        verify(rolRepositorio).existsByNombre("MODERADOR");
    }

    @Test
    @DisplayName("existsById - Debería retornar true cuando el ID existe")
    void testExistsById_True() {
        // Arrange
        when(rolRepositorio.existsById(1L)).thenReturn(true);

        // Act
        boolean result = rolServicio.existsById(1L);

        // Assert
        assertTrue(result);
        verify(rolRepositorio).existsById(1L);
    }

    @Test
    @DisplayName("existsById - Debería retornar false cuando el ID no existe")
    void testExistsById_False() {
        // Arrange
        when(rolRepositorio.existsById(99L)).thenReturn(false);

        // Act
        boolean result = rolServicio.existsById(99L);

        // Assert
        assertFalse(result);
        verify(rolRepositorio).existsById(99L);
    }

    @Test
    @DisplayName("existsById - Debería manejar ID nulo")
    void testExistsById_NullId() {
        // Arrange
        when(rolRepositorio.existsById(null)).thenReturn(false);

        // Act
        boolean result = rolServicio.existsById(null);

        // Assert
        assertFalse(result);
        verify(rolRepositorio).existsById(null);
    }

    @Test
    @DisplayName("save - Debería guardar nuevo rol correctamente")
    void testSave_NewRol() {
        // Arrange
        Rol nuevoRol = new Rol();
        nuevoRol.setNombre("MODERADOR");
        
        Rol rolGuardado = new Rol();
        rolGuardado.setId(4L);
        rolGuardado.setNombre("MODERADOR");
        
        when(rolRepositorio.save(nuevoRol)).thenReturn(rolGuardado);

        // Act
        Rol result = rolServicio.save(nuevoRol);

        // Assert
        assertNotNull(result);
        assertEquals(4L, result.getId());
        assertEquals("MODERADOR", result.getNombre());
        verify(rolRepositorio).save(nuevoRol);
    }

    @Test
    @DisplayName("save - Debería actualizar rol existente")
    void testSave_UpdateExistingRol() {
        // Arrange
        rolAdmin.setNombre("ADMIN_ACTUALIZADO");
        when(rolRepositorio.save(rolAdmin)).thenReturn(rolAdmin);

        // Act
        Rol result = rolServicio.save(rolAdmin);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("ADMIN_ACTUALIZADO", result.getNombre());
        verify(rolRepositorio).save(rolAdmin);
    }

    @Test
    @DisplayName("save - Debería manejar rol nulo")
    void testSave_NullRol() {
        // Arrange
        when(rolRepositorio.save(null)).thenThrow(IllegalArgumentException.class);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> rolServicio.save(null));
    }

    @Test
    @DisplayName("findAll - Debería retornar todos los roles existentes")
    void testFindAll_MultipleRoles() {
        // Arrange
        List<Rol> roles = Arrays.asList(rolAdmin, rolTurista, rolProveedor);
        when(rolRepositorio.findAll()).thenReturn(roles);

        // Act
        List<Rol> result = rolServicio.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("ADMIN", result.get(0).getNombre());
        assertEquals("TURISTA", result.get(1).getNombre());
        assertEquals("PROVEEDOR", result.get(2).getNombre());
        verify(rolRepositorio).findAll();
    }

    @Test
    @DisplayName("findAll - Debería retornar lista vacía cuando no hay roles")
    void testFindAll_EmptyList() {
        // Arrange
        when(rolRepositorio.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<Rol> result = rolServicio.findAll();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(rolRepositorio).findAll();
    }

    @Test
    @DisplayName("findAll - Debería retornar un solo rol si solo existe uno")
    void testFindAll_SingleRol() {
        // Arrange
        List<Rol> roles = Collections.singletonList(rolAdmin);
        when(rolRepositorio.findAll()).thenReturn(roles);

        // Act
        List<Rol> result = rolServicio.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ADMIN", result.get(0).getNombre());
        verify(rolRepositorio).findAll();
    }
}
