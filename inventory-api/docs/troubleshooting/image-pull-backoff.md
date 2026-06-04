# 🚨 Troubleshooting 가이드

본 문서는 Inventory API 개발 및 배포 과정에서 발생할 수 있는 주요 에러와 그 해결 방법, 그리고 배경 지식을 정리합니다.

---

## 1. Kubernetes 로컬 배포 시 `ImagePullBackOff` 에러

로컬 K8s 클러스터에 파드를 배포할 때, 파드 상태가 `ImagePullBackOff` 또는 `ErrImagePull`에 빠지는 경우가 있습니다.

### 🔍 원인 분석: `x509: certificate signed by unknown authority`

`kubectl describe pod <pod-name>` 명령어로 이벤트를 확인했을 때 위와 같은 인증서 에러가 발생한다면, **로컬 쿠버네티스 노드(containerd)가 로컬 Harbor 레지스트리의 인증서를 신뢰하지 못하기 때문**입니다.

이 문제가 헷갈리기 쉬운 이유는 **"나는 분명히 Docker 설정에 Insecure Registry를 추가해서 푸시(Push)까지 성공했는데 왜 쿠버네티스가 못 가져오지?"** 라고 생각하기 쉽기 때문입니다.

### 💡 핵심 개념: Docker 데몬 vs Containerd

로컬 개발 환경에서는 보통 컨테이너 기술이 두 가지 역할을 분담하여 수행합니다.

1. **이미지를 빌드하고 레지스트리에 푸시할 때 (개발자 PC)**:
   * **사용 도구:** `Docker` (Docker 데몬)
   * **설정:** `/etc/docker/daemon.json` 에 `insecure-registries`를 추가하여 자체 서명 인증서를 무시하도록 설정했습니다. 그래서 `docker push`는 성공합니다.

2. **파드를 띄우기 위해 이미지를 풀(Pull) 받을 때 (Kubernetes 노드)**:
   * **사용 도구:** 최신 쿠버네티스는 Docker가 아닌 **`containerd`** 라는 컨테이너 런타임을 기본으로 사용합니다.
   * **설정:** `containerd`는 Docker의 `daemon.json` 설정을 읽지 않습니다! 따라서 자기가 다운로드하려는 Harbor 서버의 인증서가 공인 기관(CA)에서 발급된 것이 아니라 자체 서명(Self-signed)된 것을 보고 보안 위험으로 판단하여 다운로드를 차단합니다.

### ✅ 해결 방법

쿠버네티스의 런타임인 `containerd`에게 해당 로컬 도메인(`harbor.local.test`)의 TLS 보안 검증을 무시하라고 명시적으로 알려주어야 합니다. 
**아래의 1단계(규칙 생성)와 2단계(경로 버그 수정)를 순서대로 모두 수행해야 완벽히 적용됩니다.**

#### 1단계: Containerd TLS 검증 무시 설정 (규칙 생성)

```bash
# 1. containerd 인증서 설정용 도메인 디렉토리 생성
sudo mkdir -p /etc/containerd/certs.d/harbor.local.test

# 2. hosts.toml 설정 파일 생성 (TLS 검증 무시 설정)
sudo tee /etc/containerd/certs.d/harbor.local.test/hosts.toml > /dev/null <<EOF
server = "https://harbor.local.test"

[host."https://harbor.local.test"]
  capabilities = ["pull", "resolve"]
  skip_verify = true
EOF
```

#### 2단계: (주의) config_path 경로 설정 버그 수정 및 재시작

Ubuntu 환경 등에서 `containerd`를 설치했을 때 기본 `config.toml`에 아래와 같이 경로가 설정된 경우가 있습니다.
`config_path = "/etc/containerd/certs.d:/etc/docker/certs.d"`

`containerd`는 환경변수(`PATH`)처럼 콜론(`:`)으로 구분된 다중 경로를 지원하지 않습니다. 위와 같이 설정되면 통째로 하나의 폴더 이름으로 인식하여 설정(hosts.toml)을 완전히 무시하게 됩니다. 이를 단일 경로로 수정해야 합니다.

```bash
# config_path 오류 수정 (콜론 뒤쪽 제거)
sudo sed -i 's|config_path = .*/etc/containerd/certs.d:/etc/docker/certs.d.*|config_path = "/etc/containerd/certs.d"|g' /etc/containerd/config.toml

# containerd 재시작 (설정 적용)
sudo systemctl restart containerd
```

---

### 🔎 조치 결과 검증 방법

설정이 정상적으로 적용되었는지 확인하려면 다음 두 가지 방법 중 하나를 사용할 수 있습니다.

#### 방법 A: 쿠버네티스 파드 재시작 (추천)
에러가 발생하고 있던 파드를 지우면 쿠버네티스가 즉시 새 파드를 생성합니다.

```bash
# 1. 기존 파드 삭제
kubectl delete pod -n inventory -l app=inventory-api

# 2. 파드 상태 실시간 모니터링
kubectl get pods -n inventory -w
```
**기대 결과:** 파드의 상태가 `Pending` ➔ `ContainerCreating` ➔ `Running` 으로 변경되면 성공입니다. (더 이상 `ImagePullBackOff` 발생 안 함)

#### 방법 B: crictl 도구로 직접 Pull 테스트
쿠버네티스를 거치지 않고, 설정이 변경된 노드의 컨테이너 런타임에게 직접 다운로드를 지시하여 확인합니다.

```bash
sudo crictl pull harbor.local.test/library/inventory-api:v1.0.0
```
**기대 결과:** `x509: certificate signed by unknown authority` 에러 없이 이미지의 해시값(SHA)이 출력되며 다운로드가 정상적으로 완료됩니다.
