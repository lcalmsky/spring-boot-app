![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 927a89d)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 927a89d
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

스터디 조회 기능을 구현합니다.

기능 구현 후 쿼리 튜닝을 진행합니다.

## 엔드포인트 수정

이전 포스팅에서 스터디 생성 후 생성된 스터디 화면으로 들어갔을 때 구현된 뷰가 없어서 에러가 발생했었는데요, 해당 뷰로 이동할 수 있게 컨트롤러를 수정해줍니다.

`/src/main/java/io/lcalmsky/app/settings/controller/SettingsController.java`

```java
// 생략
@Controller
@RequiredArgsConstructor
public class StudyController {
    // 생략
    private final StudyRepository studyRepository;
    // 생략
    @GetMapping("/study/{path}")
    public String viewStudy(@CurrentUser Account account, @PathVariable String path, Model model) {
        model.addAttribute(account);
        model.addAttribute(studyRepository.findByPath(path));
        return "study/view";
    }
}
```

생성한 스터디 URL을 `PathParameter`로 전달받아서 `StudyRepository`에 조회한 뒤 스터디 정보를 모델로 전달하고, `view.html` 페이지로 이동합니다. 

<details>
<summary>StudyController.java 전체 보기</summary>

```java
package io.lcalmsky.app.study.endpoint;

import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.support.CurrentUser;
import io.lcalmsky.app.study.application.StudyService;
import io.lcalmsky.app.study.domain.entity.Study;
import io.lcalmsky.app.study.form.StudyForm;
import io.lcalmsky.app.study.form.validator.StudyFormValidator;
import io.lcalmsky.app.study.infra.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
public class StudyController {
    private final StudyService studyService;
    private final StudyFormValidator studyFormValidator;
    private final StudyRepository studyRepository;

    @InitBinder("studyForm")
    public void studyFormInitBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(studyFormValidator);
    }

    @GetMapping("/new-study")
    public String newStudyForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(new StudyForm());
        return "study/form";
    }

    @PostMapping("/new-study")
    public String newStudySubmit(@CurrentUser Account account, @Valid StudyForm studyForm, Errors errors) {
        if (errors.hasErrors()) {
            return "study/form";
        }
        Study newStudy = studyService.createNewStudy(studyForm, account);
        return "redirect:/study/" + URLEncoder.encode(newStudy.getPath(), StandardCharsets.UTF_8);
    }

    @GetMapping("/study/{path}")
    public String viewStudy(@CurrentUser Account account, @PathVariable String path, Model model) {
        model.addAttribute(account);
        model.addAttribute(studyRepository.findByPath(path));
        return "study/view";
    }
}
```

</details>

## Repository 수정

`StudyController`에서 `path`로 `Study`를 조회하기위해 사용할 메서드를 `StudyRepository`에 추가합니다.

`/src/main/java/io/lcalmsky/app/study/infra/repository/StudyRepository.java`

```java
package io.lcalmsky.app.study.infra.repository;

import io.lcalmsky.app.study.domain.entity.Study;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface StudyRepository extends JpaRepository<Study, Long> {
    boolean existsByPath(String path);

    Study findByPath(String path);
}
```

## Entity 수정

view를 구현할 때 사용할 메서드를 `Study`에 추가합니다.

`/src/main/java/io/lcalmsky/app/study/domain/entity/Study.java`

```java
// 생략
public class Study {
    // 생략
    public boolean isJoinable(UserAccount userAccount) { // (1)
        Account account = userAccount.getAccount();
        return this.isPublished() && this.isRecruiting() && !this.members.contains(account) && !this.managers.contains(account);
    }

    public boolean isMember(UserAccount userAccount) { // (2)
        return this.members.contains(userAccount.getAccount());
    }

    public boolean isManager(UserAccount userAccount) { // (3)
        return this.managers.contains(userAccount.getAccount());
    }
}
```

1. 스터디에 가입이 가능한지 확인하는 메서드 입니다.
2. 스터디의 멤버인지 확인하는 메서드 입니다.
3. 스터디의 관리자인지 확인하는 메서드 입니다.

## View 작성

스터디 조회 뷰를 작성합니다.

스터디 조회 화면에서 공통으로 사용할 것들을 미리 `fragments`에 추가하겠습니다.

`/src/main/resources/templates/fragments.html`

