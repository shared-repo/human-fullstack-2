# 3. 사용자·권한·패키지·프로세스

> **과정:** 스프링 개발자를 위한 리눅스 기초: 내 애플리케이션을 서버에 배포하기
> **분량:** 8시간
> **실습 환경:** `rocky9-server` VM (스냅샷 `02-기본-명령어-완료`), Windows 터미널에서 `ssh spring`으로 접속

## 이 장의 학습 목표

이 장을 마치면 다음을 할 수 있습니다.

- 사용자와 그룹을 만들고, 애플리케이션 전용 계정이 필요한 이유를 설명할 수 있습니다.
- 파일과 디렉터리의 권한을 읽고, `chmod`와 `chown`으로 필요한 만큼만 권한을 줄 수 있습니다.
- `sudo`로 관리자 작업을 안전하게 수행할 수 있습니다.
- 심볼릭 링크로 실행할 jar 버전을 바꾸고, `tar`로 파일을 묶어 백업할 수 있습니다.
- `dnf`로 패키지를 찾고 설치·삭제할 수 있습니다.
- Java를 설치하고, 여러 버전을 전환하며, `JAVA_HOME`과 Spring 프로파일을 환경 변수로 지정할 수 있습니다.
- 실행 중인 Java 프로세스를 찾고, 백그라운드로 실행하고, 안전하게 종료할 수 있습니다.
- "Permission denied" 오류의 원인을 찾아 해결할 수 있습니다.

## 이 장에서 완성할 구조

2장에서 홈 디렉터리에 만든 `~/shop-api`를 이 장의 마지막(3.9절)에 `/opt/shop-api`로 옮기고, 애플리케이션 전용 계정 `appuser`로 실행합니다.

```text
/opt/shop-api/                 devuser:appuser  750
├── current.jar → releases/shop-api-1.0.1.jar
├── releases/                  devuser:appuser  750
│   ├── shop-api-1.0.0.jar     devuser:appuser  640
│   └── shop-api-1.0.1.jar     devuser:appuser  640
├── config/                    devuser:appuser  750
│   └── application-prod.yml   devuser:appuser  640
├── scripts/                   devuser:appuser  750
└── backup/                    devuser:devuser  700

/var/log/shop-api/             appuser:appuser  750
└── app.log                    appuser:appuser  644
```

지금은 이 표의 숫자와 이름이 무슨 뜻인지 몰라도 괜찮습니다. 이 장을 마치면 각 항목을 왜 이렇게 정했는지 설명할 수 있게 됩니다.

---

## 3.1 사용자와 그룹

### 3.1.1 리눅스는 여러 사람이 함께 쓰는 운영체제입니다

리눅스는 처음부터 여러 사용자가 한 컴퓨터를 동시에 쓰도록 만들어졌습니다. 그래서 모든 파일과 프로세스에는 **주인(소유자)**이 있고, 리눅스는 "누가 무엇을 할 수 있는가"를 항상 확인합니다.

| 개념 | 설명 |
|---|---|
| 사용자 (user) | 시스템을 사용하는 주체. 사람일 수도 있고 프로그램일 수도 있습니다 |
| 그룹 (group) | 사용자의 묶음. 여러 사용자에게 같은 권한을 한 번에 줄 때 사용합니다 |
| UID / GID | 사용자 번호 / 그룹 번호. 리눅스는 내부적으로 이름이 아니라 번호로 구분합니다 |

```bash
whoami           # 내 사용자 이름
id               # 내 UID, GID, 속한 그룹 목록
id root          # 다른 사용자의 정보
who              # 지금 접속해 있는 사용자
```

```text
uid=1000(devuser) gid=1000(devuser) groups=1000(devuser),10(wheel)
```

`devuser`가 `wheel` 그룹에 들어 있는 것은 1장의 설치 과정에서 **Make this user administrator**를 체크했기 때문입니다.

### 3.1.2 사용자 정보는 어디에 저장되는가

2장에서 잠시 보았던 `/etc/passwd`가 사용자 목록입니다.

```bash
tail -n 3 /etc/passwd
```

```text
devuser:x:1000:1000:Dev User:/home/devuser:/bin/bash
```

| 항목 | 값 | 의미 |
|---|---|---|
| 1 | `devuser` | 사용자 이름 |
| 2 | `x` | 비밀번호 자리. 실제 비밀번호는 `/etc/shadow`에 암호화되어 저장됩니다 |
| 3 | `1000` | UID |
| 4 | `1000` | 기본 그룹의 GID |
| 5 | `Dev User` | 설명 |
| 6 | `/home/devuser` | 홈 디렉터리 |
| 7 | `/bin/bash` | 로그인할 때 실행되는 셸 |

| 파일 | 내용 | 일반 사용자가 읽을 수 있는가 |
|---|---|---|
| `/etc/passwd` | 사용자 목록 | 예 |
| `/etc/shadow` | 암호화된 비밀번호 | 아니요 (`sudo` 필요) |
| `/etc/group` | 그룹 목록과 구성원 | 예 |

```bash
cat /etc/shadow          # Permission denied
grep wheel /etc/group    # wheel 그룹의 구성원
```

**UID 범위**

| UID | 용도 |
|---|---|
| 0 | root |
| 1 ~ 999 | 시스템 계정 (서비스 프로그램용, 사람이 로그인하지 않음) |
| 1000 이상 | 일반 사용자 (사람) |

```bash
grep -E "nginx|mysql|sshd" /etc/passwd    # sshd 같은 시스템 계정을 찾아봅니다
```

`sshd` 계정의 마지막 항목이 `/sbin/nologin`인 것을 확인하십시오. 이 계정으로는 아무도 로그인할 수 없다는 뜻입니다.

### 3.1.3 사용자와 그룹 관리 명령

| 명령 | 용도 |
|---|---|
| `sudo useradd 이름` | 사용자 만들기 |
| `sudo passwd 이름` | 비밀번호 지정 |
| `sudo usermod -aG 그룹 이름` | 사용자를 그룹에 **추가** |
| `sudo userdel -r 이름` | 사용자 삭제 (`-r`: 홈 디렉터리도 삭제) |
| `sudo groupadd 이름` | 그룹 만들기 |
| `groups 이름` | 사용자가 속한 그룹 확인 |

**실습: 사용자를 만들고 삭제하기**

```bash
sudo useradd tester
sudo passwd tester           # 비밀번호를 두 번 입력합니다
id tester
ls -ld /home/tester          # 홈 디렉터리가 자동으로 만들어졌습니다
grep tester /etc/group       # 같은 이름의 그룹도 자동으로 만들어졌습니다
```

Rocky Linux는 사용자를 만들면 **같은 이름의 그룹**을 함께 만들어 그 사용자의 기본 그룹으로 지정합니다.

다른 사용자로 전환해 봅니다.

```bash
su - tester                  # tester로 전환 (tester의 비밀번호 입력)
whoami
pwd                          # /home/tester
exit                         # 원래 사용자로 돌아오기
```

`su -`의 `-`는 그 사용자로 **새로 로그인한 것처럼** 환경(홈 디렉터리, 환경 변수)을 바꾸라는 뜻입니다.

실습이 끝나면 삭제합니다.

```bash
sudo userdel -r tester
id tester                    # no such user
```

> **`usermod -aG`의 `-a`를 빠뜨리지 마십시오.** `-a`(append) 없이 `usermod -G appuser devuser`를 실행하면, `devuser`는 `appuser` 그룹에만 속하게 되고 **기존의 `wheel` 그룹에서 빠집니다.** 그러면 `sudo`를 더 이상 쓸 수 없게 됩니다. 이런 실수를 했다면 root로 로그인해 바로잡거나 스냅샷으로 되돌려야 합니다.

### 3.1.4 애플리케이션 전용 계정이 필요한 이유

Spring Boot 애플리케이션을 `devuser`나 `root`로 실행하면 어떤 일이 생길까요?

| 실행 계정 | 문제 |
|---|---|
| `root` | 애플리케이션에 보안 취약점이 있으면, 공격자가 **서버 전체**를 장악할 수 있습니다 |
| `devuser` (사람 계정) | 공격자가 `devuser`의 SSH 키, 다른 프로젝트 파일, `sudo` 권한까지 노릴 수 있습니다. 담당자가 바뀌어 계정을 지우면 애플리케이션도 멈춥니다 |
| `appuser` (전용 계정) | 애플리케이션에 꼭 필요한 파일에만 접근할 수 있습니다. 문제가 생겨도 피해가 그 범위 안에 머뭅니다 |

이것을 **최소 권한 원칙(Principle of Least Privilege)**이라고 합니다. Nginx는 `nginx` 계정, MySQL은 `mysql` 계정으로 실행되는 것도 같은 이유입니다. 4장에서 설치하면 직접 확인할 수 있습니다.

애플리케이션 전용 계정은 다음과 같이 만듭니다. (실제 생성은 3.9절에서 합니다.)

```bash
sudo useradd --system --no-create-home --shell /sbin/nologin appuser
```

| 옵션 | 의미 |
|---|---|
| `--system` | 시스템 계정으로 만듭니다 (UID 1000 미만) |
| `--no-create-home` | 홈 디렉터리를 만들지 않습니다 |
| `--shell /sbin/nologin` | 이 계정으로는 로그인할 수 없습니다 |

> **체크포인트 3.1:** `/etc/passwd`의 한 줄을 보고 각 항목의 의미를 설명할 수 있습니다. 애플리케이션을 전용 계정으로 실행해야 하는 이유를 말할 수 있습니다.

---

## 3.2 권한 체계

### 3.2.1 권한 읽기

