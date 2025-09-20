package com.example.nomodel.model.infrastructure.batch;

import com.example.nomodel.member.domain.model.Email;
import com.example.nomodel.model.domain.model.document.AIModelDocument;
import com.example.nomodel.model.domain.model.AIModel;
import com.example.nomodel.model.domain.model.ModelStatistics;
import com.example.nomodel.model.domain.repository.AIModelJpaRepository;
import com.example.nomodel.model.domain.repository.AIModelSearchRepository;
import com.example.nomodel.model.domain.repository.ModelStatisticsJpaRepository;
import com.example.nomodel.member.domain.model.Member;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.review.domain.repository.ReviewRepository;
import com.example.nomodel.review.domain.model.ReviewStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AIModel Elasticsearch 인덱싱 배치 작업
 * 증분 처리를 통해 수정된 데이터만 Elasticsearch에 동기화
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AIModelIndexBatchJob {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final AIModelJpaRepository aiModelRepository;
    private final AIModelSearchRepository searchRepository;
    private final ModelStatisticsJpaRepository modelStatisticsRepository;
    private final MemberJpaRepository memberRepository;
    private final ReviewRepository reviewRepository;

    /**
     * AIModel 인덱싱 Job 정의
     */
    @Bean
    public Job aiModelIndexJob() {
        return new JobBuilder("aiModelIndexJob", jobRepository)
                .start(aiModelIndexStep())
                .build();
    }

    /**
     * AIModel 인덱싱 Step 정의 (증분 처리)
     * Step은 싱글톤으로 두고, 구성 요소만 StepScope로 설정
     */
    @Bean
    public Step aiModelIndexStep() {
        return new StepBuilder("aiModelIndexStep", jobRepository)
                .<AIModel, AIModelDocument>chunk(50, transactionManager)
                .reader(aiModelReader(null)) // StepScope 프록시 주입
                .processor(aiModelProcessor())
                .writer(aiModelWriter())
                .build();
    }

    /**
     * MySQL에서 증분 처리로 AIModel을 읽는 Reader
     * JobParameter로 전달된 fromDateTime 이후 수정된 모델 처리
     * (BaseTimeEntity 특성상 생성 시 updatedAt도 설정되므로 새 모델도 포함)
     */
    @Bean
    @StepScope
    public RepositoryItemReader<AIModel> aiModelReader(
            @Value("#{jobParameters['fromDateTime']}") LocalDateTime fromDateTime) {
        
        // fromDateTime이 없으면 최근 5분 기본값 사용 (스케줄러와 일치)
        LocalDateTime actualFromDateTime = fromDateTime != null ? 
                fromDateTime : LocalDateTime.now().minusMinutes(5);
        
        log.info("배치 Reader 초기화 - 증분 처리 시작 시간: {} (updatedAt 기준)", actualFromDateTime);
        
        return new RepositoryItemReaderBuilder<AIModel>()
                .name("aiModelIncrementalReader")
                .repository(aiModelRepository)
                .methodName("findModelsUpdatedAfterPaged")
                .arguments(actualFromDateTime)
                .sorts(Map.of("updatedAt", Sort.Direction.ASC)) // 정렬은 JPQL에서 처리
                .pageSize(50)
                .build();
    }

    /**
     * AIModel을 AIModelDocument로 변환하는 Processor
     * 모든 통계 데이터(사용량, 조회수, 평점, 리뷰수)를 포함하여 완전한 문서 생성
     */
    @Bean
    public ItemProcessor<AIModel, AIModelDocument> aiModelProcessor() {
        return aiModel -> {
            try {
                String ownerName = getOwnerName(aiModel);
                Long usageCount = getUsageCount(aiModel);
                Long viewCount = getViewCount(aiModel);
                Double rating = getAverageRating(aiModel);
                Long reviewCount = getReviewCount(aiModel);
                
                log.debug("배치 처리 - modelId: {}, usageCount: {}, viewCount: {}, rating: {}, reviewCount: {}", 
                         aiModel.getId(), usageCount, viewCount, rating, reviewCount);
                
                return AIModelDocument.from(
                    aiModel, ownerName, usageCount, viewCount, rating, reviewCount);
                
            } catch (Exception e) {
                log.error("AIModel 변환 실패: modelId={}", aiModel.getId(), e);
                return null;
            }
        };
    }

    /**
     * AIModelDocument를 Elasticsearch에 저장하는 Writer
     */
    @Bean
    public RepositoryItemWriter<AIModelDocument> aiModelWriter() {
        return new RepositoryItemWriterBuilder<AIModelDocument>()
                .repository(searchRepository)
                .methodName("save")
                .build();
    }

    /**
     * 소유자 이름 조회
     */
    private String getOwnerName(AIModel aiModel) {
        if (aiModel.getOwnerId() == null) {
            return aiModel.getOwnType() != null ? aiModel.getOwnType().name() : "ADMIN";
        }
        
        return memberRepository.findById(aiModel.getOwnerId())
                .map(Member::getEmail)
                .map(Email::getValue)
                .orElse("Unknown");
    }

    /**
     * 모델의 사용량 조회
     * ModelStatistics에서 usageCount를 가져오며, 없으면 0 반환
     */
    private Long getUsageCount(AIModel aiModel) {
        return modelStatisticsRepository.findByModelId(aiModel.getId())
                .map(ModelStatistics::getUsageCount)
                .orElse(0L);
    }

    /**
     * 모델의 조회수 조회
     * ModelStatistics에서 viewCount를 가져오며, 없으면 0 반환
     */
    private Long getViewCount(AIModel aiModel) {
        return modelStatisticsRepository.findByModelId(aiModel.getId())
                .map(ModelStatistics::getViewCount)
                .orElse(0L);
    }

    /**
     * 모델의 평점 조회
     * ReviewRepository에서 ACTIVE 상태 리뷰들의 평균 평점 계산
     */
    private Double getAverageRating(AIModel aiModel) {
        return reviewRepository.calculateAverageRatingByModelId(aiModel.getId(), ReviewStatus.ACTIVE);
    }

    /**
     * 모델의 리뷰 수 조회
     * ReviewRepository에서 ACTIVE 상태 리뷰 개수 반환
     */
    private Long getReviewCount(AIModel aiModel) {
        return reviewRepository.countByModelIdAndStatus(aiModel.getId(), ReviewStatus.ACTIVE);
    }
}