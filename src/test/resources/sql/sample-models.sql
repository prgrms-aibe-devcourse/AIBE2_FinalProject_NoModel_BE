-- 샘플 AI 모델 데이터만
INSERT INTO ai_model (id, model_name, own_type, owner_id, price, is_public, view_count, download_count, 
                      seed, prompt, negative_prompt, width, height, steps, sampler_index, n_iter, batch_size,
                      created_at, updated_at, created_by, updated_by) VALUES
(1, 'Basic Test Model', 'USER', 1, 10.00, true, 100, 10, 
 1234, 'test prompt', 'test negative', 512, 512, 20, 'EULER_A', 1, 1,
 NOW(), NOW(), 'testUser', 'testUser'),
(2, 'System Model', 'SYSTEM', null, 0.00, true, 500, 100, 
 5678, 'system prompt', 'system negative', 768, 768, 25, 'DPM_PLUS_PLUS_2M', 1, 1,
 NOW(), NOW(), 'SYSTEM', 'SYSTEM');

ALTER TABLE ai_model AUTO_INCREMENT = 10;