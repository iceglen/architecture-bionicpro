package ru.artem.papyan.auth.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import ru.artem.papyan.auth.dto.TokenResponse;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {
    
    private final TextEncryptor textEncryptor;
    private final TokenCacheService tokenCacheService;
    private final KeycloakService keycloakService;
    
    @Value("${auth.session.rotation.enabled}")
    private boolean rotationEnabled;
    
    @Value("${auth.session.rotation.interval}")
    private int rotationInterval;
    
    // In-memory tracking for rotation (можно заменить на Redis)
    private final Map<String, SessionMetadata> sessionMetadataMap = new ConcurrentHashMap<>();
    
    public String createSession(TokenResponse tokenResponse) {
        String sessionId = generateSessionId();
        String encryptedRefreshToken = encryptRefreshToken(tokenResponse.getRefreshToken());
        
        // Сохраняем токены в Redis
        tokenCacheService.saveTokens(sessionId, tokenResponse.getAccessToken(), encryptedRefreshToken);
        
        // Сохраняем метаданные сессии
        SessionMetadata metadata = new SessionMetadata();
        metadata.setCreatedAt(LocalDateTime.now());
        metadata.setRequestCount(0);
        metadata.setLastRotatedAt(LocalDateTime.now());
        sessionMetadataMap.put(sessionId, metadata);
        
        log.debug("Created new session: {}", sessionId);
        return sessionId;
    }
    
    public boolean validateSession(String sessionId) {
        if (!tokenCacheService.hasSession(sessionId)) {
            log.debug("Session not found: {}", sessionId);
            return false;
        }
        
        String accessToken = tokenCacheService.getAccessToken(sessionId);
        if (accessToken == null) {
            log.debug("Access token not found for session: {}", sessionId);
            return false;
        }
        
        // Проверяем валидность access token
        if (!keycloakService.validateToken(accessToken)) {
            log.debug("Access token invalid for session: {}, attempting refresh", sessionId);
            return refreshTokens(sessionId);
        }
        
        // Обновляем счетчик запросов
        updateRequestCount(sessionId);
        
        return true;
    }
    
    public boolean refreshTokens(String sessionId) {
        try {
            String encryptedRefreshToken = tokenCacheService.getEncryptedRefreshToken(sessionId);
            if (encryptedRefreshToken == null) {
                log.error("No refresh token found for session: {}", sessionId);
                return false;
            }
            
            String refreshToken = decryptRefreshToken(encryptedRefreshToken);
            TokenResponse newTokens = keycloakService.refreshTokens(refreshToken);
            
            // Обновляем токены в Redis
            String newEncryptedRefreshToken = encryptRefreshToken(newTokens.getRefreshToken());
            tokenCacheService.saveTokens(sessionId, newTokens.getAccessToken(), newEncryptedRefreshToken);
            
            log.debug("Successfully refreshed tokens for session: {}", sessionId);
            return true;
        } catch (Exception e) {
            log.error("Failed to refresh tokens for session: {}", sessionId, e);
            return false;
        }
    }
    
    public String rotateSessionIfNeeded(String sessionId) {
        if (!rotationEnabled) {
            return sessionId;
        }
        
        SessionMetadata metadata = sessionMetadataMap.get(sessionId);
        if (metadata == null) {
            log.debug("No metadata found for session: {}", sessionId);
            return sessionId;
        }
        
        if (metadata.getRequestCount() % rotationInterval == 0 && metadata.getRequestCount() > 0) {
            log.debug("Rotating session: {} (request count: {})", sessionId, metadata.getRequestCount());
            return rotateSession(sessionId);
        }
        
        return sessionId;
    }
    
    private String rotateSession(String oldSessionId) {
        // Создаем новую сессию
        String newSessionId = generateSessionId();
        
        // Копируем токены из старой сессии
        String accessToken = tokenCacheService.getAccessToken(oldSessionId);
        String encryptedRefreshToken = tokenCacheService.getEncryptedRefreshToken(oldSessionId);
        
        if (accessToken == null || encryptedRefreshToken == null) {
            log.error("Cannot rotate session {} - missing tokens", oldSessionId);
            return oldSessionId;
        }
        
        // Сохраняем в новой сессии
        tokenCacheService.saveTokens(newSessionId, accessToken, encryptedRefreshToken);
        
        // Копируем метаданные
        SessionMetadata oldMetadata = sessionMetadataMap.get(oldSessionId);
        SessionMetadata newMetadata = new SessionMetadata();
        newMetadata.setCreatedAt(LocalDateTime.now());
        newMetadata.setRequestCount(0);
        newMetadata.setLastRotatedAt(LocalDateTime.now());
        sessionMetadataMap.put(newSessionId, newMetadata);
        
        // Удаляем старую сессию
        tokenCacheService.deleteSession(oldSessionId);
        sessionMetadataMap.remove(oldSessionId);
        
        log.debug("Session rotated: {} -> {}", oldSessionId, newSessionId);
        return newSessionId;
    }
    
    public void deleteSession(String sessionId) {
        tokenCacheService.deleteSession(sessionId);
        sessionMetadataMap.remove(sessionId);
        log.debug("Deleted session: {}", sessionId);
    }
    
    public String getAccessToken(String sessionId) {
        return tokenCacheService.getAccessToken(sessionId);
    }
    
    private String generateSessionId() {
        return "session_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    private String encryptRefreshToken(String refreshToken) {
        return textEncryptor.encrypt(refreshToken);
    }
    
    private String decryptRefreshToken(String encryptedRefreshToken) {
        return textEncryptor.decrypt(encryptedRefreshToken);
    }
    
    private void updateRequestCount(String sessionId) {
        SessionMetadata metadata = sessionMetadataMap.get(sessionId);
        if (metadata != null) {
            metadata.setRequestCount(metadata.getRequestCount() + 1);
        }
    }
    
    @Data
    private static class SessionMetadata {
        private LocalDateTime createdAt;
        private int requestCount;
        private LocalDateTime lastRotatedAt;
    }
}