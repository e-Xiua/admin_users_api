package com.iwellness.admin_users_api.Servicios;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.iwellness.admin_users_api.Clientes.PreferenciaFeignClient;
import com.iwellness.admin_users_api.Clientes.ServicioFeignClient;
import com.iwellness.admin_users_api.DTO.EditarTuristaDTO;
import com.iwellness.admin_users_api.Entidades.Rol;
import com.iwellness.admin_users_api.Entidades.Turista;
import com.iwellness.admin_users_api.Entidades.Usuarios;
import com.iwellness.admin_users_api.Repositorios.TuristaRepositorio;
import com.iwellness.admin_users_api.Repositorios.UsuariosRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests del Servicio de Usuarios")
public class UsuariosServicioTest {

    @InjectMocks
    private UsuariosServicio usuariosServicio;

    @Mock
    private UsuariosRepositorio usuarioRepositorio;

    @Mock
    private TuristaRepositorio turistaRepositorio;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ServicioFeignClient servicioFeignClient;

    @Mock
    private PreferenciaFeignClient preferenciaFeignClient;

    private Usuarios usuarioEjemplo;
    private Rol rolTurista;

    @BeforeEach
    void setUp() {
        rolTurista = new Rol();
        rolTurista.setId(1L);
        rolTurista.setNombre("Turista");

        usuarioEjemplo = new Usuarios();
        usuarioEjemplo.setId(1L);
        usuarioEjemplo.setNombre("Usuario Test");
        usuarioEjemplo.setCorreo("test@example.com");
        usuarioEjemplo.setContraseña("password123");
        usuarioEjemplo.setRol(rolTurista);
        usuarioEjemplo.setFoto("foto.jpg");
    }

    @Test
    @DisplayName("Debe guardar usuario y codificar contraseña nueva")
    void testSave_NewPassword_ShouldEncode() {
        // Arrange
        Usuarios usuario = new Usuarios();
        usuario.setContraseña("plaintext");
        
        when(passwordEncoder.encode("plaintext")).thenReturn("encodedPassword");
        when(usuarioRepositorio.saveAndFlush(usuario)).thenReturn(usuario);

        // Act
        Usuarios result = usuariosServicio.save(usuario);

        // Assert
        assertEquals("encodedPassword", result.getContraseña());
        verify(passwordEncoder).encode("plaintext");
        verify(usuarioRepositorio).saveAndFlush(usuario);
    }

    @Test
    @DisplayName("Debe guardar usuario con contraseña ya encriptada sin re-encriptar")
    void testSave_AlreadyEncodedPassword_ShouldNotEncode() {
        // Arrange
        Usuarios usuario = new Usuarios();
        // Usar una contraseña en formato Base64 largo que coincida con isEncoded()
        String encodedPassword = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwQUJDREVGR0hJSktMTU5PUFFSUw=="; 
        usuario.setContraseña(encodedPassword);
        
        Usuarios usuarioGuardado = new Usuarios();
        usuarioGuardado.setContraseña(encodedPassword);
        when(usuarioRepositorio.saveAndFlush(any(Usuarios.class))).thenReturn(usuarioGuardado);

        // Act
        Usuarios result = usuariosServicio.save(usuario);

        // Assert
        assertNotNull(result);
        assertEquals(encodedPassword, result.getContraseña());
        verify(passwordEncoder, never()).encode(anyString());
        verify(usuarioRepositorio).saveAndFlush(any(Usuarios.class));
    }

