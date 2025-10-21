package com.iwellness.admin_users_api.Controlador;


import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iwellness.admin_users_api.DTO.UsuarioResponseDTO;
import com.iwellness.admin_users_api.Servicios.ContactService;

@RestController
@RequestMapping("/usuarios/{userId}/contacts")
public class ContactControlador {

    private final ContactService contactService;

    public ContactControlador(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping("/{contactId}")
    public ResponseEntity<Void> addContact(@PathVariable Long userId, @PathVariable Long contactId) {
        contactService.addContact(userId, contactId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> getContacts(@PathVariable Long userId) {
        List<UsuarioResponseDTO> contacts = contactService.getContacts(userId);
        return ResponseEntity.ok(contacts);
    }
}