2장에서 `ls -l`의 첫 열을 "권한"이라고만 설명했습니다. 이제 자세히 읽어 봅시다.

```text
-rw-r-----. 1 devuser appuser 4939 Oct  2 10:00 shop-api-1.0.0.jar
│└┬┘└┬┘└┬┘     └──┬──┘ └──┬──┘
│ │  │  │         │       └ 그룹
│ │  │  │         └ 소유자
│ │  │  └ 기타 사용자(other)의 권한: ---  (아무것도 못 함)
│ │  └ 그룹(group)의 권한:          r--  (읽기만)
│ └ 소유자(user)의 권한:            rw-  (읽기, 쓰기)
└ 종류: - 파일, d 디렉터리, l 링크
```

리눅스는 접근하는 사람이 누구인지 다음 순서로 확인하고, **처음 해당하는 한 가지**만 적용합니다.

1. 소유자인가? → 소유자 권한 적용
2. 아니라면, 그룹의 구성원인가? → 그룹 권한 적용
3. 둘 다 아니라면 → 기타 사용자 권한 적용

(root는 이 검사를 거치지 않고 모든 파일에 접근할 수 있습니다.)

### 3.2.2 파일과 디렉터리에서 r, w, x의 의미

같은 글자라도 파일과 디렉터리에서 뜻이 다릅니다. **이 차이를 모르면 "권한을 줬는데도 Permission denied가 나는" 상황을 해결할 수 없습니다.**

| 권한 | 파일에서 | 디렉터리에서 |
|---|---|---|
| `r` (read) | 내용 읽기 (`cat`, `java -jar`) | 안의 파일 **이름 목록** 보기 (`ls`) |
| `w` (write) | 내용 수정 | 안에서 파일 **만들기, 지우기, 이름 바꾸기** |
| `x` (execute) | 프로그램으로 실행 | 안으로 **들어가기**, 안의 파일에 접근하기 (`cd`) |

꼭 기억해야 할 두 가지가 있습니다.

1. **파일을 지울 수 있는지는 파일이 아니라 디렉터리의 `w` 권한이 결정합니다.** 읽기 전용 파일이라도 디렉터리에 `w`가 있으면 지울 수 있습니다.
2. **파일에 접근하려면 경로에 있는 모든 디렉터리에 `x` 권한이 있어야 합니다.** `/opt/shop-api/releases/app.jar`를 읽으려면 `/`, `/opt`, `/opt/shop-api`, `/opt/shop-api/releases` 모두에 `x`가 필요합니다.

> **jar 파일에는 `x` 권한이 필요 없습니다.** `java -jar app.jar`에서 실행되는 프로그램은 `java`이고, `java`는 jar 파일을 **읽기만** 합니다. 그래서 jar 파일에는 `r`만 있으면 됩니다. `x`가 필요한 것은 5장에서 만들 셸 스크립트처럼 파일 자체를 실행할 때입니다.

### 3.2.3 숫자로 권한 표현하기

각 권한에 숫자를 붙여 더한 값으로 표현합니다.

| 권한 | 숫자 |
|---|---|
| `r` | 4 |
| `w` | 2 |
| `x` | 1 |
| `-` | 0 |

```text
rwx = 4+2+1 = 7
rw- = 4+2+0 = 6
r-x = 4+0+1 = 5
r-- = 4+0+0 = 4
--- = 0
```

소유자, 그룹, 기타 순서로 세 자리를 씁니다.

| 숫자 | 권한 | 자주 쓰는 곳 |
|---|---|---|
| `755` | `rwxr-xr-x` | 누구나 들어가고 읽을 수 있는 디렉터리, 실행 파일 |
| `750` | `rwxr-x---` | 소유자와 그룹만 들어갈 수 있는 디렉터리 |
| `700` | `rwx------` | 소유자만 사용하는 디렉터리 (`~/.ssh`) |
| `644` | `rw-r--r--` | 누구나 읽을 수 있는 일반 파일 |
| `640` | `rw-r-----` | 소유자와 그룹만 읽을 수 있는 파일 (설정 파일) |
| `600` | `rw-------` | 소유자만 읽고 쓰는 파일 (`authorized_keys`, 개인 키) |

### 3.2.4 `chmod`: 권한 바꾸기

**숫자 방식**: 세 자리 전체를 한 번에 지정합니다.

```bash
cd ~/practice
touch secret.txt
ls -l secret.txt
chmod 600 secret.txt
ls -l secret.txt         # -rw-------
```

**기호 방식**: 일부만 더하거나 뺍니다.

| 대상 | 연산 | 권한 |
|---|---|---|
| `u` 소유자, `g` 그룹, `o` 기타, `a` 모두 | `+` 추가, `-` 제거, `=` 지정 | `r`, `w`, `x` |

```bash
chmod g+r secret.txt     # 그룹에 읽기 추가 → rw-r-----
chmod o-r secret.txt     # 기타의 읽기 제거
chmod u=rw,g=r,o= secret.txt
mkdir private
chmod -R 750 private     # -R: 디렉터리 안의 모든 것에 적용
```

**실습: 디렉터리의 `x` 권한 확인하기**

```bash
mkdir ~/practice/box
touch ~/practice/box/item.txt
chmod 600 ~/practice/box        # rw------- : 디렉터리에서 x를 뺐습니다
ls ~/practice/box               # 이름은 보이지만 오류도 함께 나옵니다
cat ~/practice/box/item.txt     # Permission denied
cd ~/practice/box               # Permission denied
chmod 700 ~/practice/box        # x를 다시 주면
cd ~/practice/box && pwd        # 들어갈 수 있습니다
cd ~
```

> **`chmod 777`은 해결책이 아닙니다.** 인터넷에서 "Permission denied가 나면 `chmod -R 777`을 하라"는 답을 자주 볼 수 있습니다. 이것은 **서버의 모든 사용자에게 그 파일을 수정하고 지울 권한을 주는 것**입니다. 운영 서버에서 설정 파일이나 jar가 777이면, 웹 애플리케이션의 작은 취약점 하나로 누군가 jar를 바꿔치기할 수 있습니다. 권한 문제는 항상 "**누가** 접근하는데 **무엇이** 막혔는가"를 찾아 그 부분만 열어 주어야 합니다. 3.9절에서 그 방법을 연습합니다.

### 3.2.5 `chown`, `chgrp`: 소유자 바꾸기

소유자를 바꾸는 것은 관리자만 할 수 있으므로 `sudo`가 필요합니다.

```bash
sudo chown root secret.txt            # 소유자만 변경
sudo chown root:wheel secret.txt      # 소유자와 그룹을 함께 변경
sudo chown :devuser secret.txt        # 그룹만 변경
sudo chgrp devuser secret.txt         # 그룹만 변경 (위와 같습니다)
sudo chown -R devuser:devuser private # 디렉터리 안의 모든 것을 함께 변경
ls -l secret.txt
```

### 3.2.6 `umask`: 새 파일의 기본 권한

새로 만든 파일이 어떤 권한을 가질지는 `umask`가 결정합니다. `umask`는 기본값에서 **빼는** 권한입니다.

```bash
umask                    # 0002 또는 0022 (환경에 따라 다릅니다)
touch newfile && ls -l newfile
mkdir newdir && ls -ld newdir
```

| umask | 새 파일 (기본 666에서 뺌) | 새 디렉터리 (기본 777에서 뺌) |
|---|---|---|
| `0022` | `644` (`rw-r--r--`) | `755` (`rwxr-xr-x`) |
| `0002` | `664` (`rw-rw-r--`) | `775` (`rwxrwxr-x`) |
| `0027` | `640` (`rw-r-----`) | `750` (`rwxr-x---`) |

`umask`는 3.9절에서 jar를 복사한 뒤 권한을 다시 지정해야 하는 이유와 연결됩니다.

> **참고: 특수 권한.** `ls -l`에서 `rwx` 대신 `s`나 `t`가 보이는 경우가 있습니다. `passwd` 명령(`-rwsr-xr-x`)처럼 실행하는 동안 소유자의 권한을 빌리는 **setuid**, 디렉터리 안에 새로 만든 파일이 디렉터리의 그룹을 물려받게 하는 **setgid**, `/tmp`(`drwxrwxrwt`)처럼 누구나 쓸 수 있지만 자기 파일만 지울 수 있게 하는 **sticky bit**입니다. 이 과정에서는 직접 설정하지 않지만, 보았을 때 의미를 알 수 있도록 기억해 두십시오.

> **체크포인트 3.2:** `rw-r-----`를 숫자로, `750`을 기호로 바꿀 수 있습니다. 디렉터리에서 `x` 권한을 빼면 어떤 일이 생기는지 직접 확인했습니다.

---

## 3.3 `sudo`와 root 권한

### 3.3.1 왜 root로 직접 작업하지 않는가

| 방식 | 문제 또는 장점 |
|---|---|
| root로 로그인해 작업 | 모든 명령이 관리자 권한으로 실행되므로 오타 하나로 시스템을 망가뜨릴 수 있습니다. 여러 사람이 root 비밀번호를 공유하면 누가 무엇을 했는지 알 수 없습니다 |
| 일반 사용자 + `sudo` | 관리자 권한이 필요한 명령에만 `sudo`를 붙여 **의식적으로** 실행합니다. 누가 어떤 명령을 실행했는지 기록이 남습니다 |

### 3.3.2 `sudo` 사용법

```bash
sudo dnf update                 # 명령 하나를 root 권한으로
sudo -l                         # 내가 sudo로 무엇을 할 수 있는지 확인
sudo -u appuser 명령            # root가 아닌 다른 사용자로 실행 (3.9절에서 사용)
sudo -i                         # root 셸로 전환 (꼭 필요할 때만, exit로 빠져나오기)
```

