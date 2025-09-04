package com.example.nomodel.model.infrastructure.batch;

import com.example.nomodel.member.domain.model.Email;
import com.example.nomodel.model.application.service.AIModelSearchService;
import com.example.nomodel.model.domain.document.AIModelDocument;
import com.example.nomodel.model.domain.model.AIModel;
import com.example.nomodel.model.domain.repository.AIModelJpaRepository;
import com.example.nomodel.model.domain.repository.AIModelSearchRepository;
import com.example.nomodel.member.domain.model.Member;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AIModel Elasticsearch 인덱싱 배치 작업
 * 주기적으로 MySQL의 AIModel 데이터를 Elasticsearch에 동기화
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AIModelIndexBatchJob {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final AIModelJpaRepository aiModelRepository;
    private final AIModelSearchRepository searchRepository;
    private final MemberJpaRepository memberRepository;

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
     * AIModel 인덱싱 Step 정의
     */
    @Bean
    public Step aiModelIndexStep() {
        return new StepBuilder("aiModelIndexStep", jobRepository)
                .<AIModel, AIModelDocument>chunk(50, transactionManager)
                .reader(aiModelReader())
                .processor(aiModelProcessor())
                .writer(aiModelWriter())
                .build();
    }

    /**
     * MySQL에서 AIModel을 읽는 Reader
     * 최근 업데이트된 모델부터 처리
     */
    @Bean
    public RepositoryItemReader<AIModel> aiModelReader() {
        return new RepositoryItemReaderBuilder<AIModel>()
                .name("aiModelReader")
                .repository(aiModelRepository)
                .methodName("findAll")
                .sorts(Map.of("updatedAt", Sort.Direction.DESC))
                .pageSize(50)
                .build();
    }

    /**
     * AIModel을 AIModelDocument로 변환하는 Processor
     */
    @Bean
    public ItemProcessor<AIModel, AIModelDocument> aiModelProcessor() {
        return aiModel -> {
            try {
                String ownerName = getOwnerName(aiModel);
                return AIModelDocument.from(aiModel, ownerName);
                
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
}