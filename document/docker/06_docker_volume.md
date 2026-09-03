# 6. Docker 볼륨과 데이터 관리

> 권장 시간: 1시간  
> 목표: 컨테이너 데이터 영속성 문제를 이해하고 볼륨·바인드 마운트를 목적에 맞게 활용한다

---

## 6.1 볼륨 vs 바인드 마운트 vs tmpfs

### 컨테이너 데이터의 문제

컨테이너는 기본적으로 **컨테이너 레이어에 데이터를 저장**한다.
컨테이너를 삭제하면 그 안의 데이터도 함께 사라진다.

```bash
# 문제 시나리오
docker run -d --name db mysql:8.0 -e MYSQL_ROOT_PASSWORD=secret
# → DB에 데이터 저장

docker rm -f db
# → 모든 데이터 소멸! ❌
```

이를 해결하기 위한 세 가지 스토리지 방식:

```
┌──────────────────────────────────────────────────┐
│                    컨테이너                       │
│  ┌──────────┐  ┌────────────┐  ┌──────────────┐  │
│  │  Volume  │  │Bind Mount  │  │    tmpfs     │  │
│  │(Docker   │  │(호스트 경로│  │(메모리, 임시)│  │
│  │ 관리)    │  │ 직접 연결) │  │              │  │
│  └────┬─────┘  └─────┬──────┘  └──────────────┘  │
└───────┼──────────────┼──────────────────────────┘
        ▼              ▼
  /var/lib/docker  /host/path
  /volumes/...     (호스트 파일시스템)
```

### 볼륨 (Volume)

Docker가 직접 관리하는 스토리지. `/var/lib/docker/volumes/` 에 저장.

```bash
# 볼륨 생성
docker volume create my-volume

# 볼륨 목록
docker volume ls

# 볼륨 상세 정보
docker volume inspect my-volume

# 볼륨 삭제
docker volume rm my-volume

# 사용하지 않는 볼륨 전체 삭제
docker volume prune
```

**특징**
- Docker가 경로를 관리 → OS에 무관하게 동일하게 사용
- 컨테이너 삭제 후에도 데이터 유지
- 여러 컨테이너에서 동시에 마운트 가능
- `docker volume` 명령어로 생명주기 관리

### 바인드 마운트 (Bind Mount)

호스트의 특정 경로를 컨테이너 내부 경로에 직접 연결.

```bash
# 절대 경로로 지정
docker run -v /host/absolute/path:/container/path image

# 현재 디렉토리 기준
docker run -v $(pwd)/config:/app/config image

# 읽기 전용 마운트 (:ro)
docker run -v $(pwd)/config:/app/config:ro image
```

**특징**
- 호스트 파일을 컨테이너에서 실시간으로 사용 가능
- 개발 환경에서 소스 코드 핫 리로드에 유용
- 호스트 경로가 존재해야 함 (Docker가 자동 생성하지 않음)
- 호스트 OS 의존적 (경로 구조가 달라질 수 있음)

### tmpfs 마운트

메모리(RAM)에만 저장되는 임시 스토리지. 컨테이너 종료 시 삭제.

```bash
docker run --tmpfs /tmp:size=100m image
# 또는
docker run --mount type=tmpfs,destination=/tmp,tmpfs-size=100m image
```

**특징**
- 극도로 빠른 I/O (메모리 사용)
- 민감한 임시 데이터 처리에 적합 (디스크에 기록 안 됨)
- 컨테이너 종료 시 완전 소멸

### 세 가지 방식 비교

| 항목 | Volume | Bind Mount | tmpfs |
|------|--------|-----------|-------|
| 저장 위치 | Docker 관리 영역 | 호스트 지정 경로 | 메모리 |
| 영속성 | ✅ 영속 | ✅ 영속 | ❌ 임시 |
| 컨테이너 삭제 후 유지 | ✅ | ✅ (호스트에 남음) | ❌ |
| 주 용도 | DB 데이터, 운영 | 개발 설정 파일 주입 | 임시 캐시 |
| Docker 관리 | ✅ | ❌ | ❌ |
| 성능 | 우수 | OS 의존 | 최고 |

---

## 6.2 데이터베이스 컨테이너 영속성 처리

### 볼륨 없이 실행 시 문제

```bash
# 볼륨 없이 MySQL 실행
docker run -d --name db-no-vol \
  -e MYSQL_ROOT_PASSWORD=secret \
  -e MYSQL_DATABASE=testdb \
  mysql:8.0

# 데이터 입력
docker exec -it db-no-vol mysql -uroot -psecret testdb \
  -e "CREATE TABLE users (id INT, name VARCHAR(50)); INSERT INTO users VALUES (1, 'Alice');"

# 컨테이너 삭제
docker rm -f db-no-vol

# 다시 생성 → 데이터 없음
docker run -d --name db-no-vol \
  -e MYSQL_ROOT_PASSWORD=secret \
  -e MYSQL_DATABASE=testdb \
  mysql:8.0

docker exec db-no-vol mysql -uroot -psecret testdb -e "SELECT * FROM users;"
# ERROR 1146 (42S02): Table 'testdb.users' doesn't exist  ← 데이터 소멸
```

