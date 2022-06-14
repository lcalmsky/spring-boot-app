![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: f39b053)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout f39b053
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

스터디 검색 기능을 구현합니다.

키워드를 입력 받아 스터디를 검색하는데, 스터디 제목, 관심사, 도시 이름이 키워드에 해당합니다.

로그인하지 않고도 사용할 수 있어야 합니다.

검색으로 보여질 내용은 아래와 같습니다.

* 검색 키워드, 결과 개수
* 스터디 정보
  * 이름
  * 짧은 소개
  * 태그
  * 지역
  * 멤버 수
  * 스터디 공개 일시

## 엔드포인트 추가

`MainController`에 검색 엔드포인트를 추가합니다.

`/src/main/java/io/lcalmsky/app/modules/main/endpoint/controller/MainController.java`

```java
// 생략
@Controller
@RequiredArgsConstructor
public class MainController {
    private final StudyRepository studyRepository;
    // 생략
    @GetMapping("/search/study")
    public String searchStudy(String keyword, Model model) {
        List<Study> studyList = studyRepository.findByKeyword(keyword);
        model.addAttribute(studyList);
        model.addAttribute("keyword", keyword);
        return "search";
    }
}
```

`StudyRepository`에 전달받은 `keyword`로 조회한 결과를 모델에 담아 `search` 페이지로 이동시킵니다.

<details>
<summary>MainController.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.main.endpoint.controller;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.support.CurrentUser;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.infra.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final StudyRepository studyRepository;

    @GetMapping("/")
    public String home(@CurrentUser Account account, Model model) {
        if (account != null) {
            model.addAttribute(account);
        }
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/search/study")
    public String searchStudy(String keyword, Model model) {
        List<Study> studyList = studyRepository.findByKeyword(keyword);
        model.addAttribute(studyList);
        model.addAttribute("keyword", keyword);
        return "search";
    }
}

```

</details>

## Repository 추가 및 수정

키워드로 검색하는 기능은 `querydsl`을 이용해 구현해보겠습니다.

`study` 도메인 패키지 내에 `StudyRepositoryExtension` 인터페이스를 추가합니다.

`/src/main/java/io/lcalmsky/app/modules/study/infra/repository/StudyRepositoryExtension.java`

```java
package io.lcalmsky.app.modules.study.infra.repository;

import io.lcalmsky.app.modules.study.domain.entity.Study;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
public interface StudyRepositoryExtension {
    List<Study> findByKeyword(String keyword);
}
```

`MainController`에서 검색을 위해 호출하는 메서드를 인터페이스에 추가하였습니다.

다음으로 `StudyRepository`가 `StudyRepositoryExtension`을 상속하게하면 `MainController`에서 `StudyRepository.findByKeyword`를 호출하는 부분의 컴파일 에러가 사라지게 됩니다.

`/src/main/java/io/lcalmsky/app/modules/study/infra/repository/StudyRepository.java`

```java
// 생략
@Transactional(readOnly = true)
public interface StudyRepository extends JpaRepository<Study, Long>, StudyRepositoryExtension {
    // 생략
}
```

마지막으로 이 내용을 구현할 클래스를 생성합니다.

기존에 생성한 인터페이스에 `Impl`을 추가해서 생성해야만 스프링에서 자동으로 구현체를 찾아줍니다.

> 이 부분에 대해 자세히 알고싶으신 분들은 [이 포스팅](https://jaime-note.tistory.com/79?category=994945)을 참고하시면 됩니다.

`/src/main/java/io/lcalmsky/app/modules/study/infra/repository/StudyRepositoryExtensionImpl.java`

```java
package io.lcalmsky.app.modules.study.infra.repository;

import com.querydsl.jpa.JPQLQuery;
import io.lcalmsky.app.modules.study.domain.entity.QStudy;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;

public class StudyRepositoryExtensionImpl extends QuerydslRepositorySupport implements StudyRepositoryExtension {

    public StudyRepositoryExtensionImpl() {
        super(Study.class);
    }

    @Override
    public List<Study> findByKeyword(String keyword) {
        QStudy study = QStudy.study;
        JPQLQuery<Study> query = from(study)
                .where(study.published.isTrue()
                        .and(study.title.containsIgnoreCase(keyword))
                        .or(study.tags.any().title.containsIgnoreCase(keyword))
                        .or(study.zones.any().localNameOfCity.containsIgnoreCase(keyword)));
        return query.fetch() ;
    }
}

