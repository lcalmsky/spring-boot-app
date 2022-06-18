![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 022e586)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 022e586
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

검색화면을 개선합니다.

페이지 내비게이션, 정렬 조건 등 기존에 누락된 기능들을 추가합니다.

## 라이브러리 설치

`/src/main/resources/static` 경로로 이동하여 `mark` 라이브러리를 설치합니다.

```shell
> cd /src/main/resources/static
> npm install mark.js --save
```

## MainController 수정

페이징 처리를 위해 `Model`로 전달할 값들을 추가합니다.

`/src/main/java/io/lcalmsky/app/modules/main/endpoint/controller/MainController.java`

```java
// 생략
@Controller
@RequiredArgsConstructor
public class MainController {
    // 생략
    @GetMapping("/search/study")
    public String searchStudy(String keyword, Model model,
                              @PageableDefault(size = 9, sort = "publishedDateTime", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<Study> studyPage = studyRepository.findByKeyword(keyword, pageable);
        model.addAttribute("studyPage", studyPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortProperty", pageable.getSort().toString().contains("publishedDateTime")
                ? "publishedDateTime"
                : "memberCount");
        return "search";
    }
}
```

<details>
<summary>MainController.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.main.endpoint.controller;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.support.CurrentUser;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.infra.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
    public String searchStudy(String keyword, Model model,
                              @PageableDefault(size = 9, sort = "publishedDateTime", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<Study> studyPage = studyRepository.findByKeyword(keyword, pageable);
        model.addAttribute("studyPage", studyPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortProperty", pageable.getSort().toString().contains("publishedDateTime")
                ? "publishedDateTime"
                : "memberCount");
        return "search";
    }
}
```

</details>

## Entity 수정

`Study`에 정렬을 위해 사용될 `memberCount` 변수를 추가합니다.

`/src/main/java/io/lcalmsky/app/modules/study/domain/entity/Study.java`

```java
// 생략
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Study {
    // 생략
    @ColumnDefault(value = "0")
    private Integer memberCount;
    // 생략
    public void addMember(Account account) {
        this.members.add(account);
        this.memberCount++;
    }

    public void removeMember(Account account) {
        this.members.remove(account);
        this.memberCount--;
    }
    // 생략
}
```

직접 members를 가져와 size를 구해도 되지만 추가 쿼리가 발생할 수 있기 때문에, 가입시/탈퇴시 매번 갱신하도록 수정하였습니다.

<details>
<summary>Study.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.study.domain.entity;

import io.lcalmsky.app.modules.account.domain.UserAccount;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.domain.entity.Zone;
import io.lcalmsky.app.modules.study.endpoint.form.StudyDescriptionForm;
import io.lcalmsky.app.modules.study.endpoint.form.StudyForm;
import io.lcalmsky.app.modules.tag.domain.entity.Tag;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
@NamedEntityGraph(name = "Study.withTagsAndManagers", attributeNodes = {
        @NamedAttributeNode("tags"),
        @NamedAttributeNode("managers")
})
@NamedEntityGraph(name = "Study.withZonesAndManagers", attributeNodes = {
        @NamedAttributeNode("zones"),
        @NamedAttributeNode("managers")
})
@NamedEntityGraph(name = "Study.withManagers", attributeNodes = {
        @NamedAttributeNode("managers")
})
@NamedEntityGraph(name = "Study.withMembers", attributeNodes = {
        @NamedAttributeNode("members")
})
@NamedEntityGraph(name = "Study.withTagsAndZones", attributeNodes = {
        @NamedAttributeNode("tags"),
        @NamedAttributeNode("zones")
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

    @Accessors(fluent = true)
    private boolean useBanner;

    @ColumnDefault(value = "0")
    private Integer memberCount;

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

    public void updateDescription(StudyDescriptionForm studyDescriptionForm) {
        this.shortDescription = studyDescriptionForm.getShortDescription();
        this.fullDescription = studyDescriptionForm.getFullDescription();
    }

    public void updateImage(String image) {
        this.image = image;
    }

    public void setBanner(boolean useBanner) {
        this.useBanner = useBanner;
    }

    public void addTag(Tag tag) {
        this.tags.add(tag);
    }

    public void removeTag(Tag tag) {
        this.tags.remove(tag);
    }

    public void addZone(Zone zone) {
        this.zones.add(zone);
    }

    public void removeZone(Zone zone) {
        this.zones.remove(zone);
    }

    public void publish() {
        if (this.closed || this.published) {
            throw new IllegalStateException("스터디를 이미 공개했거나 종료된 스터디 입니다.");
        }
        this.published = true;
        this.publishedDateTime = LocalDateTime.now();
    }

    public void close() {
        if (!this.published || this.closed) {
            throw new IllegalStateException("스터디를 공개하지 않았거나 이미 종료한 스터디 입니다.");
        }
        this.closed = true;
        this.closedDateTime = LocalDateTime.now();
    }

    public boolean isEnableToRecruit() {
        return this.published && this.recruitingUpdatedDateTime == null
                || this.recruitingUpdatedDateTime.isBefore(LocalDateTime.now().minusHours(1));
    }

    public void updatePath(String newPath) {
        this.path = newPath;
    }

    public void updateTitle(String newTitle) {
        this.title = newTitle;
    }

    public boolean isRemovable() {
        return !this.published;
    }

    public void startRecruit() {
        if (!isEnableToRecruit()) {
            throw new RuntimeException("인원 모집을 시작할 수 없습니다. 스터디를 공개하거나 한 시간 뒤 다시 시도하세요.");
        }
        this.recruiting = true;
        this.recruitingUpdatedDateTime = LocalDateTime.now();
    }

    public void stopRecruit() {
        if (!isEnableToRecruit()) {
            throw new RuntimeException("인원 모집을 멈출 수 없습니다. 스터디를 공개하거나 한 시간 뒤 다시 시도하세요.");
        }
        this.recruiting = false;
        this.recruitingUpdatedDateTime = LocalDateTime.now();
    }

    public void addMember(Account account) {
        this.members.add(account);
        this.memberCount++;
    }

    public void removeMember(Account account) {
        this.members.remove(account);
        this.memberCount--;
    }

    public String getEncodedPath() {
        return URLEncoder.encode(path, StandardCharsets.UTF_8);
    }

    public boolean isManagedBy(Account account) {
        return this.getManagers().contains(account);
    }
}

```

