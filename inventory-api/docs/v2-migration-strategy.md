# API V2 독립 마이크로서비스 개발 및 배포 전략 (방식 2)

이 문서는 기존 `inventory-api` (v1)이 운영 중인 상태에서, 완전히 새로운 아키텍처나 구현을 가진 **API V2를 별도의 마이크로서비스(인프라 분리)로 개발하고 배포하는 방법**을 정의합니다.

이 전략은 무중단 마이그레이션(Zero-Downtime Migration) 및 **스트랭글러 피그 패턴(Strangler Fig Pattern)**을 기반으로 합니다.

---

## 1. 개발 및 소스 코드 관리 (Repository & Branch)

v1과 v2의 결합도를 완전히 끊어내기 위해 소스 코드를 분리합니다.

*   **권장 방식**: 기존 `inventory-api` 폴더와 동급의 새로운 폴더(예: `inventory-api-v2`)를 생성하거나, 완전히 새로운 Git Repository를 생성하여 개발합니다.
*   **프로젝트 초기화**: Spring Boot 프로젝트를 새로 초기화하고, v2 전용 데이터베이스(또는 스키마) 연결, 완전히 새로운 도메인 모델과 비즈니스 로직을 처음부터 클린하게 작성합니다.
*   **엔드포인트**: 모든 API의 경로는 `@RequestMapping("/api/v2/items")`와 같이 `v2`를 명시적으로 포함해야 합니다.

---

## 2. Docker 이미지 빌드 및 태깅 전략

v2 애플리케이션은 기존 v1과 충돌하지 않는 고유한 이미지 이름(Repository)을 가져야 합니다.

*   **v1 이미지**: `harbor.*.test/library/inventory-api:v1.0.5`
*   **v2 이미지**: `harbor.*.test/library/inventory-api-v2:v1.0.0` (이미지 이름에 `-v2`를 명시하여 구별)

```bash
# v2 빌드 및 푸시 예시
docker build -t harbor.local.test/library/inventory-api-v2:v1.0.0 .
docker push harbor.local.test/library/inventory-api-v2:v1.0.0
```

---

## 3. K8s 매니페스트 구성 (Kustomize 분리)

v1과 v2는 쿠버네티스 클러스터 내에서 완전히 독립된 Deployment와 Service로 실행되어야 합니다. Kustomize 구조도 분리합니다.

**디렉토리 구조 예시:**
```text
k8s-apps/
├── inventory-api/         <-- 기존 V1
│   ├── k8s/base/...
│   └── k8s/overlays/...
└── inventory-api-v2/      <-- 신규 V2 전용 매니페스트
    └── k8s/
        ├── base/
        │   ├── deployment.yaml  (name: inventory-api-v2)
        │   └── service.yaml     (name: inventory-api-v2-svc)
        └── overlays/
            ├── local/
            ├── dev/
            ├── qa/
            └── prod/
```

**v2 Service 매니페스트 예시 (`k8s/base/service.yaml`)**:
```yaml
apiVersion: v1
kind: Service
metadata:
  name: inventory-api-v2-svc  # 서비스 이름 분리!
spec:
  selector:
    app: inventory-api-v2
  ports:
    - protocol: TCP
      port: 80
      targetPort: 8080
```

---

## 4. Argo CD 배포 파이프라인 구성

v2를 클러스터에 배포하기 위해 새로운 Argo CD Application 매니페스트를 생성합니다. (기존 v1 앱과 별개로 동작)

```yaml
# argocd/argocd-application-v2-prod.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: inventory-api-v2-prod  # v2 전용 이름
  namespace: argocd
spec:
  source:
    repoURL: 'https://github.com/sc7258/k8s-apps.git'
    path: inventory-api-v2/k8s/overlays/prod  # v2의 매니페스트 경로
  destination:
    server: 'https://kubernetes.default.svc'
    namespace: inventory-prod  # 동일한 네임스페이스에 배포 가능
```

이 시점에서 클러스터의 `inventory-prod` 네임스페이스에는 **v1 파드와 v2 파드가 동시에 떠 있게 됩니다.**

---

## 5. 트래픽 라우팅 및 무중단 전환 (Gateway API / Ingress)

클라이언트(프론트엔드)는 뒤에 서버가 몇 대인지 알 필요 없이 `/api/v1`과 `/api/v2`로 요청만 보냅니다. 쿠버네티스의 Gateway(또는 Ingress)가 경로(Path)를 보고 알맞은 서비스로 트래픽을 라우팅합니다.

**HTTPRoute (Gateway API) 설정 예시**:
```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: inventory-api-route
  namespace: inventory-prod
spec:
  parentRefs:
  - name: cilium-gateway
  rules:
  # 1. /api/v2/ 로 들어오는 요청은 V2 파드로 라우팅
  - matches:
    - path:
        type: PathPrefix
        value: /api/v2/
    backendRefs:
    - name: inventory-api-v2-svc
      port: 80

  # 2. /api/v1/ 로 들어오는 요청은 기존 V1 파드로 라우팅
  - matches:
    - path:
        type: PathPrefix
        value: /api/v1/
    backendRefs:
    - name: inventory-api-svc
      port: 80
```

## 6. 마이그레이션 완료 및 V1 은퇴 (Decommissioning)

모든 클라이언트(모바일 앱, 웹 프론트엔드 등)가 V2 API로의 마이그레이션을 완료하여 V1으로 들어오는 트래픽이 `0`이 확인되면, 기존 V1 리소스를 정리합니다.

1.  Gateway API 라우팅 룰에서 `/api/v1/` 부분 삭제
2.  Argo CD에서 `inventory-api-v1` Application 삭제 (이로 인해 v1 파드들이 모두 내려감)
3.  소스 코드 레포지토리 아카이빙 (필요시)
