![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: bd04b9d)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout bd04b9d
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

스터디 구성원 조회 기능을 구현합니다.

여태까지 계속 해왔던 것이기 때문에 자세한 설명은 생략하겠습니다.

## 엔드포인트 추가

스터디 조회 화면에서 구성원 메뉴를 클릭했을 때 관련 페이지로 이동하도록 컨트롤러를 수정해줍니다.

`/src/main/java/io/lcalmsky/app/study/endpoint/StudyController.java`

```java
// 생략
@Controller
@RequiredArgsConstructor
public class StudyController {
    // 생략
    @GetMapping("/study/{path}/members")
    public String viewStudyMembers(@CurrentUser Account account, @PathVariable String path, Model model) {
        model.addAttribute(account);
        model.addAttribute(studyRepository.findByPath(path));
        return "study/members";
    }
}
```

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

    @GetMapping("/study/{path}/members")
    public String viewStudyMembers(@CurrentUser Account account, @PathVariable String path, Model model) {
        model.addAttribute(account);
        model.addAttribute(studyRepository.findByPath(path));
        return "study/members";
    }
}
```

</details>

## 뷰 구현

먼저 `fragments`를 수정해줍니다.

`/src/main/resources/templates/fragments.html`

```html
<!DOCTYPE html>
<html lang="en"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<!--생략-->
<div th:fragment="member-list (members, isManager)" class="row px-3 justify-content-center">
    <ul class="list-unstyled col-10">
        <li class="d-flex mt-3" th:each="member: ${members}">
            <div class="flex-shrink-0">
                <svg th:if="${#strings.isEmpty(member?.profile?.image)}" th:data-jdenticon-value="${member.nickname}" width="64" height="64" class="rounded border bg-light me-3"></svg>
                <img th:if="${!#strings.isEmpty(member?.profile?.image)}" th:src="${member?.profile?.image}" width="64" height="64" class="rounded border me-3"/>
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
</html>
```

<details>
<summary>fragment.html 전체 보기</summary>

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
                    <i class="fa fa-bell-o" aria-hidden="true"></i> <!--"알림" 문자열을 종 모양 아이콘으로 수정-->
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
                      class="font-weight-light text-monospace badge badge-pill badge-info me-3">
                    <a th:href="@{'/search/tag/' + ${tag.title}}" class="text-decoration-none text-white">
                        <i class="fa fa-tag"></i> <span th:text="${tag.title}">Tag</span>
                    </a>
                </span>
                <span th:each="zone: ${study.zones}" class="font-weight-light text-monospace badge badge-primary me-3">
                    <a th:href="@{'/search/zone/' + ${zone.id}}" class="text-decoration-none text-white">
                        <i class="fa fa-globe"></i> <span th:text="${zone.localNameOfCity}">City</span>
                    </a>
                </span>
            </p>
        </div>
    </div>
</div>

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

<script th:fragment="tooltip" type="application/javascript">
    $(function () {
        $('[data-toggle="tooltip"]').tooltip()
    })
</script>

<div th:fragment="member-list (members, isManager)" class="row px-3 justify-content-center">
    <ul class="list-unstyled col-10">
        <li class="d-flex mt-3" th:each="member: ${members}">
            <div class="flex-shrink-0">
                <svg th:if="${#strings.isEmpty(member?.profile?.image)}" th:data-jdenticon-value="${member.nickname}" width="64" height="64" class="rounded border bg-light me-3"></svg>
                <img th:if="${!#strings.isEmpty(member?.profile?.image)}" th:src="${member?.profile?.image}" width="64" height="64" class="rounded border me-3"/>
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

</html>
```

</details>

현재 계정과 스터디 정보를 전달받아서 리스트 형태로 보여주는데 프로필 사진과, 닉네임, 관리자 여부에 따라 태그를 표현해주고, 한 줄 소개도 보여줍니다.

다음으로 템플릿 경로에 `study` 하위에 `members.html` 파일을 생성하고 작성합니다.

`/src/main/resources/templates/study/members.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<body class="bg-light">
<div th:replace="fragments.html :: navigation-bar"></div>
<div th:replace="fragments.html :: study-banner"></div>
<div class="container">
    <div th:replace="fragments.html :: study-info"></div>
    <div th:replace="fragments.html :: study-menu('members')"></div>

    <div th:replace="fragments.html :: member-list(${study.managers},${true})"></div>
    <div th:replace="fragments.html :: member-list(${study.members},${false})"></div>

    <div th:replace="fragments.html :: footer"></div>
</div>
<script th:replace="fragments.html :: tooltip"></script>
</body>
</html>
```

---

> 제가 프론트엔드 쪽을 너무 몰라서 그동안 Bootstrap 4.x 버전 대에서만 동작하는 클래스들을 그대로 사용한 것이 너무 많이 있었습니다.  
> 앞으로도 발견하면 조금씩 수정할 예정인데, 이미 지나간 포스팅을 모두 바꾸기엔 한계가 있어 어떤 것들을 수정했는지만 간단히 나열할 예정입니다.  
> 
> **fragments.html 수정 사항**
> * ml-* -> ms-* 로 수정
> * mr-* -> me-* 로 수정
> * dropdown-menu-sm-right -> dropdown-menu-sm-end 로 수정

## 테스트 코드 작성

```java
// 생략
@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class StudyControllerTest {
    // 생략
    @Test
    @DisplayName("스터디 멤버 뷰")
    @WithAccount("jaime")
    void studyMemberView() throws Exception {
        Account account = accountRepository.findByNickname("jaime");
        String studyPath = "study-path";
        studyService.createNewStudy(StudyForm.builder()
                .path(studyPath)
                .title("study-title")
                .shortDescription("short-description")
                .fullDescription("full-description")
                .build(), account);
        mockMvc.perform(get("/study/" + studyPath + "/members"))
                .andExpect(status().isOk())
                .andExpect(view().name("study/members"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("study"));
    }
}
```

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

    @Test
    @DisplayName("스터디 멤버 뷰")
    @WithAccount("jaime")
    void studyMemberView() throws Exception {
        Account account = accountRepository.findByNickname("jaime");
        String studyPath = "study-path";
        studyService.createNewStudy(StudyForm.builder()
                .path(studyPath)
                .title("study-title")
                .shortDescription("short-description")
                .fullDescription("full-description")
                .build(), account);
        mockMvc.perform(get("/study/" + studyPath + "/members"))
                .andExpect(status().isOk())
                .andExpect(view().name("study/members"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("study"));
    }
}
```

</details>

## 테스트

> 앞으로 별 다른 설명이 없어도 `postgres DB`를 실행하고 `local-db` 프로파일로 실행합니다.

애플리케이션을 실행한 뒤 로그인 후 스터디 화면으로 진입합니다.

제가 기존에 생성한 스터디 `url`은 `spring-boot` 이므로 주소로 바로 접속하였습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/40-01.png)

여기서 구성원 메뉴를 클릭합니다.

> 테스트하기 전에 프로필 수정 기능을 이용해 한 줄 소개와 프로필 이미지를 추가해주었습니다.
 
![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/40-02.png)

정상적으로 조회되었습니다.