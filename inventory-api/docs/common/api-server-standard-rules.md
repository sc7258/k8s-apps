# API Server Standard Rules (공통 개발 규격)

이 문서는 모든 API 서버 개발 시 준수해야 할 공통 아키텍처 및 개발 표준을 정의합니다.

## 1. API-First 개발 워크플로우
*   **Single Source of Truth**: 모든 API 설계는 `openapi/*.yaml` 명세서에서 시작한다.
*   **코드 자동 생성**: `openapi-generator`를 사용하여 DTO(Model)와 API 인터페이스를 생성한다.
*   **Delegate 패턴 사용**: 생성된 컨트롤러는 수정하지 않으며, `*ApiDelegate` 인터페이스의 구현체(DelegateImpl)에만 비즈니스 로직을 작성한다.

## 2. 계층형 아키텍처 (Clean Architecture)
*   **도메인 중심 설계**: 도메인 엔티티는 외부 프레임워크나 DB 기술에 의존하지 않는 순수 자바 코드로 유지한다 (Rich Domain Model).
*   **의존성 역전 (DIP)**: 고수준 정책(도메인)이 저수준 세부사항(DB, API 인프라)에 의존하지 않도록 구성한다.
*   **캡슐화**: 엔티티의 `@Setter` 사용을 금지하며, 의미 있는 메서드명을 통해 상태를 변경한다.

## 3. 에러 처리 표준 (Standardized Error Handling)
*   **공통 응답 구조**: 모든 에러는 아래의 JSON 구조로 통일한다.
    ```json
    {
      "errorCode": "커스텀 에러 코드 (예: I001)",
      "errorMessage": "에러 요약 메시지 (English)",
      "errorDetails": [ { "field": "...", "value": "...", "reason": "..." } ]
    }
    ```
*   **비즈니스 코드**: HTTP 상태 코드와 별개로, 비즈니스 의미를 담은 alphanumeric 코드(I001, C001 등)를 정의하여 사용한다.
*   **전역 예외 처리**: `@RestControllerAdvice`를 통해 유효성 검사 실패(`@Valid`) 등을 포함한 모든 예외를 공통 포맷으로 변환한다.

## 4. 테스트 자동화 (TDD & Testcontainers)
*   **Test-First**: 기능 구현 전 실패하는 통합 테스트 코드를 먼저 작성한다.
*   **실환경 일치**: 테스트 시 H2 대신 실제 DB 엔진(MariaDB 등)을 사용하는 **Testcontainers**를 필수적으로 활용한다.
*   **@ServiceConnection**: Spring Boot 3.1+의 기능을 활용하여 컨테이너와 애플리케이션의 설정을 자동으로 연동한다.
*   **계약 검증**: `MockMvc`를 사용하여 API 호출 결과가 OpenAPI 명세와 일치하는지 철저히 검증한다.

## 5. 도구 및 설정 규칙
*   **Lombok**: `@Getter`, `@RequiredArgsConstructor`, `@Builder` 등을 적극 활용하여 보일러플레이트를 제거한다.
*   **Swagger-UI**: 소스코드 기반 생성이 아닌, 원본 `openapi.yaml`을 직접 읽어오도록 설정하여 문서의 정확성을 보장한다.
*   **Gradle 자동화**: 빌드 시 OpenAPI Spec 파일이 자동으로 정적 리소스 경로로 복사되도록 구성한다.
