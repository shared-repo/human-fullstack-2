# 4. 네트워크와 서비스 운영

> **과정:** 스프링 개발자를 위한 리눅스 기초: 내 애플리케이션을 서버에 배포하기
> **분량:** 8시간
> **실습 환경:** `rocky9-server` VM (스냅샷 `03-Java-설치-완료`), Windows 터미널에서 `ssh spring`으로 접속
> **준비물:** 강사가 배포한 실습용 Spring Boot jar `shop-api-2.0.0.jar` (Windows PC에 저장, 소스는 부록 4-A)

## 이 장의 학습 목표

이 장을 마치면 다음을 할 수 있습니다.

- 서버에 고정 IP를 지정하고, 어떤 프로그램이 어떤 포트에서 요청을 기다리는지 확인할 수 있습니다.
- 방화벽에서 필요한 포트만 열고 닫을 수 있습니다.
- Windows에서 서버로 jar 파일을 전송하고, 첫 배포를 한 뒤 Windows 브라우저로 접속할 수 있습니다.
- Spring Boot 애플리케이션을 systemd 서비스로 등록해, 서버가 재부팅되거나 애플리케이션이 비정상 종료되어도 자동으로 다시 시작되게 할 수 있습니다.
- `journalctl`과 애플리케이션 로그 파일로 장애 원인을 찾고, 로그 파일이 디스크를 가득 채우지 않도록 관리할 수 있습니다.
- MySQL을 설치하고, 애플리케이션 전용 DB 사용자를 만들어 Spring Boot와 연결할 수 있습니다.
- "브라우저에서 접속이 안 됩니다"라는 문제를 단계별로 진단할 수 있습니다.

## 이 장에서 완성할 구성

```text
┌──────────── Windows PC ────────────┐
│  브라우저                           │
│  http://192.168.56.10:8080 ─────┐  │
└─────────────────────────────────│──┘
                                  │ 호스트 전용 네트워크
┌──────── rocky9-server (192.168.56.10) ──────────────────────┐
│  firewalld: ssh(22), 8080/tcp 허용                          │
│                                  ▼                          │
│  systemd ── shop-api.service ── java -jar current.jar       │
│               (User=appuser)       │  :8080                 │
│                                    │                        │
│  systemd ── mysqld.service ◀───────┘  :3306 (외부 차단)     │
│                                                             │
│  로그: journalctl -u shop-api, /var/log/shop-api/app.log    │
└─────────────────────────────────────────────────────────────┘
```

---

## 4.1 네트워크 설정

### 4.1.1 지금의 네트워크 상태 다시 보기

1장에서 VM에 어댑터 두 개를 연결했습니다.

```bash
ip -br addr              # -br: 한 줄씩 간단하게
```

```text
lo       UNKNOWN  127.0.0.1/8 ::1/128
enp0s3   UP       10.0.2.15/24 ...
enp0s8   UP       192.168.56.101/24 ...
```

`enp0s8`의 주소 `192.168.56.101`은 VirtualBox의 DHCP 서버가 **빌려준** 주소입니다. 지금까지는 문제가 없었지만, VM을 여러 대 만들거나 DHCP 설정이 바뀌면 다른 주소를 받을 수 있습니다. 그러면 SSH 설정, 브라우저 주소, 나중에 설정할 Nginx 설정을 모두 고쳐야 합니다. **서버의 주소는 바뀌지 않아야 하므로 고정 IP를 지정합니다.**

```bash
ip route                 # 라우팅 표: 외부로 나가는 길
cat /etc/resolv.conf     # DNS 서버: 도메인 이름을 IP로 바꿔 주는 서버
```

```text
default via 10.0.2.2 dev enp0s3 ...     ← 인터넷으로 나가는 기본 경로는 NAT 어댑터
192.168.56.0/24 dev enp0s8 ...          ← 192.168.56.x는 호스트 전용 어댑터로
```

### 4.1.2 `nmcli`로 고정 IP 지정하기

Rocky Linux는 **NetworkManager**가 네트워크를 관리하고, `nmcli`가 그 명령줄 도구입니다.

```bash
nmcli device status          # 장치 목록과 연결 상태
nmcli connection show        # 연결(설정) 목록
```

```text
NAME    UUID                                  TYPE      DEVICE
enp0s3  2f1a...                               ethernet  enp0s3
enp0s8  8c4b...                               ethernet  enp0s8
```

> **장치(device)와 연결(connection)의 차이:** 장치는 실제 네트워크 카드이고, 연결은 그 카드에 적용할 **설정 묶음**입니다. 대부분 이름이 같지만, 설치 방법에 따라 연결 이름이 `Wired connection 1`처럼 다를 수 있습니다. 아래 명령의 `enp0s8`은 **연결 이름**이므로, 위 결과의 `NAME` 열에 맞게 바꿔 입력합니다.

`192.168.56.10`을 고정 주소로 지정합니다. VirtualBox DHCP 서버가 나눠 주는 범위(`101 ~ 254`) 밖의 주소를 골라야 충돌이 생기지 않습니다.

```bash
sudo nmcli connection modify enp0s8 \
  ipv4.method manual \
  ipv4.addresses 192.168.56.10/24
```

| 설정 | 의미 |
|---|---|
| `ipv4.method manual` | DHCP로 받지 않고 직접 지정 |
| `ipv4.addresses 192.168.56.10/24` | 주소와 네트워크 크기 (`/24`는 `255.255.255.0`) |

게이트웨이(`ipv4.gateway`)는 지정하지 않습니다. 인터넷으로 나가는 길은 NAT 어댑터(`enp0s3`)가 맡고 있기 때문입니다.

설정을 적용합니다. **이 명령을 실행하면 지금의 SSH 연결이 끊깁니다.** 접속한 주소(`.101`)가 사라지기 때문이며, 정상입니다.

```bash
sudo nmcli connection up enp0s8
```

### 4.1.3 새 주소로 다시 접속하기

1. Windows의 `C:\Users\<사용자>\.ssh\config`에서 `HostName`을 바꿉니다.

```text
Host spring
    HostName 192.168.56.10
    User devuser
    IdentityFile ~/.ssh/id_ed25519
```

2. 다시 접속합니다. 처음 보는 주소이므로 지문 확인 질문에 `yes`를 입력합니다.

```powershell
ssh spring
```

3. 주소를 확인합니다.

```bash
ip -br addr show enp0s8              # 192.168.56.10/24
nmcli -g ipv4.method,ipv4.addresses connection show enp0s8
```

> **접속이 되지 않을 때:** VirtualBox 콘솔 창으로 로그인해 `ip -br addr`로 주소를 확인합니다. 오타가 있었다면 `sudo nmcli connection modify enp0s8 ipv4.method auto ipv4.addresses ""`로 DHCP로 되돌린 뒤 `sudo nmcli connection up enp0s8`를 실행하고 다시 시도합니다.

> **선택: 이름으로 접속하기.** Windows의 `C:\Windows\System32\drivers\etc\hosts` 파일을 관리자 권한 메모장으로 열고 `192.168.56.10  spring-server` 한 줄을 추가하면, 브라우저에서 `http://spring-server:8080`처럼 이름으로 접속할 수 있습니다. 서버 안에서는 같은 역할을 `/etc/hosts`가 합니다.

### 4.1.4 `ss`: 누가 어떤 포트에서 기다리는가

서버 프로그램은 특정 **포트**를 열고 요청을 기다립니다(listen). `ss`는 이 상태를 보여 줍니다.

```bash
sudo ss -tlnp
```

| 옵션 | 의미 |
|---|---|
| `-t` | TCP만 |
| `-l` | 요청을 기다리는(listen) 포트만 |
| `-n` | 이름 대신 숫자로 (`ssh` 대신 `22`) |
| `-p` | 그 포트를 연 프로세스 (다른 사용자의 프로세스를 보려면 `sudo` 필요) |

```text
State   Recv-Q  Send-Q  Local Address:Port  Peer Address:Port  Process
LISTEN  0       128           0.0.0.0:22         0.0.0.0:*      users:(("sshd",pid=812,fd=3))
LISTEN  0       128              [::]:22            [::]:*      users:(("sshd",pid=812,fd=4))
```

**`Local Address` 열을 읽는 방법이 매우 중요합니다.**

| Local Address | 의미 | 외부에서 접속 |
|---|---|---|
| `0.0.0.0:8080`, `*:8080`, `[::]:8080` | 서버의 **모든** 네트워크 주소에서 기다림 | 가능 (방화벽이 허용하면) |
| `127.0.0.1:8080` | 서버 **자기 자신**에서 온 요청만 받음 | **불가능** |
| `192.168.56.10:8080` | 그 주소로 온 요청만 받음 | 그 주소로만 가능 |

3장의 실습용 서버를 실행하고 다시 확인해 봅니다.

```bash
java -jar ~/build/shop-api-1.0.1.jar > ~/build/app.out 2>&1 &
sudo ss -tlnp | grep 8080
```

