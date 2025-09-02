-- ================================================================================
-- 나머지 테이블들의 더미 데이터
-- ================================================================================

-- 할인 정책 데이터 (20개)
INSERT INTO discount_policy (name, description, discount_type, discount_value, min_purchase_amount, max_discount_amount, start_date, end_date, is_active, created_at, updated_at) VALUES
('신규회원 할인', '신규 가입 회원을 위한 특별 할인입니다.', 'PERCENTAGE', 20.00, 10.00, 50.00, '2023-01-01', '2024-12-31', 1, '2023-01-01 00:00:00', NOW()),
('여름 시즌 할인', '여름 시즌 특별 프로모션 할인입니다.', 'PERCENTAGE', 15.00, 20.00, 100.00, '2023-06-01', '2023-08-31', 0, '2023-06-01 00:00:00', NOW()),
('구독 업그레이드 할인', '구독 플랜 업그레이드 시 적용되는 할인입니다.', 'FIXED', 10.00, 30.00, 10.00, '2023-01-01', '2024-12-31', 1, '2023-01-01 00:00:00', NOW()),
('연말 대형 세일', '연말 특별 할인 이벤트입니다.', 'PERCENTAGE', 30.00, 50.00, 200.00, '2023-11-01', '2023-12-31', 1, '2023-11-01 00:00:00', NOW()),
('학생 할인', '학생 인증 시 적용되는 할인입니다.', 'PERCENTAGE', 50.00, 5.00, 25.00, '2023-01-01', '2024-12-31', 1, '2023-01-01 00:00:00', NOW());

-- 모델 리뷰 데이터 (100개)
INSERT INTO model_review (model_id, member_id, rating, comment, is_public, created_at, updated_at)
SELECT 
    -- 공개 모델만 리뷰 가능
    (SELECT model_id FROM ai_model_tb WHERE is_public = 1 ORDER BY RAND() LIMIT 1) as model_id,
    
    -- 리뷰어 (2~605번 회원 중 랜덤)
    (FLOOR(2 + (RAND() * 604))) as member_id,
    
    -- 평점 (1~5)
    (FLOOR(1 + (RAND() * 5))) as rating,
    
    -- 리뷰 내용
    CASE FLOOR(1 + (RAND() * 10))
        WHEN 1 THEN '정말 훌륭한 모델입니다! 결과물의 품질이 매우 뛰어나고 사용하기도 편합니다.'
        WHEN 2 THEN '기대했던 것보다 좋은 결과를 얻을 수 있었습니다. 추천합니다.'
        WHEN 3 THEN '아직 몇 가지 개선점이 있지만 전반적으로 만족합니다.'
        WHEN 4 THEN '독창적이고 창의적인 결과물을 생성해줍니다. 매우 유용해요.'
        WHEN 5 THEN '사용법이 직관적이고 결과물도 예상한 대로 나왔습니다.'
        WHEN 6 THEN '가격 대비 훌륭한 성능을 보여줍니다. 강력 추천!'
        WHEN 7 THEN '몇 번의 시도 끝에 원하는 결과를 얻을 수 있었습니다.'
        WHEN 8 THEN '전문적인 용도로 사용하기에 충분한 품질입니다.'
        WHEN 9 THEN '다른 모델들과 차별화된 독특한 스타일이 마음에 듭니다.'
        ELSE '전반적으로 좋은 모델이지만 더 많은 기능이 있으면 좋겠습니다.'
    END as comment,
    
    -- 공개 여부 (90% 공개)
    CASE WHEN RAND() < 0.9 THEN 1 ELSE 0 END as is_public,
    
    -- 생성일 (최근 6개월 이내)
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 180) DAY) as created_at,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY) as updated_at

FROM (
    SELECT ROW_NUMBER() OVER () as n
    FROM information_schema.columns
    LIMIT 100
) numbers;