    @Test
    @DisplayName("Debe encontrar usuario por ID cuando existe")
    void testFindById_UserExists() {
        // Arrange
        when(usuarioRepositorio.findById(1L)).thenReturn(Optional.of(usuarioEjemplo));

        // Act
        Usuarios result = usuariosServicio.findById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("test@example.com", result.getCorreo());
        verify(usuarioRepositorio, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Debe retornar null cuando usuario no existe")
    void testFindById_UserDoesNotExist() {
        // Arrange
        when(usuarioRepositorio.findById(999L)).thenReturn(Optional.empty());

        // Act
        Usuarios result = usuariosServicio.findById(999L);

        // Assert
        assertNull(result);
        verify(usuarioRepositorio, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Debe verificar si existe usuario por correo")
    void testExistsByCorreo_CorreoExiste() {
        // Arrange
        when(usuarioRepositorio.existsByCorreo("test@example.com")).thenReturn(true);

        // Act
        boolean exists = usuariosServicio.existsByCorreo("test@example.com");

        // Assert
        assertTrue(exists);
        verify(usuarioRepositorio, times(1)).existsByCorreo("test@example.com");
    }

    @Test
    @DisplayName("Debe retornar false cuando correo no existe")
    void testExistsByCorreo_CorreoNoExiste() {
        // Arrange
        when(usuarioRepositorio.existsByCorreo("noexiste@example.com")).thenReturn(false);

        // Act
        boolean exists = usuariosServicio.existsByCorreo("noexiste@example.com");

        // Assert
        assertFalse(exists);
        verify(usuarioRepositorio, times(1)).existsByCorreo("noexiste@example.com");
    }

    @Test
    @DisplayName("Debe encontrar usuario por correo cuando existe")
    void testFindByCorreo_UsuarioExiste() {
        // Arrange
        when(usuarioRepositorio.findByCorreo("test@example.com")).thenReturn(Optional.of(usuarioEjemplo));

        // Act
        Optional<Usuarios> result = usuariosServicio.findByCorreo("test@example.com");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getCorreo());
        verify(usuarioRepositorio, times(1)).findByCorreo("test@example.com");
    }

    @Test
    @DisplayName("Debe retornar empty cuando correo no existe")
    void testFindByCorreo_UsuarioNoExiste() {
        // Arrange
        when(usuarioRepositorio.findByCorreo("noexiste@example.com")).thenReturn(Optional.empty());

        // Act
        Optional<Usuarios> result = usuariosServicio.findByCorreo("noexiste@example.com");

        // Assert
        assertFalse(result.isPresent());
        verify(usuarioRepositorio, times(1)).findByCorreo("noexiste@example.com");
    }

    @Test
    @DisplayName("Debe retornar todos los usuarios")
    void testFindAll_DebeRetornarTodosLosUsuarios() {
        // Arrange
        List<Usuarios> usuarios = Arrays.asList(usuarioEjemplo, new Usuarios());
        when(usuarioRepositorio.findAll()).thenReturn(usuarios);

        // Act
        List<Usuarios> result = usuariosServicio.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(usuarioRepositorio, times(1)).findAll();
    }

    @Test
    @DisplayName("Debe eliminar usuario turista por ID")
    void testDeleteById_Turista() throws Exception {
        // Arrange
        Long usuarioId = 1L;
        Usuarios usuarioTurista = new Usuarios();
        usuarioTurista.setId(usuarioId);
        usuarioTurista.setCorreo("turista@example.com");
        Rol rolTurista = new Rol();
        rolTurista.setId(1L);
        rolTurista.setNombre("Turista");
        usuarioTurista.setRol(rolTurista);
        
        when(usuarioRepositorio.findById(usuarioId)).thenReturn(Optional.of(usuarioTurista));
        doNothing().when(preferenciaFeignClient).eliminarPreferenciasPorTurista(usuarioId);
        doNothing().when(usuarioRepositorio).deleteById(usuarioId);

        // Act
        usuariosServicio.deleteById(usuarioId);

        // Assert
        verify(preferenciaFeignClient, times(1)).eliminarPreferenciasPorTurista(usuarioId);
        verify(usuarioRepositorio, times(1)).deleteById(usuarioId);
    }

    @Test
    @DisplayName("Debe eliminar usuario proveedor por ID")
    void testDeleteById_Proveedor() throws Exception {
        // Arrange
        Long usuarioId = 2L;
        Usuarios usuarioProveedor = new Usuarios();
        usuarioProveedor.setId(usuarioId);
        usuarioProveedor.setCorreo("proveedor@example.com");
        Rol rolProveedor = new Rol();
        rolProveedor.setId(2L);
        rolProveedor.setNombre("Proveedor");
        usuarioProveedor.setRol(rolProveedor);
        
        when(usuarioRepositorio.findById(usuarioId)).thenReturn(Optional.of(usuarioProveedor));
        doNothing().when(servicioFeignClient).eliminarServiciosPorProveedor(usuarioId);
        doNothing().when(usuarioRepositorio).deleteById(usuarioId);

        // Act
        usuariosServicio.deleteById(usuarioId);

        // Assert
        verify(servicioFeignClient, times(1)).eliminarServiciosPorProveedor(usuarioId);
        verify(usuarioRepositorio, times(1)).deleteById(usuarioId);
    }

    @Test
    @DisplayName("Debe actualizar usuario turista exitosamente")
    void testActualizarUsuarioTurista_Exitoso() {
        // Arrange
        Turista turista = new Turista();
        turista.setId(1L);
        turista.setTelefono("123456789");
        turista.setCiudad("San José");
        turista.setPais("Costa Rica");
        
        usuarioEjemplo.setTurista(turista);
        
        EditarTuristaDTO dto = new EditarTuristaDTO();
        dto.setNombre("Nombre Actualizado");
        dto.setFoto("nueva_foto.jpg");
        dto.setTelefono("987654321");
        dto.setCiudad("Heredia");
        dto.setPais("Costa Rica");
        dto.setGenero("M");
        dto.setFechaNacimiento(new Date());
        dto.setEstadoCivil("Soltero");

        when(usuarioRepositorio.findById(1L)).thenReturn(Optional.of(usuarioEjemplo));
        when(turistaRepositorio.save(any(Turista.class))).thenReturn(turista);
        when(usuarioRepositorio.save(any(Usuarios.class))).thenReturn(usuarioEjemplo);

        // Act
        Usuarios result = usuariosServicio.actualizarUsuarioTurista(1L, dto);

        // Assert
        assertNotNull(result);
        assertEquals("Nombre Actualizado", result.getNombre());
        assertEquals("nueva_foto.jpg", result.getFoto());
        verify(usuarioRepositorio, times(1)).findById(1L);
        verify(turistaRepositorio, times(1)).save(any(Turista.class));
        verify(usuarioRepositorio, times(1)).save(any(Usuarios.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar usuario inexistente")
    void testActualizarUsuarioTurista_UsuarioNoExiste() {
        // Arrange
        EditarTuristaDTO dto = new EditarTuristaDTO();
        when(usuarioRepositorio.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            usuariosServicio.actualizarUsuarioTurista(999L, dto);
        });
        verify(usuarioRepositorio, times(1)).findById(999L);
        verify(turistaRepositorio, never()).save(any());
        verify(usuarioRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar usuario sin turista asociado")
    void testActualizarUsuarioTurista_SinTuristaAsociado() {
        // Arrange
        usuarioEjemplo.setTurista(null);
        EditarTuristaDTO dto = new EditarTuristaDTO();
        when(usuarioRepositorio.findById(1L)).thenReturn(Optional.of(usuarioEjemplo));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            usuariosServicio.actualizarUsuarioTurista(1L, dto);
        });
        verify(usuarioRepositorio, times(1)).findById(1L);
        verify(turistaRepositorio, never()).save(any());
    }
}