```text
LISTEN  0  50  *:8080  *:*  users:(("java",pid=1720,fd=5))
```

3장에서 "포트가 이미 사용 중"일 때 `pgrep java`로 추측했던 것을, 이제는 **`ss -tlnp`로 정확히 확인**할 수 있습니다. 8080을 차지한 프로세스가 Java가 아닐 수도 있기 때문입니다.

```bash
sudo ss -tlnp | grep :8080       # 8080을 쓰는 프로세스와 PID 확인
```

### 4.1.5 `curl`: 명령줄 HTTP 클라이언트

`curl`은 서버 안에서 웹 요청을 보내 확인하는 도구입니다. 브라우저가 없는 서버에서 "애플리케이션이 응답하는가"를 확인하는 가장 빠른 방법입니다.

```bash
curl localhost:8080/health                  # 응답 본문
curl -i localhost:8080/health               # 응답 헤더 포함 (-i)
curl -v localhost:8080/health               # 연결 과정까지 자세히 (-v)
curl -s -o /dev/null -w "%{http_code}\n" localhost:8080/health   # 상태 코드만
```

| 옵션 | 의미 |
|---|---|
| `-i` | 응답 헤더 포함 |
| `-v` | 연결 과정 전체 출력 (문제 진단용) |
| `-s` | 진행 표시 숨김 |
| `-o 파일` | 본문을 파일로 저장 (`/dev/null`이면 버림) |
| `-w 형식` | 응답 정보 출력 (`%{http_code}`: 상태 코드) |
| `-X 메서드` | GET 외의 메서드 지정 (`-X POST`) |
| `-H "헤더"` | 요청 헤더 추가 |
| `-d '데이터'` | 요청 본문 (지정하면 POST로 보냄) |
| `--max-time 초` | 최대 대기 시간 |

POST 요청은 4.4절에서 Spring Boot 애플리케이션에 보내 봅니다.

> **체크포인트 4.1:** 고정 IP `192.168.56.10`으로 SSH 접속했고, `ss -tlnp`로 8080 포트를 연 프로세스의 PID를 찾았습니다.

---

## 4.2 방화벽

### 4.2.1 지금 Windows에서 접속할 수 있는가

실습용 서버가 실행 중인 상태에서, Windows 브라우저로 `http://192.168.56.10:8080/health`에 접속해 봅니다.

브라우저가 한참 기다리다 **연결할 수 없다**고 표시합니다. 그런데 서버 안에서는 응답합니다.

```bash
curl localhost:8080/health                  # 정상 응답
curl 192.168.56.10:8080/health              # 서버 안에서는 이 주소로도 정상 응답
```

서버 안에서는 되는데 밖에서는 안 되는 경우, 가장 먼저 의심할 것이 **방화벽**입니다.

### 4.2.2 firewalld

Rocky Linux는 **firewalld**가 방화벽을 관리합니다. 기본적으로 꼭 필요한 서비스 외에는 모든 외부 접속을 막습니다.

```bash
sudo firewall-cmd --state                   # running
sudo firewall-cmd --get-active-zones        # 어댑터가 어느 영역(zone)에 속하는가
sudo firewall-cmd --list-all                # 현재 영역의 규칙
```

```text
public (active)
  target: default
  interfaces: enp0s3 enp0s8
  services: cockpit dhcpv6-client ssh
  ports:
  ...
```

| 항목 | 의미 |
|---|---|
| `public` | 영역 이름. 신뢰할 수 없는 네트워크를 위한 기본 영역입니다 |
| `interfaces` | 이 규칙이 적용되는 어댑터 |
| `services` | 허용된 서비스. `ssh`(22번 포트)가 있어서 SSH 접속이 됩니다 |
| `ports` | 번호로 허용한 포트. 지금은 비어 있습니다 |

`cockpit`은 웹 기반 서버 관리 도구용 포트(9090)입니다. 이 과정에서는 사용하지 않으므로 닫아 둡니다.

```bash
sudo firewall-cmd --remove-service=cockpit --permanent
sudo firewall-cmd --reload
```

### 4.2.3 포트 열기

```bash
sudo firewall-cmd --add-port=8080/tcp
```

Windows 브라우저를 새로 고치면 이제 응답이 보입니다.

그런데 이 설정은 **실행 중(runtime) 규칙**에만 들어갔습니다. 확인해 봅니다.

```bash
sudo firewall-cmd --reload                  # 저장된 설정을 다시 읽기
sudo firewall-cmd --list-ports              # 비어 있습니다
```

firewalld의 규칙은 두 가지로 나뉩니다.

| 구분 | 적용 시점 | 재부팅·`--reload` 후 |
|---|---|---|
| 실행 중 (runtime) | 즉시 | 사라집니다 |
| 영구 (`--permanent`) | `--reload` 후 | 유지됩니다 |

영구 규칙으로 추가하고 적용합니다.

```bash
sudo firewall-cmd --add-port=8080/tcp --permanent
sudo firewall-cmd --reload
sudo firewall-cmd --list-all
```

> **"방화벽을 꺼 버리면 되지 않나요?"** `systemctl stop firewalld`로 방화벽을 끄면 당장은 편합니다. 하지만 4.7절에서 설치할 MySQL(3306)도 외부에 그대로 노출됩니다. 인터넷에 연결된 서버라면 몇 분 안에 자동화된 공격 도구가 비밀번호 대입을 시작합니다. 방화벽은 끄지 않고, **필요한 포트만** 엽니다.

### 4.2.4 서비스 이름으로 열기와 포트 닫기

잘 알려진 포트는 서비스 이름으로 열 수 있습니다.

```bash
sudo firewall-cmd --get-services | tr ' ' '\n' | grep -E "^(http|https|mysql)$"
```

```bash
sudo firewall-cmd --add-service=http --permanent        # 80번 포트 (5장에서 사용)
sudo firewall-cmd --reload
sudo firewall-cmd --list-services
```

닫을 때는 `--add`를 `--remove`로 바꿉니다.

```bash
sudo firewall-cmd --remove-service=http --permanent
sudo firewall-cmd --reload
```

> **클라우드의 보안 그룹:** AWS EC2에서는 서버 안의 firewalld 대신 **보안 그룹(Security Group)**이라는 클라우드 방화벽을 주로 사용합니다. 개념은 같습니다. 5.7절의 EC2 시연에서 비교합니다.

실습용 서버를 종료합니다.

```bash
kill $(pgrep java)
```

> **체크포인트 4.2:** Windows 브라우저에서 `http://192.168.56.10:8080/health`의 응답을 확인했고, `8080/tcp`가 영구 규칙으로 등록되어 있습니다.

---

## 4.3 원격 파일 전송: `scp`

### 4.3.1 `scp` 사용법

`scp`(secure copy)는 SSH 연결을 이용해 파일을 복사합니다. SSH 키와 `~/.ssh/config`의 별칭을 그대로 사용할 수 있습니다.

```text
scp  [옵션]  보낼파일  받을위치
             원격 위치는  호스트:경로  형식
```

