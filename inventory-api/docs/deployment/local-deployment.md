# 💻 Local 환경 배포 가이드

본 문서는 개발자의 로컬 환경(`k8s-local` 클러스터)에 Inventory API를 배포하는 절차를 설명합니다.

## 1. 사전 준비
* `kubectl` 컨텍스트가 로컬 클러스터를 바라보고 있어야 합니다.
  ```bash
  kubectl config use-context k8s-local
  ```
* `k8s-local` 클러스터가 실행 중이어야 합니다.
* Local 환경의 인프라 도구들은 전용 LoadBalancer IP를 가지고 있습니다.
  * API Gateway (Cilium): `192.168.88.91`
  * Harbor: `192.168.88.92`
  * Argo CD: `192.168.88.90`
* 원활한 도메인 접속을 위해 사용자 PC의 `dnsmasq` 설정을 아래와 같이 구성해야 합니다.
  ```bash
  # /etc/dnsmasq.d/k8s-local.conf 파일에 아래 내용 추가
  address=/api.local.test/192.168.88.91
  address=/harbor.local.test/192.168.88.92
  address=/argocd.local.test/192.168.88.90
  
  # 설정 적용
  sudo systemctl restart dnsmasq
  ```
* Docker 데몬이 실행 중이어야 하며, `harbor.local.test`가 Insecure Registry로 등록되어 있어야 합니다.

## 2. 이미지 빌드 및 푸시 (Harbor)
로컬 레지스트리에 이미지를 푸시합니다.

```bash
# 1. Harbor 로그인
docker login harbor.local.test

# 2. 애플리케이션 빌드
./gradlew clean build -x test

# 3. Docker 이미지 빌드
docker build -t harbor.local.test/library/inventory-api:v1.0.0 .

# 4. 이미지 푸시
docker push harbor.local.test/library/inventory-api:v1.0.0
```

## 3. GitHub 매니페스트 업데이트
`k8s/overlays/local/kustomization.yaml` 파일을 열고, 방금 푸시한 이미지 태그(`v1.0.0`)로 변경한 뒤 GitHub `main` 브랜치에 커밋 및 푸시합니다. **(Argo CD는 오직 GitHub만 바라봅니다)**

```bash
git add k8s/overlays/local/kustomization.yaml
git commit -m "chore(local): update image tag to v1.0.0"
git push origin main
```

## 4. Argo CD 연동 (최초 1회만)
해당 환경의 클러스터에 Argo CD Application 매니페스트를 적용하여 GitOps 동기화를 시작합니다.

```bash
kubectl apply -f argocd/argocd-application-local.yaml
```

## 5. 배포 확인
1. 파드 상태 확인: `kubectl get pods -n inventory`
2. 대시보드 확인: 브라우저에서 `http://argocd.local.test` 접속