```html
<!DOCTYPE html>
<html lang="en"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<!--생략-->
<div th:fragment="study-banner" th:if="${study.useBanner}" class="row" id="study-logo">
    <img th:src="${study.image}"/>
</div>

<div th:fragment="study-info">
    <div class="row pt-4 text-left justify-content-center bg-light">
        <!--스터디 이름 표시-->
        <div class="col-6">
            <a href="#" class="text-decoration-none" th:href="@{'/study/' + ${study.path}}">
                <span class="h2" th:text="${study.title}">스터디 이름</span>
            </a>
        </div>
        <div class="col-4 text-right justify-content-end">
            <!--스터디를 공개하지 않았을 경우-->
            <span th:if="${!study.published}" class="d-inline-block" tabindex="0" data-bs-toggle="tooltip"
                  data-placement="bottom" title="공개 준비중입니다.">
                <button class="btn btn-primary btn-sm" style="pointer-events: none;" type="button"
                        disabled>DRAFT</button>
            </span>
            <!--스터디가 종료된 경우-->
            <span th:if="${study.closed}" class="d-inline-block" tabindex="0" data-bs-toggle="tooltip"
                  data-placement="bottom" title="종료된 스터디 입니다.">
                <button class="btn btn-primary btn-sm" style="pointer-events: none;" type="button"
                        disabled>CLOSED</button>
            </span>
            <!--스터디 멤버를 모집하고있지 않은 경우-->
            <span th:if="${!study.recruiting}" class="d-inline-block" tabindex="0" data-bs-toggle="tooltip"
                  data-placement="bottom" title="현재 모집중이 아닙니다.">
                <button class="btn btn-primary btn-sm" style="pointer-events: none;" type="button" disabled>OFF</button>
            </span>
            <!--인증된 사용자이고 스터디가 가입 가능한 경우-->
            <span sec:authorize="isAuthenticated()" th:if="${study.isJoinable(#authentication.principal)}"
                  class="btn-group" role="group" aria-label="Basic example">
                <!--스터디 가입 링크-->
                <a class="btn btn-primary" th:href="@{'/study/' + ${study.path} + '/join'}">
                    스터디 가입
                </a>
                <!--스터디 멤버 수-->
                <a class="btn btn-primary" th:href="@{'/study/' + ${study.path} + '/members'}"
                   th:text="${study.members.size()}">1</a>
            </span>
            <!--인증된 사용자이고 스터디 멤버인 경우-->
            <span sec:authorize="isAuthenticated()"
                  th:if="${!study.closed && study.isMember(#authentication.principal)}" class="btn-group"
                  role="group">
                <!--스터디 가입 링크-->
                <a class="btn btn-outline-warning" th:href="@{'/study/' + ${study.path} + '/leave'}">
                    스터디 탈퇴
                </a>
                <!--스터디 멤버 수-->
                <a class="btn btn-primary" th:href="@{'/study/' + ${study.path} + '/members'}"
                   th:text="${study.members.size()}">1</a>
            </span>
            <!--인증된 사용자이고 스터디 관리자인 경우-->
            <span sec:authorize="isAuthenticated()"
                  th:if="${study.published && !study.closed && study.isManager(#authentication.principal)}">
                <!--모임 만들기 링크-->
                <a class="btn btn-outline-primary" th:href="@{'/study/' + ${study.path} + '/new-event'}">
                    <i class>a fa-plus"></i> 모임 만들기
                </a>
            </span>
        </div>
    </div>
    <!--스터디 짧은 소개-->
    <div class="row justify-content-center bg-light">
        <div class="col-10">
            <p class="lead" th:text="${study.shortDescription}"></p>
        </div>
    </div>
    <!--태그, 지역-->
    <div class="row justify-content-center bg-light">
        <div class="col-10">
            <p>
                <span th:each="tag: ${study.tags}"
                      class="font-weight-light text-monospace badge badge-pill badge-info mr-3">
                    <a th:href="@{'/search/tag/' + ${tag.title}}" class="text-decoration-none text-white">
                        <i class="fa fa-tag"></i> <span th:text="${tag.title}">Tag</span>
                    </a>
                </span>
                <span th:each="zone: ${study.zones}" class="font-weight-light text-monospace badge badge-primary mr-3">
                    <a th:href="@{'/search/zone/' + ${zone.id}}" class="text-decoration-none text-white">
                        <i class="fa fa-globe"></i> <span th:text="${zone.localNameOfCity}">City</span>
                    </a>
                </span>
            </p>
        </div>
    </div>
</div>
<!--스터디 메뉴, 파라미터로 아이템을 전달받아 보여줌-->
<div th:fragment="study-menu (studyMenu)" class="row px-3 justify-content-center bg-light">
    <nav class="col-10 nav nav-tabs">
        <a class="nav-item nav-link" href="#" th:classappend="${studyMenu == 'info'}? active" th:href="@{'/study/' + ${study.path}}">
            <i class="fa fa-info-circle"></i> 소개
        </a>
        <a class="nav-item nav-link" href="#" th:classappend="${studyMenu == 'members'}? active" th:href="@{'/study/' + ${study.path} + '/members'}">
            <i class="fa fa-user"></i> 구성원
        </a>
        <a class="nav-item nav-link" th:classappend="${studyMenu == 'events'}? active" href="#" th:href="@{'/study/' + ${study.path} + '/events'}">
            <i class="fa fa-calendar"></i> 모임
        </a>
        <a sec:authorize="isAuthenticated()" th:if="${study.isManager(#authentication.principal)}"
           class="nav-item nav-link" th:classappend="${studyMenu == 'settings'}? active" href="#" th:href="@{'/study/' + ${study.path} + '/settings/description'}">
            <i class="fa fa-cog"></i> 설정
        </a>
    </nav>
</div>

<!--툴팁 스크립트-->
<script th:fragment="tooltip" type="application/javascript">
    $(function () {
        $('[data-toggle="tooltip"]').tooltip()
    })
</script>

</html>
```

