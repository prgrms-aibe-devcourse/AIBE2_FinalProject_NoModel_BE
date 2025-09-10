-- 전체 프로젝트 포괄적인 테스트 데이터 생성 스크립트
-- 모든 도메인의 다양한 시나리오를 커버하는 완전한 테스트 데이터셋

-- ===== 1. MEMBER (회원) 도메인 =====
INSERT INTO member (id, username, email, password, role, status, created_at, updated_at, created_by, updated_by) VALUES
-- 기본 사용자들
(1, 'normalUser', 'normal@test.com', '$2a$10$testPasswordHash', 'USER', 'ACTIVE', '2024-01-01 10:00:00', '2024-01-01 10:00:00', 'SYSTEM', 'SYSTEM'),
(2, 'premiumUser', 'premium@test.com', '$2a$10$testPasswordHash', 'USER', 'ACTIVE', '2024-01-02 10:00:00', '2024-01-02 10:00:00', 'SYSTEM', 'SYSTEM'),
(3, 'adminUser', 'admin@test.com', '$2a$10$testPasswordHash', 'ADMIN', 'ACTIVE', '2024-01-03 10:00:00', '2024-01-03 10:00:00', 'SYSTEM', 'SYSTEM'),
(4, 'suspendedUser', 'suspended@test.com', '$2a$10$testPasswordHash', 'USER', 'SUSPENDED', '2024-01-04 10:00:00', '2024-01-04 10:00:00', 'SYSTEM', 'SYSTEM'),
-- 다양한 활동 패턴 사용자들
(5, 'activeCreator', 'creator@test.com', '$2a$10$testPasswordHash', 'USER', 'ACTIVE', '2024-01-05 10:00:00', '2024-01-05 10:00:00', 'SYSTEM', 'SYSTEM'),
(6, 'regularBuyer', 'buyer@test.com', '$2a$10$testPasswordHash', 'USER', 'ACTIVE', '2024-01-06 10:00:00', '2024-01-06 10:00:00', 'SYSTEM', 'SYSTEM'),
(7, 'reviewer', 'reviewer@test.com', '$2a$10$testPasswordHash', 'USER', 'ACTIVE', '2024-01-07 10:00:00', '2024-01-07 10:00:00', 'SYSTEM', 'SYSTEM'),
(8, 'inactiveUser', 'inactive@test.com', '$2a$10$testPasswordHash', 'USER', 'ACTIVE', '2024-01-08 10:00:00', '2024-01-08 10:00:00', 'SYSTEM', 'SYSTEM');

-- ===== 2. SUBSCRIPTION (구독) 도메인 =====
INSERT INTO subscription (id, plan_type, plan_name, price, description, duration_months, created_at, updated_at, created_by, updated_by) VALUES
(1, 'BASIC', 'Basic Plan', 9.99, 'Basic monthly subscription', 1, '2024-01-01 00:00:00', '2024-01-01 00:00:00', 'SYSTEM', 'SYSTEM'),
(2, 'PREMIUM', 'Premium Plan', 19.99, 'Premium monthly subscription', 1, '2024-01-01 00:00:00', '2024-01-01 00:00:00', 'SYSTEM', 'SYSTEM'),
(3, 'PRO', 'Pro Plan', 49.99, 'Professional monthly subscription', 1, '2024-01-01 00:00:00', '2024-01-01 00:00:00', 'SYSTEM', 'SYSTEM'),
(4, 'PREMIUM', 'Premium Annual', 199.99, 'Premium yearly subscription', 12, '2024-01-01 00:00:00', '2024-01-01 00:00:00', 'SYSTEM', 'SYSTEM');

-- 회원 구독 관계
INSERT INTO member_subscription (id, member_id, subscription_id, status, start_date, end_date, auto_renewal, created_at, updated_at, created_by, updated_by) VALUES
(1, 2, 2, 'ACTIVE', '2024-01-15 00:00:00', '2024-02-15 00:00:00', true, '2024-01-15 10:00:00', '2024-01-15 10:00:00', 'premiumUser', 'premiumUser'),
(2, 5, 3, 'ACTIVE', '2024-01-10 00:00:00', '2024-02-10 00:00:00', true, '2024-01-10 10:00:00', '2024-01-10 10:00:00', 'activeCreator', 'activeCreator'),
(3, 6, 4, 'EXPIRED', '2023-12-01 00:00:00', '2023-12-31 00:00:00', false, '2023-12-01 10:00:00', '2023-12-01 10:00:00', 'regularBuyer', 'regularBuyer');

