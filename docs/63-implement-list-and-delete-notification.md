![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 9e6cfe6)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 9e6cfe6
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

알림 목록을 조회하고 삭제하는 기능을 구현합니다.

알림 버튼을 클릭했을 때 읽지 않은 알림을 보여주고 읽은 알림도 확인하거나 삭제할 수 있습니다.

## Endpoint 추가

알림 버튼을 클릭했을 때 진입하기 위한 `Endpoint`와 읽은 알림을 조회하기 위한 `Endpoint`, 알림을 삭제하기위한 `Endpoint` 세 가지를 `NotificationController` 클래스에 추가합니다.

`/src/main/java/io/lcalmsky/app/modules/notification/endpoint/NotificationController.java`

```java
package io.lcalmsky.app.modules.notification.endpoint;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.support.CurrentUser;
import io.lcalmsky.app.modules.notification.application.NotificationService;
import io.lcalmsky.app.modules.notification.domain.entity.Notification;
import io.lcalmsky.app.modules.notification.infra.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @GetMapping("/notifications")
    public String getNotifications(@CurrentUser Account account, Model model) {
        List<Notification> notifications = notificationRepository.findByAccountAndCheckedOrderByCreatedDesc(account, false);
        long numberOfChecked = notificationRepository.countByAccountAndChecked(account, true);
        putCategorizedNotifications(model, notifications, numberOfChecked, notifications.size());
        model.addAttribute("isNew", true);
        notificationService.markAsRead(notifications);
        return "notification/list";
    }

    @GetMapping("/notifications/old")
    public String getOldNotifications(@CurrentUser Account account, Model model) {
        List<Notification> notifications = notificationRepository.findByAccountAndCheckedOrderByCreatedDesc(account, true);
        long numberOfNotChecked = notificationRepository.countByAccountAndChecked(account, false);
        putCategorizedNotifications(model, notifications, notifications.size(), numberOfNotChecked);
        model.addAttribute("isNew", false);
        return "notification/list";
    }

    @DeleteMapping("/notifications")
    public String deleteNotifications(@CurrentUser Account account) {
        notificationRepository.deleteByAccountAndChecked(account, true);
        return "redirect:/notifications";
    }

    private void putCategorizedNotifications(Model model, List<Notification> notifications, long numberOfChecked, long numberOfNotChecked) {
        ArrayList<Notification> newStudyNotifications = new ArrayList<>();
        ArrayList<Notification> eventEnrollmentNotifications = new ArrayList<>();
        ArrayList<Notification> watchingStudyNotifications = new ArrayList<>();
        for (Notification notification : notifications) {
            switch (notification.getNotificationType()) {
                case STUDY_CREATED: {
                    newStudyNotifications.add(notification);
                    break;
                }
                case EVENT_ENROLLMENT: {
                    eventEnrollmentNotifications.add(notification);
                    break;
                }
                case STUDY_UPDATED: {
                    watchingStudyNotifications.add(notification);
                    break;
                }
            }
        }
        model.addAttribute("numberOfNotChecked", numberOfNotChecked);
        model.addAttribute("numberOfChecked", numberOfChecked);
        model.addAttribute("notifications", notifications);
        model.addAttribute("newStudyNotifications", newStudyNotifications);
        model.addAttribute("eventEnrollmentNotifications", eventEnrollmentNotifications);
        model.addAttribute("watchingStudyNotifications", watchingStudyNotifications);
    }
}
```

새로운 알림과 기존 알림의 기능은 거의 비슷한데 쿼리를 위한 파라미터만 상이합니다.

공통적인 기능을 메서드로 추출해주었습니다.

나머지 특이한 사항은 없습니다.

## Service서비스 수정

읽지 않은 알림을 확인하였을 때 읽은 상태로 바꿔주어야 합니다.

해당 기능을 `NotificationService`에 구현하였습니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/modules/notification/application/NotificationService.java`

```java
package io.lcalmsky.app.modules.notification.application;

import io.lcalmsky.app.modules.notification.domain.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {
    public void markAsRead(List<Notification> notifications) {
        notifications.forEach(Notification::read);
    }
}
```

읽지 않은 알림 리스트를 전달받아 `Notification Entity`에서 읽은 상태로 직접 바꾸도록 위임합니다.

## Entity 수정

```java
// 생략
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {
    // 생략
    public void read() {
        this.checked = true;
    }
}
```

`checked` 필드를 `true`로 변환해줍니다.

<details>
<summary>Notification.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.notification.domain.entity;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {
    @Id @GeneratedValue
    private Long id;

    private String title;

    private String link;

    private String message;

    private boolean checked;

    @ManyToOne
    private Account account;

    private LocalDateTime created;

    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    public static Notification from(String title, String link, boolean checked, LocalDateTime created, String message, Account account, NotificationType notificationType) {
        Notification notification = new Notification();
        notification.title = title;
        notification.link = link;
        notification.checked = checked;
        notification.created = created;
        notification.message = message;
        notification.account = account;
        notification.notificationType = notificationType;
        return notification;
    }

    public void read() {
        this.checked = true;
    }
}
```

