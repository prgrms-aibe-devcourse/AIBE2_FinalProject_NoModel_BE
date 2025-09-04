-- ================================================================================
-- V1__add_is_primary_to_file_tb.sql
-- 
-- File 테이블에 대표 이미지 플래그 추가
-- ================================================================================

-- is_primary 컬럼 추가 (기본값 false)
ALTER TABLE file_tb 
ADD COLUMN is_primary BOOLEAN NOT NULL DEFAULT FALSE 
COMMENT '대표 이미지 여부';

-- 인덱스 추가: 대표 이미지 빠른 조회를 위함
CREATE INDEX idx_file_primary ON file_tb(relation_type, relation_id, is_primary);

-- 각 관계당 첫 번째 이미지를 대표 이미지로 설정 (선택적)
-- 기존 데이터가 있는 경우, 각 모델의 첫 번째 썸네일 또는 이미지를 대표로 설정
UPDATE file_tb f1
INNER JOIN (
    SELECT MIN(file_id) as first_file_id, relation_id
    FROM file_tb
    WHERE relation_type = 'MODEL'
    AND content_type LIKE 'image/%'
    GROUP BY relation_id
) f2 ON f1.file_id = f2.first_file_id
SET f1.is_primary = TRUE;

-- 통계 확인 (선택적)
-- SELECT relation_type, COUNT(*) as total, 
--        SUM(CASE WHEN is_primary THEN 1 ELSE 0 END) as primary_count
-- FROM file_tb 
-- GROUP BY relation_type;