| 방향 | 명령 (Windows PowerShell에서 실행) |
|---|---|
| Windows → 서버 | `scp .\app.jar spring:/home/devuser/upload/` |
| 서버 → Windows | `scp spring:/var/log/shop-api/app.log .\` |
| 디렉터리 전체 | `scp -r .\config spring:/home/devuser/upload/` |

> `scp`는 **Windows에서 실행**합니다. 서버에 접속한 SSH 창이 아니라, Windows 터미널의 새 탭(PowerShell)을 사용하십시오. 이 장에서는 명령이 실행되는 위치를 다음과 같이 구분합니다.
>
> - `powershell` 블록: Windows PowerShell
> - `bash` 블록: 서버에 SSH로 접속한 창

### 4.3.2 실습: 파일 주고받기

서버에 업로드용 디렉터리를 만듭니다.

```bash
mkdir -p ~/upload
```

Windows에서 파일을 보내고 받아 봅니다.

```powershell
cd $env:USERPROFILE\Downloads
"hello from windows" | Out-File -Encoding ascii hello.txt
scp .\hello.txt spring:/home/devuser/upload/
scp spring:/etc/os-release .\os-release.txt
type .\os-release.txt
```

서버에서 확인합니다.

```bash
cat ~/upload/hello.txt
```

> **다른 방법들:** `sftp spring`은 대화형으로 파일을 주고받는 방법이고, `rsync`는 바뀐 부분만 전송해 큰 디렉터리를 동기화할 때 유리합니다(리눅스·macOS에서 주로 사용). 화면으로 끌어다 놓는 방식이 편하다면 WinSCP 같은 프로그램도 같은 SSH 연결을 사용합니다.

> **체크포인트 4.3:** Windows와 서버 사이에 양방향으로 파일을 전송했습니다.

---

## 4.4 첫 배포

### 4.4.1 배포할 애플리케이션

강사가 배포한 `shop-api-2.0.0.jar`는 지금까지 사용한 실습용 서버와 달리 **실제 Spring Boot 3 애플리케이션**입니다.

| 항목 | 내용 |
|---|---|
| Java | 21 |
| 기능 | 상품 목록 조회·등록 API, Actuator 헬스 체크 |
| `local` 프로파일 | 메모리 DB(H2)를 사용합니다. MySQL 없이 바로 실행할 수 있습니다 |
| `prod` 프로파일 | MySQL에 연결합니다 (4.7절) |

| 메서드와 경로 | 기능 |
|---|---|
| `GET /api/products` | 상품 목록 |
| `GET /api/products/{id}` | 상품 하나 조회 |
| `POST /api/products` | 상품 등록 |
| `GET /api/info` | 애플리케이션 이름, 버전, 활성 프로파일 |
| `GET /actuator/health` | 상태 확인 (DB 연결 상태 포함) |

> **내 프로젝트로 배포하고 싶다면:** IntelliJ나 명령줄에서 jar를 만들 수 있습니다. Gradle은 `.\gradlew bootJar`(결과: `build\libs\`), Maven은 `.\mvnw package`(결과: `target\`)입니다. 단, 이번 장은 강사가 준비한 jar로 진행하고, 내 프로젝트는 5.6절의 종합 프로젝트에서 배포합니다.

### 4.4.2 jar 전송하기

```powershell
cd <shop-api-2.0.0.jar를 저장한 폴더>
scp .\shop-api-2.0.0.jar spring:/home/devuser/upload/
```

수십 MB 크기라 진행률이 표시됩니다. 서버에서 확인합니다.

```bash
ls -lh ~/upload/
```

### 4.4.3 릴리스 디렉터리에 설치하기

3장 실습 C에서 정한 규칙(소유자 `devuser`, 그룹 `appuser`, 권한 `640`)을 지키며 복사해야 합니다. `cp` 후 `chgrp`, `chmod`를 따로 실행해도 되지만, `install` 명령은 이것을 한 번에 합니다.

```bash
install -m 640 -g appuser ~/upload/shop-api-2.0.0.jar /opt/shop-api/releases/
ls -l /opt/shop-api/releases/
```

| 옵션 | 의미 |
|---|---|
| `-m 640` | 복사한 파일의 권한 |
| `-g appuser` | 복사한 파일의 그룹 |

`current.jar` 링크를 새 버전으로 바꿉니다.

```bash
ln -sfn releases/shop-api-2.0.0.jar /opt/shop-api/current.jar
ls -l /opt/shop-api/current.jar
unzip -l /opt/shop-api/current.jar | grep -E "BOOT-INF/(classes|lib)/" | head
```

3장에서 예고한 대로, Spring Boot jar 안에는 우리가 작성한 코드(`BOOT-INF/classes`)와 의존 라이브러리(`BOOT-INF/lib`)가 들어 있습니다.

### 4.4.4 실행하고 브라우저로 확인하기

`appuser`로, `local` 프로파일로 실행합니다. 결과를 바로 볼 수 있도록 포그라운드로 실행합니다.

```bash
sudo -u appuser env SPRING_PROFILES_ACTIVE=local java -jar /opt/shop-api/current.jar
```

Spring Boot 배너와 함께 로그가 출력되고, 마지막에 다음 줄이 나오면 시작이 끝난 것입니다.

```text
... Tomcat started on port 8080 (http) with context path '/'
... Started ShopApiApplication in 7.4 seconds (process running for 8.1)
```

Windows 브라우저에서 다음 주소를 열어 봅니다.

- `http://192.168.56.10:8080/api/products`
- `http://192.168.56.10:8080/api/info`
- `http://192.168.56.10:8080/actuator/health`

**축하합니다. 직접 만든 리눅스 서버에 Spring Boot 애플리케이션을 처음으로 배포했습니다.**

새 SSH 창을 열어 상품을 등록해 봅니다.

```bash
curl -i -X POST localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"키보드","price":45000}'
curl localhost:8080/api/products
```

브라우저를 새로 고치면 등록한 상품이 보입니다. 첫 번째 창에서 `Ctrl + C`로 종료합니다. `local` 프로파일은 메모리 DB를 쓰므로, 다시 실행하면 등록한 상품은 사라집니다.

> **체크포인트 4.4:** Windows 브라우저에서 `/api/products`와 `/actuator/health`의 응답을 확인했습니다.

---

## 4.5 systemd 서비스 등록

### 4.5.1 지금 방식의 문제

지금은 애플리케이션을 이렇게 실행하고 있습니다.

```bash
sudo -u appuser env SPRING_PROFILES_ACTIVE=local nohup java -jar /opt/shop-api/current.jar > /dev/null 2>&1 &
```

| 문제 | 결과 |
|---|---|
| 서버가 재부팅되면 | 아무도 다시 실행하지 않습니다 |
| 애플리케이션이 비정상 종료되면 | 아무도 모르고, 다시 시작되지도 않습니다 |
| 명령이 길고 복잡하면 | 사람마다 다르게 실행하고, 환경 변수 하나를 빠뜨리는 실수가 생깁니다 |
| 실행 중인지 확인하려면 | `ps`, `pgrep`으로 직접 찾아야 합니다 |

### 4.5.2 systemd란

**systemd**는 리눅스가 부팅될 때 가장 먼저 실행되는 프로그램(PID 1)입니다. 다른 모든 서비스를 시작하고, 감시하고, 종료합니다. 이미 `sshd`와 `firewalld`가 systemd 서비스로 실행되고 있습니다.

```bash
ps -p 1 -o pid,comm                         # PID 1은 systemd
systemctl status sshd                       # 3장에서 본 sshd 서비스
systemctl list-units --type=service --state=running
```

systemd가 관리하는 대상을 **유닛(unit)**이라고 하고, 서비스는 `.service` 파일로 정의합니다.

| 위치 | 내용 |
|---|---|
| `/usr/lib/systemd/system/` | 패키지가 설치한 유닛 파일 (직접 수정하지 않습니다) |
| `/etc/systemd/system/` | 관리자가 만든 유닛 파일 (**우리가 만드는 곳**) |

```bash
systemctl cat sshd                          # sshd 서비스의 유닛 파일 보기
```

### 4.5.3 환경 변수 파일 만들기

3.7절에서 "`.bashrc`의 환경 변수는 서비스에 전달되지 않는다"고 했습니다. 서비스에 줄 환경 변수는 별도 파일에 적습니다.

```bash
vim /opt/shop-api/config/shop-api.env
```

```bash
# shop-api 서비스 환경 변수
SPRING_PROFILES_ACTIVE=local
LOGGING_FILE_NAME=/var/log/shop-api/app.log
```

- 한 줄에 `이름=값` 하나씩 씁니다. `export`는 쓰지 않습니다.
- `LOGGING_FILE_NAME`은 Spring Boot의 `logging.file.name` 속성입니다(3.7.4절의 이름 바꾸기 규칙). 로그를 파일에도 남깁니다.

권한을 3장의 규칙에 맞춥니다. 4.7절에서 이 파일에 DB 비밀번호를 넣을 것이므로 다른 사용자는 읽을 수 없어야 합니다.

```bash
chgrp appuser /opt/shop-api/config/shop-api.env
chmod 640 /opt/shop-api/config/shop-api.env
ls -l /opt/shop-api/config/
```

### 4.5.4 유닛 파일 작성하기

`/etc/systemd/system`은 root 소유이므로 `sudo vim`으로 **그 자리에서 바로** 만듭니다.

```bash
sudo vim /etc/systemd/system/shop-api.service
```

```ini
[Unit]
Description=Shop API (Spring Boot)
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=appuser
Group=appuser
WorkingDirectory=/opt/shop-api
EnvironmentFile=/opt/shop-api/config/shop-api.env
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/shop-api/current.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=5
TimeoutStopSec=30

[Install]
WantedBy=multi-user.target
```

| 구역 | 설정 | 의미 |
|---|---|---|
| `[Unit]` | `Description` | `systemctl status`에 표시되는 설명 |
| | `After`, `Wants` | 네트워크가 준비된 뒤에 시작 |
| `[Service]` | `User`, `Group` | `appuser`로 실행 (3장의 `sudo -u appuser`를 대신합니다) |
| | `WorkingDirectory` | 실행 위치. 2.2.4절에서 본 상대 경로의 기준이 됩니다 |
| | `EnvironmentFile` | 환경 변수 파일 |
| | `ExecStart` | 실행할 명령. **반드시 절대 경로**로 씁니다 |
| | `SuccessExitStatus=143` | 아래 설명 참고 |
| | `Restart=on-failure` | 비정상 종료되면 자동으로 다시 시작 |
| | `RestartSec=5` | 다시 시작하기 전에 5초 대기 |
| | `TimeoutStopSec=30` | 종료 요청 후 30초가 지나도 끝나지 않으면 강제 종료 (`SIGKILL`) |
| `[Install]` | `WantedBy=multi-user.target` | 부팅할 때 자동 시작 대상에 포함 |

