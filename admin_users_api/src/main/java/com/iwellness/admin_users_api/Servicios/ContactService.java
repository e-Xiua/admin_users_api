package com.iwellness.admin_users_api.Servicios;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.iwellness.admin_users_api.DTO.UsuarioResponseDTO;
import com.iwellness.admin_users_api.Entidades.Contacto;
import com.iwellness.admin_users_api.Entidades.Usuarios;
import com.iwellness.admin_users_api.Repositorios.ContactRepositorio;
import com.iwellness.admin_users_api.Repositorios.UsuariosRepositorio;

@Service
public class ContactService {
    
    private final ContactRepositorio contactRepository;
    private final UsuariosRepositorio usuarioRepository; // Necesario para validar y obtener datos

    public ContactService(ContactRepositorio contactRepository, UsuariosRepositorio usuarioRepository) {
        this.contactRepository = contactRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public void addContact(Long ownerId, Long contactId) {
        // Validaciones
        if (ownerId.equals(contactId)) {
            throw new IllegalArgumentException("User cannot add themselves as a contact.");
        }
        if (!usuarioRepository.existsById(ownerId) || !usuarioRepository.existsById(contactId)) {
            throw new RuntimeException("One or both users do not exist.");
        }
        if (contactRepository.existsByOwnerUserIdAndContactUserId(ownerId, contactId)) {
            throw new IllegalArgumentException("This user is already in your contact list.");
        }

        Contacto newContact = new Contacto();
        newContact.setOwnerUserId(ownerId);
        newContact.setContactUserId(contactId);
        contactRepository.save(newContact);
    }

    public List<UsuarioResponseDTO> getContacts(Long ownerId) {
        // 1. Obtener la lista de relaciones de contacto
        List<Contacto> contacts = contactRepository.findByOwnerUserId(ownerId);

        // 2. Extraer solo los IDs de los contactos
        List<Long> contactIds = contacts.stream()
                                        .map(Contacto::getContactUserId)
                                        .collect(Collectors.toList());

        // 3. Buscar todos esos usuarios en la base de datos y convertirlos a DTO
        return usuarioRepository.findAllById(contactIds).stream()
                                .map(this::mapToUsuarioDTO) // Asume que tienes un método de mapeo
                                .collect(Collectors.toList());
    }
    private UsuarioResponseDTO mapToUsuarioDTO(Usuarios user) {
        // Lógica para convertir tu entidad Usuarios a UsuarioResponseDTO
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(user.getId());
        dto.setNombre(user.getNombre());
        dto.setCorreo(user.getCorreo());
        dto.setFoto(user.getFoto());
        dto.setRol(user.getRol().getNombre());
        // ... mapear otros campos según la entidad Usuarios y el DTO
        return dto;
    }
    }

