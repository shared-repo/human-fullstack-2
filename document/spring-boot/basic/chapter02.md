# 2장. 프로젝트 구조와 자동 설정

---

## 학습 목표

- Spring Boot 프로젝트의 디렉터리 구조와 각 파일의 역할을 설명할 수 있다.
- `build.gradle`의 의존성 관리 방식과 Starter의 구조를 이해한다.
- `@SpringBootApplication`의 내부 동작 원리를 설명할 수 있다.
- Auto Configuration이 빈을 등록하는 과정을 이해한다.
- `application.yml`로 계층적 설정을 관리할 수 있다.

---

## 2.1 `@SpringBootApplication` 내부 동작 이해

1장에서 생성한 프로젝트의 진입점인 `ImageboardApplication.java`를 다시 살펴봅니다.

```java
@SpringBootApplication
public class ImageboardApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImageboardApplication.class, args);
    }
}
```

단 하나의 어노테이션과 한 줄의 코드로 웹 애플리케이션이 실행됩니다. `@SpringBootApplication`이 실제로 어떤 역할을 하는지 내부를 살펴봅니다.

### @SpringBootApplication의 구성

`@SpringBootApplication`은 세 개의 어노테이션을 합쳐놓은 **메타 어노테이션**입니다.

```java
// Spring Boot 소스코드 (단순화)
@SpringBootConfiguration   // ① 설정 클래스 지정
@EnableAutoConfiguration   // ② 자동 설정 활성화
@ComponentScan             // ③ 컴포넌트 스캔
public @interface SpringBootApplication { ... }
```

| 어노테이션 | 역할 |
|---|---|
| `@SpringBootConfiguration` | `@Configuration`을 포함하며, 이 클래스가 Bean 정의 소스임을 나타냄 |
| `@EnableAutoConfiguration` | 클래스패스와 설정을 기반으로 빈을 자동 등록 |
| `@ComponentScan` | 현재 패키지부터 하위 패키지까지 `@Component` 계열 클래스를 스캔 |

### @ComponentScan 범위

`@ComponentScan`은 `ImageboardApplication`이 위치한 패키지(`com.example.imageboard`)와 그 하위 패키지를 자동으로 스캔합니다. 따라서 모든 클래스는 반드시 이 패키지 하위에 위치해야 합니다.

```
com.example.imageboard
├── ImageboardApplication.java   ← 기준 패키지
├── controller/                  ← 스캔 대상 ✅
├── service/                     ← 스캔 대상 ✅
├── repository/                  ← 스캔 대상 ✅
└── entity/                      ← 스캔 대상 ✅

com.example.other/               ← 스캔 대상 아님 ❌
```

> **주의**: 진입점 클래스(`@SpringBootApplication`)를 하위 패키지로 옮기면 상위 패키지의 컴포넌트가 스캔되지 않습니다. 항상 최상위 패키지에 유지하세요.

### SpringApplication.run()의 역할

`SpringApplication.run()`은 다음 과정을 순서대로 실행합니다.

```
1. Spring ApplicationContext 생성
2. @EnableAutoConfiguration 처리 → 자동 설정 클래스 로드
3. @ComponentScan 실행 → 컴포넌트 빈 등록
4. 내장 Tomcat 서버 시작
5. DispatcherServlet 등록
6. 애플리케이션 실행 완료
```

---

## 2.2 Auto Configuration 동작 원리

Spring Boot의 가장 핵심적인 기능인 Auto Configuration이 어떻게 동작하는지 이해합니다.

### 동작 원리

`@EnableAutoConfiguration`이 활성화되면 Spring Boot는 다음 파일을 읽어 자동 설정 클래스 목록을 가져옵니다.

```
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

이 파일은 `spring-boot-autoconfigure` JAR 안에 포함되어 있으며, 수백 개의 자동 설정 클래스가 나열되어 있습니다.

```
# AutoConfiguration.imports 내용 일부
org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration
org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration
org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
...
```

### @ConditionalOn* — 조건부 빈 등록

각 자동 설정 클래스는 `@ConditionalOn*` 어노테이션으로 **조건이 충족될 때만** 빈을 등록합니다. 예를 들어 Thymeleaf 자동 설정을 살펴봅니다.

```java
// ThymeleafAutoConfiguration (Spring Boot 소스코드, 단순화)
@AutoConfiguration
@ConditionalOnClass(TemplateMode.class)          // ① Thymeleaf 라이브러리가 클래스패스에 있을 때
@ConditionalOnMissingBean(SpringTemplateEngine.class) // ② 개발자가 직접 등록하지 않았을 때
public class ThymeleafAutoConfiguration {

