-- file_type 컬럼 길이 증가로 RESULT 값 저장 가능하도록 수정
ALTER TABLE file_tb
    MODIFY COLUMN file_type VARCHAR(20) NOT NULL;
