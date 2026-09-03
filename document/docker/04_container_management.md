# 4. 컨테이너 실행 및 관리

> 권장 시간: 1시간  
> 목표: docker run의 핵심 옵션을 익히고 컨테이너 생명주기와 운영 명령어를 실무 수준으로 다룬다

---

## 4.1 docker run 주요 옵션

### 기본 문법

```bash
docker run [OPTIONS] IMAGE [COMMAND] [ARG...]
```

### 포트 매핑 (-p)

컨테이너 내부 포트를 호스트 포트로 노출한다.

```bash
# -p 호스트포트:컨테이너포트
docker run -p 8080:8080 spring-demo:1.0

# 여러 포트 동시 매핑
docker run -p 8080:8080 -p 9090:9090 spring-demo:1.0

# 호스트 특정 IP에만 바인딩 (외부 노출 차단)
docker run -p 127.0.0.1:8080:8080 spring-demo:1.0

# 호스트 포트 랜덤 자동 할당 (EXPOSE에 명시된 포트 기준)
docker run -P spring-demo:1.0
docker port <컨테이너명>  # 할당된 포트 확인
```

```
호스트                컨테이너
:8080  ───────────▶  :8080 (Spring Boot)
:3306  ───────────▶  :3306 (MySQL)
```

### 백그라운드 실행 (-d)

```bash
# -d: detach 모드 (백그라운드 실행)
docker run -d -p 8080:8080 --name my-app spring-demo:1.0

# 포그라운드 실행 (로그 바로 출력, Ctrl+C 로 종료)
docker run -p 8080:8080 spring-demo:1.0

# 대화형 터미널 (-it: interactive + tty)
docker run -it eclipse-temurin:17-jre-alpine sh
```

### 컨테이너 이름 지정 (--name)

```bash
# 이름 없이 실행 → 랜덤 이름 자동 부여 (예: romantic_euler)
docker run -d spring-demo:1.0

# 이름 지정 → 명령어에서 ID 대신 이름 사용 가능
docker run -d --name my-spring-app spring-demo:1.0
docker stop my-spring-app
docker logs my-spring-app
```

### 환경변수 (-e, --env-file)

```bash
# 단일 환경변수
docker run -d \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SERVER_PORT=8080 \
  --name my-app spring-demo:1.0

# 여러 환경변수를 파일로 관리
# app.env 파일:
# SPRING_PROFILES_ACTIVE=prod
# SERVER_PORT=8080
# DB_HOST=db
# DB_PASSWORD=secret

docker run -d --env-file app.env --name my-app spring-demo:1.0
```

### 볼륨 마운트 (-v)

```bash
# 바인드 마운트: 호스트 디렉토리 ↔ 컨테이너 디렉토리 연결
docker run -v /host/path:/container/path spring-demo:1.0

# 현재 디렉토리 마운트
docker run -v $(pwd)/config:/app/config spring-demo:1.0

# 네임드 볼륨
docker run -v mydata:/app/data spring-demo:1.0
```

> 볼륨 상세 내용은 6장에서 다룸

### 자동 삭제 (--rm)

```bash
# 컨테이너 종료 시 자동 삭제 (임시 실행에 유용)
docker run --rm spring-demo:1.0

# 활용 예: 일회성 명령 실행
docker run --rm eclipse-temurin:17-jre-alpine java -version
docker run --rm mysql:8.0 mysql --version
```

### 네트워크 지정 (--network)

```bash
# 사용자 정의 네트워크에 연결
docker run -d --network my-network --name my-app spring-demo:1.0
```

> 네트워크 상세 내용은 5장에서 다룸

---

## 4.2 컨테이너 생명주기 관리

### 생명주기 상태 전이

```
          docker run / docker start
               ┌─────────────┐
               ▼             │
  [Created] → [Running] → [Exited]
                  │              ▲
                  │   docker stop/kill
                  ▼
              [Paused]
           docker pause/unpause
```

| 상태 | 설명 |
|------|------|
| Created | 생성됨, 실행 전 |
| Running | 실행 중 |
| Paused | 일시 정지 (프로세스 freeze) |
| Exited | 종료됨 (파일시스템 유지) |
| Removed | 삭제됨 |

### 주요 생명주기 명령어

```bash
# 컨테이너 생성 (실행 X)
docker create --name my-app spring-demo:1.0

# 생성된 컨테이너 시작
docker start my-app

# 실행 중인 컨테이너 중지 (SIGTERM → 10초 후 SIGKILL)
docker stop my-app

# 강제 종료 (SIGKILL 즉시 전송)
docker kill my-app

# 재시작
docker restart my-app

# 일시 정지 / 재개
docker pause my-app
docker unpause my-app

# 컨테이너 삭제 (중지 후 삭제)
docker stop my-app && docker rm my-app

# 실행 중인 컨테이너 강제 삭제
docker rm -f my-app

# 종료된 컨테이너 전체 삭제
docker container prune
```

### 재시작 정책 (--restart)