-- 쿠폰 데이터 (30개)
INSERT INTO coupon (code, name, description, discount_type, discount_value, min_purchase_amount, max_discount_amount, usage_limit, used_count, start_date, end_date, is_active, created_at, updated_at)
SELECT 
    -- 쿠폰 코드
    CONCAT('COUP', LPAD(ROW_NUMBER() OVER (), 4, '0')) as code,
    
    -- 쿠폰 이름
    CONCAT(
        CASE MOD(ROW_NUMBER() OVER (), 5)
            WHEN 0 THEN '신규회원 환영'
            WHEN 1 THEN '시즌 특별'
            WHEN 2 THEN '리뷰 작성 감사'
            WHEN 3 THEN 'VIP 회원'
            ELSE '이벤트 참여'
        END,
        ' 쿠폰 #', ROW_NUMBER() OVER ()
    ) as name,
    
    -- 설명
    '특별 할인 쿠폰입니다. 유효기간 내에 사용해주세요.' as description,
    
    -- 할인 타입
    CASE WHEN MOD(ROW_NUMBER() OVER (), 2) = 0 THEN 'PERCENTAGE' ELSE 'FIXED' END as discount_type,
    
    -- 할인 값
    CASE 
        WHEN MOD(ROW_NUMBER() OVER (), 2) = 0 THEN (5 + FLOOR(RAND() * 25))  -- 퍼센트: 5~30%
        ELSE (5 + FLOOR(RAND() * 20))                                        -- 고정: $5~25
    END as discount_value,
    
    -- 최소 구매 금액
    (10 + FLOOR(RAND() * 40)) as min_purchase_amount,
    
    -- 최대 할인 금액
    (20 + FLOOR(RAND() * 80)) as max_discount_amount,
    
    -- 사용 한도
    (10 + FLOOR(RAND() * 90)) as usage_limit,
    
    -- 사용된 횟수
    FLOOR(RAND() * 30) as used_count,
    
    -- 시작일
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY) as start_date,
    
    -- 종료일
    DATE_ADD(NOW(), INTERVAL (30 + FLOOR(RAND() * 60)) DAY) as end_date,
    
    -- 활성 여부
    CASE WHEN RAND() < 0.8 THEN 1 ELSE 0 END as is_active,
    
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 60) DAY) as created_at,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 7) DAY) as updated_at

FROM (
    SELECT ROW_NUMBER() OVER () as n
    FROM information_schema.columns
    LIMIT 30
) numbers;

-- 파일 데이터 (50개)
INSERT INTO file_tb (original_name, stored_name, file_path, file_size, content_type, upload_member_id, created_at, updated_at)
SELECT 
    -- 원본 파일명
    CONCAT(
        CASE MOD(ROW_NUMBER() OVER (), 8)
            WHEN 0 THEN 'sample_image'
            WHEN 1 THEN 'generated_art'
            WHEN 2 THEN 'custom_model'
            WHEN 3 THEN 'user_upload'
            WHEN 4 THEN 'thumbnail'
            WHEN 5 THEN 'profile_pic'
            WHEN 6 THEN 'ai_artwork'
            ELSE 'model_output'
        END,
        '_', ROW_NUMBER() OVER (),
        CASE MOD(ROW_NUMBER() OVER (), 4)
            WHEN 0 THEN '.jpg'
            WHEN 1 THEN '.png'
            WHEN 2 THEN '.webp'
            ELSE '.gif'
        END
    ) as original_name,
    
    -- 저장 파일명 (UUID 형식)
    CONCAT(
        SUBSTRING(MD5(RAND()), 1, 8), '-',
        SUBSTRING(MD5(RAND()), 1, 4), '-',
        SUBSTRING(MD5(RAND()), 1, 4), '-',
        SUBSTRING(MD5(RAND()), 1, 4), '-',
        SUBSTRING(MD5(RAND()), 1, 12),
        CASE MOD(ROW_NUMBER() OVER (), 4)
            WHEN 0 THEN '.jpg'
            WHEN 1 THEN '.png'
            WHEN 2 THEN '.webp'
            ELSE '.gif'
        END
    ) as stored_name,
    
    -- 파일 경로
    CONCAT('/uploads/', YEAR(NOW()), '/', MONTH(NOW()), '/', DAY(NOW()), '/') as file_path,
    
    -- 파일 크기 (KB)
    (50 + FLOOR(RAND() * 5000)) as file_size,
    
    -- 콘텐츠 타입
    CASE MOD(ROW_NUMBER() OVER (), 4)
        WHEN 0 THEN 'image/jpeg'
        WHEN 1 THEN 'image/png'
        WHEN 2 THEN 'image/webp'
        ELSE 'image/gif'
    END as content_type,
    
    -- 업로드 회원 ID
    (FLOOR(2 + (RAND() * 604))) as upload_member_id,
    
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 180) DAY) as created_at,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY) as updated_at

