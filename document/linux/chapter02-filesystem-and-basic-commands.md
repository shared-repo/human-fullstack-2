# 2. 파일 시스템과 기본 명령어

> **과정:** 스프링 개발자를 위한 리눅스 기초: 내 애플리케이션을 서버에 배포하기
> **분량:** 8시간
> **실습 환경:** 1장에서 만든 `rocky9-server` VM, Windows 터미널에서 `ssh spring`으로 접속

## 이 장의 학습 목표

이 장을 마치면 다음을 할 수 있습니다.

- 리눅스 디렉터리 구조를 설명하고, Spring Boot 애플리케이션의 jar·설정·로그 파일을 어디에 두어야 하는지 판단할 수 있습니다.
- 절대 경로와 상대 경로를 구분하고, 파일과 디렉터리를 만들고 복사·이동·삭제할 수 있습니다.
- 크기와 용도에 맞는 명령어로 파일 내용을 확인하고, 실시간으로 늘어나는 로그를 지켜볼 수 있습니다.
- 모르는 명령어의 사용법을 `man`과 `--help`로 스스로 찾을 수 있습니다.
- vim으로 서버의 설정 파일을 열어 수정하고 저장할 수 있습니다.
- `grep`, 파이프, 리다이렉션을 조합해 로그에서 필요한 정보만 골라낼 수 있습니다.

## 시작하기 전에

1. VM을 헤드리스로 시작하고 Windows 터미널에서 접속합니다.

```powershell
ssh spring
```

2. 이 장의 실습은 모두 `devuser`의 홈 디렉터리 안에서 진행합니다. 시스템 디렉터리는 **둘러보기만** 하고 수정하지 않습니다.
3. 실습 중 실수로 무언가를 지워도 걱정하지 마십시오. 1장에서 만든 스냅샷 `01-설치-완료`로 언제든 되돌릴 수 있습니다.

---

## 2.1 디렉터리 구조: jar 파일은 어디에 두어야 하는가

### 2.1.1 Windows와 리눅스의 차이

