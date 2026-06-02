# 🛠 Dev 환경 배포 가이드

본 문서는 개발(Dev) 환경(`k8s-dev` 클러스터)에 Inventory API를 배포하는 절차를 설명합니다.

## 1. 사전 준비
* `kubectl` 컨텍스트가 Dev 클러스터를 바라보고 있어야 합니다.
  ```bash
  kubectl config use-context k8s-dev
  ```
* Dev 환경의 인프라 도구들은 전용 LoadBalancer IP를 가지고 있습니다.
  * API Gateway (Cilium): `10.1.105.8`
  * Harbor: `10.1.105.10`
  * Argo CD: `10.1.105.9`
* 원활한 도메인 접속을 위해 사용자 PC의 `dnsmasq` 설정을 아래와 같이 구성해야 합니다.
  ```bash
  # /etc/dnsmasq.d/k8s-dev.conf 파일에 아래 내용 추가
  address=/api.dev.test/10.1.105.8
  address=/harbor.dev.test/10.1.105.10
  address=/argocd.dev.test/10.1.105.9
  
  # 설정 적용
  sudo systemctl restart dnsmasq
  ```
* Docker 데몬에 `harbor.dev.test`가 Insecure Registry로 등록되어 있어야 합니다.

## 2. 이미지 빌드 및 푸시 (Harbor)
Dev 환경용 레지스트리에 이미지를 푸시합니다.

```bash
# 1. Harbor 로그인
docker login harbor.dev.test

# 2. Docker 이미지 빌드
docker build -t harbor.dev.test/library/inventory-api:v1.0.0 .

# 3. 이미지 푸시
docker push harbor.dev.test/library/inventory-api:v1.0.0
```

## 3. GitHub 매니페스트 업데이트
`k8s/overlays/dev/kustomization.yaml` 파일을 열고, 방금 푸시한 이미지 태그(`v1.0.0`)로 변경한 뒤 GitHub `main` 브랜치에 커밋 및 푸시합니다.

```bash
git add k8s/overlays/dev/kustomization.yaml
git commit -m "chore(dev): update image tag to v1.0.0"
git push origin main
```

## 4. Argo CD 연동 (최초 1회만)
Dev 클러스터 내부에 존재하는 Argo CD에 Application 매니페스트를 적용합니다.

```bash
kubectl apply -f argocd/argocd-application-dev.yaml
```

## 5. 배포 확인
1. 파드 상태 확인: `kubectl get pods -n inventory-dev`
2. 대시보드 확인: 브라우저에서 `https://argocd.dev.test` 접속 (Dev 환경용 Argo CD 주소, 보안 경고 무시 후 접속)