설명은 인라인 주석으로 대체하였습니다.

이 `fragments`를 이용해 스터디 뷰를 작성합니다.

`/src/main/resources/templates/study/view.html`

```html
<!DOCTYPE html>
<!DOCTYPE html>
<html lang="en"
      xmlns:th="http://www.thymeleaf.org"
>
<head th:replace="fragments.html :: head"></head>
<body class="bg-light">
<div th:replace="fragments.html :: navigation-bar"></div>

<div th:replace="fragments.html :: study-banner"></div>

<div class="container">
    <!--스터디 정보-->
    <div th:replace="fragments.html :: study-info"></div>

    <!--스터디 메뉴-->
    <div th:replace="fragments.html :: study-menu(info)"></div>

    <!--스터디 상세 소개-->
    <div class="row px-3 justify-content-center">
        <div class="col-10 pt-3" th:utext="${study.fullDescription}"></div> <!--utext는 HTML 렌더링함-->
    </div>

    <div th:replace="fragments.html :: footer"></div>
</div>
<script th:replace="fragments.html::tooltip"></script>
</body>
</html>
```

## 테스트

여기까지 작성한 뒤, `postgres DB` 실행, 애플리케이션을 `local-db` 프로파일로 실행 후 로그인합니다.

그리고 스터디를 생성하지 않으셨다면 `스터디 개설` 버튼을 이용해 스터디를 생성합니다.

저는 지난 포스팅에서 이미 스터디를 생성하였기 떄문에 바로 주소로 접속할 수 있습니다.

`/study/{study-path}`

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/39-01.png)

화면이 정상적으로 노출되는 것을 확인할 수 있습니다.

하지만 여기서 문제점이 있습니다.

## N+1 problem

스터디 화면을 조회했을 때 발생하는 쿼리는 로그를 보면 확인 가능합니다.