**`-Xms256m -Xmx512m`**은 Java가 사용할 힙 메모리의 처음 크기와 최대 크기입니다. 지정하지 않으면 Java가 서버 메모리의 일정 비율을 마음대로 가져가므로, 같은 서버에서 MySQL과 함께 실행할 때는 명시하는 것이 좋습니다.

**`SuccessExitStatus=143`**이 필요한 이유: `systemctl stop`은 3.8절에서 배운 `SIGTERM`(15)을 보냅니다. Java는 `SIGTERM`으로 종료되면 종료 코드 `143`(128 + 15)을 돌려줍니다. 이 설정이 없으면 systemd는 정상 종료를 **실패**로 기록합니다.

**`WorkingDirectory=/opt/shop-api`의 숨은 효과:** Spring Boot는 실행 위치의 `./config/` 디렉터리에 있는 `application-{프로파일}.yml`을 **자동으로 읽습니다.** 그래서 2장에서 설계한 `/opt/shop-api/config/application-prod.yml`이 `prod` 프로파일로 실행할 때 자동으로 적용됩니다. 4.7절에서 이 파일을 사용합니다.

### 4.5.5 서비스 시작과 관리

새 유닛 파일을 만들거나 고친 뒤에는 **반드시** systemd에 다시 읽으라고 알려야 합니다.

```bash
sudo systemctl daemon-reload
sudo systemctl start shop-api
systemctl status shop-api
```

```text
● shop-api.service - Shop API (Spring Boot)
     Loaded: loaded (/etc/systemd/system/shop-api.service; disabled; preset: disabled)
     Active: active (running) since Fri 2026-10-02 14:10:03 KST; 12s ago
   Main PID: 2841 (java)
     Memory: 210.4M
     CGroup: /system.slice/shop-api.service
             └─2841 /usr/bin/java -Xms256m -Xmx512m -jar /opt/shop-api/current.jar

Oct 02 14:10:11 spring-server java[2841]: ... Started ShopApiApplication in 7.9 seconds
```

| 항목 | 읽는 법 |
|---|---|
| `Loaded: ... disabled` | 유닛 파일은 읽었지만, 부팅 시 자동 시작은 **아직 꺼져 있습니다** |
| `Active: active (running)` | 실행 중 |
| `Main PID` | 3장에서 `pgrep`으로 찾던 PID |
| 아래 몇 줄 | 최근 로그 (4.6절의 `journalctl`) |

```bash
curl localhost:8080/actuator/health
ps -o user,pid,cmd -p $(systemctl show -p MainPID --value shop-api)   # 실행 사용자가 appuser인지 확인
```

| 명령 | 동작 |
|---|---|
| `sudo systemctl start 서비스` | 시작 |
| `sudo systemctl stop 서비스` | 종료 (`SIGTERM`) |
| `sudo systemctl restart 서비스` | 재시작 (새 jar를 배포한 뒤 사용) |
| `systemctl status 서비스` | 상태와 최근 로그 |
| `sudo systemctl enable 서비스` | 부팅 시 자동 시작 켜기 |
| `sudo systemctl disable 서비스` | 부팅 시 자동 시작 끄기 |
| `sudo systemctl enable --now 서비스` | 자동 시작을 켜고 지금 바로 시작 |
| `systemctl is-active 서비스` | `active` / `inactive` / `failed` 한 단어로 확인 (스크립트에서 사용) |
| `systemctl is-enabled 서비스` | `enabled` / `disabled` |
| `systemctl cat 서비스` | 유닛 파일 내용 |

부팅 시 자동 시작을 켭니다.

```bash
sudo systemctl enable shop-api
systemctl is-enabled shop-api           # enabled
```

### 4.5.6 실습: 자동 재시작 확인하기

**정상 종료는 다시 시작하지 않습니다.**

```bash
sudo systemctl stop shop-api
systemctl status shop-api               # inactive (dead)
sudo systemctl start shop-api
```

**비정상 종료는 다시 시작합니다.** 애플리케이션이 메모리 부족 등으로 갑자기 죽은 상황을 `kill -9`로 만들어 봅니다.

```bash
sudo kill -9 $(systemctl show -p MainPID --value shop-api)
systemctl status shop-api               # activating (auto-restart)
sleep 15
systemctl status shop-api               # 다시 active (running), Main PID가 바뀌었습니다
```

```bash
sudo journalctl -u shop-api -n 20 --no-pager | grep -E "Main process exited|Scheduled restart|Started"
```

```text
shop-api.service: Main process exited, code=killed, status=9/KILL
shop-api.service: Scheduled restart job, restart counter is at 1.
Started Shop API (Spring Boot).
```

### 4.5.7 실습: 재부팅 후 자동 시작 확인하기

```bash
sudo reboot
```

1분 정도 기다린 뒤 다시 접속합니다.

```bash
ssh spring
systemctl status shop-api
```

Windows 브라우저에서 `http://192.168.56.10:8080/api/products`도 확인합니다. **아무 명령도 실행하지 않았는데 애플리케이션이 실행 중입니다.**

> **유닛 파일을 고쳤는데 반영되지 않을 때:** `sudo systemctl daemon-reload`를 빠뜨린 경우가 대부분입니다. `systemctl status`에 `Warning: The unit file ... changed on disk. Run 'systemctl daemon-reload'`라는 경고가 표시됩니다.

> **유닛 파일을 홈 디렉터리에서 만들어 옮기면:** `~/shop-api.service`를 만든 뒤 `sudo mv`로 옮기면 SELinux 보안 정보가 홈 디렉터리의 것으로 남아 systemd가 파일을 읽지 못할 수 있습니다. 이럴 때는 `sudo restorecon -v /etc/systemd/system/shop-api.service`로 바로잡습니다. SELinux는 5.2절에서 자세히 배웁니다.

> **체크포인트 4.5:** `kill -9` 후 자동으로 다시 시작되는 것과, 재부팅 후 자동으로 시작되는 것을 확인했습니다.

---

## 4.6 로그 관리

### 4.6.1 `journalctl`: systemd의 로그

systemd로 실행한 서비스가 화면(표준 출력, 표준 오류)에 출력하는 내용은 모두 **journal**에 저장됩니다. Spring Boot의 콘솔 로그도 여기에 남습니다.

```bash
sudo journalctl -u shop-api                      # shop-api 서비스의 전체 로그 (less로 열림)
sudo journalctl -u shop-api -n 50 --no-pager     # 최근 50줄을 화면에 바로
sudo journalctl -u shop-api -f                   # 실시간 (tail -f와 같은 역할)
```

| 옵션 | 의미 |
|---|---|
| `-u 서비스` | 특정 서비스의 로그만 |
| `-n 숫자` | 최근 몇 줄 |
| `-f` | 실시간으로 계속 보기 |
| `--since`, `--until` | 시간 범위 (`--since "10 min ago"`, `--since "2026-10-02 14:00"`) |
| `-p err` | 오류 이상의 심각도만 |
| `-b` | 이번 부팅 이후의 로그 (`-b -1`: 지난번 부팅) |
| `--no-pager` | `less` 없이 바로 출력 (`grep`과 함께 쓸 때) |
| `-o cat` | 날짜·호스트 정보 없이 메시지만 |

**실습: 원하는 로그만 찾기**

```bash
sudo journalctl -u shop-api --since "30 min ago" --no-pager | grep -E "WARN|ERROR"
sudo journalctl -u shop-api -b --no-pager | grep "Started ShopApiApplication"   # 이번 부팅 후 시작 기록
sudo journalctl -p err -b --no-pager | tail                                       # 시스템 전체의 오류
sudo journalctl --list-boots                     # 부팅 기록 (재부팅 후에도 로그가 남는지 확인)
sudo journalctl --disk-usage                     # journal이 차지한 디스크 용량
```

2장에서 배운 `grep`, `|`, `sort`, `uniq`를 `journalctl` 뒤에 그대로 연결할 수 있습니다.

### 4.6.2 애플리케이션 로그 파일

`LOGGING_FILE_NAME`을 지정했으므로 Spring Boot는 같은 로그를 파일에도 씁니다.

```bash
ls -l /var/log/shop-api/
tail -f /var/log/shop-api/app.log
```

다른 창에서 요청을 보내 로그가 쌓이는 것을 확인합니다.

```bash
curl localhost:8080/api/products
curl localhost:8080/api/products/999         # 없는 상품
```

