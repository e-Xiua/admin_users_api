package com.iwellness.admin_users_api.Clientes;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "preferencias-ms", url = "${feign.client.preferencias.url:http://localhost:8081}/api/turistaXPreferencia")
public interface PreferenciaFeignClient {
    @DeleteMapping("/eliminarPorTurista/{idTurista}")
    void eliminarPreferenciasPorTurista(@PathVariable("idTurista") Long idTurista);
}
