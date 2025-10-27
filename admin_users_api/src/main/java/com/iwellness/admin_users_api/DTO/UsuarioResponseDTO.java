package com.iwellness.admin_users_api.DTO;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.iwellness.admin_users_api.Entidades.Proveedor;
import com.iwellness.admin_users_api.Entidades.Turista;
import com.iwellness.admin_users_api.Entidades.Usuarios;

import lombok.Data;

@Data
public class UsuarioResponseDTO {
    private Long id;
    private String nombre;
    private String correo;
    private String foto;
    private String rol;
    
    // Información específica de turista
    private TuristaInfo turistaInfo;
    
    // Información específica de proveedor
    private ProveedorInfo proveedorInfo;
    
    @Data
    public static class TuristaInfo {
        private Long id;
        private String telefono;
        private String direccion;
        private String ciudad;
        private String pais;
        private String genero;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private Date fechaNacimiento;
        private String estadoCivil;
    }
    
    @Data
    public static class ProveedorInfo {
        private Long id;
        private String nombre_empresa;
        private String coordenadaX;
        private String coordenadaY;
        private String cargoContacto;
        private String telefono;
        private String identificacionFiscal;
        private String telefonoEmpresa;
        private String licenciasPermisos;
        private String certificadosCalidad;
    }
    
    public static UsuarioResponseDTO fromEntity(Usuarios usuario) {
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(usuario.getId());
        dto.setNombre(usuario.getNombre());
        dto.setCorreo(usuario.getCorreo());
        dto.setFoto(usuario.getFoto());
        dto.setRol(usuario.getRol().getNombre());
        
        // Si el rol es Turista, incluir información de turista
        if (usuario.getTurista() != null) {
            TuristaInfo turistaInfo = new TuristaInfo();
            Turista turista = usuario.getTurista();
            
            turistaInfo.setId(turista.getId());
            turistaInfo.setTelefono(turista.getTelefono());
            turistaInfo.setCiudad(turista.getCiudad());
            turistaInfo.setPais(turista.getPais());
            turistaInfo.setGenero(turista.getGenero());
            turistaInfo.setFechaNacimiento(turista.getFechaNacimiento());
            turistaInfo.setEstadoCivil(turista.getEstadoCivil());
            
            dto.setTuristaInfo(turistaInfo);
        }
        
        // Si el rol es Proveedor, incluir información de proveedor
        if (usuario.getProveedor() != null) {
            ProveedorInfo proveedorInfo = new ProveedorInfo();
            Proveedor proveedor = usuario.getProveedor();
            
            proveedorInfo.setId(proveedor.getId());
            proveedorInfo.setNombre_empresa(proveedor.getNombre_empresa());
            proveedorInfo.setCoordenadaX(proveedor.getCoordenadaX());
            proveedorInfo.setCoordenadaY(proveedor.getCoordenadaY());
            proveedorInfo.setCargoContacto(proveedor.getCargoContacto());
            proveedorInfo.setTelefono(proveedor.getTelefono());
            proveedorInfo.setTelefonoEmpresa(proveedor.getTelefonoEmpresa());
            
            dto.setProveedorInfo(proveedorInfo);
        }
        
        return dto;
    }
}