처음 `sudo`를 사용하면 **내 비밀번호**(root 비밀번호가 아님)를 묻습니다. 한 번 입력하면 5분 동안은 다시 묻지 않습니다.

### 3.3.3 누가 `sudo`를 쓸 수 있는가

`sudo` 권한은 `/etc/sudoers` 파일에 정의되어 있습니다.

```bash
sudo grep -E "^%wheel|^root" /etc/sudoers
```

```text
root    ALL=(ALL)       ALL
%wheel  ALL=(ALL)       ALL
```

| 부분 | 의미 |
|---|---|
| `%wheel` | `wheel` 그룹의 구성원은 (`%`는 그룹을 뜻합니다) |
| `ALL=` | 모든 호스트에서 |
| `(ALL)` | 어떤 사용자로든 |
| `ALL` | 모든 명령을 실행할 수 있습니다 |

그래서 사용자에게 `sudo` 권한을 주려면 `wheel` 그룹에 넣으면 됩니다.

```bash
sudo usermod -aG wheel 사용자이름
```

> **`/etc/sudoers`는 반드시 `visudo`로 수정하십시오.** vim으로 직접 열어 수정하다 문법 오류가 생기면 `sudo`가 아예 동작하지 않아, 그 실수를 고칠 방법도 사라집니다. `sudo visudo`는 저장하기 전에 문법을 검사해 줍니다. 특정 사용자에게 일부 명령만 허용하는 설정은 `/etc/sudoers.d/` 디렉터리에 별도 파일로 만드는 것이 관례입니다(`sudo visudo -f /etc/sudoers.d/파일이름`).

### 3.3.4 `sudo` 기록 확인

```bash
sudo journalctl _COMM=sudo | tail -n 5
```

```text
Oct 02 10:12:31 spring-server sudo[1532]: devuser : TTY=pts/0 ; PWD=/home/devuser ; USER=root ; COMMAND=/usr/bin/dnf update
```

누가(`devuser`), 어디서(`PWD`), 누구 권한으로(`USER=root`), 무엇을(`COMMAND`) 실행했는지 남아 있습니다. `journalctl`은 4.6절에서 자세히 배웁니다.

> **체크포인트 3.3:** `sudo -l`로 내 권한을 확인했고, `sudo` 실행 기록을 찾아보았습니다.

---

## 3.4 링크와 압축

### 3.4.1 심볼릭 링크

**심볼릭 링크(symbolic link)**는 다른 파일이나 디렉터리를 가리키는 특수 파일입니다. Windows의 바로 가기와 비슷합니다.

```bash
ln -s 원본경로 링크이름
```

```bash
cd ~/practice
echo "version 1" > v1.txt
echo "version 2" > v2.txt
ln -s v1.txt current.txt
ls -l current.txt            # current.txt -> v1.txt
cat current.txt              # version 1
```

링크가 가리키는 대상을 바꿉니다.

```bash
ln -sfn v2.txt current.txt   # -f: 기존 링크를 덮어씀, -n: 링크가 디렉터리를 가리켜도 링크 자체를 교체
cat current.txt              # version 2
readlink -f current.txt      # 링크가 최종적으로 가리키는 실제 경로
```

**배포에 링크를 쓰는 이유**

```text
/opt/shop-api/current.jar → releases/shop-api-1.0.1.jar
```

- 서비스 설정(4.5절)에는 항상 `current.jar`만 적어 둡니다. 새 버전을 배포할 때 설정 파일을 고칠 필요가 없습니다.
- 새 버전에 문제가 생기면 링크만 이전 버전으로 바꾸고 재시작하면 **몇 초 만에 되돌릴(롤백)** 수 있습니다.

원본을 지우면 링크는 **깨진 링크**가 됩니다.

```bash
rm v1.txt
ln -s v1.txt broken.txt
ls -l broken.txt             # 대상이 없는 링크 (터미널에서 빨간색으로 표시되기도 합니다)
cat broken.txt               # No such file or directory
rm broken.txt                # 링크 자체를 지웁니다 (원본에는 영향 없음)
```

> **참고:** 2장에서 Rocky Linux의 `/bin`이 `/usr/bin`을 가리킨다고 했습니다. `ls -l /bin`으로 확인해 보십시오.

### 3.4.2 `tar`: 파일 묶기와 압축

`tar`는 여러 파일과 디렉터리를 **하나의 파일로 묶습니다.** `z` 옵션을 함께 쓰면 gzip으로 압축합니다. 리눅스에서 백업과 배포에 가장 많이 쓰는 형식입니다.

| 옵션 | 의미 |
|---|---|
| `c` | 만들기 (create) |
| `x` | 풀기 (extract) |
| `t` | 내용 목록 보기 (list) |
| `z` | gzip 압축 사용 |
| `v` | 처리 중인 파일 이름 출력 (verbose) |
| `f 파일` | 묶은 파일의 이름 (항상 마지막 옵션으로 씁니다) |
| `-C 디렉터리` | 지정한 디렉터리로 이동한 뒤 처리 |

```bash
cd ~
tar czvf practice-backup.tar.gz practice      # practice 디렉터리를 묶고 압축
ls -lh practice-backup.tar.gz
tar tzf practice-backup.tar.gz                # 풀지 않고 내용만 확인
mkdir restore
tar xzvf practice-backup.tar.gz -C restore    # restore 디렉터리에 풀기
ls restore/practice
rm -r restore
```

> **외우는 방법:** 만들 때는 **c**reate **z**ip **f**ile (`czf`), 풀 때는 e**x**tract **z**ip **f**ile (`xzf`), 볼 때는 lis**t** (`tzf`)입니다.

### 3.4.3 `zip`, `unzip`

Windows 사용자와 파일을 주고받을 때는 zip 형식이 편리합니다. 최소 설치에는 들어 있지 않으므로 설치합니다.

```bash
sudo dnf install -y zip unzip
zip -r practice.zip practice
unzip -l practice.zip            # 내용 목록
```

> **jar 파일의 정체:** jar 파일은 사실 **zip 형식**입니다. 3.6절에서 jar를 만든 뒤 `unzip -l`로 안을 들여다볼 것입니다. 실제 Spring Boot jar를 열어 보면 `BOOT-INF/classes`(우리가 작성한 코드)와 `BOOT-INF/lib`(의존성 라이브러리)가 들어 있습니다.

> **체크포인트 3.4:** 심볼릭 링크의 대상을 바꾸어 보았고, `tar`로 디렉터리를 묶고 풀어 보았습니다.

---

## 3.5 패키지 관리

### 3.5.1 패키지와 저장소

**패키지**는 프로그램과 설정 파일, 설치 정보를 하나로 묶은 파일입니다. RHEL 계열은 `.rpm` 형식을 사용합니다. **저장소(repository)**는 패키지를 모아 둔 인터넷 서버입니다.

`dnf`는 저장소에서 패키지를 찾아, 필요한 **의존 패키지까지 함께** 내려받아 설치합니다. Maven이나 Gradle이 `pom.xml`, `build.gradle`에 적은 라이브러리와 그 의존성을 Maven 저장소에서 내려받는 것과 같은 원리입니다.

| Spring 개발 | 리눅스 |
|---|---|
| Maven Central | Rocky Linux 저장소 |
| `pom.xml`의 dependency | 설치할 패키지 |
| `mvn install` | `dnf install` |
| 전이 의존성 자동 해결 | 의존 패키지 자동 설치 |

```bash
dnf repolist
```

| 저장소 | 내용 |
|---|---|
| `baseos` | 운영체제의 핵심 패키지 |
| `appstream` | 애플리케이션과 개발 도구 (Java, Nginx, MySQL 등) |
| `extras` | 추가 저장소 설정 등 |

> **참고:** Rocky Linux에서는 Fedora 프로젝트가 관리하는 추가 저장소 **EPEL**(`sudo dnf install epel-release`)을 사용할 수 있습니다. `htop` 같은 편리한 도구가 EPEL에 있습니다. 반면 AWS의 Amazon Linux 2023은 EPEL을 지원하지 않습니다. 5.7절의 EC2 시연에서 이 차이를 다시 다룹니다.

### 3.5.2 `dnf` 명령

| 명령 | 용도 |
|---|---|
| `dnf search 단어` | 이름과 설명으로 패키지 찾기 |
| `dnf info 패키지` | 패키지 정보 (버전, 크기, 설명) |
| `sudo dnf install 패키지` | 설치 |
| `sudo dnf remove 패키지` | 삭제 |
| `sudo dnf update` | 설치된 패키지 모두 업데이트 |
| `dnf check-update` | 업데이트할 수 있는 패키지 목록만 확인 |
| `dnf list installed` | 설치된 패키지 목록 |
| `dnf provides 경로` | 어떤 패키지가 그 파일을 제공하는지 찾기 |
| `dnf history` | 설치·삭제 기록 |

**실습: 패키지 찾아 설치하고 지우기**

```bash
dnf search wget
dnf info wget
sudo dnf install -y wget
which wget
sudo dnf remove -y wget
which wget                       # 없어졌습니다
dnf history | head
```

**실습: 명령어가 없을 때 패키지 찾기**

`nslookup`이라는 명령을 실행해 봅니다.

```bash
nslookup rockylinux.org          # command not found
dnf provides "*/bin/nslookup"    # bind-utils 패키지에 들어 있다는 것을 알 수 있습니다
```

