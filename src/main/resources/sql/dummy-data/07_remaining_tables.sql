-- ================================================================================
-- 나머지 테이블 더미 데이터 (최종 버전)
-- ================================================================================
-- 주의: member_subscription 데이터는 06_member_subscriptions.sql에서 생성됩니다.

-- 1. 포인트 정책 데이터 (20개)
INSERT INTO point_policy (name, point_amount, policy_type, is_active, priority, refer_type, valid_from, valid_to) VALUES
('회원가입 축하', 1000, 'SIGNUP', 1, 1, 'MEMBER', '2023-01-01', '2024-12-31'),
('모델 구매', 100, 'PURCHASE', 1, 2, 'MODEL', '2023-01-01', '2024-12-31'),
('리뷰 작성', 50, 'REVIEW', 1, 3, 'REVIEW', '2023-01-01', '2024-12-31'),
('구독 갱신', 500, 'SUBSCRIPTION', 1, 4, 'SUBSCRIPTION', '2023-01-01', '2024-12-31'),
('출석체크', 10, 'DAILY', 1, 5, 'SYSTEM', '2023-01-01', '2024-12-31'),
('이벤트 참여', 200, 'EVENT', 1, 6, 'EVENT', '2023-01-01', '2024-12-31'),
('모델 업로드', 300, 'UPLOAD', 1, 7, 'MODEL', '2023-01-01', '2024-12-31'),
('추천 성공', 2000, 'REFERRAL', 1, 8, 'MEMBER', '2023-01-01', '2024-12-31'),
('생일 축하', 1500, 'BIRTHDAY', 1, 9, 'MEMBER', '2023-01-01', '2024-12-31'),
('VIP 달성', 5000, 'VIP', 1, 10, 'MEMBER', '2023-01-01', '2024-12-31'),
('첫 구매', 500, 'FIRST_PURCHASE', 1, 11, 'ORDER', '2023-01-01', '2024-12-31'),
('대량 구매', 1000, 'BULK_PURCHASE', 1, 12, 'ORDER', '2023-01-01', '2024-12-31'),
('재구매', 200, 'REPEAT_PURCHASE', 1, 13, 'ORDER', '2023-01-01', '2024-12-31'),
('소셜 공유', 20, 'SOCIAL_SHARE', 1, 14, 'SOCIAL', '2023-01-01', '2024-12-31'),
('평점 5점', 100, 'HIGH_RATING', 1, 15, 'REVIEW', '2023-01-01', '2024-12-31'),
('월간 활동', 300, 'MONTHLY_ACTIVE', 1, 16, 'MEMBER', '2023-01-01', '2024-12-31'),
('특별 이벤트', 1000, 'SPECIAL_EVENT', 1, 17, 'EVENT', '2023-01-01', '2024-12-31'),
('베타 테스터', 2000, 'BETA_TESTER', 1, 18, 'SYSTEM', '2023-01-01', '2024-12-31'),
('피드백 제공', 150, 'FEEDBACK', 1, 19, 'SYSTEM', '2023-01-01', '2024-12-31'),
('장기 회원', 3000, 'LONG_TERM', 1, 20, 'MEMBER', '2023-01-01', '2024-12-31');

-- 3. 회원 포인트 잔액 (605개 - 모든 회원)
INSERT INTO member_point_balance (member_id, total_points, available_points, pending_points, reserved_points, version)
SELECT 
    member_id,
    FLOOR(100 + (RAND() * 49900)) as total_points,
    FLOOR(50 + (RAND() * 29950)) as available_points,
    FLOOR(0 + (RAND() * 1000)) as pending_points,
    FLOOR(0 + (RAND() * 500)) as reserved_points,
    1 as version
FROM member_tb;