</details>

## 검색 뷰 수정

search.html 파일을 수정합니다.

`/src/main/resources/templates/search.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html::head"></head>
<body class="bg-light">
    <div th:replace="fragments.html::navigation-bar"></div>
    <div class="container">
        <div class="py-5 text-center">
            <!--생략-->
            <div class="dropdown">
                <button class="btn btn-light dropdown-toggle" type="button" id="dropdownMenuButton"
                        data-bs-toggle="dropdown" area-haspopup="false">
                    정렬 방식
                </button>
                <div class="dropdown-menu" aria-labelledby="dropdownMenuButton">
                    <a class="dropdown-item"
                       th:classappend="${#strings.equals(sortProperty, 'publishedDateTime')}? active"
                       th:href="@{'/search/study?sort=publishedDateTime,desc&keyword=' + ${keyword}}">
                        스터디 공개일
                    </a>
                    <a class="dropdown-item"
                       th:classappend="${#strings.equals(sortProperty, 'memberCount')}? active"
                       th:href="@{'/search/study?sort=memberCount,desc&keyword=' + ${keyword}}">
                        멤버수
                    </a>
                </div>
            </div>
        </div>
        <!--생략-->
        <div class="row justify-content-center">
            <div class="col-sm-10">
                <nav>
                    <ul class="pagination justify-content-center">
                        <li class="page-item" th:classappend="${!studyPage.hasPrevious()}? disabled">
                            <a th:href="@{'/search/study?keyword=' + ${keyword} + '&sort=' + ${sortProperty} + ',desc&page=' + ${studyPage.getNumber() - 1}}"
                               class="page-link" tabindex="-1" aria-disabled="true">
                                <!--hasPrvious가 false일 때 class 뒤에 disabled를 추가함-->
                                Previous
                            </a>
                        </li>
                        <li class="page-item" th:classappend="${i == studyPage.getNumber()}? active"
                            th:each="i: ${#numbers.sequence(0, studyPage.getTotalPages() - 1)}">
                            <a th:href="@{'/search/study?keyword=' + ${keyword} + '&sort=' + ${sortProperty} + ',desc&page=' + ${i}}"
                               class="page-link" href="#" th:text="${i + 1}">1</a>
                        </li>
                        <li class="page-item" th:classappend="${!studyPage.hasNext()}? disabled">
                            <!--hasNexts가 false일 때 class 뒤에 disabled를 추가함-->
                            <a th:href="@{'/search/study?keyword=' + ${keyword} + '&sort=' + ${sortProperty} + ',desc&page=' + ${studyPage.getNumber() + 1}}"
                               class="page-link">
                                Next
                            </a>
                        </li>
                    </ul>
                </nav>
            </div>
        </div>
    </div>
    <!--생략-->
    <script src="/node_modules/mark.js/dist/jquery.mark.min.js"></script>
    <script type="application/javascript">
        $(function () {
            var mark = function () {
                var keyword = $("#keyword").text();
                var options = {
                    "each": function (element) {
                        setTimeout(function () {
                            $(element).addClass("animate");
                        }, 150);
                    }
                };
                $(".context").unmark({
                    done: function () {
                        $(".context").mark(keyword, options);
                    }
                });
            };
            mark();
        })
    </script>
</body>
</html>
```

페이지 내비게이션과 키워드 하일라이트 기능을 추가하였습니다.