    @Bean
    public SpringTemplateEngine templateEngine() {
        return new SpringTemplateEngine();
    }
}
```

| 어노테이션 | 조건 |
|---|---|
| `@ConditionalOnClass` | 특정 클래스가 클래스패스에 존재할 때 |
| `@ConditionalOnMissingBean` | 해당 타입의 빈이 아직 등록되지 않았을 때 |
| `@ConditionalOnProperty` | `application.yml`의 특정 프로퍼티 값이 일치할 때 |
| `@ConditionalOnWebApplication` | 웹 애플리케이션 환경일 때 |

### Auto Configuration 오버라이드

`@ConditionalOnMissingBean` 덕분에 개발자가 직접 빈을 등록하면 자동 설정은 해당 빈을 건너뜁니다. 즉, **자동 설정은 항상 개발자 설정보다 우선순위가 낮습니다.**

```java
// 개발자가 직접 Thymeleaf 엔진을 커스터마이즈하면 Auto Configuration은 적용되지 않음
@Configuration
public class ThymeleafConfig {

    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setEnableSpringELCompiler(true); // 커스텀 설정
        return engine;
    }
}
```

### 적용된 Auto Configuration 확인

어떤 자동 설정이 적용되었는지 확인하려면 `application.yml`에 다음을 추가합니다.

```yaml
logging:
  level:
    org.springframework.boot.autoconfigure: DEBUG
```

애플리케이션 실행 시 콘솔에 **Positive matches** (적용된 설정)와 **Negative matches** (조건 미충족으로 제외된 설정)가 출력됩니다.

```
============================
CONDITIONS EVALUATION REPORT
============================

Positive matches:
-----------------
   ThymeleafAutoConfiguration matched:
      - @ConditionalOnClass found required class 'TemplateMode' (OnClassCondition)

Negative matches:
-----------------
   ActiveMQAutoConfiguration:
      - @ConditionalOnClass did not find required class 'ActiveMQConnectionFactory'
```

---

## 2.3 `build.gradle` 의존성 관리와 Starter 구조

### Gradle 기본 구조

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.x.x'
    id 'io.spring.dependency-management' version '1.x.x'
}
```

두 플러그인의 역할을 구분합니다.

| 플러그인 | 역할 |
|---|---|
| `org.springframework.boot` | `bootRun`, `bootJar` 태스크 제공. 실행 가능한 JAR 패키징 |
| `io.spring.dependency-management` | `spring-boot-dependencies` BOM을 가져와 의존성 버전을 자동 관리 |

### BOM(Bill of Materials)과 버전 관리

`io.spring.dependency-management` 플러그인은 내부적으로 `spring-boot-dependencies` BOM을 임포트합니다. BOM에는 Spring Boot와 호환되는 수백 개 라이브러리의 검증된 버전이 명시되어 있습니다.

```groovy
// 버전을 직접 명시하지 않아도 됨 — BOM이 관리
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'    // 버전 없음
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa' // 버전 없음
    runtimeOnly 'org.mariadb.jdbc:mariadb-java-client'                   // 버전 없음
}
```

특정 라이브러리의 버전을 BOM 기본값과 다르게 쓰고 싶다면 명시적으로 지정할 수 있습니다.

```groovy
dependencies {
    // 버전을 명시하면 BOM 기본값을 덮어씀
    implementation 'com.querydsl:querydsl-jpa:5.1.0'
}
```

### Starter의 구조

Starter는 특정 기능에 필요한 의존성 묶음입니다. `spring-boot-starter-web`의 내부를 살펴봅니다.

```
spring-boot-starter-web
├── spring-boot-starter              ← 공통 기반 (logging, auto-configure 등)
│   ├── spring-boot
│   ├── spring-boot-autoconfigure
│   └── spring-boot-starter-logging  ← Logback
├── spring-boot-starter-json         ← Jackson (JSON 직렬화)
├── spring-boot-starter-tomcat       ← 내장 Tomcat
└── spring-webmvc                    ← Spring MVC
```

개발자가 `spring-boot-starter-web` 하나만 추가하면 이 모든 라이브러리가 함께 추가됩니다.

### 의존성 범위(Configuration)

```groovy
dependencies {
    implementation '...'        // 컴파일 + 런타임 모두 사용
    runtimeOnly '...'           // 런타임에만 필요 (MariaDB 드라이버 등)
    compileOnly '...'           // 컴파일 시에만 필요, JAR에 포함 안 됨 (Lombok)
    annotationProcessor '...'   // 어노테이션 처리기 (Lombok)
    testImplementation '...'    // 테스트에서만 사용
}
```

