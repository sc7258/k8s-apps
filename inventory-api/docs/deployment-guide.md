# Inventory API Deployment Guide

이 문서는 로컬 쿠버네티스 환경에서 **Harbor**와 **Argo CD**를 활용하여 Inventory API를 GitOps 방식으로 배포하는 절차를 설명합니다.

## 아키텍처 개요
1. **소스 관리**: GitHub (`sc7258/k8s-apps`)
2. **컨테이너 레지스트리**: Local Harbor (`harbor.local.test`)
3. **지속적 배포 (CD)**: Argo CD (`argocd.local.test`)

---

## 1. 사전 준비 (Prerequisites)

### 1.1. Docker Insecure Registry 설정
로컬 Harbor는 자체 서명된 인증서를 사용하므로, Docker가 이를 신뢰할 수 있도록 설정해야 합니다.
기본적으로 `/etc/docker/daemon.json` 파일이 존재하지 않을 수 있으므로 아래 명령어를 통해 생성합니다.

1. `/etc/docker/daemon.json` 파일 생성 및 설정 추가:
   ```bash
   sudo mkdir -p /etc/docker
   sudo tee /etc/docker/daemon.json > /dev/null <<EOF
   {
     "insecure-registries": ["harbor.local.test"]
   }
   EOF
   ```
2. Docker 데몬을 재시작합니다:
   ```bash
   sudo systemctl restart docker
   ```
3. Harbor에 로그인하여 연결을 확인합니다 (기본 계정: `admin` / 패스워드 입력):
   ```bash
   docker login harbor.local.test
   ```

---

## 2. 이미지 빌드 및 푸시 (CI)

소스 코드를 변경한 후, 애플리케이션을 빌드하고 Harbor 레지스트리에 푸시합니다.

```bash
# 1. 애플리케이션 jar 빌드 (테스트 생략)
./gradlew clean build -x test

# 2. Docker 이미지 빌드 (Harbor 경로 및 태그 지정)
docker build -t harbor.local.test/library/inventory-api:v1 .

# 3. Harbor 레지스트리에 이미지 푸시
docker push harbor.local.test/library/inventory-api:v1
```
*(참고: 버전을 올릴 때는 `v1` 대신 `v2`, `v3` 등 새로운 태그를 사용해야 합니다.)*

---

## 3. K8s 매니페스트 업데이트 (Kustomize)

이 프로젝트는 다중 환경(`local`, `dev`, `qa`, `prod`) 배포를 지원하기 위해 **Kustomize**를 사용합니다.
새로운 이미지를 배포할 때는 `base` 파일이 아닌, 각 환경의 `overlays/*/kustomization.yaml` 파일을 수정합니다.

**수정 예시 (`k8s/overlays/local/kustomization.yaml`)**:
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
  - ../../base

images:
  - name: inventory-api
    newName: harbor.local.test/library/inventory-api   # 환경에 맞는 레지스트리 주소
    newTag: v2                                         # 새로운 이미지 태그로 변경

replicas:
  - name: inventory-api
    count: 1
```

수정이 완료되면 GitHub 레포지토리에 푸시합니다:
```bash
git add k8s/overlays/local/kustomization.yaml
git commit -m "chore: update local image tag to v2"
git push origin main
```

---

## 4. Argo CD 연동 (GitOps 배포)

Argo CD가 GitHub의 변경 사항을 감지하여 자동으로 클러스터에 동기화하도록 Application 리소스를 생성합니다. (최초 1회만 수행)
각 환경에 배포할 때는 `path` 속성에 해당 환경의 **overlay 경로**를 지정합니다.

```yaml
# argocd-application-local.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: inventory-api-local
  namespace: argocd
spec:
  project: default
  source:
    repoURL: 'https://github.com/sc7258/k8s-apps.git'
    targetRevision: main
    path: inventory-api/k8s/overlays/local  # <--- Kustomize overlay 경로 지정
  destination:
    server: 'https://kubernetes.default.svc'
    namespace: inventory
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
    - CreateNamespace=true
```

위의 매니페스트를 저장한 후 적용하거나, 터미널에서 아래 명령어를 실행합니다:

```bash
kubectl apply -f argocd-application-local.yaml
```

---

## 5. 배포 상태 확인 및 라우팅 (Gateway API)

1. **파드 상태 확인**: 
   ```bash
   kubectl get pods -n inventory
   ```
2. **Argo CD 대시보드**: 브라우저에서 `http://argocd.local.test`에 접속하여 동기화 상태를 시각적으로 확인할 수 있습니다.
3. **외부 노출 (향후 계획)**: 애플리케이션 파드가 정상적으로 실행되면, 외부에서 접근할 수 있도록 Gateway API(`HTTPRoute`)를 생성하여 라우팅 설정을 추가할 수 있습니다.