<details>
<summary>search.html 전체 보기</summary>

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html::head"></head>
<body class="bg-light">
    <div th:replace="fragments.html::navigation-bar"></div>
    <div class="container">
        <div class="py-5 text-center">
            <p class="lead" th:if="${studyPage.getTotalElements() == 0}">
                <strong th:text="${keyword}" id="keyword" class="context"></strong>에 해당하는 스터디가 없습니다.
            </p>
            <p class="lead" th:if="${studyPage.getTotalElements() > 0}">
                <strong th:text="${keyword}" id="keyword" class="context"></strong>에 해당하는 스터디를
                <span th:text="${studyPage.getTotalElements()}"></span>개 찾았습니다.
            </p>
            <div class="dropdown">
                <button class="btn btn-light dropdown-toggle" type="button" id="dropdownMenuButton"
                        data-bs-toggle="dropdown" area-haspopup="false">
                    정렬 방식
                </button>
                <div class="dropdown-menu" aria-labelledby="dropdownMenuButton">
                    <a class="dropdown-item"
                       th:classappend="${#strings.equals(sortProperty, 'publishedDateTime')}? active"
                       th:href="@{'/search/study?sort=publishedDateTime,desc&keyword=' + ${keyword}}">
                        스터디 공개일
                    </a>
                    <a class="dropdown-item"
                       th:classappend="${#strings.equals(sortProperty, 'memberCount')}? active"
                       th:href="@{'/search/study?sort=memberCount,desc&keyword=' + ${keyword}}">
                        멤버수
                    </a>
                </div>
            </div>
        </div>
        <div class="row justify-content-center">
            <div class="col-sm-10">
                <div class="row">
                    <div class="col-md-4" th:each="study: ${studyPage.getContent()}">
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
        <div class="row justify-content-center">
            <div class="col-sm-10">
                <nav>
                    <ul class="pagination justify-content-center">
                        <li class="page-item" th:classappend="${!studyPage.hasPrevious()}? disabled">
                            <a th:href="@{'/search/study?keyword=' + ${keyword} + '&sort=' + ${sortProperty} + ',desc&page=' + ${studyPage.getNumber() - 1}}"
                               class="page-link" tabindex="-1" aria-disabled="true">
                                <!--hasPrvious가 false일 때 class 뒤에 disabled를 추가함-->
                                Previous
                            </a>
                        </li>
                        <li class="page-item" th:classappend="${i == studyPage.getNumber()}? active"
                            th:each="i: ${#numbers.sequence(0, studyPage.getTotalPages() - 1)}">
                            <a th:href="@{'/search/study?keyword=' + ${keyword} + '&sort=' + ${sortProperty} + ',desc&page=' + ${i}}"
                               class="page-link" href="#" th:text="${i + 1}">1</a>
                        </li>
                        <li class="page-item" th:classappend="${!studyPage.hasNext()}? disabled">
                            <!--hasNexts가 false일 때 class 뒤에 disabled를 추가함-->
                            <a th:href="@{'/search/study?keyword=' + ${keyword} + '&sort=' + ${sortProperty} + ',desc&page=' + ${studyPage.getNumber() + 1}}"
                               class="page-link">
                                Next
                            </a>
                        </li>
                    </ul>
                </nav>
            </div>
        </div>
    </div>
    <div th:replace="fragments.html::footer"></div>
    <script th:replace="fragments.html::date-time"></script>
    <script src="/node_modules/mark.js/dist/jquery.mark.min.js"></script>
    <script type="application/javascript">
        $(function () {
            var mark = function () {
                var keyword = $("#keyword").text();
                var options = {
                    "each": function (element) {
                        setTimeout(function () {
                            $(element).addClass("animate");
                        }, 150);
                    }
                };
                $(".context").unmark({
                    done: function () {
                        $(".context").mark(keyword, options);
                    }
                });
            };
            mark();
        })
    </script>
</body>
</html>
```

</details>

하일라이트 css 수정을 위해 `fragments.html` 파일에 추가해줍니다.

```html
<!DOCTYPE html>
<html lang="en"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head th:fragment="head">
    /*생략*/
    <style>
        /*생략*/
        mark {
            padding: 0;
            background: transparent;
            background: linear-gradient(to right, #f0ad4e 50%, transparent 50%);
            background-position: right bottom;
            background-size: 200% 100%;
            transition: all .5s ease;
            color: #fff;
        }

        mark.animate {
            background-position: left bottom;
            color: #000;
        }
    </style>
</head>
/*생략*/
</html>
```

<details>
<summary>fragments.html 전체 보기</summary>

```html
<!DOCTYPE html>
<html lang="en"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head th:fragment="head">
    <meta charset="UTF-8">
    <title>Webluxible</title>
    <!--    font 추가-->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@300;400;500&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="/node_modules/bootstrap/dist/css/bootstrap.min.css"/>
    <link rel="stylesheet" href="/node_modules/font-awesome/css/font-awesome.min.css"/> <!--font-awesome 추가-->
    <link rel="stylesheet" href="/node_modules/@yaireo/tagify/dist/tagify.css"/>
    <script src="/node_modules/jdenticon/dist/jdenticon.min.js"></script> <!--jdenticon script 추가-->
    <script src="/node_modules/jquery/dist/jquery.min.js"></script> <!--index.html에서 옮김-->
    <script src="/node_modules/bootstrap/dist/js/bootstrap.bundle.min.js"></script> <!--index.html에서 옮김-->
    <style>
        .container {
            max-width: 100%;
        }

        .tagify-outside {
            border: 0;
            padding: 0;
            margin: 0;
        }

        /*font 설정*/
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Noto Sans KR", "Segoe UI", "Roboto Light", serif, Arial, "Noto Sans";
        }

        body,
        input,
        button,
        select,
        optgroup,
        textarea,
        .tooltip,
        .popover {
            font-family: -apple-system, BlinkMacSystemFont, "Noto Sans KR", "Segoe UI", "Roboto Light", serif, Arial, "Noto Sans";
        }

        mark {
            padding: 0;
            background: transparent;
            background: linear-gradient(to right, #f0ad4e 50%, transparent 50%);
            background-position: right bottom;
            background-size: 200% 100%;
            transition: all .5s ease;
            color: #fff;
        }

        mark.animate {
            background-position: left bottom;
            color: #000;
        }
    </style>
</head>

<footer th:fragment="footer">
    <div class="row justify-content-center">
        <small class="d-flex mb-3 text-muted" style="justify-content: center">Webluxible &copy; 2021</small>
    </div>
</footer>

<div th:fragment="settings-menu (currentMenu)" class="list-group">
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'profile'} ? active" href="#"
       th:href="@{/settings/profile}">프로필</a>
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'password'} ? active" href="#"
       th:href="@{/settings/password}">패스워드</a>
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'notification'} ? active"
       href="#" th:href="@{/settings/notification}">알림 설정</a>
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'tags'} ? active" href="#"
       th:href="@{/settings/tags}">관심 주제</a>
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'zones'} ? active" href="#"
       th:href="@{/settings/zones}">활동 지역</a>
    <a class="list-group-item list-group-item-action list-group-item-danger"
       th:classappend="${currentMenu == 'account'}? active" href="#" th:href="@{/settings/account}">계정</a>
