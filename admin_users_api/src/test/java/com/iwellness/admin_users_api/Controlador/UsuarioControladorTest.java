package com.iwellness.admin_users_api.Controlador;

import com.iwellness.admin_users_api.DTO.EditarProveedorDTO;
import com.iwellness.admin_users_api.DTO.EditarTuristaDTO;
import com.iwellness.admin_users_api.DTO.UsuarioResponseDTO;
import com.iwellness.admin_users_api.Entidades.Proveedor;
import com.iwellness.admin_users_api.Entidades.Rol;
import com.iwellness.admin_users_api.Entidades.Turista;
import com.iwellness.admin_users_api.Entidades.Usuarios;
import com.iwellness.admin_users_api.Servicios.Rabbit.MensajeServiceUsers;
import com.iwellness.admin_users_api.Servicios.UsuariosServicio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.Date;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de UsuarioControlador")
class UsuarioControladorTest {

    @Mock
    private UsuariosServicio usuariosServicio;

    @Mock
    private MensajeServiceUsers mensajeServiceUsers;

    @InjectMocks
    private UsuarioControlador usuarioControlador;

    private Usuarios usuarioAdmin;
    private Usuarios usuarioTurista;
    private Usuarios usuarioProveedor;
    private Rol rolAdmin;
    private Rol rolTurista;
    private Rol rolProveedor;

    @BeforeEach
    void setUp() {
        // Crear roles
        rolAdmin = new Rol();
        rolAdmin.setId(1L);
        rolAdmin.setNombre("Admin");

        rolTurista = new Rol();
        rolTurista.setId(2L);
        rolTurista.setNombre("Turista");

        rolProveedor = new Rol();
        rolProveedor.setId(3L);
        rolProveedor.setNombre("Proveedor");

        // Crear usuario admin
        usuarioAdmin = new Usuarios();
        usuarioAdmin.setId(1L);
        usuarioAdmin.setNombre("Admin User");
        usuarioAdmin.setCorreo("admin@example.com");
        usuarioAdmin.setRol(rolAdmin);

        // Crear usuario turista
        usuarioTurista = new Usuarios();
        usuarioTurista.setId(2L);
        usuarioTurista.setNombre("Tourist User");
        usuarioTurista.setCorreo("turista@example.com");
        usuarioTurista.setRol(rolTurista);
        
        Turista turista = new Turista();
        turista.setId(2L);
        turista.setUsuarios(usuarioTurista);
        turista.setTelefono("12345678");
        turista.setCiudad("San José");
        turista.setPais("Costa Rica");
        usuarioTurista.setTurista(turista);

        // Crear usuario proveedor
        usuarioProveedor = new Usuarios();
        usuarioProveedor.setId(3L);
        usuarioProveedor.setNombre("Provider User");
        usuarioProveedor.setCorreo("proveedor@example.com");
        usuarioProveedor.setRol(rolProveedor);
        
        Proveedor proveedor = new Proveedor();
        proveedor.setId(3L);
        proveedor.setUsuarios(usuarioProveedor);
        proveedor.setNombre_empresa("Empresa Test");
        proveedor.setTelefono("87654321");
        proveedor.setCoordenadaX("10.0");
        proveedor.setCoordenadaY("-84.0");
        usuarioProveedor.setProveedor(proveedor);
    }

    @Test
    @DisplayName("obtenerTodosLosUsuarios - Debería retornar lista de usuarios con éxito")
    void obtenerUsuarios_DeberiaRetornarListaUsuarios() {
        // Arrange
        List<Map<String, Object>> usuarios = new ArrayList<>();
        Map<String, Object> usuario = new HashMap<>();
        usuario.put("id", 1L);
        usuario.put("nombre", "Test User");
        usuario.put("correo", "test@example.com");
        usuarios.add(usuario);
        
        when(usuariosServicio.findAllWithDetails()).thenReturn(usuarios);

        // Act
        ResponseEntity<?> response = usuarioControlador.obtenerTodosLosUsuarios();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(usuarios, response.getBody());
        verify(usuariosServicio).findAllWithDetails();
    }

    @Test
    @DisplayName("obtenerTodosLosUsuarios - Debería manejar error del servicio")
    void obtenerUsuarios_DeberiaRetornarError() {
        // Arrange
        when(usuariosServicio.findAllWithDetails()).thenThrow(new RuntimeException("Error en DB"));

        // Act
        ResponseEntity<?> response = usuarioControlador.obtenerTodosLosUsuarios();

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error al obtener los usuarios"));
    }