```text
2022-04-13 00:46:22.433 DEBUG 41765 --- [io-8080-exec-10] org.hibernate.SQL                        : 
    select
        study0_.id as id1_4_,
        study0_.closed as closed2_4_,
        study0_.closed_date_time as closed_d3_4_,
        study0_.full_description as full_des4_4_,
        study0_.image as image5_4_,
        study0_.path as path6_4_,
        study0_.published as publishe7_4_,
        study0_.published_date_time as publishe8_4_,
        study0_.recruiting as recruiti9_4_,
        study0_.recruiting_updated_date_time as recruit10_4_,
        study0_.short_description as short_d11_4_,
        study0_.title as title12_4_,
        study0_.use_banner as use_ban13_4_ 
    from
        study study0_ 
    where
        study0_.path=?
2022-04-13 00:46:22.435 TRACE 41765 --- [io-8080-exec-10] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [VARCHAR] - [spring-boot]
2022-04-13 00:46:22.477 DEBUG 41765 --- [io-8080-exec-10] org.hibernate.SQL                        : 
    select
        members0_.study_id as study_id1_6_0_,
        members0_.members_account_id as members_2_6_0_,
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
2022-04-13 00:46:22.478 TRACE 41765 --- [io-8080-exec-10] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [92]
2022-04-13 00:46:22.480 DEBUG 41765 --- [io-8080-exec-10] org.hibernate.SQL                        : 
    select
        tags0_.study_id as study_id1_7_0_,
        tags0_.tags_id as tags_id2_7_0_,
        tag1_.id as id1_9_1_,
        tag1_.title as title2_9_1_ 
    from
        study_tags tags0_ 
    inner join
        tag tag1_ 
            on tags0_.tags_id=tag1_.id 
    where
        tags0_.study_id=?
2022-04-13 00:46:22.481 TRACE 41765 --- [io-8080-exec-10] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [92]
2022-04-13 00:46:22.483 DEBUG 41765 --- [io-8080-exec-10] org.hibernate.SQL                        : 
    select
        zones0_.study_id as study_id1_8_0_,
        zones0_.zones_id as zones_id2_8_0_,
        zone1_.id as id1_10_1_,
        zone1_.city as city2_10_1_,
        zone1_.local_name_of_city as local_na3_10_1_,
        zone1_.province as province4_10_1_ 
    from
        study_zones zones0_ 
    inner join
        zone zone1_ 
            on zones0_.zones_id=zone1_.id 
    where
        zones0_.study_id=?
2022-04-13 00:46:22.488 TRACE 41765 --- [io-8080-exec-10] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [92]
2022-04-13 00:46:22.494 DEBUG 41765 --- [io-8080-exec-10] org.hibernate.SQL                        : 
    select
        managers0_.study_id as study_id1_5_0_,
        managers0_.managers_account_id as managers2_5_0_,
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
        study_managers managers0_ 
    inner join
        account account1_ 
            on managers0_.managers_account_id=account1_.account_id 
    where
        managers0_.study_id=?
2022-04-13 00:46:22.495 TRACE 41765 --- [io-8080-exec-10] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [92]

```

무려 다섯 번이 발생한 것을 알 수 있는데요, 차례대로 (1) `path`로 `study`를 조회, (2) `study` 멤버인지 조회, (3) `study` `tags` 조회, (4) `study` `zones` 조회, (5) `study` 관리자인지 조회한 것 입니다.

상식적으로 생각해보면 한 번만 스터디 정보를 조회한 뒤 내부적으로 다 처리할 수 있는데 왜 이런 일이 발생하는 걸까요?

