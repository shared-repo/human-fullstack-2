# 1. Docker 기초 이해

> 권장 시간: 1시간  
> 목표: 컨테이너 기술의 개념을 이해하고 Docker 환경을 구성한다

---

## 1.1 컨테이너 vs 가상머신 개념 비교

### 가상머신(VM)이란?

하드웨어를 소프트웨어로 에뮬레이션하여 그 위에 완전한 운영체제(Guest OS)를 실행하는 방식이다.
각 VM은 독립된 OS 커널을 가지므로 격리 수준은 높지만, 그만큼 자원 소비가 크고 기동 시간이 길다.

```
┌─────────────────────────────────────┐
│         App A      App B      App C │
│        ┌─────┐   ┌─────┐   ┌─────┐ │
│        │Guest│   │Guest│   │Guest│ │
│        │ OS  │   │ OS  │   │ OS  │ │
│        └─────┘   └─────┘   └─────┘ │
│             Hypervisor              │
│              Host OS                │
│              Hardware               │
└─────────────────────────────────────┘
```

### 컨테이너란?

OS 커널을 공유하면서 프로세스 수준에서 격리하는 방식이다.
별도의 Guest OS 없이 호스트 OS 커널 위에서 직접 실행되므로 가볍고 빠르다.

```
┌─────────────────────────────────────┐
│         App A      App B      App C │
│        ┌─────┐   ┌─────┐   ┌─────┐ │
│        │Libs │   │Libs │   │Libs │ │
│        └─────┘   └─────┘   └─────┘ │
│           Docker Engine (Daemon)    │
│              Host OS                │
│              Hardware               │
└─────────────────────────────────────┘
```

### 비교 요약

| 항목 | 가상머신 (VM) | 컨테이너 |
|------|-------------|---------|
| 격리 단위 | OS 수준 | 프로세스 수준 |
| 기동 시간 | 분(minute) 단위 | 초(second) 단위 |
| 이미지 크기 | GB 단위 | MB 단위 |
| 자원 효율 | 낮음 | 높음 |
| OS 커널 | 각자 보유 | 호스트와 공유 |
| 이식성 | 낮음 | 높음 |

### Spring Boot 관점에서의 이점

- JAR 파일과 실행 환경(JRE, 환경변수, 포트)을 하나의 이미지로 패키징 가능
- "내 PC에서는 됐는데요" 문제 해결 → 실행 환경 자체를 배포
- 로컬 개발, 테스트 서버, 운영 서버 환경을 동일하게 유지

---

## 1.2 Docker 아키텍처

Docker는 클라이언트-서버 구조로 동작한다.

```
┌────────────────┐         ┌──────────────────────────────────┐
│  Docker Client │─REST──▶│          Docker Daemon            │
│                │         │                                  │
│  docker build  │         │  ┌──────────┐  ┌─────────────┐  │
│  docker pull   │         │  │  Images  │  │ Containers  │  │
│  docker run    │         │  └──────────┘  └─────────────┘  │
└────────────────┘         └──────────────┬───────────────────┘
                                          │
                                          ▼
                              ┌───────────────────────┐
                              │    Registry           │
                              │  (Docker Hub /        │
                              │   Private Registry)   │
                              └───────────────────────┘
```

### 구성 요소 설명

| 구성 요소 | 역할 |
|----------|------|
| **Docker Client** | 사용자가 명령어를 입력하는 CLI 인터페이스 (`docker` 명령어) |
| **Docker Daemon** | 실제 이미지/컨테이너를 관리하는 백그라운드 프로세스 (`dockerd`) |
| **Registry** | 이미지를 저장하고 배포하는 저장소 (Docker Hub, 사설 레지스트리) |

### 명령어 흐름 예시

```bash
# 1. 클라이언트가 Daemon에 요청
docker run nginx

# 내부 동작 순서:
# ① Client → Daemon: "nginx 이미지로 컨테이너 실행 요청"
# ② Daemon: 로컬에 nginx 이미지 존재 여부 확인
# ③ 없으면 Registry(Docker Hub)에서 pull
# ④ 이미지로 컨테이너 생성 후 실행
```

---

## 1.3 이미지와 컨테이너의 관계

### 이미지 (Image)

- 컨테이너 실행에 필요한 파일 시스템과 설정의 **읽기 전용 템플릿**
- 레이어(Layer) 구조로 구성되어 변경된 부분만 저장 (효율적)
- 클래스(Class) 에 비유할 수 있음

### 컨테이너 (Container)

- 이미지를 기반으로 **실행 중인 프로세스** (인스턴스)
- 이미지 위에 읽기/쓰기 가능한 레이어가 추가됨
- 객체(Object/Instance) 에 비유할 수 있음
- 종료되어도 삭제하지 않으면 파일시스템은 유지됨

```
  [이미지]  ──────────────────────────────────────────────
  nginx:latest                                             │
                                      ┌──────────────────┐│
                                      │    Container A   ││  docker run
                          이미지로    │  (nginx 실행 중) │├──────────
                          컨테이너    └──────────────────┘│
                          생성 가능   ┌──────────────────┐│
                                      │    Container B   ││  docker run
                                      │  (nginx 실행 중) ││──────────
                                      └──────────────────┘│
  ─────────────────────────────────────────────────────────
```