```groovy
// 우리 프로젝트의 의존성 범위 적용 예시
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    runtimeOnly 'org.mariadb.jdbc:mariadb-java-client'   // 런타임에만 필요

    compileOnly 'org.projectlombok:lombok'               // JAR에 포함 안 됨
    annotationProcessor 'org.projectlombok:lombok'       // 컴파일 시 코드 생성

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

### 의존성 트리 확인

추가된 의존성과 전이 의존성 전체를 확인하려면 터미널에서 다음 명령을 실행합니다.

```bash
./gradlew dependencies --configuration compileClasspath
```

---

## 2.4 `application.yml` 계층적 설정 관리

### properties vs yml

Spring Boot는 `application.properties`와 `application.yml` 두 형식을 모두 지원합니다. `yml`은 계층 구조를 들여쓰기로 표현하여 중복을 줄이고 가독성을 높입니다.

```properties
# application.properties — 반복적인 키 구조
spring.datasource.url=jdbc:mariadb://localhost:3306/imageboard
spring.datasource.username=boarduser
spring.datasource.password=board1234
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

```yaml
# application.yml — 계층 구조로 중복 제거
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/imageboard
    username: boarduser
    password: board1234
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

> **들여쓰기 주의**: yml은 탭이 아닌 **스페이스**로 들여쓰기해야 합니다. IntelliJ IDEA는 yml 파일에서 탭을 자동으로 스페이스로 변환합니다.

### 이미지 게시판 전체 설정

이 과정에서 사용할 `application.yml` 전체 설정입니다.

```yaml
spring:
  application:
    name: imageboard

  # 데이터소스 설정
  datasource:
    url: jdbc:mariadb://localhost:3306/imageboard?characterEncoding=UTF-8&serverTimezone=Asia/Seoul
    username: boarduser
    password: board1234
    driver-class-name: org.mariadb.jdbc.Driver

  # JPA 설정
  jpa:
    hibernate:
      ddl-auto: update        # 개발: update | 운영: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  # Thymeleaf 설정
  thymeleaf:
    cache: false              # 개발 중 템플릿 캐시 비활성화

# 서버 설정
server:
  port: 8080

# 파일 업로드 설정 (5장 이미지 업로드에서 사용)
spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 10MB     # 파일 하나의 최대 크기
      max-request-size: 30MB  # 요청 전체의 최대 크기

# 로깅 설정
logging:
  level:
    com.example.imageboard: DEBUG   # 우리 패키지는 DEBUG 레벨
    org.hibernate.SQL: DEBUG        # 실행 SQL 출력
    org.hibernate.type: TRACE       # 바인딩 파라미터 출력
```

### 프로파일 분리

개발 환경과 운영 환경의 설정을 분리하려면 프로파일별 설정 파일을 만듭니다.

```
src/main/resources/
├── application.yml           ← 공통 설정
├── application-dev.yml       ← 개발 환경 설정
└── application-prod.yml      ← 운영 환경 설정
```

```yaml
# application.yml — 공통 설정 및 기본 프로파일 지정
spring:
  profiles:
    active: dev               # 기본 활성 프로파일
  application:
    name: imageboard
```

```yaml
# application-dev.yml — 개발 환경
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/imageboard
    username: boarduser
    password: board1234
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  thymeleaf:
    cache: false

logging:
  level:
    com.example.imageboard: DEBUG
```

```yaml
# application-prod.yml — 운영 환경
spring:
  datasource:
    url: jdbc:mariadb://prod-db-server:3306/imageboard
    username: ${DB_USERNAME}      # 환경변수에서 읽어옴
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate          # 운영에서는 스키마 변경 금지
    show-sql: false
  thymeleaf:
    cache: true

logging:
  level:
    com.example.imageboard: INFO
```

프로파일 활성화 방법은 여러 가지입니다.

```bash
# 방법 1: 애플리케이션 실행 시 JVM 옵션
java -jar imageboard.jar -Dspring.profiles.active=prod

# 방법 2: Gradle 실행 시
./gradlew bootRun --args='--spring.profiles.active=prod'

# 방법 3: 환경변수
export SPRING_PROFILES_ACTIVE=prod
```

### @Value와 @ConfigurationProperties

`application.yml` 값을 Java 코드에서 읽는 두 가지 방법입니다.

**@Value — 단일 값 주입**

```yaml
# application.yml
file:
  upload-dir: /uploads/images