</div>

<nav th:fragment="navigation-bar" class="navbar navbar-expand-sm navbar-dark bg-dark">
    <a class="navbar-brand ms-3" href="/" th:href="@{/}">
        <img src="/images/logo.png" width="30" height="30">
    </a>
    <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-target="#navbarSupportedContent"
            aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
        <span class="navbar-toggler-icon"></span>
    </button>

    <div class="collapse navbar-collapse" id="navbarSupportedContent">
        <ul class="navbar-nav me-auto">
            <li class="nav-item">
                <form th:action="@{/search/study}" class="form-inline" method="get">
                    <input class="form-control me-sm-2" name="keyword" type="search" placeholder="스터디 찾기"
                           aria-label="Search"/>
                </form>
            </li>
        </ul>

        <ul class="navbar-nav justify-content-end">
            <li class="nav-item" sec:authorize="!isAuthenticated()">
                <a class="nav-link" th:href="@{/login}">로그인</a>
            </li>
            <li class="nav-item" sec:authorize="!isAuthenticated()">
                <a class="nav-link" th:href="@{/sign-up}">가입</a>
            </li>
            <li class="nav-item" sec:authorize="isAuthenticated()">
                <a class="nav-link" th:href="@{/notifications}">
                    <i th:if="${!hasNotification}" class="fa fa-bell-o" aria-hidden="true"></i>
                    <span class="text-info"><i th:if="${hasNotification}" class="fa fa-bell"
                                               aria-hidden="true"></i></span>
                </a>
            </li>
            <li class="nav-item" sec:authorize="isAuthenticated()">
                <!-- 경로 오타 수정-->
                <a class="nav-link btn btn-outline-primary" th:href="@{/new-study}">
                    <i class="fa fa-plus" aria-hidden="true"></i> 스터디 개설 <!--"스터디 개설" 문자열 앞에 플러스 아이콘 추가-->
                </a>
            </li>
            <li class="nav-item dropdown" sec:authorize="isAuthenticated()">
                <a class="nav-link dropdown-toggle" href="#" id="userDropdown" role="button" data-bs-toggle="dropdown"
                   aria-haspopup="true" aria-expanded="false">
                    <!-- 이미지가 존재하면 이미지를, 그렇지 않으면 아바타를 보여주도록 수정(시작) -->
                    <svg th:if="${#strings.isEmpty(account?.profile?.image)}"
                         th:data-jdenticon-value="${#authentication.name}" width="24" height="24"
                         class="rounded border bg-light"></svg><!--"프로필" 대신 아바타 이미지를 보여줌-->
                    <img th:if="${!#strings.isEmpty(account?.profile?.image)}"
                         th:src="${account.profile.image}" width="24" height="24" class="rounded border"/>
                    <!-- 이미지가 존재하면 이미지를, 그렇지 않으면 아바타를 보여주도록 수정(끝) -->
                </a>
                <div class="dropdown-menu dropdown-menu-sm-end" aria-labelledby="userDropdown">
                    <h6 class="dropdown-header">
                        <span sec:authentication="name">Username</span>
                    </h6>
                    <a class="dropdown-item" th:href="@{'/profile/' + ${#authentication.name}}">프로필</a>
                    <a class="dropdown-item">스터디</a>
                    <div class="dropdown-divider"></div>
                    <a class="dropdown-item" href="#" th:href="@{'/settings/profile'}">설정</a>
                    <form class="form-inline my-2 my-lg-0" action="#" th:action="@{/logout}" method="post">
                        <button class="dropdown-item" type="submit">로그아웃</button>
                    </form>
                </div>
            </li>
        </ul>
    </div>
</nav>

