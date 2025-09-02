-- ================================================================================
-- 모델 통계 더미 데이터 (500개)
-- ================================================================================

-- 각 AI 모델에 대응하는 통계 데이터 생성
INSERT INTO model_statistics_tb (model_id, usage_count, view_count, created_at, updated_at)
SELECT 
    t.model_id,
    t.usage_count,
    -- 조회수 (사용횟수의 5~20배로 일관되게 계산)
    FLOOR(t.usage_count * (5 + RAND() * 15)) as view_count,
    t.created_at,
    -- 수정일 (최근 30일 이내, created_at 이후로 보정)
    GREATEST(t.created_at, DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)) as updated_at
FROM (
    SELECT 
        ai_model_tb.model_id,
        -- 사용 횟수 (공개/유료 여부에 따라 차등)
        CASE 
            WHEN ai_model_tb.is_public = b'1' AND ai_model_tb.price = 0.00 THEN FLOOR(1000 + (RAND() * 299000))  -- 공개 무료: 1K~300K
            WHEN ai_model_tb.is_public = b'1' AND ai_model_tb.price > 0.00 THEN FLOOR(100 + (RAND() * 49900))    -- 공개 유료: 100~50K
            ELSE FLOOR(1 + (RAND() * 999))                                                                       -- 비공개: 1~1K
        END as usage_count,
        -- 생성일 (모델 생성일과 동일하거나 이후)
        GREATEST(ai_model_tb.created_at, DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY)) as created_at
    FROM ai_model_tb
) t
ORDER BY t.model_id;