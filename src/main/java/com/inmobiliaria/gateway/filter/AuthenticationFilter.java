package com.inmobiliaria.gateway.filter;

import com.inmobiliaria.gateway.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter implements GlobalFilter, Ordered { // 👈 Cambio clave aquí

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. Rutas públicas
        if (path.contains("/api/auth/login") || path.contains("/api/public/")) {
            return chain.filter(exchange);
        }
        
        // 👇👇👇 2. NUEVO: DEJAR PASAR LA PETICIÓN INVISIBLE DEL NAVEGADOR 👇👇👇
        if (exchange.getRequest().getMethod().name().equals("OPTIONS")) {
            return chain.filter(exchange);
        }

        // 2. Obtener Token
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtUtil.isTokenValid(token)) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            // 3. Extraer datos
            String username = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractClaim(token, claims -> claims.get("rol", String.class));

            // 4. EL TRUCO: Inyectar headers directamente en el exchange de salida
            // Usamos mutate() pero aplicado al objeto exchange que recibe el chain
            return chain.filter(exchange.mutate()
                .request(r -> r.header("X-Auth-User", username)
                              .header("X-Auth-Roles", role))
                .build());

        } catch (Exception e) {
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public int getOrder() {
        return -1; // 👈 Esto hace que sea lo PRIMERO que se ejecute
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}