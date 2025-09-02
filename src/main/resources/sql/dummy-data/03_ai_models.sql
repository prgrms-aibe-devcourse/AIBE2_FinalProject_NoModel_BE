-- ================================================================================
-- AI 모델 더미 데이터 (500개)
-- ================================================================================

-- 관리자 모델 20개
INSERT INTO ai_model_tb (
    model_name, own_type, owner_id, price, is_public,
    seed, prompt, negative_prompt, width, height, steps, sampler_index, n_iter, batch_size,
    created_at, updated_at
) VALUES
('Stable Diffusion v1.5', 'ADMIN', 1, 19.99, 1, -1, 'professional image generation, high quality', 'low quality, blurry', 512, 512, 20, 'DDIM', 1, 1, NOW(), NOW()),
('SDXL 1.0', 'ADMIN', 1, 29.99, 1, -1, 'next generation stable diffusion, improved quality', 'poor quality, distorted', 1024, 1024, 25, 'EULER_A', 1, 1, NOW(), NOW()),
('LoRA Fantasy Art', 'ADMIN', 1, 0.00, 1, 12345, 'fantasy art style, magical atmosphere', 'realistic, modern', 768, 768, 30, 'DPM_PLUS_PLUS_2M', 1, 1, NOW(), NOW()),
('Anime Character Generator', 'ADMIN', 1, 15.99, 1, -1, 'anime character, manga style, colorful', 'realistic, 3d render', 512, 768, 28, 'EULER', 1, 2, NOW(), NOW()),
('Photorealistic Portrait', 'ADMIN', 1, 39.99, 1, 67890, 'photorealistic portrait, professional lighting', 'cartoon, anime style', 1024, 1024, 40, 'DPM_PLUS_PLUS_2M_KARRAS', 1, 1, NOW(), NOW()),
('Landscape Master', 'ADMIN', 1, 0.00, 1, -1, 'beautiful landscape, scenic nature', 'urban, indoor', 768, 512, 25, 'HEUN', 1, 1, NOW(), NOW()),
('Abstract Art Creator', 'ADMIN', 1, 12.99, 1, 11111, 'abstract art, modern artistic style', 'realistic, photographic', 512, 512, 35, 'LMS', 1, 1, NOW(), NOW()),
('Cyberpunk Style', 'ADMIN', 1, 25.99, 1, -1, 'cyberpunk aesthetic, futuristic neon', 'medieval, ancient', 768, 768, 32, 'DPM_PLUS_PLUS_SDE', 1, 2, NOW(), NOW()),
('Watercolor Painter', 'ADMIN', 1, 0.00, 1, 22222, 'watercolor painting style, artistic brush', 'digital, sharp edges', 512, 768, 30, 'EULER_A', 1, 1, NOW(), NOW()),
('3D Render Model', 'ADMIN', 1, 49.99, 1, -1, '3d render, octane render, high detail', '2d, flat illustration', 1024, 1024, 50, 'DPM_PLUS_PLUS_2M', 1, 1, NOW(), NOW()),
('Logo Design AI', 'ADMIN', 1, 18.99, 1, 33333, 'logo design, minimalist, professional brand', 'complex, detailed background', 1024, 1024, 25, 'PLMS', 1, 4, NOW(), NOW()),
('Cartoon Style Converter', 'ADMIN', 1, 0.00, 1, -1, 'cartoon style, pixar animation style', 'realistic photography', 512, 512, 20, 'DDIM', 1, 1, NOW(), NOW()),
('Architecture Visualizer', 'ADMIN', 1, 35.99, 1, 44444, 'architectural visualization, modern building', 'natural landscape', 1024, 768, 45, 'DPM_PLUS_PLUS_2M_KARRAS', 1, 1, NOW(), NOW()),
('Fashion Design Studio', 'ADMIN', 1, 22.99, 1, -1, 'fashion design, haute couture, runway', 'casual clothing', 768, 1024, 30, 'HEUN', 1, 1, NOW(), NOW()),
('Texture Generator Pro', 'ADMIN', 1, 0.00, 1, 55555, 'seamless texture, material surface', 'solid color, flat', 1024, 1024, 35, 'LMS_KARRAS', 1, 1, NOW(), NOW()),
('Game Asset Creator', 'ADMIN', 1, 28.99, 1, -1, 'game asset, isometric view, pixel perfect', 'realistic photography', 1024, 1024, 40, 'EULER', 2, 1, NOW(), NOW()),
('Vintage Photo Filter', 'ADMIN', 1, 0.00, 1, 66666, 'vintage photography, retro aesthetic', 'modern, digital', 768, 768, 25, 'DPM2', 1, 1, NOW(), NOW()),
('Concept Art Master', 'ADMIN', 1, 45.99, 1, -1, 'concept art, entertainment industry', 'final product, polished', 1024, 768, 50, 'DPM_PLUS_PLUS_SDE_KARRAS', 1, 1, NOW(), NOW()),
('Pattern Designer', 'ADMIN', 1, 16.99, 1, 77777, 'seamless pattern, textile design', 'random, chaotic', 1024, 1024, 30, 'DPM2_A', 1, 1, NOW(), NOW()),
('Sketch to Art Converter', 'ADMIN', 1, 0.00, 1, -1, 'sketch to detailed art, line art enhancement', 'photograph, realistic', 768, 768, 35, 'EULER_A', 1, 1, NOW(), NOW());

