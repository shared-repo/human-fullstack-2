# 1장. Spring Boot 개요 및 환경 설정

---

## 학습 목표

- Spring Framework와 Spring Boot의 차이를 설명할 수 있다.
- Spring Boot 4.x의 주요 특징을 이해한다.
- Java 21 및 개발 도구를 설치하고 개발 환경을 구성할 수 있다.
- Spring Initializr를 이용해 Gradle 기반 Spring Boot 프로젝트를 생성할 수 있다.

---

## 1.1 Spring Framework vs Spring Boot

### Spring Framework의 한계

Spring Framework는 엔터프라이즈 애플리케이션 개발에 필요한 강력한 기능을 제공하지만, 프로젝트를 시작하기 위해 개발자가 직접 처리해야 할 설정이 매우 많았습니다.

**기존 Spring Framework 프로젝트의 어려움**

- `web.xml`, `applicationContext.xml`, `dispatcher-servlet.xml` 등 수많은 XML 설정 파일 작성
- 라이브러리 간 버전 호환성을 직접 확인하고 관리
- WAS(Tomcat 등)를 별도로 설치하고 WAR 파일을 배포하는 복잡한 배포 과정
- 프로젝트마다 반복되는 동일한 설정 코드 작성

```xml
<!-- 기존 Spring MVC — web.xml 설정 예시 -->
<servlet>
    <servlet-name>dispatcher</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
    <init-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>/WEB-INF/spring/dispatcher-servlet.xml</param-value>
    </init-param>
    <load-on-startup>1</load-on-startup>
</servlet>
<servlet-mapping>
    <servlet-name>dispatcher</servlet-name>
    <url-pattern>/</url-pattern>
</servlet-mapping>
```

### Spring Boot가 해결한 것

Spring Boot는 "**설정보다 관례(Convention over Configuration)**" 원칙을 적용하여 위 문제들을 해결합니다.

| 구분 | Spring Framework | Spring Boot |
|---|---|---|
| 프로젝트 설정 | XML 또는 Java Config 직접 작성 | Auto Configuration으로 자동 처리 |
| 의존성 관리 | 버전 호환성을 개발자가 직접 확인 | Starter가 검증된 버전 세트를 제공 |
| 서버 실행 | 외부 WAS 설치 후 WAR 배포 | 내장 Tomcat으로 `main()` 실행 |
| 빌드 결과물 | WAR 파일 | 실행 가능한 JAR 파일 |
| 프로퍼티 관리 | 직접 PropertySource 설정 | `application.yml` 자동 로드 |

```java
// Spring Boot — 이것만으로 웹 애플리케이션이 실행된다
@SpringBootApplication
public class BoardApplication {
    public static void main(String[] args) {
        SpringApplication.run(BoardApplication.class, args);
    }
}
```

> **핵심 정리**: Spring Boot는 Spring Framework를 대체하는 것이 아니라, Spring Framework 위에서 **반복적인 설정 작업을 자동화**해주는 도구입니다. 기존에 배운 Spring MVC, AOP, 트랜잭션 등 모든 지식은 Spring Boot에서도 그대로 사용됩니다.

---

## 1.2 Spring Boot 4.x 주요 특징

이 교육과정은 **Spring Boot 4.x** (Spring Framework 7.x 기반)을 사용합니다.

### Java 21 기본 지원

Spring Boot 4.x는 Java 21을 기본 베이스라인으로 채택합니다. Java 21의 주요 기능이 Spring과 깊게 통합됩니다.

**Virtual Thread (가상 스레드) 통합**

Java 21에서 정식 출시된 Project Loom의 Virtual Thread를 Spring Boot가 공식 지원합니다. 기존의 플랫폼 스레드(OS 스레드)보다 훨씬 가볍게 동작하여, 대량의 동시 요청을 효율적으로 처리할 수 있습니다.

```yaml
# application.yml — 한 줄로 Virtual Thread 활성화
spring:
  threads:
    virtual:
      enabled: true
```

**Record, Sealed Class 활용**

Java 21의 Record와 Sealed Class가 Spring 생태계 전반에서 자연스럽게 활용됩니다.

```java
// DTO를 Record로 간결하게 표현
public record BoardCreateRequest(
    @NotBlank String title,
    @NotBlank String content
) {}
```

### Jakarta EE 11 기반

Spring Framework 7.x는 Jakarta EE 11을 기반으로 합니다. 기존 `javax.*` 패키지가 `jakarta.*` 패키지로 완전히 전환되었습니다. Spring Boot 3.x를 경험한 분이라면 이미 익숙한 변경사항입니다.

```java
// javax → jakarta 패키지 변경
import jakarta.persistence.Entity;       // 구: javax.persistence.Entity
import jakarta.validation.Valid;         // 구: javax.validation.Valid
import jakarta.servlet.http.HttpServlet; // 구: javax.servlet.http.HttpServlet
```