<script type="application/javascript" th:fragment="form-validation">
    (function () {
        'use strict';

        window.addEventListener('load', function () {
            // Fetch all the forms we want to apply custom Bootstrap validation styles to
            const forms = document.getElementsByClassName('needs-validation');

            // Loop over them and prevent submission
            Array.prototype.filter.call(forms, function (form) {
                form.addEventListener('submit', function (event) {
                    if (form.checkValidity() === false) {
                        event.preventDefault();
                        event.stopPropagation();
                    }
                    form.classList.add('was-validated')
                }, false)
            })
        }, false)
    }())
</script>

<svg th:fragment="svg-symbols" xmlns="http://www.w3.org/2000/svg" style="display: none;">
    <symbol id="info-fill" fill="currentColor" viewBox="0 0 16 16">
        <path d="M8 16A8 8 0 1 0 8 0a8 8 0 0 0 0 16zm.93-9.412-1 4.705c-.07.34.029.533.304.533.194 0 .487-.07.686-.246l-.088.416c-.287.346-.92.598-1.465.598-.703 0-1.002-.422-.808-1.319l.738-3.468c.064-.293.006-.399-.287-.47l-.451-.081.082-.381 2.29-.287zM8 5.5a1 1 0 1 1 0-2 1 1 0 0 1 0 2z"/>
    </symbol>
    <symbol id="check-circle-fill" fill="currentColor" viewBox="0 0 16 16">
        <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zm-3.97-3.03a.75.75 0 0 0-1.08.022L7.477 9.417 5.384 7.323a.75.75 0 0 0-1.06 1.06L6.97 11.03a.75.75 0 0 0 1.079-.02l3.992-4.99a.75.75 0 0 0-.01-1.05z"/>
    </symbol>
    <symbol id="exclamation-triangle-fill" fill="currentColor" viewBox="0 0 16 16">
        <path d="M8.982 1.566a1.13 1.13 0 0 0-1.96 0L.165 13.233c-.457.778.091 1.767.98 1.767h13.713c.889 0 1.438-.99.98-1.767L8.982 1.566zM8 5c.535 0 .954.462.9.995l-.35 3.507a.552.552 0 0 1-1.1 0L7.1 5.995A.905.905 0 0 1 8 5zm.002 6a1 1 0 1 1 0 2 1 1 0 0 1 0-2z"/>
    </symbol>
    <symbol id="exclamation-triangle-fill" fill="currentColor" viewBox="0 0 16 16">
        <path d="M8.982 1.566a1.13 1.13 0 0 0-1.96 0L.165 13.233c-.457.778.091 1.767.98 1.767h13.713c.889 0 1.438-.99.98-1.767L8.982 1.566zM8 5c.535 0 .954.462.9.995l-.35 3.507a.552.552 0 0 1-1.1 0L7.1 5.995A.905.905 0 0 1 8 5zm.002 6a1 1 0 1 1 0 2 1 1 0 0 1 0-2z"/>
    </symbol>
</svg>

<svg th:fragment="symbol-info" class="bi flex-shrink-0 me-2" width="24" height="24" role="img" aria-label="Info:">
    <use xlink:href="#info-fill"/>
</svg>
<svg th:fragment="symbol-success" class="bi flex-shrink-0 me-2" width="24" height="24" role="img" aria-label="Success:">
    <use xlink:href="#check-circle-fill"/>
</svg>
<svg th:fragment="symbol-warning" class="bi flex-shrink-0 me-2" width="24" height="24" role="img" aria-label="Warning:">
    <use xlink:href="#exclamation-triangle-fill"/>
</svg>
<svg th:fragment="symbol-danger" class="bi flex-shrink-0 me-2" width="24" height="24" role="img" aria-label="Danger:">
    <use xlink:href="#exclamation-triangle-fill"/>
</svg>

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
        <div class="col-4 text-end justify-content-end">
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
                <a class="btn btn-outline-danger" th:href="@{'/study/' + ${study.path} + '/leave'}">
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
                    <i class="fa fa-plus"></i> 모임 만들기
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
                      class="font-weight-light font-monospace badge rounded-pill bg-success me-3">
                    <a th:href="@{'/search/study?keyword=' + ${tag.title}}" class="text-decoration-none text-white">
                        <i class="fa fa-tag"></i> <span th:text="${tag.title}">Tag</span>
                    </a>
                </span>
                <span th:each="zone: ${study.zones}"
                      class="font-weight-light font-monospace badge rounded-pill bg-primary me-3">
                    <a th:href="@{'/search/study?keyword=' + ${zone.localNameOfCity}}"
                       class="text-decoration-none text-white">
                        <i class="fa fa-globe"></i> <span th:text="${zone.localNameOfCity}">City</span>
                    </a>
                </span>
            </p>
        </div>
    </div>
</div>