-- ===== 3. POINT (포인트) 도메인 =====
INSERT INTO point_policy (id, policy_type, points, description, is_active, created_at, updated_at, created_by, updated_by) VALUES
(1, 'SIGN_UP', 1000, '회원가입 보너스', true, '2024-01-01 00:00:00', '2024-01-01 00:00:00', 'SYSTEM', 'SYSTEM'),
(2, 'DAILY_LOGIN', 10, '일일 로그인 보너스', true, '2024-01-01 00:00:00', '2024-01-01 00:00:00', 'SYSTEM', 'SYSTEM'),
(3, 'MODEL_PURCHASE', 50, '모델 구매 적립', true, '2024-01-01 00:00:00', '2024-01-01 00:00:00', 'SYSTEM', 'SYSTEM'),
(4, 'MODEL_UPLOAD', 100, '모델 업로드 보너스', true, '2024-01-01 00:00:00', '2024-01-01 00:00:00', 'SYSTEM', 'SYSTEM'),
(5, 'REVIEW_WRITE', 25, '리뷰 작성 보너스', true, '2024-01-01 00:00:00', '2024-01-01 00:00:00', 'SYSTEM', 'SYSTEM');

-- 회원 포인트 잔액
INSERT INTO member_point_balance (id, member_id, current_balance, total_earned, total_spent, created_at, updated_at, created_by, updated_by) VALUES
(1, 1, 1500, 2000, 500, '2024-01-01 10:00:00', '2024-01-10 15:30:00', 'SYSTEM', 'SYSTEM'),
(2, 2, 2500, 3000, 500, '2024-01-02 10:00:00', '2024-01-11 16:30:00', 'SYSTEM', 'SYSTEM'),
(3, 5, 3200, 4000, 800, '2024-01-05 10:00:00', '2024-01-12 14:20:00', 'SYSTEM', 'SYSTEM'),
(4, 6, 800, 1500, 700, '2024-01-06 10:00:00', '2024-01-13 12:15:00', 'SYSTEM', 'SYSTEM'),
(5, 7, 1200, 1500, 300, '2024-01-07 10:00:00', '2024-01-14 09:45:00', 'SYSTEM', 'SYSTEM');

-- 포인트 거래 내역
INSERT INTO point_transaction (id, member_id, transaction_type, transaction_direction, points, balance_after, description, referer_type, referer_id, created_at, created_by) VALUES
-- 가입 보너스
(1, 1, 'SIGN_UP', 'EARNED', 1000, 1000, '회원가입 보너스', 'POLICY', 1, '2024-01-01 10:00:00', 'SYSTEM'),
(2, 2, 'SIGN_UP', 'EARNED', 1000, 1000, '회원가입 보너스', 'POLICY', 1, '2024-01-02 10:00:00', 'SYSTEM'),
-- 모델 업로드 보너스
(3, 5, 'MODEL_UPLOAD', 'EARNED', 100, 1100, '모델 업로드 보너스', 'MODEL', 1, '2024-01-05 11:00:00', 'SYSTEM'),
-- 구매 적립
(4, 6, 'MODEL_PURCHASE', 'EARNED', 50, 1050, '모델 구매 적립', 'MODEL', 1, '2024-01-06 14:30:00', 'SYSTEM'),
-- 포인트 사용
(5, 1, 'MODEL_PURCHASE', 'SPENT', 500, 1500, '모델 구매 결제', 'MODEL', 2, '2024-01-10 15:30:00', 'normalUser');

-- ===== 4. COUPON (쿠폰) 도메인 =====
INSERT INTO coupon (id, coupon_code, coupon_name, discount_type, discount_value, min_purchase_amount, max_discount_amount, usage_limit, used_count, start_date, end_date, status, created_at, updated_at, created_by, updated_by) VALUES
(1, 'WELCOME2024', '신규 가입 환영 쿠폰', 'PERCENTAGE', 20.00, 10.00, 50.00, 1000, 25, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 'ACTIVE', '2024-01-01 00:00:00', '2024-01-01 00:00:00', 'SYSTEM', 'SYSTEM'),
(2, 'SAVE5000', '5천원 할인 쿠폰', 'FIXED', 5000.00, 20000.00, 5000.00, 500, 120, '2024-01-15 00:00:00', '2024-03-15 23:59:59', 'ACTIVE', '2024-01-15 00:00:00', '2024-01-15 00:00:00', 'SYSTEM', 'SYSTEM'),
(3, 'PREMIUM30', '프리미엄 30% 할인', 'PERCENTAGE', 30.00, 30.00, 100.00, 100, 45, '2024-02-01 00:00:00', '2024-02-29 23:59:59', 'ACTIVE', '2024-02-01 00:00:00', '2024-02-01 00:00:00', 'SYSTEM', 'SYSTEM'),
(4, 'EXPIRED2023', '만료된 쿠폰', 'PERCENTAGE', 15.00, 10.00, 30.00, 200, 180, '2023-12-01 00:00:00', '2023-12-31 23:59:59', 'EXPIRED', '2023-12-01 00:00:00', '2023-12-01 00:00:00', 'SYSTEM', 'SYSTEM');

