package ru.artem.papyan.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import ru.artem.papyan.auth.dto.TokenResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private TextEncryptor textEncryptor;

    @Mock
    private TokenCacheService tokenCacheService;

    @Mock
    private KeycloakService keycloakService;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(textEncryptor, tokenCacheService, keycloakService);
        // Use reflection to set private fields
        try {
            var rotationEnabledField = SessionService.class.getDeclaredField("rotationEnabled");
            rotationEnabledField.setAccessible(true);
            rotationEnabledField.set(sessionService, true);
            
            var rotationIntervalField = SessionService.class.getDeclaredField("rotationInterval");
            rotationIntervalField.setAccessible(true);
            rotationIntervalField.set(sessionService, 3);
        } catch (Exception e) {
            fail("Failed to set private fields for testing");
        }
    }

    @Test
    void testCreateSession() {
        // Given
        TokenResponse tokenResponse = new TokenResponse("access-token", "refresh-token", "Bearer", 120, 1800, "openid");

        when(textEncryptor.encrypt("refresh-token")).thenReturn("encrypted-refresh-token");

        // When
        String sessionId = sessionService.createSession(tokenResponse);

        // Then
        assertNotNull(sessionId);
        assertTrue(sessionId.startsWith("session_"));
        verify(tokenCacheService).saveTokens(eq(sessionId), eq("access-token"), eq("encrypted-refresh-token"));
    }

    @Test
    void testValidateSession_ValidSession() {
        // Given
        String sessionId = "session_123";
        when(tokenCacheService.hasSession(sessionId)).thenReturn(true);
        when(tokenCacheService.getAccessToken(sessionId)).thenReturn("valid-access-token");
        when(keycloakService.validateToken("valid-access-token")).thenReturn(true);

        // When
        boolean result = sessionService.validateSession(sessionId);

        // Then
        assertTrue(result);
    }

    @Test
    void testValidateSession_InvalidSession() {
        // Given
        String sessionId = "session_123";
        when(tokenCacheService.hasSession(sessionId)).thenReturn(false);

        // When
        boolean result = sessionService.validateSession(sessionId);

        // Then
        assertFalse(result);
    }

    @Test
    void testRefreshTokens_Success() {
        // Given
        String sessionId = "session_123";
        when(tokenCacheService.getEncryptedRefreshToken(sessionId)).thenReturn("encrypted-refresh-token");
        when(textEncryptor.decrypt("encrypted-refresh-token")).thenReturn("refresh-token");
        
        TokenResponse newTokens = new TokenResponse("new-access-token", "new-refresh-token", "Bearer", 120, 1800, "openid");
        when(keycloakService.refreshTokens("refresh-token")).thenReturn(newTokens);
        when(textEncryptor.encrypt("new-refresh-token")).thenReturn("encrypted-new-refresh-token");

        // When
        boolean result = sessionService.refreshTokens(sessionId);

        // Then
        assertTrue(result);
        verify(tokenCacheService).saveTokens(eq(sessionId), eq("new-access-token"), eq("encrypted-new-refresh-token"));
    }

    @Test
    void testRefreshTokens_Failure() {
        // Given
        String sessionId = "session_123";
        when(tokenCacheService.getEncryptedRefreshToken(sessionId)).thenReturn(null);

        // When
        boolean result = sessionService.refreshTokens(sessionId);

        // Then
        assertFalse(result);
    }
}