package com.example.nomodel._core.config;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FireBaseConfig {

    @Value("${firebase.config.key-path}")
    private Resource firebaseKeyResource;

    @Bean
    public FirebaseApp firebaseApp() {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                if (!firebaseKeyResource.exists()) {
                    throw new ApplicationException(ErrorCode.FIREBASE_KEY_FILE_NOT_FOUND);
                }

                try (InputStream keyInputStream = firebaseKeyResource.getInputStream()) {
                    FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(keyInputStream));

                    FirebaseOptions options = optionsBuilder.build();
                    return FirebaseApp.initializeApp(options);
                }
            } catch (IOException e) {
                throw new ApplicationException(ErrorCode.FIREBASE_KEY_FILE_READ_ERROR);
            } catch (Exception e) {
                throw new ApplicationException(ErrorCode.FIREBASE_INITIALIZATION_FAILED);
            }
        }

        return FirebaseApp.getInstance();
    }
}