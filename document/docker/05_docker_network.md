# 5. Docker 네트워크

> 권장 시간: 1.5시간  
> 목표: Docker 네트워크 드라이버의 차이를 이해하고 Spring Boot ↔ DB 컨테이너 간 통신을 구성한다

---

## 5.1 네트워크 드라이버 종류

### Docker 네트워크 개요

Docker는 컨테이너 간, 컨테이너와 외부 간 통신을 위한 가상 네트워크를 제공한다.
네트워크 드라이버에 따라 격리 수준과 동작 방식이 달라진다.

```bash
# 기본 생성된 네트워크 목록 확인
docker network ls

# 출력 예시:
# NETWORK ID     NAME      DRIVER    SCOPE
# a1b2c3d4e5f6   bridge    bridge    local
# b2c3d4e5f6a1   host      host      local
# c3d4e5f6a1b2   none      null      local
```

### bridge 드라이버 (기본값)

컨테이너마다 가상 네트워크 인터페이스를 생성하고 호스트의 브릿지(`docker0`)에 연결한다.
`docker run` 시 네트워크를 지정하지 않으면 기본 bridge 네트워크에 연결된다.

```
호스트
┌─────────────────────────────────────────────┐
│                                             │
│    ┌──────────┐        ┌──────────┐         │
│    │Container │        │Container │         │
│    │  (App)   │        │  (DB)    │         │
│    │172.17.0.2│        │172.17.0.3│         │
│    └────┬─────┘        └────┬─────┘         │
│         │                  │               │
│    ─────┴──────────────────┴──── docker0   │
│              bridge network                │
│                     │                      │
│                  eth0 (호스트 NIC)          │
└─────────────────────────────────────────────┘
```

**특징**
- 같은 bridge 네트워크 컨테이너끼리 IP로 통신 가능
- 기본 bridge는 컨테이너 이름으로 DNS 해석 불가 (IP만 가능)
- **사용자 정의 bridge는 컨테이너 이름 DNS 해석 지원** ← 실무 핵심

### host 드라이버

컨테이너가 호스트의 네트워크 스택을 그대로 사용한다. 포트 매핑 불필요.

```bash
# 호스트 네트워크 사용 (Linux only, macOS/Windows 미지원)
docker run --network=host nginx
# → localhost:80 으로 바로 접근 가능 (-p 옵션 불필요)
```

**특징**
- 네트워크 성능 최대 (NAT 오버헤드 없음)
- 포트 격리 없음 → 컨테이너 포트가 호스트에 직접 노출
- 보안상 주의 필요 / macOS·Windows에서 동작하지 않음

### none 드라이버

네트워크 인터페이스를 생성하지 않는다. 완전한 네트워크 격리.

```bash
docker run --network=none alpine ping google.com
# ping: bad address 'google.com' (네트워크 없음)
```

**특징**
- 외부 통신이 전혀 필요 없는 배치성 작업에 활용
- 보안이 중요한 데이터 처리 작업

### 드라이버 비교 요약

| 드라이버 | 격리 | DNS 이름 해석 | 포트 매핑 | 주 사용처 |
|---------|------|-------------|---------|---------|
| bridge (기본) | 컨테이너 간 격리 | ❌ (기본) / ✅ (사용자 정의) | 필요 | 일반 개발/운영 |
| host | 격리 없음 | 불필요 | 불필요 | 고성능 네트워크 |
| none | 완전 격리 | 없음 | 없음 | 오프라인 배치 |

---

## 5.2 사용자 정의 네트워크와 컨테이너 간 통신

### 왜 사용자 정의 네트워크가 필요한가?

기본 bridge 네트워크의 문제:
- **컨테이너 이름으로 통신 불가** → IP 주소를 직접 지정해야 함
- 컨테이너 IP는 재시작 시 변경될 수 있음 → 하드코딩 위험

사용자 정의 bridge 네트워크의 장점:
- **컨테이너 이름 = DNS 호스트명** → `http://db:3306` 형태로 통신
- 같은 네트워크 안 컨테이너끼리만 통신 가능 → 격리성 확보
- 네트워크 단위 관리 (연결/해제 동적 가능)

### 네트워크 생성 및 관리

