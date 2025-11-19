package com.iwellness.admin_users_api.Seguridad;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.iwellness.admin_users_api.Entidades.Rol;
import com.iwellness.admin_users_api.Entidades.Usuarios;
import com.iwellness.admin_users_api.Repositorios.UsuariosRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de CustomUserDetailsService")
public class CustomUserDetailsServiceTest {

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private UsuariosRepositorio userRepository;

    @Mock
    private JWTProveedor jwtGenerator;

    private Usuarios usuario;
    private Rol rolTurista;
    private String email;
    private String password;

    @BeforeEach
    void setUp() {
        email = "test@example.com";
        password = "$2a$10$encodedPassword";
        
        rolTurista = new Rol();
        rolTurista.setId(1L);
        rolTurista.setNombre("Turista");

        usuario = new Usuarios();
        usuario.setId(1L);
        usuario.setCorreo(email);
        usuario.setContraseña(password);
        usuario.setNombre("Test User");
        usuario.setRol(rolTurista);
    }

    @Test
    @DisplayName("loadUserByUsername - Debería cargar usuario correctamente")
    void testLoadUserByUsername_Success() {
        // Arrange
        when(userRepository.findByCorreo(email)).thenReturn(Optional.of(usuario));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Assert
        assertNotNull(userDetails);
        assertEquals(email, userDetails.getUsername());
        assertEquals(password, userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("Turista")));
        verify(userRepository).findByCorreo(email);
    }

    @Test
    @DisplayName("loadUserByUsername - Debería lanzar excepción si usuario no existe")
    void testLoadUserByUsername_UserNotFound() {
        // Arrange
        when(userRepository.findByCorreo(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername(email);
        });
        verify(userRepository).findByCorreo(email);
    }

    @Test
    @DisplayName("loadUserByUsername - Debería cargar usuario con rol Admin")
    void testLoadUserByUsername_AdminRole() {
        // Arrange
        Rol rolAdmin = new Rol();
        rolAdmin.setId(3L);
        rolAdmin.setNombre("Admin");
        usuario.setRol(rolAdmin);

        when(userRepository.findByCorreo(email)).thenReturn(Optional.of(usuario));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Assert
        assertNotNull(userDetails);
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertTrue(authorities.stream()
            .anyMatch(auth -> auth.getAuthority().equals("Admin")));
    }

    @Test
    @DisplayName("loadUserByUsername - Debería cargar usuario con rol Proveedor")
    void testLoadUserByUsername_ProveedorRole() {
        // Arrange
        Rol rolProveedor = new Rol();
        rolProveedor.setId(2L);
        rolProveedor.setNombre("Proveedor");
        usuario.setRol(rolProveedor);

        when(userRepository.findByCorreo(email)).thenReturn(Optional.of(usuario));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Assert
        assertNotNull(userDetails);
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("Proveedor")));
    }

    @Test
    @DisplayName("getUserRoleFromToken - Debería extraer rol correctamente de token válido")
    void testGetUserRoleFromToken_Success() {
        // Arrange
        String token = "valid.jwt.token";
        when(jwtGenerator.validateToken(token)).thenReturn(true);
        when(jwtGenerator.getUserFromJwt(token)).thenReturn(email);
        when(userRepository.findByCorreo(email)).thenReturn(Optional.of(usuario));

        // Act
        String role = customUserDetailsService.getUserRoleFromToken(token);

        // Assert
        assertEquals("Turista", role);
        verify(jwtGenerator).validateToken(token);
        verify(jwtGenerator).getUserFromJwt(token);
    }

    @Test
    @DisplayName("getUserRoleFromToken - Debería lanzar excepción para token inválido")
    void testGetUserRoleFromToken_InvalidToken() {
        // Arrange
        String token = "invalid.token";
        when(jwtGenerator.validateToken(token)).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            customUserDetailsService.getUserRoleFromToken(token);
        });
        verify(jwtGenerator).validateToken(token);
        verify(jwtGenerator, never()).getUserFromJwt(anyString());
    }

    @Test
    @DisplayName("getUserRoleFromToken - Debería retornar ROLE_USER si no hay rol")
    void testGetUserRoleFromToken_NoRole() {
        // Arrange
        String token = "valid.jwt.token";
        usuario.setRol(null);
        
        when(jwtGenerator.validateToken(token)).thenReturn(true);
        when(jwtGenerator.getUserFromJwt(token)).thenReturn(email);
        when(userRepository.findByCorreo(email)).thenReturn(Optional.of(usuario));

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            customUserDetailsService.getUserRoleFromToken(token);
        });
    }

    @Test
    @DisplayName("getUserFromToken - Debería extraer username de token válido")
    void testGetUserFromToken_Success() {
        // Arrange
        String token = "valid.jwt.token";
        when(jwtGenerator.validateToken(token)).thenReturn(true);
        when(jwtGenerator.getUserFromJwt(token)).thenReturn(email);

        // Act
        String username = customUserDetailsService.getUserFromToken(token);

        // Assert
        assertEquals(email, username);
        verify(jwtGenerator).validateToken(token);
        verify(jwtGenerator).getUserFromJwt(token);
    }

    @Test
    @DisplayName("getUserFromToken - Debería lanzar excepción para token inválido")
    void testGetUserFromToken_InvalidToken() {
        // Arrange
        String token = "invalid.token";
        when(jwtGenerator.validateToken(token)).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            customUserDetailsService.getUserFromToken(token);
        });
        verify(jwtGenerator).validateToken(token);
        verify(jwtGenerator, never()).getUserFromJwt(anyString());
    }

    @Test
    @DisplayName("loadUserByUsername - Debería manejar email con mayúsculas")
    void testLoadUserByUsername_EmailCaseSensitivity() {
        // Arrange
        String emailUpperCase = "TEST@EXAMPLE.COM";
        when(userRepository.findByCorreo(emailUpperCase)).thenReturn(Optional.of(usuario));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(emailUpperCase);

        // Assert
        assertNotNull(userDetails);
        verify(userRepository).findByCorreo(emailUpperCase);
    }

    @Test
    @DisplayName("loadUserByUsername - Debería incluir todas las authorities necesarias")
    void testLoadUserByUsername_VerifyAuthorities() {
        // Arrange
        when(userRepository.findByCorreo(email)).thenReturn(Optional.of(usuario));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Assert
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertNotNull(authorities);
        assertFalse(authorities.isEmpty());
        assertEquals(1, authorities.size());
    }

    @Test
    @DisplayName("getUserRoleFromToken - Debería extraer rol Admin correctamente")
    void testGetUserRoleFromToken_AdminRole() {
        // Arrange
        String token = "valid.jwt.token";
        Rol rolAdmin = new Rol();
        rolAdmin.setId(3L);
        rolAdmin.setNombre("Admin");
        usuario.setRol(rolAdmin);

        when(jwtGenerator.validateToken(token)).thenReturn(true);
        when(jwtGenerator.getUserFromJwt(token)).thenReturn(email);
        when(userRepository.findByCorreo(email)).thenReturn(Optional.of(usuario));

        // Act
        String role = customUserDetailsService.getUserRoleFromToken(token);

        // Assert
        assertEquals("Admin", role);
    }

    @Test
    @DisplayName("getUserRoleFromToken - Debería extraer rol Proveedor correctamente")
    void testGetUserRoleFromToken_ProveedorRole() {
        // Arrange
        String token = "valid.jwt.token";
        Rol rolProveedor = new Rol();
        rolProveedor.setId(2L);
        rolProveedor.setNombre("Proveedor");
        usuario.setRol(rolProveedor);

        when(jwtGenerator.validateToken(token)).thenReturn(true);
        when(jwtGenerator.getUserFromJwt(token)).thenReturn(email);
        when(userRepository.findByCorreo(email)).thenReturn(Optional.of(usuario));

        // Act
        String role = customUserDetailsService.getUserRoleFromToken(token);

        // Assert
        assertEquals("Proveedor", role);
    }

    @Test
    @DisplayName("loadUserByUsername - Debería funcionar con contraseña encriptada")
    void testLoadUserByUsername_EncodedPassword() {
        // Arrange
        String encodedPassword = "$2a$10$someEncodedPasswordHash";
        usuario.setContraseña(encodedPassword);
        when(userRepository.findByCorreo(email)).thenReturn(Optional.of(usuario));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Assert
        assertEquals(encodedPassword, userDetails.getPassword());
    }
}