| 비교 | `journalctl -u shop-api` | `/var/log/shop-api/app.log` |
|---|---|---|
| 기록하는 쪽 | systemd (화면 출력을 받아서) | Spring Boot (Logback) |
| 시작 실패 기록 | **남습니다** (Java 실행 오류, 잘못된 경로 등) | Spring이 시작되기 전의 오류는 남지 않습니다 |
| 검색 | 시간·서비스·심각도로 걸러 보기 편리 | `grep`, `tail`로 확인 |
| 다른 도구로 수집 | 별도 설정 필요 | 로그 수집 도구가 파일을 읽기 쉬움 |

> **장애가 나면 어디부터 보는가:** 서비스가 **시작조차 되지 않으면** `systemctl status`와 `journalctl -u`를 먼저 봅니다. 서비스는 실행 중인데 **요청 처리 중 오류**가 나면 애플리케이션 로그 파일도 함께 봅니다.

### 4.6.3 로그가 디스크를 채우지 않게 하기

로그 파일은 계속 커집니다. 관리하지 않으면 언젠가 디스크를 가득 채워 서버 전체가 멈춥니다. 두 가지 방법이 있습니다.

**방법 1. Spring Boot가 직접 돌리기 (애플리케이션 로그)**

Spring Boot의 파일 로그는 기본적으로 **스스로 파일을 돌립니다(rolling).** 파일이 10MB가 되면 `app.log.2026-10-02.0.gz`처럼 압축해 보관하고 새 파일을 시작하며, 7일이 지난 파일은 지웁니다. 다음 속성으로 조정합니다.

```yaml
logging:
  logback:
    rollingpolicy:
      max-file-size: 10MB        # 파일 하나의 최대 크기
      max-history: 14            # 보관 일수
      total-size-cap: 1GB        # 전체 보관 용량의 상한
```

그래서 Spring Boot의 로그 파일에는 아래의 `logrotate`를 **함께 적용하지 않습니다.** 두 도구가 같은 파일을 동시에 돌리면 로그가 사라지거나 엉뚱한 파일에 쓰일 수 있습니다.

**방법 2. `logrotate` (스스로 돌리지 못하는 프로그램의 로그)**

`logrotate`는 로그 파일을 주기적으로 이름을 바꾸고, 압축하고, 오래된 것을 지우는 시스템 도구입니다. Nginx, MySQL 등 많은 패키지가 설치될 때 `logrotate` 설정을 함께 넣어 둡니다. 3장의 `nohup ... > app.out` 방식으로 만든 로그 파일도 `logrotate`로 관리해야 합니다.

```bash
cat /etc/logrotate.conf          # 기본 설정
ls /etc/logrotate.d/             # 패키지별 설정
cat /etc/logrotate.d/dnf         # 예: dnf의 로그 관리 설정
```

**실습: `logrotate` 동작 확인하기**

시스템 설정을 건드리지 않도록, 홈 디렉터리에서 실험합니다.

```bash
mkdir -m 755 ~/lr                # logrotate는 그룹 쓰기 권한이 있는 디렉터리를 거부합니다
seq 1 10000 > ~/lr/batch.log     # 1부터 10000까지 숫자로 로그 흉내
vim ~/lr/logrotate.conf
```

```text
/home/devuser/lr/batch.log {
    daily
    rotate 3
    compress
    missingok
    notifempty
    copytruncate
}
```

| 설정 | 의미 |
|---|---|
| `daily` | 하루에 한 번 돌립니다 (`weekly`, `monthly`, `size 100M`도 가능) |
| `rotate 3` | 3개까지만 보관하고 더 오래된 것은 지웁니다 |
| `compress` | 돌린 파일을 gzip으로 압축합니다 |
| `missingok` | 파일이 없어도 오류로 처리하지 않습니다 |
| `notifempty` | 비어 있으면 돌리지 않습니다 |
| `copytruncate` | 파일을 복사한 뒤 원래 파일을 비웁니다. 파일을 계속 열어 두고 쓰는 프로그램에 사용합니다 |

설정 파일을 다른 사용자가 고칠 수 없게 권한을 바꿉니다. `logrotate`는 그룹이나 다른 사용자가 쓸 수 있는 설정 파일을 보안상 거부할 수 있습니다.

```bash
chmod 644 ~/lr/logrotate.conf
```

강제로 실행해 봅니다(`-f`). `-s`는 마지막 실행 기록을 저장할 파일입니다.

```bash
logrotate -s ~/lr/state -f ~/lr/logrotate.conf
ls -l ~/lr
```

```text
batch.log          ← 비워졌습니다 (0 바이트)
batch.log.1.gz     ← 이전 내용이 압축되어 보관되었습니다
```

로그를 다시 쓰고 네 번 더 실행해, 3개만 남는지 확인합니다.

```bash
for i in 1 2 3 4; do seq 1 1000 >> ~/lr/batch.log; logrotate -s ~/lr/state -f ~/lr/logrotate.conf; done
ls -l ~/lr                       # batch.log.1.gz ~ batch.log.3.gz만 남았습니다
zcat ~/lr/batch.log.1.gz | head -3   # 압축된 로그를 풀지 않고 보기
```

(`for` 반복문은 5.3절에서 배웁니다.)

시스템에서는 `logrotate`가 **매일 자동으로** 실행되며, `/etc/logrotate.d/`의 모든 설정을 적용합니다.

```bash
systemctl list-timers | grep logrotate
```

> **체크포인트 4.6:** `journalctl -u shop-api`에서 시간과 심각도로 로그를 걸러 보았고, `logrotate`로 오래된 로그가 3개만 남는 것을 확인했습니다.

---

## 4.7 데이터베이스 연동

### 4.7.1 MySQL 설치

Rocky Linux의 AppStream 저장소에 MySQL 8 서버가 들어 있습니다.

```bash
dnf info mysql-server | grep -E "Name|Version"
sudo dnf install -y mysql-server
sudo systemctl enable --now mysqld
systemctl status mysqld
```

4.5절에서 우리가 만든 서비스와 똑같은 방식으로 관리됩니다.

```bash
sudo ss -tlnp | grep mysqld           # 3306 포트 (33060은 MySQL X 프로토콜)
ps -o user,pid,cmd -C mysqld          # mysql 계정으로 실행됩니다 (3.1.4절의 최소 권한 원칙)
sudo ls /var/lib/mysql | head          # 데이터 파일 위치 (2.1절의 /var/lib)
ls /etc/my.cnf /etc/my.cnf.d/          # 설정 파일 위치 (/etc)
```

> **3306 포트는 열지 않습니다.** MySQL은 같은 서버의 Spring Boot만 접속하면 됩니다. `firewall-cmd --list-ports`에 3306이 없으므로 외부에서는 접속할 수 없습니다. 외부 DB 관리 도구로 접속해야 할 때도 포트를 여는 대신 **SSH 터널**을 사용하는 것이 안전합니다(DBeaver, MySQL Workbench 등의 SSH 터널 설정 기능).

### 4.7.2 기본 보안 설정

```bash
sudo mysql_secure_installation
```

| 질문 | 권장 답 | 이유 |
|---|---|---|
| VALIDATE PASSWORD component | `y`, 강도 `1` (MEDIUM) | 약한 비밀번호를 막습니다 |
| root 비밀번호 | 강한 비밀번호 입력 | DB 관리자 계정 보호 |
| Remove anonymous users? | `y` | 이름 없는 사용자 삭제 |
| Disallow root login remotely? | `y` | root는 서버 안에서만 |
| Remove test database? | `y` | 불필요한 DB 삭제 |
| Reload privilege tables now? | `y` | 바로 적용 |

MEDIUM 강도에서는 비밀번호가 **8자 이상, 대문자·소문자·숫자·특수 문자를 모두** 포함해야 합니다.

### 4.7.3 애플리케이션 전용 DB와 사용자 만들기

리눅스에서 애플리케이션을 root가 아닌 `appuser`로 실행하듯, DB에도 **애플리케이션 전용 사용자**를 만들어 그 DB에만 권한을 줍니다.

```bash
mysql -u root -p
```

```sql
CREATE DATABASE shop CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'shop_app'@'localhost' IDENTIFIED BY 'Shop#App2026';
GRANT ALL PRIVILEGES ON shop.* TO 'shop_app'@'localhost';
SHOW GRANTS FOR 'shop_app'@'localhost';
EXIT;
```

| 부분 | 의미 |
|---|---|
| `utf8mb4` | 한글과 이모지까지 저장할 수 있는 문자 집합 |
| `'shop_app'@'localhost'` | **서버 안(localhost)에서 접속할 때만** 허용되는 사용자 |
| `shop.*` | `shop` DB의 모든 테이블에만 권한 |

새 사용자로 접속되는지 확인합니다.

```bash
mysql -u shop_app -p shop -e "SELECT DATABASE(), CURRENT_USER();"
```

> 비밀번호 `Shop#App2026`은 실습용 예시입니다. 실제 서버에서는 반드시 다른 값을 사용하십시오.

### 4.7.4 Spring Boot 설정: 설정 파일과 비밀번호 분리

