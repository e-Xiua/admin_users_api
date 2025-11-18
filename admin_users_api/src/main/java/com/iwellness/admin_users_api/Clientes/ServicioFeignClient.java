package com.iwellness.admin_users_api.Clientes;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "servicio-ms", url = "${feign.client.servicio.url:http://localhost:8080}/api/servicio")
public interface ServicioFeignClient {    @DeleteMapping("/eliminarPorProveedor/{idProveedor}")
    void eliminarServiciosPorProveedor(@PathVariable("idProveedor") Long idProveedor);
}