### 볼륨으로 영속성 보장

```bash
# 볼륨 생성
docker volume create mysql-data

# MySQL + 볼륨 연결
docker run -d \
  --name db \
  -v mysql-data:/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=secret \
  -e MYSQL_DATABASE=testdb \
  mysql:8.0

# 데이터 입력
docker exec -it db mysql -uroot -psecret testdb \
  -e "CREATE TABLE users (id INT, name VARCHAR(50)); INSERT INTO users VALUES (1, 'Alice');"

# 컨테이너 삭제
docker rm -f db

# 같은 볼륨으로 새 컨테이너 실행
docker run -d \
  --name db-new \
  -v mysql-data:/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=secret \
  mysql:8.0

# 데이터 유지 확인
docker exec db-new mysql -uroot -psecret testdb -e "SELECT * FROM users;"
# +----+-------+
# | id | name  |
# +----+-------+
# |  1 | Alice |  ← 데이터 유지! ✅
```

### 주요 DB 컨테이너의 데이터 디렉토리

| DB | 컨테이너 내 데이터 경로 |
|----|----------------------|
| MySQL / MariaDB | `/var/lib/mysql` |
| PostgreSQL | `/var/lib/postgresql/data` |
| MongoDB | `/data/db` |
| Redis | `/data` |

---

## 6.3 설정 파일 외부 주입

### application.yml 바인드 마운트

이미지를 재빌드하지 않고 설정만 교체하는 패턴.

```yaml
# config/application-prod.yml (호스트에 준비)
spring:
  datasource:
    url: jdbc:mysql://db:3306/proddb
    username: produser
    password: prodpass
  jpa:
    hibernate:
      ddl-auto: validate

server:
  port: 8080

logging:
  level:
    root: WARN
    com.example: INFO
```

```bash
# 호스트의 설정 파일을 컨테이너에 마운트
docker run -d \
  --name spring-app \
  -p 8080:8080 \
  -v $(pwd)/config/application-prod.yml:/app/config/application.yml:ro \
  -e SPRING_CONFIG_LOCATION=file:/app/config/application.yml \
  spring-demo:1.0
```

### 로그 디렉토리 바인드 마운트

```bash
# 로그를 호스트에 저장 (수집, 분석 용이)
mkdir -p ./logs

docker run -d \
  --name spring-app \
  -p 8080:8080 \
  -v $(pwd)/logs:/app/logs \
  -e LOGGING_FILE_NAME=/app/logs/app.log \
  spring-demo:1.0

# 호스트에서 바로 로그 확인
tail -f ./logs/app.log
```

---

## 6.4 볼륨 백업 및 복구

### 볼륨 백업

```bash
# 방법: 임시 컨테이너로 볼륨을 압축하여 호스트에 저장
docker run --rm \
  -v mysql-data:/source:ro \
  -v $(pwd):/backup \
  alpine \
  tar czf /backup/mysql-backup-$(date +%Y%m%d).tar.gz -C /source .

# 백업 파일 확인
ls -lh mysql-backup-*.tar.gz
```

### 볼륨 복구

```bash
# 새 볼륨 생성
docker volume create mysql-data-restored

# 백업 파일에서 복구
docker run --rm \
  -v mysql-data-restored:/target \
  -v $(pwd):/backup \
  alpine \
  tar xzf /backup/mysql-backup-20240101.tar.gz -C /target

# 복구된 볼륨으로 컨테이너 실행
docker run -d \
  --name db-restored \
  -v mysql-data-restored:/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=secret \
  mysql:8.0
```

### 볼륨 내용 직접 확인

```bash
# 임시 컨테이너로 볼륨 내용 탐색
docker run --rm \
  -v mysql-data:/data \
  alpine \
  ls -la /data
```

---

## 실습

### 실습 6-1: 볼륨 없을 때 데이터 손실 체험

```bash
# 1. 볼륨 없이 MySQL 실행
docker run -d \
  --name no-vol-db \
  -e MYSQL_ROOT_PASSWORD=pass \
  -e MYSQL_DATABASE=demo \
  mysql:8.0

sleep 30  # 기동 대기

# 2. 테이블 생성 및 데이터 입력
docker exec no-vol-db mysql -uroot -ppass demo \
  -e "CREATE TABLE memo(id INT AUTO_INCREMENT PRIMARY KEY, content VARCHAR(100));
      INSERT INTO memo(content) VALUES ('중요한 데이터');
      SELECT * FROM memo;"

# 3. 컨테이너 삭제
docker rm -f no-vol-db

# 4. 동일한 이미지로 재생성
docker run -d \
  --name no-vol-db \
  -e MYSQL_ROOT_PASSWORD=pass \
  -e MYSQL_DATABASE=demo \
  mysql:8.0

sleep 30

# 5. 데이터 확인 → 소멸
docker exec no-vol-db mysql -uroot -ppass demo \
  -e "SELECT * FROM memo;"

# 정리
docker rm -f no-vol-db
```