### 향상된 Auto Configuration

Auto Configuration 메커니즘이 더욱 정교해졌습니다. `@ConditionalOn*` 어노테이션 기반의 조건부 빈 등록이 개선되어, 개발자가 설정을 오버라이드하기 더 쉬워졌습니다.

### GraalVM Native Image 지원 강화

애플리케이션을 네이티브 실행 파일로 컴파일하는 GraalVM Native Image 지원이 더욱 성숙해졌습니다. 빠른 시작 시간과 낮은 메모리 사용량이 필요한 환경(컨테이너, 서버리스)에 유리합니다. (본 과정에서는 다루지 않습니다.)

---

## 1.3 개발 환경 구성

### 필수 설치 목록

| 도구 | 버전 | 역할 |
|---|---|---|
| JDK | 21 (LTS) | Java 런타임 및 컴파일러 |
| IntelliJ IDEA | 2025.3 이상 (무료) | 통합 개발 환경 |
| MariaDB | 11.x | 데이터베이스 서버 |
| Git | 최신 버전 | 소스 코드 버전 관리 |

> Gradle은 별도 설치가 불필요합니다. Spring Boot 프로젝트에 **Gradle Wrapper**가 포함되어 있어 `./gradlew` 명령으로 자동 다운로드 및 실행됩니다.

### JDK 21 설치

**Windows / macOS — SDKMAN 사용 (권장)**

```bash
# SDKMAN 설치
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# JDK 21 설치 (Temurin 배포판)
sdk install java 21.0.3-tem

# 설치 확인
java -version
# openjdk version "21.0.3" ...
```

**macOS — Homebrew 사용**

```bash
brew install --cask temurin@21
```

**Windows — 직접 설치**

