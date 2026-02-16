package ru.artem.papyan.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.crypto.codec.Hex;
import java.nio.charset.StandardCharsets;

@Configuration
public class AppConfig {
    
    @Value("${auth.session.encryption.password}")
    private String encryptionPassword;
    
    @Value("${auth.session.encryption.salt}")
    private String encryptionSalt;
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
    
    @Bean
    public TextEncryptor textEncryptor() {
        // Для тестирования используем no-op encryptor, чтобы проверить, запускается ли приложение
        // В продакшене нужно заменить на реальный шифратор
        return new TextEncryptor() {
            @Override
            public String encrypt(String text) {
                return text; // no-op для тестирования
            }
            
            @Override
            public String decrypt(String encryptedText) {
                return encryptedText; // no-op для тестирования
            }
        };
    }
}
