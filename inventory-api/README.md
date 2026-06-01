# Inventory API (사내 자산/재고 관리 시스템)

쿠버네티스 환경 실습을 위한 Spring Boot 기반의 샘플 백엔드 프로젝트입니다.

## 기술 스택
- **Framework**: Spring Boot 3.2.5
- **Language**: Java 17
- **Database**: MariaDB
- **Migration**: Flyway
- **Build Tool**: Gradle

## 프로젝트 구조
- `com.example.inventory.entity`: JPA 엔티티 (Item)
- `com.example.inventory.repository`: Spring Data JPA 인터페이스
- `com.example.inventory.service`: 비즈니스 로직 및 트랜잭션 관리
- `com.example.inventory.controller`: REST API 엔드포인트

## 주요 API 엔드포인트 (V1)
- `GET /api/v1/items`: 전체 자산 목록 조회
- `GET /api/v1/items/{id}`: 특정 자산 상세 조회
- `POST /api/v1/items`: 신규 자산 등록
- `PUT /api/v1/items/{id}`: 자산 정보 수정
- `DELETE /api/v1/items/{id}`: 자산 삭제

## Documentation
Detailed documentation for this project can be found in the [docs](./docs) directory:
- [Development Environment Setup](./docs/development-environment.md): Telepresence, Docker, and Testcontainers.
- [API Server Standard Rules](./docs/common/api-server-standard-rules.md): Master blueprint for all API projects.
- [Engineering Standards](./docs/common/engineering-standards.md): TDD, DDD, and Clean Architecture principles.
- [API-First Workflow](./docs/api-first-workflow.md): OpenAPI Generator and Delegate pattern.

## Quick Start Guide

### 1. Database Connectivity (Telepresence)
```bash
telepresence connect
SPRING_PROFILES_ACTIVE=k8s ./gradlew bootRun
```

### 2. API Documentation (Swagger-UI)
- **Swagger-UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Build & Containerization

### Jar 빌드
```bash
./gradlew clean build
```

### Docker 이미지 빌드
```bash
docker build -t inventory-api:latest .
```

## 프로젝트 로드맵
현재 프로젝트의 진행 상황과 향후 계획은 [GEMINI.md](./GEMINI.md)에서 확인하실 수 있습니다.
