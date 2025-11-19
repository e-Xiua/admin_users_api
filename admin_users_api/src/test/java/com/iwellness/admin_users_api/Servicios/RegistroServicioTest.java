package com.iwellness.admin_users_api.Servicios;

import com.iwellness.admin_users_api.Entidades.Proveedor;
import com.iwellness.admin_users_api.Entidades.Usuarios;
import com.iwellness.admin_users_api.Entidades.Rol;
import com.iwellness.admin_users_api.Entidades.Turista;
import com.iwellness.admin_users_api.Repositorios.UsuariosRepositorio;
import com.iwellness.admin_users_api.Repositorios.RolRepositorio;
import com.iwellness.admin_users_api.Repositorios.TuristaRepositorio;
import com.iwellness.admin_users_api.Repositorios.ProveedorRepositorio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests del Servicio de Registro")
class RegistroServicioTest {

    @Mock
    private UsuariosRepositorio usuariosRepositorio;

    @Mock
    private RolRepositorio rolRepositorio;

    @Mock
    private TuristaRepositorio turistaRepositorio;

    @Mock
    private ProveedorRepositorio proveedorRepositorio;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistroServicio registroServicio;

    private Rol rolTurista;
    private Rol rolProveedor;
    private Rol rolAdmin;

    @BeforeEach
    void setUp() {
        rolTurista = new Rol();
        rolTurista.setId(1L);
        rolTurista.setNombre("Turista");

        rolProveedor = new Rol();
        rolProveedor.setId(2L);
        rolProveedor.setNombre("Proveedor");

        rolAdmin = new Rol();
        rolAdmin.setId(3L);
        rolAdmin.setNombre("Admin");
    }

    @Test
    @DisplayName("Debe verificar que correo existe")
    void testVerificarCorreo_CorreoExistente() {
        // Arrange
        String correo = "test@example.com";
        when(usuariosRepositorio.findByCorreo(correo)).thenReturn(Optional.of(new Usuarios()));

        // Act
        boolean resultado = registroServicio.verificarCorreo(correo);

        // Assert
        assertTrue(resultado, "El método debe devolver true si el correo ya está registrado.");
        verify(usuariosRepositorio, times(1)).findByCorreo(correo);
    }

    @Test
    @DisplayName("Debe verificar que correo no existe")
    void testVerificarCorreo_CorreoNoExistente() {
        // Arrange
        String correo = "test@example.com";
        when(usuariosRepositorio.findByCorreo(correo)).thenReturn(Optional.empty());

        // Act
        boolean resultado = registroServicio.verificarCorreo(correo);

        // Assert
        assertFalse(resultado, "El método debe devolver false si el correo no está registrado.");
        verify(usuariosRepositorio, times(1)).findByCorreo(correo);
    }

    @Test
    @DisplayName("Debe rechazar registro cuando correo ya existe")
    void testRegistrarUsuario_CorreoExistente() {
        // Arrange
        Map<String, Object> datos = new HashMap<>();
        datos.put("correo", "existente@example.com");
        datos.put("contraseña", "password123");
        datos.put("nombre", "Usuario Test");
        datos.put("foto", "ruta/foto.jpg");
        
        when(usuariosRepositorio.existsByCorreo("existente@example.com")).thenReturn(true);
        
        // Act
        String resultado = registroServicio.registrarUsuario(datos, "Turista");
        
        // Assert
        assertEquals("Error: El correo ya está registrado", resultado);
        verify(usuariosRepositorio, never()).save(any(Usuarios.class));
    }

    @Test
    @DisplayName("Debe rechazar registro cuando rol no existe")
    void testRegistrarUsuario_RolNoEncontrado() {
        // Arrange
        Map<String, Object> datos = new HashMap<>();
        datos.put("correo", "nuevo@example.com");
        datos.put("contraseña", "password123");
        datos.put("nombre", "Usuario Test");
        datos.put("foto", "ruta/foto.jpg");
    
        when(usuariosRepositorio.existsByCorreo("nuevo@example.com")).thenReturn(false);
        when(rolRepositorio.findByNombre("Turista")).thenReturn(Optional.empty());
    
        // Act
        String resultado = registroServicio.registrarUsuario(datos, "Turista");
    
        // Assert
        assertEquals("Error: No se encontró el rol adecuado", resultado);
        verify(usuariosRepositorio, never()).save(any(Usuarios.class));
    }

    @Test
    @DisplayName("Debe rechazar registro cuando faltan datos obligatorios")
    void testRegistrarUsuario_DatosIncompletos() {
        // Arrange - Sin nombre
        Map<String, Object> datos = new HashMap<>();
        datos.put("correo", "nuevo@example.com");
        datos.put("contraseña", "password123");

        // Act
        String resultado = registroServicio.registrarUsuario(datos, "Turista");

        // Assert
        assertEquals("Error: Faltan datos obligatorios", resultado);
        verify(usuariosRepositorio, never()).save(any(Usuarios.class));
    }

