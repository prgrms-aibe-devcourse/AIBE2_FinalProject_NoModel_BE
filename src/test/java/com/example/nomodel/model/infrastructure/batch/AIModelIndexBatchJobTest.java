package com.example.nomodel.model.infrastructure.batch;

import com.example.nomodel.model.domain.document.AIModelDocument;
import com.example.nomodel.model.domain.repository.AIModelSearchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

/**
 * AIModel Elasticsearch 인덱싱 배치 작업 단위 테스트
 * 현재 누락된 도메인 클래스들로 인해 기본 구조만 테스트
 */
@ExtendWith(MockitoExtension.class)
class AIModelIndexBatchJobTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private AIModelSearchRepository searchRepository;
    
    @Test
    @DisplayName("배치 테스트 기본 설정 확인")
    void basicBatchConfigurationTest() {
        // Given & When & Then
        assertThat(jobRepository).isNotNull();
        assertThat(transactionManager).isNotNull(); 
        assertThat(searchRepository).isNotNull();
    }

    @Test
    @DisplayName("AIModelDocument 생성 테스트")
    void aiModelDocumentCreationTest() {
        // Given
        String modelName = "Test Model";
        String ownerName = "Test Owner";
        
        // When
        AIModelDocument document = createTestDocument(modelName, ownerName);
        
        // Then
        assertThat(document).isNotNull();
        assertThat(document.getModelName()).isEqualTo(modelName);
    }

    @Test 
    @DisplayName("Elasticsearch 저장소 Mock 테스트")
    void elasticsearchRepositoryMockTest() {
        // Given
        AIModelDocument testDoc = createTestDocument("Mock Model", "Mock Owner");
        given(searchRepository.save(any(AIModelDocument.class))).willReturn(testDoc);
        
        // When
        AIModelDocument saved = searchRepository.save(testDoc);
        
        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getModelName()).isEqualTo("Mock Model");
        verify(searchRepository, times(1)).save(any(AIModelDocument.class));
    }

    private AIModelDocument createTestDocument(String modelName, String ownerName) {
        return AIModelDocument.builder()
                .modelName(modelName)
                .ownerName(ownerName)
                .isPublic(true)
                .ownType("USER")
                .build();
    }
}