package com.iwellness.admin_users_api.Repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.iwellness.admin_users_api.Entidades.Contacto;

public interface ContactRepositorio extends JpaRepository<Contacto, Long> {
     // Encuentra todos los IDs de contacto para un usuario específico
    List<Contacto> findByOwnerUserId(Long ownerUserId);

    // Revisa si ya existe una relación de contacto
    boolean existsByOwnerUserIdAndContactUserId(Long ownerUserId, Long contactUserId);
}
