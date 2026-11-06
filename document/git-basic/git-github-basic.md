# Git & GitHub 입문 강좌 (초안)

## 강좌 소개

- **대상**: Git을 처음 사용해보는 학습자 (프로그래밍 기초 지식은 있으나 버전 관리는 처음인 경우 포함)
- **실습 환경**
  - 클라이언트: [git-scm](https://git-scm.com/) (Git 공식 배포판, CLI 기준)
  - 서버(원격 저장소): [GitHub](https://github.com/)
- **강좌 목표**
  - Git과 GitHub의 기본 개념을 이해한다.
  - 로컬에서 저장소를 만들고 변경 사항을 커밋할 수 있다.
  - GitHub에 저장소를 만들고 원격으로 push/pull 할 수 있다.
  - Organization을 만들고 협업자(Collaborator)를 초대할 수 있다.
  - 브랜치를 활용하고, 충돌(conflict)이 발생했을 때 해결할 수 있다.

---

## 커리큘럼 개요

| 챕터 | 주제 | 예상 시간 |
|---|---|---|
| 1 | Git이란 무엇인가 | 15분 |
| 2 | Git 설치 (git-scm) | 15분 |
| 3 | GitHub 계정 생성 | 10분 |
| 4 | Git 초기 설정 | 10분 |
| 5 | 로컬 저장소 생성 및 기본 명령어 (add, commit) | 30분 |
| 6 | GitHub 저장소 생성 및 연결 (push) | 30분 |
| 7 | Organization 생성 | 15분 |
| 8 | Collaborator 초대 | 15분 |
| 9 | Branch 기본 | 30분 |
| 10 | Merge Conflict 해결 | 40분 |
| 11 | 마무리 및 다음 단계 | 10분 |

---

## Chapter 1. Git이란 무엇인가

### 1-1. 버전 관리 시스템(VCS)이란?
- 파일의 변경 이력을 기록하고, 필요할 때 이전 상태로 되돌리거나 비교할 수 있게 해주는 도구
- 여러 사람이 동시에 같은 프로젝트를 작업할 때 충돌 없이 협업할 수 있도록 도와줌

### 1-2. Git의 특징
- **분산형 버전 관리 시스템(DVCS)**: 모든 사용자가 전체 이력을 로컬에 가지고 있음
- 빠른 속도, 브랜치 관리의 용이함
- 오픈소스이며 사실상 업계 표준

### 1-3. Git vs GitHub
- **Git**: 버전 관리 도구 자체 (로컬에서 동작)
- **GitHub**: Git 저장소를 호스팅해주는 온라인 서비스 (원격 저장소 + 협업 기능)

> 실습 Tip: "Git은 도구, GitHub은 그 도구로 만든 결과물을 올려두는 창고"라고 비유하면 이해하기 쉽습니다.

---

## Chapter 2. Git 설치 (git-scm)

### 2-1. 설치 파일 다운로드
1. [https://git-scm.com](https://git-scm.com) 접속
2. 운영체제(Windows / macOS / Linux)에 맞는 설치 파일 다운로드
3. 기본 옵션으로 설치 진행 (입문자는 기본값 권장)

### 2-2. 설치 확인
터미널(또는 Git Bash)에서 아래 명령어 실행:
```bash
git --version
```
버전 정보가 출력되면 설치 성공

### 2-3. 참고: GUI 도구
- 강좌는 CLI(명령어) 중심으로 진행하지만, 필요 시 GitHub Desktop, Sourcetree 같은 GUI 도구도 병행 안내 가능

---

## Chapter 3. GitHub 계정 생성

### 3-1. 회원가입
1. [https://github.com](https://github.com) 접속 후 `Sign up` 클릭
2. 이메일, 비밀번호, 사용자명(username) 입력
3. 이메일 인증 진행

### 3-2. 계정 설정 권장 사항
- 프로필 사진 및 소개 등록
- 2단계 인증(2FA) 설정 (보안 강화, 선택이지만 권장)

### 3-3. SSH Key 등록 (선택, 추후 심화 과정에서 다룰 수 있음)
- HTTPS 방식으로도 실습 가능하므로 입문 단계에서는 생략 가능
- 필요 시 `Settings > SSH and GPG keys`에서 등록

---

## Chapter 4. Git 초기 설정

계정을 만들었다면, 로컬 Git에 사용자 정보를 등록합니다.

```bash
git config --global user.name "본인 이름"
git config --global user.email "본인 이메일"
```

설정 확인:
```bash
git config --list
```

> 이메일은 GitHub 가입 시 사용한 이메일과 동일하게 맞추는 것을 권장 (커밋 기록이 GitHub 프로필과 연동됨)

---

## Chapter 5. 로컬 저장소 생성 및 기본 명령어

### 5-1. 저장소 초기화
```bash
mkdir my-first-project
cd my-first-project
git init
```
- `git init`: 현재 폴더를 Git 저장소로 초기화 (숨김 폴더 `.git` 생성)

### 5-2. 파일 생성 및 상태 확인
```bash
echo "# 나의 첫 프로젝트" > README.md
git status
```
- `git status`: 현재 변경 사항(추적/미추적 파일)을 확인하는 가장 자주 쓰는 명령어

### 5-3. add — 스테이징
```bash
git add README.md
# 전체 파일을 추가하려면
git add .
```
- 작업 디렉토리의 변경 사항을 커밋 대상(스테이징 영역)으로 등록

### 5-4. commit — 기록 남기기
```bash
git commit -m "최초 커밋: README 추가"
```
- 스테이징된 변경 사항을 저장소 이력에 기록
- 커밋 메시지는 변경 내용을 명확히 설명하도록 작성

### 5-5. 커밋 이력 확인
```bash
git log
git log --oneline
```

> 실습: 파일을 수정 → add → commit 을 2~3회 반복해보며 흐름 체득

---

## Chapter 6. GitHub 저장소 생성 및 연결 (push)

### 6-1. GitHub에서 저장소(Repository) 생성
1. GitHub 로그인 후 우측 상단 `+` > `New repository` 클릭
2. Repository name 입력 (예: `my-first-project`)
3. Public/Private 선택
4. README, .gitignore 등은 로컬에 이미 있으므로 **초기화 없이** 생성 (또는 학습 목적에 따라 선택)
5. `Create repository` 클릭

### 6-2. 로컬 저장소와 원격 저장소 연결
```bash
git remote add origin https://github.com/사용자명/my-first-project.git
git remote -v   # 연결 확인
```

### 6-3. push — 원격 저장소에 업로드
```bash
git branch -M main
git push -u origin main
```
- `-u` 옵션: 이후에는 `git push` 만으로도 동일 브랜치에 push 가능하도록 기본 연결 설정

### 6-4. pull — 원격 변경 사항 가져오기
```bash
git pull origin main
```
- 팀원이 올린 변경 사항이나 GitHub 웹에서 직접 수정한 내용을 로컬로 반영할 때 사용

### 6-5. clone — 저장소 복제 (참고)
```bash
git clone https://github.com/사용자명/my-first-project.git
```
- 이미 존재하는 원격 저장소를 로컬로 통째로 가져올 때 사용 (init + remote add를 대신함)

---

## Chapter 7. Organization 생성

여러 명이 함께 프로젝트를 관리할 때는 개인 계정보다 **Organization**을 활용하는 것이 일반적입니다.

### 7-1. Organization 생성 절차
1. GitHub 우측 상단 `+` > `New organization` 클릭
2. 플랜 선택 (무료 플랜인 Free 선택 가능)
3. Organization 이름, 연락 이메일 입력
4. 소유자 확인 후 생성 완료

### 7-2. Organization vs 개인 계정
| 구분 | 개인 계정 | Organization |
|---|---|---|
| 소유자 | 1인 | 여러 관리자 가능 |
| 목적 | 개인 프로젝트 | 팀/회사 단위 협업 |
| 권한 관리 | 단순 | 팀(Team) 단위 세분화 가능 |

### 7-3. Organization 내 저장소 생성
- Organization 페이지에서 `New repository` 선택 시, 해당 Organization 소유의 저장소로 생성됨
- 이후 절차(연결, push 등)는 개인 저장소와 동일

---

## Chapter 8. Collaborator 초대

### 8-1. 개인 저장소에 협업자 초대
1. 저장소 페이지 > `Settings` > `Collaborators` 이동
2. `Add people` 클릭
3. 초대할 사용자의 GitHub 아이디 또는 이메일 입력
4. 초대 전송 → 상대방이 이메일/알림으로 수락하면 접근 권한 부여

### 8-2. Organization에서 팀원 초대
1. Organization 페이지 > `People` 탭 > `Invite member`
2. 사용자 초대 후, 필요 시 `Team`을 만들어 저장소별 권한 세분화 가능
   - 예: `frontend-team`, `backend-team` 별로 접근 가능한 저장소 다르게 설정

### 8-3. 권한 종류 (참고)
- **Read**: 열람만 가능
- **Write**: push 가능
- **Admin**: 저장소 설정 변경까지 가능 (초대, 삭제 등)

> 실습: 스터디원 또는 테스트 계정을 collaborator로 초대해보고, 상대방 계정으로 clone/push 테스트

---

## Chapter 9. Branch 기본

### 9-1. Branch란?
- 원본(main)에 영향을 주지 않고 독립적으로 기능을 개발/실험할 수 있는 작업 공간
- 완료 후 main으로 병합(merge)

### 9-2. 브랜치 생성 및 이동
```bash
git branch feature/login       # 브랜치 생성
git checkout feature/login     # 브랜치 이동
# 위 두 명령을 한 번에:
git checkout -b feature/login
```

### 9-3. 브랜치에서 작업 후 커밋
```bash
# 파일 수정 후
git add .
git commit -m "로그인 기능 추가"
```

### 9-4. 원격에 브랜치 push
```bash
git push -u origin feature/login
```

### 9-5. Pull Request(PR) 생성 (GitHub)
1. GitHub 저장소 페이지에서 `Compare & pull request` 버튼 클릭 (push 직후 자동 안내됨)
2. base(main) ← compare(feature/login) 확인
3. 제목/설명 작성 후 `Create pull request`
4. 리뷰어 지정 및 코드 리뷰 진행 (Organization/팀 프로젝트에서 특히 중요)
5. 승인 후 `Merge pull request`

### 9-6. 로컬에서 직접 merge (참고, PR 없이)
```bash
git checkout main
git merge feature/login
```

---

## Chapter 10. Merge Conflict 해결

### 10-1. Conflict가 발생하는 경우
- 같은 파일의 같은 부분을 서로 다른 브랜치(또는 사람)가 수정한 뒤 merge/pull 할 때 발생

### 10-2. Conflict 상황 재현 실습 (권장 실습 시나리오)
1. main 브랜치에서 특정 파일의 특정 줄 수정 → commit
2. 다른 브랜치(예: feature)에서 **같은 파일의 같은 줄**을 다르게 수정 → commit
3. main으로 이동 후 `git merge feature` 실행 → 충돌 발생 확인

### 10-3. 충돌 마커 이해
충돌이 발생하면 파일에 아래와 같은 마커가 표시됩니다.
```
<<<<<<< HEAD
현재 브랜치(main)의 내용
=======
병합하려는 브랜치(feature)의 내용
>>>>>>> feature
```

### 10-4. 충돌 해결 절차
1. 충돌 파일을 열어 원하는 내용으로 직접 수정 (마커 `<<<<<<<`, `=======`, `>>>>>>>` 모두 삭제)
2. 수정 완료 후 다시 add
   ```bash
   git add 충돌났던파일명
   ```
3. merge 커밋 완료
   ```bash
   git commit
   ```
   (충돌 merge는 보통 커밋 메시지가 자동 생성되며, 그대로 저장하면 됨)

### 10-5. Conflict 예방을 위한 팁
- 작업을 작은 단위로 자주 커밋/push
- 작업 시작 전 `git pull`로 최신 상태 유지
- 팀원과 작업 범위(파일/영역) 겹치지 않게 사전 조율
- GitHub PR 화면에서도 충돌 여부를 미리 확인 가능 (Merge 버튼 비활성화로 표시)

### 10-6. merge 중단하고 싶을 때
```bash
git merge --abort
```
- 충돌 해결이 어려울 때 merge 시도 자체를 취소하고 이전 상태로 복귀

---

## Chapter 11. 마무리 및 다음 단계

### 11-1. 이 강좌에서 다룬 핵심 명령어 정리
| 명령어 | 설명 |
|---|---|
| `git init` | 로컬 저장소 초기화 |
| `git status` | 변경 상태 확인 |
| `git add` | 스테이징 |
| `git commit` | 커밋(이력 기록) |
| `git remote add` | 원격 저장소 연결 |
| `git push` | 원격에 업로드 |
| `git pull` | 원격 변경 사항 가져오기 |
| `git clone` | 원격 저장소 복제 |
| `git branch` | 브랜치 생성/조회 |
| `git checkout -b` | 브랜치 생성+이동 |
| `git merge` | 브랜치 병합 |
| `git merge --abort` | 병합 취소 |

### 11-2. 추가로 학습하면 좋은 주제 (심화 과정 제안)
- `.gitignore` 활용
- `git rebase`
- `git stash`
- Fork 기반 오픈소스 기여 워크플로우
- GitHub Actions (CI/CD 기초)
- SSH Key 기반 인증

### 11-3. 실습 과제 (제안)
1. 개인 저장소를 만들어 README를 3회 이상 수정/커밋/push 해보기
2. Organization을 만들고, 스터디원을 collaborator로 초대해보기
3. 브랜치를 만들어 기능을 추가하고 PR을 생성해 merge 해보기
4. 의도적으로 충돌을 발생시키고 직접 해결해보기

---

*본 문서는 초안(draft)이며, 실습 스크린샷, 발표 슬라이드, 퀴즈/체크리스트 등을 추가해 보강할 예정입니다.*
