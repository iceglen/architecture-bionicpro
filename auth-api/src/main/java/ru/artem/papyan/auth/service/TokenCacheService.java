package ru.artem.papyan.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCacheService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String ACCESS_TOKEN_PREFIX = "access_token:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String SESSION_PREFIX = "session:";
    
    // TTL в секундах: 1800 секунд = 30 минут (дольше чем access token 2 минуты)
    private static final long SESSION_TTL = 1800L;
    
    public void saveTokens(String sessionId, String accessToken, String encryptedRefreshToken) {
        String accessTokenKey = ACCESS_TOKEN_PREFIX + sessionId;
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + sessionId;
        String sessionKey = SESSION_PREFIX + sessionId;
        
        // Сохраняем access token с коротким TTL (например, 5 минут для надежности)
        redisTemplate.opsForValue().set(accessTokenKey, accessToken, Duration.ofMinutes(5));
        
        // Сохраняем зашифрованный refresh token с более долгим TTL
        redisTemplate.opsForValue().set(refreshTokenKey, encryptedRefreshToken, Duration.ofSeconds(SESSION_TTL));
        
        // Сохраняем сессию с TTL
        redisTemplate.opsForValue().set(sessionKey, "active", Duration.ofSeconds(SESSION_TTL));
        
        log.debug("Saved tokens for session: {}", sessionId);
    }
    
    public String getAccessToken(String sessionId) {
        String key = ACCESS_TOKEN_PREFIX + sessionId;
        return redisTemplate.opsForValue().get(key);
    }
    
    public String getEncryptedRefreshToken(String sessionId) {
        String key = REFRESH_TOKEN_PREFIX + sessionId;
        return redisTemplate.opsForValue().get(key);
    }
    
    public boolean hasSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }
    
    public void deleteSession(String sessionId) {
        String accessTokenKey = ACCESS_TOKEN_PREFIX + sessionId;
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + sessionId;
        String sessionKey = SESSION_PREFIX + sessionId;
        
        redisTemplate.delete(accessTokenKey);
        redisTemplate.delete(refreshTokenKey);
        redisTemplate.delete(sessionKey);
        
        log.debug("Deleted session data for: {}", sessionId);
    }
    
    public void updateAccessToken(String sessionId, String newAccessToken) {
        String key = ACCESS_TOKEN_PREFIX + sessionId;
        redisTemplate.opsForValue().set(key, newAccessToken, Duration.ofMinutes(5));
        log.debug("Updated access token for session: {}", sessionId);
    }
    
    public void extendSession(String sessionId) {
        String sessionKey = SESSION_PREFIX + sessionId;
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + sessionId;
        
        if (redisTemplate.hasKey(sessionKey) != null && redisTemplate.hasKey(sessionKey)) {
            redisTemplate.expire(sessionKey, Duration.ofSeconds(SESSION_TTL));
            redisTemplate.expire(refreshTokenKey, Duration.ofSeconds(SESSION_TTL));
            log.debug("Extended session TTL for: {}", sessionId);
        }
    }
}