이처럼 다른 문서에 나온 명령어가 없을 때는 `dnf provides`로 어떤 패키지를 설치해야 하는지 찾습니다. (설치는 필요할 때 하면 됩니다.)

**`rpm`: 설치된 패키지 조회**

`rpm`은 `dnf`보다 아래 단계의 도구로, 이미 설치된 패키지를 조회할 때 유용합니다.

```bash
rpm -qa | grep openssh           # 설치된 패키지 중 openssh가 들어간 것
rpm -ql openssh-server | head    # 패키지가 설치한 파일 목록
rpm -qf /etc/ssh/sshd_config     # 이 파일은 어느 패키지가 설치했는가
```

> **체크포인트 3.5:** `dnf provides`로 명령어가 들어 있는 패키지를 찾을 수 있습니다.

---

## 3.6 Java 설치

### 3.6.1 설치할 패키지 고르기

```bash
dnf search openjdk | grep -E "^java-(17|21)"
```

Rocky Linux는 Java 버전마다 여러 패키지를 제공합니다.

| 패키지 | 내용 | 용도 |
|---|---|---|
| `java-21-openjdk-headless` | 실행 환경 (GUI 라이브러리 제외) | 서버에서 jar 실행 |
| `java-21-openjdk` | 실행 환경 + GUI 라이브러리 | 데스크톱 프로그램 실행 |
| `java-21-openjdk-devel` | 개발 도구 (`javac`, `jar` 등) | 서버에서 컴파일이 필요할 때 |

운영 서버에서 Spring Boot jar를 실행하는 데는 **`-headless`만으로 충분합니다.** 빌드는 개발 PC나 CI 서버에서 하고, 운영 서버에는 실행 환경만 두는 것이 원칙입니다. 이 과정에서는 실습용 jar를 서버에서 직접 만들어 보기 위해 `-devel`도 설치합니다.

### 3.6.2 Java 21 설치

```bash
sudo dnf install -y java-21-openjdk-devel
java -version
javac -version
```

```text
openjdk version "21.0.x" ...
OpenJDK Runtime Environment (Red_Hat-21.0.x...) ...
```

`java` 명령은 실제로 어디에 있을까요?

```bash
which java                       # /usr/bin/java
ls -l /usr/bin/java              # /etc/alternatives/java를 가리키는 링크
readlink -f /usr/bin/java        # 최종 실제 위치
ls -l /usr/lib/jvm               # 설치된 JDK 목록
```

`/usr/bin/java` → `/etc/alternatives/java` → `/usr/lib/jvm/java-21-openjdk-.../bin/java`처럼 **링크가 두 단계**로 연결되어 있습니다. 가운데의 `/etc/alternatives`가 여러 버전 중 무엇을 쓸지 결정합니다.

### 3.6.3 여러 Java 버전 사용하기: `alternatives`

실무에서는 오래된 프로젝트(Spring Boot 2.x, Java 11·17)와 새 프로젝트(Spring Boot 3.x, Java 17·21)를 같은 서버에서 다뤄야 할 때가 있습니다. Java 17 실행 환경을 추가로 설치해 봅니다.

```bash
sudo dnf install -y java-17-openjdk-headless
sudo alternatives --config java
```

```text
There are 2 programs which provide 'java'.

  Selection    Command
-----------------------------------------------
*+ 1           java-21-openjdk.x86_64 (/usr/lib/jvm/java-21-openjdk-.../bin/java)
   2           java-17-openjdk.x86_64 (/usr/lib/jvm/java-17-openjdk-.../bin/java)

Enter to keep the current selection[+], or type selection number:
```

Java 17의 번호를 입력하고 확인합니다.

```bash
java -version                    # 17로 바뀌었습니다
javac -version                   # javac는 여전히 21입니다 (devel은 21만 설치했기 때문)
```

이 상태는 3.6.4절의 실습에서 사용합니다. 실습 후에 다시 21로 돌려놓습니다.

### 3.6.4 실습용 jar 만들기

이 장과 4장의 실습에서 사용할 작은 웹 서버를 만듭니다. Spring Boot는 아니지만, Spring Boot 애플리케이션과 같은 방식으로 동작하도록 만들었습니다.

- `SERVER_PORT`, `SPRING_PROFILES_ACTIVE` 환경 변수로 포트와 프로파일을 바꿀 수 있습니다.
- `LOG_FILE` 환경 변수를 지정하면 로그를 파일에도 씁니다.
- `/health`로 요청하면 상태를 JSON으로 응답합니다(Spring Boot Actuator의 `/actuator/health`와 비슷합니다).
- 정상 종료 신호를 받으면 `Shutting down gracefully...`를 남깁니다.

아래 블록 전체를 복사해 SSH 터미널에 붙여넣습니다.

```bash
mkdir -p ~/build && cd ~/build
cat > MiniServer.java << 'EOF'
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

public class MiniServer {

    static final String VERSION = "1.0.0";
    static final String LOG_FILE = System.getenv("LOG_FILE");

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));
        String profile = System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "default");
        long pid = ProcessHandle.current().pid();

        log("Starting MiniServer v" + VERSION + " (profile: " + profile + ", PID: " + pid + ")");

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", exchange -> {
            String body = "{\"status\":\"UP\",\"version\":\"" + VERSION
                    + "\",\"profile\":\"" + profile + "\"}\n";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
            log("GET /health");
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                log("Shutting down gracefully...");
            } catch (UncheckedIOException ignored) {
            }
        }));

        server.start();
        log("Started on port " + port);
    }

    static void log(String message) {
        String line = LocalDateTime.now() + "  INFO " + message + System.lineSeparator();
        System.out.print(line);
        if (LOG_FILE != null) {
            try {
                Files.writeString(Path.of(LOG_FILE), line,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
EOF
```

컴파일하고 jar로 묶습니다.

```bash
javac MiniServer.java                                   # MiniServer.class 생성
jar cfe shop-api-1.0.0.jar MiniServer MiniServer.class  # c: 만들기, f: 파일 이름, e: 시작 클래스
unzip -l shop-api-1.0.0.jar                             # jar 안을 들여다봅니다
```

```text
  Length      Date    Time    Name
---------  ---------- -----   ----
        0  2026-10-02 10:30   META-INF/
       79  2026-10-02 10:30   META-INF/MANIFEST.MF
     5xxx  2026-10-02 10:30   MiniServer.class
```

`MANIFEST.MF` 파일에 시작 클래스(`Main-Class`)가 기록되어 있어서 `java -jar`로 바로 실행할 수 있습니다.

```bash
unzip -p shop-api-1.0.0.jar META-INF/MANIFEST.MF        # -p: 파일 내용을 화면에 출력
```

**실행해 보기: Java 버전 불일치 오류**

지금 `java`는 17, `javac`는 21입니다. Java 21로 컴파일한 jar를 Java 17로 실행하면 어떻게 될까요?

```bash
java -jar shop-api-1.0.0.jar
```

```text
Error: LinkageError occurred while loading main class MiniServer
	java.lang.UnsupportedClassVersionError: MiniServer has been compiled by a more recent version
	of the Java Runtime (class file version 65.0), this version of the Java Runtime only
	recognizes class file versions up to 61.0
```

| class file version | Java 버전 |
|---|---|
| 52.0 | 8 |
| 55.0 | 11 |
| 61.0 | 17 |
| 65.0 | 21 |

**이 오류는 실무에서 매우 자주 만납니다.** 개발 PC에서 Java 21로 빌드한 Spring Boot 3 애플리케이션을 Java 17이 설치된 서버에 올렸을 때 정확히 이 메시지가 나옵니다. 이 메시지를 보면 "서버의 Java 버전이 빌드한 버전보다 낮다"는 것을 바로 알 수 있어야 합니다.

Java 21로 되돌리고 다시 실행합니다.

```bash
sudo alternatives --config java      # java-21 번호 선택
java -version
java -jar shop-api-1.0.0.jar
```

```text
2026-10-02T10:35:12.123456  INFO Starting MiniServer v1.0.0 (profile: default, PID: 2210)
2026-10-02T10:35:12.156789  INFO Started on port 8080
```

서버가 실행되고 프롬프트가 돌아오지 않습니다. **Windows 터미널에서 새 탭을 열어** `ssh spring`으로 접속한 뒤 요청을 보내 봅니다.

```bash
curl localhost:8080/health
```

```json
{"status":"UP","version":"1.0.0","profile":"default"}
```

첫 번째 창에 `GET /health` 로그가 찍힌 것을 확인하고, `Ctrl + C`로 종료합니다. 종료할 때 `Shutting down gracefully...`가 출력되는지도 확인합니다.

**두 번째 버전 만들기**

3.4절에서 배운 링크로 버전을 바꾸는 실습을 위해 1.0.1 버전을 만듭니다.

```bash
vim MiniServer.java       # VERSION = "1.0.0"을 "1.0.1"로 바꾸고 저장 (/VERSION 으로 검색)
javac MiniServer.java
jar cfe shop-api-1.0.1.jar MiniServer MiniServer.class
ls -l *.jar
```

> **체크포인트 3.6:** `UnsupportedClassVersionError`를 직접 재현하고 원인을 설명할 수 있습니다. `~/build`에 `shop-api-1.0.0.jar`와 `shop-api-1.0.1.jar`가 있습니다.

---

## 3.7 환경 변수

### 3.7.1 환경 변수란

**환경 변수**는 실행되는 프로그램에 전달되는 "이름=값" 형태의 설정입니다. 프로그램은 코드를 바꾸지 않고도 환경 변수에 따라 다르게 동작할 수 있습니다. Java에서는 `System.getenv("이름")`으로 읽습니다.