    @Test
    @DisplayName("Debe registrar turista exitosamente")
    void testRegistrarUsuario_TuristaExitoso() {
        // Arrange
        Map<String, Object> datos = new HashMap<>();
        datos.put("correo", "turista@example.com");
        datos.put("contraseña", "password123");
        datos.put("nombre", "Turista Test");
        datos.put("foto", "foto.jpg");
        datos.put("telefono", "123456789");
        datos.put("ciudad", "San José");
        datos.put("pais", "Costa Rica");
        datos.put("genero", "M");

        Usuarios usuarioGuardado = new Usuarios();
        usuarioGuardado.setId(1L);
        usuarioGuardado.setCorreo("turista@example.com");

        when(usuariosRepositorio.existsByCorreo("turista@example.com")).thenReturn(false);
        when(rolRepositorio.findByNombre("Turista")).thenReturn(Optional.of(rolTurista));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(usuariosRepositorio.save(any(Usuarios.class))).thenReturn(usuarioGuardado);
        when(turistaRepositorio.save(any(Turista.class))).thenReturn(new Turista());

        // Act
        String resultado = registroServicio.registrarUsuario(datos, "Turista");

        // Assert
        assertEquals("Registro exitoso", resultado);
        verify(usuariosRepositorio, times(1)).save(any(Usuarios.class));
        verify(turistaRepositorio, times(1)).save(any(Turista.class));
        verify(passwordEncoder, times(1)).encode("password123");
    }

    @Test
    @DisplayName("Debe registrar proveedor exitosamente")
    void testRegistrarUsuario_ProveedorExitoso() {
        // Arrange
        Map<String, Object> datos = new HashMap<>();
        datos.put("correo", "proveedor@example.com");
        datos.put("contraseña", "password123");
        datos.put("nombre", "Proveedor Test");
        datos.put("foto", "foto.jpg");
        datos.put("nombreComercial", "Comercio Test");
        datos.put("direccion", "Dirección Test");

        Usuarios usuarioGuardado = new Usuarios();
        usuarioGuardado.setId(1L);
        usuarioGuardado.setCorreo("proveedor@example.com");

        when(usuariosRepositorio.existsByCorreo("proveedor@example.com")).thenReturn(false);
        when(rolRepositorio.findByNombre("Proveedor")).thenReturn(Optional.of(rolProveedor));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(usuariosRepositorio.save(any(Usuarios.class))).thenReturn(usuarioGuardado);
        when(proveedorRepositorio.save(any(Proveedor.class))).thenReturn(new Proveedor());

        // Act
        String resultado = registroServicio.registrarUsuario(datos, "Proveedor");

        // Assert
        assertEquals("Registro exitoso", resultado);
        verify(usuariosRepositorio, times(2)).save(any(Usuarios.class)); // Se llama 2 veces: una en registrarUsuario, otra en crearProveedor
        verify(proveedorRepositorio, times(1)).save(any(Proveedor.class));
        verify(passwordEncoder, times(1)).encode("password123");
    }

    @Test
    @DisplayName("Debe registrar admin exitosamente")
    void testRegistrarUsuario_AdminExitoso() {
        // Arrange
        Map<String, Object> datos = new HashMap<>();
        datos.put("correo", "admin@example.com");
        datos.put("contraseña", "password123");
        datos.put("nombre", "Admin Test");
        datos.put("foto", "foto.jpg");

        Usuarios usuarioGuardado = new Usuarios();
        usuarioGuardado.setId(1L);
        usuarioGuardado.setCorreo("admin@example.com");

        when(usuariosRepositorio.existsByCorreo("admin@example.com")).thenReturn(false);
        when(rolRepositorio.findByNombre("Admin")).thenReturn(Optional.of(rolAdmin));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(usuariosRepositorio.save(any(Usuarios.class))).thenReturn(usuarioGuardado);

        // Act
        String resultado = registroServicio.registrarUsuario(datos, "Admin");

        // Assert
        assertEquals("Registro exitoso", resultado);
        verify(usuariosRepositorio, times(1)).save(any(Usuarios.class));
        verify(turistaRepositorio, never()).save(any());
        verify(proveedorRepositorio, never()).save(any());
        verify(passwordEncoder, times(1)).encode("password123");
    }

    @Test
    @DisplayName("Debe rechazar registro con correo nulo")
    void testRegistrarUsuario_CorreoNulo() {
        // Arrange
        Map<String, Object> datos = new HashMap<>();
        datos.put("correo", null);
        datos.put("contraseña", "password123");
        datos.put("nombre", "Usuario Test");

        // Act
        String resultado = registroServicio.registrarUsuario(datos, "Turista");

        // Assert
        assertEquals("Error: Faltan datos obligatorios", resultado);
        verify(usuariosRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Debe rechazar registro con contraseña nula")
    void testRegistrarUsuario_ContraseñaNula() {
        // Arrange
        Map<String, Object> datos = new HashMap<>();
        datos.put("correo", "test@example.com");
        datos.put("contraseña", null);
        datos.put("nombre", "Usuario Test");

        // Act
        String resultado = registroServicio.registrarUsuario(datos, "Turista");

        // Assert
        assertEquals("Error: Faltan datos obligatorios", resultado);
        verify(usuariosRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Debe codificar contraseña al registrar")
    void testRegistrarUsuario_DebeCodearContraseña() {
        // Arrange
        Map<String, Object> datos = new HashMap<>();
        datos.put("correo", "test@example.com");
        datos.put("contraseña", "plainPassword");
        datos.put("nombre", "Test User");
        datos.put("foto", "foto.jpg");

        Usuarios usuarioGuardado = new Usuarios();
        usuarioGuardado.setId(1L);

        when(usuariosRepositorio.existsByCorreo(anyString())).thenReturn(false);
        when(rolRepositorio.findByNombre("Turista")).thenReturn(Optional.of(rolTurista));
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(usuariosRepositorio.save(any(Usuarios.class))).thenReturn(usuarioGuardado);
        when(turistaRepositorio.save(any(Turista.class))).thenReturn(new Turista());

        // Act
        registroServicio.registrarUsuario(datos, "Turista");

        // Assert
        verify(passwordEncoder, times(1)).encode("plainPassword");
    }
}
