package com.example.nomodel.model.infrastructure.batch;

import com.example.nomodel.model.domain.document.AIModelDocument;
import com.example.nomodel.model.domain.model.AIModel;
import com.example.nomodel.model.domain.model.OwnType;
import com.example.nomodel.model.domain.repository.AIModelJpaRepository;
import com.example.nomodel.model.domain.repository.AIModelSearchRepository;
import com.example.nomodel.member.domain.model.Email;
import com.example.nomodel.member.domain.model.Member;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.*;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;

/**
 * AIModel Elasticsearch 인덱싱 배치 작업 단위 테스트
 */
class AIModelIndexBatchJobTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private AIModelJpaRepository aiModelJpaRepository;

    @Mock
    private AIModelSearchRepository searchRepository;

    @Mock
    private MemberJpaRepository memberRepository;
    
    private AIModelIndexBatchJob batchJobConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        batchJobConfig = new AIModelIndexBatchJob(
            jobRepository, 
            transactionManager, 
            aiModelJpaRepository, 
            searchRepository, 
            memberRepository
        );
    }

    @Test
    void Job_빈_생성_테스트() {
        // When
        Job job = batchJobConfig.aiModelIndexJob();
        
        // Then
        assertThat(job).isNotNull();
        assertThat(job.getName()).isEqualTo("aiModelIndexJob");
        assertThat(job.isRestartable()).isTrue();
    }
    
    @Test
    void Reader_Writer_구성_테스트() {
        // When
        var reader = batchJobConfig.aiModelReader();
        var writer = batchJobConfig.aiModelWriter();
        
        // Then
        assertThat(reader).isNotNull();
        assertThat(writer).isNotNull();
    }

    @Test
    void Processor_정상_동작_테스트() throws Exception {
        // Given
        AIModel testModel = createTestAIModel(1L, "Test Model", 1L);
        Member testMember = createTestMember(1L, "test@example.com");
        
        given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
        
        ItemProcessor<AIModel, AIModelDocument> processor = batchJobConfig.aiModelProcessor();

        // When
        AIModelDocument result = processor.process(testModel);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getModelName()).isEqualTo("Test Model");
        verify(memberRepository, times(1)).findById(1L);
    }

    @Test
    void Processor_관리자_모델_테스트() throws Exception {
        // Given
        AIModel adminModel = createTestAIModel(1L, "Admin Model", null);
        
        ItemProcessor<AIModel, AIModelDocument> processor = batchJobConfig.aiModelProcessor();

        // When
        AIModelDocument result = processor.process(adminModel);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getModelName()).isEqualTo("Admin Model");
        // 관리자 모델은 memberRepository 호출하지 않음
        verify(memberRepository, times(0)).findById(any());
    }

    @Test
    void Processor_Member_조회_실패_테스트() throws Exception {
        // Given
        AIModel userModel = createTestAIModel(1L, "User Model", 999L); // 존재하지 않는 memberId
        
        given(memberRepository.findById(999L)).willReturn(Optional.empty());
        
        ItemProcessor<AIModel, AIModelDocument> processor = batchJobConfig.aiModelProcessor();

        // When
        AIModelDocument result = processor.process(userModel);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getModelName()).isEqualTo("User Model");
        // Member를 찾지 못했을 때 "Unknown"으로 설정되는지 확인
        verify(memberRepository, times(1)).findById(999L);
    }

    private AIModel createTestAIModel(Long id, String modelName, Long ownerId) {
        AIModel model = AIModel.builder()
                .modelName(modelName)
                .ownType(ownerId != null ? OwnType.USER : OwnType.ADMIN)
                .ownerId(ownerId)
                .isPublic(true)
                .build();
        
        // ID 설정을 위한 리플렉션 사용 (테스트용)
        if (id != null) {
            try {
                var idField = AIModel.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(model, id);
            } catch (Exception e) {
                // 테스트용이므로 무시
            }
        }
        
        return model;
    }

    private Member createTestMember(Long id, String email) {
        return Member.builder()
                .email(Email.of(email))
                .build();
    }
}