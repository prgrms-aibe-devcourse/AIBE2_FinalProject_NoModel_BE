-- 테스트 후 데이터 정리 스크립트 (필요시 사용)
-- 주의: @Transactional이 있으면 자동 롤백되므로 보통 불필요
DELETE FROM report;
DELETE FROM model_review;
DELETE FROM file;
DELETE FROM ai_model;
DELETE FROM point_transaction;
DELETE FROM member_point_balance;
DELETE FROM point_policy;
DELETE FROM coupon;
DELETE FROM member_subscription;
DELETE FROM subscription;
DELETE FROM member;