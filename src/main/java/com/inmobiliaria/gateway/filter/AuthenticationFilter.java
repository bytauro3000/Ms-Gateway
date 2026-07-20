package com.inmobiliaria.gateway.filter;

import com.inmobiliaria.gateway.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
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
public class AuthenticationFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    // Secreto compartido que identifica al Gateway ante los servicios internos
    @Value("${gateway.secret-key}")
    private String gatewaySecretKey;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. Rutas públicas — pasan DIRECTAS sin mutar el request
        //    para NO perder la cookie de refresh_token ni otros headers.
        //    El backend ya acepta estas rutas sin X-Gateway-Secret.
        if (esRutaPublica(path)) {
            return chain.filter(exchange);
        }

        // 2. Peticiones OPTIONS del navegador (preflight CORS)
        if (exchange.getRequest().getMethod().name().equals("OPTIONS")) {
            return chain.filter(exchange);
        }

        // 3. Obtener y validar el JWT
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtUtil.isTokenValid(token)) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            // 4. Extraer datos del token
            String username = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractClaim(token, claims -> claims.get("rol", String.class));

            // 5. Reenviar la petición con los tres headers:
            //    - X-Auth-User / X-Auth-Roles → identifican al usuario en los servicios internos
            //    - X-Gateway-Secret           → prueba que la petición viene del Gateway
            return chain.filter(exchange.mutate()
                .request(r -> r
                    .header("X-Auth-User", username)
                    .header("X-Auth-Roles", role)
                    .header("X-Gateway-Secret", gatewaySecretKey))
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

    private boolean esRutaPublica(String path) {
        return path.contains("/api/auth/login")
                || path.contains("/api/auth/refresh")
                || path.contains("/api/auth/logout")
                || path.contains("/api/public/");
    }
}