[JPA 관련 포스팅](https://jaime-note.tistory.com/54)에서 다룬 적이 있긴 합니다만 이런 문제를 `N+1 problem`이라고 부릅니다.

`join` 관계에 있는 테이블에 대해 `lazy` 로딩을 하기 때문인데요, `lazy` 로딩은 `entity` 내에 `collection` 타입의 필드가 존재할 때 바로 조회해서 가져오는 것이 아니라 추가 쿼리를 통해 가져오는 방식을 말합니다.

> ```java
> @Target({METHOD, FIELD}) 
> @Retention(RUNTIME)
> public @interface ManyToMany {
> 
>     Class targetEntity() default void.class;
> 
>     CascadeType[] cascade() default {};
> 
>     FetchType fetch() default LAZY;
> 
>     String mappedBy() default "";
> }
> ```
> 
> `Study` Entity에서 `@ManyToMany` 애너테이션을 사용했던 것을 기억하실텐데, `FetchType`에 보면 기본 값이 `LAZY`로 되어있습니다.

이러한 방식은 상황에 따라 성능 향상에 도움을 주기도 하지만, 지금 같은 경우는 쿼리가 5번이나 발생하게 되므로 오히려 비효율적이라고 할 수 있습니다.

자잘한 쿼리가 다수 발생하는 것과 묵직한 쿼리가 한 번 발생하는 것 또한 트레이드오프가 있지만, 엄청나게 복잡하고 시간이 오래 걸리는 쿼리가 아니라면 쿼리 수를 줄이는 방법으로 튜닝해 볼 수 있습니다.

쿼리 튜닝을 위해 `Entity`와 `Repository`에 추가적인 정보를 전달해줘야 합니다.

여러 가지 방법이 있지만 `@EntityGraph`를 이용해보도록 하겠습니다.

먼저 `Study` `Entity`를 수정합니다.

`/src/main/java/io/lcalmsky/app/study/domain/entity/Study.java`

```java
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import javax.persistence.Entity;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
// 생략
@Entity
@NamedEntityGraph(name = "Study.withAll", attributeNodes = {
        @NamedAttributeNode("tags"),
        @NamedAttributeNode("zones"),
        @NamedAttributeNode("managers"),
        @NamedAttributeNode("members")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Study {
    // 생략
}

```

`EntityGraph`에 이름을 명시해주는 작업으로 `Study.withAll`이라는 이름을 가지고 `tags`, `zones`, `managers`, `members` 네 가지 `attribute`에 대해 `Lazy` 로딩을 사용하지 않겠다는 뜻입니다.

<details>
<summary>수정된 Study.java 전체 보기</summary>

```java
package io.lcalmsky.app.study.domain.entity;

import io.lcalmsky.app.account.domain.UserAccount;
import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.domain.entity.Zone;
import io.lcalmsky.app.study.form.StudyForm;
import io.lcalmsky.app.tag.domain.entity.Tag;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@NamedEntityGraph(name = "Study.withAll", attributeNodes = {
        @NamedAttributeNode("tags"),
        @NamedAttributeNode("zones"),
        @NamedAttributeNode("managers"),
        @NamedAttributeNode("members")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Study {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToMany
    private Set<Account> managers = new HashSet<>();

    @ManyToMany
    private Set<Account> members = new HashSet<>();

    @Column(unique = true)
    private String path;

    private String title;

    private String shortDescription;

    @Lob @Basic(fetch = FetchType.EAGER)
    private String fullDescription;

    @Lob @Basic(fetch = FetchType.EAGER)
    private String image;

    @ManyToMany
    private Set<Tag> tags = new HashSet<>();

    @ManyToMany
    private Set<Zone> zones = new HashSet<>();

    private LocalDateTime publishedDateTime;

    private LocalDateTime closedDateTime;

    private LocalDateTime recruitingUpdatedDateTime;

    private boolean recruiting;

    private boolean published;

    private boolean closed;

    private boolean useBanner;

    public static Study from(StudyForm studyForm) {
        Study study = new Study();
        study.title = studyForm.getTitle();
        study.shortDescription = studyForm.getShortDescription();
        study.fullDescription = studyForm.getFullDescription();
        study.path = studyForm.getPath();
        return study;
    }

    public void addManager(Account account) {
        managers.add(account);
    }

    public boolean isJoinable(UserAccount userAccount) {
        Account account = userAccount.getAccount();
        return this.isPublished() && this.isRecruiting() && !this.members.contains(account) && !this.managers.contains(account);
    }

    public boolean isMember(UserAccount userAccount) {
        return this.members.contains(userAccount.getAccount());
    }

    public boolean isManager(UserAccount userAccount) {
        return this.managers.contains(userAccount.getAccount());
    }
}

```

</details>

다음으로 `StudyRepository`도 수정해줍니다.

`/src/main/java/io/lcalmsky/app/study/infra/repository/StudyRepository.java`

```java
package io.lcalmsky.app.study.infra.repository;

import io.lcalmsky.app.study.domain.entity.Study;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface StudyRepository extends JpaRepository<Study, Long> {
    boolean existsByPath(String path);

    @EntityGraph(value = "Study.withAll", type = EntityGraph.EntityGraphType.LOAD)
    Study findByPath(String path);
}
```

아까 명시한 `Study.withAll` `EntityGraph`를 `findByPath` 메서드를 사용할 때 적용한다는 뜻입니다.

이렇게 수정한 뒤 다시 애플리케이션을 실행하고 동일하게 스터디 화면으로 진입해보면,

```text
2022-04-13 00:49:54.947 DEBUG 41765 --- [nio-8080-exec-9] org.hibernate.SQL                        : 
    select
        study0_.id as id1_4_0_,
        account2_.account_id as account_1_0_1_,
        account4_.account_id as account_1_0_2_,
        zone6_.id as id1_10_3_,
        tag8_.id as id1_9_4_,
        study0_.closed as closed2_4_0_,
        study0_.closed_date_time as closed_d3_4_0_,
        study0_.full_description as full_des4_4_0_,
        study0_.image as image5_4_0_,
        study0_.path as path6_4_0_,
        study0_.published as publishe7_4_0_,
        study0_.published_date_time as publishe8_4_0_,
        study0_.recruiting as recruiti9_4_0_,
        study0_.recruiting_updated_date_time as recruit10_4_0_,
        study0_.short_description as short_d11_4_0_,
        study0_.title as title12_4_0_,
        study0_.use_banner as use_ban13_4_0_,
        account2_.created_date as created_2_0_1_,
        account2_.last_modified_date as last_mod3_0_1_,
        account2_.email as email4_0_1_,
        account2_.email_token as email_to5_0_1_,
        account2_.email_token_generated_at as email_to6_0_1_,
        account2_.is_valid as is_valid7_0_1_,
        account2_.joined_at as joined_a8_0_1_,
        account2_.nickname as nickname9_0_1_,
        account2_.study_created_by_email as study_c10_0_1_,
        account2_.study_created_by_web as study_c11_0_1_,
        account2_.study_registration_result_by_email as study_r12_0_1_,
        account2_.study_registration_result_by_web as study_r13_0_1_,
        account2_.study_updated_by_email as study_u14_0_1_,
        account2_.study_updated_by_web as study_u15_0_1_,
        account2_.password as passwor16_0_1_,
        account2_.bio as bio17_0_1_,
        account2_.company as company18_0_1_,
        account2_.image as image19_0_1_,
        account2_.job as job20_0_1_,
        account2_.location as locatio21_0_1_,
        account2_.url as url22_0_1_,
        managers1_.study_id as study_id1_5_0__,
        managers1_.managers_account_id as managers2_5_0__,
        account4_.created_date as created_2_0_2_,
        account4_.last_modified_date as last_mod3_0_2_,
        account4_.email as email4_0_2_,
        account4_.email_token as email_to5_0_2_,
        account4_.email_token_generated_at as email_to6_0_2_,
        account4_.is_valid as is_valid7_0_2_,
        account4_.joined_at as joined_a8_0_2_,
        account4_.nickname as nickname9_0_2_,
        account4_.study_created_by_email as study_c10_0_2_,
        account4_.study_created_by_web as study_c11_0_2_,
        account4_.study_registration_result_by_email as study_r12_0_2_,
        account4_.study_registration_result_by_web as study_r13_0_2_,
        account4_.study_updated_by_email as study_u14_0_2_,
        account4_.study_updated_by_web as study_u15_0_2_,
        account4_.password as passwor16_0_2_,
        account4_.bio as bio17_0_2_,
        account4_.company as company18_0_2_,
        account4_.image as image19_0_2_,
        account4_.job as job20_0_2_,
        account4_.location as locatio21_0_2_,
        account4_.url as url22_0_2_,
        members3_.study_id as study_id1_6_1__,
        members3_.members_account_id as members_2_6_1__,
        zone6_.city as city2_10_3_,
        zone6_.local_name_of_city as local_na3_10_3_,
        zone6_.province as province4_10_3_,
        zones5_.study_id as study_id1_8_2__,
        zones5_.zones_id as zones_id2_8_2__,
        tag8_.title as title2_9_4_,
        tags7_.study_id as study_id1_7_3__,
        tags7_.tags_id as tags_id2_7_3__ 
    from
        study study0_ 
    left outer join
        study_managers managers1_ 
            on study0_.id=managers1_.study_id 
    left outer join
        account account2_ 
            on managers1_.managers_account_id=account2_.account_id 
    left outer join
        study_members members3_ 
            on study0_.id=members3_.study_id 
    left outer join
        account account4_ 
            on members3_.members_account_id=account4_.account_id 
    left outer join
        study_zones zones5_ 
            on study0_.id=zones5_.study_id 
    left outer join
        zone zone6_ 
            on zones5_.zones_id=zone6_.id 
    left outer join
        study_tags tags7_ 
            on study0_.id=tags7_.study_id 
    left outer join
        tag tag8_ 
            on tags7_.tags_id=tag8_.id 
    where
        study0_.path=?
2022-04-13 00:49:54.947 TRACE 41765 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [VARCHAR] - [spring-boot]

```

이렇게 다소 복잡하지만 하나의 쿼리만 발생한 것을 확인할 수 있습니다.

> 이 쿼리 말고 다른 쿼리가 발생한다면, 애플리케이션 재시작 후 화면에서 새로고침을 누르거나 기타 다른 방법으로 스터디 화면에 진입했기 때문입니다. 예전에 구현한 로그인 유지 기능을 위해 계정 정보 관련 쿼리가 두 번 더 발생할 수 있는데, 정확한 확인을 위해선 이미 진입한 뒤에 로그를 지우고 다시 한 번 더 새로고침을 눌러보세요.

## 테스트 코드 작성

오늘 구현한 기능을 테스트하기 위한 코드를 작성합니다.

`/src/test/java/io/lcalmsky/app/study/endpoint/StudyControllerTest.java`

```java
// 생략
@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class StudyControllerTest {
    // 생략
    @Test
    @DisplayName("스터디 뷰")
    @WithAccount("jaime")
    void studyView() throws Exception {
        Account account = accountRepository.findByNickname("jaime");
        String studyPath = "study-path";
        studyService.createNewStudy(StudyForm.builder()
                .path(studyPath)
                .title("study-title")
                .shortDescription("short-description")
                .fullDescription("full-description")
                .build(), account);
        mockMvc.perform(get("/study/" + studyPath))
                .andExpect(status().isOk())
                .andExpect(view().name("study/view"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("study"));
    }
}
```

스터디 뷰로 진입하기 위해 먼저 `StudyService`를 이용해 스터디를 생성하고, 해당 주소로 진입했을 때 뷰를 정확하게 반환하는지, `model`로 `account`와 `study` 정보를 전달하는지 확인합니다.

<details>
<summary>StudyControllerTest.java 전체 보기</summary>

```java
package io.lcalmsky.app.study.endpoint;

import io.lcalmsky.app.WithAccount;
import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.infra.repository.AccountRepository;
import io.lcalmsky.app.study.application.StudyService;
import io.lcalmsky.app.study.form.StudyForm;
import io.lcalmsky.app.study.infra.repository.StudyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class StudyControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired AccountRepository accountRepository;
    @Autowired StudyRepository studyRepository;
    @Autowired StudyService studyService;

    @Test
    @DisplayName("스터디 폼 조회")
    @WithAccount("jaime")
    void studyForm() throws Exception {
        mockMvc.perform(get("/new-study"))
                .andExpect(status().isOk())
                .andExpect(view().name("study/form"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("studyForm"));
    }

    @Test
    @DisplayName("스터디 추가: 정상")
    @WithAccount("jaime")
    void createStudy() throws Exception {
        String studyPath = "study-test";
        mockMvc.perform(post("/new-study")
                        .param("path", studyPath)
                        .param("title", "study-title")
                        .param("shortDescription", "short-description")
                        .param("fullDescription", "fullDescription")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath));
        assertTrue(studyRepository.existsByPath(studyPath));
    }

    @Test
    @DisplayName("스터디 추가: 입력값 비정상")
    @WithAccount("jaime")
    void createStudyWithError() throws Exception {
        String studyPath = "s";
        mockMvc.perform(post("/new-study")
                        .param("path", studyPath)
                        .param("title", "study-title")
                        .param("shortDescription", "short-description")
                        .param("fullDescription", "fullDescription")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("study/form"))
                .andExpect(model().hasErrors());
    }

    @Test
    @DisplayName("스터디 추가: 입력값 중복")
    @WithAccount("jaime")
    void createStudyWithDuplicate() throws Exception {
        Account account = accountRepository.findByNickname("jaime");
        String duplicatedPath = "study-path";
        studyService.createNewStudy(StudyForm.builder()
                .path(duplicatedPath)
                .title("study-title")
                .shortDescription("short-description")
                .fullDescription("full-description")
                .build(), account);
        mockMvc.perform(post("/new-study")
                        .param("path", duplicatedPath)
                        .param("title", "study-title")
                        .param("shortDescription", "short-description")
                        .param("fullDescription", "fullDescription")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("study/form"))
                .andExpect(model().hasErrors());
    }

    @Test
    @DisplayName("스터디 뷰")
    @WithAccount("jaime")
    void studyView() throws Exception {
        Account account = accountRepository.findByNickname("jaime");
        String studyPath = "study-path";
        studyService.createNewStudy(StudyForm.builder()
                .path(studyPath)
                .title("study-title")
                .shortDescription("short-description")
                .fullDescription("full-description")
                .build(), account);
        mockMvc.perform(get("/study/" + studyPath))
                .andExpect(status().isOk())
                .andExpect(view().name("study/view"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("study"));
    }
}
```

</details>

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/39-02.png)

정상적으로 수행되었습니다!