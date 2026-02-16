package ru.artem.papyan.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequest {
    private String grantType;
    private String clientId;
    private String clientSecret;
    private String username;
    private String password;
    private String refreshToken;
    private String code;
    private String redirectUri;
    
    public static TokenRequest forPassword(String clientId, String clientSecret, String username, String password) {
        TokenRequest request = new TokenRequest();
        request.setGrantType("password");
        request.setClientId(clientId);
        request.setClientSecret(clientSecret);
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }
    
    public static TokenRequest forRefreshToken(String clientId, String clientSecret, String refreshToken) {
        TokenRequest request = new TokenRequest();
        request.setGrantType("refresh_token");
        request.setClientId(clientId);
        request.setClientSecret(clientSecret);
        request.setRefreshToken(refreshToken);
        return request;
    }
    
    public static TokenRequest forAuthorizationCode(String clientId, String clientSecret, String code, String redirectUri) {
        TokenRequest request = new TokenRequest();
        request.setGrantType("authorization_code");
        request.setClientId(clientId);
        request.setClientSecret(clientSecret);
        request.setCode(code);
        request.setRedirectUri(redirectUri);
        return request;
    }
}
