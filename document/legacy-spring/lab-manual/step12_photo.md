# 단계 12. 맛집 사진 첨부 기능 구현

> **목표** : 맛집 등록 시 사진을 여러 장 첨부하고, 목록에서 대표 사진 썸네일을 표시하며, 상세 페이지에서 모든 사진을 확인합니다.
> **소요 시간** : 약 2시간

---

## 12.1 전체 흐름 이해

### 등록 흐름

```
브라우저 (write.jsp — 파일 선택 후 폼 제출)
  ↓ POST /restaurant/write  (multipart/form-data)
RestaurantController.write()
  ↓ ① 사진 파일을 서버 디스크에 저장
  ↓ ② restaurantService.register(restaurant, photoList) 호출
RestaurantServiceImpl.register()
  ↓ restaurantMapper.insert(restaurant)          → restaurant 테이블 저장
  ↓ restaurantPhotoMapper.insert(photo) × N개   → restaurant_photo 테이블 저장
RestaurantController
  ↓ 목록 페이지로 리다이렉트
```

### 목록 흐름 (썸네일)

```
RestaurantController.list()
  ↓ restaurantService.getList(condition)
RestaurantMapper.selectAll()
  ↓ restaurant 테이블 조회 + 서브쿼리로 첫 번째 사진 파일명 포함
list.jsp
  ↓ thumbnailUrl 이 있으면 <img> 태그로 표시
```

### 상세 흐름 (전체 사진)

```
RestaurantController.detail()
  ↓ restaurantService.getPhotos(no) 호출
RestaurantPhotoMapper.selectByRestaurantNo()
  ↓ restaurant_photo 테이블에서 해당 맛집의 사진 전체 조회
detail.jsp
  ↓ photos 목록을 갤러리 형태로 출력
```

---

## 12.2 작성 순서

```
① DB — restaurant_photo 테이블 생성
② webapp/resources/uploads/ 폴더 생성
③ web.xml — Multipart 설정 추가
④ spring-mvc.xml — MultipartResolver 빈 등록
⑤ mybatis-config.xml — RestaurantPhoto typeAlias 추가
⑥ RestaurantPhoto.java — 도메인 클래스 작성
⑦ Restaurant.java — thumbnailUrl 필드 추가
⑧ RestaurantPhotoMapper.java — 인터페이스 작성
⑨ RestaurantPhotoMapper.xml — SQL 작성
⑩ RestaurantMapper.xml — selectAll 수정 (썸네일 서브쿼리 추가)
⑪ RestaurantService.java — register 시그니처 수정, getPhotos 추가
⑫ RestaurantServiceImpl.java — 구현 수정
⑬ RestaurantController.java — write, detail 수정
⑭ write.jsp — 파일 첨부 폼 추가
⑮ list.jsp — 썸네일 열 추가
⑯ detail.jsp — 사진 갤러리 추가
```

---

## 12.3 DB — restaurant_photo 테이블 생성

MariaDB에 접속하여 아래 SQL을 실행합니다.

```sql
CREATE TABLE restaurant_photo (
    no            INT          AUTO_INCREMENT PRIMARY KEY,
    restaurant_no INT          NOT NULL COMMENT '맛집 번호 (FK)',
    file_name     VARCHAR(255) NOT NULL COMMENT '서버 저장 파일명 (UUID)',
    ori_name      VARCHAR(255) NOT NULL COMMENT '원본 파일명',
    reg_date      DATETIME     DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_photo_restaurant
        FOREIGN KEY (restaurant_no)
        REFERENCES restaurant(no)
        ON DELETE CASCADE   -- 맛집 삭제 시 사진 레코드도 자동 삭제
);
```

> **`ON DELETE CASCADE` 의 역할**
>
> 맛집을 삭제하면 `restaurant_photo` 테이블의 관련 레코드도 자동으로 삭제됩니다.  
> 단, **서버 디스크의 실제 파일은 자동으로 삭제되지 않습니다.** (향후 확장 사항)

테이블 생성 확인:

```sql
DESCRIBE restaurant_photo;
```

---

## 12.4 업로드 폴더 생성

서버에 저장될 사진 파일의 위치를 미리 만들어 둡니다.

```
src/main/webapp/resources/
└── uploads/          ← 이 폴더를 새로 만듭니다
```

**폴더 만드는 방법**

1. `src/main/webapp/resources` 를 오른쪽 클릭합니다.
2. **New → Folder** 를 선택합니다.
3. 폴더 이름에 `uploads` 를 입력하고 **Finish** 를 클릭합니다.