-- ===== 5. AI_MODEL (AI 모델) 도메인 =====
INSERT INTO ai_model (id, model_name, own_type, owner_id, price, is_public, view_count, download_count, 
                      seed, prompt, negative_prompt, width, height, steps, sampler_index, n_iter, batch_size,
                      created_at, updated_at, created_by, updated_by) VALUES
-- 공개 모델들
(1, 'Anime Style V2', 'USER', 5, 15.00, true, 1250, 89, 
 1234567, 'anime style, high quality, detailed', 'blurry, low quality', 512, 512, 20, 'EULER_A', 1, 1,
 '2024-01-05 11:00:00', '2024-01-05 11:00:00', 'activeCreator', 'activeCreator'),
(2, 'Realistic Portrait', 'USER', 5, 25.00, true, 980, 67, 
 2345678, 'realistic portrait, professional photography', 'cartoon, anime', 768, 768, 25, 'DPM_PLUS_PLUS_2M', 1, 1,
 '2024-01-06 14:00:00', '2024-01-06 14:00:00', 'activeCreator', 'activeCreator'),
(3, 'Fantasy Landscape', 'USER', 2, 20.00, true, 756, 45, 
 3456789, 'fantasy landscape, magical, ethereal', 'modern, urban', 1024, 512, 30, 'LMS_KARRAS', 1, 1,
 '2024-01-07 09:30:00', '2024-01-07 09:30:00', 'premiumUser', 'premiumUser'),
-- 비공개 모델들  
(4, 'Private Experimental', 'USER', 5, 50.00, false, 12, 2, 
 4567890, 'experimental style, work in progress', 'finished, commercial', 512, 512, 15, 'HEUN', 1, 1,
 '2024-01-08 16:20:00', '2024-01-08 16:20:00', 'activeCreator', 'activeCreator'),
-- 시스템 모델들
(5, 'Default Base Model', 'SYSTEM', null, 0.00, true, 5000, 1200, 
 1111111, 'high quality, detailed', 'low quality, blurry', 512, 512, 20, 'EULER_A', 1, 1,
 '2024-01-01 00:00:00', '2024-01-01 00:00:00', 'SYSTEM', 'SYSTEM'),
(6, 'Premium Base Model', 'SYSTEM', null, 0.00, true, 3500, 800, 
 2222222, 'premium quality, ultra detailed', 'low quality, artifacts', 768, 768, 30, 'DPM_PLUS_PLUS_2M_KARRAS', 1, 1,
 '2024-01-01 00:00:00', '2024-01-01 00:00:00', 'SYSTEM', 'SYSTEM');

-- ===== 6. FILE (파일) 도메인 =====
INSERT INTO file (id, original_name, stored_name, file_path, file_size, file_type, relation_type, relation_id, created_at, updated_at, created_by, updated_by) VALUES
-- AI 모델 관련 파일들
(1, 'anime_v2_preview.jpg', 'model_1_preview_20240105.jpg', '/uploads/models/previews/', 245760, 'IMAGE', 'MODEL_PREVIEW', 1, '2024-01-05 11:05:00', '2024-01-05 11:05:00', 'activeCreator', 'activeCreator'),
(2, 'anime_v2_sample1.png', 'model_1_sample1_20240105.png', '/uploads/models/samples/', 1024000, 'IMAGE', 'MODEL_SAMPLE', 1, '2024-01-05 11:10:00', '2024-01-05 11:10:00', 'activeCreator', 'activeCreator'),
(3, 'anime_v2_sample2.png', 'model_1_sample2_20240105.png', '/uploads/models/samples/', 987654, 'IMAGE', 'MODEL_SAMPLE', 1, '2024-01-05 11:15:00', '2024-01-05 11:15:00', 'activeCreator', 'activeCreator'),
(4, 'realistic_preview.jpg', 'model_2_preview_20240106.jpg', '/uploads/models/previews/', 356789, 'IMAGE', 'MODEL_PREVIEW', 2, '2024-01-06 14:05:00', '2024-01-06 14:05:00', 'activeCreator', 'activeCreator'),
-- 프로필 이미지들
(5, 'profile_pic.jpg', 'member_5_profile_20240105.jpg', '/uploads/profiles/', 128000, 'IMAGE', 'MEMBER_PROFILE', 5, '2024-01-05 10:30:00', '2024-01-05 10:30:00', 'activeCreator', 'activeCreator'),
(6, 'admin_avatar.png', 'member_3_profile_20240103.png', '/uploads/profiles/', 95000, 'IMAGE', 'MEMBER_PROFILE', 3, '2024-01-03 10:30:00', '2024-01-03 10:30:00', 'adminUser', 'adminUser');