2장에서 vim으로 만든 `/opt/shop-api/config/application-prod.yml`을 실제 운영 설정으로 고칩니다. (2장 실습에서 포트를 9090으로 바꾸고 여러 곳을 수정했으므로 처음부터 다시 씁니다.)

```bash
vim /opt/shop-api/config/application-prod.yml
```

`:%d`를 입력하면 파일 내용이 모두 지워집니다. 그 다음 `i`를 눌러 다음 내용을 입력합니다.

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/shop
    username: shop_app
    password: ${DB_PASSWORD}

logging:
  file:
    name: /var/log/shop-api/app.log
```

`${DB_PASSWORD}`는 **환경 변수 `DB_PASSWORD`의 값으로 바꿔 넣으라**는 뜻입니다. 이렇게 하면 설정 파일은 Git에 올리거나 다른 사람과 공유해도 되고, 비밀번호는 권한이 제한된 환경 변수 파일에만 남습니다.

환경 변수 파일을 고칩니다.

```bash
vim /opt/shop-api/config/shop-api.env
```

```bash
# shop-api 서비스 환경 변수
SPRING_PROFILES_ACTIVE=prod
DB_PASSWORD=Shop#App2026
```

`LOGGING_FILE_NAME`은 `application-prod.yml`에 적었으므로 지웠습니다.

> **환경 변수 파일의 비밀번호에 쓰면 안 되는 문자:** `EnvironmentFile`은 셸이 아니라 systemd가 읽으므로 대부분의 특수 문자를 그대로 쓸 수 있습니다. 다만 공백, 따옴표(`"`, `'`), 역슬래시(`\`), `$`는 해석이 달라질 수 있으므로 피하는 것이 안전합니다.

모든 설정 파일의 권한이 규칙에 맞는지 확인합니다. vim으로 다시 저장해도 기존 파일의 권한은 유지됩니다.

```bash
ls -l /opt/shop-api/config/          # 모두 devuser appuser, -rw-r-----
```

### 4.7.5 서비스 의존 관계 추가하기

이제 `shop-api`는 MySQL이 먼저 실행되어 있어야 정상적으로 시작됩니다. 부팅할 때 MySQL보다 먼저 시작되지 않도록 유닛 파일에 순서를 지정합니다.

```bash
sudo vim /etc/systemd/system/shop-api.service
```

`[Unit]` 구역을 다음과 같이 고칩니다.

```ini
[Unit]
Description=Shop API (Spring Boot)
After=network-online.target mysqld.service
Wants=network-online.target mysqld.service
```

| 설정 | 의미 |
|---|---|
| `After=mysqld.service` | `mysqld`가 시작된 **뒤에** 시작합니다 (순서) |
| `Wants=mysqld.service` | `shop-api`를 시작할 때 `mysqld`도 함께 시작합니다 (의존) |

```bash
sudo systemctl daemon-reload
sudo systemctl restart shop-api
sudo journalctl -u shop-api -f
```

`The following 1 profile is active: "prod"`와 `HikariPool-1 - Start completed.`, `Started ShopApiApplication`이 보이면 MySQL 연결에 성공한 것입니다. `Ctrl + C`로 빠져나옵니다.

### 4.7.6 동작 확인

```bash
curl localhost:8080/api/info                     # "profiles":["prod"]
curl localhost:8080/actuator/health              # "db":{"status":"UP", ... "database":"MySQL"}
curl -X POST localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"모니터","price":289000}'
```

DB에 실제로 저장되었는지 확인합니다.

```bash
mysql -u shop_app -p shop -e "SHOW TABLES; SELECT * FROM products;"
```

서비스를 재시작해도 데이터가 남아 있습니다. `local` 프로파일의 메모리 DB와 다른 점입니다.

```bash
sudo systemctl restart shop-api
sleep 15
curl localhost:8080/api/products
```

> **`Public Key Retrieval is not allowed` 오류가 날 때:** MySQL 8의 기본 인증 방식과 JDBC 드라이버의 보안 설정 때문에 생기는 오류입니다. 실습 환경에서는 URL 끝에 `?allowPublicKeyRetrieval=true&useSSL=false`를 붙여 해결할 수 있습니다. 운영 환경에서는 SSL 연결을 설정하는 것이 원칙입니다.

> **체크포인트 4.7:** `/actuator/health`에서 `db`의 상태가 `UP`이고, API로 등록한 상품이 MySQL의 `products` 테이블에 저장되었습니다.

---

## 4.8 실습: 장애 진단

### 4.8.1 "브라우저에서 접속이 안 됩니다" 진단 순서

실무에서 가장 많이 받는 문의입니다. 다음 순서로 **서버 안쪽부터 바깥쪽으로** 확인하면 어디서 막혔는지 빠르게 찾을 수 있습니다.

```text
① 서비스가 실행 중인가?          systemctl status shop-api
        │ 아니요 → journalctl -u shop-api 로 시작 실패 원인 확인
        ▼ 예
② 포트에서 기다리고 있는가?       sudo ss -tlnp | grep 8080
        │ 아니요 → 아직 시작 중이거나, 다른 포트로 떴는지 확인
        │ 127.0.0.1:8080 → 외부 요청을 받지 않는 설정 (server.address)
        ▼ 예 (*:8080)
③ 서버 안에서 응답하는가?         curl localhost:8080/actuator/health
        │ 아니요 → 애플리케이션 로그 확인 (오류 응답, DB 연결 등)
        ▼ 예
④ 방화벽이 열려 있는가?           sudo firewall-cmd --list-all
        │ 아니요 → 포트 열기
        ▼ 예
⑤ 주소가 맞는가?                  ip -br addr, 브라우저 주소 확인
```

아래 시나리오들을 이 순서로 진단해 보십시오. 각 시나리오는 **강사가 문제를 만들고 학습자가 진단**하거나, 짝을 지어 서로 문제를 내는 방식으로 진행할 수 있습니다. 해답을 펼치기 전에, 위의 몇 번째 단계에서 원인을 찾았는지 기록합니다.

### 시나리오 A. 포트 충돌

**문제 만들기**

```bash
sudo systemctl stop shop-api
java -jar ~/build/shop-api-1.0.1.jar > ~/build/app.out 2>&1 &    # 3장의 실습용 서버가 8080을 먼저 차지
sudo systemctl start shop-api
```

**증상:** 브라우저의 `/api/products`가 `{"status":"UP","version":"1.0.1"...}` 같은 엉뚱한 응답을 주거나, 404가 납니다. 잠시 후 `systemctl status shop-api`를 보면 `activating (auto-restart)`가 반복됩니다.

<details>
<summary>진단과 해결</summary>

**① 단계**에서 서비스가 정상 실행 상태가 아니라는 것을 알 수 있습니다.

```bash
systemctl status shop-api
sudo journalctl -u shop-api -n 30 --no-pager | grep -A 6 "APPLICATION FAILED"
```

```text
***************************
APPLICATION FAILED TO START
***************************

Description:

Web server failed to start. Port 8080 was already in use.
```

**② 단계**로 누가 8080을 쓰는지 찾습니다.

```bash
sudo ss -tlnp | grep :8080            # users:(("java",pid=3310,...)) 
ps -o user,pid,cmd -p 3310            # devuser가 실행한 shop-api-1.0.1.jar
```

```bash
kill 3310                             # 확인한 PID
sleep 10
systemctl status shop-api             # Restart=on-failure 덕분에 저절로 다시 시작되어 active (running)
```

**배운 점:** systemd 서비스로 운영하는 서버에서 같은 애플리케이션을 손으로 따로 실행하면 안 됩니다. 배포할 때 이전 프로세스가 남아 있는지 `ss -tlnp`로 확인하는 습관을 들이십시오.

</details>

### 시나리오 B. 방화벽

**문제 만들기**

```bash
sudo firewall-cmd --remove-port=8080/tcp
```

(`--permanent` 없이 실행했으므로 실행 중 규칙에서만 빠집니다.)

**증상:** 브라우저가 한참 기다리다 연결할 수 없다고 표시합니다. 서버 안의 `curl`은 정상입니다.

<details>
<summary>진단과 해결</summary>

①, ②, ③ 단계는 모두 정상입니다. **④ 단계**에서 `ports`에 `8080/tcp`가 없는 것을 찾습니다.

```bash
sudo firewall-cmd --list-ports
sudo firewall-cmd --reload            # 영구 규칙에는 남아 있으므로 다시 읽으면 복구됩니다
sudo firewall-cmd --list-ports
```

**배운 점:** "서버 안에서는 되는데 밖에서는 안 된다"면 애플리케이션 문제가 아니라 그 사이(방화벽, 주소)의 문제입니다. 또 `--permanent` 없이 바꾼 규칙은 `--reload`나 재부팅으로 사라진다는 점도 확인했습니다. 반대로, 실행 중 규칙만 추가해 두고 영구 규칙을 빠뜨리면 재부팅 후 갑자기 접속이 안 되는 장애가 생깁니다.