```

`QuerydslRepositorySupport`를 상속하여 `Entity` 타입만 부모 객체에 전달하여 기본 설정을 완료하였습니다. (자세한 내용은 [여기](https://jaime-note.tistory.com/80?category=994945) 참고)

`querydsl`을 이용해 쿼리를 생성한 뒤 조회하도록 하였습니다.

이 부분이 나중에 문제가 되는데요, 뷰를 구현한 이후에 확인해보도록 하겠습니다.

## SecurityConfig 수정

검색 기능은 로그인하지 않아도 쓸 수 있게 하기 위해 `SecurityConfig`를 수정합니다.

```java
// 생략
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private final UserDetailsService userDetailsService;
    private final DataSource dataSource;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .mvcMatchers("/", "/login", "/sign-up", "/check-email-token",
                        "/email-login", "/check-email-login", "/login-link", "/login-by-email", 
                        "/search/study").permitAll() // /search/study 추가
                .mvcMatchers(HttpMethod.GET, "/profile/*").permitAll()
                .anyRequest().authenticated();
        http.formLogin()
                .loginPage("/login")
                .permitAll();
        http.logout()
                .logoutSuccessUrl("/");
        http.rememberMe()
                .userDetailsService(userDetailsService)
                .tokenRepository(tokenRepository());
    }
    // 생략
}
```

<details>
<summary>SecurityConfig.java 전체 보기</summary>

```java
package io.lcalmsky.app.infra.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private final UserDetailsService userDetailsService;
    private final DataSource dataSource;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .mvcMatchers("/", "/login", "/sign-up", "/check-email-token",
                        "/email-login", "/check-email-login", "/login-link", "/login-by-email", "/search/study").permitAll()
                .mvcMatchers(HttpMethod.GET, "/profile/*").permitAll()
                .anyRequest().authenticated();
        http.formLogin()
                .loginPage("/login")
                .permitAll();
        http.logout()
                .logoutSuccessUrl("/");
        http.rememberMe()
                .userDetailsService(userDetailsService)
                .tokenRepository(tokenRepository());
    }

    @Bean
    public PersistentTokenRepository tokenRepository() {
        JdbcTokenRepositoryImpl jdbcTokenRepository = new JdbcTokenRepositoryImpl();
        jdbcTokenRepository.setDataSource(dataSource);
        return jdbcTokenRepository;
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                .mvcMatchers("/node_modules/**");
    }
}

```

</details>

## 뷰 작성

`search.html` 파일을 생성하고 아래 코드를 작성합니다.

`/src/main/resources/templates/search.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html::head"></head>
<body class="bg-light">
    <div th:replace="fragments.html::navigation-bar"></div>
    <div class="container">
        <div class="py-5 text-center">
            <p class="lead" th:if="${#lists.isEmpty(studyList)}">
                <strong th:text="${keyword}" id="keyword" class="context"></strong>에 해당하는 스터디가 없습니다.
            </p>
            <p class="lead" th:if="${!#lists.isEmpty(studyList)}">
                <strong th:text="${keyword}" id="keyword" class="context"></strong>에 해당하는 스터디를
                <span th:text="${studyList.size()}"></span>개 찾았습니다.
            </p>
        </div>
        <div class="row justify-content-center">
            <div class="col-sm-10">
                <div class="row">
                    <div class="col-md-4" th:each="study: ${studyList}">
                        <div class="card mb-4 shadow-sm">
                            <div class="card-body">
                                <a th:href="@{'/study/' + ${study.path}}" class="text-decoration-none">
                                    <h5 class="card-title context" th:text="${study.title}"></h5>
                                </a>
                                <p class="card-text" th:text="${study.shortDescription}">Short description</p>
                                <p class="card-text context">
                                    <span th:each="tag: ${study.tags}"
                                          class="font-weight-light font-monospace badge rounded-pill bg-success mr-3">
                                        <a th:href="@{'/search/study?keyword=' + ${tag.title}}"
                                           class="text-decoration-none text-white">
                                            <i class="fa fa-tag"></i> <span th:text="${tag.title}">Tag</span>
                                        </a>
                                    </span>
                                    <span th:each="zone: ${study.zones}"
                                          class="font-weight-light font-monospace badge rounded-pill bg-primary mr-3">
                                        <a th:href="@{'/search/study?keyword=' + ${zone.localNameOfCity}}"
                                           class="text-decoration-none text-white">
                                            <i class="fa fa-globe"></i> <span th:text="${zone.localNameOfCity}"
                                                                              class="text-white">City</span>
                                        </a>
                                    </span>
                                </p>
                                <div class="d-flex justify-content-between align-items-center">
                                    <small class="text-muted">
                                        <i class="fa fa-user-circle"></i>
                                        <span th:text="${study.members.size()}"></span>명
                                    </small>
                                    <small class="text-muted date" th:text="${study.publishedDateTime}">9 mins</small>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div th:replace="fragments.html::footer"></div>
    <script th:replace="fragments.html::date-time"></script>
