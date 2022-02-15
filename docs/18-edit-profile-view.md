![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 878b1db)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 878b1db
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

프로필 수정을 위한 뷰를 구현합니다.

## 컨트롤러 구현

먼저 페이지에 진입할 수 있게 컨트롤러를 구현합니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/settings/controller/SettingsController.java`

```java
package io.lcalmsky.app.settings.controller;

import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.support.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SettingsController {

    @GetMapping("/settings/profile")
    public String profileUpdateForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(Profile.from(account));
        return "settings/profile";
    }
}
```

현재 인증된 사용자만 접근할 수 있게 @CurrentUser 애너테이션을 사용하였고, model로 계정과 프로필 정보를 넘겨준 뒤 페이지를 반환하게 하였습니다.

## Profile Form 클래스 생성

위에서 사용된 Profile 클래스는 이전에 Account 클래스에 Embedded 되어있는 클래스가 아닌 Form 용 클래스 입니다.

따라서 별도로 생성해주어야 합니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/settings/controller/Profile.java`

```java
package io.lcalmsky.app.settings.controller;

import io.lcalmsky.app.account.domain.entity.Account;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile {
    private String bio;
    private String url;
    private String job;
    private String location;

    public static Profile from(Account account) {
        return new Profile(account);
    }

    protected Profile(Account account) {
        this.bio = Optional.ofNullable(account.getProfile()).map(Account.Profile::getBio).orElse(null);
        this.url = Optional.ofNullable(account.getProfile()).map(Account.Profile::getUrl).orElse(null);
        this.job = Optional.ofNullable(account.getProfile()).map(Account.Profile::getJob).orElse(null);
        this.location = Optional.ofNullable(account.getProfile()).map(Account.Profile::getLocation).orElse(null);
    }
}
```

이전 포스팅을 보고 따라서 작성하셨다면 Profile 클래스에서 에러가 발생할 텐데요, 약간의 수정이 필요합니다.

## 기존 소스 코드 수정

다음으로 뷰를 구현하기 전에 수정해야 할 부분이 있습니다. (강의를 착실하게 따라가지 않은 제 잘못 😭)

URL을 여러 개 입력받아 DB에 저장했다가 다시 화면에 표시해주려고 했었는데, 자꾸 이 부분 때문에 버그가 발생하고 그걸 찾아서 수정하는데 시간이 너무 오래 소모돼서 강의와 동일하게 URL을 List 타입에서 String 타입으로 변경하였습니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/account/domain/entity/Account.java`

```java
// 생략
public class Account extends AuditingEntity {
    // 생략
    @PostLoad
    private void init() { // (1)
        if (profile == null) {
            profile = new Profile();
        }
        if (notificationSetting == null) {
            notificationSetting = new NotificationSetting();
        }
    }
    @Embeddable
    @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder @Getter @ToString
    public static class Profile {
        private String bio;
        private String url; // (2)
        private String job;
        private String location;
        private String company;

        @Lob @Basic(fetch = FetchType.EAGER)
        private String image;
    }
    // 생략
}
```

1. @Embedded를 사용했을 때 자동으로 초기화되지 않아 템플릿 로드시 에러가 발생하여, Entity 로드 이후 null일 경우 객체를 생성하게 하였습니다.
2. List<String> 타입에서 String 타입으로 변경하였습니다.

<details>
<summary>Account.java 전체 보기</summary>

```java
package io.lcalmsky.app.account.domain.entity;

import io.lcalmsky.app.domain.entity.AuditingEntity;
import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder @Getter @ToString
public class Account extends AuditingEntity {

    @Id @GeneratedValue
    @Column(name = "account_id")
    private Long id;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String nickname;

    private String password;

    private boolean isValid;

    private String emailToken;

    private LocalDateTime joinedAt;

    @Embedded
    private Profile profile = new Profile();

    @Embedded
    private NotificationSetting notificationSetting = new NotificationSetting();

    private LocalDateTime emailTokenGeneratedAt;

    public void generateToken() {
        this.emailToken = UUID.randomUUID().toString();
        this.emailTokenGeneratedAt = LocalDateTime.now();
    }

    public boolean enableToSendEmail() {
        return this.emailTokenGeneratedAt.isBefore(LocalDateTime.now().minusMinutes(5));
    }

    public void verified() {
        this.isValid = true;
        joinedAt = LocalDateTime.now();
    }

    @PostLoad
    private void init() {
        if (profile == null) {
            profile = new Profile();
        }
        if (notificationSetting == null) {
            notificationSetting = new NotificationSetting();
        }
    }
    @Embeddable
    @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder @Getter @ToString
    public static class Profile {
        private String bio;
        private String url;
        private String job;
        private String location;
        private String company;

