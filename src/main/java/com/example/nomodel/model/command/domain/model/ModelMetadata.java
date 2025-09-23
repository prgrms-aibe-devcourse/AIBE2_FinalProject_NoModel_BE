package com.example.nomodel.model.command.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ModelMetadata {

    @Column(name = "seed")
    private Long seed;

    @Column(name = "prompt", length = 2000)
    private String prompt;

    @Column(name = "negative_prompt", length = 1000)
    private String negativePrompt;

    @Column(name = "width", nullable = false)
    private Integer width;

    @Column(name = "height", nullable = false)
    private Integer height;

    @Column(name = "steps", nullable = false)
    private Integer steps;

    @Enumerated(EnumType.STRING)
    @Column(name = "sampler_index", nullable = false)
    private SamplerType samplerIndex;

    @Column(name = "n_iter", nullable = false)
    private Integer nIter;

    @Column(name = "batch_size", nullable = false)
    private Integer batchSize;

    public static ModelMetadata of(Long seed, String prompt, String negativePrompt, 
                                   Integer width, Integer height, Integer steps,
                                   SamplerType samplerIndex, Integer nIter, Integer batchSize) {
        return ModelMetadata.builder()
                .seed(seed)
                .prompt(prompt)
                .negativePrompt(negativePrompt)
                .width(width)
                .height(height)
                .steps(steps)
                .samplerIndex(samplerIndex)
                .nIter(nIter)
                .batchSize(batchSize)
                .build();
    }

    public ModelMetadata updatePrompt(String prompt, String negativePrompt) {
        return ModelMetadata.builder()
                .seed(this.seed)
                .prompt(prompt)
                .negativePrompt(negativePrompt)
                .width(this.width)
                .height(this.height)
                .steps(this.steps)
                .samplerIndex(this.samplerIndex)
                .nIter(this.nIter)
                .batchSize(this.batchSize)
                .build();
    }

    public ModelMetadata updateDimensions(Integer width, Integer height) {
        return ModelMetadata.builder()
                .seed(this.seed)
                .prompt(this.prompt)
                .negativePrompt(this.negativePrompt)
                .width(width)
                .height(height)
                .steps(this.steps)
                .samplerIndex(this.samplerIndex)
                .nIter(this.nIter)
                .batchSize(this.batchSize)
                .build();
    }

    public ModelMetadata updateSamplingSettings(Integer steps, SamplerType samplerIndex, Integer nIter, Integer batchSize) {
        return ModelMetadata.builder()
                .seed(this.seed)
                .prompt(this.prompt)
                .negativePrompt(this.negativePrompt)
                .width(this.width)
                .height(this.height)
                .steps(steps)
                .samplerIndex(samplerIndex)
                .nIter(nIter)
                .batchSize(batchSize)
                .build();
    }

    /**
     * 고해상도 이미지인지 확인 (1920x1080 이상)
     * @return 고해상도 여부
     */
    public boolean isHighResolution() {
        return this.width != null && this.height != null && 
               (this.width * this.height) >= (1920 * 1080);
    }

    /**
     * 배치 처리 총 이미지 수
     * @return 총 생성될 이미지 수
     */
    public Integer getTotalImages() {
        return this.nIter * this.batchSize;
    }

    /**
     * 랜덤 시드인지 확인 (-1이면 랜덤)
     * @return 랜덤 시드 여부
     */
    public boolean isRandomSeed() {
        return this.seed == null || this.seed == -1L;
    }

    /**
     * 고정 시드인지 확인 (특정 시드 번호)
     * @return 고정 시드 여부
     */
    public boolean isFixedSeed() {
        return !isRandomSeed();
    }

    /**
     * 시드값 업데이트 (랜덤 시드로 설정하려면 -1 사용)
     * @param seed 시드값 (-1: 랜덤, 양수: 고정 시드)
     * @return 업데이트된 ModelMetadata
     */
    public ModelMetadata updateSeed(Long seed) {
        return ModelMetadata.builder()
                .seed(seed)
                .prompt(this.prompt)
                .negativePrompt(this.negativePrompt)
                .width(this.width)
                .height(this.height)
                .steps(this.steps)
                .samplerIndex(this.samplerIndex)
                .nIter(this.nIter)
                .batchSize(this.batchSize)
                .build();
    }
}