FROM (
    SELECT ROW_NUMBER() OVER () as n
    FROM information_schema.columns
    LIMIT 50
) numbers;

-- 신고 데이터 (25개)
INSERT INTO report_tb (reporter_id, reported_content_type, reported_content_id, report_type, reason, description, status, admin_response, processed_at, created_at, updated_at)
SELECT 
    -- 신고자 ID
    (FLOOR(2 + (RAND() * 604))) as reporter_id,
    
    -- 신고 대상 타입
    CASE MOD(ROW_NUMBER() OVER (), 3)
        WHEN 0 THEN 'MODEL'
        WHEN 1 THEN 'REVIEW'
        ELSE 'USER'
    END as reported_content_type,
    
    -- 신고 대상 ID
    CASE MOD(ROW_NUMBER() OVER (), 3)
        WHEN 0 THEN (SELECT model_id FROM ai_model_tb WHERE is_public = 1 ORDER BY RAND() LIMIT 1)
        WHEN 1 THEN (SELECT review_id FROM model_review ORDER BY RAND() LIMIT 1)
        ELSE (FLOOR(2 + (RAND() * 604)))
    END as reported_content_id,
    
    -- 신고 유형
    CASE MOD(ROW_NUMBER() OVER (), 6)
        WHEN 0 THEN 'INAPPROPRIATE_CONTENT'
        WHEN 1 THEN 'COPYRIGHT_VIOLATION'
        WHEN 2 THEN 'SPAM'
        WHEN 3 THEN 'HARASSMENT'
        WHEN 4 THEN 'FALSE_INFORMATION'
        ELSE 'OTHER'
    END as report_type,
    
    -- 신고 사유
    CASE MOD(ROW_NUMBER() OVER (), 6)
        WHEN 0 THEN '부적절한 내용이 포함되어 있습니다.'
        WHEN 1 THEN '저작권을 침해하는 내용입니다.'
        WHEN 2 THEN '스팸성 게시물입니다.'
        WHEN 3 THEN '다른 사용자를 괴롭히는 내용입니다.'
        WHEN 4 THEN '잘못된 정보를 제공합니다.'
        ELSE '기타 사유'
    END as reason,
    
    -- 상세 설명
    '신고 내용에 대한 상세한 설명입니다. 관련 규정을 위반한 것으로 보입니다.' as description,
    
    -- 처리 상태
    CASE MOD(ROW_NUMBER() OVER (), 4)
        WHEN 0 THEN 'PENDING'
        WHEN 1 THEN 'INVESTIGATING'
        WHEN 2 THEN 'RESOLVED'
        ELSE 'REJECTED'
    END as status,
    
    -- 관리자 응답
    CASE MOD(ROW_NUMBER() OVER (), 4)
        WHEN 2 THEN '신고 내용을 확인하여 적절한 조치를 취했습니다.'
        WHEN 3 THEN '검토 결과 규정 위반 사항을 발견하지 못했습니다.'
        ELSE NULL
    END as admin_response,
    
    -- 처리일
    CASE MOD(ROW_NUMBER() OVER (), 4)
        WHEN 0 THEN NULL
        WHEN 1 THEN NULL
        ELSE DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
    END as processed_at,
    
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 90) DAY) as created_at,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 7) DAY) as updated_at

FROM (
    SELECT ROW_NUMBER() OVER () as n
    FROM information_schema.columns
    LIMIT 25
) numbers;