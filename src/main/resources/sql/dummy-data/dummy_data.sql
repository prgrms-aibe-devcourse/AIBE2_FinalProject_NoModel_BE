-- ================================================================================
-- NoModel 프로젝트 더미 데이터 생성 마스터 스크립트
-- ================================================================================
-- 모든 테이블에 대한 더미 데이터를 순차적으로 생성합니다.
-- 총 1,100+ 개 레코드 생성 (회원 605개, AI 모델 500개, 통계 500개 등)

-- 설정
SET foreign_key_checks = 0;
SET sql_mode = 'NO_AUTO_VALUE_ON_ZERO';
SET autocommit = 1;

SELECT '========== NoModel 더미 데이터 생성 시작 ==========' as status, NOW() as timestamp;

-- 0단계: 테이블 구조 정리 (BaseTimeEntity 필드로 통일)
SELECT '0단계: 테이블 구조 정리 중...' as status, NOW() as timestamp;
SOURCE 00_cleanup_tables.sql;

-- 1단계: 기존 데이터 정리
SELECT '1단계: 기존 데이터 정리 중...' as status, NOW() as timestamp;
SOURCE 01_clear_data.sql;

-- 2단계: 회원 데이터 생성
SELECT '2단계: 회원 데이터 생성 중... (605개)' as status, NOW() as timestamp;
SOURCE 02_members.sql;

-- 3단계: AI 모델 데이터 생성
SELECT '3단계: AI 모델 데이터 생성 중... (500개)' as status, NOW() as timestamp;
SOURCE 03_ai_models.sql;

-- 4단계: 모델 통계 데이터 생성
SELECT '4단계: 모델 통계 데이터 생성 중... (500개)' as status, NOW() as timestamp;
SOURCE 04_model_statistics.sql;

-- 5단계: 구독 상품 데이터 생성
SELECT '5단계: 구독 상품 데이터 생성 중... (10개)' as status, NOW() as timestamp;
SOURCE 05_subscriptions.sql;

-- 6단계: 구독 상품만 생성 (회원 구독은 7단계에서)
SELECT '6단계: 구독 상품 생성 완료 (10개)' as status, NOW() as timestamp;

-- 7단계: 나머지 테이블 데이터 생성
SELECT '7단계: 나머지 테이블 데이터 생성 중...' as status, NOW() as timestamp;
SELECT '- 회원 구독 (150개)' as sub_status;
SELECT '- 포인트 정책 (20개)' as sub_status;
SELECT '- 포인트 잔액 (605개)' as sub_status;
SELECT '- 포인트 거래 (1000개)' as sub_status;
SELECT '- 쿠폰 (30개)' as sub_status;
SELECT '- 할인 정책 (20개)' as sub_status;
SELECT '- 모델 리뷰 (100개)' as sub_status;  
SELECT '- 파일 (50개)' as sub_status;
SELECT '- 신고 (25개)' as sub_status;
SELECT '- 이벤트 발행 (100개)' as sub_status;
SOURCE 07_remaining_tables.sql;

-- 설정 복원
SET foreign_key_checks = 1;

-- ================================================================================
-- 완료 및 결과 확인
-- ================================================================================
SELECT '========== 더미 데이터 생성 완료 ==========' as status, NOW() as timestamp;

-- 생성된 데이터 통계
SELECT 
    '데이터 생성 완료 통계' as summary,
    (SELECT COUNT(*) FROM member_tb) as 회원수,
    (SELECT COUNT(*) FROM ai_model_tb) as 모델수,
    (SELECT COUNT(*) FROM model_statistics_tb) as 통계수,
    (SELECT COUNT(*) FROM subscription) as 구독상품수,
    (SELECT COUNT(*) FROM member_subscription) as 회원구독수,
    (SELECT COUNT(*) FROM point_policy) as 포인트정책수,
    (SELECT COUNT(*) FROM member_point_balance) as 포인트잔액수,
    (SELECT COUNT(*) FROM point_transaction) as 포인트거래수,
    (SELECT COUNT(*) FROM model_review) as 리뷰수,
    (SELECT COUNT(*) FROM coupon) as 쿠폰수,
    (SELECT COUNT(*) FROM discount_policy) as 할인정책수,
    (SELECT COUNT(*) FROM file_tb) as 파일수,
    (SELECT COUNT(*) FROM report_tb) as 신고수,
    (SELECT COUNT(*) FROM event_publication) as 이벤트수;

-- 모델 소유자별 분포
SELECT 
    '모델 소유자별 분포' as category,
    own_type as 소유자타입,
    COUNT(*) as 개수,
    ROUND(AVG(CASE WHEN price > 0 THEN price END), 2) as 평균가격
FROM ai_model_tb 
GROUP BY own_type;

-- 회원 상태별 분포  
SELECT 
    '회원 상태별 분포' as category,
    status as 상태,
    COUNT(*) as 개수,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM member_tb), 1) as 비율
FROM member_tb 
GROUP BY status
ORDER BY 개수 DESC;

-- 인기 모델 TOP 10
SELECT 
    '인기 모델 TOP 10' as category,
    m.model_name as 모델명,
    m.own_type as 소유자,
    ms.usage_count as 사용횟수,
    ms.view_count as 조회수,
    m.price as 가격
FROM model_statistics_tb ms
JOIN ai_model_tb m ON ms.model_id = m.model_id
ORDER BY ms.usage_count DESC
LIMIT 10;

SELECT '🎉 NoModel 더미 데이터 생성이 성공적으로 완료되었습니다!' as final_message;