        @Lob @Basic(fetch = FetchType.EAGER)
        private String image;
    }
    @Embeddable
    @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder @Getter @ToString
    public static class NotificationSetting {
        private boolean studyCreatedByEmail;
        private boolean studyCreatedByWeb;
        private boolean studyRegistrationResultByEmail;
        private boolean studyRegistrationResultByWeb;
        private boolean studyUpdatedByEmail;
        private boolean studyUpdatedByWeb;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        Account account = (Account) o;
        return id != null && Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
```

</details>

Account 클래스가 변경되었으므로 영향 받는 모든 곳을 수정해줘야하는데 다행히 한 개의 html 파일만 수정하면 됩니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/resources/templates/account/profile.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<body class="bg-light">
<div th:replace="fragments.html :: navigation-bar"></div>
<div class="container">
    <div class="row mt-5 justify-content-center">
        <div class="col-2">
        </div>
            <!-- 생략 -->
        <div class="col-8">
            <!-- 생략 -->
        </div>
        <div class="row mt-3 justify-content-center">
            <div class="col-2">
            <!-- 생략-->
            </div>
            <div class="col-8">
                <div class="tab-content" id="v-pills-tabContent">
                    <div class="tab-pane fade show active" id="v-pills-profile" role="tabpanel"
                         aria-labelledby="v-pills-home-tab">
                        <p th:if="${!#strings.isEmpty(account.profile.url)}"> <!-- 이 부분에서 lists.isEmpty를 체크하는 부분을 strings.isEmpty로 수정하였습니다.-->
                            <span style="...">
                                <i class="fa fa-link col-1"></i>
                            </span>
                            <span th:text="${account.profile.url}" class="col-11"></span>
                        </p>
                        <!-- 생략-->
                    </div>
                    <div class="tab-pane fade" id="v-pills-study" role="tabpanel" aria-labelledby="v-pills-profile-tab">
                        Study
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
```

주석으로 수정한 부분을 표기하였습니다.

<details>
<summary>profile.html 전체 보기</summary>

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<body class="bg-light">
<div th:replace="fragments.html :: navigation-bar"></div>
<div class="container">
    <div class="row mt-5 justify-content-center">
        <div class="col-2">
            <!-- avatar -->
            <svg th:if="${#strings.isEmpty(account.profile.image)}" class="img-fluid float-left rounded img-thumbnail"
                 th:data-jdenticon-value="${account.nickname}" width="125" height="125"></svg>
            <svg th:if="${!#strings.isEmpty(account.profile.image)}" class="img-fluid float-left rounded img-thumbnail"
                 th:src="${account.profile.image}" width="125" height="125"></svg>
        </div>
        <div class="col-8">
            <!-- nickname-->
            <h1 class="display-4" th:text="${account.nickname}">nickname</h1>
            <!-- bio -->
            <p class="lead" th:if="${!#strings.isEmpty(account.profile.bio)}" th:text="${account.profile.bio}">bio</p>
            <p class="lead" th:if="${#strings.isEmpty(account.profile.bio) && isOwner}">한 줄 소개를 추가해주세요.</p>
        </div>
        <div class="row mt-3 justify-content-center">
            <div class="col-2">
                <div class="nav flex-column nav-pills" id="v-pills-tab" role="tablist" aria-orientation="vertical">
                    <a class="nav-link active" id="v-pills-intro-tab" data-bs-toggle="pill" href="#v-pills-profile"
                       role="tab" aria-controls="v-pills-profile" aria-selected="true">소개</a>
                    <a class="nav-link" id="v-pills-study-tab" data-bs-toggle="pill" href="#v-pills-study" role="tab"
                       aria-controls="v-pills-study" aria-selected="false">스터디</a>
                </div>
            </div>
            <div class="col-8">
                <div class="tab-content" id="v-pills-tabContent">
                    <div class="tab-pane fade show active" id="v-pills-profile" role="tabpanel"
                         aria-labelledby="v-pills-home-tab">
                        <p th:if="${!#strings.isEmpty(account.profile.url)}">
                            <span style="...">
                                <i class="fa fa-link col-1"></i>
                            </span>
                            <span th:text="${account.profile.url}" class="col-11"></span>
                        </p>
                        <p th:if="${!#strings.isEmpty(account.profile.job)}">
                            <span style="...">
                                <i class="fa fa-briefcase col-1"></i>
                            </span>
                            <span th:text="${account.profile.job}" class="col-9"></span>
                        </p>
                        <p th:if="${!#strings.isEmpty(account.profile.location)}">
                            <span style="...">
                                <i class="fa fa-location-arrow col-1"></i>
                            </span>
                            <span th:text="${account.profile.location}" class="col-9"></span>
                        </p>
                        <p th:if="${isOwner}">
                            <span style="font-size: 20px">
                                <i class="fa fa-envelope-o col-1"></i>
                            </span>
                            <span th:text="${account.email}" class="col-9"></span>
                        </p>
                        <p th:if="${isOwner || account.valid}">
                            <span style="...">
                                <i class="fa fa-calendar-o col-1"></i>
                            </span>
                            <span th:if="${isOwner && !account.valid}" class="col-9">
                                <a href="#"
                                   th:href="@{'/check-email?email=' + ${account.email}}">가입을 완료하려면 이메일을 확인하세요.</a>
                            </span>
                            <span th:text="${#temporals.format(account.joinedAt, 'yyyy년 M월 가입')}" class="col-9"></span>
                        </p>
                        <div th:if="${isOwner}">
                            <a class="btn btn-outline-primary" href="#" th:href="@{/settings/profile}">프로필 수정</a>
                        </div>
                    </div>

