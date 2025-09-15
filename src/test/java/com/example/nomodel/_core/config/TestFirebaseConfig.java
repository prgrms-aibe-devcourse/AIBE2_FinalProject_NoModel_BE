package com.example.nomodel._core.config;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 테스트용 Firebase 설정
 * 실제 Firebase 연결 없이 더미 FirebaseApp 인스턴스를 생성
 */
@TestConfiguration
@Profile("test")
public class TestFirebaseConfig {

    @Bean
    @Primary
    public FirebaseApp firebaseApp() {
        // 이미 초기화된 Firebase 앱이 있는지 확인
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        // 테스트용 더미 Firebase 앱 생성
        FirebaseOptions options = FirebaseOptions.builder()
                .setProjectId("test-project")
                .setServiceAccountId("test@test-project.iam.gserviceaccount.com")
                .build();

        return FirebaseApp.initializeApp(options, "test-app");
    }
}