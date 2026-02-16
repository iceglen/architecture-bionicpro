package ru.artem.papyan.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import ru.artem.papyan.auth.dto.TokenResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private KeycloakService keycloakService;

    @BeforeEach
    void setUp() {
        keycloakService = new KeycloakService(restTemplate, objectMapper);
        
        // Use reflection to set private fields
        try {
            var authServerUrlField = KeycloakService.class.getDeclaredField("authServerUrl");
            authServerUrlField.setAccessible(true);
            authServerUrlField.set(keycloakService, "http://localhost:8080");
            
            var realmField = KeycloakService.class.getDeclaredField("realm");
            realmField.setAccessible(true);
            realmField.set(keycloakService, "reports-realm");
            
            var clientIdField = KeycloakService.class.getDeclaredField("clientId");
            clientIdField.setAccessible(true);
            clientIdField.set(keycloakService, "auth-api-client");
            
            var clientSecretField = KeycloakService.class.getDeclaredField("clientSecret");
            clientSecretField.setAccessible(true);
            clientSecretField.set(keycloakService, "auth-api-secret-1234567890");
        } catch (Exception e) {
            fail("Failed to set private fields for testing");
        }
    }

    @Test
    void testGetTokensByAuthorizationCode() {
        // Given
        String code = "auth-code-123";
        String redirectUri = "http://localhost:8081/login/oauth2/code/keycloak";
        
        TokenResponse expectedResponse = new TokenResponse(
            "access-token-123", 
            "refresh-token-123", 
            "Bearer", 
            120, 
            1800, 
            "openid profile email"
        );
        
        when(restTemplate.postForEntity(anyString(), any(), eq(TokenResponse.class)))
            .thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        // When
        TokenResponse actualResponse = keycloakService.getTokensByAuthorizationCode(code, redirectUri);

        // Then
        assertNotNull(actualResponse);
        assertEquals("access-token-123", actualResponse.getAccessToken());
        assertEquals("refresh-token-123", actualResponse.getRefreshToken());
        assertEquals("Bearer", actualResponse.getTokenType());
        assertEquals(120, actualResponse.getExpiresIn());
        assertEquals(1800, actualResponse.getRefreshExpiresIn());
        assertEquals("openid profile email", actualResponse.getScope());
    }

    @Test
    void testRefreshTokens() {
        // Given
        String refreshToken = "refresh-token-123";
        
        TokenResponse expectedResponse = new TokenResponse(
            "new-access-token-456", 
            "new-refresh-token-456", 
            "Bearer", 
            120, 
            1800, 
            "openid profile email"
        );
        
        when(restTemplate.postForEntity(anyString(), any(), eq(TokenResponse.class)))
            .thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        // When
        TokenResponse actualResponse = keycloakService.refreshTokens(refreshToken);

        // Then
        assertNotNull(actualResponse);
        assertEquals("new-access-token-456", actualResponse.getAccessToken());
        assertEquals("new-refresh-token-456", actualResponse.getRefreshToken());
    }

    @Test
    void testValidateToken_Valid() throws Exception {
        // Given
        String token = "access-token-123";
        String introspectionResponse = "{\"active\": true}";
        
        when(objectMapper.readTree(introspectionResponse))
            .thenReturn(objectMapper.createObjectNode().put("active", true));
        
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
            .thenReturn(new ResponseEntity<>(introspectionResponse, HttpStatus.OK));

        // When
        boolean result = keycloakService.validateToken(token);

        // Then
        assertTrue(result);
    }

    @Test
    void testValidateToken_Invalid() throws Exception {
        // Given
        String token = "invalid-token-123";
        String introspectionResponse = "{\"active\": false}";
        
        when(objectMapper.readTree(introspectionResponse))
            .thenReturn(objectMapper.createObjectNode().put("active", false));
        
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
            .thenReturn(new ResponseEntity<>(introspectionResponse, HttpStatus.OK));

        // When
        boolean result = keycloakService.validateToken(token);

        // Then
        assertFalse(result);
    }
}