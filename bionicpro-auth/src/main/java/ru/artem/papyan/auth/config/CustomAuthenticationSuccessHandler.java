package ru.artem.papyan.auth.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import ru.artem.papyan.auth.dto.TokenResponse;
import ru.artem.papyan.auth.service.KeycloakService;
import ru.artem.papyan.auth.service.SessionService;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final KeycloakService keycloakService;
    private final SessionService sessionService;
    
    private static final String SESSION_COOKIE_NAME = "AUTH_SESSION_ID";
    private static final String FRONTEND_URL = "http://localhost:3000";
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                       HttpServletResponse response, 
                                       Authentication authentication) throws IOException, ServletException {
        
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            try {
                // Получаем OAuth2AuthorizedClient для получения access token
                OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                    oauthToken.getAuthorizedClientRegistrationId(), 
                    oauthToken.getName()
                );
                
                if (authorizedClient == null) {
                    log.error("No authorized client found for authentication: {}", authentication.getName());
                    redirectToFrontendWithError(response, "Authentication failed");
                    return;
                }
                
                OAuth2AccessToken oauth2AccessToken = authorizedClient.getAccessToken();
                if (oauth2AccessToken == null) {
                    log.error("No access token found in authorized client");
                    redirectToFrontendWithError(response, "No access token received");
                    return;
                }
                
                // Для Keycloak нам нужен refresh token, который не доступен через OAuth2AuthorizedClient
                // Поэтому используем authorization code из request parameters
                String code = request.getParameter("code");
                if (code == null) {
                    log.error("No authorization code found in request");
                    redirectToFrontendWithError(response, "No authorization code");
                    return;
                }
                
                String redirectUri = getRedirectUri(request);
                log.debug("Exchanging authorization code for tokens with redirectUri: {}", redirectUri);
                
                // Обмениваем authorization code на токены через KeycloakService
                TokenResponse tokenResponse = keycloakService.getTokensByAuthorizationCode(code, redirectUri);
                
                // Создаем сессию
                String sessionId = sessionService.createSession(tokenResponse);
                
                // Устанавливаем session cookie
                setSessionCookie(response, sessionId);
                
                // Перенаправляем на фронтенд
                redirectToFrontend(response);
                
                log.info("Authentication successful for user: {}, session: {}", 
                        authentication.getName(), sessionId);
                        
            } catch (Exception e) {
                log.error("Error processing authentication success", e);
                redirectToFrontendWithError(response, "Authentication processing failed");
            }
        } else {
            log.error("Unsupported authentication type: {}", authentication.getClass());
            redirectToFrontendWithError(response, "Unsupported authentication type");
        }
    }
    
    private String getRedirectUri(HttpServletRequest request) {
        // Строим redirect URI для callback
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();
        
        String redirectUri = String.format("%s://%s", scheme, serverName);
        if (serverPort != 80 && serverPort != 443) {
            redirectUri += ":" + serverPort;
        }
        redirectUri += contextPath + "/login/oauth2/code/keycloak";
        
        return redirectUri;
    }
    
    private void setSessionCookie(HttpServletResponse response, String sessionId) {
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, sessionId);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // false для localhost, true для production
        cookie.setPath("/");
        cookie.setMaxAge(30 * 60); // 30 минут
        response.addCookie(cookie);
        
        log.debug("Session cookie set: {}", sessionId);
    }
    
    private void redirectToFrontend(HttpServletResponse response) throws IOException {
        response.sendRedirect(FRONTEND_URL);
    }
    
    private void redirectToFrontendWithError(HttpServletResponse response, String error) throws IOException {
        response.sendRedirect(FRONTEND_URL + "?error=" + error);
    }
}