1. [Adoptium 공식 사이트](https://adoptium.net)에서 Temurin JDK 21 다운로드
2. 설치 후 시스템 환경변수 `JAVA_HOME`을 JDK 설치 경로로 설정
3. `PATH`에 `%JAVA_HOME%\bin` 추가

### MariaDB 설치

**macOS**

```bash
brew install mariadb
brew services start mariadb
```

**Windows**

1. [MariaDB 공식 사이트](https://mariadb.org/download/)에서 설치 파일 다운로드
2. 설치 시 root 비밀번호 설정

**초기 데이터베이스 생성**

```sql
-- MariaDB 접속 후 실행
CREATE DATABASE imageboard CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'boarduser'@'localhost' IDENTIFIED BY 'board1234';
GRANT ALL PRIVILEGES ON imageboard.* TO 'boarduser'@'localhost';
FLUSH PRIVILEGES;
```

### IntelliJ IDEA 설정

**권장 플러그인**

- **Lombok** — 보일러플레이트 코드 자동 생성

> JetBrains는 2025.3부터 Community / Ultimate 구분을 없애고 **단일 IntelliJ IDEA**로 통합했습니다. Spring Boot 프로젝트 마법사, Spring·Thymeleaf 문법 하이라이팅이 무료로 제공되므로 별도 플러그인 없이 실습이 가능합니다.

**Annotation Processing 활성화 (Lombok 필수 설정)**

`Settings → Build, Execution, Deployment → Compiler → Annotation Processors`
→ **Enable annotation processing** 체크

---

## 1.4 Spring Initializr로 프로젝트 생성

### Spring Initializr 접속

웹 브라우저에서 [https://start.spring.io](https://start.spring.io)에 접속하거나, IDE에서 `File → New → Project → Spring Boot`를 선택하면 Spring Initializr와 연동된 프로젝트 마법사를 바로 사용할 수 있습니다.

### 프로젝트 설정

아래 값으로 설정합니다.

| 항목 | 값 |
|---|---|
| Project | **Gradle - Groovy** |
| Language | Java |
| Spring Boot | **4.x.x** (최신 정식 버전 선택) |
| Group | `com.example` |
| Artifact | `imageboard` |
| Name | `imageboard` |
| Packaging | Jar |
| Java | **21** |

### 의존성 추가

**ADD DEPENDENCIES** 버튼을 클릭하여 다음 의존성을 추가합니다.

| 의존성 | 역할 |
|---|---|
| Spring Web | Spring MVC, 내장 Tomcat |
| Thymeleaf | 서버 사이드 템플릿 엔진 |
| Spring Data JPA | JPA / Hibernate 연동 |
| MariaDB Driver | MariaDB JDBC 드라이버 |
| Lombok | 어노테이션 기반 코드 생성 |
| Validation | Bean Validation (jakarta.validation) |

설정 완료 후 **GENERATE** 버튼을 클릭하여 ZIP 파일을 다운로드합니다.

### 프로젝트 구조 살펴보기

압축 해제 후 IntelliJ IDEA로 열면 다음과 같은 구조가 생성됩니다.

```
imageboard/
├── build.gradle                  ← 의존성 및 빌드 설정
├── settings.gradle               ← 프로젝트 이름 설정
├── gradlew                       ← Gradle Wrapper 실행 스크립트 (Mac/Linux)
├── gradlew.bat                   ← Gradle Wrapper 실행 스크립트 (Windows)
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties  ← Gradle 버전 명시
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/example/imageboard/
    │   │       └── ImageboardApplication.java   ← 진입점
    │   └── resources/
    │       ├── application.properties           ← 설정 파일 (yml로 변경 예정)
    │       ├── static/                          ← CSS, JS, 이미지 등 정적 리소스
    │       └── templates/                       ← Thymeleaf 템플릿
    └── test/
        └── java/
            └── com/example/imageboard/
                └── ImageboardApplicationTests.java
```

### build.gradle 살펴보기

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.x.x'      // Spring Boot 플러그인
    id 'io.spring.dependency-management' version '1.x.x' // 의존성 버전 관리
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)    // Java 21 명시
    }
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'          // Spring MVC
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'    // Thymeleaf
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'     // Spring Data JPA
    implementation 'org.springframework.boot:spring-boot-starter-validation'   // Bean Validation
    runtimeOnly 'org.mariadb.jdbc:mariadb-java-client'                         // MariaDB 드라이버
    compileOnly 'org.projectlombok:lombok'                                      // Lombok
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

> **Starter란?** `spring-boot-starter-web`처럼 `starter`가 붙은 의존성은 특정 기능에 필요한 라이브러리 묶음입니다. 예를 들어 `starter-web`은 Spring MVC, Jackson, 내장 Tomcat을 포함합니다. 개발자는 버전을 직접 명시하지 않아도 되며, `spring-boot-dependencies`가 검증된 버전 조합을 자동으로 관리합니다.

### application.yml 설정

`src/main/resources/application.properties` 파일을 삭제하고 같은 위치에 `application.yml` 파일을 생성합니다.

```yaml
spring:
  application:
    name: imageboard

  # MariaDB 데이터소스 설정
  datasource:
    url: jdbc:mariadb://localhost:3306/imageboard?characterEncoding=UTF-8&serverTimezone=Asia/Seoul
    username: boarduser
    password: board1234
    driver-class-name: org.mariadb.jdbc.Driver

  # JPA / Hibernate 설정
  jpa:
    hibernate:
      ddl-auto: update          # 개발 환경: update / 운영 환경: validate
    show-sql: true              # 실행되는 SQL 콘솔 출력
    properties:
      hibernate:
        format_sql: true        # SQL 가독성 있게 포맷

  # Thymeleaf 설정 (개발 중 캐시 비활성화)
  thymeleaf:
    cache: false

# 서버 포트 (기본값 8080)
server:
  port: 8080
```

### 첫 실행 확인

`ImageboardApplication.java`를 열고 `main()` 메서드 옆의 ▶ 버튼을 클릭하거나, 터미널에서 다음 명령을 실행합니다.

```bash
./gradlew bootRun
```

콘솔에 다음과 같은 메시지가 출력되면 정상 실행된 것입니다.

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v4.x.x)

...
Started ImageboardApplication in 2.345 seconds (process running for 2.6)
```

브라우저에서 `http://localhost:8080`에 접속하면 **Whitelabel Error Page**가 표시됩니다. 아직 컨트롤러를 만들지 않았기 때문에 정상입니다. 서버가 실행 중이라는 확인이 됩니다.

---

## 정리

| 핵심 개념 | 내용 |
|---|---|
| Spring Boot | Spring Framework 위에서 자동 설정을 제공하는 도구 |
| Auto Configuration | 클래스패스에 있는 라이브러리를 감지하여 빈을 자동으로 등록 |
| Starter | 기능별로 묶인 의존성 세트, 버전 호환성을 보장 |
| Gradle Wrapper | 팀원 간 동일한 Gradle 버전을 보장하는 스크립트 |
| application.yml | 계층적 구조로 설정을 관리하는 Spring Boot 설정 파일 |

---

## 연습 문제

1. `build.gradle`에서 `spring-boot-starter-web`을 제거하고 애플리케이션을 실행하면 어떤 변화가 생기는지 확인해 보세요.
2. `application.yml`에서 `server.port`를 `9090`으로 변경하고, 브라우저로 접속해 보세요.
3. `spring.jpa.show-sql`을 `false`로 설정하면 어떤 변화가 생기는지 확인해 보세요.

---

## 다음 장 예고

2장에서는 Spring Boot의 핵심 동작 원리인 **Auto Configuration**을 더 깊이 살펴보고, `application.yml`을 이용한 계층적 설정 관리, 그리고 Gradle 빌드 구성을 자세히 다룹니다.
