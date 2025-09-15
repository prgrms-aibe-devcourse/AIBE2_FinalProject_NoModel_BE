-- 기본 회원 데이터만 (가벼운 테스트용)
INSERT INTO member (id, username, email, password, role, status, created_at, updated_at, created_by, updated_by) VALUES
(1, 'normalUser', 'normal@test.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER', 'ACTIVE', NOW(), NOW(), 'SYSTEM', 'SYSTEM'),
(2, 'adminUser', 'admin@test.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'ADMIN', 'ACTIVE', NOW(), NOW(), 'SYSTEM', 'SYSTEM'),
(3, 'suspendedUser', 'suspended@test.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER', 'SUSPENDED', NOW(), NOW(), 'SYSTEM', 'SYSTEM');

ALTER TABLE member AUTO_INCREMENT = 10;