```bash
env | head               # 현재 셸의 모든 환경 변수
echo $HOME               # 변수 값 보기: 이름 앞에 $
echo $USER
echo $PATH
```

### 3.7.2 `PATH`: 명령어를 찾는 경로

```bash
echo $PATH
```

```text
/home/devuser/.local/bin:/home/devuser/bin:/usr/local/bin:/usr/bin:/usr/local/sbin:/usr/sbin
```

`java`를 입력하면 셸은 `PATH`에 적힌 디렉터리를 **왼쪽부터 순서대로** 찾아, 처음 발견한 `java`를 실행합니다. `PATH`에 없는 위치의 프로그램은 전체 경로로 실행해야 합니다.

```bash
which java                       # PATH의 /usr/bin에서 찾았습니다
which namei                      # 3.9절에서 사용할 명령도 PATH 안에 있습니다
/usr/bin/java -version           # 전체 경로로 실행해도 같습니다
```

> **현재 디렉터리의 스크립트를 실행할 때 `./`를 붙이는 이유:** 보안을 위해 현재 디렉터리(`.`)는 `PATH`에 들어 있지 않습니다. 그래서 5장에서 만들 `deploy.sh`는 `./deploy.sh`로 실행합니다.

### 3.7.3 변수 만들기와 `export`

```bash
GREETING="hello"          # 셸 변수 만들기 (= 앞뒤에 공백이 없어야 합니다)
echo $GREETING
bash -c 'echo "자식 프로세스: $GREETING"'     # 비어 있습니다
export GREETING           # 환경 변수로 내보내기
bash -c 'echo "자식 프로세스: $GREETING"'     # 이제 전달됩니다
unset GREETING            # 변수 삭제
```

| 구분 | 범위 |
|---|---|
| 셸 변수 | 현재 셸 안에서만 사용 |
| 환경 변수 (`export`) | 현재 셸 **그리고 여기서 실행하는 모든 프로그램**에 전달 |

`java -jar`로 실행한 Spring Boot 애플리케이션도 셸이 실행한 프로그램이므로, `export`한 변수만 전달받습니다.

### 3.7.4 Spring Boot와 환경 변수

Spring Boot는 환경 변수를 **설정 값으로 자동으로 읽습니다.** 이름은 다음 규칙으로 바꿉니다.