```bash
# 사용자 정의 bridge 네트워크 생성
docker network create my-network

# 서브넷/게이트웨이 지정 (선택)
docker network create \
  --driver bridge \
  --subnet=172.20.0.0/16 \
  --gateway=172.20.0.1 \
  my-network

# 네트워크 목록
docker network ls

# 네트워크 상세 정보 (연결된 컨테이너, IP 등)
docker network inspect my-network

# 네트워크 삭제 (연결된 컨테이너 없을 때만 가능)
docker network rm my-network

# 사용하지 않는 네트워크 전체 삭제
docker network prune
```

### 컨테이너를 네트워크에 연결

```bash
# 실행 시 네트워크 지정
docker run -d --network my-network --name app spring-demo:1.0

# 실행 중인 컨테이너에 네트워크 추가 연결
docker network connect my-network existing-container

# 네트워크에서 컨테이너 분리
docker network disconnect my-network existing-container
```

---

## 5.3 컨테이너 DNS 이름 해석

### 동작 원리

사용자 정의 네트워크에서 Docker는 내장 DNS 서버를 제공한다.
컨테이너 이름 또는 `--network-alias`로 지정한 이름이 DNS로 해석된다.

```
my-network 내부
┌─────────────────────────────────────────────────┐
│                                                 │
│  ┌──────────────┐    "db" DNS 해석    ┌───────┐ │
│  │ spring-app   │ ─────────────────▶ │  db   │ │
│  │ (172.20.0.2) │   172.20.0.3       │MySQL  │ │
│  └──────────────┘                    └───────┘ │
│                   Docker 내장 DNS               │
└─────────────────────────────────────────────────┘
```

### Spring Boot `application.yml` 설정 예시

```yaml
spring:
  datasource:
    # 컨테이너 이름 'db'를 호스트명으로 직접 사용
    url: jdbc:mysql://db:3306/mydb
    username: root
    password: secret
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 네트워크 별칭 (alias) 활용

```bash
# 여러 컨테이너에 동일한 alias 부여 → 로드밸런싱 효과
docker run -d --network my-network \
  --network-alias backend \
  --name app1 spring-demo:1.0

docker run -d --network my-network \
  --network-alias backend \
  --name app2 spring-demo:1.0

# nginx가 'backend'로 요청 → app1 또는 app2로 분산
```

---

## 5.4 Spring Boot ↔ DB 컨테이너 간 연결 실습 구성

### 전체 구성도

```
사용자
  │
  │ HTTP :8080
  ▼
┌────────────────────────────────────────────────┐
│              app-network (bridge)              │
│                                                │
│  ┌─────────────────┐    ┌────────────────────┐ │
│  │   spring-app    │    │        db          │ │
│  │ (Spring Boot)   │───▶│     (MySQL 8.0)    │ │
│  │  port: 8080     │    │    port: 3306      │ │
│  └─────────────────┘    └────────────────────┘ │
│                                                │
└────────────────────────────────────────────────┘
  호스트 :8080 에서만 외부 접근 가능
  DB는 외부 포트 매핑 없음 (내부망에서만 접근)
```

---

## 실습

### 실습 5-1: 기본 bridge vs 사용자 정의 네트워크 DNS 비교

```bash
# === 기본 bridge 네트워크 테스트 ===

# 두 컨테이너를 기본 네트워크에 실행
docker run -d --name net-a alpine sleep 3600
docker run -d --name net-b alpine sleep 3600

# net-a에서 net-b를 이름으로 ping → 실패
docker exec net-a ping -c 2 net-b
# ping: bad address 'net-b'  ← 이름 해석 불가

# IP 주소로는 통신 가능
IP_B=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' net-b)
docker exec net-a ping -c 2 $IP_B

# 정리
docker stop net-a net-b && docker rm net-a net-b


# === 사용자 정의 네트워크 테스트 ===

# 네트워크 생성
docker network create test-network

# 같은 네트워크에 두 컨테이너 실행
docker run -d --network test-network --name svc-a alpine sleep 3600
docker run -d --network test-network --name svc-b alpine sleep 3600

# svc-a에서 svc-b를 이름으로 ping → 성공
docker exec svc-a ping -c 2 svc-b
# PING svc-b: 56 data bytes  ← 이름 해석 성공!

# 정리
docker stop svc-a svc-b && docker rm svc-a svc-b
docker network rm test-network
```

**확인 포인트**
- 기본 bridge: 이름 DNS 해석 실패, IP로만 통신 가능
- 사용자 정의 bridge: 컨테이너 이름으로 DNS 해석 성공

---

### 실습 5-2: Spring Boot + MySQL 연결 구성

```bash
# 1. 전용 네트워크 생성
docker network create app-network