</details>

## View 수정

먼저 `fragments.html` 파일에 알림 메시지를 보여줄 부분을 정의하였습니다. 

`/Users/jaime/git-repo/spring-boot-app/src/main/resources/templates/fragments.html`

```html
<!DOCTYPE html>
<html lang="en"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<!--생략-->
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
                    <span class="text-info"><i th:if="${hasNotification}" class="fa fa-bell" aria-hidden="true"></i></span>
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

다음으로 알림 조회 화면을 `notification` 디렉토리 하위에 구현합니다.


`/Users/jaime/git-repo/spring-boot-app/src/main/resources/templates/notification/list.html

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html::head"></head>
<body class="bg-light">
    <nav th:replace="fragments.html::navigation-bar"></nav>
    <div class="container">
        <div class="row py-5 text-center">
            <div class="col-3">
                <ul class="list-group">
                    <a href="#" th:href="@{/notifications}" th:classappend="${isNew}? active"
                       class="list-group-item list-group-item-action d-flex justify-content-between align-items-center">
                        읽지 않은 알림
                        <span th:text="${numberOfNotChecked}">3</span>
                    </a>
                    <a href="#" th:href="@{/notifications/old}" th:classappend="${!isNew}? active"
                       class="list-group-item list-group-item-action d-flex justify-content-between align-items-center">
                        읽은 알림
                        <span th:text="${numberOfChecked}">0</span>
                    </a>
                </ul>

                <ul class="list-group mt-4">
                    <a href="#" th:if="${newStudyNotifications.size() > 0}"
                       class="list-group-item list-group-item-action d-flex justify-content-between align-items-center">
                        새 스터디 알림
                        <span th:text="${newStudyNotifications.size()}">3</span>
                    </a>
                    <a href="#" th:if="${eventEnrollmentNotifications.size() > 0}"
                       class="list-group-item list-group-item-action d-flex justify-content-between align-items-center">
                        모임 참가 신청 알림
                        <span th:text="${eventEnrollmentNotifications.size()}">0</span>
                    </a>
                    <a href="#" th:if="${watchingStudyNotifications.size() > 0}"
                       class="list-group-item list-group-item-action d-flex justify-content-between align-items-center">
                        관심있는 스터디 알림
                        <span th:text="${watchingStudyNotifications.size()}">0</span>
                    </a>
                </ul>

                <ul class="list-group mt-4" th:if="${numberOfChecked > 0}">
                    <form class="row" th:action="@{/notifications}" th:method="delete">
                        <button type="submit" class="btn btn-outline-warning" aria-describedby="deleteHelp">
                            읽은 알림 삭제
                        </button>
                        <small id="deleteHelp">삭제하지 않아도 한달이 지난 알림은 사라집니다.</small>
                    </form>
                </ul>
            </div>
            <div class="col-9">
                <div class="card" th:if="${notifications.size() == 0}">
                    <div class="card-header">
                        알림 메시지가 없습니다.
                    </div>
                </div>

                <div class="card" th:if="${newStudyNotifications.size() > 0}">
                    <div class="card-header">
                        주요 활동 지역에 관심있는 주제의 스터디가 생겼습니다.
                    </div>
                    <div th:replace="fragments.html::notification-list (notifications=${newStudyNotifications})"></div>
                </div>

                <div class="card mt-4" th:if="${eventEnrollmentNotifications.size() > 0}">
                    <div class="card-header">
                        모임 참가 신청 관련 소식이 있습니다.
                    </div>
                    <div th:replace="fragments.html::notification-list (notifications=${eventEnrollmentNotifications})"></div>
                </div>

                <div class="card mt-4" th:if="${watchingStudyNotifications.size() > 0}">
                    <div class="card-header">
                        참여중인 스터디 관련 소식이 있습니다.
                    </div>
                    <div th:replace="fragments.html::notification-list (notifications=${watchingStudyNotifications})"></div>
                </div>
            </div>
        </div>
        <div th:replace="fragments.html::footer"></div>
    </div>
    <script th:replace="fragments.html::date-time"></script>
</body>
</html>
```

## 테스트

지난번과 마찬가지로 스터디를 생성하고 주제와 지역을 설정한 뒤 스터디를 공개합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/61-01.png)
![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/61-02.png)
![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/61-03.png)
![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/61-04.png)

다시 홈 화면으로 돌아가면 알림 버튼이 활성화 된 것을 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/61-05.png)

알림 버튼을 클릭하면 읽지 않은 알림을 우선적으로 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/61-06.png)

이 화면에서 새로고침을 해보면 읽지 않은 알림이 모두 읽은 알림으로 이동됩니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/61-07.png)

알림 삭제 버튼을 누르면 알림이 모두 제거됩니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/61-08.png)