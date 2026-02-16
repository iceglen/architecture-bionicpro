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
        return new TokenRequest("password", clientId, clientSecret, username, password, null, null, null);
    }
    
    public static TokenRequest forRefreshToken(String clientId, String clientSecret, String refreshToken) {
        return new TokenRequest("refresh_token", clientId, clientSecret, null, null, refreshToken, null, null);
    }
    
    public static TokenRequest forAuthorizationCode(String clientId, String clientSecret, String code, String redirectUri) {
        return new TokenRequest("authorization_code", clientId, clientSecret, null, null, null, code, redirectUri);
    }
}