-- 4. 포인트 거래 내역 (1000개)
INSERT INTO point_transaction (member_id, transaction_type, direction, point_amount, balance_before, balance_after, referer_type, referer_id, created_at)
SELECT 
    (FLOOR(1 + (RAND() * 605))) as member_id,
    CASE ROW_NUMBER() OVER () % 8
        WHEN 0 THEN 'REWARD'
        WHEN 1 THEN 'PURCHASE'
        WHEN 2 THEN 'BONUS'
        WHEN 3 THEN 'REFUND'
        WHEN 4 THEN 'MODEL_USAGE'
        WHEN 5 THEN 'COMMISSION'
        WHEN 6 THEN 'WITHDRAWAL'
        ELSE 'EXPIRY'
    END as transaction_type,
    CASE 
        WHEN ROW_NUMBER() OVER () % 8 IN (0, 2, 3, 5) THEN 'CREDIT'
        ELSE 'DEBIT'
    END as direction,
    CASE 
        WHEN ROW_NUMBER() OVER () % 8 IN (0, 2, 3, 5) THEN FLOOR(10 + (RAND() * 500))
        ELSE FLOOR(10 + (RAND() * 1000))
    END as point_amount,
    FLOOR(RAND() * 30000) as balance_before,
    FLOOR(RAND() * 30000) as balance_after,
    CASE ROW_NUMBER() OVER () % 8
        WHEN 0 THEN 'SYSTEM'
        WHEN 1 THEN 'ORDER'
        WHEN 2 THEN 'EVENT'
        WHEN 3 THEN 'ORDER'
        WHEN 4 THEN 'STORE_PURCHASE'
        WHEN 5 THEN 'REFERRAL'
        WHEN 6 THEN 'MANUAL'
        ELSE 'SYSTEM'
    END as referer_type,
    FLOOR(1 + (RAND() * 1000)) as referer_id,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY) as created_at
FROM (
    SELECT ROW_NUMBER() OVER () as n
    FROM information_schema.columns a
    CROSS JOIN information_schema.columns b
    LIMIT 1000
) numbers;

-- 5. 쿠폰 데이터 (30개)
INSERT INTO coupon (coupon_code, name, discount_type, discount_value, status, valid_from, valid_until, used_by, used_at)
SELECT 
    CONCAT('COUP', LPAD(ROW_NUMBER() OVER (), 4, '0')) as coupon_code,
    CONCAT(
        CASE ROW_NUMBER() OVER () % 5
            WHEN 0 THEN '신규회원 환영'
            WHEN 1 THEN '시즌 특별'
            WHEN 2 THEN '리뷰 작성 감사'
            WHEN 3 THEN 'VIP 회원'
            ELSE '이벤트 참여'
        END,
        ' 쿠폰 #', ROW_NUMBER() OVER ()
    ) as name,
    CASE WHEN ROW_NUMBER() OVER () % 2 = 0 THEN 'PERCENTAGE' ELSE 'FIXED_AMOUNT' END as discount_type,
    CASE 
        WHEN ROW_NUMBER() OVER () % 2 = 0 THEN (5 + FLOOR(RAND() * 25))
        ELSE (5 + FLOOR(RAND() * 20))
    END as discount_value,
    CASE WHEN RAND() < 0.3 THEN 'USED' ELSE 'ACTIVE' END as status,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY) as valid_from,
    DATE_ADD(NOW(), INTERVAL (30 + FLOOR(RAND() * 60)) DAY) as valid_until,
    CASE WHEN RAND() < 0.3 THEN FLOOR(2 + (RAND() * 604)) ELSE NULL END as used_by,
    CASE WHEN RAND() < 0.3 THEN DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY) ELSE NULL END as used_at
FROM (
    SELECT ROW_NUMBER() OVER () as n
    FROM information_schema.columns
    LIMIT 30
) numbers;

-- 6. 할인 정책 데이터 (20개)
INSERT INTO discount_policy (name, discount_type, discount_value, is_active, max_usage_per_user, min_subscription_months, valid_from, valid_until) VALUES
('신규회원 할인', 'PERCENTAGE', 20.00, 1, 1, 0, '2023-01-01', '2024-12-31'),
('여름 시즌 할인', 'PERCENTAGE', 15.00, 0, 1, 0, '2023-06-01', '2023-08-31'),
('구독 업그레이드 할인', 'FIXED_AMOUNT', 10.00, 1, 1, 1, '2023-01-01', '2024-12-31'),
('연말 대형 세일', 'PERCENTAGE', 30.00, 1, 2, 0, '2023-11-01', '2023-12-31'),
('학생 할인', 'PERCENTAGE', 50.00, 1, 1, 0, '2023-01-01', '2024-12-31'),
('VIP 회원 특가', 'PERCENTAGE', 25.00, 1, 5, 6, '2023-01-01', '2024-12-31'),
('첫 구매 할인', 'FIXED_AMOUNT', 5.00, 1, 1, 0, '2023-01-01', '2024-12-31'),
('대량 구매 할인', 'PERCENTAGE', 10.00, 1, 1, 0, '2023-01-01', '2024-12-31'),
('추천인 할인', 'FIXED_AMOUNT', 15.00, 1, 1, 0, '2023-01-01', '2024-12-31'),
('생일축하 할인', 'PERCENTAGE', 20.00, 1, 1, 0, '2023-01-01', '2024-12-31'),
('앱 런칭 기념', 'PERCENTAGE', 35.00, 0, 1, 0, '2023-09-01', '2023-10-31'),
('Black Friday 세일', 'PERCENTAGE', 40.00, 0, 1, 0, '2023-11-24', '2023-11-26'),
('창립 기념일 할인', 'FIXED_AMOUNT', 25.00, 0, 1, 0, '2023-03-15', '2023-03-31'),
('재구매 고객 할인', 'PERCENTAGE', 12.00, 1, 10, 3, '2023-01-01', '2024-12-31'),
('구독자 전용 할인', 'PERCENTAGE', 18.00, 1, 1, 1, '2023-01-01', '2024-12-31'),
('조기 결제 할인', 'FIXED_AMOUNT', 8.00, 1, 1, 0, '2023-01-01', '2024-12-31'),
('단체 구매 할인', 'PERCENTAGE', 22.00, 1, 1, 0, '2023-01-01', '2024-12-31'),
('리뷰 작성 할인', 'FIXED_AMOUNT', 12.00, 1, 1, 0, '2023-01-01', '2024-12-31'),
('긴급 세일', 'PERCENTAGE', 45.00, 0, 1, 0, '2023-08-15', '2023-08-20'),
('플래시 세일', 'PERCENTAGE', 50.00, 0, 1, 0, '2023-12-15', '2023-12-16');