> `uploads` 폴더 안에 `.gitkeep` 이라는 빈 파일을 하나 만들어 두면 Git에서 빈 폴더를 추적할 수 있습니다.

---

## 12.5 web.xml 수정 — Multipart 설정 추가

파일 업로드를 처리하려면 `DispatcherServlet` 에 `<multipart-config>` 를 추가해야 합니다.  
`web.xml` 에서 `<servlet>` 블록을 찾아 아래와 같이 수정합니다.

**수정 전**

```xml
<servlet>
    <servlet-name>dispatcher</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
    <init-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>/WEB-INF/spring/spring-mvc.xml</param-value>
    </init-param>
    <load-on-startup>1</load-on-startup>
</servlet>
```

**수정 후**

```xml
<servlet>
    <servlet-name>dispatcher</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
    <init-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>/WEB-INF/spring/spring-mvc.xml</param-value>
    </init-param>
    <load-on-startup>1</load-on-startup>

    <!-- ↓ 파일 업로드 설정 추가 -->
    <multipart-config>
        <!-- 파일 1개의 최대 크기: 5 MB -->
        <max-file-size>5242880</max-file-size>
        <!-- 요청 전체의 최대 크기: 20 MB -->
        <max-request-size>20971520</max-request-size>
        <!-- 항상 디스크에 즉시 기록 -->
        <file-size-threshold>0</file-size-threshold>
    </multipart-config>
</servlet>
```

> **크기 단위**
>
> `5242880` = 5 × 1024 × 1024 = 5 MB  
> `20971520` = 20 × 1024 × 1024 = 20 MB  
> 필요에 따라 조정합니다.

---

## 12.6 spring-mvc.xml 수정 — MultipartResolver 빈 등록

`spring-mvc.xml` 의 `</beans>` 태그 바로 앞에 아래 내용을 추가합니다.

```xml
<!-- 파일 업로드 처리 (Jakarta Servlet 표준 방식) -->
<bean id="multipartResolver"
      class="org.springframework.web.multipart.support.StandardServletMultipartResolver"/>
```

---

## 12.7 mybatis-config.xml 수정 — typeAlias 추가

`mybatis-config.xml` 의 `<typeAliases>` 블록에 `RestaurantPhoto` 를 추가합니다.

```xml
<typeAliases>
    <typeAlias type="com.food.domain.Member"          alias="Member"/>
    <typeAlias type="com.food.domain.Restaurant"      alias="Restaurant"/>
    <typeAlias type="com.food.domain.SearchCondition" alias="SearchCondition"/>
    <typeAlias type="com.food.domain.RestaurantPhoto" alias="RestaurantPhoto"/>  <!-- ← 추가 -->
</typeAliases>
```

---

## 12.8 RestaurantPhoto 도메인 클래스 작성

`com.food.domain` 패키지 안에 `RestaurantPhoto.java` 파일을 만듭니다.

```java
package com.food.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RestaurantPhoto {

    private int           no;
    private int           restaurantNo;  // restaurant_photo.restaurant_no
    private String        fileName;      // 서버에 저장된 파일명 (UUID 기반)
    private String        oriName;       // 사용자가 업로드한 원본 파일명
    private LocalDateTime regDate;

}
```

---

## 12.9 Restaurant 도메인 수정 — thumbnailUrl 필드 추가

`Restaurant.java` 에 대표 사진 URL 을 담을 필드를 추가합니다.  
이 필드는 DB 컬럼이 아니라 목록 조회 시 서브쿼리 결과로 채워집니다.

```java
package com.food.domain;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Restaurant {

    private int           no;
    private int           memberId;
    private String        name;
    private String        category;
    private String        address;
    private String        memo;
    private LocalDate     visitDate;
    private LocalDateTime regDate;

    // ↓ 추가 — DB 컬럼 없음, 목록 조회 시 서브쿼리로 채워짐
    private String thumbnailUrl;

}
```

---

## 12.10 RestaurantPhotoMapper 인터페이스 작성

`com.food.mapper` 패키지 안에 `RestaurantPhotoMapper.java` 파일을 만듭니다.

```java
package com.food.mapper;

import com.food.domain.RestaurantPhoto;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface RestaurantPhotoMapper {

    /**
     * 사진 정보를 DB에 저장
     */
    void insert(RestaurantPhoto photo);

    /**
     * 특정 맛집의 사진 전체 조회 (등록 순)
     */
    List<RestaurantPhoto> selectByRestaurantNo(@Param("restaurantNo") int restaurantNo);

}
```

---

## 12.11 RestaurantPhotoMapper.xml 작성