| 구분 | Windows | 리눅스 |
|---|---|---|
| 최상위 위치 | 드라이브마다 따로 있습니다 (`C:\`, `D:\`) | 하나뿐입니다. 모든 것이 `/`(루트) 아래에 있습니다 |
| 경로 구분자 | `\` (역슬래시) | `/` (슬래시) |
| 대소문자 | 구분하지 않습니다 (`App.yml` = `app.yml`) | **구분합니다** (`App.yml` ≠ `app.yml`) |
| 숨김 파일 | 파일 속성으로 지정합니다 | 이름이 `.`으로 시작하면 숨김 파일입니다 (`.bashrc`, `.ssh`) |
| 확장자 | 파일 종류를 결정합니다 | 관례일 뿐입니다. 확장자가 없는 실행 파일도 많습니다 |
| 다른 디스크 | 새 드라이브 문자(`E:\`)로 나타납니다 | 기존 디렉터리 아래에 연결(마운트)합니다 |

> **Spring 개발자가 자주 겪는 문제:** Windows에서 개발할 때는 `application-Prod.yml`과 `--spring.profiles.active=prod`가 문제없이 동작하다가, 리눅스 서버에 올리면 프로파일 설정 파일을 찾지 못하는 경우가 있습니다. 리눅스가 대소문자를 구분하기 때문입니다. 파일 이름은 처음부터 소문자로 통일하는 습관을 들이는 것이 좋습니다.

### 2.1.2 리눅스 디렉터리 지도

리눅스의 디렉터리 배치는 **FHS(Filesystem Hierarchy Standard)**라는 표준을 따릅니다. 그래서 어떤 배포판의 서버에 접속하더라도 설정 파일은 `/etc`, 로그는 `/var/log`에서 찾을 수 있습니다.

```text
/
├── boot      부팅에 필요한 커널과 파일
├── dev       장치 파일 (디스크, 터미널 등)
├── etc       시스템과 프로그램의 설정 파일
├── home      일반 사용자의 홈 디렉터리
│   └── devuser
├── opt       별도로 설치하는 애플리케이션
├── proc      실행 중인 프로세스와 커널 정보 (가상 파일 시스템)
├── root      root 사용자의 홈 디렉터리
├── run       실행 중에만 필요한 임시 정보 (PID 파일 등)
├── srv       서버가 외부에 제공하는 데이터
├── tmp       임시 파일 (재부팅 시 정리될 수 있음)
├── usr       설치된 프로그램과 라이브러리
│   ├── bin   일반 명령어 (ls, cp, java 등)
│   └── lib   라이브러리
└── var       계속 바뀌는 데이터
    ├── lib   프로그램의 데이터 (MySQL 데이터베이스 파일 등)
    └── log   로그 파일
```

| 디렉터리 | 역할 | 이 과정에서 만나는 예 |
|---|---|---|
| `/etc` | 설정 파일 | `/etc/nginx/nginx.conf`, `/etc/my.cnf`, `/etc/ssh/sshd_config` |
| `/home` | 사용자별 개인 공간 | `/home/devuser` |
| `/opt` | 패키지 관리자 없이 직접 설치하는 애플리케이션 | **우리가 만든 Spring Boot jar** |
| `/usr/bin` | 일반 명령어 | `ls`, `vim`, `java` |
| `/var/log` | 로그 | `/var/log/nginx/access.log` |
| `/var/lib` | 프로그램이 관리하는 데이터 | `/var/lib/mysql` |
| `/tmp` | 임시 파일 | Spring Boot 내장 Tomcat의 임시 작업 디렉터리 |
| `/proc` | 커널이 실시간으로 만들어 보여 주는 정보 | `/proc/cpuinfo`, `/proc/meminfo` |

> **참고:** Rocky Linux 9에서 `/bin`, `/sbin`, `/lib`는 각각 `/usr/bin`, `/usr/sbin`, `/usr/lib`를 가리키는 **링크**입니다. 예전에는 따로 있었지만 지금은 하나로 합쳐졌습니다. 링크는 3.4절에서 배웁니다.

### 2.1.3 리눅스에서는 모든 것이 파일입니다

리눅스는 일반 문서뿐 아니라 디스크, 키보드, 실행 중인 프로세스의 정보까지 **파일처럼** 다룹니다. 그래서 파일을 읽는 명령어 하나로 다양한 정보를 확인할 수 있습니다.

```bash
cat /proc/cpuinfo | head -20     # CPU 정보
cat /proc/meminfo | head -5      # 메모리 정보
ls /dev                          # 장치 파일 목록
```

`|`(파이프)와 `head`는 이 장의 뒷부분에서 배웁니다. 지금은 "앞 명령의 결과에서 앞부분만 보여 준다"고만 이해해 두십시오.

### 2.1.4 Spring Boot 애플리케이션은 어디에 두는가

Spring Boot 애플리케이션은 `dnf`로 설치하는 패키지가 아니라 **우리가 직접 가져다 놓는 애플리케이션**입니다. FHS에 따르면 이런 애플리케이션은 `/opt` 아래에 둡니다. 이 과정에서는 다음과 같은 구조를 사용합니다.

```text
/opt/shop-api/                     애플리케이션 전용 디렉터리
├── releases/                      버전별 jar 보관
│   ├── shop-api-1.0.0.jar
│   └── shop-api-1.0.1.jar
├── current.jar → releases/...     현재 실행할 버전을 가리키는 링크 (3.4절)
├── config/                        운영용 설정
│   └── application-prod.yml
├── scripts/                       배포·백업 스크립트 (5.4절)
└── backup/                        배포 직전 백업

/var/log/shop-api/                 애플리케이션 로그
└── app.log
```

| 무엇을 | 어디에 | 이유 |
|---|---|---|
| jar 파일 | `/opt/shop-api/releases` | 직접 설치하는 애플리케이션의 표준 위치입니다. 버전별로 보관해 두면 문제가 생겼을 때 이전 버전으로 바로 돌아갈 수 있습니다 |
| 운영 설정 (`application-prod.yml`) | `/opt/shop-api/config` | jar 밖에 두면 다시 빌드하지 않고도 설정을 바꿀 수 있습니다. DB 비밀번호 같은 민감한 정보를 jar에 넣지 않아도 됩니다 |
| 로그 | `/var/log/shop-api` | 계속 커지는 데이터는 `/var` 아래에 둡니다. 운영자는 로그를 `/var/log`에서 찾습니다 |

> **왜 홈 디렉터리에 두지 않는가:** `~/shop-api.jar`처럼 개인 홈 디렉터리에서 실행하면 처음에는 편합니다. 하지만 그 사용자가 퇴사해 계정이 삭제되면 애플리케이션도 함께 사라지고, 다른 운영자는 파일이 어디 있는지 찾기 어렵습니다. 서버의 파일은 **누가 보아도 예상할 수 있는 위치**에 두는 것이 원칙입니다.

`/opt`에 디렉터리를 만들려면 관리자 권한과 소유권 설정이 필요합니다. 그래서 이 장에서는 같은 구조를 홈 디렉터리 안에 먼저 만들어 보고(2.7절), 3장에서 권한을 배운 뒤 `/opt`로 옮깁니다.

> **체크포인트 2.1:** Spring Boot 애플리케이션의 jar, 운영 설정, 로그를 각각 어느 디렉터리에 두는지와 그 이유를 말할 수 있습니다.

---

## 2.2 기본 명령어

### 2.2.1 명령어의 형식

리눅스 명령어는 대부분 다음 형식을 따릅니다.

```text
명령어  [옵션]  [인자]
ls      -l      /etc
```

| 부분 | 의미 | 예 |
|---|---|---|
| 명령어 | 실행할 프로그램 | `ls` |
| 옵션 | 동작 방식을 바꿉니다. 보통 `-`로 시작합니다 | `-l` (자세히), `-a` (숨김 파일 포함) |
| 인자 | 명령어가 다룰 대상 | `/etc` |

짧은 옵션은 합쳐 쓸 수 있습니다. `ls -l -a -h`와 `ls -lah`는 같습니다. 긴 옵션은 `--`로 시작합니다(`ls --all`).

### 2.2.2 작업 효율을 높이는 단축키

명령어를 배우기 전에, 터미널을 빠르게 다루는 방법부터 익혀 둡니다.

| 키 | 동작 |
|---|---|
| `Tab` | 명령어나 파일 이름 자동 완성. 두 번 누르면 후보 목록이 나옵니다 |
| `↑` / `↓` | 이전에 실행한 명령어 불러오기 |
| `Ctrl + R` | 이전 명령어 검색 (검색어를 입력하고 `Enter`) |
| `Ctrl + C` | 실행 중인 명령어 중단 |
| `Ctrl + L` | 화면 지우기 (`clear`와 같습니다) |
| `Ctrl + A` / `Ctrl + E` | 줄의 맨 앞 / 맨 뒤로 이동 |
| `Ctrl + U` | 커서 앞의 내용 지우기 |

```bash
history          # 실행한 명령어 목록
!42              # history의 42번 명령어를 다시 실행
!!               # 바로 앞 명령어를 다시 실행
sudo !!          # 바로 앞 명령어를 sudo를 붙여 다시 실행
```

> **Tab 키를 습관으로 만드십시오.** 긴 경로를 직접 입력하면 오타가 생기기 쉽습니다. `Tab` 키로 자동 완성이 되지 않는다면, 그 경로가 존재하지 않거나 철자가 틀렸다는 신호입니다.

### 2.2.3 현재 위치 확인과 이동: `pwd`, `cd`

```bash
pwd              # 현재 위치 출력 (print working directory)
```

```text
/home/devuser
```

```bash
cd /etc          # /etc로 이동
pwd
cd               # 인자 없이 실행하면 홈 디렉터리로 이동
cd ~             # 홈 디렉터리로 이동 (위와 같습니다)
cd -             # 바로 전에 있던 디렉터리로 돌아가기
```

### 2.2.4 절대 경로와 상대 경로

| 구분 | 형식 | 기준 | 예 |
|---|---|---|---|
| 절대 경로 | `/`로 시작합니다 | 루트(`/`) | `/home/devuser/practice` |
| 상대 경로 | `/`로 시작하지 않습니다 | 현재 위치 | `practice`, `../devuser` |

상대 경로에서 사용하는 특수 기호입니다.

| 기호 | 의미 |
|---|---|
| `.` | 현재 디렉터리 |
| `..` | 상위 디렉터리 |
| `~` | 내 홈 디렉터리 |

```bash
cd /var/log      # 절대 경로로 이동
cd ..            # 상위 디렉터리(/var)로 이동
pwd
cd ./log         # 상대 경로로 다시 /var/log로 이동 (./는 생략할 수 있습니다)
cd ../../etc     # 두 단계 위로 올라간 뒤 etc로 이동 → /etc
```

> **실무에서는 언제 무엇을 쓰는가:** 터미널에서 직접 이동할 때는 짧은 상대 경로가 편합니다. 하지만 **스크립트, systemd 설정 파일, `application.yml`의 경로 설정에는 절대 경로를 사용**해야 합니다. 상대 경로는 "어디에서 실행했는가"에 따라 가리키는 위치가 달라지기 때문입니다. `logging.file.name: logs/app.log`라고 적으면, jar를 실행한 위치에 따라 로그가 엉뚱한 곳에 만들어집니다.

### 2.2.5 목록 보기: `ls`

```bash
ls               # 현재 디렉터리의 파일 목록
ls /etc          # 지정한 디렉터리의 목록
ls -a            # 숨김 파일 포함 (all)
ls -l            # 자세히 (long)
ls -lh           # 파일 크기를 KB, MB 단위로 (human-readable)
ls -lt           # 수정 시간 순서로 (최근 것이 위)
ls -ltr          # 수정 시간 역순으로 (최근 것이 아래, 로그 확인에 편리)
ls -ld /etc      # 디렉터리 안이 아니라 디렉터리 자신의 정보
```

`ls -l`의 결과를 읽는 방법입니다.

```text
-rw-r--r--. 1 root root 2001 Oct  1 09:12 /etc/passwd
drwxr-xr-x. 2 root root 4096 Oct  1 09:10 /etc/ssh
```

| 위치 | 예 | 의미 |
|---|---|---|
| 첫 글자 | `-`, `d`, `l` | 종류: 일반 파일, 디렉터리, 링크 |
| 다음 9글자 | `rw-r--r--` | 권한 (3.2절에서 자세히 배웁니다) |
| 끝의 `.` | `.` | SELinux 보안 정보가 붙어 있다는 표시 (5.2절) |
| 숫자 | `1` | 링크 수 |
| 첫 번째 이름 | `root` | 소유자 |
| 두 번째 이름 | `root` | 소유 그룹 |
| 숫자 | `2001` | 크기 (바이트) |
| 날짜 | `Oct  1 09:12` | 마지막 수정 시간 |
| 마지막 | `/etc/passwd` | 이름 |

### 2.2.6 디렉터리와 빈 파일 만들기: `mkdir`, `touch`

이제부터 실습용 디렉터리에서 직접 만들어 봅니다.

```bash
cd ~
mkdir practice                         # 디렉터리 하나 만들기
mkdir practice/docs practice/temp      # 여러 개를 한 번에
mkdir practice/a/b/c                   # 오류: 중간 디렉터리 a, b가 없습니다
mkdir -p practice/a/b/c                # -p: 중간 디렉터리까지 한 번에 만듭니다
touch practice/docs/readme.txt         # 빈 파일 만들기 (이미 있으면 수정 시간만 바뀝니다)
ls -R practice                         # 하위 디렉터리까지 모두 보기
```

구조를 나무 모양으로 보여 주는 `tree` 명령은 최소 설치에 들어 있지 않습니다. 패키지 설치는 3.5절에서 자세히 배우지만, 편의를 위해 지금 설치해 둡니다.

```bash
sudo dnf install -y tree
tree practice
```

```text
practice
├── a
│   └── b
│       └── c
├── docs
│   └── readme.txt
└── temp
```

### 2.2.7 복사, 이동, 이름 바꾸기: `cp`, `mv`

```bash
cd ~/practice
cp docs/readme.txt temp/                 # 파일을 temp 디렉터리로 복사
cp docs/readme.txt docs/readme.bak       # 다른 이름으로 복사
cp -r docs docs-backup                   # 디렉터리는 -r(재귀) 옵션이 필요합니다
cp -i docs/readme.txt temp/              # -i: 덮어쓰기 전에 확인
```

`mv`는 **이동**과 **이름 바꾸기**를 모두 합니다. 리눅스에는 이름만 바꾸는 별도 명령이 없습니다.

```bash
mv temp/readme.txt temp/notes.txt        # 이름 바꾸기
mv temp/notes.txt docs/                  # 다른 디렉터리로 이동
mv docs-backup archive                   # 디렉터리 이름 바꾸기
```

> **주의:** `cp`와 `mv`는 같은 이름의 파일이 있으면 **묻지 않고 덮어씁니다.** 중요한 파일을 다룰 때는 `-i` 옵션을 붙이는 습관을 들이십시오. (Rocky Linux에서는 root 사용자에게만 `-i`가 기본으로 설정되어 있습니다.)

### 2.2.8 삭제: `rm`, `rmdir`

```bash
rm docs/readme.bak          # 파일 삭제
rm -i docs/readme.txt       # 삭제 전에 확인 (n을 입력해 취소해 봅니다)
rmdir a/b/c                 # 빈 디렉터리 삭제
rmdir a                     # 오류: 비어 있지 않습니다
rm -r a                     # 디렉터리와 그 안의 모든 것 삭제
```

> **경고: 리눅스에는 휴지통이 없습니다.** `rm`으로 지운 파일은 되돌릴 수 없습니다. 특히 `rm -rf`는 확인 없이 모든 것을 지우므로, 실행하기 전에 다음을 지키십시오.
>
> 1. `pwd`로 현재 위치를 먼저 확인합니다.
> 2. 지울 대상을 `ls`로 먼저 확인한 뒤, 같은 인자로 `rm`을 실행합니다.
> 3. 변수나 와일드카드가 들어간 `rm -rf`는 한 번 더 읽어 봅니다. `rm -rf /opt/shop-api/ *`처럼 공백 하나가 잘못 들어가면 현재 디렉터리의 모든 파일이 삭제됩니다.

### 2.2.9 와일드카드

여러 파일을 한 번에 지정할 때 사용합니다.

| 기호 | 의미 | 예 | 일치하는 이름 |
|---|---|---|---|
| `*` | 0개 이상의 아무 글자 | `*.jar` | `shop-api-1.0.0.jar`, `a.jar` |
| `?` | 정확히 한 글자 | `app-?.log` | `app-1.log`, `app-2.log` |
| `[ ]` | 괄호 안의 글자 중 하나 | `app-[12].log` | `app-1.log`, `app-2.log` |

```bash
cd ~/practice
touch app-1.log app-2.log app-3.log app-10.log shop-api-1.0.0.jar
ls *.log
ls app-?.log            # app-10.log는 나오지 않습니다
ls app-[12].log
rm app-*.log            # 와일드카드로 한 번에 삭제
```

> **체크포인트 2.2:** `~/practice` 안에서 디렉터리를 만들고, 파일을 복사·이동·삭제할 수 있습니다. 절대 경로와 상대 경로의 차이를 예를 들어 설명할 수 있습니다.

---

## 2.3 파일 내용 확인

파일의 크기와 목적에 따라 알맞은 명령을 고릅니다.

| 상황 | 명령어 |
|---|---|
| 짧은 파일 전체 보기 | `cat` |
| 긴 파일을 넘겨 가며 보기, 검색하기 | `less` |
| 파일의 앞부분만 보기 | `head` |
| 파일의 끝부분만 보기 | `tail` |
| 늘어나는 로그를 실시간으로 보기 | `tail -f` |

### 2.3.1 `cat`: 짧은 파일 전체 보기

```bash
cat /etc/os-release
cat -n /etc/os-release       # 줄 번호와 함께
cat /etc/hostname
```

`cat`은 이름 그대로 원래 여러 파일을 이어 붙이는(concatenate) 명령입니다.

```bash
cat /etc/hostname /etc/os-release     # 두 파일을 이어서 출력
```

> **긴 파일에 `cat`을 쓰지 마십시오.** 수만 줄짜리 로그에 `cat`을 실행하면 화면이 끝없이 넘어가고, 원하는 부분은 이미 지나가 버립니다. 이럴 때는 `less`, `tail`, `grep`을 사용합니다.

### 2.3.2 `less`: 긴 파일 넘겨 보기

```bash
less /etc/services
```

| 키 | 동작 |
|---|---|
| `Space` / `b` | 한 화면 아래로 / 위로 |
| `↑` / `↓` | 한 줄씩 이동 |
| `g` / `G` | 파일의 맨 처음 / 맨 끝 |
| `/단어` | 아래 방향으로 검색 |
| `?단어` | 위 방향으로 검색 |
| `n` / `N` | 다음 / 이전 검색 결과 |
| `q` | 종료 |

**실습:** `/etc/services`에서 `/8080`을 검색해 보십시오. 이 파일은 잘 알려진 포트 번호와 서비스 이름을 정리한 목록입니다. 8080 포트에는 어떤 이름이 붙어 있습니까?

> `less`의 키 조작은 2.4절의 `man`에서도 똑같이 사용합니다.

### 2.3.3 `head`, `tail`: 앞부분과 끝부분 보기

```bash
head /etc/passwd             # 앞 10줄 (기본값)
head -n 3 /etc/passwd        # 앞 3줄
tail /etc/passwd             # 끝 10줄
tail -n 3 /etc/passwd        # 끝 3줄
tail -n +5 /etc/passwd       # 5번째 줄부터 끝까지
```

`/etc/passwd`는 사용자 계정 목록입니다. 마지막 줄에서 1장에서 만든 `devuser`를 찾을 수 있습니다. 각 항목의 의미는 3.1절에서 배웁니다.

### 2.3.4 `tail -f`: 실시간 로그 보기

운영 중인 서버에서 가장 많이 사용하는 명령 중 하나입니다. `-f`(follow) 옵션을 붙이면 파일 끝을 보여 준 뒤 종료하지 않고, 새로운 줄이 추가될 때마다 바로 화면에 출력합니다.

**실습: 터미널 두 개로 확인하기**

1. Windows 터미널에서 새 탭을 열고 `ssh spring`으로 한 번 더 접속합니다. 이제 두 개의 창이 있습니다.
2. **창 1**에서 빈 로그 파일을 만들고 지켜봅니다.

```bash
touch ~/practice/live.log
tail -f ~/practice/live.log
```

3. **창 2**에서 로그 파일에 한 줄씩 추가합니다. (`>>`는 2.6절에서 배웁니다. 파일 끝에 내용을 덧붙이는 기호입니다.)

```bash
echo "애플리케이션 시작" >> ~/practice/live.log
echo "GET /api/products" >> ~/practice/live.log
echo "ERROR 데이터베이스 연결 실패" >> ~/practice/live.log
```

4. 창 1에 한 줄씩 바로 나타나는 것을 확인합니다.
5. 창 1에서 `Ctrl + C`를 눌러 종료합니다.

4장에서 Spring Boot 애플리케이션을 서비스로 실행하면, 이 방법으로 요청이 들어올 때마다 로그가 쌓이는 것을 실시간으로 확인하게 됩니다.

> **`tail -f`와 `tail -F`:** 로그 파일은 일정 크기가 되면 이름이 바뀌고 새 파일이 만들어집니다(4.6절의 `logrotate`). `-f`는 이름이 바뀐 예전 파일을 계속 지켜보지만, `-F`는 같은 이름으로 새로 만들어진 파일을 다시 따라갑니다. 운영 서버에서 오래 지켜볼 때는 `tail -F`가 더 안전합니다.

> **체크포인트 2.3:** 두 개의 SSH 창에서 `tail -f`로 로그가 실시간으로 추가되는 것을 확인했습니다.

---

## 2.4 도움말 찾기

리눅스 명령어와 옵션을 모두 외울 필요는 없습니다. 대신 **필요할 때 정확한 사용법을 찾는 방법**을 익혀 둡니다. 서버에 인터넷 검색 환경이 없을 때도 도움말은 항상 서버 안에 있습니다.

### 2.4.1 `--help`: 빠른 요약

대부분의 명령어는 `--help` 옵션으로 짧은 사용법을 보여 줍니다.

```bash
ls --help
cp --help | less          # 내용이 길면 less로 넘겨 봅니다
```

### 2.4.2 `man`: 자세한 매뉴얼

```bash
man ls
```

`man` 화면은 `less`와 같은 키로 조작합니다. 매뉴얼은 보통 다음 순서로 구성됩니다.

| 항목 | 내용 |
|---|---|
| NAME | 명령어 이름과 한 줄 설명 |
| SYNOPSIS | 사용 형식. `[ ]` 안은 생략할 수 있는 부분입니다 |
| DESCRIPTION | 자세한 설명과 옵션 목록 |
| EXAMPLES | 사용 예 (없는 명령어도 많습니다) |
| SEE ALSO | 관련 명령어 |

**실습:** `man ls`에서 `/-S`를 검색해 보십시오. 파일을 크기 순서로 정렬하는 옵션입니다. 홈 디렉터리에서 직접 실행해 봅니다.

매뉴얼은 여러 **섹션**으로 나뉘어 있습니다. 같은 이름이 여러 섹션에 있을 때는 번호를 지정합니다.

| 섹션 | 내용 | 예 |
|---|---|---|
| 1 | 일반 명령어 | `man 1 passwd` (비밀번호 변경 명령) |
| 5 | 설정 파일의 형식 | `man 5 passwd` (`/etc/passwd` 파일의 형식) |
| 8 | 관리자 명령어 | `man 8 useradd` |

> **`man` 명령이 없다고 나올 때:** 설치 환경에 따라 매뉴얼 프로그램이 빠져 있을 수 있습니다. `sudo dnf install -y man-db man-pages`로 설치합니다.

### 2.4.3 명령어의 정체 확인하기

```bash
type ls          # ls가 별칭(alias)인지, 실제 프로그램인지 확인
type cd          # 셸에 들어 있는 내장 명령
which java       # 실행 파일의 위치 (3.6절에서 Java를 설치한 뒤 다시 확인합니다)
which vim
```

```text
ls is aliased to `ls --color=auto'
cd is a shell builtin
```

`cd`처럼 셸에 내장된 명령은 `man cd`로는 찾을 수 없고, `help cd`로 확인합니다.

> **체크포인트 2.4:** `man`이나 `--help`를 이용해, 배우지 않은 옵션(예: `ls -S`)의 의미를 스스로 찾아 사용해 보았습니다.

---

## 2.5 vim 필수 사용법

### 2.5.1 왜 vim을 배워야 하는가

운영 서버에는 GUI가 없으므로 VS Code나 IntelliJ로 파일을 열 수 없습니다. 서버의 설정 파일을 수정하려면 터미널 안에서 동작하는 편집기가 필요합니다. vi와 vim은 **거의 모든 리눅스 서버에 기본으로 설치되어 있는** 편집기라, 어떤 서버에 접속하더라도 사용할 수 있습니다.

Rocky Linux 최소 설치에는 기능을 줄인 `vi`만 들어 있습니다. 문법 강조 등 기능이 더 많은 `vim`을 설치합니다.

```bash
sudo dnf install -y vim-enhanced
```

### 2.5.2 vim의 모드

vim이 처음에 어렵게 느껴지는 이유는 **모드**가 있기 때문입니다. 일반 편집기처럼 열자마자 글자를 입력하면, 입력한 글자가 명령으로 해석됩니다.

```text
                 i, a, o
  ┌────────────┐ ───────▶ ┌────────────┐
  │ 일반 모드   │          │ 입력 모드   │
  │ (Normal)   │ ◀─────── │ (Insert)   │
  └────────────┘   Esc    └────────────┘
     │     ▲
   : │     │ Enter 또는 Esc
     ▼     │
  ┌────────────┐
  │ 명령 모드   │  :w 저장, :q 종료
  │ (Command)  │
  └────────────┘
```

| 모드 | 하는 일 | 들어가는 방법 |
|---|---|---|
| 일반 모드 | 이동, 삭제, 복사, 붙여넣기 | vim을 열었을 때의 기본 모드. 다른 모드에서 `Esc` |
| 입력 모드 | 글자 입력 | 일반 모드에서 `i`, `a`, `o` |
| 명령 모드 | 저장, 종료, 바꾸기 | 일반 모드에서 `:` |

> **길을 잃었을 때:** 지금 어떤 모드인지 모르겠다면 `Esc`를 두세 번 누르십시오. 항상 일반 모드로 돌아옵니다. 그 다음 `:q!`를 입력하고 `Enter`를 누르면 저장하지 않고 빠져나올 수 있습니다.

### 2.5.3 꼭 알아야 할 명령

**입력 모드로 들어가기**

| 키 | 동작 |
|---|---|
| `i` | 커서 앞에서 입력 시작 |
| `a` | 커서 뒤에서 입력 시작 |
| `o` | 아래에 새 줄을 만들고 입력 시작 |
| `O` | 위에 새 줄을 만들고 입력 시작 |

**저장과 종료 (명령 모드)**

| 명령 | 동작 |
|---|---|
| `:w` | 저장 |
| `:q` | 종료 (바뀐 내용이 있으면 거부됩니다) |
| `:wq` | 저장하고 종료 |
| `:q!` | 저장하지 않고 강제 종료 |

**이동 (일반 모드)**

| 키 | 동작 |
|---|---|
| 방향키 또는 `h` `j` `k` `l` | 왼쪽, 아래, 위, 오른쪽 |
| `0` / `$` | 줄의 맨 앞 / 맨 끝 |
| `gg` / `G` | 파일의 맨 처음 / 맨 끝 |
| `:25` | 25번째 줄로 이동 |

**편집 (일반 모드)**

| 키 | 동작 |
|---|---|
| `x` | 커서 위치의 글자 삭제 |
| `dd` | 현재 줄 삭제(잘라내기) |
| `yy` | 현재 줄 복사 |
| `p` | 커서 아래에 붙여넣기 |
| `u` | 되돌리기 |
| `Ctrl + r` | 다시 실행 |
| `3dd` | 숫자를 앞에 붙이면 반복합니다 (3줄 삭제) |

**검색과 바꾸기**

| 명령 | 동작 |
|---|---|
| `/단어` | 아래 방향으로 검색, `n` / `N`으로 다음 / 이전 결과 |
| `:%s/old/new/g` | 파일 전체에서 `old`를 `new`로 모두 바꾸기 |
| `:set number` | 줄 번호 표시 |

### 2.5.4 실습: Spring 설정 파일 편집하기

1. 새 파일을 엽니다.

```bash
cd ~/practice
vim application-prod.yml
```

2. `i`를 눌러 입력 모드로 들어가 다음 내용을 입력합니다. **들여쓰기는 반드시 스페이스 2칸**으로 입력합니다.

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/shop
    username: shop_user
    password: changeme

logging:
  file:
    name: /var/log/shop-api/app.log
```

3. `Esc`를 누르고 `:wq`로 저장하고 종료합니다.
4. `cat application-prod.yml`로 내용을 확인합니다.
5. 다시 `vim application-prod.yml`로 열고 다음을 수정해 봅니다.
   - `/port`로 검색해 이동한 뒤, `8080`을 `9090`으로 바꿉니다. (`x`로 지우고 `i`로 입력)
   - `:%s/shop_user/shop_app/g`로 사용자 이름을 한 번에 바꿉니다.
   - `logging:`으로 시작하는 줄에서 `3dd`로 세 줄을 삭제한 뒤, `u`로 되돌립니다.
   - 마지막 줄로 이동(`G`)하고 `o`를 눌러 새 줄에 `# 2장 실습 완료`를 입력합니다.
6. `:wq`로 저장합니다.

> **YAML과 탭 문자:** YAML은 들여쓰기에 탭 문자를 허용하지 않습니다. 서버에서 `application.yml`을 수정하다 탭 키를 누르면, Spring Boot가 시작할 때 설정 파일을 읽지 못하고 오류를 냅니다. 아래 2.5.5절처럼 vim이 탭 키를 스페이스로 바꾸도록 설정해 두면 이런 실수를 막을 수 있습니다.

### 2.5.5 vim 환경 설정

홈 디렉터리의 `.vimrc` 파일에 설정을 적어 두면 vim을 열 때마다 적용됩니다.

```bash
vim ~/.vimrc
```

```vim
set number          " 줄 번호 표시
set expandtab       " 탭 키를 누르면 스페이스로 입력
set tabstop=2       " 탭 하나를 스페이스 2칸으로
set shiftwidth=2    " 자동 들여쓰기도 2칸
syntax on           " 문법 강조
```

저장한 뒤 `application-prod.yml`을 다시 열어 줄 번호와 색상이 표시되는지 확인합니다.

### 2.5.6 더 연습하고 싶다면

vim에는 30분 정도 걸리는 대화형 연습 프로그램이 들어 있습니다. 수업이 끝난 뒤 한 번 따라 해 보는 것을 권장합니다.

```bash
vimtutor
```

> **체크포인트 2.5:** vim으로 `application-prod.yml`을 만들고, 검색·바꾸기·삭제·되돌리기를 사용해 수정한 뒤 저장했습니다.

---

## 2.6 텍스트 처리: 로그에서 원하는 정보 찾기

### 2.6.1 실습용 로그 파일 만들기

이 절에서는 Spring Boot 애플리케이션이 실제로 남기는 형식의 로그로 실습합니다. 아래 블록 **전체**를 복사해 SSH 터미널에 붙여넣고 `Enter`를 누르십시오. 첫 줄의 `<< 'EOF'`부터 마지막 줄의 `EOF`까지 사이의 내용이 `app.log` 파일로 저장됩니다.

```bash
cat > ~/practice/app.log << 'EOF'
2026-10-01T09:00:01.102+09:00  INFO 2481 --- [shop-api] [           main] c.e.shop.ShopApiApplication              : Starting ShopApiApplication v1.0.0 using Java 21.0.4 with PID 2481
2026-10-01T09:00:01.108+09:00  INFO 2481 --- [shop-api] [           main] c.e.shop.ShopApiApplication              : The following 1 profile is active: "prod"
2026-10-01T09:00:03.415+09:00  INFO 2481 --- [shop-api] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port 8080 (http)
2026-10-01T09:00:04.020+09:00  INFO 2481 --- [shop-api] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2026-10-01T09:00:04.512+09:00  INFO 2481 --- [shop-api] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2026-10-01T09:00:06.731+09:00  INFO 2481 --- [shop-api] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080 (http) with context path '/'
2026-10-01T09:00:06.745+09:00  INFO 2481 --- [shop-api] [           main] c.e.shop.ShopApiApplication              : Started ShopApiApplication in 6.112 seconds (process running for 6.874)
2026-10-01T09:05:12.301+09:00  INFO 2481 --- [shop-api] [nio-8080-exec-1] c.e.shop.controller.ProductController    : GET /api/products
2026-10-01T09:05:40.118+09:00  INFO 2481 --- [shop-api] [nio-8080-exec-2] c.e.shop.controller.ProductController    : GET /api/products/12
2026-10-01T09:06:02.457+09:00  WARN 2481 --- [shop-api] [nio-8080-exec-3] c.e.shop.service.ProductService          : Product not found: id=999
2026-10-01T09:07:15.880+09:00  INFO 2481 --- [shop-api] [nio-8080-exec-4] c.e.shop.controller.OrderController      : POST /api/orders
2026-10-01T09:07:15.902+09:00 ERROR 2481 --- [shop-api] [nio-8080-exec-4] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] threw exception
java.lang.NullPointerException: Cannot invoke "com.example.shop.domain.Member.getId()" because "member" is null
    at com.example.shop.service.OrderService.createOrder(OrderService.java:42)
    at com.example.shop.controller.OrderController.create(OrderController.java:31)
2026-10-01T09:10:33.540+09:00  INFO 2481 --- [shop-api] [nio-8080-exec-5] c.e.shop.controller.ProductController    : GET /api/products
2026-10-01T09:12:47.009+09:00  WARN 2481 --- [shop-api] [l-1 housekeeper] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Thread starvation or clock leap detected
2026-10-01T09:15:08.214+09:00  INFO 2481 --- [shop-api] [nio-8080-exec-7] c.e.shop.controller.MemberController     : GET /api/members/3
2026-10-01T09:15:59.630+09:00  INFO 2481 --- [shop-api] [nio-8080-exec-8] c.e.shop.controller.OrderController      : POST /api/orders
2026-10-01T09:16:00.011+09:00 ERROR 2481 --- [shop-api] [nio-8080-exec-8] o.h.engine.jdbc.spi.SqlExceptionHelper   : Duplicate entry 'ORD-20261001-0007' for key 'orders.order_no'
2026-10-01T09:20:21.775+09:00  INFO 2481 --- [shop-api] [nio-8080-exec-9] c.e.shop.controller.ProductController    : GET /api/products
2026-10-01T09:21:44.302+09:00  WARN 2481 --- [shop-api] [io-8080-exec-10] .m.m.a.DefaultHandlerExceptionResolver   : Resolved [org.springframework.web.HttpRequestMethodNotSupportedException: Request method 'DELETE' is not supported]
2026-10-01T09:25:03.866+09:00  INFO 2481 --- [shop-api] [nio-8080-exec-1] c.e.shop.controller.ProductController    : GET /api/products/12
2026-10-01T09:30:17.452+09:00 ERROR 2481 --- [shop-api] [nio-8080-exec-2] o.h.engine.jdbc.spi.SqlExceptionHelper   : Communications link failure
2026-10-01T09:30:40.093+09:00  INFO 2481 --- [shop-api] [nio-8080-exec-3] c.e.shop.controller.ProductController    : GET /api/products
2026-10-01T09:31:26.517+09:00  INFO 2481 --- [shop-api] [nio-8080-exec-4] c.e.shop.controller.MemberController     : POST /api/members
EOF
```

파일이 제대로 만들어졌는지 확인합니다.

```bash
wc -l ~/practice/app.log
```

```text
26 /home/devuser/practice/app.log
```

Spring Boot 기본 로그 한 줄은 다음과 같이 구성되어 있습니다.

```text
2026-10-01T09:06:02.457+09:00  WARN 2481 --- [shop-api] [nio-8080-exec-3] c.e.shop.service.ProductService  : Product not found: id=999
└──────────── 시간 ──────────┘ └레벨┘ └PID┘     └앱 이름┘ └──── 스레드 ────┘ └────── 로거(클래스) ──────┘   └──── 메시지 ────┘
```

예외가 발생하면 `java.lang...`으로 시작하는 예외 이름과 `at ...`으로 시작하는 **스택 트레이스**가 여러 줄에 걸쳐 이어집니다.

이후 실습은 모두 `~/practice`에서 진행합니다.

```bash
cd ~/practice
```

### 2.6.2 `grep`: 원하는 줄만 골라내기

`grep`은 파일에서 **특정 문자열이 들어 있는 줄**만 출력합니다. 로그 분석에서 가장 많이 사용하는 명령입니다.

```bash
grep ERROR app.log                # ERROR가 들어 있는 줄
grep -n ERROR app.log             # 줄 번호와 함께 (-n)
grep -c ERROR app.log             # 일치하는 줄의 개수만 (-c)
grep -i exception app.log         # 대소문자 구분 없이 (-i)
grep -v INFO app.log              # INFO가 들어 있지 않은 줄 (-v, 반대로)
grep -E "ERROR|WARN" app.log      # ERROR 또는 WARN (-E, 확장 정규식)
```

| 옵션 | 의미 |
|---|---|
| `-n` | 줄 번호 표시 |
| `-c` | 일치하는 줄의 개수 |
| `-i` | 대소문자 무시 |
| `-v` | 일치하지 **않는** 줄 |
| `-E` | 확장 정규식 사용 (`\|`로 여러 패턴) |
| `-r` | 디렉터리 안의 모든 파일에서 검색 |
| `-A 숫자` / `-B 숫자` / `-C 숫자` | 일치한 줄의 뒤 / 앞 / 앞뒤 몇 줄 함께 출력 |

**예외의 앞뒤 확인하기**

`grep NullPointerException app.log`만으로는 어디서 예외가 났는지 알 수 없습니다. 스택 트레이스는 다음 줄들에 있기 때문입니다.

```bash
grep -A 2 NullPointerException app.log     # 예외 줄과 그 뒤 2줄
grep -B 1 NullPointerException app.log     # 예외 줄과 그 앞 1줄 (어떤 요청이었는지)
```

결과에서 예외가 `OrderService.java`의 42번째 줄에서 발생했다는 것을 확인할 수 있습니다. 실무에서 장애가 나면 이 방법으로 가장 먼저 원인 위치를 찾습니다.

**설정 파일에서 찾기**

```bash
grep -n port application-prod.yml
grep -r "8080" ~/practice              # practice 안의 모든 파일에서 8080 찾기
```

> **문자열에 공백이나 특수 문자가 있을 때:** `grep "GET /api/products" app.log`처럼 큰따옴표로 감쌉니다. 따옴표가 없으면 셸이 공백을 기준으로 인자를 나누어, `/api/products`를 검색어가 아닌 파일 이름으로 해석합니다.

### 2.6.3 입출력과 리다이렉션

리눅스 명령은 세 가지 통로로 데이터를 주고받습니다.

| 이름 | 번호 | 기본 연결 | 예 |
|---|---|---|---|
| 표준 입력 (stdin) | 0 | 키보드 | 명령이 읽는 데이터 |
| 표준 출력 (stdout) | 1 | 화면 | 정상 결과 |
| 표준 오류 (stderr) | 2 | 화면 | 오류 메시지 |

Java의 `System.out`과 `System.err`가 바로 표준 출력과 표준 오류입니다. **리다이렉션**은 이 통로를 화면 대신 파일로 바꾸는 기능입니다.

| 기호 | 동작 |
|---|---|
| `>` | 표준 출력을 파일로 저장 (**파일이 있으면 덮어씁니다**) |
| `>>` | 표준 출력을 파일 끝에 추가 |
| `2>` | 표준 오류를 파일로 저장 |
| `2>&1` | 표준 오류를 표준 출력과 같은 곳으로 보냄 |
| `<` | 파일을 표준 입력으로 사용 |

```bash
grep ERROR app.log > errors.txt          # 결과를 파일로 저장
cat errors.txt
grep WARN app.log >> errors.txt          # 기존 내용 뒤에 추가
cat errors.txt
grep ERROR app.log > errors.txt          # >는 덮어쓰므로 WARN 줄은 사라집니다
cat errors.txt
```

**표준 출력과 표준 오류 나누기**

```bash
ls ~/practice /nothing                     # 정상 결과와 오류가 함께 화면에 나옵니다
ls ~/practice /nothing > out.txt           # 오류는 여전히 화면에 나옵니다
ls ~/practice /nothing > out.txt 2> err.txt  # 정상 결과와 오류를 각각 다른 파일로
cat out.txt
cat err.txt
ls ~/practice /nothing > all.txt 2>&1      # 둘 다 한 파일로
cat all.txt
```

> **`2>&1`의 순서:** `> all.txt 2>&1`은 "표준 출력을 파일로 보내고, 표준 오류도 표준 출력이 가는 곳으로 보내라"는 뜻입니다. 순서를 바꾸어 `2>&1 > all.txt`로 쓰면 오류는 화면으로 나갑니다. 4장에서 `nohup java -jar app.jar > app.log 2>&1 &` 같은 명령을 사용할 때 이 원리가 그대로 쓰입니다.

**출력 버리기: `/dev/null`**

`/dev/null`은 들어온 데이터를 모두 버리는 특수 파일입니다. 결과는 필요 없고 성공 여부만 알고 싶을 때 사용합니다.

```bash
ls /nothing 2> /dev/null                   # 오류 메시지를 버립니다
```

### 2.6.4 파이프: 명령을 연결하기

**파이프(`|`)**는 앞 명령의 표준 출력을 뒤 명령의 표준 입력으로 넘깁니다. 작은 명령들을 조립해 원하는 결과를 만드는, 리눅스에서 가장 강력한 기능입니다.

```text
grep ERROR app.log  │  wc -l
  (ERROR 줄 출력)   ─▶  (줄 수 세기)  ─▶  3
```

Java의 Stream API와 비교하면 이해하기 쉽습니다.

```java
lines.stream()
     .filter(line -> line.contains("ERROR"))   // grep ERROR
     .count();                                  // wc -l
```

### 2.6.5 `wc`, `sort`, `uniq`: 세고, 정렬하고, 묶기

**`wc`: 개수 세기**

```bash
wc app.log           # 줄 수, 단어 수, 바이트 수
wc -l app.log        # 줄 수만
grep WARN app.log | wc -l
```

**`sort`: 정렬**

```bash
sort app.log | head -3          # 사전 순서로 정렬
sort -r app.log | head -3       # 역순 (-r)
```

| 옵션 | 의미 |
|---|---|
| `-r` | 역순 |
| `-n` | 숫자 크기로 정렬 (`10`이 `9`보다 뒤에 오도록) |
| `-k 숫자` | 지정한 열을 기준으로 정렬 |

**`uniq`: 중복 묶기**

`uniq`는 **바로 이웃한** 같은 줄만 하나로 묶습니다. 그래서 거의 항상 `sort` 뒤에 사용합니다.

```bash
grep -oE "ERROR|WARN|INFO" app.log | uniq -c          # 정렬하지 않으면 제대로 묶이지 않습니다
grep -oE "ERROR|WARN|INFO" app.log | sort | uniq -c   # 정렬한 뒤 묶기
```

`grep`의 `-o` 옵션은 줄 전체가 아니라 **일치한 부분만** 출력합니다.

```text
      3 ERROR
     17 INFO
      3 WARN
```

**조합하기: 로그 레벨별 개수를 많은 순서로**

```bash
grep -oE "ERROR|WARN|INFO" app.log | sort | uniq -c | sort -rn
```

**조합하기: 가장 많이 호출된 API**

```bash
grep -oE "(GET|POST) /api/[a-z0-9/]+" app.log | sort | uniq -c | sort -rn
```

```text
      4 GET /api/products
      2 POST /api/orders
      2 GET /api/products/12
      1 POST /api/members
      1 GET /api/members/3
```

명령을 한 단계씩 늘려 가며 실행해 보면 각 단계에서 데이터가 어떻게 바뀌는지 이해할 수 있습니다. 파이프 명령이 원하는 대로 동작하지 않을 때도 이렇게 앞에서부터 하나씩 확인합니다.

```bash
grep -oE "(GET|POST) /api/[a-z0-9/]+" app.log
grep -oE "(GET|POST) /api/[a-z0-9/]+" app.log | sort
grep -oE "(GET|POST) /api/[a-z0-9/]+" app.log | sort | uniq -c
grep -oE "(GET|POST) /api/[a-z0-9/]+" app.log | sort | uniq -c | sort -rn
```

> **더 알아보기: `find`로 파일 찾기**
> `grep`이 파일의 **내용**을 찾는다면, `find`는 파일의 **이름과 속성**으로 찾습니다.
> ```bash
> find ~/practice -name "*.log"            # 이름으로 찾기
> find ~/practice -name "*.txt" -mmin -30  # 30분 안에 수정된 .txt 파일
> find /etc -name "*.conf" 2> /dev/null    # 권한 없는 디렉터리의 오류는 버리기
> ```

> **체크포인트 2.6:** 파이프로 명령을 연결해 로그 레벨별 개수를 많은 순서로 출력했습니다.

---

## 2.7 실습: 디렉터리 구조 설계와 로그 분석

이 절은 2장의 내용을 종합하는 실습입니다. 먼저 스스로 해결해 보고, 막히는 부분이 있을 때만 해답을 펼쳐 보십시오.

### 실습 A. 애플리케이션 디렉터리 구조 만들기

2.1.4절에서 설계한 구조를 홈 디렉터리 안에 만들어 봅니다. 3장에서 권한을 배운 뒤 이 구조를 `/opt`로 옮깁니다.

**과제**

1. `~/shop-api` 아래에 `releases`, `config`, `scripts`, `backup`, `logs` 디렉터리를 **명령 한 줄로** 만듭니다.
2. `releases`에 빈 파일 `shop-api-1.0.0.jar`, `shop-api-1.0.1.jar`를 만듭니다.
3. 2.5절에서 만든 `~/practice/application-prod.yml`을 `config`로 **복사**합니다.
4. 2.6절의 `~/practice/app.log`를 `logs`로 **복사**합니다.
5. `shop-api-1.0.0.jar`를 `backup`으로 **이동**합니다.
6. `tree ~/shop-api`로 결과를 확인합니다.

**기대 결과**

```text
/home/devuser/shop-api
├── backup
│   └── shop-api-1.0.0.jar
├── config
│   └── application-prod.yml
├── logs
│   └── app.log
├── releases
│   └── shop-api-1.0.1.jar
└── scripts
```

<details>
<summary>해답</summary>

```bash
mkdir -p ~/shop-api/{releases,config,scripts,backup,logs}
touch ~/shop-api/releases/shop-api-1.0.0.jar ~/shop-api/releases/shop-api-1.0.1.jar
cp ~/practice/application-prod.yml ~/shop-api/config/
cp ~/practice/app.log ~/shop-api/logs/
mv ~/shop-api/releases/shop-api-1.0.0.jar ~/shop-api/backup/
tree ~/shop-api
```

1번의 `{releases,config,...}`는 **중괄호 확장**입니다. 셸이 `~/shop-api/releases ~/shop-api/config ...`처럼 여러 인자로 펼쳐 줍니다. `echo ~/shop-api/{a,b,c}`로 어떻게 펼쳐지는지 확인해 보십시오. 중괄호 안에는 공백을 넣지 않습니다.

</details>

### 실습 B. 로그 분석하기

`~/shop-api/logs/app.log`를 분석해 다음 질문에 답하십시오. 답과 함께 **사용한 명령**을 기록합니다.

| 번호 | 질문 |
|---|---|
| B-1 | 로그 파일은 모두 몇 줄입니까? |
| B-2 | ERROR는 몇 건입니까? 각각 몇 시 몇 분에 발생했습니까? |
| B-3 | INFO가 **아닌** 줄(WARN, ERROR, 스택 트레이스 포함)만 `~/shop-api/logs/issues.log`에 저장하십시오. 몇 줄입니까? |
| B-4 | `NullPointerException`은 어느 클래스, 몇 번째 줄에서 발생했습니까? 이 예외를 일으킨 요청은 무엇입니까? |
| B-5 | 데이터베이스와 관련된 오류(`SqlExceptionHelper`)의 메시지를 모두 출력하십시오. |
| B-6 | 애플리케이션이 시작되는 데 몇 초가 걸렸습니까? 활성화된 프로파일은 무엇입니까? |
| B-7 | `GET /api/products`(상품 목록, 상세 조회 제외)는 몇 번 호출되었습니까? |
| B-8 | 로그 레벨별 개수를 많은 순서로 출력하고, 그 결과를 `~/shop-api/logs/summary.txt`에 저장하십시오. |

<details>
<summary>해답</summary>

**B-1.** 26줄

```bash
wc -l ~/shop-api/logs/app.log
```

**B-2.** 3건, 09:07, 09:16, 09:30

```bash
cd ~/shop-api/logs
grep -c ERROR app.log
grep ERROR app.log
```

시간만 보고 싶다면 각 줄의 12번째부터 16번째 글자를 잘라 냅니다(`cut`은 텍스트의 일부를 잘라 내는 명령입니다).

```bash
grep ERROR app.log | cut -c 12-16
```

**B-3.** 9줄 (WARN 3줄, ERROR 3줄, 예외와 스택 트레이스 3줄)

```bash
grep -v " INFO " app.log > issues.log
wc -l issues.log
```

`INFO` 앞뒤에 공백을 넣은 이유는, 메시지 안에 `INFO`라는 글자가 들어 있는 다른 레벨의 줄을 잘못 제외하지 않기 위해서입니다. 검색어를 정확하게 지정하는 습관을 들이십시오.

**B-4.** `OrderService.java`의 42번째 줄(`createOrder` 메서드), 요청은 `POST /api/orders`

```bash
grep -A 2 NullPointerException app.log
grep -B 4 NullPointerException app.log
```

예외 바로 위 줄은 `dispatcherServlet`의 오류 메시지이고, 그보다 위에서 같은 스레드(`nio-8080-exec-4`)가 처리한 요청을 찾을 수 있습니다. 스레드 이름으로 검색하면 더 정확합니다.

```bash
grep "exec-4\]" app.log
```

결과의 마지막 줄(09:31, `POST /api/members`)은 같은 스레드가 **나중에** 처리한 다른 요청입니다. 톰캣은 스레드를 재사용하므로, 스레드 이름으로 찾을 때는 시간도 함께 확인해야 합니다.

**B-5.**

```bash
grep SqlExceptionHelper app.log
```

`Duplicate entry 'ORD-20261001-0007' for key 'orders.order_no'`(주문 번호 중복)와 `Communications link failure`(DB 연결 끊김) 두 건입니다.

**B-6.** 6.112초, 프로파일은 `prod`

```bash
grep "Started" app.log
grep "profile" app.log
```

**B-7.** 4번

```bash
grep -c "GET /api/products$" app.log
```

`$`는 정규식에서 **줄의 끝**을 뜻합니다. `$` 없이 검색하면 `GET /api/products/12`도 함께 세어져 6번이 나옵니다.

**B-8.**

```bash
grep -oE "ERROR|WARN|INFO" app.log | sort | uniq -c | sort -rn > summary.txt
cat summary.txt
```

```text
     17 INFO
      3 WARN
      3 ERROR
```

</details>

> **체크포인트 2.7:** `tree ~/shop-api` 결과가 기대 결과와 같고, 실습 B의 질문 8개에 명령과 함께 답했습니다.

### 실습이 끝나면

VM을 종료하고 스냅샷을 만듭니다.

```bash
sudo poweroff
```

VirtualBox에서 스냅샷 이름을 `02-기본-명령어-완료`로 만듭니다.

---

## 2장 정리

| 절 | 핵심 내용 |
|---|---|
| 2.1 | 리눅스는 `/` 하나에서 시작하며 대소문자를 구분합니다. 직접 설치한 애플리케이션은 `/opt`, 설정은 `/etc`, 로그는 `/var/log`에 둡니다 |
| 2.2 | 절대 경로는 `/`부터, 상대 경로는 현재 위치부터 시작합니다. 스크립트와 설정 파일에는 절대 경로를 씁니다. 리눅스에는 휴지통이 없습니다 |
| 2.3 | 짧은 파일은 `cat`, 긴 파일은 `less`, 끝부분은 `tail`, 실시간 로그는 `tail -f`로 봅니다 |
| 2.4 | `--help`로 요약을, `man`으로 자세한 사용법을 찾습니다 |
| 2.5 | vim은 일반·입력·명령 모드로 나뉩니다. 길을 잃으면 `Esc` 후 `:q!` 입니다. YAML 편집 시 탭 문자를 조심합니다 |
| 2.6 | `grep`으로 고르고, `>`·`>>`·`2>&1`로 저장하고, `\|`로 연결하고, `sort \| uniq -c`로 집계합니다 |

## 2장에서 사용한 명령어

**이동과 파일 관리**

| 명령어 | 용도 |
|---|---|
| `pwd` | 현재 위치 출력 |
| `cd 경로` / `cd` / `cd -` | 이동 / 홈으로 / 이전 위치로 |
| `ls -lah` | 숨김 파일을 포함해 자세히, 읽기 쉬운 크기로 |
| `ls -ltr` | 수정 시간 순서로 (최근 것이 아래) |
| `mkdir -p 경로` | 중간 디렉터리까지 한 번에 만들기 |
| `touch 파일` | 빈 파일 만들기 |
| `cp -r 원본 대상` | 복사 (디렉터리는 `-r`) |
| `mv 원본 대상` | 이동 또는 이름 바꾸기 |
| `rm -r 대상` / `rmdir 디렉터리` | 삭제 / 빈 디렉터리 삭제 |
| `tree 경로` | 디렉터리 구조를 나무 모양으로 출력 |
| `history` | 실행한 명령어 목록 |

**내용 확인과 도움말**

| 명령어 | 용도 |
|---|---|
| `cat -n 파일` | 줄 번호와 함께 전체 출력 |
| `less 파일` | 넘겨 가며 보기, `/`로 검색, `q`로 종료 |
| `head -n 숫자 파일` | 앞부분 보기 |
| `tail -n 숫자 파일` | 끝부분 보기 |
| `tail -F 파일` | 실시간으로 지켜보기 |
| `명령어 --help` | 사용법 요약 |
| `man 명령어` | 자세한 매뉴얼 |
| `type 명령어` / `which 명령어` | 명령어의 종류 / 실행 파일 위치 |

**vim**

| 명령 | 용도 |
|---|---|
| `i`, `a`, `o` | 입력 모드로 |
| `Esc` | 일반 모드로 |
| `:w`, `:q`, `:wq`, `:q!` | 저장, 종료, 저장 후 종료, 강제 종료 |
| `dd`, `yy`, `p`, `u` | 줄 삭제, 줄 복사, 붙여넣기, 되돌리기 |
| `/단어`, `:%s/old/new/g` | 검색, 전체 바꾸기 |

**텍스트 처리**

| 명령어 | 용도 |
|---|---|
| `grep -n -i -v -c -E 패턴 파일` | 패턴이 들어 있는 줄 찾기 |
| `grep -A/-B/-C 숫자` | 일치한 줄의 앞뒤 함께 보기 |
| `grep -o` | 일치한 부분만 출력 |
| `wc -l` | 줄 수 세기 |
| `sort -rn` | 숫자 기준 역순 정렬 |
| `uniq -c` | 이웃한 중복 줄을 묶고 개수 표시 |
| `>`, `>>`, `2>`, `2>&1` | 출력을 파일로 저장, 추가, 오류 저장, 오류를 출력과 합치기 |
| `\|` | 앞 명령의 출력을 뒤 명령의 입력으로 |

## 2장 체크포인트 제출

다음 두 가지를 제출합니다.

1. `tree ~/shop-api` 실행 화면
2. 실습 B의 질문별 답과 사용한 명령을 정리한 텍스트 (`cat ~/shop-api/logs/summary.txt` 화면 포함)

## 확인 문제

**1.** Spring Boot 애플리케이션의 로그 파일을 두기에 가장 알맞은 위치는 어디입니까?

1. `/etc/shop-api`
2. `/var/log/shop-api`
3. `/home/devuser`
4. `/tmp`

**2.** 현재 위치가 `/var/log`일 때, `cd ../lib/mysql`을 실행하면 어디로 이동합니까?

1. `/var/log/lib/mysql`
2. `/lib/mysql`
3. `/var/lib/mysql`
4. `/usr/lib/mysql`

**3.** 운영 중인 서버에서 로그 파일에 새로 추가되는 내용을 실시간으로 지켜보는 명령은 무엇입니까?

1. `cat app.log`
2. `head -f app.log`
3. `less app.log`
4. `tail -F app.log`

**4.** vim에서 내용을 수정하다가 저장하지 않고 빠져나오려고 합니다. 올바른 순서는 무엇입니까?

1. `Esc` → `:wq`
2. `Esc` → `:q!`
3. `Ctrl + C` → `:w`
4. `i` → `:q`

**5.** 다음 명령의 결과로 알맞은 것은 무엇입니까?

```bash
grep -oE "ERROR|WARN|INFO" app.log | sort | uniq -c | sort -rn
```

1. 로그 레벨별 줄을 시간 순서로 출력합니다
2. 로그 레벨별 개수를 많은 순서로 출력합니다
3. ERROR, WARN, INFO가 아닌 줄만 출력합니다
4. 로그 파일을 레벨별로 나누어 저장합니다

<details>
<summary>정답과 해설</summary>

1. **2번.** 계속 커지는 데이터인 로그는 `/var/log` 아래에 둡니다. `/etc`는 설정 파일, `/tmp`는 재부팅 시 정리될 수 있는 임시 파일의 위치입니다.
2. **3번.** `..`은 상위 디렉터리인 `/var`이므로, `/var` 아래의 `lib/mysql`로 이동합니다.
3. **4번.** `tail -F`(또는 `-f`)는 파일 끝을 계속 지켜봅니다. `-F`는 로그 파일이 교체되어도 새 파일을 따라가므로 운영 서버에 더 알맞습니다.
4. **2번.** `Esc`로 일반 모드로 돌아온 뒤 `:q!`로 저장하지 않고 강제 종료합니다. `:wq`는 저장하고 종료합니다.
5. **2번.** `grep -o`로 레벨 이름만 뽑고, `sort`로 같은 것끼리 모은 뒤, `uniq -c`로 개수를 세고, `sort -rn`으로 개수가 많은 순서로 정렬합니다.

</details>

## 다음 장 미리 보기

2장의 실습 A에서는 애플리케이션 구조를 홈 디렉터리에 만들었습니다. 실제 운영에서는 이 구조를 `/opt/shop-api`에 두고, 사람 계정이 아닌 **애플리케이션 전용 계정**으로 실행합니다. 3장에서는 사용자와 권한, 패키지 관리, Java 설치, 환경 변수, 프로세스 관리를 배우며 Spring Boot 애플리케이션을 실행할 준비를 마쳐 보겠습니다.