-- 7. 모델 리뷰 데이터 (100개)
INSERT INTO model_review (model_id, reviewer_id, value, content, status, created_at)
WITH RECURSIVE seq(n) AS (
    SELECT 1 
    UNION ALL 
    SELECT n + 1 FROM seq WHERE n < 100
)
SELECT 
    (SELECT model_id FROM ai_model_tb WHERE is_public = b'1' ORDER BY RAND() LIMIT 1) as model_id,
    (FLOOR(2 + (RAND() * 604))) as reviewer_id,
    (FLOOR(1 + (RAND() * 5))) as value,
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
    END as content,
    CASE WHEN RAND() < 0.9 THEN 'ACTIVE' ELSE 'BLOCKED' END as status,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 180) DAY) as created_at
FROM seq;

-- 8. 파일 데이터 (50개)
INSERT INTO file_tb (file_name, file_type, file_url, content_type, relation_type, relation_id, is_primary, created_at, updated_at)
SELECT 
    CONCAT(
        CASE ROW_NUMBER() OVER () % 8
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
        CASE ROW_NUMBER() OVER () % 4
            WHEN 0 THEN '.jpg'
            WHEN 1 THEN '.png'
            WHEN 2 THEN '.webp'
            ELSE '.gif'
        END
    ) as file_name,
    CASE WHEN ROW_NUMBER() OVER () % 2 = 0 THEN 'PREVIEW' ELSE 'THUMBNAIL' END as file_type,
    -- Firebase Storage URLs을 사용 (실제 업로드된 이미지)
    CASE ROW_NUMBER() OVER () % 5
        WHEN 0 THEN 'https://storage.googleapis.com/download/storage/v1/b/nomodel-fdaae.firebasestorage.app/o/testImage_be8eb30c-ce18-438f-b485-593a6ef2dcfe?generation=1756904909282705&alt=media'
        WHEN 1 THEN 'https://storage.googleapis.com/download/storage/v1/b/nomodel-fdaae.firebasestorage.app/o/sample_model_image_001.jpg?generation=1756904909282705&alt=media'
        WHEN 2 THEN 'https://storage.googleapis.com/download/storage/v1/b/nomodel-fdaae.firebasestorage.app/o/generated_art_002.png?generation=1756904909282705&alt=media'
        WHEN 3 THEN 'https://storage.googleapis.com/download/storage/v1/b/nomodel-fdaae.firebasestorage.app/o/custom_thumbnail_003.webp?generation=1756904909282705&alt=media'
        ELSE 'https://storage.googleapis.com/download/storage/v1/b/nomodel-fdaae.firebasestorage.app/o/ai_artwork_default.jpg?generation=1756904909282705&alt=media'
    END as file_url,
    CASE ROW_NUMBER() OVER () % 4
        WHEN 0 THEN 'image/jpeg'
        WHEN 1 THEN 'image/png'
        WHEN 2 THEN 'image/webp'
        ELSE 'image/gif'
    END as content_type,
    CASE ROW_NUMBER() OVER () % 4
        WHEN 0 THEN 'MODEL'
        WHEN 1 THEN 'REVIEW'
        WHEN 2 THEN 'PROFILE'
        ELSE 'AD'
    END as relation_type,
    FLOOR(1 + (RAND() * 1000)) as relation_id,
    -- 각 relation_id의 첫 번째 파일을 대표 이미지로 설정 (약 20% 확률)
    CASE WHEN ROW_NUMBER() OVER (PARTITION BY FLOOR(1 + (RAND() * 1000)) ORDER BY RAND()) = 1 
         AND RAND() < 0.2 THEN TRUE 
         ELSE FALSE 
    END as is_primary,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 180) DAY) as created_at,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY) as updated_at