<div th:fragment="study-menu (studyMenu)" class="row px-3 justify-content-center bg-light">
    <nav class="col-10 nav nav-tabs">
        <a class="nav-item nav-link" href="#" th:classappend="${studyMenu == 'info'}? active"
           th:href="@{'/study/' + ${study.path}}">
            <i class="fa fa-info-circle"></i> 소개
        </a>
        <a class="nav-item nav-link" href="#" th:classappend="${studyMenu == 'members'}? active"
           th:href="@{'/study/' + ${study.path} + '/members'}">
            <i class="fa fa-user"></i> 구성원
        </a>
        <a class="nav-item nav-link" th:classappend="${studyMenu == 'events'}? active" href="#"
           th:href="@{'/study/' + ${study.path} + '/events'}">
            <i class="fa fa-calendar"></i> 모임
        </a>
        <a sec:authorize="isAuthenticated()" th:if="${study.isManager(#authentication.principal)}"
           class="nav-item nav-link" th:classappend="${studyMenu == 'settings'}? active" href="#"
           th:href="@{'/study/' + ${study.path} + '/settings/description'}">
            <i class="fa fa-cog"></i> 설정
        </a>
    </nav>
</div>

<script th:fragment="tooltip" type="application/javascript">
    $(function () {
        $('[data-toggle="tooltip"]').tooltip()
    })
</script>

<div th:fragment="member-list (members, isManager)" class="row px-3 justify-content-center">
    <ul class="list-unstyled col-10">
        <li class="d-flex mt-3" th:each="member: ${members}">
            <div class="flex-shrink-0">
                <svg th:if="${#strings.isEmpty(member?.profile?.image)}" th:data-jdenticon-value="${member.nickname}"
                     width="64" height="64" class="rounded border bg-light me-3"></svg>
                <img th:if="${!#strings.isEmpty(member?.profile?.image)}" th:src="${member?.profile?.image}" width="64"
                     height="64" class="rounded border me-3"/>
            </div>
            <div class="flex-grow-1 ms-3">
                <h5 class="mt-0 mb-1">
                    <span th:text="${member.nickname}"></span>
                    <span th:if="${isManager}" class="badge bg-primary">관리자</span>
                </h5>
                <span th:text="${member.profile.bio}"></span>
            </div>
        </li>
    </ul>
</div>

<div th:fragment="study-settings-menu (currentMenu)" class="list-group">
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'description'}? active"
       href="#" th:href="@{'/study/' + ${study.path} + '/settings/description'}">소개</a>
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'image'}? active"
       href="#" th:href="@{'/study/' + ${study.path} + '/settings/banner'}">배너 이미지</a>
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'tags'}? active"
       href="#" th:href="@{'/study/' + ${study.path} + '/settings/tags'}">스터디 주제</a>
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'zones'}? active"
       href="#" th:href="@{'/study/' + ${study.path} + '/settings/zones'}">활동 지역</a>
    <a class="list-group-item list-group-item-action list-group-item-danger"
       th:classappend="${currentMenu == 'study'}? active"
       href="#" th:href="@{'/study/' + ${study.path} + '/settings/study'}">스터디</a>
</div>

<div th:fragment="editor-script">
    <script src="/node_modules/tinymce/tinymce.min.js"></script>
    <script>
        tinymce.init({
            selector: 'textarea#fullDescription'
        })
    </script>
</div>

<div th:fragment="message" th:if="${message}" class="alert alert-info alert-dismissible fade show mt-3" role="alert">
    <svg class="bi flex-shrink-0 me-2" width="24" height="24" role="img" aria-label="Success:">
        <use xlink:href="#check-circle-fill"/>
    </svg>
    <span th:text="${message}">완료</span>
    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
</div>

<script type="application/javascript" th:inline="javascript" th:fragment="ajax-csrf-header">
    $(function () {
        var csrfToken = /*[[${_csrf.token}]]*/ null;
        var csrfHeader = /*[[${_csrf.headerName}]]*/ null;
        $(document).ajaxSend(function (e, xhr, options) {
            xhr.setRequestHeader(csrfHeader, csrfToken);
        });
    });
</script>

<div th:fragment="update-tags (baseUrl)">
    <script src="/node_modules/@yaireo/tagify/dist/tagify.min.js"></script>
    <script type="application/javascript" th:inline="javascript">
        $(function () {
            function tagRequest(url, tagTitle) {
                $.ajax({
                    dataType: "json",
                    autocomplete: {
                        enabled: true,
                        rightKey: true,
                    },
                    contentType: "application/json; charset=utf-8",
                    method: "POST",
                    url: "[(${baseUrl})]" + url,
                    data: JSON.stringify({'tagTitle': tagTitle})
                }).done(function (data, status) {
                    console.log("${data} and status is ${status}");
                });
            }

            function onAdd(e) {
                tagRequest("/add", e.detail.data.value);
            }

            function onRemove(e) {
                tagRequest("/remove", e.detail.data.value);
            }

            var tagInput = document.querySelector("#tags");
            var tagify = new Tagify(tagInput, {
                pattern: /^.{0,20}$/,
                whitelist: JSON.parse(document.querySelector("#whitelist").textContent),
                dropdown: {
                    enabled: 1,
                }
            });
            tagify.on("add", onAdd);
            tagify.on("remove", onRemove);
            tagify.DOM.input.classList.add('form-control');
            tagify.DOM.scope.parentNode.insertBefore(tagify.DOM.input, tagify.DOM.scope);
        });
    </script>
</div>