</details>

### 시나리오 C. 외부 요청을 받지 않는 설정

**문제 만들기**

```bash
echo "SERVER_ADDRESS=127.0.0.1" >> /opt/shop-api/config/shop-api.env
sudo systemctl restart shop-api
```

**증상:** 방화벽은 열려 있고 서비스도 실행 중인데 브라우저에서 접속할 수 없습니다. 서버 안의 `curl localhost:8080`은 정상입니다.

<details>
<summary>진단과 해결</summary>

**② 단계**에서 `Local Address`가 `127.0.0.1:8080`인 것을 찾습니다.

```bash
sudo ss -tlnp | grep :8080            # 127.0.0.1:8080
```

환경 변수 `SERVER_ADDRESS`는 Spring Boot의 `server.address` 속성입니다. `127.0.0.1`로 지정하면 서버 자신에게서 온 요청만 받습니다. 원인이 된 줄을 지우고 재시작합니다.

```bash
vim /opt/shop-api/config/shop-api.env    # SERVER_ADDRESS 줄 삭제 (dd)
sudo systemctl restart shop-api
sudo ss -tlnp | grep :8080            # *:8080
```

**배운 점:** 이 설정은 일부러 쓰이기도 합니다. 5장에서 Nginx를 앞에 두면, Spring Boot는 `127.0.0.1`에서만 요청을 받게 하고 외부 요청은 모두 Nginx를 거치도록 만듭니다. **같은 설정이 상황에 따라 장애 원인이 되기도, 보안 설정이 되기도 합니다.**

</details>

### 시나리오 D. DB 중단

**문제 만들기**

```bash
sudo systemctl stop mysqld
```

**증상:** 브라우저의 `/api/products`가 오류(500)를 돌려줍니다. 30초 정도 걸린 뒤에 오류가 나기도 합니다.

<details>
<summary>진단과 해결</summary>

①, ②, ④ 단계는 정상입니다. **③ 단계**에서 상태 확인 결과가 다릅니다.

```bash
curl -s -o /dev/null -w "%{http_code}\n" localhost:8080/actuator/health    # 503
curl localhost:8080/actuator/health                                         # "db":{"status":"DOWN"...}
grep -E "ERROR|Communications link failure" /var/log/shop-api/app.log | tail -5
```

2장 실습 로그에서 보았던 `Communications link failure`가 실제로 나타납니다. DB 서비스 상태를 확인하고 시작합니다.

```bash
systemctl status mysqld               # inactive (dead)
sudo systemctl start mysqld
curl localhost:8080/actuator/health   # 잠시 후 다시 UP
```

애플리케이션은 재시작하지 않아도 DB 연결 풀(HikariCP)이 자동으로 다시 연결합니다.

**더 생각해 보기:** 이번에는 MySQL을 멈춘 상태에서 `shop-api`를 **재시작**해 보십시오. `systemctl status`와 `journalctl`에서 어떤 일이 일어나는지 관찰하고, MySQL을 다시 시작하면 어떻게 되는지 확인해 보십시오. (`Restart=on-failure`가 어떤 역할을 하는지 설명할 수 있어야 합니다.)

</details>

### 시나리오 E. 서비스가 시작되지 않습니다

**문제 만들기**

```bash
rm /opt/shop-api/current.jar
ln -s releases/shop-api-2.0.1.jar /opt/shop-api/current.jar     # 존재하지 않는 버전
sudo systemctl restart shop-api
```

**증상:** 브라우저에서 접속할 수 없습니다.

<details>
<summary>진단과 해결</summary>

**① 단계**에서 서비스가 계속 재시작을 반복하는 것을 찾습니다.

```bash
systemctl status shop-api
sudo journalctl -u shop-api -n 10 --no-pager
```

```text
java[4102]: Error: Unable to access jarfile /opt/shop-api/current.jar
systemd[1]: shop-api.service: Main process exited, code=exited, status=1/FAILURE
```

이 오류는 Spring Boot가 시작되기도 전에 발생했으므로 `/var/log/shop-api/app.log`에는 **아무 기록도 남지 않습니다.** 4.6.2절의 표처럼, 시작 실패는 `journalctl`에서 찾아야 합니다.

```bash
namei -l /opt/shop-api/current.jar     # 3장에서 배운 경로 확인: 링크 대상이 없습니다
ln -sfn releases/shop-api-2.0.0.jar /opt/shop-api/current.jar
sudo systemctl restart shop-api
```

**배운 점:** 배포 직후에는 반드시 `systemctl status`와 헬스 체크로 확인합니다. 5.4절의 배포 스크립트는 이 확인을 자동으로 하고, 실패하면 이전 버전으로 되돌립니다.

</details>

### 실습이 끝나면

모든 시나리오를 복구한 뒤, 정상 상태인지 확인합니다.

```bash
systemctl is-active shop-api mysqld          # active, active
systemctl is-enabled shop-api mysqld         # enabled, enabled
curl localhost:8080/actuator/health
sudo firewall-cmd --list-ports               # 8080/tcp
pgrep -u devuser -a java                     # 손으로 실행한 java가 남아 있지 않아야 합니다
```

VM을 종료하고 스냅샷 `04-서비스-등록-완료`를 만듭니다.

```bash
sudo poweroff
```

> **체크포인트 4.8:** 다섯 가지 시나리오를 진단 순서에 따라 해결하고, 각각 몇 번째 단계에서 원인을 찾았는지 기록했습니다.

---

## 4장 정리

| 절 | 핵심 내용 |
|---|---|
| 4.1 | 서버에는 `nmcli`로 고정 IP를 지정합니다. `ss -tlnp`로 포트와 프로세스를, `Local Address`로 외부 접속 가능 여부를 확인합니다 |
| 4.2 | firewalld는 필요한 포트만 엽니다. `--permanent`로 추가하고 `--reload`로 적용합니다 |
| 4.3 | `scp`는 Windows에서 실행하며, SSH 키와 별칭을 그대로 사용합니다 |
| 4.4 | `install -m 640 -g appuser`로 권한을 지키며 jar를 설치하고, `current.jar` 링크를 바꿉니다 |
| 4.5 | 유닛 파일에 실행 사용자, 작업 디렉터리, 환경 변수 파일, 재시작 정책을 정의합니다. 수정 후에는 `daemon-reload`를 실행합니다 |
| 4.6 | 시작 실패는 `journalctl -u`, 요청 처리 오류는 애플리케이션 로그에서 찾습니다. Spring Boot 로그는 스스로 돌리고, 다른 로그는 `logrotate`가 관리합니다 |
| 4.7 | DB에도 애플리케이션 전용 사용자를 만듭니다. 비밀번호는 설정 파일이 아닌 권한이 제한된 환경 변수 파일에 둡니다 |
| 4.8 | 접속 장애는 서비스 → 포트 → 서버 안 응답 → 방화벽 → 주소 순서로 진단합니다 |

## 4장에서 사용한 명령어

**네트워크와 방화벽**

| 명령어 | 용도 |
|---|---|
| `ip -br addr`, `ip route` | 주소, 라우팅 확인 |
| `nmcli device status`, `nmcli connection show` | 장치, 연결 확인 |
| `nmcli connection modify 이름 ipv4.method manual ipv4.addresses 주소/24` | 고정 IP 지정 |
| `nmcli connection up 이름` | 설정 적용 |
| `sudo ss -tlnp` | 기다리는 포트와 프로세스 |
| `curl -i`, `curl -v`, `curl -X POST -H -d` | HTTP 요청 |
| `firewall-cmd --list-all` | 방화벽 규칙 확인 |
| `firewall-cmd --add-port=8080/tcp --permanent` | 포트 영구 허용 |
| `firewall-cmd --add-service=http --permanent` | 서비스 이름으로 허용 |
| `firewall-cmd --remove-port=... --permanent` | 허용 제거 |
| `firewall-cmd --reload` | 영구 규칙 적용 |
| `scp 파일 호스트:경로`, `scp -r` | 원격 복사 |
| `install -m 권한 -g 그룹 원본 대상` | 권한·그룹을 지정하며 복사 |

**서비스와 로그**

| 명령어 | 용도 |
|---|---|
| `systemctl daemon-reload` | 유닛 파일 다시 읽기 |
| `systemctl start/stop/restart 서비스` | 시작, 종료, 재시작 |
| `systemctl status 서비스` | 상태와 최근 로그 |
| `systemctl enable --now 서비스` | 자동 시작 켜고 바로 시작 |
| `systemctl is-active`, `is-enabled` | 상태를 한 단어로 |
| `systemctl show -p MainPID --value 서비스` | 메인 프로세스의 PID |
| `systemctl cat 서비스` | 유닛 파일 내용 |
| `journalctl -u 서비스 -f` | 서비스 로그 실시간 |
| `journalctl -u 서비스 --since "10 min ago" --no-pager` | 시간 범위로 |
| `journalctl -p err -b` | 이번 부팅 후의 오류 |
| `logrotate -s 상태파일 -f 설정파일` | 로그 돌리기 강제 실행 |
| `zcat 파일.gz` | 압축된 파일 내용 보기 |

