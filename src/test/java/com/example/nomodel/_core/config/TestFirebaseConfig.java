package com.example.nomodel._core.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.GenericContainer;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 통합 테스트용 Firebase 설정
 * Firebase Emulator Suite와 연동하여 실제 Firebase API를 로컬에서 에뮬레이션
 */
@TestConfiguration
@Profile({"test", "itest"})
public class TestFirebaseConfig {

    @Autowired(required = false)
    private GenericContainer<?> firebaseEmulatorContainer;

    @Bean("testFirebaseApp")
    public FirebaseApp testFirebaseApp() throws IOException {
        // 이미 초기화된 Firebase 앱이 있는지 확인
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        // 통합 테스트에서는 Firebase Emulator 사용
        if (firebaseEmulatorContainer != null) {
            return createFirebaseAppWithEmulator();
        } else {
            // 단위 테스트에서는 더미 앱 사용
            return createDummyFirebaseApp();
        }
    }

    private FirebaseApp createFirebaseAppWithEmulator() throws IOException {
        // Firebase Emulator 설정
        String authEmulatorHost = firebaseEmulatorContainer.getHost() + ":" +
                                  firebaseEmulatorContainer.getMappedPort(9099);
        String firestoreEmulatorHost = firebaseEmulatorContainer.getHost() + ":" +
                                       firebaseEmulatorContainer.getMappedPort(8080);

        // 환경 변수로 에뮬레이터 설정
        System.setProperty("FIREBASE_AUTH_EMULATOR_HOST", authEmulatorHost);
        System.setProperty("FIRESTORE_EMULATOR_HOST", firestoreEmulatorHost);

        // 에뮬레이터 전용 Mock Credentials 사용
        // 에뮬레이터에서는 실제 인증이 필요없으므로 빈 credentials 사용
        GoogleCredentials mockCredentials = GoogleCredentials.create(null);

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(mockCredentials)
                .setProjectId("demo-test-project")
                .build();

        return FirebaseApp.initializeApp(options, "integration-test-app");
    }

    private FirebaseApp createDummyFirebaseApp() {
        FirebaseOptions options = FirebaseOptions.builder()
                .setProjectId("test-project")
                .setServiceAccountId("test@test-project.iam.gserviceaccount.com")
                .build();

        return FirebaseApp.initializeApp(options, "test-app");
    }
}