-- ================================================================================
-- 회원 더미 데이터 (605명)
-- ================================================================================

-- 테스트용 특별 계정
-- 공통 비밀번호: password123
INSERT INTO member_tb (username, email, password, role, status, created_at, updated_at) VALUES
('시스템관리자', 'admin@nomodel.com', '$2a$10$VTBDvqMl8hovng7euw98bug52GBvQ.NZSNZMGS3seEAv7ndMZGvoC', 'ADMIN', 'ACTIVE', '2023-01-01 00:00:00', NOW()),
('테스트유저', 'test@nomodel.com', '$2a$10$VTBDvqMl8hovng7euw98bug52GBvQ.NZSNZMGS3seEAv7ndMZGvoC', 'USER', 'ACTIVE', '2023-01-15 00:00:00', NOW()),
('프리미엄유저', 'premium@nomodel.com', '$2a$10$VTBDvqMl8hovng7euw98bug52GBvQ.NZSNZMGS3seEAv7ndMZGvoC', 'USER', 'ACTIVE', '2023-02-01 00:00:00', NOW()),
('비즈니스유저', 'business@nomodel.com', '$2a$10$VTBDvqMl8hovng7euw98bug52GBvQ.NZSNZMGS3seEAv7ndMZGvoC', 'USER', 'ACTIVE', '2023-03-01 00:00:00', NOW()),
('정지된유저', 'suspended@nomodel.com', '$2a$10$VTBDvqMl8hovng7euw98bug52GBvQ.NZSNZMGS3seEAv7ndMZGvoC', 'USER', 'SUSPENDED', '2023-04-01 00:00:00', NOW());

-- 일반 회원 500명
INSERT INTO member_tb (username, email, password, role, status, created_at, updated_at)
SELECT 
    t.username,
    t.email,
    t.password,
    t.role,
    t.status,
    t.created_at,
    -- updated_at은 created_at 이후로 보정
    GREATEST(t.created_at, DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)) as updated_at
FROM (
    WITH RECURSIVE seq(n) AS (
        SELECT 1 
        UNION ALL 
        SELECT n + 1 FROM seq WHERE n < 500
    )
    SELECT 
        CONCAT(
            CASE MOD(n, 10)
                WHEN 0 THEN '김민수'
                WHEN 1 THEN '이영희'
                WHEN 2 THEN '박철수'
                WHEN 3 THEN '최지연'
                WHEN 4 THEN '정대한'
                WHEN 5 THEN '한소라'
                WHEN 6 THEN '강민준'
                WHEN 7 THEN '조은비'
                WHEN 8 THEN '윤서진'
                ELSE '홍길동'
            END,
            '_', n
        ) as username,
        CONCAT('user_', n, '@nomodel.com') as email,
        '$2a$10$VTBDvqMl8hovng7euw98bug52GBvQ.NZSNZMGS3seEAv7ndMZGvoC' as password,
        CASE WHEN MOD(n, 50) = 0 THEN 'ADMIN' ELSE 'USER' END as role,
        CASE 
            WHEN MOD(n, 100) = 0 THEN 'SUSPENDED'
            WHEN MOD(n, 20) = 0 THEN 'INACTIVE'  
            ELSE 'ACTIVE'
        END as status,
        -- created_at을 하위 SELECT에서 고정
        DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 730) DAY) as created_at
    FROM seq
) t;

-- 이벤트 회원 100명
INSERT INTO member_tb (username, email, password, role, status, created_at, updated_at)
SELECT 
    t.username,
    t.email,
    t.password,
    t.role,
    t.status,
    t.created_at,
    -- updated_at은 created_at 이후로 보정
    GREATEST(t.created_at, DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 180) DAY)) as updated_at
FROM (
    WITH RECURSIVE seq(n) AS (
        SELECT 1 
        UNION ALL 
        SELECT n + 1 FROM seq WHERE n < 100
    )
    SELECT 
        CONCAT('이벤트유저_', n) as username,
        CONCAT('event_', n, '@nomodel.com') as email,
        '$2a$10$VTBDvqMl8hovng7euw98bug52GBvQ.NZSNZMGS3seEAv7ndMZGvoC' as password,
        'USER' as role,
        'ACTIVE' as status,
        -- created_at을 하위 SELECT에서 고정  
        DATE_SUB(NOW(), INTERVAL 180 DAY) as created_at
    FROM seq
) t;