**MySQL**

| 명령어 | 용도 |
|---|---|
| `dnf install mysql-server` | MySQL 설치 |
| `mysql_secure_installation` | 기본 보안 설정 |
| `mysql -u 사용자 -p DB` | 접속 |
| `mysql ... -e "SQL"` | SQL 한 줄 실행 |
| `CREATE USER`, `GRANT ... ON db.*` | 전용 사용자와 권한 |

## 4장 체크포인트 제출

다음 네 가지를 제출합니다.

1. Windows 브라우저에서 `http://192.168.56.10:8080/actuator/health`를 연 화면 (`db`가 `UP`)
2. `systemctl status shop-api` 화면 (`enabled`, `active (running)`)
3. `/etc/systemd/system/shop-api.service` 파일 내용
4. 4.8절 시나리오 A~E의 진단 기록 (몇 번째 단계에서, 어떤 명령으로 원인을 찾았는지)

## 확인 문제

**1.** `sudo ss -tlnp`의 결과에 `127.0.0.1:8080`이 표시되었습니다. 이 애플리케이션에 대한 설명으로 알맞은 것은 무엇입니까?

1. 외부에서도 8080 포트로 접속할 수 있습니다
2. 서버 자신에게서 온 요청만 받으므로 외부에서 접속할 수 없습니다
3. 방화벽이 8080 포트를 막고 있습니다
4. 애플리케이션이 실행되지 않았습니다

**2.** `sudo firewall-cmd --add-port=8080/tcp`로 포트를 연 뒤 서버를 재부팅했더니 다시 접속할 수 없게 되었습니다. 원인은 무엇입니까?

1. 재부팅하면 firewalld가 꺼지기 때문입니다
2. `--permanent` 없이 추가해 실행 중 규칙에만 들어갔기 때문입니다
3. 8080은 열 수 없는 포트이기 때문입니다
4. `--reload`를 실행하지 않았기 때문입니다

**3.** 유닛 파일의 `ExecStart`를 수정하고 `sudo systemctl restart shop-api`를 실행했는데 바뀐 내용이 적용되지 않았습니다. 빠뜨린 명령은 무엇입니까?

1. `sudo systemctl enable shop-api`
2. `sudo systemctl daemon-reload`
3. `sudo firewall-cmd --reload`
4. `source ~/.bashrc`

**4.** `Restart=on-failure`로 설정한 서비스에 대한 설명으로 **틀린** 것은 무엇입니까?

1. `kill -9`로 강제 종료되면 다시 시작합니다
2. 오류로 종료(종료 코드 1)되면 다시 시작합니다
3. `systemctl stop`으로 종료하면 다시 시작합니다
4. 서버 재부팅 후 자동 시작 여부는 `enable`이 결정합니다

**5.** 서비스가 시작되자마자 종료되고, `/var/log/shop-api/app.log`에는 아무 기록도 없습니다. 가장 먼저 확인할 것은 무엇입니까?

1. `sudo journalctl -u shop-api`
2. `sudo firewall-cmd --list-all`
3. `mysql -u shop_app -p`
4. Windows 브라우저의 캐시

<details>
<summary>정답과 해설</summary>

1. **2번.** `127.0.0.1`은 서버 자신을 가리키는 주소입니다. 외부 접속을 받으려면 `0.0.0.0`(또는 `*`)에서 기다려야 합니다.
2. **2번.** `--permanent` 없이 추가한 규칙은 실행 중 규칙에만 들어가며, 재부팅이나 `--reload` 후에 사라집니다.
3. **2번.** 유닛 파일을 고친 뒤에는 `daemon-reload`로 systemd가 파일을 다시 읽게 해야 합니다.
4. **3번.** `systemctl stop`은 관리자가 의도한 정상 종료이므로 다시 시작하지 않습니다. `SuccessExitStatus=143`도 이 정상 종료를 실패로 보지 않게 하는 설정입니다.
5. **1번.** 애플리케이션 로그 파일은 Spring Boot가 시작된 뒤에야 기록됩니다. jar 경로 오류, Java 실행 오류처럼 그 전에 실패한 원인은 `journalctl`에 남습니다.

</details>

## 다음 장 미리 보기

이제 애플리케이션은 서비스로 안정적으로 운영되고 있습니다. 하지만 사용자는 `:8080`처럼 포트 번호를 붙여 접속해야 하고, 새 버전을 배포할 때마다 여러 명령을 손으로 입력해야 합니다. 5장에서는 **Nginx**를 앞에 두어 80번 포트로 서비스하고, 그 과정에서 만나는 **SELinux** 문제를 해결해 봅니다. 그리고 4장에서 손으로 한 배포 과정을 **셸 스크립트**로 자동화하고, 지금까지 배운 모든 것을 종합해 여러분의 팀 프로젝트를 배포해 보겠습니다.

---

## 부록 4-A. 실습용 Spring Boot 애플리케이션 (강사용)

`shop-api-2.0.0.jar`의 소스입니다. 강사가 미리 빌드해 학습자에게 배포합니다. JDK 21과 Maven이 설치된 PC에서 `mvn package`를 실행하면 `target/shop-api-2.0.0.jar`가 만들어집니다(IntelliJ에서는 Maven 창의 `package`).

> Spring Boot 버전은 작성 시점의 3.5 버전대를 기준으로 했습니다. 강의장에서 사용하는 버전에 맞게 `<version>`을 조정하고, **수업 전에 반드시 빌드와 실행을 확인**하십시오.

### 디렉터리 구조

```text
shop-api/
├── pom.xml
└── src/main/
    ├── java/com/example/shop/
    │   ├── ShopApiApplication.java
    │   ├── InfoController.java
    │   └── product/
    │       ├── Product.java
    │       ├── ProductController.java
    │       ├── ProductRepository.java
    │       └── ProductRequest.java
    └── resources/
        ├── application.yml
        ├── application-local.yml
        └── application-prod.yml
```

### pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.0</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>shop-api</artifactId>
    <version>2.0.0</version>
    <name>shop-api</name>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### ShopApiApplication.java

```java
package com.example.shop;

import com.example.shop.product.Product;
import com.example.shop.product.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ShopApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopApiApplication.class, args);
    }

    // DB가 비어 있으면 예시 상품을 등록합니다.
    @Bean
    CommandLineRunner seed(ProductRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new Product("노트북", 1290000));
                repository.save(new Product("마우스", 25000));
                repository.save(new Product("USB 허브", 39000));
            }
        };
    }
}
```

### InfoController.java

```java
package com.example.shop;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InfoController {

    private final Environment environment;
    private final String version;

    public InfoController(Environment environment, @Value("${app.version}") String version) {
        this.environment = environment;
        this.version = version;
    }

    @GetMapping("/api/info")
    public Map<String, Object> info() {
        return Map.of(
                "app", "shop-api",
                "version", version,
                "profiles", List.of(environment.getActiveProfiles()));
    }
}
```

### product/Product.java

```java
package com.example.shop.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Product() {
    }

    public Product(String name, int price) {
        this.name = name;
        this.price = price;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
```

### product/ProductRepository.java

```java
package com.example.shop.product;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
```

### product/ProductRequest.java

```java
package com.example.shop.product;

public record ProductRequest(String name, int price) {
}
```

### product/ProductController.java

```java
package com.example.shop.product;

import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductRepository repository;

    public ProductController(ProductRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Product> list() {
        log.info("GET /api/products");
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> get(@PathVariable Long id) {
        log.info("GET /api/products/{}", id);
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("Product not found: id={}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    public ResponseEntity<Product> create(@RequestBody ProductRequest request) {
        log.info("POST /api/products name={}", request.name());
        Product saved = repository.save(new Product(request.name(), request.price()));
        return ResponseEntity.created(URI.create("/api/products/" + saved.getId())).body(saved);
    }
}
```

### resources/application.yml

```yaml
spring:
  application:
    name: shop-api
  jpa:
    open-in-view: false

server:
  shutdown: graceful

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always

app:
  version: 2.0.0
```

### resources/application-local.yml

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:shop
  jpa:
    hibernate:
      ddl-auto: create-drop
```

### resources/application-prod.yml

```yaml
# DB 접속 정보는 서버의 /opt/shop-api/config/application-prod.yml과
# 환경 변수 DB_PASSWORD로 전달합니다.
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

> **`ddl-auto: update`에 대해:** 실습 편의를 위해 애플리케이션이 시작될 때 테이블을 자동으로 만들도록 했습니다. 실제 운영에서는 테이블 구조 변경을 Flyway나 Liquibase 같은 도구로 관리하고 `ddl-auto`는 `validate` 또는 `none`으로 두는 것이 원칙입니다.
