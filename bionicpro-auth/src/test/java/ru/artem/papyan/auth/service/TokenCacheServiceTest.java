package ru.artem.papyan.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenCacheServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TokenCacheService tokenCacheService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        tokenCacheService = new TokenCacheService(redisTemplate);
    }

    @Test
    void testSaveTokens() {
        // Given
        String sessionId = "session_123";
        String accessToken = "access-token-123";
        String encryptedRefreshToken = "encrypted-refresh-token-123";

        // When
        tokenCacheService.saveTokens(sessionId, accessToken, encryptedRefreshToken);

        // Then
        verify(valueOperations).set(eq("access_token:session_123"), eq(accessToken), any());
        verify(valueOperations).set(eq("refresh_token:session_123"), eq(encryptedRefreshToken), any());
        verify(valueOperations).set(eq("session:session_123"), eq("active"), any());
    }

    @Test
    void testGetAccessToken() {
        // Given
        String sessionId = "session_123";
        String expectedToken = "access-token-123";
        when(valueOperations.get("access_token:session_123")).thenReturn(expectedToken);

        // When
        String actualToken = tokenCacheService.getAccessToken(sessionId);

        // Then
        assertEquals(expectedToken, actualToken);
    }

    @Test
    void testGetEncryptedRefreshToken() {
        // Given
        String sessionId = "session_123";
        String expectedToken = "encrypted-refresh-token-123";
        when(valueOperations.get("refresh_token:session_123")).thenReturn(expectedToken);

        // When
        String actualToken = tokenCacheService.getEncryptedRefreshToken(sessionId);

        // Then
        assertEquals(expectedToken, actualToken);
    }

    @Test
    void testHasSession_Exists() {
        // Given
        String sessionId = "session_123";
        when(redisTemplate.hasKey("session:session_123")).thenReturn(true);

        // When
        boolean result = tokenCacheService.hasSession(sessionId);

        // Then
        assertTrue(result);
    }

    @Test
    void testHasSession_NotExists() {
        // Given
        String sessionId = "session_123";
        when(redisTemplate.hasKey("session:session_123")).thenReturn(false);

        // When
        boolean result = tokenCacheService.hasSession(sessionId);

        // Then
        assertFalse(result);
    }

    @Test
    void testDeleteSession() {
        // Given
        String sessionId = "session_123";

        // When
        tokenCacheService.deleteSession(sessionId);

        // Then
        verify(redisTemplate).delete("access_token:session_123");
        verify(redisTemplate).delete("refresh_token:session_123");
        verify(redisTemplate).delete("session:session_123");
    }

    @Test
    void testUpdateAccessToken() {
        // Given
        String sessionId = "session_123";
        String newAccessToken = "new-access-token-456";

        // When
        tokenCacheService.updateAccessToken(sessionId, newAccessToken);

        // Then
        verify(valueOperations).set(eq("access_token:session_123"), eq(newAccessToken), any());
    }
}