</body>
</html>
```

> `fragments.html`에 `tags`, `zones`를 표현하는 부분에 오타 및 `Bootstrap 5`버전에 맞지 않는 `class`를 사용하는 부분이 있어 수정하였습니다. 
> 
> * 버전 변경 
>   * 기존: font-weight-light text-monospace badge badge-pill badge-info me-3
>   * 변경: font-weight-light font-monospace badge rounded-pill bg-success me-3
> * url 변경
>   * 기존: /search/tags/
>   * 변경: /search/study?keyword=
> 
> <details>
> <summary>수정한 부분 보기</summary>
> 
> ```html
> <div class="row justify-content-center bg-light">
>     <div class="col-10">
>         <p>
>             <span th:each="tag: ${study.tags}"
>                   class="font-weight-light font-monospace badge rounded-pill bg-success me-3">
>                 <a th:href="@{'/search/study?keyword=' + ${tag.title}}" class="text-decoration-none text-white">
>                     <i class="fa fa-tag"></i> <span th:text="${tag.title}">Tag</span>
>                 </a>
>             </span>
>             <span th:each="zone: ${study.zones}" class="font-weight-light font-monospace badge rounded-pill bg-primary me-3">
>                 <a th:href="@{'/search/study?keyword=' + ${zone.localNameOfCity}}" class="text-decoration-none text-white">
>                     <i class="fa fa-globe"></i> <span th:text="${zone.localNameOfCity}">City</span>
>                 </a>
>             </span>
>         </p>
>     </div>
> </div>
> ```
> 
> </details>

## 테스트

애플리케이션 실행 후 검색창에 질의합니다.

주제는 jpa, spring, 지역은 서울특별시만 지정해서 스터디를 생성했으므로 몇 개 조회되지 않습니다.

제대로 된 테스트를 하려면 데이터를 직접 생성해서 추가하신뒤에 테스트하시면 됩니다.

저는 이후 다른 내용까지 추가한 뒤에 그렇게 진행할 예정입니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/66-01.png)
![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/66-02.png)
![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/66-03.png)
![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/66-04.png)
![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/66-05.png)

검색이 정상적으로 수행되는 것을 확인하였습니다.

태그나 지역을 클릭해도 바로 해당 내용으로 검색됩니다.

---

여기서 로그를 확인해보면 치명적인 결함을 찾을 수 있는데요, 바로 n+1 problem이 발생한다는 것입니다.

<details>
<summary>spring을 검색했을 때 로그</summary>

```text
2022-06-15 00:34:02.981 DEBUG 23280 --- [nio-8080-exec-9] org.hibernate.SQL                        : 
    select
        study0_.id as id1_7_,
        study0_.closed as closed2_7_,
        study0_.closed_date_time as closed_d3_7_,
        study0_.full_description as full_des4_7_,
        study0_.image as image5_7_,
        study0_.path as path6_7_,
        study0_.published as publishe7_7_,
        study0_.published_date_time as publishe8_7_,
        study0_.recruiting as recruiti9_7_,
        study0_.recruiting_updated_date_time as recruit10_7_,
        study0_.short_description as short_d11_7_,
        study0_.title as title12_7_,
        study0_.use_banner as use_ban13_7_ 
    from
        study study0_ 
    where
        study0_.published=? 
        and (
            lower(study0_.title) like ? escape '!'
        ) 
        or exists (
            select
                1 
            from
                study_tags tags1_,
                tag tag2_ 
            where
                study0_.id=tags1_.study_id 
                and tags1_.tags_id=tag2_.id 
                and (
                    lower(tag2_.title) like ? escape '!'
                )
        ) 
        or exists (
            select
                1 
            from
                study_zones zones3_,
                zone zone4_ 
            where
                study0_.id=zones3_.study_id 
                and zones3_.zones_id=zone4_.id 
                and (
                    lower(zone4_.local_name_of_city) like ? escape '!'
                )
        )
