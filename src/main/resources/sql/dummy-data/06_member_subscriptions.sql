-- ================================================================================
-- 회원 구독 더미 데이터 (200개)
-- ================================================================================

-- 활성 구독 150개
INSERT INTO member_subscription (member_id, subscription_id, started_at, expires_at, status, auto_renewal, paid_amount)
WITH RECURSIVE seq(n) AS (
    SELECT 1 
    UNION ALL 
    SELECT n + 1 FROM seq WHERE n < 150
)
SELECT 
    t.member_id,
    t.subscription_id,
    t.started_at,
    t.expires_at,
    -- 상태 (expires_at 기준으로 결정)
    CASE 
        WHEN t.expires_at > NOW() THEN 'ACTIVE' 
        ELSE 'EXPIRED' 
    END as status,
    t.auto_renewal,
    t.paid_amount
FROM (
    SELECT 
        -- 회원 ID (2~605번 중에서, 관리자는 제외하고 랜덤 선택)
        (FLOOR(2 + (RAND() * 604))) as member_id,
        
        -- 구독 상품 ID (1~9번, 활성 구독만)
        (FLOOR(1 + (RAND() * 9))) as subscription_id_val,
        
        -- 구독 상품 ID를 변수로 저장하여 재사용
        @sub_id := (FLOOR(1 + (RAND() * 9))) as subscription_id,
        
        -- 시작일 (최근 11개월 이내)
        @start_date := DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 330) DAY) as started_at,
        
        -- 종료일 (시작일 + 구독 기간, subscription_id 기준)
        DATE_ADD(
            @start_date,
            INTERVAL 
            CASE @sub_id
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
        
        -- 자동 갱신 (70% 자동갱신)
        CASE WHEN RAND() < 0.7 THEN 1 ELSE 0 END as auto_renewal,
        
        -- 결제 금액 (subscription_id 기준)
        CASE @sub_id
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
    FROM seq
) t;

-- 만료된 구독 50개
INSERT INTO member_subscription (member_id, subscription_id, started_at, expires_at, status, auto_renewal, paid_amount)
WITH RECURSIVE seq(n) AS (
    SELECT 1 
    UNION ALL 
    SELECT n + 1 FROM seq WHERE n < 50
)
SELECT 
    t.member_id,
    t.subscription_id,
    t.started_at,
    t.expires_at,
    'EXPIRED' as status,  -- 모두 만료 상태
    t.auto_renewal,
    t.paid_amount
FROM (
    SELECT 
        -- 회원 ID
        (FLOOR(2 + (RAND() * 604))) as member_id,
        
        -- 구독 상품 ID를 변수로 저장하여 재사용
        @sub_id := (FLOOR(1 + (RAND() * 10))) as subscription_id,
        
        -- 시작일 (6개월~2년 전)
        @start_date := DATE_SUB(NOW(), INTERVAL (180 + FLOOR(RAND() * 550)) DAY) as started_at,
        
        -- 종료일 (시작일 + 구독 기간 후 만료, subscription_id 기준)
        DATE_ADD(
            @start_date,
            INTERVAL 
            CASE @sub_id
                WHEN 1 THEN 30   -- Basic Plan
                WHEN 2 THEN 30   -- Standard Plan  
                WHEN 3 THEN 30   -- Premium Plan
                WHEN 4 THEN 30   -- Enterprise Plan
                WHEN 5 THEN 365  -- Basic Annual
                WHEN 6 THEN 365  -- Standard Annual
                WHEN 7 THEN 365  -- Premium Annual
                WHEN 8 THEN 30   -- Student Plan
                WHEN 9 THEN 30   -- Creator Plan
                ELSE 30          -- Legacy Plan
            END DAY
        ) as expires_at,
        
        -- 자동갱신 (30% 자동갱신이었음)
        CASE WHEN RAND() < 0.3 THEN 1 ELSE 0 END as auto_renewal,
        
        -- 결제 금액 (subscription_id 기준)
        CASE @sub_id
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
    FROM seq
) t
WHERE t.expires_at < NOW();  -- 만료된 구독만 선택