# Project Context: Inventory API

이 파일은 Gemini CLI가 프로젝트를 이해하고 관리하기 위한 지침을 담고 있습니다.

## 개발 가이드라인
- **Architecture**: API-First, Clean Architecture (DIP), and Delegate Pattern을 준수합니다.
- **Domain (DDD)**: Rich Domain Model을 지향하며, Entity 내부에서 비즈니스 로직을 처리합니다.
- **Testing (TDD)**: 모든 작업은 실패하는 테스트(Testcontainers 기반) 작성부터 시작합니다.
- **Standards Document**: 상세 내용은 [docs/engineering-standards.md](./docs/engineering-standards.md)를 참고합니다.

## 인프라 및 배포 전략
- **Containerization**: JRE 17 이미지를 기반으로 Docker 이미지를 빌드합니다.
- **K8s Integration**: ConfigMap을 통해 DB 접속 정보를 주입받고, Sealed Secrets를 사용하여 민감한 정보를 관리합니다.
- **CI/CD**: Argo CD를 통한 GitOps 배포 프로세스를 따릅니다.

## 진행 상황
- [x] 프로젝트 기초 구조 생성 (Maven, Spring Boot)
- [x] DB 스키마 설계 및 Flyway 마이그레이션 스크립트 작성
- [x] API-First 개발 환경 구축 (OpenAPI Generator)
- [x] 핵심 CRUD API 구현 (OpenAPI Spec 기반)
- [x] 통합 테스트 및 에러 처리 표준화 (Testcontainers, Naver-style)
- [x] Dockerfile 작성 및 빌드 확인
- [x] K8s Deployment/Service 매니페스트 작성 (V1 Baseline)
- [ ] 도메인 모델 리팩토링 (Rich Domain Model 적용)

## 🚀 Inventory API Evolution Roadmap

### 📍 V1: Foundation (Current)
**목표**: 기본적인 CRUD 기능 구현 및 데이터베이스 마이그레이션 체계 구축
- [x] Spring Boot + Gradle 프로젝트 초기화
- [x] MariaDB 연동 및 Flyway(V1__init.sql) 적용
- [x] Item 엔티티 및 기본 Repository/Service/Controller 구현
- [x] 기본적인 REST API 제공 (목록, 상세, 등록, 수정, 삭제)

### 📍 V2: Enhanced Logic & Robustness
**목표**: 비즈니스 로직 고도화 및 애플리케이션 안정성 강화
- [ ] **QueryDSL 도입**: 카테고리별 필터링, 자산명 검색 등 복잡한 동적 쿼리 구현
- [ ] **Validation & Exception Handling**: 
    - `@Valid`를 통한 입력 데이터 검증
    - `@RestControllerAdvice` 기반 전역 에러 핸들러 구축
- [x] **API Documentation**: Swagger/OpenAPI UI 연동
- [ ] **Dockerization**: 최적화된 Dockerfile 작성 및 로컬 이미지 빌드

### 📍 V3: Cloud-Native & Security
**목표**: 쿠버네티스 환경 최적화 및 보안/관측성 강화
- [ ] **Observability**: 
    - Spring Boot Actuator 연동
    - Prometheus 메트릭 노출 및 Grafana 대시보드 구성
- [ ] **Security**: 
    - Keycloak 연동을 통한 OAuth2/OIDC 인증 적용
    - API 권한 제어 (관리자/사용자)
- [ ] **K8s Advanced Deployment**:
    - ConfigMap/Secret을 이용한 환경 설정 분리
    - Liveness/Readiness Probe 설정
    - HPA(Auto Scaling) 테스트
