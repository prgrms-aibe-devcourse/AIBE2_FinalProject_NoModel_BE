-- ================================================================================
-- 회원 구독 더미 데이터 (200개)
-- ================================================================================

-- 활성 구독 150개
INSERT INTO member_subscription (member_id, subscription_id, started_at, expires_at, status, auto_renewal, paid_amount)
SELECT 
    -- 회원 ID (2~605번 중에서, 관리자는 제외하고 랜덤 선택)
    (FLOOR(2 + (RAND() * 604))) as member_id,
    
    -- 구독 상품 ID (1~9번, 활성 구독만)
    (FLOOR(1 + (RAND() * 9))) as subscription_id,
    
    -- 시작일 (최근 11개월 이내)
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 330) DAY) as started_at,
    
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
    ) as expires_at,
    
    -- 상태 (80% 활성)
    CASE WHEN RAND() < 0.8 THEN 'ACTIVE' ELSE 'EXPIRED' END as status,
    
    -- 자동 갱신 (70% 자동갱신)
    CASE WHEN RAND() < 0.7 THEN 1 ELSE 0 END as auto_renewal,
    
    -- 결제 금액 (구독 상품별 가격)
    CASE (FLOOR(1 + (RAND() * 9)))
        WHEN 1 THEN 9900    -- Basic Plan
        WHEN 2 THEN 29900   -- Standard Plan
        WHEN 3 THEN 99000   -- Premium Plan
        WHEN 4 THEN 299000  -- Enterprise Plan
        WHEN 5 THEN 108900  -- Basic Annual (10% 할인)
        WHEN 6 THEN 322920  -- Standard Annual (10% 할인)
        WHEN 7 THEN 1069200 -- Premium Annual (10% 할인)
        WHEN 8 THEN 19900   -- Student Plan
        ELSE 49900          -- Creator Plan
    END as paid_amount

FROM (
    SELECT ROW_NUMBER() OVER () as n
    FROM information_schema.columns
    LIMIT 150
) numbers;

-- 만료된 구독 50개
INSERT INTO member_subscription (member_id, subscription_id, started_at, expires_at, status, auto_renewal, paid_amount)
SELECT 
    -- 회원 ID
    (FLOOR(2 + (RAND() * 604))) as member_id,
    
    -- 구독 상품 ID (레거시 플랜 포함)
    (FLOOR(1 + (RAND() * 10))) as subscription_id,
    
    -- 시작일 (6개월~2년 전)
    DATE_SUB(NOW(), INTERVAL (180 + FLOOR(RAND() * 550)) DAY) as started_at,
    
    -- 종료일 (이미 만료됨)
    DATE_SUB(NOW(), INTERVAL (1 + FLOOR(RAND() * 180)) DAY) as expires_at,
    
    -- 상태 (만료됨)
    'EXPIRED' as status,
    
    -- 자동갱신 (30% 자동갱신이었음)
    CASE WHEN RAND() < 0.3 THEN 1 ELSE 0 END as auto_renewal,
    
    -- 결제 금액 (구독 상품별 가격)
    CASE (FLOOR(1 + (RAND() * 10)))
        WHEN 1 THEN 9900    -- Basic Plan
        WHEN 2 THEN 29900   -- Standard Plan
        WHEN 3 THEN 99000   -- Premium Plan
        WHEN 4 THEN 299000  -- Enterprise Plan
        WHEN 5 THEN 108900  -- Basic Annual
        WHEN 6 THEN 322920  -- Standard Annual
        WHEN 7 THEN 1069200 -- Premium Annual
        WHEN 8 THEN 19900   -- Student Plan
        WHEN 9 THEN 49900   -- Creator Plan
        ELSE 29900          -- Legacy Plan
    END as paid_amount

FROM (
    SELECT ROW_NUMBER() OVER () as n
    FROM information_schema.columns
    LIMIT 50
) numbers;