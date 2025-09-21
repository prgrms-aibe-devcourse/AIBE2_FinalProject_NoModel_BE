package com.example.nomodel.generate.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stable Diffusion API 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StableDiffusionRequest {
    
    private String prompt;
    
    @JsonProperty("negative_prompt")
    private String negativePrompt;
    
    private int width;
    private int height;
    private int steps;
    
    @JsonProperty("cfg_scale")
    private double cfgScale;
    
    @JsonProperty("sampler_index")
    private String samplerIndex;
    
    @JsonProperty("restore_faces")
    private boolean restoreFaces;
    
    private boolean tiling;
    
    @JsonProperty("n_iter")
    private int nIter;
    
    @JsonProperty("batch_size")
    private int batchSize;
    
    private long seed;
    private long subseed;
    
    @JsonProperty("subseed_strength")
    private int subseedStrength;
    
    @JsonProperty("seed_resize_from_h")
    private int seedResizeFromH;
    
    @JsonProperty("seed_resize_from_w")
    private int seedResizeFromW;
    
    @JsonProperty("enable_hr")
    private boolean enableHr;
    
    @JsonProperty("save_images")
    private boolean saveImages;
    
    @JsonProperty("send_images")
    private boolean sendImages;
    
    @JsonProperty("do_not_save_samples")
    private boolean doNotSaveSamples;
    
    @JsonProperty("do_not_save_grid")
    private boolean doNotSaveGrid;
}