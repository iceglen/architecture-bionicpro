package ru.artem.papyan.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.artem.papyan.auth.service.SessionService;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionFilter extends OncePerRequestFilter {
    
    private final SessionService sessionService;
    
    private static final String SESSION_COOKIE_NAME = "AUTH_SESSION_ID";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String sessionId = extractSessionId(request);
        
        if (sessionId != null) {
            log.debug("Processing request with session ID: {}", sessionId);
            
            // Проверяем валидность сессии
            if (sessionService.validateSession(sessionId)) {
                // Ротация сессии при необходимости
                String newSessionId = sessionService.rotateSessionIfNeeded(sessionId);
                
                if (!sessionId.equals(newSessionId)) {
                    // Устанавливаем новую сессию в cookie
                    setSessionCookie(response, newSessionId);
                    sessionId = newSessionId;
                }
                
                // Устанавливаем аутентификацию в SecurityContext
                String accessToken = sessionService.getAccessToken(sessionId);
                if (accessToken != null) {
                    PreAuthenticatedAuthenticationToken authentication = 
                        new PreAuthenticatedAuthenticationToken(sessionId, accessToken, null);
                    authentication.setAuthenticated(true);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("Authentication set for session: {}", sessionId);
                }
            } else {
                log.debug("Session validation failed for: {}", sessionId);
                SecurityContextHolder.clearContext();
                clearSessionCookie(response);
            }
        } else {
            log.debug("No session ID found in request");
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractSessionId(HttpServletRequest request) {
        // 1. Проверяем cookie
        if (request.getCookies() != null) {
            Optional<Cookie> sessionCookie = Arrays.stream(request.getCookies())
                .filter(cookie -> SESSION_COOKIE_NAME.equals(cookie.getName()))
                .findFirst();
            
            if (sessionCookie.isPresent()) {
                return sessionCookie.get().getValue();
            }
        }
        
        // 2. Проверяем заголовок (на случай, если cookie не поддерживаются)
        String sessionHeader = request.getHeader("X-Session-ID");
        if (sessionHeader != null && !sessionHeader.trim().isEmpty()) {
            return sessionHeader.trim();
        }
        
        return null;
    }
    
    private void setSessionCookie(HttpServletResponse response, String sessionId) {
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, sessionId);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // false для localhost, true для production
        cookie.setPath("/");
        cookie.setMaxAge(30 * 60); // 30 минут
        response.addCookie(cookie);
        
        log.debug("Set session cookie: {}", sessionId);
    }
    
    private void clearSessionCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Удалить cookie
        response.addCookie(cookie);
        
        log.debug("Cleared session cookie");
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Не фильтруем публичные endpoints и OAuth2 endpoints
        return path.startsWith("/api/auth/login") || 
               path.startsWith("/login/oauth2") ||
               path.startsWith("/oauth2/") ||
               path.startsWith("/error");
    }
}