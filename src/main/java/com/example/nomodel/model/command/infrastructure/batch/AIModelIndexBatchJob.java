package com.example.nomodel.model.command.infrastructure.batch;

import com.example.nomodel.model.command.domain.model.document.AIModelDocument;
import com.example.nomodel.model.command.domain.model.AIModel;
import com.example.nomodel.model.command.domain.model.ModelStatistics;
import com.example.nomodel.model.command.application.dto.ModelIndexProjection;
import com.example.nomodel.model.command.domain.repository.AIModelJpaRepository;
import com.example.nomodel.review.domain.model.ReviewStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
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
    private final ElasticsearchOperations elasticsearchOperations;

    private static final int CHUNK_SIZE = 200;
    private static final ReviewStatus REVIEW_STATUS = ReviewStatus.ACTIVE;

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
                .<ModelIndexProjection, AIModelDocument>chunk(CHUNK_SIZE, transactionManager)
                .reader(aiModelReader(null)) // StepScope 프록시 주입
                .processor(aiModelProcessor())
                .writer(aiModelWriter())
                .taskExecutor(aiModelIndexTaskExecutor())
                .throttleLimit(4)
                .build();
    }

    /**
     * MySQL 에서 증분 처리로 AIModel을 읽는 Reader
     * JobParameter로 전달된 fromDateTime 이후 수정된 모델 처리
     * (BaseTimeEntity 특성상 생성 시 updatedAt도 설정되므로 새 모델도 포함)
     */
    @Bean
    @StepScope
    public RepositoryItemReader<ModelIndexProjection> aiModelReader(
            @Value("#{jobParameters['fromDateTime']}") LocalDateTime fromDateTime) {
        
        // fromDateTime이 없으면 최근 5분 기본값 사용 (스케줄러와 일치)
        LocalDateTime actualFromDateTime = fromDateTime != null ? 
                fromDateTime : LocalDateTime.now().minusMinutes(5);
        
        log.info("배치 Reader 초기화 - 증분 처리 시작 시간: {} (updatedAt 기준)", actualFromDateTime);
        
        return new RepositoryItemReaderBuilder<ModelIndexProjection>()
                .name("aiModelIncrementalReader")
                .repository(aiModelRepository)
                .methodName("findModelIndexesUpdatedAfter")
                .arguments(actualFromDateTime, REVIEW_STATUS)
                .sorts(Map.of("updatedAt", Sort.Direction.ASC)) // 정렬은 JPQL에서 처리
                .pageSize(CHUNK_SIZE)
                .build();
    }

    /**
     * AIModel을 AIModelDocument로 변환하는 Processor
     * 모든 통계 데이터(사용량, 조회수, 평점, 리뷰수)를 포함하여 완전한 문서 생성
     */
    @Bean
    public ItemProcessor<ModelIndexProjection, AIModelDocument> aiModelProcessor() {
        return projection -> {
            try {
                AIModel model = projection.getModel();
                ModelStatistics statistics = projection.getStatistics();

                Long usageCount = statistics != null ? statistics.getUsageCount() : 0L;
                Long viewCount = statistics != null ? statistics.getViewCount() : 0L;
                Double rating = projection.getAverageRating() != null ? projection.getAverageRating() : 0.0;
                Long reviewCount = projection.getReviewCount() != null ? projection.getReviewCount() : 0L;

                log.debug("배치 처리 - modelId: {}, usageCount: {}, viewCount: {}, rating: {}, reviewCount: {}",
                        model.getId(), usageCount, viewCount, rating, reviewCount);

                return AIModelDocument.from(
                        model, projection.getOwnerName(), usageCount, viewCount, rating, reviewCount);

            } catch (Exception e) {
                log.error("AIModel 변환 실패: modelId={}", projection.getModel().getId(), e);
                return null;
            }
        };
    }

    /**
     * AIModelDocument를 Elasticsearch에 저장하는 Writer
     */
    @Bean
    public ItemWriter<AIModelDocument> aiModelWriter() {
        return items -> {
            if (items.isEmpty()) {
                return;
            }
            elasticsearchOperations.save(items);
        };
    }

    @Bean
    public TaskExecutor aiModelIndexTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("ai-model-index-");
        executor.setConcurrencyLimit(4);
        return executor;
    }
}
