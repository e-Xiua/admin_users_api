package com.iwellness.admin_users_api.Controlador;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iwellness.admin_users_api.DTO.ProveedorDTO;
import com.iwellness.admin_users_api.DTO.TuristaDTO;
import com.iwellness.admin_users_api.DTO.UsuarioResponseDTO;
import com.iwellness.admin_users_api.DTO.UsuariosDTO;
import com.iwellness.admin_users_api.Entidades.PasswordResetToken;
import com.iwellness.admin_users_api.Entidades.Turista;
import com.iwellness.admin_users_api.Entidades.Usuarios;
import com.iwellness.admin_users_api.Repositorios.PasswordResetTokenRepository;
import com.iwellness.admin_users_api.Repositorios.TuristaRepositorio;
import com.iwellness.admin_users_api.Seguridad.CustomUserDetailsService;
import com.iwellness.admin_users_api.Seguridad.JWTProveedor;
import com.iwellness.admin_users_api.Servicios.EmailService;
import com.iwellness.admin_users_api.Servicios.RegistroServicio;
import com.iwellness.admin_users_api.Servicios.UsuariosServicio;
import com.iwellness.admin_users_api.Servicios.Rabbit.MensajeServiceUsers;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class LogInControlador {

    private static final Logger logger = LoggerFactory.getLogger(LogInControlador.class);

    @Autowired
    private RegistroServicio registroServicio;

    @Autowired
    private UsuariosServicio usuariosServicio;

    @Autowired
    private MensajeServiceUsers mensajeServiceUsers;
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JWTProveedor jwtProveedor;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TuristaRepositorio turistaRepositorio;
    
    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> login(@RequestBody UsuariosDTO user) {
        try {
            logger.info("Intento de inicio de sesión para el usuario: {}", user.getCorreo());
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getCorreo(), user.getContraseña()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = jwtProveedor.TokenGenerado(authentication);
            logger.info("Inicio de sesión exitoso para el usuario: {}", user.getCorreo());
            return new ResponseEntity<>(token, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error de autenticación para el usuario: {}", user.getCorreo(), e);
            return new ResponseEntity<>("Autenticación fallida", HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping(value = "/registro/Turista", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> registrarTurista(@RequestBody Map<String, Object> datos) {
        try {
            String resultado = registroServicio.registrarUsuario(datos, "Turista");
    
            if (resultado.equals("Registro exitoso")) {
                String correo = (String) datos.get("correo");
                Usuarios nuevoTurista = usuariosServicio.findByCorreo(correo)
                    .orElseThrow(() -> new RuntimeException("Turista no encontrado"));
    
                // Busca el objeto Turista asociado al usuario
                Turista turista = turistaRepositorio.findByUsuarios(nuevoTurista)
                    .orElseThrow(() -> new RuntimeException("Turista no encontrado para el usuario"));
    
                // Construir el DTO solo con los campos necesarios
                TuristaDTO turistaDTO = new TuristaDTO();
                turistaDTO.setIdTurista(turista.getId());
                turistaDTO.setNombre(nuevoTurista.getNombre());
                turistaDTO.setTelefono(turista.getTelefono());
                turistaDTO.setCiudad(turista.getCiudad());
                turistaDTO.setPais(turista.getPais());
                turistaDTO.setGenero(turista.getGenero());
                turistaDTO.setEstadoCivil(turista.getEstadoCivil());
    
                ObjectMapper objectMapper = new ObjectMapper();
                String json = objectMapper.writeValueAsString(turistaDTO);
                mensajeServiceUsers.enviarMensajeTurista(json);
    
                return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resultado);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body("Error en el registro: " + e.getMessage());
        }
    }

    @PostMapping(value = "/registro/Proveedor", consumes = MediaType.APPLICATION_JSON_VALUE)
public ResponseEntity<?> registrarProveedor(@RequestBody Map<String, Object> datos) {
    try {
        // Registrar el proveedor
        String resultado = registroServicio.registrarUsuario(datos, "Proveedor");
        
        if (resultado.equals("Registro exitoso")) {
            // Generar token automáticamente
            String correo = (String) datos.get("correo");
            String contraseña = (String) datos.get("contraseña");
            
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(correo, contraseña));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = jwtProveedor.TokenGenerado(authentication);

            //Obtener el proveedor recien registrado
            Usuarios nuevoProveedor = usuariosServicio.findByCorreo(correo).orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

            // Convertir el token a DTO
            ProveedorDTO proveedorDTO = new ProveedorDTO();
            proveedorDTO.setIdProveedor(nuevoProveedor.getId());
            proveedorDTO.setNombre(nuevoProveedor.getNombre());
            proveedorDTO.setNombre_empresa(nuevoProveedor.getProveedor().getNombre_empresa());
            proveedorDTO.setCargoContacto(nuevoProveedor.getProveedor().getCargoContacto());
            proveedorDTO.setTelefono(nuevoProveedor.getProveedor().getTelefono());
            proveedorDTO.setTelefonoEmpresa(nuevoProveedor.getProveedor().getTelefonoEmpresa());
            proveedorDTO.setCoordenadaX(nuevoProveedor.getProveedor().getCoordenadaX());
            proveedorDTO.setCoordenadaY(nuevoProveedor.getProveedor().getCoordenadaY());

            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(proveedorDTO);
            mensajeServiceUsers.enviarMensajeProveedor(json);
            
            // Devolver respuesta con token
            Map<String, String> response = new HashMap<>();
            response.put("message", resultado);
            response.put("token", token);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resultado);
        }
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .body("Error en el registro: " + e.getMessage());
    }
}

    @PostMapping(value = "/registro/Admin", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> registrarAdmin(@RequestBody Map<String, Object> datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registroServicio.registrarUsuario(datos, "Admin"));
    }

    
    // GET: Obtener el rol del usuario a partir del JWT
    @GetMapping("/role")
    public ResponseEntity<String> getRoleFromToken(@RequestHeader("Authorization") String token) {
        try {
            // Extraer el token JWT del encabezado Authorization
            String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;

            // Obtener el rol desde el token
            String role = customUserDetailsService.getUserRoleFromToken(jwtToken);

            // Devolver el rol
            return ResponseEntity.ok(role);
        } catch (IllegalArgumentException e) {
            // Manejar token JWT inválido
            return ResponseEntity.badRequest().body("Invalid JWT token");
        } catch (Exception e) {
            // Manejar otros errores
            return ResponseEntity.status(500).body("Internal server error");
        }
    }

    @GetMapping("/info")
    public ResponseEntity<?> ObtenerInforUsuario(@RequestHeader("Authorization") String token){
        try {
            // Extraer el token JWT del encabezado Authorization
            String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;

            // Obtener el username desde el token
            String username = customUserDetailsService.getUserFromToken(jwtToken);

            // Buscar usuario en la base de datos
            Optional<Usuarios> usuarioOpt = usuariosServicio.findByCorreo(username);

            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
            }
            
            Usuarios usuario = usuarioOpt.get();
            
            // Usar el método fromEntity del DTO para evitar problemas de serialización
            UsuarioResponseDTO responseDTO = UsuarioResponseDTO.fromEntity(usuario);
    
            return ResponseEntity.ok(responseDTO);

        }catch (Exception e) {
            logger.error("Error al obtener información del usuario", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token inválido");
        }
    }

    @PostMapping("/request-reset-password")
    public ResponseEntity<?> requestResetPassword(@RequestParam String correo) {
        Optional<Usuarios> usuario = usuariosServicio.findByCorreo(correo);
        if (usuario.isEmpty()) {
            return ResponseEntity.badRequest().body("Correo no encontrado");
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setCorreo(correo);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        passwordResetTokenRepository.save(resetToken);

        String enlaceRecuperacion = "http://localhost:4200/restablecer?token=" + token;

        String cuerpoCorreo = "Hola " + usuario.get().getNombre() + ",\n\n"
            + "Haz clic en el siguiente enlace para restablecer tu contraseña:\n"
            + enlaceRecuperacion + "\n\n"
            + "Este enlace expirará en 15 minutos.\n\n"
            + "Saludos,\nEquipo I-Wellness";

        emailService.enviarCorreo(correo, "Recuperación de contraseña", cuerpoCorreo);

        return ResponseEntity.ok("Correo de recuperación enviado");
    }

    @PostMapping("/reset-password")
public ResponseEntity<?> resetPassword(@RequestParam String token, @RequestParam String nuevaContrasena) {
    Optional<PasswordResetToken> resetToken = passwordResetTokenRepository.findByToken(token);

    if (resetToken.isEmpty() || resetToken.get().getExpiryDate().isBefore(LocalDateTime.now())) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Token inválido o expirado"));
    }

    Optional<Usuarios> usuario = usuariosServicio.findByCorreo(resetToken.get().getCorreo());

    if (usuario.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Usuario no encontrado"));
    }

    Usuarios user = usuario.get();
    user.setContraseña(passwordEncoder.encode(nuevaContrasena)); 
    usuariosServicio.save(user);

    passwordResetTokenRepository.delete(resetToken.get());

    return ResponseEntity.ok(Map.of("mensaje", "Contraseña restablecida correctamente"));
}



}
