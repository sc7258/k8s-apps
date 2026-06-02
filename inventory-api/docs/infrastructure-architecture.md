# 인프라 아키텍처 배포 및 관리 전략

본 문서는 다중 환경(`local`, `dev`, `qa`, `prod`)을 위한 쿠버네티스 인프라 구축 시, 도구(Harbor, Argo CD, CI/CD, 모니터링)들의 배치 전략과 이미지 프로모션(승격)에 대한 가이드를 제공합니다.

---

## 1. 아키텍처 비교 개요

### 1.1. 분산형 모델 (Decentralized - 현재 구상 중인 방식)
각 환경(클러스터)마다 애플리케이션 파드와 인프라 도구(Harbor, Argo CD, Grafana 등)를 모두 설치하는 방식입니다.

*   **장점**: 완벽한 물리적 격리로 인해 한 클러스터의 장애가 다른 클러스터에 영향을 주지 않습니다.
*   **단점 (한계점)**:
    *   **막대한 리소스 낭비**: 각 클러스터마다 무거운 인프라 툴이 동작하여 서버 비용이 기하급수적으로 증가합니다.
    *   **운영 복잡도**: 관리자가 4개의 Argo CD, 4개의 Harbor, 4개의 Grafana 대시보드를 따로 접속하고 관리해야 합니다.
    *   **이미지 무결성 훼손**: QA에서 테스트한 이미지를 Prod로 넘길 때, 이미지를 다시 빌드하거나 다른 Harbor로 복사해야 하는 번거로움과 리스크가 발생합니다.

### 1.2. 중앙 집중형 모델 (Hub and Spoke - 엔터프라이즈 실무 표준)
단 하나의 **관리용 클러스터(Management Cluster)**에만 모든 인프라 도구를 설치하고, 나머지 **타겟 클러스터(Dev, QA, Prod)**에는 순수하게 비즈니스 애플리케이션(Inventory API 등)만 배포하는 방식입니다.

*   **장점**: 리소스 최적화, 단일 통제 지점(Single Pane of Glass) 확보, 배포 파이프라인의 일관성 유지.
*   **단점**: 관리 클러스터에 장애가 발생하면 전체 배포 파이프라인이 멈출 수 있으므로, 관리 클러스터 자체의 고가용성(HA) 구성이 필수적입니다.

---

## 2. 중앙 집중형(Hub and Spoke) 구성도

```text
[ Management Cluster (관리용) ]
 ├── Harbor (단일 중앙 레지스트리)
 ├── Argo CD (중앙 배포 지휘관)
 ├── GitLab Runner / Jenkins (통합 CI)
 └── Grafana (통합 모니터링 대시보드)
       │
       │ (Argo CD가 Kubeconfig를 통해 각 클러스터에 원격 배포)
       │
       ├──▶ [ Dev Cluster ]   (오직 Inventory API Dev 파드만 실행)
       ├──▶ [ QA Cluster ]    (오직 Inventory API QA 파드만 실행)
       └──▶ [ Prod Cluster ]  (오직 Inventory API Prod 파드만 실행)
```

---

## 3. 핵심 전략: QA ➔ Prod 이미지 승격 (Image Promotion)

"QA 환경의 이미지가 무분별하게 Prod로 배포되는 것을 막아야 한다"는 보안 요구사항은 Harbor를 여러 개 두는 것이 아니라, **단일 Harbor 내의 프로젝트 분리와 RBAC(역할 기반 접근 제어)**를 통해 해결합니다.

### 3.1. Harbor 내부의 논리적 격리 (Projects)
하나의 Harbor에 다음과 같이 환경별 프로젝트(디렉토리)를 생성합니다.

1.  `dev-images` 프로젝트: 개발자 및 CI 서버가 자유롭게 Push/Pull 가능
2.  `qa-images` 프로젝트: QA 담당자와 특정 파이프라인만 Push 가능
3.  **`prod-images` 프로젝트 (가장 중요)**: 
    *   **사람이나 일반 CI 서버는 Push 절대 불가 (Read-Only)**
    *   Prod 클러스터는 이곳에서만 이미지를 Pull 할 수 있음.

### 3.2. 올바른 배포 워크플로우 (Build Once, Run Anywhere)
1.  **빌드**: 소스코드는 최초 1회만 빌드되어 `dev-images/inventory-api:v1.0.0`으로 저장됩니다.
2.  **QA 검증**: QA 클러스터는 이 이미지를 가져다 테스트를 진행합니다.
3.  **승격 (Promotion)**: QA 테스트가 완벽히 통과되면, 관리자 승인 하에 Harbor의 **이미지 복제(Replication/Retagging) 기능**을 사용하여 해당 이미지를 그대로 `prod-images/inventory-api:v1.0.0`으로 복사(승격)합니다. (다시 빌드하지 않습니다!)
4.  **운영 배포**: Prod 환경의 Kustomize(`overlays/prod/kustomization.yaml`)는 항상 `prod-images` 경로만 바라보도록 설정되어 있으므로, 승격된 이미지를 안전하게 Prod 클러스터에 배포합니다.

---

## 4. 인프라 도구별 통합 방안

*   **Argo CD**: Management Cluster에 하나만 띄우고, Settings > Clusters 메뉴에서 Dev, QA, Prod 클러스터의 API 서버 주소와 토큰을 등록하여 중앙에서 통제합니다.
*   **Grafana**: Management Cluster에 하나만 띄웁니다. 단, 각 타겟 클러스터(Dev, QA, Prod)에는 가벼운 Agent(Prometheus Agent 등)만 설치하여 메트릭을 중앙으로 쏴주는 방식(Remote Write)을 사용합니다.
*   **CI Runner**: 소스코드를 빌드하는 러너 역시 Management Cluster 쪽에 배치하여 리소스를 공유합니다.
