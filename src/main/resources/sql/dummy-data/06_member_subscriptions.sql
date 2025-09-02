-- ================================================================================
-- 회원 구독 더미 데이터 (200개)
-- ================================================================================

-- 활성 구독 150개
INSERT INTO member_subscription (member_id, subscription_id, start_date, end_date, is_active, auto_renewal, remaining_usage, total_usage_count, created_at, updated_at)
SELECT 
    -- 회원 ID (2~605번 중에서, 관리자는 제외하고 랜덤 선택)
    (FLOOR(2 + (RAND() * 604))) as member_id,
    
    -- 구독 상품 ID (1~9번, 활성 구독만)
    (FLOOR(1 + (RAND() * 9))) as subscription_id,
    
    -- 시작일 (최근 11개월 이내)
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 330) DAY) as start_date,
    
    -- 종료일 (시작일 + 구독 기간)
    DATE_ADD(
        DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 330) DAY),
        INTERVAL 
        CASE (FLOOR(1 + (RAND() * 9)))
            WHEN 1 THEN 30   -- Basic Plan
            WHEN 2 THEN 30   -- Standard Plan  
            WHEN 3 THEN 30   -- Premium Plan
            WHEN 4 THEN 30   -- Enterprise Plan
            WHEN 5 THEN 365  -- Basic Annual
            WHEN 6 THEN 365  -- Standard Annual
            WHEN 7 THEN 365  -- Premium Annual
            WHEN 8 THEN 30   -- Student Plan
            ELSE 30          -- Creator Plan
        END DAY
    ) as end_date,
    
    -- 활성 여부 (80% 활성)
    CASE WHEN RAND() < 0.8 THEN 1 ELSE 0 END as is_active,
    
    -- 자동 갱신 (70% 자동갱신)
    CASE WHEN RAND() < 0.7 THEN 1 ELSE 0 END as auto_renewal,
    
    -- 남은 사용량 (구독별 최대 사용량의 10~90%)
    CASE (FLOOR(1 + (RAND() * 9)))
        WHEN 1 THEN FLOOR(10 + (RAND() * 80))    -- Basic: 10~90
        WHEN 2 THEN FLOOR(50 + (RAND() * 400))   -- Standard: 50~450
        WHEN 3 THEN FLOOR(200 + (RAND() * 1600)) -- Premium: 200~1800
        WHEN 4 THEN -1                           -- Enterprise: 무제한
        WHEN 5 THEN FLOOR(10 + (RAND() * 80))    -- Basic Annual: 10~90
        WHEN 6 THEN FLOOR(50 + (RAND() * 400))   -- Standard Annual: 50~450
        WHEN 7 THEN FLOOR(200 + (RAND() * 1600)) -- Premium Annual: 200~1800
        WHEN 8 THEN FLOOR(50 + (RAND() * 400))   -- Student: 50~450
        ELSE FLOOR(100 + (RAND() * 800))          -- Creator: 100~900
    END as remaining_usage,
    
    -- 총 사용량 (최대 사용량에서 남은 사용량을 뺀 값)
    CASE (FLOOR(1 + (RAND() * 9)))
        WHEN 1 THEN 100 - FLOOR(10 + (RAND() * 80))
        WHEN 2 THEN 500 - FLOOR(50 + (RAND() * 400))
        WHEN 3 THEN 2000 - FLOOR(200 + (RAND() * 1600))
        WHEN 4 THEN FLOOR(RAND() * 10000)  -- Enterprise: 랜덤 사용량
        WHEN 5 THEN 100 - FLOOR(10 + (RAND() * 80))
        WHEN 6 THEN 500 - FLOOR(50 + (RAND() * 400))
        WHEN 7 THEN 2000 - FLOOR(200 + (RAND() * 1600))
        WHEN 8 THEN 500 - FLOOR(50 + (RAND() * 400))
        ELSE 1000 - FLOOR(100 + (RAND() * 800))
    END as total_usage_count,
    
    -- 생성일 (시작일과 동일)
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 330) DAY) as created_at,
    
    -- 수정일 (최근 7일 이내)
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 7) DAY) as updated_at

FROM (
    SELECT ROW_NUMBER() OVER () as n
    FROM information_schema.columns
    LIMIT 150
) numbers;

-- 만료된 구독 50개
INSERT INTO member_subscription (member_id, subscription_id, start_date, end_date, is_active, auto_renewal, remaining_usage, total_usage_count, created_at, updated_at)
SELECT 
    -- 회원 ID
    (FLOOR(2 + (RAND() * 604))) as member_id,
    
    -- 구독 상품 ID (레거시 플랜 포함)
    (FLOOR(1 + (RAND() * 10))) as subscription_id,
    
    -- 시작일 (6개월~2년 전)
    DATE_SUB(NOW(), INTERVAL (180 + FLOOR(RAND() * 550)) DAY) as start_date,
    
    -- 종료일 (이미 만료됨)
    DATE_SUB(NOW(), INTERVAL (1 + FLOOR(RAND() * 180)) DAY) as end_date,
    
    -- 비활성
    0 as is_active,
    
    -- 자동갱신 (30% 자동갱신이었음)
    CASE WHEN RAND() < 0.3 THEN 1 ELSE 0 END as auto_renewal,
    
    -- 남은 사용량 0
    0 as remaining_usage,
    
    -- 총 사용량 (구독별 최대치에 근접)
    CASE (FLOOR(1 + (RAND() * 10)))
        WHEN 1 THEN FLOOR(80 + (RAND() * 20))     -- Basic: 80~100
        WHEN 2 THEN FLOOR(450 + (RAND() * 50))    -- Standard: 450~500
        WHEN 3 THEN FLOOR(1800 + (RAND() * 200))  -- Premium: 1800~2000
        WHEN 4 THEN FLOOR(5000 + (RAND() * 15000)) -- Enterprise: 5K~20K
        WHEN 5 THEN FLOOR(80 + (RAND() * 20))     -- Basic Annual
        WHEN 6 THEN FLOOR(450 + (RAND() * 50))    -- Standard Annual
        WHEN 7 THEN FLOOR(1800 + (RAND() * 200))  -- Premium Annual
        WHEN 8 THEN FLOOR(450 + (RAND() * 50))    -- Student
        WHEN 9 THEN FLOOR(900 + (RAND() * 100))   -- Creator
        ELSE FLOOR(250 + (RAND() * 50))            -- Legacy
    END as total_usage_count,
    
    -- 생성일
    DATE_SUB(NOW(), INTERVAL (180 + FLOOR(RAND() * 550)) DAY) as created_at,
    
    -- 수정일 (종료일과 비슷)
    DATE_SUB(NOW(), INTERVAL (1 + FLOOR(RAND() * 180)) DAY) as updated_at

FROM (
    SELECT ROW_NUMBER() OVER () as n
    FROM information_schema.columns
    LIMIT 50
) numbers;