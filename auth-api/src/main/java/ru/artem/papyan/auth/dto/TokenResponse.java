package ru.artem.papyan.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Integer expiresIn;
    private Integer refreshExpiresIn;
    private String scope;
}