2022-06-15 00:34:02.982 TRACE 23280 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BOOLEAN] - [true]
2022-06-15 00:34:02.982 TRACE 23280 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [2] as [VARCHAR] - [%spring%]
2022-06-15 00:34:02.982 TRACE 23280 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [3] as [VARCHAR] - [%spring%]
2022-06-15 00:34:02.982 TRACE 23280 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [4] as [VARCHAR] - [%spring%]
2022-06-15 00:34:03.010 DEBUG 23280 --- [nio-8080-exec-9] org.hibernate.SQL                        : 
    select
        count(notificati0_.id) as col_0_0_ 
    from
        notification notificati0_ 
    where
        notificati0_.account_account_id=? 
        and notificati0_.checked=?
2022-06-15 00:34:03.010 TRACE 23280 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [86]
2022-06-15 00:34:03.010 TRACE 23280 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [2] as [BOOLEAN] - [false]
2022-06-15 00:34:03.019 DEBUG 23280 --- [nio-8080-exec-9] org.hibernate.SQL                        : 
    select
        tags0_.study_id as study_id1_10_0_,
        tags0_.tags_id as tags_id2_10_0_,
        tag1_.id as id1_12_1_,
        tag1_.title as title2_12_1_ 
    from
        study_tags tags0_ 
    inner join
        tag tag1_ 
            on tags0_.tags_id=tag1_.id 
    where
        tags0_.study_id=?
2022-06-15 00:34:03.019 TRACE 23280 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [87]
2022-06-15 00:34:03.020 DEBUG 23280 --- [nio-8080-exec-9] org.hibernate.SQL                        : 
    select
        zones0_.study_id as study_id1_11_0_,
        zones0_.zones_id as zones_id2_11_0_,
        zone1_.id as id1_13_1_,
        zone1_.city as city2_13_1_,
        zone1_.local_name_of_city as local_na3_13_1_,
        zone1_.province as province4_13_1_ 
    from
        study_zones zones0_ 
    inner join
        zone zone1_ 
            on zones0_.zones_id=zone1_.id 
    where
        zones0_.study_id=?
2022-06-15 00:34:03.020 TRACE 23280 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [87]
2022-06-15 00:34:03.021 DEBUG 23280 --- [nio-8080-exec-9] org.hibernate.SQL                        : 
    select
        members0_.study_id as study_id1_9_0_,
        members0_.members_account_id as members_2_9_0_,
        account1_.account_id as account_1_0_1_,
        account1_.created_date as created_2_0_1_,
        account1_.last_modified_date as last_mod3_0_1_,
        account1_.email as email4_0_1_,
        account1_.email_token as email_to5_0_1_,
        account1_.email_token_generated_at as email_to6_0_1_,
        account1_.is_valid as is_valid7_0_1_,
        account1_.joined_at as joined_a8_0_1_,
        account1_.nickname as nickname9_0_1_,
        account1_.study_created_by_email as study_c10_0_1_,
        account1_.study_created_by_web as study_c11_0_1_,
        account1_.study_registration_result_by_email as study_r12_0_1_,
        account1_.study_registration_result_by_web as study_r13_0_1_,
        account1_.study_updated_by_email as study_u14_0_1_,
        account1_.study_updated_by_web as study_u15_0_1_,
        account1_.password as passwor16_0_1_,
        account1_.bio as bio17_0_1_,
        account1_.company as company18_0_1_,
        account1_.image as image19_0_1_,
        account1_.job as job20_0_1_,
        account1_.location as locatio21_0_1_,
        account1_.url as url22_0_1_ 
    from
        study_members members0_ 
    inner join
        account account1_ 
            on members0_.members_account_id=account1_.account_id 
    where
        members0_.study_id=?
2022-06-15 00:34:03.021 TRACE 23280 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [87]
2022-06-15 00:34:03.021 DEBUG 23280 --- [nio-8080-exec-9] org.hibernate.SQL                        : 
    select
        tags0_.study_id as study_id1_10_0_,
        tags0_.tags_id as tags_id2_10_0_,
        tag1_.id as id1_12_1_,
        tag1_.title as title2_12_1_ 
    from
        study_tags tags0_ 
    inner join
        tag tag1_ 
            on tags0_.tags_id=tag1_.id 
    where
        tags0_.study_id=?
