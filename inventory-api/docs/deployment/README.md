# 🚀 배포 가이드라인 (Deployment Guides)

본 디렉토리는 Inventory API 프로젝트의 각 환경별 배포 방법과 아키텍처 전략을 명세합니다.

## 📌 운영 환경별 배포 가이드
각 환경(클러스터)에 맞는 상세 배포 절차는 아래 문서를 참고하세요.

1. [Local 환경 배포 가이드](./local-deployment.md) (`k8s-local`)
2. [Dev 환경 배포 가이드](./dev-deployment.md) (`k8s-dev`)
3. *QA 및 Prod 배포 가이드는 추후 업데이트 예정*

## 🏗 아키텍처 전략 문서
인프라 구성 및 V2 전환 전략 등 근본적인 아키텍처 설계 방향은 아래 문서를 참고하세요.

* [인프라 아키텍처 배포 및 관리 전략](../infrastructure-architecture.md) (Hub and Spoke 모델)
* [API V2 독립 마이크로서비스 마이그레이션 전략](../v2-migration-strategy.md) (Strangler Fig 패턴)

---

### 배포 파이프라인 개요 (GitOps)
우리의 배포 파이프라인은 철저히 **GitOps (Argo CD + Kustomize)** 원칙을 따릅니다.
1. 개발자가 소스 코드를 수정하고 **이미지를 빌드 및 푸시**합니다.
2. `k8s/overlays/<환경>/kustomization.yaml` 파일의 **이미지 태그를 수정하여 GitHub에 푸시**합니다.
3. 대상 클러스터의 **Argo CD**가 GitHub 변경 사항을 감지하여 자동으로 배포합니다.
