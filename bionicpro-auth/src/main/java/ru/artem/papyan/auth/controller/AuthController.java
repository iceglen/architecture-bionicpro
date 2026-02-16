package ru.artem.papyan.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.artem.papyan.auth.service.SessionService;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final SessionService sessionService;
    
    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        // Этот endpoint используется Spring Security OAuth2 для перенаправления на Keycloak
        // Фактическая аутентификация происходит через OAuth2 flow
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        // Получаем sessionId из cookie
        String sessionId = extractSessionId(request);
        if (sessionId != null) {
            sessionService.deleteSession(sessionId);
            log.debug("Session deleted: {}", sessionId);
        }
        
        // Очищаем SecurityContext
        SecurityContextHolder.clearContext();
        
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(HttpServletRequest request) {
        String sessionId = extractSessionId(request);
        Map<String, Object> response = new HashMap<>();
        
        if (sessionId != null && sessionService.validateSession(sessionId)) {
            response.put("authenticated", true);
            response.put("sessionId", sessionId);
        } else {
            response.put("authenticated", false);
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/protected")
    public ResponseEntity<Map<String, String>> protectedEndpoint() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "This is a protected endpoint");
        response.put("principal", authentication != null ? authentication.getName() : "anonymous");
        response.put("authenticated", String.valueOf(authentication != null && authentication.isAuthenticated()));
        
        return ResponseEntity.ok(response);
    }
    
    private String extractSessionId(HttpServletRequest request) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("AUTH_SESSION_ID".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}