| `application.yml` 속성 | 환경 변수 이름 |
|---|---|
| `server.port` | `SERVER_PORT` |
| `spring.profiles.active` | `SPRING_PROFILES_ACTIVE` |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` |

규칙: 점(`.`)은 밑줄(`_`)로, 하이픈(`-`)은 지우고, 모두 대문자로 씁니다.

그래서 운영 서버에서는 DB 비밀번호 같은 민감한 값을 jar나 Git에 넣지 않고 **환경 변수로 전달**하는 방식을 많이 사용합니다.

**실습: 환경 변수로 실행 방식 바꾸기**

```bash
cd ~/build
export SPRING_PROFILES_ACTIVE=dev
java -jar shop-api-1.0.0.jar
```

다른 창에서 `curl localhost:8080/health`를 실행하면 `"profile":"dev"`가 나옵니다. `Ctrl + C`로 종료합니다.

명령 앞에 변수를 쓰면 **그 명령에만** 적용됩니다.

```bash
SERVER_PORT=9090 SPRING_PROFILES_ACTIVE=prod java -jar shop-api-1.0.0.jar
```

다른 창에서 확인합니다.

```bash
curl localhost:9090/health       # "profile":"prod"
curl localhost:8080/health       # 연결 실패: 8080에서는 실행 중이 아닙니다
```

종료한 뒤 `echo $SERVER_PORT`를 실행하면 비어 있습니다. 그 명령에만 적용되었기 때문입니다.

### 3.7.5 설정을 영구적으로 저장하기

`export`는 로그아웃하면 사라집니다. 로그인할 때마다 적용하려면 설정 파일에 적습니다.

| 파일 | 적용 범위 |
|---|---|
| `~/.bashrc` | 나(현재 사용자)의 셸 |
| `/etc/profile.d/이름.sh` | 서버에 로그인하는 모든 사용자의 셸 |

`JAVA_HOME`을 설정해 봅니다. `JAVA_HOME`은 Maven, Gradle, Tomcat 같은 도구가 JDK 위치를 찾을 때 사용합니다.

```bash
ls -l /usr/lib/jvm               # java-21-openjdk 라는 이름의 링크를 확인합니다
vim ~/.bashrc
```

파일 끝에 다음을 추가합니다.

```bash
# Java
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export PATH=$JAVA_HOME/bin:$PATH
```

버전 번호가 들어간 실제 디렉터리(`java-21-openjdk-21.0.x...`) 대신 **버전 번호가 없는 링크**를 사용하면, `dnf update`로 Java가 업데이트되어도 설정을 고칠 필요가 없습니다.

저장한 뒤 현재 셸에 바로 적용합니다.

```bash
source ~/.bashrc                 # 설정 파일을 다시 읽기
echo $JAVA_HOME
$JAVA_HOME/bin/java -version
```

> **중요: `.bashrc`는 서비스에 적용되지 않습니다.** `.bashrc`는 **사람이 로그인해서 셸을 열 때** 읽는 파일입니다. 4.5절에서 systemd로 실행하는 서비스는 셸을 거치지 않으므로 `.bashrc`의 환경 변수를 전혀 받지 못합니다. "터미널에서 실행하면 되는데 서비스로 등록하면 프로파일이 적용되지 않는다"는 문제의 원인이 대부분 이것입니다. 서비스의 환경 변수는 서비스 설정 파일에 따로 지정합니다.

> **체크포인트 3.7:** 환경 변수로 실습용 서버의 포트와 프로파일을 바꾸어 실행했습니다. `.bashrc`에 `JAVA_HOME`을 설정했습니다.

---

## 3.8 프로세스 관리

### 3.8.1 프로세스란

**프로그램**은 디스크에 저장된 파일이고, **프로세스**는 그 프로그램이 메모리에 올라가 실행 중인 상태입니다. 같은 jar를 두 번 실행하면 프로세스가 두 개 생깁니다. 모든 프로세스는 고유한 번호인 **PID**를 가집니다.

### 3.8.2 프로세스 보기: `ps`, `pgrep`

먼저 실습용 서버를 실행해 둡니다. (첫 번째 창)

```bash
cd ~/build
java -jar shop-api-1.0.0.jar
```

두 번째 창에서 프로세스를 찾아봅니다.

```bash
ps                           # 현재 터미널에서 실행한 프로세스만
ps -ef | grep java           # 시스템 전체에서 java 찾기
ps aux | grep java           # 다른 형식 (CPU, 메모리 사용률 포함)
pgrep -a java                # 이름으로 찾기 (PID와 명령줄)
```

```text
UID          PID    PPID  C STIME TTY          TIME CMD
devuser     2350    2210  2 10:40 pts/0    00:00:01 java -jar shop-api-1.0.0.jar
devuser     2402    2380  0 10:41 pts/1    00:00:00 grep --color=auto java
```

| 열 | 의미 |
|---|---|
| `UID` | 실행한 사용자 (**3.9절에서 `appuser`로 바뀌는 것을 확인합니다**) |
| `PID` | 프로세스 번호 |
| `PPID` | 부모 프로세스 번호 (이 프로세스를 실행한 프로세스) |
| `CMD` | 실행한 명령 |

> `ps -ef | grep java` 결과에 `grep java` 자신도 함께 나옵니다. `grep`도 실행 중인 프로세스이기 때문입니다. `pgrep`은 이런 문제가 없습니다.

### 3.8.3 실시간 모니터링: `top`

```bash
top
```

| 키 | 동작 |
|---|---|
| `P` | CPU 사용률 순서로 정렬 |
| `M` | 메모리 사용률 순서로 정렬 |
| `1` | CPU 코어별로 보기 |
| `u` | 특정 사용자의 프로세스만 보기 |
| `q` | 종료 |

윗부분의 `load average`는 최근 1분, 5분, 15분 동안 실행을 기다린 작업의 평균 수입니다. CPU 코어 수(이 VM은 2)보다 계속 크다면 서버가 바쁘다는 뜻입니다. `M`을 눌러 `java` 프로세스가 메모리를 얼마나 쓰는지(`RES` 열) 확인해 보십시오.

> **참고:** 보기 편한 `htop`은 EPEL 저장소에 있습니다(`sudo dnf install -y epel-release && sudo dnf install -y htop`).

### 3.8.4 프로세스 종료: `kill`과 신호

`kill`은 이름과 달리 프로세스에 **신호(signal)**를 보내는 명령입니다.

| 신호 | 번호 | 의미 | 보내는 방법 |
|---|---|---|---|
| `SIGTERM` | 15 | 정상 종료 요청. 프로그램이 마무리 작업을 할 수 있습니다 | `kill PID` (기본값) |
| `SIGINT` | 2 | 중단 요청 | `Ctrl + C` |
| `SIGKILL` | 9 | 강제 종료. 프로그램이 거부하거나 마무리할 수 없습니다 | `kill -9 PID` |
| `SIGHUP` | 1 | 터미널 연결이 끊김 | SSH 연결 끊김 |
| `SIGTSTP` | 20 | 일시 정지 | `Ctrl + Z` |

**실습: 정상 종료와 강제 종료 비교하기**

1. 두 번째 창에서 PID를 확인하고 정상 종료 신호를 보냅니다.

```bash
pgrep -a java
kill 2350                    # 확인한 PID로 바꿔 입력합니다
```

첫 번째 창에 `Shutting down gracefully...`가 출력되고 종료됩니다.

2. 첫 번째 창에서 다시 실행하고, 이번에는 강제 종료합니다.

```bash
java -jar shop-api-1.0.0.jar          # 첫 번째 창
```

```bash
kill -9 $(pgrep java)                 # 두 번째 창
```

첫 번째 창에는 `Killed`만 출력되고 `Shutting down gracefully...`는 나오지 않습니다. 프로그램이 마무리할 기회를 얻지 못한 것입니다.

`$(명령)`은 **명령 치환**입니다. 괄호 안 명령의 결과(여기서는 PID)가 그 자리에 들어갑니다.

> **`kill -9`를 습관적으로 쓰지 마십시오.** Spring Boot는 `SIGTERM`을 받으면 처리 중인 요청을 마치고, DB 연결 풀을 닫고, `@PreDestroy` 메서드를 실행한 뒤 종료합니다(graceful shutdown). `kill -9`는 이 과정을 모두 건너뛰므로 처리 중이던 주문이 반쯤 저장되는 등의 문제가 생길 수 있습니다. 항상 `kill`로 먼저 요청하고, 일정 시간이 지나도 종료되지 않을 때만 `kill -9`를 사용합니다.

### 3.8.5 포그라운드와 백그라운드

지금까지는 서버를 실행하면 그 창을 쓸 수 없어 창을 두 개 열었습니다. **백그라운드**로 실행하면 한 창에서 계속 작업할 수 있습니다.

| 명령 | 동작 |
|---|---|
| `명령 &` | 백그라운드로 실행 |
| `jobs` | 현재 셸의 백그라운드 작업 목록 |
| `fg %번호` | 백그라운드 작업을 포그라운드로 가져오기 |
| `Ctrl + Z` | 포그라운드 작업을 일시 정지 |
| `bg %번호` | 일시 정지한 작업을 백그라운드에서 계속 실행 |

```bash
cd ~/build
java -jar shop-api-1.0.0.jar &        # [1] 2501 처럼 작업 번호와 PID가 표시됩니다
curl localhost:8080/health            # 같은 창에서 바로 요청할 수 있습니다
jobs
fg %1                                 # 포그라운드로 가져오기
```

`Ctrl + Z`로 일시 정지한 뒤 상태를 확인합니다.

```bash
jobs                                  # Stopped
curl --max-time 3 localhost:8080/health   # 응답이 없습니다: 일시 정지 상태이기 때문입니다
bg %1                                 # 백그라운드에서 다시 실행
curl localhost:8080/health            # 다시 응답합니다
kill %1                               # 작업 번호로도 종료할 수 있습니다
```

로그가 화면에 섞여 나와 불편하다면, 2장에서 배운 리다이렉션으로 파일에 저장합니다.

```bash
java -jar shop-api-1.0.0.jar > app.out 2>&1 &
tail -f app.out                       # Ctrl + C로 tail만 종료 (서버는 계속 실행)
kill %1
```

### 3.8.6 SSH 연결이 끊겨도 실행하기: `nohup`

**실습: 연결을 끊으면 어떻게 되는가**

1. 백그라운드로 실행합니다.

```bash
java -jar ~/build/shop-api-1.0.0.jar > ~/build/app.out 2>&1 &
```

2. Windows 터미널 탭의 **X 버튼**으로 창을 닫아 SSH 연결을 끊습니다. (`exit`가 아니라 연결이 갑자기 끊긴 상황을 만드는 것입니다.)
3. 다시 접속해 확인합니다.

```bash
pgrep -a java                         # 아무것도 나오지 않습니다
```

SSH 연결이 끊기면 그 셸에서 실행한 프로세스들이 `SIGHUP` 신호를 받고 함께 종료됩니다. 노트북을 닫거나 와이파이가 잠시 끊겨도 서버가 내려가는 것입니다.

`nohup`(no hang up)은 `SIGHUP`을 무시하도록 실행합니다.

```bash
nohup java -jar ~/build/shop-api-1.0.0.jar > ~/build/app.out 2>&1 &
```

다시 창을 닫았다가 접속해 확인합니다.

```bash
pgrep -a java                         # 계속 실행 중입니다
curl localhost:8080/health
kill $(pgrep java)
```

> **`nohup`은 임시 방편입니다.** `nohup`으로 실행한 애플리케이션은 서버가 재부팅되면 다시 시작되지 않고, 비정상 종료되어도 아무도 알아채지 못합니다. 운영 환경에서는 4.5절에서 배울 **systemd 서비스**로 실행합니다. 하지만 `nohup ... > 파일 2>&1 &` 형식은 오래된 서버의 배포 스크립트에서 아직도 자주 볼 수 있으니 의미를 알아 두어야 합니다.

### 3.8.7 "포트가 이미 사용 중입니다"

같은 포트로 서버를 두 번 실행해 봅니다.

```bash
java -jar ~/build/shop-api-1.0.0.jar > ~/build/app.out 2>&1 &
java -jar ~/build/shop-api-1.0.0.jar
```

```text
Exception in thread "main" java.net.BindException: Address already in use
```

Spring Boot에서는 `Web server failed to start. Port 8080 was already in use.`라는 메시지로 나타납니다. 이전에 실행한 프로세스가 아직 살아 있다는 뜻이므로, 그 프로세스를 찾아 종료합니다. 어떤 프로세스가 포트를 쓰고 있는지 정확히 찾는 방법(`ss -tlnp`)은 4.1절에서 배웁니다.

```bash
pgrep -a java
kill $(pgrep java)
```

> **체크포인트 3.8:** `kill`과 `kill -9`의 차이를 로그로 확인했습니다. `nohup` 없이 백그라운드로 실행한 프로세스가 SSH 연결 종료와 함께 사라지는 것을 확인했습니다.

---

## 3.9 실습: 애플리케이션 전용 계정과 권한 설정

이제 이 장에서 배운 내용을 모아, 2장에서 만든 `~/shop-api`를 운영 서버의 구조로 옮기고 `appuser` 계정으로 실행합니다.

### 실습 A. 전용 계정 만들기

```bash
sudo useradd --system --no-create-home --shell /sbin/nologin appuser
id appuser
grep appuser /etc/passwd
```

`devuser`를 `appuser` 그룹에 추가합니다. 그래야 `devuser`가 애플리케이션 로그를 읽을 수 있습니다.

```bash
sudo usermod -aG appuser devuser
id                           # 아직 appuser 그룹이 보이지 않습니다
```

그룹 변경은 **다시 로그인해야** 적용됩니다. `exit`로 접속을 끊고 `ssh spring`으로 다시 접속합니다.

```bash
id                           # groups=...,10(wheel),9xx(appuser)
```

### 실습 B. 디렉터리를 `/opt`로 옮기기

```bash
sudo mv ~/shop-api /opt/
ls -l /opt
```

2장에서 `touch`로 만든 빈 jar 파일과, 홈 디렉터리 시절의 `logs` 디렉터리를 정리합니다.

```bash
rm /opt/shop-api/releases/*.jar /opt/shop-api/backup/*.jar
rm -r /opt/shop-api/logs
```

3.6절에서 만든 실제 jar를 복사하고, 현재 버전을 가리키는 링크를 만듭니다.

```bash
cp ~/build/shop-api-1.0.0.jar ~/build/shop-api-1.0.1.jar /opt/shop-api/releases/
ln -s releases/shop-api-1.0.0.jar /opt/shop-api/current.jar
ls -l /opt/shop-api
```

링크를 `releases/...`처럼 **상대 경로**로 만든 이유는, 나중에 `/opt/shop-api` 디렉터리 전체를 다른 위치로 옮기거나 백업에서 복원해도 링크가 깨지지 않게 하기 위해서입니다.

로그 디렉터리를 만듭니다.

```bash
sudo mkdir /var/log/shop-api
```

### 실습 C. 소유자와 권한 설정하기

설계는 다음과 같습니다.

| 대상 | 소유자:그룹 | 권한 | 이유 |
|---|---|---|---|
| `/opt/shop-api` 전체 | `devuser:appuser` | 디렉터리 `750`, 파일 `640` | 배포 담당자(`devuser`)만 수정하고, 애플리케이션(`appuser`)은 **읽기만** 합니다 |
| `/opt/shop-api/backup` | `devuser:devuser` | `700` | 애플리케이션이 볼 필요가 없습니다 |
| `/var/log/shop-api` | `appuser:appuser` | `750` | 애플리케이션이 로그를 씁니다. `devuser`는 그룹 권한으로 읽습니다 |

> **애플리케이션이 자기 jar와 설정을 수정할 수 없게 하는 이유:** 애플리케이션에 파일 업로드 취약점 같은 문제가 있어도, 공격자가 jar나 설정 파일을 바꿔치기할 수 없습니다. 애플리케이션이 **써야 하는 곳은 로그 디렉터리뿐**입니다.

```bash
sudo chown -R devuser:appuser /opt/shop-api
sudo find /opt/shop-api -type d -exec chmod 750 {} \;
sudo find /opt/shop-api -type f -exec chmod 640 {} \;
sudo chown devuser:devuser /opt/shop-api/backup
sudo chmod 700 /opt/shop-api/backup
sudo chown appuser:appuser /var/log/shop-api
sudo chmod 750 /var/log/shop-api
```

`find ... -type d -exec chmod 750 {} \;`는 "디렉터리(`-type d`)를 모두 찾아, 찾은 것(`{}`) 각각에 `chmod 750`을 실행하라"는 뜻입니다. `chmod -R 750`을 쓰면 파일에도 불필요한 `x` 권한이 붙기 때문에 디렉터리와 파일을 나누어 지정했습니다.

결과를 확인합니다.

```bash
tree -pug /opt/shop-api      # -p 권한, -u 소유자, -g 그룹
ls -ld /var/log/shop-api
```

### 실습 D. `appuser`로 실행하기

`appuser`는 로그인할 수 없는 계정이므로, `sudo -u`로 그 계정의 권한을 빌려 실행합니다.

`sudo`가 백그라운드에서 비밀번호를 물으면 명령이 멈춰 버리므로, 먼저 `sudo -v`로 인증해 둡니다.

```bash
sudo -v
sudo -u appuser env \
  SPRING_PROFILES_ACTIVE=prod \
  LOG_FILE=/var/log/shop-api/app.log \
  nohup java -jar /opt/shop-api/current.jar > /dev/null 2>&1 &
```

| 부분 | 의미 |
|---|---|
| `\` | 긴 명령을 여러 줄로 나누어 씁니다 (줄 끝의 `\` 뒤에는 공백이 없어야 합니다) |
| `sudo -u appuser` | `appuser` 권한으로 실행 |
| `env 변수=값 ...` | 환경 변수를 지정한 뒤 뒤의 명령을 실행 |
| `> /dev/null 2>&1` | 화면 출력은 버립니다. 로그는 `LOG_FILE`로 파일에 남습니다 |

> **왜 `env`를 쓰는가:** `sudo`는 보안을 위해 기본적으로 현재 셸의 환경 변수를 **전달하지 않습니다.** `export SPRING_PROFILES_ACTIVE=prod`를 해 두고 `sudo -u appuser java -jar ...`로 실행하면 프로파일이 적용되지 않습니다. `env`로 실행할 명령과 함께 지정하면 확실하게 전달됩니다.

확인합니다.

```bash
ps -ef | grep [j]ava                 # UID 열이 appuser인지 확인 ([j]ava는 grep 자신을 결과에서 빼는 요령입니다)
curl localhost:8080/health           # "version":"1.0.0","profile":"prod"
ls -l /var/log/shop-api              # app.log의 소유자는 appuser
tail /var/log/shop-api/app.log       # devuser도 그룹 권한으로 읽을 수 있습니다
```

### 실습 E. 링크로 버전 바꾸기

```bash
ln -sfn releases/shop-api-1.0.1.jar /opt/shop-api/current.jar
ls -l /opt/shop-api/current.jar
curl localhost:8080/health           # 아직 1.0.0입니다
```

링크를 바꾸어도 **이미 실행 중인 프로세스는 예전 jar를 계속 사용합니다.** 재시작해야 새 버전이 적용됩니다.

```bash
sudo kill $(pgrep -u appuser java)   # appuser의 java 프로세스에 정상 종료 신호
sudo -v
sudo -u appuser env SPRING_PROFILES_ACTIVE=prod LOG_FILE=/var/log/shop-api/app.log \
  nohup java -jar /opt/shop-api/current.jar > /dev/null 2>&1 &
curl localhost:8080/health           # "version":"1.0.1"
tail -n 6 /var/log/shop-api/app.log  # 종료와 재시작 기록
```

`appuser`의 프로세스이므로 `devuser`가 종료하려면 `sudo`가 필요합니다. 다른 사용자의 프로세스는 함부로 종료할 수 없습니다.

### 실습 F. 백업 만들기

```bash
tar czf /opt/shop-api/backup/config-$(date +%Y%m%d).tar.gz -C /opt/shop-api config
ls -l /opt/shop-api/backup
tar tzf /opt/shop-api/backup/config-*.tar.gz
```

`$(date +%Y%m%d)`는 오늘 날짜(예: `20261002`)로 바뀝니다. 5장의 배포 스크립트에서 이 방법을 그대로 사용합니다.

### 실습 G. "Permission denied" 재현하고 해결하기

실무에서 자주 만나는 권한 문제를 일부러 만들어 보고 해결합니다. 각 문제마다 **증상 → 누가 → 무엇에 접근하다 → 무엇이 막혔는가**를 먼저 생각한 뒤 해답을 확인하십시오.

먼저 실행 중인 애플리케이션을 종료합니다.

```bash
sudo kill $(pgrep -u appuser java)
```

다음 명령을 여러 번 쓰므로 **별칭(alias)**으로 등록해 둡니다. `vim ~/.bashrc`로 파일을 열고 끝에 다음 줄을 추가한 뒤 저장합니다.

```bash
alias runapp='sudo -u appuser env SPRING_PROFILES_ACTIVE=prod LOG_FILE=/var/log/shop-api/app.log java -jar /opt/shop-api/current.jar'
```

```bash
source ~/.bashrc
```

이번에는 결과를 바로 볼 수 있도록 포그라운드로 실행합니다. 정상이라면 `Started on port 8080`이 나오고, `Ctrl + C`로 종료합니다.

**문제 G-1. 로그 디렉터리**

```bash
sudo chown root:root /var/log/shop-api
runapp
```

```text
Exception in thread "main" java.io.UncheckedIOException: java.nio.file.AccessDeniedException: /var/log/shop-api/app.log
```

<details>
<summary>원인과 해결</summary>

- **누가:** `appuser`
- **무엇에:** `/var/log/shop-api/app.log`에 쓰려고 함
- **무엇이 막혔는가:** 디렉터리의 소유자가 `root`이고 권한이 `750`이므로, `appuser`는 기타 사용자(`---`)에 해당해 들어갈 수도 쓸 수도 없습니다.

```bash
sudo chown appuser:appuser /var/log/shop-api
runapp
```

Spring Boot에서는 `logging.file.name`에 지정한 위치에 쓸 수 없을 때 이와 비슷한 오류로 시작에 실패합니다. 로그 디렉터리를 `root`로 만들고 소유자를 바꾸지 않는 실수가 매우 흔합니다.

</details>

**문제 G-2. jar 파일**

```bash
chmod 600 /opt/shop-api/releases/shop-api-1.0.1.jar
runapp
```

```text
Error: Unable to access jarfile /opt/shop-api/current.jar
```

<details>
<summary>원인과 해결</summary>

- **누가:** `appuser`
- **무엇에:** `current.jar`가 가리키는 `shop-api-1.0.1.jar`를 읽으려고 함
- **무엇이 막혔는가:** 파일 권한이 `600`이라 그룹(`appuser`)에게 읽기 권한이 없습니다.

```bash
chmod 640 /opt/shop-api/releases/shop-api-1.0.1.jar
runapp
```

**실무에서 이 문제가 생기는 경로:** 새 jar를 다른 곳에서 복사해 오면 `umask`나 원래 파일의 권한에 따라 `600`이 될 수 있습니다. 그래서 5장의 배포 스크립트에는 복사한 뒤 권한을 다시 지정하는 단계가 들어갑니다.

</details>

**문제 G-3. 디렉터리의 `x` 권한**

```bash
chmod 740 /opt/shop-api/releases
runapp
```

jar 파일의 권한은 `640`으로 정상인데도 같은 오류가 납니다.

<details>
<summary>원인과 해결</summary>

- **누가:** `appuser`
- **무엇에:** `/opt/shop-api/releases/shop-api-1.0.1.jar`
- **무엇이 막혔는가:** `releases` 디렉터리의 그룹 권한이 `r--`라 `x`가 없습니다. 디렉터리에 `x`가 없으면 그 안의 파일에 **접근할 수 없습니다.** 파일 자체의 권한이 아무리 넉넉해도 소용이 없습니다.

경로 전체의 권한을 한눈에 확인하는 `namei` 명령이 이런 문제를 찾는 데 유용합니다.

```bash
namei -l /opt/shop-api/current.jar
```

```text
f: /opt/shop-api/current.jar
dr-xr-xr-x root    root    /
drwxr-xr-x root    root    opt
drwxr-x--- devuser appuser shop-api
lrwxrwxrwx devuser appuser current.jar -> releases/shop-api-1.0.1.jar
drwxr----- devuser appuser releases          ← 그룹에 x가 없습니다
-rw-r----- devuser appuser shop-api-1.0.1.jar
```

```bash
chmod 750 /opt/shop-api/releases
runapp
```

</details>

**문제 G-4. 로그를 읽을 수 없습니다**

이번에는 `devuser`를 `appuser` 그룹에서 빼 봅니다.

```bash
sudo gpasswd -d devuser appuser      # devuser를 appuser 그룹에서 제거
```

`exit`로 접속을 끊고 다시 접속한 뒤 로그를 읽어 봅니다.

```bash
tail /var/log/shop-api/app.log
```

```text
tail: cannot open '/var/log/shop-api/app.log' for reading: Permission denied
```

<details>
<summary>원인과 해결</summary>

- **누가:** `devuser`
- **무엇에:** `/var/log/shop-api/app.log`
- **무엇이 막혔는가:** `devuser`가 더 이상 `appuser` 그룹의 구성원이 아니므로 기타 사용자 권한(`---`)이 적용됩니다.

```bash
sudo usermod -aG appuser devuser
```

다시 로그인한 뒤 `id`로 그룹을 확인하고 로그를 읽습니다. **그룹을 추가한 직후에는 다시 로그인해야 적용된다**는 점을 잊으면, 권한을 분명히 주었는데도 계속 거부되는 상황을 만나게 됩니다.

</details>

**토론:** 위의 네 문제를 모두 `sudo chmod -R 777 /opt/shop-api /var/log/shop-api` 한 줄로 "해결"할 수도 있습니다. 이 방법이 왜 위험한지, 실습 C의 설계 원칙과 비교해 설명해 보십시오.

> **체크포인트 3.9:** `appuser`로 실행한 애플리케이션이 `/health`에 `"version":"1.0.1","profile":"prod"`로 응답하고, 네 가지 권한 문제를 원인과 함께 해결했습니다.

### 실습이 끝나면

```bash
sudo kill $(pgrep -u appuser java)
sudo poweroff
```

VirtualBox에서 스냅샷 `03-Java-설치-완료`를 만듭니다.

---

## 3장 정리

| 절 | 핵심 내용 |
|---|---|
| 3.1 | 사용자 정보는 `/etc/passwd`, 그룹은 `/etc/group`에 있습니다. 애플리케이션은 로그인할 수 없는 전용 계정으로 실행합니다. `usermod`에는 반드시 `-aG`를 씁니다 |
| 3.2 | 권한은 소유자·그룹·기타 순서의 `rwx`입니다. 디렉터리의 `x`가 없으면 안의 파일에 접근할 수 없습니다. `777`은 해결책이 아닙니다 |
| 3.3 | 관리자 작업은 `sudo`로 합니다. `wheel` 그룹이 `sudo` 권한을 가집니다. `sudoers`는 `visudo`로만 수정합니다 |
| 3.4 | `ln -sfn`으로 `current.jar` 링크를 바꾸어 배포와 롤백을 합니다. `tar czf`로 묶고 `tar xzf`로 풉니다. jar는 zip 형식입니다 |
| 3.5 | `dnf search/install/remove`로 패키지를 관리하고, `dnf provides`로 명령어가 든 패키지를 찾습니다 |
| 3.6 | 운영 서버에는 `-headless`면 충분합니다. `alternatives`로 버전을 바꿉니다. `UnsupportedClassVersionError`는 서버의 Java 버전이 낮다는 뜻입니다 |
| 3.7 | `export`한 변수만 프로그램에 전달됩니다. Spring Boot는 `SERVER_PORT` 같은 환경 변수를 설정으로 읽습니다. `.bashrc`는 서비스에 적용되지 않습니다 |
| 3.8 | `kill`(SIGTERM)로 먼저 요청하고, `kill -9`는 마지막 수단입니다. SSH가 끊기면 백그라운드 프로세스도 종료되므로, 임시로는 `nohup`을, 운영에서는 systemd를 씁니다 |

## 3장에서 사용한 명령어

**사용자와 권한**

| 명령어 | 용도 |
|---|---|
| `id`, `whoami`, `who` | 사용자 정보, 현재 사용자, 접속자 |
| `useradd`, `userdel -r` | 사용자 만들기, 삭제 |
| `useradd --system --no-create-home --shell /sbin/nologin` | 애플리케이션 전용 계정 만들기 |
| `passwd 이름` | 비밀번호 지정 |
| `usermod -aG 그룹 이름` | 그룹에 추가 |
| `gpasswd -d 이름 그룹` | 그룹에서 제거 |
| `su - 이름` | 다른 사용자로 전환 |
| `chmod 750 대상`, `chmod g+r 대상` | 권한 변경 (숫자, 기호) |
| `chown 사용자:그룹 대상` | 소유자 변경 |
| `umask` | 새 파일의 기본 권한 확인 |
| `namei -l 경로` | 경로의 모든 단계의 권한 확인 |
| `sudo -l`, `sudo -u 사용자 명령` | 내 sudo 권한 확인, 다른 사용자로 실행 |
| `visudo` | sudoers 파일 안전하게 수정 |

**링크, 압축, 패키지**

| 명령어 | 용도 |
|---|---|
| `ln -s 원본 링크`, `ln -sfn 원본 링크` | 심볼릭 링크 만들기, 교체 |
| `readlink -f 링크` | 링크의 실제 경로 |
| `tar czf 파일.tar.gz 대상` | 묶고 압축 |
| `tar tzf 파일.tar.gz` | 내용 확인 |
| `tar xzf 파일.tar.gz -C 위치` | 풀기 |
| `zip -r`, `unzip -l`, `unzip -p` | zip 압축, 목록, 내용 출력 |
| `dnf search/info/install/remove` | 패키지 찾기, 정보, 설치, 삭제 |
| `dnf provides "*/bin/명령"` | 명령어가 든 패키지 찾기 |
| `rpm -qa`, `rpm -ql`, `rpm -qf` | 설치된 패키지 조회 |
| `alternatives --config java` | Java 버전 전환 |