                    <div class="tab-pane fade" id="v-pills-study" role="tabpanel" aria-labelledby="v-pills-profile-tab">
                        Study
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
```

</details>

## 프로필 뷰 작성

프로필 뷰에서 작성할 fragment를 먼저 추가해줍니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/resources/templates/fragments.html`

```html
<!DOCTYPE html>
<html lang="en"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<!-- 생략 --> 
<div th:fragment="settings-menu (currentMenu)" class="list-group">
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'profile'} ? active" href="#" th:href="@{/settings/profile}">프로필</a>
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'password'} ? active" href="#" th:href="@{/settings/password}">패스워드</a>
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'notification'} ? active" href="#" th:href="@{/settings/notification}">알림 설정</a>
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'tags'} ? active" href="#" th:href="@{/settings/tags}">관심 주제</a>
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'zones'} ? active" href="#" th:href="@{/settings/zones}">활동 지역</a>
</div>
<!-- 생략 --> 
</html>
```

프로필 화면의 왼쪽을 차지하게 될 메뉴입니다. 프로필, 패스워드, 알림 설정, 관심 주제, 활동 지역 페이지가 각각 생성될 것이라서 미리 fragment로 분리하였습니다.

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
    <link rel="stylesheet" href="/node_modules/bootstrap/dist/css/bootstrap.min.css"/>
    <link rel="stylesheet" href="/node_modules/font-awesome/css/font-awesome.min.css"/> <!--font-awesome 추가-->
    <script src="/node_modules/jdenticon/dist/jdenticon.min.js"></script> <!--jdenticon script 추가-->
    <script src="/node_modules/jquery/dist/jquery.min.js"></script> <!--index.html에서 옮김-->
    <script src="/node_modules/bootstrap/dist/js/bootstrap.bundle.min.js"></script> <!--index.html에서 옮김-->
    <style>
        .container {
            max-width: 100%;
        }
    </style>
</head>

<footer th:fragment="footer">
    <div class="row justify-content-center">
        <small class="d-flex mb-3 text-muted" style="justify-content: center">Webluxible &copy; 2021</small>
    </div>
</footer>

<div th:fragment="settings-menu (currentMenu)" class="list-group">
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'profile'} ? active" href="#" th:href="@{/settings/profile}">프로필</a>
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'password'} ? active" href="#" th:href="@{/settings/password}">패스워드</a>
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'notification'} ? active" href="#" th:href="@{/settings/notification}">알림 설정</a>
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'tags'} ? active" href="#" th:href="@{/settings/tags}">관심 주제</a>
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'zones'} ? active" href="#" th:href="@{/settings/zones}">활동 지역</a>
</div>