`src/main/resources/mappers/` 폴더 안에 `RestaurantPhotoMapper.xml` 파일을 만듭니다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
    PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.food.mapper.RestaurantPhotoMapper">

    <!-- 사진 정보 저장 -->
    <insert id="insert" useGeneratedKeys="true" keyProperty="no">
        INSERT INTO restaurant_photo (restaurant_no, file_name, ori_name)
        VALUES (#{restaurantNo}, #{fileName}, #{oriName})
    </insert>

    <!-- 특정 맛집의 사진 전체 조회 -->
    <select id="selectByRestaurantNo" resultType="RestaurantPhoto">
        SELECT no, restaurant_no, file_name, ori_name, reg_date
          FROM restaurant_photo
         WHERE restaurant_no = #{restaurantNo}
         ORDER BY no ASC
    </select>

</mapper>
```

---

## 12.12 RestaurantMapper.xml 수정 — selectAll 에 썸네일 서브쿼리 추가

`RestaurantMapper.xml` 의 `selectAll` SQL 을 아래와 같이 수정합니다.  
서브쿼리로 각 맛집의 첫 번째 사진 파일명을 `thumbnail_url` 로 가져옵니다.  
MyBatis 의 `mapUnderscoreToCamelCase` 설정에 의해 `thumbnail_url` → `thumbnailUrl` 로 자동 변환됩니다.

**수정 전**

```xml
<select id="selectAll" resultType="Restaurant">
    SELECT no, member_id, name, category, address, memo, visit_date, reg_date
      FROM restaurant
    <where>
        member_id = #{memberId}
        <if test="category != null and category != ''">
            AND category = #{category}
        </if>
        <if test="keyword != null and keyword != ''">
            AND name LIKE CONCAT('%', #{keyword}, '%')
        </if>
    </where>
    ORDER BY no DESC
</select>
```

**수정 후**

```xml
<select id="selectAll" resultType="Restaurant">
    SELECT r.no, r.member_id, r.name, r.category,
           r.address, r.memo, r.visit_date, r.reg_date,
           (SELECT file_name
              FROM restaurant_photo
             WHERE restaurant_no = r.no
             ORDER BY no ASC
             LIMIT 1) AS thumbnail_url
      FROM restaurant r
    <where>
        r.member_id = #{memberId}
        <if test="category != null and category != ''">
            AND r.category = #{category}
        </if>
        <if test="keyword != null and keyword != ''">
            AND r.name LIKE CONCAT('%', #{keyword}, '%')
        </if>
    </where>
    ORDER BY r.no DESC
</select>
```

> **서브쿼리 설명**
>
> `(SELECT file_name FROM restaurant_photo WHERE restaurant_no = r.no ORDER BY no ASC LIMIT 1) AS thumbnail_url`
>
> 각 맛집(`r.no`)에 등록된 사진 중 가장 먼저 등록된 사진(`ORDER BY no ASC`)의 파일명 1개를 가져옵니다.  
> 사진이 없는 맛집은 `thumbnail_url` 이 `null` 로 반환됩니다.

---

## 12.13 RestaurantService 인터페이스 수정

`RestaurantService.java` 에서 `register` 메서드 시그니처를 수정하고 `getPhotos` 를 추가합니다.

> **이 파일은 단계 10 까지 수정된 상태를 기준으로 합니다.**  
> `getList(SearchCondition condition)` 은 단계 10 에서 이미 변경된 시그니처입니다.  
> 이번 단계에서는 `register` 시그니처 수정과 `getPhotos` 추가만 신규 변경 사항입니다.

```java
package com.food.service;

import com.food.domain.Restaurant;
import com.food.domain.RestaurantPhoto;
import com.food.domain.SearchCondition;
import java.util.List;

public interface RestaurantService {

    List<Restaurant> getList(SearchCondition condition);  // 단계 10 에서 변경됨

    // ↓ 수정 — photos 파라미터 추가
    /**
     * 맛집 등록 (사진 포함)
     * @param restaurant 맛집 정보
     * @param photos     저장할 사진 목록 (없으면 빈 리스트)
     */
    void register(Restaurant restaurant, List<RestaurantPhoto> photos);

    Restaurant getDetail(int no);

    void modify(Restaurant restaurant);

    void remove(int no);

    // ↓ 추가
    /**
     * 특정 맛집의 사진 전체 조회
     */
    List<RestaurantPhoto> getPhotos(int restaurantNo);

}
```

---

## 12.14 RestaurantServiceImpl 수정

`RestaurantServiceImpl.java` 를 아래와 같이 수정합니다.

```java
package com.food.service.impl;

import com.food.domain.Restaurant;
import com.food.domain.RestaurantPhoto;
import com.food.domain.SearchCondition;
import com.food.mapper.RestaurantMapper;
import com.food.mapper.RestaurantPhotoMapper;
import com.food.service.RestaurantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class RestaurantServiceImpl implements RestaurantService {

    @Autowired
    private RestaurantMapper restaurantMapper;

    @Autowired
    private RestaurantPhotoMapper restaurantPhotoMapper;   // ← 추가

    @Override
    @Transactional(readOnly = true)
    public List<Restaurant> getList(SearchCondition condition) {
        return restaurantMapper.selectAll(condition);
    }

    // ↓ 수정 — photos 파라미터 추가, 사진 DB 저장 포함
    @Override
    @Transactional
    public void register(Restaurant restaurant, List<RestaurantPhoto> photos) {
        // ① 맛집 저장 (auto-generated no 를 restaurant.no 에 채움)
        restaurantMapper.insert(restaurant);

        // ② 사진 저장 (restaurantNo 를 방금 생성된 no 로 설정)
        for (RestaurantPhoto photo : photos) {
            photo.setRestaurantNo(restaurant.getNo());
            restaurantPhotoMapper.insert(photo);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Restaurant getDetail(int no) {
        return restaurantMapper.selectOne(no);
    }

    @Override
    @Transactional
    public void modify(Restaurant restaurant) {
        restaurantMapper.update(restaurant);
    }

    @Override
    @Transactional
    public void remove(int no) {
        restaurantMapper.delete(no);
        // restaurant_photo 는 ON DELETE CASCADE 로 자동 삭제됨
    }

    // ↓ 추가
    @Override
    @Transactional(readOnly = true)
    public List<RestaurantPhoto> getPhotos(int restaurantNo) {
        return restaurantPhotoMapper.selectByRestaurantNo(restaurantNo);
    }

}
```

> **register 트랜잭션 처리**
>
> `@Transactional` 로 묶여 있으므로 맛집 저장과 사진 저장이 하나의 트랜잭션으로 실행됩니다.  
> 사진 저장 중 오류가 발생하면 맛집 저장도 자동으로 롤백됩니다.

---

## 12.15 RestaurantController 수정

`RestaurantController.java` 의 `write()` 메서드와 `detail()` 메서드를 수정합니다.  
전체 파일 내용은 다음과 같습니다.

```java
package com.food.controller;

import com.food.domain.Member;
import com.food.domain.Restaurant;
import com.food.domain.RestaurantPhoto;
import com.food.domain.SearchCondition;
import com.food.service.RestaurantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/restaurant")
public class RestaurantController {

    @Autowired
    private RestaurantService restaurantService;

    /* ── 목록 ──────────────────────────────────── */
    @GetMapping("/list")
    public String list(SearchCondition condition, HttpSession session, Model model) {
        Member loginMember = (Member) session.getAttribute("loginMember");
        condition.setMemberId(loginMember.getId());
        model.addAttribute("list", restaurantService.getList(condition));
        model.addAttribute("loginMember", loginMember);
        model.addAttribute("condition", condition);
        return "restaurant/list";
    }

    /* ── 등록 폼 ────────────────────────────────── */
    @GetMapping("/write")
    public String writeForm() {
        return "restaurant/write";
    }

    /* ── 등록 처리 (사진 첨부 포함) ──────────────── */
    @PostMapping("/write")
    public String write(Restaurant restaurant,
                        @RequestParam(value = "photos", required = false)
                            List<MultipartFile> photos,
                        HttpSession session,
                        HttpServletRequest request) throws IOException {

        Member loginMember = (Member) session.getAttribute("loginMember");
        restaurant.setMemberId(loginMember.getId());

        // ① 사진 파일을 서버 디스크에 저장하고 RestaurantPhoto 목록 생성
        List<RestaurantPhoto> photoList = saveUploadedFiles(photos, request);

        // ② 맛집 + 사진 정보를 DB에 저장 (하나의 트랜잭션)
        restaurantService.register(restaurant, photoList);

        return "redirect:/restaurant/list";
    }

    /* ── 상세 조회 (사진 목록 포함) ──────────────── */
    @GetMapping("/detail")
    public String detail(@RequestParam int no, HttpSession session, Model model) {
        Restaurant restaurant        = restaurantService.getDetail(no);
        List<RestaurantPhoto> photos = restaurantService.getPhotos(no);  // ← 추가
        Member loginMember           = (Member) session.getAttribute("loginMember");

        model.addAttribute("restaurant", restaurant);
        model.addAttribute("photos", photos);              // ← 추가
        model.addAttribute("loginMember", loginMember);
        return "restaurant/detail";
    }

    /* ── 수정 폼 ────────────────────────────────── */
    @GetMapping("/edit")
    public String editForm(@RequestParam int no, HttpSession session, Model model) {
        Restaurant restaurant = restaurantService.getDetail(no);
        Member loginMember    = (Member) session.getAttribute("loginMember");
        if (restaurant.getMemberId() != loginMember.getId()) {
            return "redirect:/restaurant/list";
        }
        model.addAttribute("restaurant", restaurant);
        return "restaurant/edit";
    }

    /* ── 수정 처리 ──────────────────────────────── */
    @PostMapping("/edit")
    public String edit(Restaurant restaurant, HttpSession session) {
        Member loginMember = (Member) session.getAttribute("loginMember");
        Restaurant saved   = restaurantService.getDetail(restaurant.getNo());
        if (saved.getMemberId() != loginMember.getId()) {
            return "redirect:/restaurant/list";
        }
        restaurantService.modify(restaurant);
        return "redirect:/restaurant/detail?no=" + restaurant.getNo();
    }

    /* ── 삭제 처리 ──────────────────────────────── */
    @PostMapping("/delete")
    public String delete(@RequestParam int no, HttpSession session) {
        Member loginMember = (Member) session.getAttribute("loginMember");
        Restaurant saved   = restaurantService.getDetail(no);
        if (saved.getMemberId() != loginMember.getId()) {
            return "redirect:/restaurant/list";
        }
        restaurantService.remove(no);
        return "redirect:/restaurant/list";
    }

    /* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
       private 헬퍼 메서드 — 파일 저장
    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
    private List<RestaurantPhoto> saveUploadedFiles(
            List<MultipartFile> files, HttpServletRequest request) throws IOException {

        List<RestaurantPhoto> result = new ArrayList<>();
        if (files == null || files.isEmpty()) return result;

        // 업로드 폴더 절대 경로 구하기
        String uploadDir = request.getServletContext()
                                  .getRealPath("/resources/uploads/");
        new File(uploadDir).mkdirs();  // 폴더가 없으면 자동 생성

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;  // 선택하지 않은 파일 입력창은 건너뜀

            String oriName  = file.getOriginalFilename();
            String ext      = oriName.substring(oriName.lastIndexOf('.'));
            String fileName = UUID.randomUUID().toString() + ext;  // 중복 방지

            // 디스크에 저장
            file.transferTo(new File(uploadDir + fileName));

            // RestaurantPhoto 객체 생성 (restaurantNo 는 서비스에서 채움)
            RestaurantPhoto photo = new RestaurantPhoto();
            photo.setFileName(fileName);
            photo.setOriName(oriName);
            result.add(photo);
        }
        return result;
    }

}
```

> **`getRealPath()` 의 역할**
>
> `request.getServletContext().getRealPath("/resources/uploads/")` 는  
> 웹 애플리케이션의 `/resources/uploads/` 경로에 해당하는 **서버의 실제 파일 시스템 경로**를 반환합니다.

---

## 12.16 write.jsp 수정 — 파일 첨부 폼 추가

`WEB-INF/views/restaurant/write.jsp` 를 아래와 같이 수정합니다.  
폼에 `enctype="multipart/form-data"` 를 반드시 추가해야 파일이 전송됩니다.

**수정 전 (폼 태그)**

```jsp
<form method="post" action="/restaurant/write">
```

**수정 후 (폼 태그)**

```jsp
<form method="post" action="/restaurant/write" enctype="multipart/form-data">
```

사진 입력 행을 테이블 마지막 데이터 행 뒤, 버튼 행 앞에 추가합니다.

**추가할 행**

```jsp
<tr>
    <td>사진</td>
    <td>
        <input type="file" name="photos" accept="image/*" multiple>
        <br>
        <small>JPG, PNG, GIF 형식 / 파일당 최대 5 MB / 여러 장 선택 가능</small>
    </td>
</tr>
```

**write.jsp 전체 코드**

```jsp
<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>맛집 등록 — 나만의 맛집 노트</title>
</head>
<body>

<h2>맛집 등록</h2>

<!-- enctype="multipart/form-data" 반드시 필요 -->
<form method="post" action="/restaurant/write" enctype="multipart/form-data">

    <table>
        <tr>
            <td>가게 이름 *</td>
            <td><input type="text" name="name" required style="width: 300px;"></td>
        </tr>
        <tr>
            <td>카테고리 *</td>
            <td>
                <select name="category" required>
                    <option value="">-- 선택 --</option>
                    <option value="한식">한식</option>
                    <option value="중식">중식</option>
                    <option value="일식">일식</option>
                    <option value="양식">양식</option>
                    <option value="분식">분식</option>
                    <option value="카페">카페</option>
                    <option value="기타">기타</option>
                </select>
            </td>
        </tr>
        <tr>
            <td>주소 *</td>
            <td><input type="text" name="address" required style="width: 300px;"></td>
        </tr>
        <tr>
            <td>방문일 *</td>
            <td><input type="date" name="visitDate" required></td>
        </tr>
        <tr>
            <td>메모</td>
            <td><textarea name="memo" rows="4" cols="40"></textarea></td>
        </tr>
        <!-- ↓ 사진 첨부 행 추가 -->
        <tr>
            <td>사진</td>
            <td>
                <input type="file" name="photos" accept="image/*" multiple>
                <br>
                <small>JPG, PNG, GIF 형식 / 파일당 최대 5 MB / 여러 장 선택 가능</small>
            </td>
        </tr>
        <tr>
            <td colspan="2">
                <button type="submit">등록하기</button>
                <a href="/restaurant/list">취소</a>
            </td>
        </tr>
    </table>

</form>

</body>
</html>
```

> **`multiple` 속성**
>
> `<input type="file" multiple>` 을 사용하면 파일 선택 창에서 Ctrl(또는 Cmd)을 누른 채 여러 파일을 한 번에 선택할 수 있습니다.

---

## 12.17 list.jsp 수정 — 썸네일 열 추가

`WEB-INF/views/restaurant/list.jsp` 를 아래 코드로 교체합니다.

> **변경 사항 요약**
>
> - **사진 열 추가** — 테이블 첫 번째 열로 썸네일 이미지 열을 추가합니다.
> - **번호 열 제거** — 기존 "번호" 열(`${r.no}`)을 삭제합니다. 사진 열을 추가해도 컬럼 수가 늘어나지 않도록 조정한 것입니다.
> - 그 외 검색 폼, 네비게이션, 버튼 구조는 단계 10 의 코드를 그대로 유지합니다.

```jsp
<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>맛집 목록 — 나만의 맛집 노트</title>
</head>
<body>

<!-- 상단 네비게이션 -->
<div>
    <strong>${loginMember.name}</strong>님 환영합니다.
    <a href="/restaurant/write">맛집 등록</a>
    <a href="/member/logout">로그아웃</a>
</div>

<hr>

<!-- 검색 폼 -->
<form method="get" action="/restaurant/list">
    <select name="category">
        <option value="">전체 카테고리</option>
        <option value="한식" ${condition.category == '한식' ? 'selected' : ''}>한식</option>
        <option value="중식" ${condition.category == '중식' ? 'selected' : ''}>중식</option>
        <option value="일식" ${condition.category == '일식' ? 'selected' : ''}>일식</option>
        <option value="양식" ${condition.category == '양식' ? 'selected' : ''}>양식</option>
        <option value="분식" ${condition.category == '분식' ? 'selected' : ''}>분식</option>
        <option value="카페" ${condition.category == '카페' ? 'selected' : ''}>카페</option>
        <option value="기타" ${condition.category == '기타' ? 'selected' : ''}>기타</option>
    </select>

    <input type="text" name="keyword"
           value="${condition.keyword}"
           placeholder="가게 이름으로 검색">

    <button type="submit">검색</button>
    <a href="/restaurant/list">전체 보기</a>
</form>

<h2>나의 맛집 목록</h2>

<!-- 목록이 없을 때 -->
<c:if test="${empty list}">
    <p>검색 결과가 없습니다.</p>
</c:if>

<!-- 목록이 있을 때 -->
<c:if test="${not empty list}">
    <table border="1">
        <thead>
            <tr>
                <th>사진</th>  <!-- ← 번호 열을 사진 열로 교체 -->
                <th>가게 이름</th>
                <th>카테고리</th>
                <th>방문일</th>
                <th>등록일</th>
                <th>관리</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="r" items="${list}">
            <tr>
                <!-- ↓ 썸네일 열 -->
                <td style="text-align: center;">
                    <c:if test="${not empty r.thumbnailUrl}">
                        <img src="/resources/uploads/${r.thumbnailUrl}"
                             alt="사진"
                             style="width: 60px; height: 60px; object-fit: cover;">
                    </c:if>
                    <c:if test="${empty r.thumbnailUrl}">
                        <span style="color: #bbb;">📷</span>
                    </c:if>
                </td>
                <td><a href="/restaurant/detail?no=${r.no}">${r.name}</a></td>
                <td>${r.category}</td>
                <td>${r.visitDate}</td>
                <td><fmt:formatDate value="${r.regDate}" pattern="yyyy-MM-dd"/></td>
                <td>
                    <a href="/restaurant/edit?no=${r.no}">수정</a>
                    <form method="post" action="/restaurant/delete"
                          style="display: inline;"
                          onsubmit="return confirm('삭제하시겠습니까?')">
                        <input type="hidden" name="no" value="${r.no}">
                        <button type="submit">삭제</button>
                    </form>
                </td>
            </tr>
            </c:forEach>
        </tbody>
    </table>
</c:if>

</body>
</html>
```

---

## 12.18 detail.jsp 수정 — 사진 갤러리 추가

`WEB-INF/views/restaurant/detail.jsp` 에 사진 갤러리 영역을 추가합니다.

```jsp
<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>${restaurant.name} — 나만의 맛집 노트</title>
</head>
<body>

<h2>맛집 상세</h2>

<table border="1">
    <tr>
        <td>가게 이름</td>
        <td>${restaurant.name}</td>
    </tr>
    <tr>
        <td>카테고리</td>
        <td>${restaurant.category}</td>
    </tr>
    <tr>
        <td>주소</td>
        <td>${restaurant.address}</td>
    </tr>
    <tr>
        <td>방문일</td>
        <td>${restaurant.visitDate}</td>
    </tr>
    <tr>
        <td>메모</td>
        <td>${restaurant.memo}</td>
    </tr>
    <tr>
        <td>등록일</td>
        <td>${restaurant.regDate}</td>
    </tr>
</table>

<!-- ↓ 사진 갤러리 (추가된 부분) -->
<h3>사진</h3>

<c:if test="${not empty photos}">
    <div style="display: flex; flex-wrap: wrap; gap: 10px; margin-top: 8px;">
        <c:forEach var="p" items="${photos}">
            <div>
                <img src="/resources/uploads/${p.fileName}"
                     alt="${p.oriName}"
                     style="width: 150px; height: 150px; object-fit: cover; cursor: pointer;"
                     onclick="openPhoto(this.src)">
                <p style="font-size: 0.8em; color: #999; margin: 2px 0;">${p.oriName}</p>
            </div>
        </c:forEach>
    </div>
</c:if>
<c:if test="${empty photos}">
    <p style="color: #999;">등록된 사진이 없습니다.</p>
</c:if>

<br>

<!-- 본인이 등록한 맛집일 때만 수정·삭제 버튼 표시 -->
<c:if test="${restaurant.memberId == loginMember.id}">
    <a href="/restaurant/edit?no=${restaurant.no}">수정</a>
    &nbsp;
    <form method="post" action="/restaurant/delete"
          style="display: inline;"
          onsubmit="return confirm('삭제하시겠습니까?')">
        <input type="hidden" name="no" value="${restaurant.no}">
        <button type="submit">삭제</button>
    </form>
</c:if>

<br><br>
<a href="/restaurant/list">목록으로</a>

<!-- 사진 원본 보기 (lightbox 간이 구현) -->
<div id="lightbox"
     style="display: none; position: fixed; top: 0; left: 0;
            width: 100%; height: 100%; background: rgba(0,0,0,0.85);
            z-index: 9999; align-items: center; justify-content: center;
            cursor: zoom-out;"
     onclick="this.style.display='none'">
    <img id="lightbox-img" src="" alt="원본 사진"
         style="max-width: 90%; max-height: 90%;">
</div>

<script>
function openPhoto(src) {
    var lb = document.getElementById('lightbox');
    document.getElementById('lightbox-img').src = src;
    lb.style.display = 'flex';
}
</script>

</body>
</html>
```

---

## 12.19 동작 확인

### 12.19.1 서버 재시작

서버를 재시작하고 콘솔에 오류가 없는지 확인합니다.

### 12.19.2 사진 첨부 등록 테스트

1. 로그인 후 **맛집 등록** 페이지로 이동합니다.
2. 모든 필수 항목을 입력합니다.
3. **사진** 입력창에서 이미지 파일을 1~3장 선택합니다.
4. **등록하기** 버튼을 클릭합니다.
5. 목록 페이지로 이동한 후, 방금 등록한 맛집 행에 썸네일이 보이면 성공입니다.

### 12.19.3 DB 확인

```sql
SELECT * FROM restaurant_photo;
```

등록한 사진 수만큼 레코드가 있으면 성공입니다.

### 12.19.4 파일 확인

Eclipse의 Project Explorer에서 `src/main/webapp/resources/uploads/` 폴더를 확인합니다.  
UUID 형식의 파일명(`예: 3f2a1b0c-...-.jpg`)으로 파일이 저장되어 있으면 성공입니다.

> 파일이 보이지 않으면 프로젝트를 오른쪽 클릭 → **Refresh** 를 선택합니다.

### 12.19.5 상세 페이지 사진 확인

목록에서 해당 맛집 이름을 클릭합니다.  
상세 페이지에 등록한 사진이 표시되면 성공입니다.  
사진 클릭 시 원본 크기로 볼 수 있으면 lightbox도 정상입니다.

### 12.19.6 사진 없이 등록 테스트

사진을 선택하지 않고 맛집을 등록합니다.  
정상적으로 목록에 추가되고 썸네일 자리에 📷 아이콘이 표시되면 성공입니다.

---

## 자주 발생하는 오류

| 오류 | 원인 | 해결 방법 |
|---|---|---|
| `java.lang.IllegalStateException: No multipart config` | `web.xml` 에 `<multipart-config>` 가 없음 | `web.xml` 의 `<servlet>` 블록 안에 `<multipart-config>` 를 추가합니다 |
| `MultipartException: Current request is not a multipart request` | 폼에 `enctype="multipart/form-data"` 가 없음 | `write.jsp` 의 `<form>` 태그에 `enctype` 을 추가합니다 |
| 사진 파일이 저장되지 않음 | `uploads/` 폴더가 없거나 쓰기 권한 없음 | Eclipse에서 `uploads` 폴더 생성 여부를 확인합니다. `new File(uploadDir).mkdirs()` 코드가 있는지도 확인합니다 |
| 목록에 썸네일이 표시되지 않음 | `Restaurant.thumbnailUrl` 필드 누락 | `Restaurant.java` 에 `thumbnailUrl` 필드가 있는지 확인합니다 |
| 썸네일 이미지가 깨짐 (404) | URL 경로 오류 | `spring-mvc.xml` 의 `<mvc:default-servlet-handler/>` 가 있는지 확인합니다 |
| 사진이 DB에 저장됐지만 맛집이 없음 | 트랜잭션 롤백 실패 | `@Transactional` 이 `register` 메서드에 있는지 확인합니다 |
| `getPhotos` 호출 시 NullPointerException | `RestaurantPhotoMapper` 주입 실패 | `RestaurantServiceImpl` 에 `@Autowired RestaurantPhotoMapper` 가 있는지 확인합니다 |

---

## ✅ 단계 12 완료 체크리스트

- [ ] `restaurant_photo` 테이블이 생성됐습니다.
- [ ] `src/main/webapp/resources/uploads/` 폴더가 생성됐습니다.
- [ ] `web.xml` 의 `<servlet>` 블록에 `<multipart-config>` 가 추가됐습니다.
- [ ] `spring-mvc.xml` 에 `StandardServletMultipartResolver` 빈이 등록됐습니다.
- [ ] `mybatis-config.xml` 에 `RestaurantPhoto` typeAlias 가 추가됐습니다.
- [ ] `RestaurantPhoto.java` 도메인 클래스가 작성됐습니다.
- [ ] `Restaurant.java` 에 `thumbnailUrl` 필드가 추가됐습니다.
- [ ] `RestaurantPhotoMapper.java` 인터페이스가 작성됐습니다.
- [ ] `RestaurantPhotoMapper.xml` 이 `src/main/resources/mappers/` 에 작성됐습니다.
- [ ] `RestaurantMapper.xml` 의 `selectAll` SQL 에 썸네일 서브쿼리가 추가됐습니다.
- [ ] `RestaurantService.java` 의 `register` 시그니처가 수정됐고 `getPhotos` 가 추가됐습니다.
- [ ] `RestaurantServiceImpl.java` 에 `RestaurantPhotoMapper` 가 주입됐고 구현이 수정됐습니다.
- [ ] `RestaurantController.java` 의 `write()` 와 `detail()` 메서드가 수정됐습니다.
- [ ] `write.jsp` 에 `enctype="multipart/form-data"` 와 파일 입력창이 추가됐습니다.
- [ ] `list.jsp` 에 썸네일 열이 추가됐습니다 (번호 열 제거).
- [ ] `detail.jsp` 에 사진 갤러리와 lightbox 가 추가됐습니다.
- [ ] 사진을 첨부하여 맛집을 등록하면 목록에 썸네일이 표시됩니다.
- [ ] 상세 페이지에서 등록한 모든 사진이 표시됩니다.
- [ ] 사진 없이 등록해도 정상적으로 동작합니다.

모든 항목이 체크됐으면 **단계 13** 으로 이동합니다 (또는 프로젝트를 완성합니다).