**환경 변수와 프로세스**

| 명령어 | 용도 |
|---|---|
| `env`, `echo $이름` | 환경 변수 목록, 값 확인 |
| `export 이름=값` | 환경 변수 지정 |
| `변수=값 명령` | 그 명령에만 변수 적용 |
| `source ~/.bashrc` | 설정 파일 다시 읽기 |
| `alias 이름='명령'` | 명령 별칭 만들기 |
| `ps -ef`, `pgrep -a 이름` | 프로세스 찾기 |
| `top` | 실시간 모니터링 |
| `kill PID`, `kill -9 PID` | 정상 종료 요청, 강제 종료 |
| `명령 &`, `jobs`, `fg`, `bg` | 백그라운드 실행과 작업 관리 |
| `nohup 명령 > 파일 2>&1 &` | 연결이 끊겨도 계속 실행 |

## 3장 체크포인트 제출

다음 세 가지를 제출합니다.

1. `tree -pug /opt/shop-api`와 `ls -ld /var/log/shop-api` 실행 화면
2. `ps -ef | grep [j]ava`(UID가 `appuser`)와 `curl localhost:8080/health`(`1.0.1`, `prod`) 실행 화면
3. 실습 G의 네 문제 각각에 대해 "누가, 무엇에, 무엇이 막혔는가"를 한 줄씩 정리한 텍스트

