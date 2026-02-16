package ru.artem.papyan.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import ru.artem.papyan.auth.dto.TokenRequest;
import ru.artem.papyan.auth.dto.TokenResponse;

import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;
    
    @Value("${keycloak.realm}")
    private String realm;
    
    @Value("${keycloak.resource}")
    private String clientId;
    
    @Value("${keycloak.credentials.secret}")
    private String clientSecret;
    
    public TokenResponse getTokensByAuthorizationCode(String code, String redirectUri) {
        log.debug("Getting tokens by authorization code for client: {}", clientId);
        
        TokenRequest request = TokenRequest.forAuthorizationCode(clientId, clientSecret, code, redirectUri);
        return exchangeTokens(request);
    }
    
    public TokenResponse getTokensByPassword(String username, String password) {
        log.debug("Getting tokens by password for user: {}", username);
        
        TokenRequest request = TokenRequest.forPassword(clientId, clientSecret, username, password);
        return exchangeTokens(request);
    }
    
    public TokenResponse refreshTokens(String refreshToken) {
        log.debug("Refreshing tokens using refresh token");
        
        TokenRequest request = TokenRequest.forRefreshToken(clientId, clientSecret, refreshToken);
        return exchangeTokens(request);
    }
    
    public boolean validateToken(String token) {
        try {
            String introspectionUrl = String.format("%s/realms/%s/protocol/openid-connect/token/introspect", 
                authServerUrl, realm);
            
            HttpHeaders headers = createBasicAuthHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("token", token);
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(introspectionUrl, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                var jsonNode = objectMapper.readTree(response.getBody());
                return jsonNode.has("active") && jsonNode.get("active").asBoolean();
            }
            
            return false;
        } catch (Exception e) {
            log.error("Error validating token", e);
            return false;
        }
    }
    
    private TokenResponse exchangeTokens(TokenRequest request) {
        try {
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", 
                authServerUrl, realm);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", request.getGrantType());
            body.add("client_id", request.getClientId());
            body.add("client_secret", request.getClientSecret());
            
            if (request.getUsername() != null) {
                body.add("username", request.getUsername());
                body.add("password", request.getPassword());
            }
            
            if (request.getRefreshToken() != null) {
                body.add("refresh_token", request.getRefreshToken());
            }
            
            if (request.getCode() != null) {
                body.add("code", request.getCode());
                body.add("redirect_uri", request.getRedirectUri());
            }
            
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(tokenUrl, entity, TokenResponse.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.debug("Successfully obtained tokens for grant type: {}", request.getGrantType());
                return response.getBody();
            } else {
                log.error("Failed to get tokens. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to obtain tokens from Keycloak");
            }
        } catch (Exception e) {
            log.error("Error exchanging tokens for grant type: {}", request.getGrantType(), e);
            throw new RuntimeException("Failed to exchange tokens", e);
        }
    }
    
    private HttpHeaders createBasicAuthHeaders() {
        String credentials = clientId + ":" + clientSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedCredentials);
        return headers;
    }
}