<div th:fragment="update-zones (baseUrl)">
    <script src="/node_modules/@yaireo/tagify/dist/tagify.min.js"></script>
    <script type="application/javascript">
        $(function () {
            function tagRequest(url, zoneName) {
                $.ajax({
                    dataType: "json",
                    autocomplete: {
                        enabled: true,
                        rightKey: true,
                    },
                    contentType: "application/json; charset=utf-8",
                    method: "POST",
                    url: "[(${baseUrl})]" + url,
                    data: JSON.stringify({'zoneName': zoneName})
                }).done(function (data, status) {
                    console.log("${data} and status is ${status}");
                });
            }

            function onAdd(e) {
                tagRequest("/add", e.detail.data.value);
            }

            function onRemove(e) {
                tagRequest("/remove", e.detail.data.value);
            }

            var tagInput = document.querySelector("#zones");

            var tagify = new Tagify(tagInput, {
                enforceWhitelist: true,
                whitelist: JSON.parse(document.querySelector("#whitelist").textContent),
                dropdown: {
                    enabled: 1, // suggest tags after a single character input
                } // map tags
            });

            tagify.on("add", onAdd);
            tagify.on("remove", onRemove);

            // add a class to Tagify's input element
            tagify.DOM.input.classList.add('form-control');
            // re-place Tagify's input element outside of the  element (tagify.DOM.scope), just before it
            tagify.DOM.scope.parentNode.insertBefore(tagify.DOM.input, tagify.DOM.scope);
        });
    </script>
</div>

<div th:fragment="date-time">
    <script src="/node_modules/moment/min/moment-with-locales.min.js"></script>
    <script type="application/javascript">
        $(function () {
            moment.locale('ko');
            $(".date-time").text(function (index, dateTime) {
                return moment(dateTime, "YYYY-MM-DD`T`hh:mm").format('LLL');
            });
            $(".date").text(function (index, dateTime) {
                return moment(dateTime, "YYYY-MM-DD`T`hh:mm").format('LL');
            });
            $(".weekday").text(function (index, dateTime) {
                return moment(dateTime, "YYYY-MM-DD`T`hh:mm").format('dddd');
            });
            $(".time").text(function (index, dateTime) {
                return moment(dateTime, "YYYY-MM-DD`T`hh:mm").format('LT');
            });
            $(".calendar").text(function (index, dateTime) {
                return moment(dateTime, "YYYY-MM-DD`T`hh:mm").calendar();
            });
            $(".fromNow").text(function (index, dateTime) {
                return moment(dateTime, "YYYY-MM-DD`T`hh:mm").fromNow();
            });
            $(".date-weekday-time").text(function (index, dateTime) {
                return moment(dateTime, "YYYY-MM-DD`T`hh:mm").format('LLLL');
            });
        })
    </script>
</div>