# 2. MySQL 컨테이너 실행 (포트 외부 노출 없음)
docker run -d \
  --name db \
  --network app-network \
  -e MYSQL_ROOT_PASSWORD=secret \
  -e MYSQL_DATABASE=mydb \
  -e MYSQL_USER=appuser \
  -e MYSQL_PASSWORD=apppass \
  mysql:8.0

# 3. MySQL 기동 완료 대기 (약 30초)
docker logs -f db
# "ready for connections" 메시지 확인 후 Ctrl+C

# 4. Spring Boot 앱 실행 (DB 컨테이너 이름 'db'로 연결)
docker run -d \
  --name spring-app \
  --network app-network \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/mydb \
  -e SPRING_DATASOURCE_USERNAME=appuser \
  -e SPRING_DATASOURCE_PASSWORD=apppass \
  -e SPRING_JPA_HIBERNATE_DDL_AUTO=update \
  spring-demo:1.0

# 5. 앱 로그에서 DB 연결 성공 확인
docker logs -f spring-app

# 6. API 호출 테스트
curl http://localhost:8080/actuator/health

# 7. spring-app에서 db 이름으로 접근되는지 확인
docker exec spring-app ping -c 2 db

# 8. DB는 외부에서 직접 접근 불가 확인
# (포트 매핑이 없으므로 호스트에서 mysql 클라이언트로 3306 접근 불가)

# 9. 정리
docker stop spring-app db
docker rm spring-app db
docker network rm app-network
```

---

### 실습 5-3: 네트워크 격리 확인

```bash
# 두 개의 분리된 네트워크 생성
docker network create network-a
docker network create network-b

# 각 네트워크에 컨테이너 배치
docker run -d --network network-a --name app-a alpine sleep 3600
docker run -d --network network-b --name app-b alpine sleep 3600

# app-a → app-b 통신 시도 (다른 네트워크 → 실패)
docker exec app-a ping -c 2 app-b
# ping: bad address 'app-b'  ← 다른 네트워크라 통신 불가

# app-a를 network-b에도 연결 (멀티 네트워크)
docker network connect network-b app-a

# 이제 app-a → app-b 통신 가능
docker exec app-a ping -c 2 app-b
# 성공!

# 정리
docker stop app-a app-b && docker rm app-a app-b
docker network rm network-a network-b
```

---

### 실습 5-4: 네트워크 상세 정보 확인

```bash
# 네트워크 생성 + 컨테이너 연결
docker network create inspect-network
docker run -d --network inspect-network --name inspect-app alpine sleep 3600
docker run -d --network inspect-network --name inspect-db mysql:8.0 \
  -e MYSQL_ROOT_PASSWORD=secret

# 네트워크 상세 조회 (연결된 컨테이너 및 IP 확인)
docker network inspect inspect-network

# 출력에서 확인할 항목:
# - Subnet, Gateway
# - Containers 섹션: 각 컨테이너의 IP, MAC 주소

# 컨테이너 관점에서 네트워크 확인
docker inspect inspect-app | grep -A 20 '"Networks"'

# 정리
docker stop inspect-app inspect-db
docker rm inspect-app inspect-db
docker network rm inspect-network
```

---

## 핵심 정리

| 개념 | 핵심 내용 |
|------|----------|
| 기본 bridge | 컨테이너 이름 DNS 해석 불가, 실무 사용 비권장 |
| 사용자 정의 bridge | 컨테이너 이름으로 DNS 해석 가능 → 실무 표준 |
| host | 네트워크 격리 없음, Linux 전용, 고성능 필요 시 |
| none | 완전 격리, 외부 통신 필요 없는 배치 작업 |
| `--network` | 컨테이너 실행 시 네트워크 지정 옵션 |
| DB 포트 미노출 | DB 컨테이너에 `-p` 옵션 생략 → 외부 직접 접근 차단 |
| Spring Boot 연결 | `SPRING_DATASOURCE_URL=jdbc:mysql://컨테이너명:3306/db` |

---

## 참고 자료

- [Docker 네트워크 공식 문서](https://docs.docker.com/network/)
- [bridge 네트워크 드라이버](https://docs.docker.com/network/drivers/bridge/)
- [컨테이너 네트워킹 튜토리얼](https://docs.docker.com/network/network-tutorial-standalone/)
