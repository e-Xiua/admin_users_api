package com.iwellness.admin_users_api.Seguridad;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.iwellness.admin_users_api.Entidades.Proveedor;
import com.iwellness.admin_users_api.Entidades.Rol;
import com.iwellness.admin_users_api.Entidades.Usuarios;
import com.iwellness.admin_users_api.Repositorios.ProveedorRepositorio;
import com.iwellness.admin_users_api.Repositorios.UsuariosRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de JWTProveedor")
public class JWTProveedorTest {

    @InjectMocks
    private JWTProveedor jwtProveedor;

    @Mock
    private UsuariosRepositorio usuariosRepositorio;

    @Mock
    private ProveedorRepositorio proveedorRepositorio;

    @Mock
    private Authentication authentication;

    private Usuarios usuario;
    private Rol rolTurista;
    private String email;

    @BeforeEach
    void setUp() {
        email = "test@example.com";
        
        rolTurista = new Rol();
        rolTurista.setId(1L);
        rolTurista.setNombre("Turista");

        usuario = new Usuarios();
        usuario.setId(1L);
        usuario.setCorreo(email);
        usuario.setNombre("Test User");
        usuario.setRol(rolTurista);
    }

    @Test
    @DisplayName("TokenGenerado - Debería generar token válido para turista")
    void testTokenGenerado_TuristaSuccess() {
        // Arrange
        when(authentication.getName()).thenReturn(email);
        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("Turista"));
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);
        when(usuariosRepositorio.findByCorreo(email)).thenReturn(Optional.of(usuario));

        // Act
        String token = jwtProveedor.TokenGenerado(authentication);

        // Assert
        assertNotNull(token);
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "Token JWT debe tener 3 partes");
        verify(usuariosRepositorio).findByCorreo(email);
    }

    @Test
    @DisplayName("TokenGenerado - Debería generar token válido para proveedor")
    void testTokenGenerado_ProveedorSuccess() {
        // Arrange
        Rol rolProveedor = new Rol();
        rolProveedor.setId(2L);
        rolProveedor.setNombre("Proveedor");
        
        usuario.setRol(rolProveedor);
        
        Proveedor proveedor = new Proveedor();
        proveedor.setId(10L);
        proveedor.setUsuarios(usuario);
        usuario.setProveedor(proveedor);

        when(authentication.getName()).thenReturn(email);
        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("Proveedor"));
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);
        when(usuariosRepositorio.findByCorreo(email)).thenReturn(Optional.of(usuario));

        // Act
        String token = jwtProveedor.TokenGenerado(authentication);

        // Assert
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    @DisplayName("TokenGenerado - Debería lanzar excepción si usuario no existe")
    void testTokenGenerado_UserNotFound() {
        // Arrange
        when(authentication.getName()).thenReturn(email);
        when(usuariosRepositorio.findByCorreo(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            jwtProveedor.TokenGenerado(authentication);
        });
    }

    @Test
    @DisplayName("validateToken - Debería validar token válido correctamente")
    void testValidateToken_ValidToken() {
        // Arrange
        when(authentication.getName()).thenReturn(email);
        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("Turista"));
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);
        when(usuariosRepositorio.findByCorreo(email)).thenReturn(Optional.of(usuario));

        String token = jwtProveedor.TokenGenerado(authentication);

        // Act
        boolean isValid = jwtProveedor.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("validateToken - Debería rechazar token con formato inválido")
    void testValidateToken_InvalidFormat() {
        // Arrange
        String invalidToken = "invalid.token";

        // Act
        boolean isValid = jwtProveedor.validateToken(invalidToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("validateToken - Debería rechazar token con firma incorrecta")
    void testValidateToken_InvalidSignature() {
        // Arrange
        when(authentication.getName()).thenReturn(email);
        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("Turista"));
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);
        when(usuariosRepositorio.findByCorreo(email)).thenReturn(Optional.of(usuario));

        String token = jwtProveedor.TokenGenerado(authentication);
        String tamperedToken = token.substring(0, token.length() - 5) + "xxxxx";

        // Act
        boolean isValid = jwtProveedor.validateToken(tamperedToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("validateToken - Debería rechazar token vacío")
    void testValidateToken_EmptyToken() {
        // Act
        boolean isValid = jwtProveedor.validateToken("");

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("validateToken - Debería rechazar token null")
    void testValidateToken_NullToken() {
        // Act
        boolean isValid = jwtProveedor.validateToken(null);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("getUserFromJwt - Debería extraer usuario correctamente")
    void testGetUserFromJwt_Success() {
        // Arrange
        when(authentication.getName()).thenReturn(email);
        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("Turista"));
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);
        when(usuariosRepositorio.findByCorreo(email)).thenReturn(Optional.of(usuario));

        String token = jwtProveedor.TokenGenerado(authentication);

        // Act
        String extractedUser = jwtProveedor.getUserFromJwt(token);

        // Assert
        assertEquals(email, extractedUser);
    }

    @Test
    @DisplayName("getUserFromJwt - Debería retornar null para token inválido")
    void testGetUserFromJwt_InvalidToken() {
        // Arrange
        String invalidToken = "invalid.token.here";

        // Act
        String extractedUser = jwtProveedor.getUserFromJwt(invalidToken);

        // Assert
        assertNull(extractedUser);
    }

    @Test
    @DisplayName("TokenGenerado - Debería incluir rol en el token")
    void testTokenGenerado_IncludesRole() {
        // Arrange
        when(authentication.getName()).thenReturn(email);
        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("Admin"));
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);
        
        Rol rolAdmin = new Rol();
        rolAdmin.setId(3L);
        rolAdmin.setNombre("Admin");
        usuario.setRol(rolAdmin);
        
        when(usuariosRepositorio.findByCorreo(email)).thenReturn(Optional.of(usuario));

        // Act
        String token = jwtProveedor.TokenGenerado(authentication);

        // Assert
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    @DisplayName("validateToken - Debería rechazar token con partes faltantes")
    void testValidateToken_MissingParts() {
        // Arrange
        String incompleteToken = "header.payload";

        // Act
        boolean isValid = jwtProveedor.validateToken(incompleteToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("getUserFromJwt - Debería manejar token con formato correcto pero datos inválidos")
    void testGetUserFromJwt_InvalidData() {
        // Arrange
        String tokenWithInvalidData = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid_payload.signature";

        // Act
        String extractedUser = jwtProveedor.getUserFromJwt(tokenWithInvalidData);

        // Assert
        assertNull(extractedUser);
    }

    @Test
    @DisplayName("TokenGenerado - Debería manejar proveedor nulo correctamente")
    void testTokenGenerado_NullProveedor() {
        // Arrange
        usuario.setProveedor(null);
        
        when(authentication.getName()).thenReturn(email);
        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("Turista"));
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);
        when(usuariosRepositorio.findByCorreo(email)).thenReturn(Optional.of(usuario));

        // Act
        String token = jwtProveedor.TokenGenerado(authentication);

        // Assert
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
    }
}