2022-06-15 00:34:03.021 TRACE 23280 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [101]
2022-06-15 00:34:03.022 DEBUG 23280 --- [nio-8080-exec-9] org.hibernate.SQL                        : 
    select
        zones0_.study_id as study_id1_11_0_,
        zones0_.zones_id as zones_id2_11_0_,
        zone1_.id as id1_13_1_,
        zone1_.city as city2_13_1_,
        zone1_.local_name_of_city as local_na3_13_1_,
        zone1_.province as province4_13_1_ 
    from
        study_zones zones0_ 
    inner join
        zone zone1_ 
            on zones0_.zones_id=zone1_.id 
    where
        zones0_.study_id=?
2022-06-15 00:34:03.022 TRACE 23280 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [101]
2022-06-15 00:34:03.022 DEBUG 23280 --- [nio-8080-exec-9] org.hibernate.SQL                        : 
    select
        members0_.study_id as study_id1_9_0_,
        members0_.members_account_id as members_2_9_0_,
        account1_.account_id as account_1_0_1_,
        account1_.created_date as created_2_0_1_,
        account1_.last_modified_date as last_mod3_0_1_,
        account1_.email as email4_0_1_,
        account1_.email_token as email_to5_0_1_,
        account1_.email_token_generated_at as email_to6_0_1_,
        account1_.is_valid as is_valid7_0_1_,
        account1_.joined_at as joined_a8_0_1_,
        account1_.nickname as nickname9_0_1_,
        account1_.study_created_by_email as study_c10_0_1_,
        account1_.study_created_by_web as study_c11_0_1_,
        account1_.study_registration_result_by_email as study_r12_0_1_,
        account1_.study_registration_result_by_web as study_r13_0_1_,
        account1_.study_updated_by_email as study_u14_0_1_,
        account1_.study_updated_by_web as study_u15_0_1_,
        account1_.password as passwor16_0_1_,
        account1_.bio as bio17_0_1_,
        account1_.company as company18_0_1_,
        account1_.image as image19_0_1_,
        account1_.job as job20_0_1_,
        account1_.location as locatio21_0_1_,
        account1_.url as url22_0_1_ 
    from
        study_members members0_ 
    inner join
        account account1_ 
            on members0_.members_account_id=account1_.account_id 
    where
        members0_.study_id=?
2022-06-15 00:34:03.022 TRACE 23280 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [101]
2022-06-15 00:34:03.023 DEBUG 23280 --- [nio-8080-exec-9] org.hibernate.SQL                        : 
    select
        tags0_.study_id as study_id1_10_0_,
        tags0_.tags_id as tags_id2_10_0_,
        tag1_.id as id1_12_1_,
        tag1_.title as title2_12_1_ 
    from
        study_tags tags0_ 
    inner join
        tag tag1_ 
            on tags0_.tags_id=tag1_.id 
    where
        tags0_.study_id=?
2022-06-15 00:34:03.023 TRACE 23280 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [103]
2022-06-15 00:34:03.023 DEBUG 23280 --- [nio-8080-exec-9] org.hibernate.SQL                        : 
    select
        zones0_.study_id as study_id1_11_0_,
        zones0_.zones_id as zones_id2_11_0_,
        zone1_.id as id1_13_1_,
        zone1_.city as city2_13_1_,
        zone1_.local_name_of_city as local_na3_13_1_,
        zone1_.province as province4_13_1_ 
    from
        study_zones zones0_ 
    inner join
        zone zone1_ 
            on zones0_.zones_id=zone1_.id 
    where
        zones0_.study_id=?