```

```java
@Service
public class FileService {

    @Value("${file.upload-dir}")
    private String uploadDir;
}
```

**@ConfigurationProperties — 관련 설정을 묶어서 주입 (권장)**

관련 설정이 여러 개일 때 클래스로 묶어 관리하면 더 명확합니다.

```yaml
# application.yml
file:
  upload-dir: /uploads/images
  max-size: 10485760       # 10MB (bytes)
  allowed-extensions:
    - jpg
    - jpeg
    - png
    - gif
```

```java
@ConfigurationProperties(prefix = "file")
@Component
public class FileProperties {

    private String uploadDir;
    private long maxSize;
    private List<String> allowedExtensions;

    // Getter, Setter (또는 Lombok @Data)
}
```

```java
// 주입해서 사용
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileProperties fileProperties;

    public void upload(MultipartFile file) {
        String dir = fileProperties.getUploadDir();
        // ...
    }
}
```

`@ConfigurationProperties`를 활성화하려면 `build.gradle`에 의존성을 추가합니다.

```groovy
dependencies {
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
}
```

이렇게 하면 IDE에서 `application.yml` 작성 시 자동완성도 지원됩니다.

---

## 2.5 프로젝트 패키지 구조 설계

이미지 게시판을 구현하기 위한 패키지 구조를 미리 설계합니다. 앞으로 모든 실습은 이 구조를 기반으로 진행됩니다.

```
src/main/java/com/example/imageboard/
│
├── ImageboardApplication.java          ← 진입점
│
├── controller/                         ← 웹 요청 처리
│   ├── BoardController.java
│   └── MemberController.java
│
├── service/                            ← 비즈니스 로직
│   ├── BoardService.java
│   └── MemberService.java
│
├── repository/                         ← 데이터 접근
│   ├── BoardRepository.java
│   └── MemberRepository.java
│
├── entity/                             ← JPA 엔티티
│   ├── Board.java
│   ├── Member.java
│   └── AttachedImage.java
│
├── dto/                                ← 데이터 전달 객체
│   ├── BoardCreateRequest.java
│   ├── BoardUpdateRequest.java
│   └── BoardResponse.java
│
└── config/                             ← 설정 클래스
    ├── FileProperties.java
    └── SecurityConfig.java             ← (6장에서 추가)

src/main/resources/
├── application.yml
├── application-dev.yml
├── application-prod.yml
├── static/
│   ├── css/
│   └── js/
└── templates/
    ├── layout/
    │   └── default.html                ← 공통 레이아웃
    ├── board/
    │   ├── list.html
    │   ├── detail.html
    │   ├── create.html
    │   └── edit.html
    └── member/
        ├── login.html
        └── register.html
```

지금 바로 이 패키지 구조를 IntelliJ IDEA에서 만들어 두면 이후 실습이 더 원활하게 진행됩니다.

---

## 정리

| 핵심 개념 | 내용 |
|---|---|
| `@SpringBootApplication` | `@SpringBootConfiguration` + `@EnableAutoConfiguration` + `@ComponentScan`의 합성 어노테이션 |
| Auto Configuration | 클래스패스 탐색 후 `@ConditionalOn*` 조건을 만족하는 빈을 자동 등록 |
| `@ConditionalOnMissingBean` | 개발자가 직접 빈을 등록하면 자동 설정이 그 빈을 건너뜀 |
| Starter | 기능별 의존성 묶음. BOM이 검증된 버전을 자동 관리 |
| 프로파일 | `application-{profile}.yml`로 환경별 설정을 분리 |
| `@ConfigurationProperties` | 관련 설정을 클래스로 묶어 타입 안전하게 주입 |

---

## 연습 문제

1. `build.gradle`에서 `spring-boot-starter-thymeleaf`를 제거하고 애플리케이션을 실행하면 Auto Configuration 리포트에 어떤 변화가 생기는지 확인해 보세요. (`logging.level.org.springframework.boot.autoconfigure: DEBUG` 활용)
2. `application.yml`에 임의의 커스텀 프로퍼티를 추가하고 `@Value`와 `@ConfigurationProperties` 방식으로 각각 주입해 보세요.
3. `application-dev.yml`을 만들고 `server.port`를 `9090`으로 설정한 뒤, `dev` 프로파일로 실행해 보세요.

---

## 다음 장 예고

3장에서는 Spring MVC 웹 계층을 구현합니다. Controller를 작성하고, Thymeleaf 템플릿으로 게시판 목록·상세 화면을 만듭니다.