---

### 실습 6-2: 볼륨으로 DB 영속성 확보

```bash
# 1. 볼륨 생성
docker volume create demo-db-data

# 2. 볼륨 연결 MySQL 실행
docker run -d \
  --name vol-db \
  -v demo-db-data:/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=pass \
  -e MYSQL_DATABASE=demo \
  mysql:8.0

sleep 30

# 3. 데이터 입력
docker exec vol-db mysql -uroot -ppass demo \
  -e "CREATE TABLE memo(id INT AUTO_INCREMENT PRIMARY KEY, content VARCHAR(100));
      INSERT INTO memo(content) VALUES ('볼륨으로 보존된 데이터');
      SELECT * FROM memo;"

# 4. 컨테이너 삭제 (볼륨은 유지)
docker rm -f vol-db

# 볼륨 확인
docker volume ls
docker volume inspect demo-db-data

# 5. 같은 볼륨으로 새 컨테이너 실행
docker run -d \
  --name vol-db-new \
  -v demo-db-data:/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=pass \
  mysql:8.0

sleep 20

# 6. 데이터 유지 확인
docker exec vol-db-new mysql -uroot -ppass demo \
  -e "SELECT * FROM memo;"

# 정리
docker rm -f vol-db-new
docker volume rm demo-db-data
```

---

### 실습 6-3: 설정 파일 바인드 마운트

```bash
# 1. 로컬 설정 파일 준비
mkdir -p ./app-config
cat > ./app-config/application.yml << 'EOF'
server:
  port: 8080

spring:
  application:
    name: docker-demo

logging:
  level:
    root: DEBUG
EOF

# 2. 설정 파일 마운트 후 실행
docker run -d \
  --name config-test \
  -p 8080:8080 \
  -v $(pwd)/app-config:/app/config:ro \
  -e SPRING_CONFIG_LOCATION=file:/app/config/application.yml \
  spring-demo:1.0

# 3. 설정 적용 확인
docker logs config-test | grep "DEBUG\|config"

# 4. 이미지 재빌드 없이 설정만 변경
sed -i 's/DEBUG/WARN/' ./app-config/application.yml
docker restart config-test
docker logs config-test | grep "level"

# 정리
docker stop config-test && docker rm config-test
rm -rf ./app-config
```

---

### 실습 6-4: 볼륨 백업 및 복구

```bash
# 1. 데이터가 있는 볼륨 준비
docker volume create backup-source
docker run -d \
  --name source-db \
  -v backup-source:/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=pass \
  -e MYSQL_DATABASE=demo \
  mysql:8.0

sleep 30
docker exec source-db mysql -uroot -ppass demo \
  -e "CREATE TABLE data(val VARCHAR(100)); INSERT INTO data VALUES ('백업 테스트');"
docker rm -f source-db

# 2. 볼륨 백업
mkdir -p ./backups
docker run --rm \
  -v backup-source:/source:ro \
  -v $(pwd)/backups:/backup \
  alpine \
  tar czf /backup/db-backup.tar.gz -C /source .

ls -lh ./backups/

# 3. 새 볼륨에 복구
docker volume create backup-target
docker run --rm \
  -v backup-target:/target \
  -v $(pwd)/backups:/backup \
  alpine \
  tar xzf /backup/db-backup.tar.gz -C /target

# 4. 복구된 데이터 확인
docker run -d \
  --name restored-db \
  -v backup-target:/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=pass \
  mysql:8.0

sleep 20
docker exec restored-db mysql -uroot -ppass demo \
  -e "SELECT * FROM data;"

# 정리
docker rm -f restored-db
docker volume rm backup-source backup-target
rm -rf ./backups
```

---

## 핵심 정리

| 개념 | 핵심 내용 |
|------|----------|
| 볼륨 | Docker 관리, 운영 데이터 영속성 보장, 컨테이너 삭제 후에도 유지 |
| 바인드 마운트 | 호스트 경로 직접 연결, 개발 환경 설정 파일 주입에 유용 |
| tmpfs | 메모리 저장, 임시 데이터 처리 |
| `-v 볼륨명:/경로` | 네임드 볼륨 마운트 |
| `-v $(pwd)/경로:/경로` | 바인드 마운트 |
| `:ro` | 읽기 전용 마운트 (설정 파일 보호) |
| DB 데이터 경로 | MySQL: `/var/lib/mysql`, PostgreSQL: `/var/lib/postgresql/data` |
| 백업 | 임시 컨테이너 + `tar` 로 볼륨 내용 추출 |

---

## 참고 자료

- [Docker 볼륨 공식 문서](https://docs.docker.com/storage/volumes/)
- [바인드 마운트 공식 문서](https://docs.docker.com/storage/bind-mounts/)
- [스토리지 방식 비교](https://docs.docker.com/storage/)