<div th:fragment="event-form (mode, action)">
    <div class="py-5 text-center">
        <h2><a th:href="@{'/study/' + ${study.path}}"><span th:text="${study.title}">스터디</span></a> /
            <span th:if="${mode == 'edit'}" th:text="${event.title}"></span>
            <span th:if="${mode == 'new'}">새 모임 만들기</span>
    </div>
    <div class="row justify-content-center">
        <form class="needs-validation col-sm-10" th:action="@{${action}}"
              th:object="${eventForm}" method="post" novalidate>
            <div class="form-group mt-3">
                <label for="title">모임 이름</label>
                <input id="title" type="text" th:field="*{title}" class="form-control"
                       placeholder="모임 이름" aria-describedby="titleHelp" required>
                <small id="titleHelp" class="form-text text-muted">
                    모임 이름을 50자 이내로 입력하세요.
                </small>
                <small class="invalid-feedback">모임 이름을 입력하세요.</small>
                <small class="form-text text-danger" th:if="${#fields.hasErrors('title')}"
                       th:errors="*{title}">Error</small>
            </div>
            <div class="form-group mt-3" th:if="${mode == 'new'}">
                <label for="eventType">모집 방법</label>
                <select th:field="*{eventType}" class="form-select me-sm-2" id="eventType"
                        aria-describedby="eventTypeHelp">
                    <option th:value="FCFS">선착순</option>
                    <option th:value="CONFIRMATIVE">관리자 확인</option>
                </select>
                <small id="eventTypeHelp" class="form-text text-muted">
                    두가지 모집 방법이 있습니다.<br/>
                    <strong>선착순</strong>으로 모집하는 경우, 모집 인원 이내의 접수는 자동으로 확정되며, 제한 인원을 넘는 신청은 대기 신청이 되며 이후에 확정된 신청 중에 취소가
                    발생하면 선착순으로 대기 신청자를 확정 신청자도 변경합니다. 단, 등록 마감일 이후에는 취소해도 확정 여부가 바뀌지 않습니다.<br/>
                    <strong>관리자 확인</strong>으로 모집하는 경우, 모임 및 스터디 관리자가 모임 신청 목록을 조회하고 직접 확정 여부를 정할 수 있습니다. 등록 마감일 이후에는 변경할
                    수 없습니다.
                </small>
            </div>
            <div class="row">
                <div class="form-group col-md-3 mt-3">
                    <label for="limitOfEnrollments">모집 인원</label>
                    <input id="limitOfEnrollments" type="number" th:field="*{limitOfEnrollments}" class="form-control"
                           placeholder="0"
                           aria-describedby="limitOfEnrollmentsHelp">
                    <small id="limitOfEnrollmentsHelp" class="form-text text-muted">
                        최대 수용 가능한 모임 참석 인원을 설정하세요. 최소 2인 이상 모임이어야 합니다.
                    </small>
                    <small class="invalid-feedback">모임 신청 마감 일시를 입력하세요.</small>
                    <small class="form-text text-danger" th:if="${#fields.hasErrors('limitOfEnrollments')}"
                           th:errors="*{limitOfEnrollments}">Error</small>
                </div>
                <div class="form-group col-md-3 mt-3">
                    <label for="endEnrollmentDateTime">등록 마감 일시</label>
                    <input id="endEnrollmentDateTime" type="datetime-local" th:field="*{endEnrollmentDateTime}"
                           class="form-control"
                           aria-describedby="endEnrollmentDateTimeHelp" required>
                    <small id="endEnrollmentDateTimeHelp" class="form-text text-muted">
                        등록 마감 이전에만 스터디 모임 참가 신청을 할 수 있습니다.
                    </small>
                    <small class="invalid-feedback">모임 신청 마감 일시를 입력하세요.</small>
                    <small class="form-text text-danger" th:if="${#fields.hasErrors('endEnrollmentDateTime')}"
                           th:errors="*{endEnrollmentDateTime}">Error</small>
                </div>
                <div class="form-group col-md-3 mt-3">
                    <label for="startDateTime">모임 시작 일시</label>
                    <input id="startDateTime" type="datetime-local" th:field="*{startDateTime}" class="form-control"
                           aria-describedby="startDateTimeHelp" required>
                    <small id="startDateTimeHelp" class="form-text text-muted">
                        모임 시작 일시를 입력하세요. 상세한 모임 일정은 본문에 적어주세요.
                    </small>
                    <small class="invalid-feedback">모임 시작 일시를 입력하세요.</small>
                    <small class="form-text text-danger" th:if="${#fields.hasErrors('startDateTime')}"
                           th:errors="*{startDateTime}">Error</small>
                </div>
                <div class="form-group col-md-3 mt-3">
                    <label for="startDateTime">모임 종료 일시</label>
                    <input id="endDateTime" type="datetime-local" th:field="*{endDateTime}" class="form-control"
                           aria-describedby="endDateTimeHelp" required>
                    <small id="endDateTimeHelp" class="form-text text-muted">
                        모임 종료 일시가 지나면 모임은 자동으로 종료 상태로 바뀝니다.
                    </small>
                    <small class="invalid-feedback">모임 종료 일시를 입력하세요.</small>
                    <small class="form-text text-danger" th:if="${#fields.hasErrors('endDateTime')}"
                           th:errors="*{endDateTime}">Error</small>
                </div>
            </div>
            <div class="form-group mt-3">
                <label for="fullDescription">모임 설명</label>
                <textarea id="fullDescription" type="textarea" th:field="*{description}" class="editor form-control"
                          placeholder="모임을 자세히 설명해 주세요." aria-describedby="fullDescriptionHelp" required></textarea>
                <small id="fullDescriptionHelp" class="form-text text-muted">
                    모임에서 다루는 주제, 장소, 진행 방식 등을 자세히 적어 주세요.
                </small>
                <small class="invalid-feedback">모임 설명을 입력하세요.</small>
                <small class="form-text text-danger" th:if="${#fields.hasErrors('description')}"
                       th:errors="*{description}">Error</small>
            </div>
            <div class="form-group mt-3 d-grid">
                <button class="btn btn-primary btn-block" type="submit"
                        aria-describedby="submitHelp" th:text="${mode == 'edit' ? '모임 수정' : '모임 만들기'}">모임 수정
                </button>
                </button>
            </div>
        </form>
    </div>
</div>

<ul th:fragment="notification-list (notifications)" class="list-group list-group-flush">
    <a href="#" th:href="@{${notification.link}}" th:each="notification: ${notifications}"
       class="list-group-item list-group-item-action">
        <div class="d-flex w-100 justify-content-between">
            <small class="text-muted" th:text="${notification.title}">Noti title</small>
            <small class="fromNow text-muted" th:text="${notification.created}">3 days ago</small>
        </div>
        <p th:text="${notification.message}" class="text-left mb-0 mt-1">message</p>
    </a>
</ul>

</html>
```

</details>

## 테스트

애플리케이션 실행 후 jpa를 검색하여 화면을 확인합니다.

그냥 검색했을 때는 스터디 공개 오름차순으로 결과가 나타나고 하일라이트 및 페이지 내비게이션이 정확히 동작하는 것을 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/69-01.png)

정렬 조건을 수정하여 검색하면, 
![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/69-02.png)
역시 정확히 동작하는 것을 확인할 수 있습니다.
![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/69-03.png)

멤버수 내림차순 정렬을 테스트하기 위해 기존 스터디에 가입한 뒤 다시 테스트 해보았습니다.

먼저 지난번에 생성한 테스트 계정으로 스터디를 가입하고,

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/69-04.png)

다시 멤버수로 정렬하여 조회하였더니 가입한 스터디가 가장 먼저 노출되는 것을 확인하였습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/69-05.png)