-- ===== 7. REVIEW (리뷰) 도메인 =====
INSERT INTO model_review (id, model_id, reviewer_id, rating, review_title, review_content, status, created_at, updated_at, created_by, updated_by) VALUES
(1, 1, 6, 5, '훌륭한 애니메이션 스타일!', '정말 마음에 듭니다. 디테일이 살아있고 색감이 예술적이에요. 강력 추천합니다!', 'ACTIVE', '2024-01-08 10:30:00', '2024-01-08 10:30:00', 'regularBuyer', 'regularBuyer'),
(2, 1, 7, 4, '좋은 품질이지만...', '전체적으로 만족하지만 조금 더 다양한 포즈가 있으면 좋겠어요.', 'ACTIVE', '2024-01-09 14:20:00', '2024-01-09 14:20:00', 'reviewer', 'reviewer'),
(3, 2, 6, 5, '사실적인 인물 표현 최고!', '인물의 표정과 질감이 정말 리얼합니다. 프로페셔널한 작품에 사용하기 좋아요.', 'ACTIVE', '2024-01-10 09:15:00', '2024-01-10 09:15:00', 'regularBuyer', 'regularBuyer'),
(4, 3, 7, 3, '평범한 수준', '나쁘지 않지만 특별함이 부족해요. 가격 대비 조금 아쉽네요.', 'ACTIVE', '2024-01-11 16:45:00', '2024-01-11 16:45:00', 'reviewer', 'reviewer'),
(5, 1, 1, 5, '정말 추천해요!', '초보자도 쉽게 사용할 수 있고 결과물이 만족스럽습니다.', 'ACTIVE', '2024-01-12 11:30:00', '2024-01-12 11:30:00', 'normalUser', 'normalUser');

-- ===== 8. REPORT (신고) 도메인 =====
INSERT INTO report (id, target_type, target_id, reporter_id, report_reason, report_description, status, created_at, updated_at, created_by, updated_by) VALUES
(1, 'MODEL', 4, 6, '부적절한 콘텐츠', '이 모델이 부적절한 내용을 생성하는 것 같습니다.', 'PENDING', '2024-01-13 14:30:00', '2024-01-13 14:30:00', 'regularBuyer', 'regularBuyer'),
(2, 'REVIEW', 4, 2, '스팸/광고', '리뷰 내용이 광고성 게시물처럼 보입니다.', 'REVIEWED', '2024-01-14 09:45:00', '2024-01-15 10:30:00', 'premiumUser', 'adminUser'),
(3, 'MODEL', 1, 8, '저작권 침해', '이 모델이 기존 작품을 무단으로 사용한 것 같습니다.', 'REJECTED', '2024-01-15 16:20:00', '2024-01-16 09:15:00', 'inactiveUser', 'adminUser');

-- AUTO_INCREMENT 시퀀스 조정
ALTER TABLE member AUTO_INCREMENT = 10;
ALTER TABLE subscription AUTO_INCREMENT = 10;
ALTER TABLE member_subscription AUTO_INCREMENT = 10;
ALTER TABLE point_policy AUTO_INCREMENT = 10;
ALTER TABLE member_point_balance AUTO_INCREMENT = 10;
ALTER TABLE point_transaction AUTO_INCREMENT = 10;
ALTER TABLE coupon AUTO_INCREMENT = 10;
ALTER TABLE ai_model AUTO_INCREMENT = 10;
ALTER TABLE file AUTO_INCREMENT = 10;
ALTER TABLE model_review AUTO_INCREMENT = 10;
ALTER TABLE report AUTO_INCREMENT = 10;