<nav th:fragment="navigation-bar" class="navbar navbar-expand-sm navbar-dark bg-dark">
    <a class="navbar-brand" href="/" th:href="@{/}">
        <img src="/images/logo.png" width="30" height="30">
    </a>
    <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-target="#navbarSupportedContent"
            aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
        <span class="navbar-toggler-icon"></span>
    </button>

    <div class="collapse navbar-collapse" id="navbarSupportedContent">
        <ul class="navbar-nav mr-auto">
            <li class="nav-item">
                <form th:action="@{/search/study}" class="form-inline" method="get">
                    <input class="form-control mr-sm-2" name="keyword" type="search" placeholder="스터디 찾기"
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
                    <i class="fa fa-bell-o" aria-hidden="true"></i> <!--"알림" 문자열을 종 모양 아이콘으로 수정-->
                </a>
            </li>
            <li class="nav-item" sec:authorize="isAuthenticated()">
                <a class="nav-link btn btn-outline-primary" th:href="@{/notifications}">
                    <i class="fa fa-plus" aria-hidden="true"></i> 스터디 개설 <!--"스터디 개설" 문자열 앞에 플러스 아이콘 추가-->
                </a>
            </li>
            <li class="nav-item dropdown" sec:authorize="isAuthenticated()">
                <a class="nav-link dropdown-toggle" href="#" id="userDropdown" role="button" data-bs-toggle="dropdown"
                   aria-haspopup="true" aria-expanded="false">
                    <svg data-jdenticon-value="user127" th:data-jdenticon-value="${#authentication.name}" width="24"
                         height="24" class="rounded border bg-light"></svg><!--"프로필" 대신 아바타 이미지를 보여줌-->
                </a>
                <div class="dropdown-menu dropdown-menu-sm-right" aria-labelledby="userDropdown">
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

</html>
```

</details>

다음은 프로필 뷰를 작성합니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/resources/templates/settings/profile.html`

account 하위에도 profile.html 파일이 있으니 settings 하위에 만들어서 구분해줍니다.

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<body class="bg-light">
<div th:replace="fragments.html :: navigation-bar"></div>
<div class="container">
    <div class="row mt-5 justify-content-center">
        <div class="col-2">
            <div th:replace="fragments.html::settings-menu (currentMenu='profile')"></div>
        </div>
        <div class="col-8">
            <h2 class="col-sm-12" th:text="${account.nickname}">nickname</h2>
            <div class="row mt-3">
                <form class="col-sm-6" action="#" th:action="@{/settings/profile}" th:object="${profile}" method="post"
                      novalidate>
                    <div class="form-group">
                        <label for="bio">한 줄 소개</label>
                        <input id="bio" type="text" th:field="*{bio}" class="form-control"
                               placeholder="간략한 한 줄을 추가해 보세요." aria-describedby="bioHelp" required/>
                        <small id="bioHelp" class="form-text text-muted">
                            35자 이내로 입력하세요.
                        </small>
                        <small class="form-text text-danger" th:if="${#fields.hasErrors('bio')}" th:errors="*{bio}">
                            35자를 초과하였습니다.
                        </small>
                    </div>
                    <div class="form-group">
                        <label for="url">링크</label>
                        <input id="url" type="text" th:field="*{url}" class="form-control"
                               placeholder="http://www.example.com" aria-describedby="urlHelp" required/>
                        <small id="urlHelp" class="form-text text-muted">
                            블로그, GitHub 등 본인을 표현할 수 있는 링크를 추가하세요.
                        </small>
                        <small class="form-text text-danger" th:if="${#fields.hasErrors('url')}" th:errors="*{bio}">
                            올바른 URL이 아닙니다.
                        </small>
                    </div>
                    <div class="form-group">
                        <label for="job">직업</label>
                        <input id="job" type="text" th:field="*{job}" class="form-control"
                               placeholder="어떤 일을 하고 계신가요?" aria-describedby="jobHelp" required/>
                        <small id="jobHelp" class="form-text text-muted">
                            ex) 개발자, 학생, 취준생, ...
                        </small>
                    </div>
                    <div class="form-group">
                        <label for="location">활동 지역</label>
                        <input id="location" type="text" th:field="*{location}" class="form-control"
                               placeholder="서울, 경기 등" aria-describedby="locationHelp" required/>
                        <small id="locationHelp" class="form-text text-muted">
                            주요 활동 지역(사는 곳 또는 직장 위치)의 도시 이름을 입력하세요.
                        </small>
                    </div>
                    <div class="form-group">
                        <button class="button btn-primary btn-block" type="submit" aria-describedby="submitHelp">수정하기
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
</body>
</html>
```

앞서 분리한 fragment를 추가해주고 우측에 표시할 form을 작성했습니다.

각 div 별로 label, input, 설명 메시지, 에러가 발생했을 경우 에러 메시지로 구성되어 있습니다.

## 테스트

여기까지 작성이 완료되었다면 애플리케이션을 실행해 화면이 잘 표시되는지 확인합니다.

가입 후 프로필 메뉴에 진입해 프로필 수정을 클릭했을 때 아래처럼 표시되면 성공입니다!

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/18-01.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/18-02.png)