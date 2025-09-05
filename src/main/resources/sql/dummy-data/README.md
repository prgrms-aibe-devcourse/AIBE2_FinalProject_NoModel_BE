# NoModel 프로젝트 더미 데이터

## 📋 개요

NoModel 프로젝트의 모든 테이블에 대한 더미 데이터 생성 스크립트입니다.

- **구조**: 모듈화된 파일 구조
- **총 데이터**: 1,100+ 개 레코드
- **자동 정리**: 실행 전 모든 테이블 자동 정리
- **실제 구조**: 프로젝트의 실제 JPA 엔티티 필드 반영

## 🗂️ 파일 구조

```
dummy-data/
├── dummy_data.sql          # 메인 마스터 스크립트 (전체 실행)
├── 00_cleanup_tables.sql   # 테이블 구조 정리 (BaseTimeEntity 통일)
├── 01_clear_data.sql       # 기존 데이터 정리
├── 02_members.sql          # 회원 데이터 (605개)
├── 03_ai_models.sql        # AI 모델 데이터 (500개)
├── 04_model_statistics.sql # 모델 통계 데이터 (500개)
├── 05_subscriptions.sql    # 구독 상품 데이터 (10개)
├── 06_member_subscriptions.sql # 회원 구독 데이터 (200개)
├── 07_remaining_tables.sql # 나머지 테이블들
└── README.md              # 사용 설명서
```

## 🚀 사용 방법

### IntelliJ Query Console에서 실행 (권장)

1. **Database 연결**: Database 탭에서 MySQL 연결
2. **Query Console 열기**: 연결된 DB 우클릭 → "New" → "Query Console"
3. **마스터 스크립트 실행**: 
   - `dummy_data.sql` 파일 열기
   - 전체 선택 (Ctrl/Cmd + A)
   - 실행 (Ctrl/Cmd + Enter)

### 개별 파일 실행
```sql
-- 0. 테이블 구조 정리 (BaseTimeEntity 통일)
SOURCE 00_cleanup_tables.sql;

-- 1. 기존 데이터 정리
SOURCE 01_clear_data.sql;

-- 2. 회원 데이터 생성
SOURCE 02_members.sql;

-- 3. AI 모델 데이터 생성  
SOURCE 03_ai_models.sql;

-- 4. 모델 통계 데이터 생성
SOURCE 04_model_statistics.sql;

-- 5. 구독 상품 데이터 생성
SOURCE 05_subscriptions.sql;
```

### 터미널에서 실행
```bash
mysql -h 127.0.0.1 -P 3306 -u nomodel -pnomodel nomodel < src/main/resources/sql/dummy-data/dummy_data.sql
```

## 📊 생성되는 데이터

### 🔄 자동 정리 기능
- 실행 전 모든 테이블을 자동으로 비움
- 외래키 참조 순서 고려한 안전한 정리
- 기존 데이터 걱정 없이 실행 가능

### 📈 생성 데이터 상세

#### 1. 회원 (605명)
- **테스트 계정**: 5개 (admin, test, premium, business, suspended)
- **일반 회원**: 500명 (다양한 이름과 상태)
- **이벤트 회원**: 100명 (특정 기간 가입자)

**테스트 계정 정보**:
```
admin@nomodel.com     (ADMIN, ACTIVE)
test@nomodel.com      (USER, ACTIVE)
premium@nomodel.com   (USER, ACTIVE)
business@nomodel.com  (USER, ACTIVE)
suspended@nomodel.com (USER, SUSPENDED)
```
**공통 비밀번호**: `password123`

#### 2. AI 모델 (500개)
- **관리자 모델**: 20개 (다양한 가격대의 공개 모델)
- **사용자 모델**: 480개 (개인 커스텀 모델, 일부 유료)

#### 3. 모델 통계 (500개)
- 모든 AI 모델에 대응하는 사용량/조회수 통계
- 공개/비공개, 무료/유료에 따른 현실적 분포

#### 4. 구독 상품 (10개)
- Basic, Standard, Premium, Enterprise 플랜
- 연간 할인 플랜들
- 특별 한정 상품 (Student, Creator, Legacy)

## 🎯 주요 특징

### ✅ 모듈화된 구조
- 각 테이블별로 독립적인 SQL 파일
- 마스터 스크립트로 전체 실행 관리
- 확장성과 유지보수성 향상

### ✅ 실제 구조 반영
- 실제 JPA 엔티티 필드명 정확히 반영
- `BaseTimeEntity`의 `created_at`, `updated_at` 사용
- `BaseEntity`의 auditing 필드 지원
- MySQL의 `bit(1)` 타입 처리 (`is_public`)
- 실제 enum 값들 사용
- Embedded 객체 (Email, Password) 구조 반영

### ✅ 데이터 품질
- 고유 제약조건 준수 (이메일 중복 방지)
- 현실적인 데이터 분포
- 외래키 참조 관계 유지
- 랜덤하지만 일관성 있는 데이터

### ✅ 사용 편의성
- 모듈화된 파일 구조로 관리 용이
- 실행 전 자동 정리
- 상세한 실행 결과 리포트
- IntelliJ Query Console 최적화

## 📋 실행 결과 예시

```
=== 더미 데이터 생성 완료 ===

데이터 생성 완료 통계:
- 회원수: 605
- 모델수: 500  
- 통계수: 500
- 구독상품수: 10

모델 소유자별 분포:
- ADMIN: 20개, 평균가격: 15.99
- USER: 480개, 평균가격: 25.43

회원 상태별 분포:
- ACTIVE: 540명 (89.3%)
- INACTIVE: 52명 (8.6%)
- SUSPENDED: 13명 (2.1%)

인기 모델 TOP 10:
1. Stable Diffusion v1.5 (사용횟수: 284,592)
2. SDXL 1.0 (사용횟수: 267,834)
...
```

## ⚠️ 주의사항

1. **Docker Volume**: Docker 환경에서 실행 시 데이터가 영구 저장됩니다
2. **자동 정리**: 스크립트 실행 시 기존 데이터가 모두 삭제됩니다
3. **실행 시간**: 대량 데이터 생성으로 1-2분 소요될 수 있습니다
4. **환경 호환성**: MySQL/MariaDB 환경에서 테스트되었습니다

## 🔧 트러블슈팅

### 연결 오류
```bash
# TCP 연결 사용
mysql -h 127.0.0.1 -P 3306 -u nomodel -pnomodel nomodel
```

### 권한 오류
```bash
# Docker 환경에서 MySQL 재시작
docker compose restart mysql
```

### 데이터 확인
```sql
-- 생성된 데이터 확인
SELECT COUNT(*) FROM member_tb;
SELECT COUNT(*) FROM ai_model_tb;
SELECT COUNT(*) FROM model_statistics_tb;
```

---

**🎉 완벽하게 작동하는 모듈화된 구조로 NoModel 프로젝트의 모든 더미 데이터를 간편하게 생성하세요!**