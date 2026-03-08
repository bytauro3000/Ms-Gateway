package com.inmobiliaria.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class HealthController {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @GetMapping("/ping")
    public Mono<ResponseEntity<Map<String, String>>> checkHealth() {
        // Ahora solo despertamos los servicios que no tengan un cron job externo
        String urlServiciosBasicos = "https://serviciobasico.onrender.com/api/public/ping";

        enviarPingDespertador(urlServiciosBasicos, "MS-ServiciosBasicos");

        Map<String, String> response = new HashMap<>();
        response.put("status", "Health Check OK");
        response.put("message", "Gateway awake. Waking up internal services with retries...");

        return Mono.just(ResponseEntity.ok(response));
    }

    private void enviarPingDespertador(String url, String nombreServicio) {
        webClientBuilder.build().get()
            .uri(url)
            .retrieve()
            .bodyToMono(String.class)
            .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))) 
            .doOnSuccess(s -> System.out.println("EXITO: " + nombreServicio + " ha respondido al ping."))
            .doOnError(e -> System.out.println("INFO: " + nombreServicio + " aún despertando..."))
            .subscribe(); 
    }
}