```bash
# no: 재시작 안 함 (기본값)
docker run --restart=no spring-demo:1.0

# always: 항상 재시작 (Docker Daemon 재시작 시에도)
docker run --restart=always spring-demo:1.0

# unless-stopped: 수동으로 중지하지 않는 한 항상 재시작
docker run --restart=unless-stopped spring-demo:1.0

# on-failure: 비정상 종료 시만 재시작 (최대 횟수 지정 가능)
docker run --restart=on-failure:3 spring-demo:1.0
```

| 정책 | 설명 | 운영 환경 적합성 |
|------|------|-----------------|
| `no` | 재시작 없음 | 개발/테스트 |
| `always` | 항상 재시작 | 데몬성 서비스 |
| `unless-stopped` | 수동 중지 전까지 재시작 | 운영 권장 |
| `on-failure` | 오류 종료 시만 재시작 | 배치성 작업 |

---

## 4.3 로그 확인 및 컨테이너 접속

### 로그 확인 (docker logs)

```bash
# 전체 로그 출력
docker logs my-app

# 실시간 로그 스트리밍 (-f: follow)
docker logs -f my-app

# 마지막 N줄만 출력
docker logs --tail 100 my-app

# 실시간 + 마지막 50줄
docker logs -f --tail 50 my-app

# 특정 시간 이후 로그
docker logs --since 2024-01-01T00:00:00 my-app
docker logs --since 30m my-app   # 최근 30분

# 타임스탬프 포함
docker logs -t my-app

# Spring Boot 로그에서 ERROR만 필터링
docker logs my-app 2>&1 | grep ERROR
```

### 컨테이너 내부 접속 (docker exec)

```bash
# 실행 중인 컨테이너에 bash 접속
docker exec -it my-app bash
# Alpine 기반이면 sh 사용
docker exec -it my-app sh

# 단일 명령어 실행 (접속 없이)
docker exec my-app ls -la /app
docker exec my-app java -version
docker exec my-app cat /app/application.yml

# 환경변수 확인
docker exec my-app env
docker exec my-app env | grep SPRING

# 프로세스 확인
docker exec my-app ps aux
```

### 컨테이너 파일 복사 (docker cp)

```bash
# 컨테이너 → 호스트
docker cp my-app:/app/logs/app.log ./app.log

# 호스트 → 컨테이너
docker cp ./config/application.yml my-app:/app/config/
```

### 컨테이너 상세 정보 (docker inspect)

```bash
# 전체 상세 정보 (JSON 형식)
docker inspect my-app

# IP 주소만 추출
docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' my-app

# 마운트 정보
docker inspect -f '{{json .Mounts}}' my-app | python3 -m json.tool

# 환경변수 확인
docker inspect -f '{{json .Config.Env}}' my-app | python3 -m json.tool

# 재시작 정책 확인
docker inspect -f '{{.HostConfig.RestartPolicy.Name}}' my-app
```

### 컨테이너 리소스 사용량 모니터링 (docker stats)

```bash
# 실시간 리소스 사용량 (CPU, 메모리, 네트워크, I/O)
docker stats

# 특정 컨테이너만
docker stats my-app

# 한 번만 출력 (스트리밍 X)
docker stats --no-stream

# 컨테이너 실행 이벤트 모니터링
docker events --filter container=my-app
```

---

## 4.4 리소스 제한

### 메모리 제한

```bash
# 메모리 최대값 설정
docker run -d --memory=512m spring-demo:1.0

# 메모리 + 스왑 합계 설정 (스왑 비활성화: --memory-swap = --memory)
docker run -d --memory=512m --memory-swap=512m spring-demo:1.0

# 메모리 소프트 리밋 (권고 한도, OOM killer 우선순위 조정)
docker run -d --memory=512m --memory-reservation=256m spring-demo:1.0
```

### CPU 제한

```bash
# CPU 할당량 (--cpus: 소수점 가능)
docker run -d --cpus=1.5 spring-demo:1.0   # 1.5 CPU 코어 사용

# 특정 CPU 코어 지정
docker run -d --cpuset-cpus="0,1" spring-demo:1.0   # 0번, 1번 코어만

# CPU 상대 가중치 (기본 1024, 높을수록 우선순위 높음)
docker run -d --cpu-shares=512 spring-demo:1.0
```

### 리소스 사용 확인

```bash
# 제한 확인
docker inspect -f '{{.HostConfig.Memory}}' my-app        # bytes
docker inspect -f '{{.HostConfig.NanoCpus}}' my-app      # nano CPU

# 실시간 사용량
docker stats my-app --no-stream
```

### Spring Boot 운영 권장 리소스 설정

```bash
docker run -d \
  --name spring-app \
  --memory=512m \
  --memory-swap=512m \
  --cpus=1.0 \
  --restart=unless-stopped \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError" \
  spring-demo:1.0
```

---

## 실습

### 실습 4-1: docker run 옵션 종합 실습

