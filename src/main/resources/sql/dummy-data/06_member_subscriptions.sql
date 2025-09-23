-- ================================================================================
-- 회원 구독 더미 데이터 (100개)
-- ================================================================================

-- 활성 구독 70개 (FREE 40개, PRO 20개, ENTERPRISE 10개)
INSERT INTO member_subscription (member_id, subscription_id, started_at, expires_at, status, auto_renewal, paid_amount)
WITH RECURSIVE seq(n) AS (
    SELECT 1 
    UNION ALL 
    SELECT n + 1 FROM seq WHERE n < 70
)
SELECT 
    t.member_id,
    t.subscription_id,
    t.started_at,
    t.expires_at,
    'ACTIVE' as status,
    t.auto_renewal,
    t.paid_amount
FROM (
    SELECT 
        -- 회원 ID (2~105번 중에서 랜덤 선택, 더 현실적인 범위)
        (FLOOR(2 + (RAND() * 104))) as member_id,
        
        -- 구독 상품 ID (FREE 많이, PRO 중간, ENTERPRISE 적게)
        @sub_id := CASE 
            WHEN @rank <= 40 THEN 1  -- FREE (40개)
            WHEN @rank <= 60 THEN 2  -- PRO (20개) 
            ELSE 3                   -- ENTERPRISE (10개)
        END as subscription_id,
        
        @rank := (@row_number := @row_number + 1) as rank_num,
        
        -- 시작일 (최근 6개월 이내)
        @start_date := DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 180) DAY) as started_at,
        
        -- 종료일 (시작일 + 30일)
        DATE_ADD(@start_date, INTERVAL 30 DAY) as expires_at,
        
        -- 자동 갱신 (PRO, ENTERPRISE는 높은 비율로 자동갱신)
        CASE @sub_id
            WHEN 1 THEN CASE WHEN RAND() < 0.3 THEN 1 ELSE 0 END  -- FREE: 30%
            WHEN 2 THEN CASE WHEN RAND() < 0.8 THEN 1 ELSE 0 END  -- PRO: 80%
            ELSE CASE WHEN RAND() < 0.9 THEN 1 ELSE 0 END         -- ENTERPRISE: 90%
        END as auto_renewal,
        
        -- 결제 금액 (subscription_id 기준)
        CASE @sub_id
            WHEN 1 THEN 0.00      -- FREE Plan
            WHEN 2 THEN 19.99     -- PRO Plan
            ELSE 199.99           -- ENTERPRISE Plan
        END as paid_amount
    FROM seq
    CROSS JOIN (SELECT @row_number := 0) r
) t;

-- 만료된 구독 30개
INSERT INTO member_subscription (member_id, subscription_id, started_at, expires_at, status, auto_renewal, paid_amount)
WITH RECURSIVE seq(n) AS (
    SELECT 1 
    UNION ALL 
    SELECT n + 1 FROM seq WHERE n < 30
)
SELECT 
    t.member_id,
    t.subscription_id,
    t.started_at,
    t.expires_at,
    'EXPIRED' as status,
    t.auto_renewal,
    t.paid_amount
FROM (
    SELECT 
        -- 회원 ID
        (FLOOR(2 + (RAND() * 104))) as member_id,
        
        -- 구독 상품 ID (만료된 구독도 비슷한 비율)
        @sub_id := CASE 
            WHEN @rank <= 15 THEN 1  -- FREE (15개)
            WHEN @rank <= 25 THEN 2  -- PRO (10개)
            ELSE 3                   -- ENTERPRISE (5개)
        END as subscription_id,
        
        @rank := (@row_number := @row_number + 1) as rank_num,
        
        -- 시작일 (6개월~1년 전)
        @start_date := DATE_SUB(NOW(), INTERVAL (180 + FLOOR(RAND() * 185)) DAY) as started_at,
        
        -- 종료일 (시작일 + 30일, 현재보다 과거)
        DATE_ADD(@start_date, INTERVAL 30 DAY) as expires_at,
        
        -- 자동갱신 (만료된 구독은 낮은 비율)
        CASE WHEN RAND() < 0.2 THEN 1 ELSE 0 END as auto_renewal,
        
        -- 결제 금액
        CASE @sub_id
            WHEN 1 THEN 0.00      -- FREE Plan
            WHEN 2 THEN 19.99     -- PRO Plan  
            ELSE 199.99           -- ENTERPRISE Plan
        END as paid_amount
    FROM seq
    CROSS JOIN (SELECT @row_number := 0) r
) t
WHERE t.expires_at < NOW();  -- 만료된 구독만 선택