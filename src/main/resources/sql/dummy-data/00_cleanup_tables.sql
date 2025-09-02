-- ================================================================================
-- 테이블 구조 정리 스크립트
-- ================================================================================
-- 기존 테이블에서 사용하지 않는 컬럼 제거하여 JPA 엔티티와 완전 일치시킴

-- 외래키 체크 비활성화
SET foreign_key_checks = 0;

-- member_tb 테이블 정리
ALTER TABLE member_tb DROP COLUMN reg_time;
ALTER TABLE member_tb DROP COLUMN update_time;

-- ai_model_tb 테이블 정리  
ALTER TABLE ai_model_tb DROP COLUMN reg_time;
ALTER TABLE ai_model_tb DROP COLUMN update_time;

-- model_statistics_tb 테이블 정리
ALTER TABLE model_statistics_tb DROP COLUMN reg_time;
ALTER TABLE model_statistics_tb DROP COLUMN update_time;

-- 외래키 체크 활성화
SET foreign_key_checks = 1;

SELECT '테이블 구조 정리 완료 - BaseTimeEntity 필드만 유지' as status;