```bash
# 1. 기본 실행 (포트, 이름, 환경변수)
docker run -d \
  -p 8080:8080 \
  --name spring-run \
  -e SPRING_PROFILES_ACTIVE=local \
  spring-demo:1.0

# 2. 실행 확인
docker ps

# 3. 로그로 기동 확인
docker logs -f spring-run
# Spring Boot 기동 완료 메시지 확인 후 Ctrl+C

# 4. 애플리케이션 접속
curl http://localhost:8080

# 5. 환경변수 주입 확인
docker exec spring-run env | grep SPRING

# 6. 정리
docker stop spring-run && docker rm spring-run
```

---

### 실습 4-2: 컨테이너 생명주기 실습

```bash
# 1. 컨테이너 시작
docker run -d -p 8080:8080 --name lifecycle-test spring-demo:1.0

# 2. 상태 확인 (Running)
docker ps

# 3. 일시 정지
docker pause lifecycle-test
docker ps   # Status: Up X minutes (Paused)

# → curl http://localhost:8080 접속 시 응답 없음 확인

# 4. 재개
docker unpause lifecycle-test
docker ps   # Status: Up X minutes

# → curl http://localhost:8080 다시 응답 확인

# 5. 중지
docker stop lifecycle-test
docker ps -a  # Status: Exited

# 6. 재시작
docker start lifecycle-test
docker ps     # 다시 Running

# 7. 삭제
docker stop lifecycle-test
docker rm lifecycle-test
docker ps -a  # 사라짐 확인
```

---

### 실습 4-3: 로그 확인 및 컨테이너 접속

```bash
# 1. 앱 실행
docker run -d -p 8080:8080 --name log-test spring-demo:1.0

# 2. 전체 로그 확인
docker logs log-test

# 3. 실시간 로그 (다른 터미널에서 curl 요청하며 확인)
docker logs -f --tail 20 log-test
# 다른 터미널: curl http://localhost:8080/actuator/health

# 4. 컨테이너 내부 접속
docker exec -it log-test sh

# 내부에서 실행할 명령어:
  ls -la /app
  java -version
  env | grep SPRING
  cat /etc/alpine-release   # Alpine 버전 확인
  exit

# 5. 단일 명령어 실행
docker exec log-test ps aux
docker exec log-test df -h

# 6. 파일 복사 (로그 파일이 있다면)
# docker cp log-test:/app/logs/app.log ./

# 7. 정리
docker stop log-test && docker rm log-test
```

---

### 실습 4-4: 리소스 제한 및 모니터링

```bash
# 1. 메모리 제한 없이 실행
docker run -d -p 8080:8080 --name no-limit spring-demo:1.0

# 2. 메모리 512MB 제한으로 실행
docker run -d -p 8081:8080 --memory=512m --cpus=1.0 --name with-limit spring-demo:1.0

# 3. 실시간 리소스 비교
docker stats no-limit with-limit --no-stream

# 4. 제한값 확인
docker inspect no-limit  | grep -A5 '"Memory"'
docker inspect with-limit | grep -A5 '"Memory"'

# 5. 재시작 정책 설정
docker run -d -p 8082:8080 \
  --name restart-test \
  --restart=unless-stopped \
  spring-demo:1.0

docker inspect restart-test | grep -A3 'RestartPolicy'

# 6. 정리
docker stop no-limit with-limit restart-test
docker rm no-limit with-limit restart-test
```

---

### 실습 4-5: --env-file 활용

```bash
# 1. 환경변수 파일 생성
cat > app.env << 'EOF'
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
LOGGING_LEVEL_ROOT=INFO
EOF

# 2. env-file로 실행
docker run -d \
  -p 8080:8080 \
  --name envfile-test \
  --env-file app.env \
  spring-demo:1.0

# 3. 환경변수 주입 확인
docker exec envfile-test env

# 4. 정리
docker stop envfile-test && docker rm envfile-test
rm app.env
```

---

## 핵심 정리

| 명령어 | 용도 |
|--------|------|
| `docker run -d -p -e --name` | 백그라운드 실행, 포트·환경변수·이름 지정 |
| `docker ps / ps -a` | 실행 중 / 전체 컨테이너 목록 |
| `docker stop / start / restart` | 컨테이너 중지 / 시작 / 재시작 |
| `docker rm / rm -f` | 컨테이너 삭제 / 강제 삭제 |
| `docker logs -f --tail` | 실시간 로그 스트리밍 |
| `docker exec -it ... sh` | 컨테이너 내부 접속 |
| `docker stats` | 실시간 리소스 사용량 확인 |
| `docker inspect` | 컨테이너 상세 설정 확인 |
| `--restart=unless-stopped` | 운영 환경 권장 재시작 정책 |
| `--memory / --cpus` | 리소스 제한 설정 |

---

## 참고 자료

- [docker run 공식 레퍼런스](https://docs.docker.com/engine/reference/run/)
- [컨테이너 리소스 제한 문서](https://docs.docker.com/config/containers/resource_constraints/)
- [docker logs 공식 문서](https://docs.docker.com/engine/reference/commandline/logs/)
