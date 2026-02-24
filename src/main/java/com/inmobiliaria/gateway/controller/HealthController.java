package com.inmobiliaria.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class HealthController { // Clase renombrada a salud

    @Autowired
    private WebClient.Builder webClientBuilder;

    @GetMapping("/ping")
    public Mono<ResponseEntity<Map<String, String>>> checkHealth() {
        // URLs de tus servicios en Render
        String urlMonolito = "https://inmobiliariaivan.onrender.com/api/public/ping";
        String urlServiciosBasicos = "https://serviciosbasicos.onrender.com/api/public/ping";

        // Envía pings en segundo plano para despertar los servicios
        webClientBuilder.build().get().uri(urlMonolito).retrieve().bodyToMono(String.class).subscribe();
        webClientBuilder.build().get().uri(urlServiciosBasicos).retrieve().bodyToMono(String.class).subscribe();

        // Respuesta para confirmar que el Gateway está activo
        Map<String, String> response = new HashMap<>();
        response.put("status", "Health Check OK");
        response.put("message", "Gateway awake. Waking up internal services...");

        return Mono.just(ResponseEntity.ok(response));
    }
}