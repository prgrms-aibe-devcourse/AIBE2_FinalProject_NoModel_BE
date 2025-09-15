package com.example.nomodel._core.base;

import com.example.nomodel._core.config.TestOAuth2Config;
import com.example.nomodel._core.config.RestDocsConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 컨트롤러 레이어 단위 테스트를 위한 베이스 클래스
 * @WebMvcTest를 사용하여 컨트롤러만 테스트합니다.
 *
 * 특징:
 * - 컨트롤러 레이어만 로드
 * - 서비스 레이어는 Mock 처리
 * - Spring Security 필터 비활성화 (addFilters = false)
 * - REST Docs 자동 설정
 * - 매우 빠른 실행 속도
 *
 * 사용 시나리오:
 * - 컨트롤러 단위 테스트
 * - API 문서 생성 (REST Docs)
 * - Request/Response 매핑 테스트
 * - 입력값 검증 테스트
 *
 * 예시:
 * ```java
 * @WebMvcTest(controllers = MemberController.class)
 * class MemberControllerTest extends BaseWebMvcTest {
 *     @MockitoBean
 *     private MemberService memberService;
 *
 *     // 컨트롤러만 테스트
 * }
 * ```
 */
@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
@ActiveProfiles("test")
@Import({RestDocsConfiguration.class, TestOAuth2Config.class})
public abstract class BaseWebMvcTest {

    @Autowired
    protected MockMvc mockMvc;
}