FROM (
    SELECT ROW_NUMBER() OVER () as n
    FROM information_schema.columns
    LIMIT 50
) numbers;

-- 9. 신고 데이터 (25개) - MySQL syntax 오류 수정
INSERT INTO report_tb (target_type, target_id, report_status, reason_detail, admin_note, created_by, modified_by, created_at, updated_at)
SELECT 
    CASE WHEN ROW_NUMBER() OVER () % 2 = 0 THEN 'MODEL' ELSE 'REVIEW' END as target_type,
    CASE 
        WHEN ROW_NUMBER() OVER () % 2 = 0 THEN (SELECT model_id FROM ai_model_tb WHERE is_public = 1 ORDER BY RAND() LIMIT 1)
        ELSE (SELECT id FROM model_review ORDER BY RAND() LIMIT 1)
    END as target_id,
    CASE ROW_NUMBER() OVER () % 5
        WHEN 0 THEN 'PENDING'
        WHEN 1 THEN 'UNDER_REVIEW'
        WHEN 2 THEN 'RESOLVED'
        WHEN 3 THEN 'ACCEPTED'
        ELSE 'REJECTED'
    END as report_status,
    CASE ROW_NUMBER() OVER () % 6
        WHEN 0 THEN '부적절한 내용이 포함되어 있습니다.'
        WHEN 1 THEN '저작권을 침해하는 내용입니다.'
        WHEN 2 THEN '스팸성 게시물입니다.'
        WHEN 3 THEN '다른 사용자를 괴롭히는 내용입니다.'
        WHEN 4 THEN '잘못된 정보를 제공합니다.'
        ELSE '기타 사유로 신고합니다.'
    END as reason_detail,
    CASE ROW_NUMBER() OVER () % 5
        WHEN 2 THEN '신고 내용을 확인하여 적절한 조치를 취했습니다.'
        WHEN 3 THEN '신고 내용이 타당하여 해당 컨텐츠를 차단했습니다.'
        WHEN 4 THEN '검토 결과 규정 위반 사항을 발견하지 못했습니다.'
        ELSE NULL
    END as admin_note,
    CONCAT('user', FLOOR(2 + (RAND() * 604))) as created_by,
    CASE ROW_NUMBER() OVER () % 5
        WHEN 2 THEN 'admin'
        WHEN 3 THEN 'admin'
        WHEN 4 THEN 'admin'
        ELSE NULL
    END as modified_by,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 90) DAY) as created_at,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 7) DAY) as updated_at
FROM (
    SELECT ROW_NUMBER() OVER () as n
    FROM information_schema.columns
    LIMIT 25
) numbers;

-- 10. 이벤트 발행 데이터 (100개)
INSERT INTO event_publication (id, event_type, listener_id, serialized_event, publication_date, completion_date)
WITH RECURSIVE seq(n) AS (
    SELECT 1 
    UNION ALL 
    SELECT n + 1 FROM seq WHERE n < 100
)
SELECT 
    UNHEX(REPLACE(UUID(), '-', '')) as id,
    CASE n % 8
        WHEN 0 THEN 'MEMBER_REGISTERED'
        WHEN 1 THEN 'MODEL_UPLOADED'
        WHEN 2 THEN 'MODEL_PURCHASED'
        WHEN 3 THEN 'REVIEW_CREATED'
        WHEN 4 THEN 'SUBSCRIPTION_STARTED'
        WHEN 5 THEN 'POINT_EARNED'
        WHEN 6 THEN 'COUPON_USED'
        ELSE 'REPORT_SUBMITTED'
    END as event_type,
    CONCAT('listener-', FLOOR(1 + (RAND() * 10))) as listener_id,
    CONCAT('{"timestamp":"', NOW(), '","userId":', FLOOR(1 + (RAND() * 605)), ',"data":"event_data"}') as serialized_event,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY) as publication_date,
    CASE WHEN RAND() < 0.8 
        THEN DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 29) DAY)
        ELSE NULL 
    END as completion_date
FROM seq;