2022-06-15 00:34:03.023 TRACE 23280 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [103]
2022-06-15 00:34:03.024 DEBUG 23280 --- [nio-8080-exec-9] org.hibernate.SQL                        : 
    select
        members0_.study_id as study_id1_9_0_,
        members0_.members_account_id as members_2_9_0_,
        account1_.account_id as account_1_0_1_,
        account1_.created_date as created_2_0_1_,
        account1_.last_modified_date as last_mod3_0_1_,
        account1_.email as email4_0_1_,
        account1_.email_token as email_to5_0_1_,
        account1_.email_token_generated_at as email_to6_0_1_,
        account1_.is_valid as is_valid7_0_1_,
        account1_.joined_at as joined_a8_0_1_,
        account1_.nickname as nickname9_0_1_,
        account1_.study_created_by_email as study_c10_0_1_,
        account1_.study_created_by_web as study_c11_0_1_,
        account1_.study_registration_result_by_email as study_r12_0_1_,
        account1_.study_registration_result_by_web as study_r13_0_1_,
        account1_.study_updated_by_email as study_u14_0_1_,
        account1_.study_updated_by_web as study_u15_0_1_,
        account1_.password as passwor16_0_1_,
        account1_.bio as bio17_0_1_,
        account1_.company as company18_0_1_,
        account1_.image as image19_0_1_,
        account1_.job as job20_0_1_,
        account1_.location as locatio21_0_1_,
        account1_.url as url22_0_1_ 
    from
        study_members members0_ 
    inner join
        account account1_ 
            on members0_.members_account_id=account1_.account_id 
    where
        members0_.study_id=?
2022-06-15 00:34:03.024 TRACE 23280 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [103]
2022-06-15 00:34:03.024 DEBUG 23280 --- [nio-8080-exec-9] org.hibernate.SQL                        : 
    select
        tags0_.study_id as study_id1_10_0_,
        tags0_.tags_id as tags_id2_10_0_,
        tag1_.id as id1_12_1_,
        tag1_.title as title2_12_1_ 
    from
        study_tags tags0_ 
    inner join
        tag tag1_ 
            on tags0_.tags_id=tag1_.id 
    where
        tags0_.study_id=?
2022-06-15 00:34:03.024 TRACE 23280 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [105]
2022-06-15 00:34:03.025 DEBUG 23280 --- [nio-8080-exec-9] org.hibernate.SQL                        : 
    select
        zones0_.study_id as study_id1_11_0_,
        zones0_.zones_id as zones_id2_11_0_,
        zone1_.id as id1_13_1_,
        zone1_.city as city2_13_1_,
        zone1_.local_name_of_city as local_na3_13_1_,
        zone1_.province as province4_13_1_ 
    from
        study_zones zones0_ 
    inner join
        zone zone1_ 
            on zones0_.zones_id=zone1_.id 
    where
        zones0_.study_id=?
2022-06-15 00:34:03.025 TRACE 23280 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [105]
2022-06-15 00:34:03.025 DEBUG 23280 --- [nio-8080-exec-9] org.hibernate.SQL                        : 
    select
        members0_.study_id as study_id1_9_0_,
        members0_.members_account_id as members_2_9_0_,
        account1_.account_id as account_1_0_1_,
        account1_.created_date as created_2_0_1_,
        account1_.last_modified_date as last_mod3_0_1_,
        account1_.email as email4_0_1_,
        account1_.email_token as email_to5_0_1_,
        account1_.email_token_generated_at as email_to6_0_1_,
        account1_.is_valid as is_valid7_0_1_,
        account1_.joined_at as joined_a8_0_1_,
        account1_.nickname as nickname9_0_1_,
        account1_.study_created_by_email as study_c10_0_1_,
        account1_.study_created_by_web as study_c11_0_1_,
        account1_.study_registration_result_by_email as study_r12_0_1_,
        account1_.study_registration_result_by_web as study_r13_0_1_,
        account1_.study_updated_by_email as study_u14_0_1_,
        account1_.study_updated_by_web as study_u15_0_1_,
        account1_.password as passwor16_0_1_,
        account1_.bio as bio17_0_1_,
        account1_.company as company18_0_1_,
        account1_.image as image19_0_1_,
        account1_.job as job20_0_1_,
        account1_.location as locatio21_0_1_,
        account1_.url as url22_0_1_ 
    from
        study_members members0_ 
    inner join
        account account1_ 
            on members0_.members_account_id=account1_.account_id 
    where
        members0_.study_id=?
2022-06-15 00:34:03.025 TRACE 23280 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [105]

```

</details>

그 이유는 뷰에서 `study.tags`, `study.zones`, `study.members를` 각각 호출하고 있기 때문인데요, `querydsl`을 이용해서 쿼리할 때 한번에 조회할 수 있도록 수정이 필요합니다.

이 내용은 다음 포스팅에서 다루도록 하겠습니다.

> 이전에 포스팅한 관련 내용을 먼저 확인하실 분들은 [여기](https://jaime-note.tistory.com/71?category=994945)로!