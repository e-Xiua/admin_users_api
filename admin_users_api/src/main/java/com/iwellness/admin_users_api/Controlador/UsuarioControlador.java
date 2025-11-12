package com.iwellness.admin_users_api.Controlador;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iwellness.admin_users_api.DTO.EditarProveedorDTO;
import com.iwellness.admin_users_api.DTO.EditarTuristaDTO;
import com.iwellness.admin_users_api.DTO.ProveedorDTO;
import com.iwellness.admin_users_api.DTO.TuristaDTO;
import com.iwellness.admin_users_api.DTO.UsuarioResponseDTO;
import com.iwellness.admin_users_api.Entidades.Usuarios;
import com.iwellness.admin_users_api.Servicios.UsuariosServicio;
import com.iwellness.admin_users_api.Servicios.Rabbit.MensajeServiceUsers;

@RestController
@RequestMapping("/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioControlador {

    @Autowired
    private UsuariosServicio usuariosServicio;


    @Autowired
    private MensajeServiceUsers mensajeServiceUsers;

    



    // Método helper para obtener el usuario actual
    Usuarios getUsuarioActual() {
        String emailUsuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuariosServicio.findByCorreo(emailUsuarioActual).orElse(null);
    }

    // Método helper para verificar si es admin
    boolean isAdmin(Usuarios usuario) {
        return usuario != null && "Admin".equals(usuario.getRol().getNombre());
    }

    // Método helper para verificar si es el propietario
    boolean isOwner(Usuarios usuario, Long id) {
        return usuario != null && usuario.getId().equals(id);
    }

    @GetMapping("/all")
    public ResponseEntity<?> obtenerTodosLosUsuarios() {
        try {
            // Usar el nuevo método que devuelve Map<String, Object>
            return ResponseEntity.ok(usuariosServicio.findAllWithDetails());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body("Error al obtener los usuarios: " + e.getMessage());
        }
    }

    @GetMapping("/buscar/{id}")
    public ResponseEntity<?> obtenerUsuarioPorId(@PathVariable Long id) {
        try {
            Usuarios usuarioActual = getUsuarioActual();
            
            if (usuarioActual == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                       .body("Usuario no autenticado");
            }
            
            // Verificar permisos: solo admins o el propio usuario pueden acceder
            if (!isAdmin(usuarioActual) && !isOwner(usuarioActual, id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                       .body("No tiene permisos para acceder a este perfil");
            }
            
            Object usuario = usuariosServicio.findByIdWithDetails(id);
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                       .body("No se encontró el usuario con ID: " + id);
            }
            return ResponseEntity.ok(usuario);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body("Error al obtener el usuario: " + e.getMessage());
        }
    }

        /**
     * Devuelve los detalles de un usuario sin verificar la propiedad.
     * Ideal para páginas de perfil públicas.
     * Aún requiere que el solicitante esté autenticado.
     */
    @GetMapping("/perfil-publico/{id}")
    public ResponseEntity<?> obtenerPerfilPublicoPorId(@PathVariable Long id) {
        try {
            // 1. Validar que hay un usuario autenticado
            if (getUsuarioActual() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                       .body("Debe estar autenticado para ver perfiles");
            }

        // 2. Llamar al servicio para obtener el usuario y validar que es un proveedor
            Usuarios usuario = usuariosServicio.findById(id);
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                       .body("No se encontró un proveedor con el ID: " + id);
            }

            UsuarioResponseDTO responseDTO = UsuarioResponseDTO.fromEntity(usuario);

            // 4. Devolvemos el DTO en la respuesta. El JSON será limpio y seguro.
            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            // Captura cualquier otra excepción del servicio (ej. RuntimeException)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body("Error al obtener el perfil público: " + e.getMessage());
        }
    }
 
    @PutMapping("/editarTurista/{id}")
    public ResponseEntity<?> editarUsuarioTurista(@PathVariable Long id, @RequestBody EditarTuristaDTO dto) {
        try {
            Usuarios usuarioActual = getUsuarioActual();
            
            if (usuarioActual == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                       .body("Usuario no autenticado");
            }
            
            // Solo admins o el propio usuario pueden editar
            if (!isAdmin(usuarioActual) && !isOwner(usuarioActual, id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                       .body("No tiene permisos para editar este usuario");
            }

            Usuarios usuarioActualizado = usuariosServicio.actualizarUsuarioTurista(id, dto);

            // Convertir a DTO
            TuristaDTO turistaDTO = new TuristaDTO();
            turistaDTO.setIdTurista(usuarioActualizado.getId());
            turistaDTO.setNombre(usuarioActualizado.getNombre());
            turistaDTO.setTelefono(usuarioActualizado.getTurista().getTelefono());
            turistaDTO.setFechaNacimiento(
                usuarioActualizado.getTurista().getFechaNacimiento() == null ? null :
                new java.sql.Date(usuarioActualizado.getTurista().getFechaNacimiento().getTime())
            );
            turistaDTO.setGenero(usuarioActualizado.getTurista().getGenero());
            turistaDTO.setCiudad(usuarioActualizado.getTurista().getCiudad());
            turistaDTO.setPais(usuarioActualizado.getTurista().getPais());
            turistaDTO.setEstadoCivil(usuarioActualizado.getTurista().getEstadoCivil());

            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(turistaDTO);
            System.out.println("Mensaje enviado a la cola: " + json);

            // Enviar mensaje a la cola
            mensajeServiceUsers.enviarMensajeTurista(json);


            return ResponseEntity.ok(turistaDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/editarProveedor/{id}")
    public ResponseEntity<?> editarUsuarioProveedor(@PathVariable Long id, @RequestBody EditarProveedorDTO dto) {
        try {
            //1. Actualizar el usuario proveedor
            Usuarios usuarioActual = usuariosServicio.actualizarUsuarioProveedor(id, dto);
            System.out.println("Usuario actual: " + (usuarioActual != null ? usuarioActual.getCorreo() : "null"));

            if (usuarioActual == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                       .body("Usuario no autenticado");
            }
            
            // Solo admins o el propio usuario pueden editar
            if (!isAdmin(usuarioActual) && !isOwner(usuarioActual, id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                       .body("No tiene permisos para editar este usuario");
            }

            //2. Actualizar el proveedor
            System.out.println("Actualizando proveedor con ID: " + id);
            Usuarios usuarioProveedorActualizado = usuariosServicio.actualizarUsuarioProveedor(id, dto);

            //3. Convertir a DTO y enviar a la cola
            System.out.println("Convertiendo a DTO y enviando a la cola");
            ProveedorDTO proveedorDTO = new ProveedorDTO();
            proveedorDTO.setIdProveedor(usuarioProveedorActualizado.getId());
            proveedorDTO.setNombre(usuarioProveedorActualizado.getNombre());
            proveedorDTO.setNombre_empresa(usuarioProveedorActualizado.getProveedor().getNombre_empresa());
            proveedorDTO.setCargoContacto(usuarioProveedorActualizado.getProveedor().getCargoContacto());
            proveedorDTO.setTelefono(usuarioProveedorActualizado.getProveedor().getTelefono());
            proveedorDTO.setTelefonoEmpresa(usuarioProveedorActualizado.getProveedor().getTelefonoEmpresa());
            proveedorDTO.setCoordenadaX(usuarioProveedorActualizado.getProveedor().getCoordenadaX());
            proveedorDTO.setCoordenadaY(usuarioProveedorActualizado.getProveedor().getCoordenadaY());

            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(proveedorDTO);
            System.out.println("Mensaje enviado a la cola: " + json);

            //4. Enviar mensaje a la cola
            System.out.println("Enviando mensaje a la cola");
            mensajeServiceUsers.enviarMensajeProveedor(json);

            return ResponseEntity.ok(usuarioProveedorActualizado);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long id) {
        try {
            Usuarios usuarioActual = getUsuarioActual();
            
            if (usuarioActual == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                       .body("Usuario no autenticado");
            }
            
            // Solo administradores pueden eliminar usuarios
            if (!isAdmin(usuarioActual)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                       .body("Solo los administradores pueden eliminar usuarios");
            }
            
            Usuarios existingUsuario = usuariosServicio.findById(id);
            if (existingUsuario == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                       .body("No se encontró el usuario con ID: " + id);
            }
            
            usuariosServicio.deleteById(id);
            return ResponseEntity.ok("Usuario eliminado con éxito");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body("Error al eliminar el usuario: " + e.getMessage());
        }
    }

    @GetMapping("/proveedores")
    public ResponseEntity<?> obtenerProveedores() {
        try {
            // 1. Obtener todos los proveedores como entidad
            List<Usuarios> proveedores = usuariosServicio.obtenerProveedores();

            // 2. Mapear a DTO
            List<UsuarioResponseDTO> proveedoresDTO = proveedores.stream()
                .map(UsuarioResponseDTO::fromEntity)
                .collect(Collectors.toList());

            // 3. Enviar cada proveedor a la cola
            ObjectMapper objectMapper = new ObjectMapper();
            for (UsuarioResponseDTO proveedor : proveedoresDTO) {
                String json = objectMapper.writeValueAsString(proveedor.getProveedorInfo());
                mensajeServiceUsers.enviarMensajeProveedor(json);
            }


            return ResponseEntity.ok(proveedoresDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al enviar proveedores a la cola: " + e.getMessage());
        }
}

    @GetMapping("/turistas")
    public ResponseEntity<?> obtenerTuristas() {
        try {
            Usuarios usuarioActual = getUsuarioActual();
            // Solo administradores pueden ver todos los turistas
            if (!isAdmin(usuarioActual)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                       .body("No tiene permisos para ver todos los turistas");
            }

            List<UsuarioResponseDTO> turistas = usuariosServicio.obtenerTuristas().stream()
                .map(UsuarioResponseDTO::fromEntity)
                .collect(Collectors.toList());

            ObjectMapper objectMapper = new ObjectMapper();
            for (UsuarioResponseDTO turista : turistas) {
                String json = objectMapper.writeValueAsString(turista.getTuristaInfo());
                mensajeServiceUsers.enviarMensajeTurista(json);
            }
            // Ahora devolvemos la lista de turistas en vez de solo un mensaje
            return ResponseEntity.ok(turistas);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body("Error al obtener los turistas: " + e.getMessage());
        }
    }
}