## 확인 문제

**1.** 파일의 권한이 `-rw-r-----`, 소유자가 `devuser`, 그룹이 `appuser`입니다. `appuser` 그룹에 속하지 않은 `tester` 사용자가 이 파일에 대해 할 수 있는 것은 무엇입니까?

1. 읽기와 쓰기
2. 읽기만
3. 실행만
4. 아무것도 할 수 없음

**2.** `java -jar /opt/shop-api/current.jar`를 실행할 때, jar 파일에 반드시 필요한 권한은 무엇입니까?

1. 읽기(`r`)
2. 쓰기(`w`)
3. 실행(`x`)
4. 읽기와 실행(`r-x`)

**3.** 개발 PC에서 빌드한 jar를 서버에서 실행했더니 `class file version 65.0 ... only recognizes class file versions up to 61.0` 오류가 났습니다. 알맞은 조치는 무엇입니까?

1. jar 파일에 `chmod 755`를 적용합니다
2. 서버에 Java 21을 설치하고 사용하도록 설정합니다
3. 서버에 Java 17을 설치합니다
4. `JAVA_HOME`을 삭제합니다

**4.** 터미널에서 `export SPRING_PROFILES_ACTIVE=prod`를 실행하고 `java -jar app.jar`로 실행하면 prod 프로파일이 적용됩니다. 그런데 같은 내용을 `~/.bashrc`에 적어 두고 systemd 서비스로 실행하면 적용되지 않습니다. 그 이유로 알맞은 것은 무엇입니까?

1. systemd는 Java를 지원하지 않기 때문입니다
2. `.bashrc`는 사용자가 셸을 열 때만 읽히고, 서비스는 셸을 거치지 않기 때문입니다
3. 환경 변수 이름이 잘못되었기 때문입니다
4. `export`는 root만 사용할 수 있기 때문입니다

**5.** 운영 중인 Spring Boot 애플리케이션을 종료하는 가장 알맞은 방법은 무엇입니까?

1. `kill -9 PID`로 즉시 종료합니다
2. `kill PID`로 정상 종료를 요청하고, 종료되지 않을 때만 `kill -9`를 사용합니다
3. VM의 전원을 끕니다
4. SSH 창을 닫습니다

<details>
<summary>정답과 해설</summary>

1. **4번.** `tester`는 소유자도 아니고 그룹 구성원도 아니므로 기타 사용자 권한(`---`)이 적용됩니다.
2. **1번.** 실행되는 프로그램은 `java`이고 jar 파일은 읽기만 합니다. 단, jar까지 가는 경로의 모든 디렉터리에는 `x` 권한이 필요합니다.
3. **2번.** class file version 65.0은 Java 21, 61.0은 Java 17입니다. Java 21로 빌드한 jar를 Java 17에서 실행하려 해서 생긴 오류이므로 서버의 Java를 21로 올려야 합니다.
4. **2번.** `.bashrc`는 대화형 셸이 시작될 때 읽는 파일입니다. systemd 서비스의 환경 변수는 서비스 설정 파일에 따로 지정해야 합니다(4.5절).
5. **2번.** `SIGTERM`을 받은 Spring Boot는 처리 중인 요청을 마치고 자원을 정리한 뒤 종료합니다. `kill -9`는 이 과정을 건너뛰므로 마지막 수단으로만 사용합니다.

</details>

## 다음 장 미리 보기

지금은 애플리케이션을 `sudo -u appuser ... nohup ... &`라는 긴 명령으로 실행하고 있습니다. 서버가 재부팅되면 다시 실행해야 하고, 애플리케이션이 비정상 종료되어도 아무도 알 수 없습니다. 또 Windows 브라우저에서는 아직 접속할 수 없습니다. 4장에서는 네트워크와 방화벽을 설정해 외부에서 접속할 수 있게 하고, 애플리케이션을 **systemd 서비스**로 등록해 자동으로 시작하고 재시작되도록 만들겠습니다. MySQL도 설치해 Spring 애플리케이션과 연결해 봅니다.