    @Test
    @DisplayName("obtenerTodosLosUsuarios - Debería retornar lista vacía si no hay usuarios")
    void obtenerUsuarios_ListaVacia() {
        // Arrange
        when(usuariosServicio.findAllWithDetails()).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<?> response = usuarioControlador.obtenerTodosLosUsuarios();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<?> result = (List<?>) response.getBody();
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("obtenerUsuarioPorId - Admin debería poder ver cualquier usuario")
    void obtenerUsuarioPorId_AdminAccess() {
        // Arrange
        UsuarioControlador spyControlador = spy(usuarioControlador);
        doReturn(usuarioAdmin).when(spyControlador).getUsuarioActual();
        lenient().doReturn(true).when(spyControlador).isAdmin(usuarioAdmin);
        lenient().doReturn(false).when(spyControlador).isOwner(usuarioAdmin, 2L);

        Map<String, Object> usuario = new HashMap<>();
        usuario.put("id", 2L);
        usuario.put("nombre", "Test User");
        usuario.put("correo", "test@example.com");
        when(usuariosServicio.findByIdWithDetails(2L)).thenReturn(usuario);

        // Act
        ResponseEntity<?> response = spyControlador.obtenerUsuarioPorId(2L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(usuario, response.getBody());
    }

    @Test
    @DisplayName("obtenerUsuarioPorId - Usuario debería poder ver su propio perfil")
    void obtenerUsuarioPorId_OwnerAccess() {
        // Arrange
        UsuarioControlador spyControlador = spy(usuarioControlador);
        doReturn(usuarioTurista).when(spyControlador).getUsuarioActual();
        lenient().doReturn(false).when(spyControlador).isAdmin(usuarioTurista);
        lenient().doReturn(true).when(spyControlador).isOwner(usuarioTurista, 2L);

        Map<String, Object> usuario = new HashMap<>();
        usuario.put("id", 2L);
        usuario.put("nombre", "Tourist User");
        when(usuariosServicio.findByIdWithDetails(2L)).thenReturn(usuario);

        // Act
        ResponseEntity<?> response = spyControlador.obtenerUsuarioPorId(2L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("obtenerUsuarioPorId - No autenticado debería retornar 401")
    void obtenerUsuarioPorId_Unauthorized() {
        // Arrange
        UsuarioControlador spyControlador = spy(usuarioControlador);
        doReturn(null).when(spyControlador).getUsuarioActual();

        // Act
        ResponseEntity<?> response = spyControlador.obtenerUsuarioPorId(1L);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Usuario no autenticado"));
    }

    @Test
    @DisplayName("obtenerUsuarioPorId - Sin permisos debería retornar 403")
    void obtenerUsuarioPorId_Forbidden() {
        // Arrange
        UsuarioControlador spyControlador = spy(usuarioControlador);
        doReturn(usuarioTurista).when(spyControlador).getUsuarioActual();
        doReturn(false).when(spyControlador).isAdmin(usuarioTurista);
        doReturn(false).when(spyControlador).isOwner(usuarioTurista, 3L);

        // Act
        ResponseEntity<?> response = spyControlador.obtenerUsuarioPorId(3L);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("No tiene permisos"));
    }

    @Test
    @DisplayName("obtenerUsuarioPorId - Usuario no encontrado debería retornar 404")
    void obtenerUsuarioPorId_NotFound() {
        // Arrange
        UsuarioControlador spyControlador = spy(usuarioControlador);
        doReturn(usuarioAdmin).when(spyControlador).getUsuarioActual();
        lenient().doReturn(true).when(spyControlador).isAdmin(usuarioAdmin);
        when(usuariosServicio.findByIdWithDetails(99L)).thenReturn(null);

        // Act
        ResponseEntity<?> response = spyControlador.obtenerUsuarioPorId(99L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("No se encontró el usuario"));
    }

    @Test
    @DisplayName("obtenerPerfilPublicoPorId - Usuario autenticado puede ver perfil público")
    void obtenerPerfilPublico_Success() {
        // Arrange
        UsuarioControlador spyControlador = spy(usuarioControlador);
        doReturn(usuarioTurista).when(spyControlador).getUsuarioActual();
        when(usuariosServicio.findById(3L)).thenReturn(usuarioProveedor);

        // Act
        ResponseEntity<?> response = spyControlador.obtenerPerfilPublicoPorId(3L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("obtenerPerfilPublicoPorId - Sin autenticación debería retornar 401")
    void obtenerPerfilPublico_Unauthorized() {
        // Arrange
        UsuarioControlador spyControlador = spy(usuarioControlador);
        doReturn(null).when(spyControlador).getUsuarioActual();

        // Act
        ResponseEntity<?> response = spyControlador.obtenerPerfilPublicoPorId(1L);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("editarUsuarioTurista - Admin puede editar turista")
    void editarUsuarioTurista_AdminSuccess() throws Exception {
        // Arrange
        UsuarioControlador spyControlador = spy(usuarioControlador);
        doReturn(usuarioAdmin).when(spyControlador).getUsuarioActual();
        lenient().doReturn(true).when(spyControlador).isAdmin(usuarioAdmin);

        EditarTuristaDTO dto = new EditarTuristaDTO();
        dto.setTelefono("99998888");
        dto.setCiudad("Cartago");

        when(usuariosServicio.actualizarUsuarioTurista(eq(2L), any(EditarTuristaDTO.class)))
            .thenReturn(usuarioTurista);

        // Act
        ResponseEntity<?> response = spyControlador.editarUsuarioTurista(2L, dto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(mensajeServiceUsers).enviarMensajeTurista(anyString());
    }

    @Test
    @DisplayName("editarUsuarioTurista - Sin autenticación debería retornar 401")
    void editarUsuarioTurista_Unauthorized() {
        // Arrange
        UsuarioControlador spyControlador = spy(usuarioControlador);
        doReturn(null).when(spyControlador).getUsuarioActual();

        EditarTuristaDTO dto = new EditarTuristaDTO();

        // Act
        ResponseEntity<?> response = spyControlador.editarUsuarioTurista(2L, dto);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("editarUsuarioProveedor - Admin puede editar proveedor")
    void editarUsuarioProveedor_AdminSuccess() throws Exception {
        // Arrange
        EditarProveedorDTO dto = new EditarProveedorDTO();
        dto.setTelefono("11112222");
        dto.setNombre_empresa("Nueva Empresa");

        // Mock del servicio para que retorne el proveedor actualizado
        when(usuariosServicio.actualizarUsuarioProveedor(eq(3L), any(EditarProveedorDTO.class)))
            .thenReturn(usuarioProveedor);

        // Act
        ResponseEntity<?> response = usuarioControlador.editarUsuarioProveedor(3L, dto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(mensajeServiceUsers).enviarMensajeProveedor(anyString());
    }

    @Test
    @DisplayName("eliminarUsuario - Admin puede eliminar usuario")
    void eliminarUsuario_AdminSuccess() {
        // Arrange
        UsuarioControlador spyControlador = spy(usuarioControlador);
        doReturn(usuarioAdmin).when(spyControlador).getUsuarioActual();
        doReturn(true).when(spyControlador).isAdmin(usuarioAdmin);
        when(usuariosServicio.findById(2L)).thenReturn(usuarioTurista);

        // Act
        ResponseEntity<?> response = spyControlador.eliminarUsuario(2L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(usuariosServicio).deleteById(2L);
    }

    @Test
    @DisplayName("eliminarUsuario - No admin no puede eliminar")
    void eliminarUsuario_Forbidden() {
        // Arrange
        UsuarioControlador spyControlador = spy(usuarioControlador);
        doReturn(usuarioTurista).when(spyControlador).getUsuarioActual();
        doReturn(false).when(spyControlador).isAdmin(usuarioTurista);

        // Act
        ResponseEntity<?> response = spyControlador.eliminarUsuario(3L);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(usuariosServicio, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminarUsuario - Usuario no encontrado retorna 404")
    void eliminarUsuario_NotFound() {
        // Arrange
        UsuarioControlador spyControlador = spy(usuarioControlador);
        doReturn(usuarioAdmin).when(spyControlador).getUsuarioActual();
        doReturn(true).when(spyControlador).isAdmin(usuarioAdmin);
        when(usuariosServicio.findById(99L)).thenReturn(null);

        // Act
        ResponseEntity<?> response = spyControlador.eliminarUsuario(99L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("obtenerProveedores - Debería retornar lista de proveedores")
    void obtenerProveedores_Success() throws Exception {
        // Arrange
        List<Usuarios> proveedores = Arrays.asList(usuarioProveedor);
        when(usuariosServicio.obtenerProveedores()).thenReturn(proveedores);

        // Act
        ResponseEntity<?> response = usuarioControlador.obtenerProveedores();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(mensajeServiceUsers).enviarMensajeProveedor(anyString());
    }

    @Test
    @DisplayName("obtenerTuristas - Admin puede ver turistas")
    void obtenerTuristas_AdminSuccess() throws Exception {
        // Arrange
        UsuarioControlador spyControlador = spy(usuarioControlador);
        doReturn(usuarioAdmin).when(spyControlador).getUsuarioActual();
        doReturn(true).when(spyControlador).isAdmin(usuarioAdmin);
        
        List<Usuarios> turistas = Arrays.asList(usuarioTurista);
        when(usuariosServicio.obtenerTuristas()).thenReturn(turistas);

        // Act
        ResponseEntity<?> response = spyControlador.obtenerTuristas();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(mensajeServiceUsers).enviarMensajeTurista(anyString());
    }

    @Test
    @DisplayName("obtenerTuristas - No admin no puede ver lista")
    void obtenerTuristas_Forbidden() {
        // Arrange
        UsuarioControlador spyControlador = spy(usuarioControlador);
        doReturn(usuarioTurista).when(spyControlador).getUsuarioActual();
        doReturn(false).when(spyControlador).isAdmin(usuarioTurista);

        // Act
        ResponseEntity<?> response = spyControlador.obtenerTuristas();

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(usuariosServicio, never()).obtenerTuristas();
    }
}