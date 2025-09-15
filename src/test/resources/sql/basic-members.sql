-- 기본 회원 데이터만 (가벼운 테스트용)
INSERT INTO member (id, username, email, password, role, status, created_at, updated_at, created_by, updated_by) VALUES
(1, 'testUser', 'test@example.com', '$2a$10$testPasswordHash', 'USER', 'ACTIVE', NOW(), NOW(), 'SYSTEM', 'SYSTEM'),
(2, 'adminUser', 'admin@example.com', '$2a$10$testPasswordHash', 'ADMIN', 'ACTIVE', NOW(), NOW(), 'SYSTEM', 'SYSTEM');

ALTER TABLE member AUTO_INCREMENT = 10;