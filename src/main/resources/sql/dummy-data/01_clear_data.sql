-- ================================================================================
-- 기존 데이터 정리 스크립트
-- ================================================================================
-- 모든 테이블의 데이터를 안전하게 정리하고 AUTO_INCREMENT 초기화

-- 외래키 참조 순서에 따라 역순으로 정리
SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

-- 외래키가 있는 테이블부터 순서대로 삭제
DELETE FROM member_subscription WHERE 1=1;
DELETE FROM model_statistics_tb WHERE 1=1;
DELETE FROM ai_model_tb WHERE 1=1;
DELETE FROM member_tb WHERE 1=1;
DELETE FROM subscription WHERE 1=1;

-- AUTO_INCREMENT 리셋
ALTER TABLE member_tb AUTO_INCREMENT = 1;
ALTER TABLE ai_model_tb AUTO_INCREMENT = 1;
ALTER TABLE model_statistics_tb AUTO_INCREMENT = 1;
ALTER TABLE subscription AUTO_INCREMENT = 1;
ALTER TABLE member_subscription AUTO_INCREMENT = 1;

SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

SELECT '기존 데이터 정리 완료' as status;