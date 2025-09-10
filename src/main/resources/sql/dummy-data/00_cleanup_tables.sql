-- ================================================================================
-- 테이블 초기화 스크립트
-- ================================================================================
-- 모든 테이블을 DROP하여 JPA가 새로 생성하도록 함

-- 외래키 체크 비활성화
SET foreign_key_checks = 0;

-- 모든 테이블 DROP (외래키 순서 고려)
DROP TABLE IF EXISTS model_statistics_tb;
DROP TABLE IF EXISTS member_subscription;
DROP TABLE IF EXISTS member_point_balance;
DROP TABLE IF EXISTS point_transaction;
DROP TABLE IF EXISTS point_policy;
DROP TABLE IF EXISTS member_coupon;
DROP TABLE IF EXISTS coupon;
DROP TABLE IF EXISTS discount_policy;
DROP TABLE IF EXISTS model_review;
DROP TABLE IF EXISTS model_report;
DROP TABLE IF EXISTS file_tb;
DROP TABLE IF EXISTS event_outbox;
DROP TABLE IF EXISTS refresh_token;
DROP TABLE IF EXISTS ai_model_tb;
DROP TABLE IF EXISTS subscription;
DROP TABLE IF EXISTS member_tb;

-- 외래키 체크 활성화
SET foreign_key_checks = 1;

SELECT '모든 테이블 DROP 완료 - JPA가 새로 생성함' as status;