### Java 개념과 비교

| Java | Docker |
|------|--------|
| `.class` 파일 (컴파일 결과) | Dockerfile |
| Class 정의 | Image |
| new 키워드로 생성한 객체 | Container |
| JVM | Docker Engine |

---

## 1.4 Docker 설치 및 환경 구성

### 설치 방법

**Windows / macOS**
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) 설치
- Docker Engine + CLI + Docker Compose 포함

**Linux (Ubuntu 기준)**

```bash
# 기존 버전 제거
sudo apt-get remove docker docker-engine docker.io containerd runc

# 필수 패키지 설치
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg

# Docker 공식 GPG 키 추가
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
  sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# 저장소 추가
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Docker Engine 설치
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# sudo 없이 docker 명령어 사용 (재로그인 필요)
sudo usermod -aG docker $USER
```

### 설치 확인

```bash
# Docker 버전 확인
docker --version
# 출력 예시: Docker version 25.0.3, build 4debf41

# Docker Compose 버전 확인
docker compose version
# 출력 예시: Docker Compose version v2.24.5

# Daemon 동작 확인
docker info

# 전체 동작 테스트
docker run hello-world
```

### Docker Desktop 주요 설정 (Windows/macOS)

| 설정 항목 | 권장값 | 설명 |
|----------|--------|------|
| Memory | 4GB 이상 | Spring Boot + DB 동시 실행 고려 |
| CPUs | 2 이상 | 빌드 속도에 영향 |
| Disk image size | 60GB 이상 | 이미지 저장 공간 |

---

## 실습

### 실습 1-1: hello-world 컨테이너 실행

```bash
# 이미지 다운로드 + 컨테이너 실행
docker run hello-world

# 기대 출력
# Hello from Docker!
# This message shows that your installation appears to be working correctly.
```

**확인 포인트**
1. Registry(Docker Hub)에서 `hello-world` 이미지를 자동으로 pull 했는가?
2. 컨테이너가 실행되고 메시지를 출력한 뒤 종료되었는가?

---

### 실습 1-2: nginx 컨테이너 실행 및 접속

```bash
# nginx 컨테이너를 백그라운드로 실행 (-d: detach, -p: 포트 매핑)
docker run -d -p 8080:80 --name my-nginx nginx

# 실행 중인 컨테이너 목록 확인
docker ps

# 브라우저 또는 curl로 접속
curl http://localhost:8080
# 기대 출력: nginx 기본 HTML 페이지

# 컨테이너 중지 및 삭제
docker stop my-nginx
docker rm my-nginx
```

**확인 포인트**
1. `-p 8080:80` 의 의미: 호스트 8080 포트 → 컨테이너 80 포트
2. `docker ps`에서 컨테이너 ID, 이름, 포트 매핑 확인

---

### 실습 1-3: 이미지와 컨테이너 관계 체험

```bash
# 동일한 이미지로 컨테이너 2개 동시 실행
docker run -d -p 8081:80 --name nginx-a nginx
docker run -d -p 8082:80 --name nginx-b nginx

# 두 컨테이너 모두 실행 중인지 확인
docker ps

# 각각 다른 포트로 접속
curl http://localhost:8081
curl http://localhost:8082

# 정리
docker stop nginx-a nginx-b
docker rm nginx-a nginx-b
```

**확인 포인트**
- 하나의 이미지(`nginx`)로 독립적인 컨테이너 2개가 실행됨
- 각 컨테이너는 서로 다른 포트를 사용해 격리됨

---

### 실습 1-4: Docker 주요 명령어 탐색

```bash
# 로컬에 저장된 이미지 목록
docker images

# 실행 중 + 종료된 컨테이너 전체 목록
docker ps -a

# 이미지 상세 정보 확인
docker inspect nginx

# 이미지 삭제
docker rmi hello-world

# 컨테이너 로그 확인
docker run -d -p 8080:80 --name test-nginx nginx
docker logs test-nginx

# 정리
docker stop test-nginx && docker rm test-nginx
```

---

## 핵심 정리

| 개념 | 한 줄 요약 |
|------|-----------|
| 컨테이너 | OS 커널을 공유하는 격리된 프로세스 실행 환경 |
| 이미지 | 컨테이너의 실행 템플릿 (읽기 전용) |
| Docker Daemon | 이미지/컨테이너를 실제로 관리하는 백그라운드 서비스 |
| Registry | 이미지를 저장하고 공유하는 저장소 |
| `docker run` | 이미지 pull + 컨테이너 생성 + 실행을 한 번에 수행 |

---

## 참고 자료

- [Docker 공식 문서 - Get Started](https://docs.docker.com/get-started/)
- [Docker Hub](https://hub.docker.com/)
- [Play with Docker (브라우저에서 Docker 실습)](https://labs.play-with-docker.com/)