-- 사용자 커스텀 모델 480개
INSERT INTO ai_model_tb (
    model_name, own_type, owner_id, price, is_public,
    seed, prompt, negative_prompt, width, height, steps, sampler_index, n_iter, batch_size,
    created_at, updated_at
)
SELECT 
    -- 모델명 생성
    CONCAT(
        CASE MOD(ROW_NUMBER() OVER (), 20)
            WHEN 0 THEN 'Custom Art Style'
            WHEN 1 THEN 'Personal Portrait'
            WHEN 2 THEN 'Fantasy World'
            WHEN 3 THEN 'Modern Design'
            WHEN 4 THEN 'Creative Vision'
            WHEN 5 THEN 'Artistic Expression'
            WHEN 6 THEN 'Digital Masterpiece'
            WHEN 7 THEN 'Unique Style'
            WHEN 8 THEN 'Creative Model'
            WHEN 9 THEN 'Personal Project'
            WHEN 10 THEN 'Custom Generator'
            WHEN 11 THEN 'Art Collection'
            WHEN 12 THEN 'Style Experiment'
            WHEN 13 THEN 'Creative Work'
            WHEN 14 THEN 'Personal Art'
            WHEN 15 THEN 'Custom Creation'
            WHEN 16 THEN 'Artistic Vision'
            WHEN 17 THEN 'Digital Art'
            WHEN 18 THEN 'Creative Studio'
            ELSE 'Personal Model'
        END,
        ' v', FLOOR(1 + (RAND() * 5)), '.', FLOOR(RAND() * 10)
    ) as model_name,
    
    'USER' as own_type,
    
    -- 소유자 ID (6~605번, 관리자와 테스트 계정 제외)
    ((ROW_NUMBER() OVER () - 1) % 600) + 6 as owner_id,
    
    -- 가격 설정 (비공개는 0, 공개 중 일부만 유료)
    CASE 
        WHEN MOD(ROW_NUMBER() OVER (), 10) < 7 THEN 0.00   -- 비공개(70%)는 가격 없음
        WHEN RAND() < 0.6 THEN 0.00                        -- 공개 중 60% 무료
        ELSE ROUND(5 + (RAND() * 45), 2)                   -- 공개 중 40% 유료 (5~50)
    END as price,
    
    -- 공개 여부 (70% 비공개)
    CASE WHEN MOD(ROW_NUMBER() OVER (), 10) < 7 THEN 0 ELSE 1 END as is_public,
    
    -- 시드값 (-1 또는 랜덤)
    CASE WHEN RAND() > 0.3 THEN -1 ELSE FLOOR(RAND() * 100000) END as seed,
    
    -- 프롬프트
    CASE MOD(ROW_NUMBER() OVER (), 8)
        WHEN 0 THEN 'my custom style, personal vision'
        WHEN 1 THEN 'unique artistic approach, creative'
        WHEN 2 THEN 'brand specific style, consistent'
        WHEN 3 THEN 'artistic experiment, innovative'
        WHEN 4 THEN 'professional quality, polished'
        WHEN 5 THEN 'personal project, meaningful'
        WHEN 6 THEN 'vibrant colors, expressive'
        ELSE 'high quality, detailed artwork'
    END as prompt,
    
    -- 네거티브 프롬프트
    CASE MOD(ROW_NUMBER() OVER (), 4)
        WHEN 0 THEN 'bad quality, amateur work'
        WHEN 1 THEN 'low quality, blurry image'
        WHEN 2 THEN 'worst quality, distorted'
        ELSE 'poor execution, unprofessional'
    END as negative_prompt,
    
    -- 이미지 크기
    CASE MOD(ROW_NUMBER() OVER (), 5)
        WHEN 0 THEN 512
        WHEN 1 THEN 768
        WHEN 2 THEN 1024
        WHEN 3 THEN 640
        ELSE 896
    END as width,
    
    CASE MOD(ROW_NUMBER() OVER (), 5)
        WHEN 0 THEN 512
        WHEN 1 THEN 768
        WHEN 2 THEN 1024
        WHEN 3 THEN 640
        ELSE 896
    END as height,
    
    -- 스텝수 (10~50)
    FLOOR(RAND() * 40) + 10 as steps,
    
    -- 샘플러 (ENUM 값 중 랜덤 선택)
    CASE MOD(ROW_NUMBER() OVER (), 19)
        WHEN 0 THEN 'DDIM'
        WHEN 1 THEN 'DPM2'
        WHEN 2 THEN 'DPM2_A'
        WHEN 3 THEN 'DPM2_A_KARRAS'
        WHEN 4 THEN 'DPM2_KARRAS'
        WHEN 5 THEN 'DPM_ADAPTIVE'
        WHEN 6 THEN 'DPM_FAST'
        WHEN 7 THEN 'DPM_PLUS_PLUS_2M'
        WHEN 8 THEN 'DPM_PLUS_PLUS_2M_KARRAS'
        WHEN 9 THEN 'DPM_PLUS_PLUS_2S_A'
        WHEN 10 THEN 'DPM_PLUS_PLUS_2S_A_KARRAS'
        WHEN 11 THEN 'DPM_PLUS_PLUS_SDE'
        WHEN 12 THEN 'DPM_PLUS_PLUS_SDE_KARRAS'
        WHEN 13 THEN 'EULER'
        WHEN 14 THEN 'EULER_A'
        WHEN 15 THEN 'HEUN'
        WHEN 16 THEN 'LMS'
        WHEN 17 THEN 'LMS_KARRAS'
        ELSE 'PLMS'
    END as sampler_index,
    
    -- 반복 횟수 (1~3)
    FLOOR(RAND() * 3) + 1 as n_iter,
    
    -- 배치 크기 (1~3)
    FLOOR(RAND() * 3) + 1 as batch_size,
    
    -- 생성일 (최근 1년 이내 랜덤)
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY) as created_at,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY) as updated_at

FROM (
    SELECT ROW_NUMBER() OVER () as n
    FROM information_schema.columns
    LIMIT 480
) numbers;