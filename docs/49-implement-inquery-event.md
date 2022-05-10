![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 42fb381)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 42fb381
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

모임 조회 기능을 구현합니다.

## 엔드포인트 추가

모임 조회 화면을 보여줄 엔드포인트를 `EventController`에 추가합니다.

`/src/main/java/io/lcalmsky/app/event/endpoint/EventController.java`

```java
// 생략
@Controller
@RequestMapping("/study/{path}")
@RequiredArgsConstructor
public class EventController {

    private final StudyService studyService;
    private final EventService eventService;
    private final EventRepository eventRepository;
    private final StudyRepository studyRepository;
    private final EventValidator eventValidator;

    // 생략
    @GetMapping("/events/{id}")
    public String getEvent(@CurrentUser Account account, @PathVariable String path, @PathVariable Long id, Model model) {
        model.addAttribute(account);
        model.addAttribute(eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 모임은 존재하지 않습니다.")));
        model.addAttribute(studyRepository.findStudyWithManagersByPath(path));
        return "event/view";
    }
}
```

<details>
<summary>EventController.java 전체 보기</summary>

```java
package io.lcalmsky.app.event.endpoint;

import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.support.CurrentUser;
import io.lcalmsky.app.event.application.EventService;
import io.lcalmsky.app.event.domain.entity.Event;
import io.lcalmsky.app.event.form.EventForm;
import io.lcalmsky.app.event.infra.repository.EventRepository;
import io.lcalmsky.app.event.validator.EventValidator;
import io.lcalmsky.app.study.application.StudyService;
import io.lcalmsky.app.study.domain.entity.Study;
import io.lcalmsky.app.study.infra.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Controller
@RequestMapping("/study/{path}")
@RequiredArgsConstructor
public class EventController {

    private final StudyService studyService;
    private final EventService eventService;
    private final EventRepository eventRepository;
    private final StudyRepository studyRepository;
    private final EventValidator eventValidator;

    @InitBinder("eventForm")
    public void initBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(eventValidator);
    }

    @GetMapping("/new-event")
    public String newEventForm(@CurrentUser Account account, @PathVariable String path, Model model) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        model.addAttribute(study);
        model.addAttribute(account);
        model.addAttribute(new EventForm());
        return "event/form";
    }

    @PostMapping("/new-event")
    public String createNewEvent(@CurrentUser Account account, @PathVariable String path, @Valid EventForm eventForm, Errors errors, Model model) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        if (errors.hasErrors()) {
            model.addAttribute(account);
            model.addAttribute(study);
            return "event/form";
        }
        Event event = eventService.createEvent(study, eventForm, account);
        return "redirect:/study/" + study.getEncodedPath() + "/events/" + event.getId();
    }

    @GetMapping("/events/{id}")
    public String getEvent(@CurrentUser Account account, @PathVariable String path, @PathVariable Long id, Model model) {
        model.addAttribute(account);
        model.addAttribute(eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 모임은 존재하지 않습니다.")));
        model.addAttribute(studyRepository.findStudyWithManagersByPath(path));
        return "event/view";
    }
}
```

</details>

## Entity 수정

뷰에서 Event를 보여주기 위해 필요한 몇 가지 기능들을 도메인 `Entity`에 추가해줍니다. 

`/src/main/java/io/lcalmsky/app/event/domain/entity/Event.java`

```java
// 생략
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
public class Event {
    // 생략
    public boolean isEnrollableFor(UserAccount userAccount) {
        return isNotClosed() && !isAlreadyEnrolled(userAccount);
    }

    public boolean isDisenrollableFor(UserAccount userAccount) {
        return isNotClosed() && isAlreadyEnrolled(userAccount);
    }

    private boolean isNotClosed() {
        return this.endEnrollmentDateTime.isAfter(LocalDateTime.now());
    }

    private boolean isAlreadyEnrolled(UserAccount userAccount) {
        Account account = userAccount.getAccount();
        for (Enrollment enrollment : this.enrollments) {
            if (enrollment.getAccount().equals(account)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAttended(UserAccount userAccount) {
        Account account = userAccount.getAccount();
        for (Enrollment enrollment : this.enrollments) {
            if (enrollment.getAccount().equals(account) && enrollment.isAttended()) {
                return true;
            }
        }
        return false;
    }
}
```

> 사용자 계정이 모임에 참가할 수 있는지 여부와 참석 완료 여부를 확인하는 메스드를 추가하였습니다.
> 
> `Enrollment Entity`에 오타가 있어 수정하였습니다.
> 
> `/src/main/java/io/lcalmsky/app/event/domain/entity/Enrollment.java`
> 
> `private boolean attend;` -> `private boolean attended;`
> 
> <details>
> <summary>Enrollment.java 전체 보기</summary>
> 
> ```java
> package io.lcalmsky.app.event.domain.entity;
> 
> import io.lcalmsky.app.account.domain.entity.Account;
> import lombok.*;
> 
> import javax.persistence.Entity;
> import javax.persistence.GeneratedValue;
> import javax.persistence.Id;
> import javax.persistence.ManyToOne;
> import java.time.LocalDateTime;
> 
> @Entity
> @NoArgsConstructor(access = AccessLevel.PROTECTED)
> @Getter
> @ToString
> @EqualsAndHashCode(of = "id")
> public class Enrollment {
> 
>     @Id
>     @GeneratedValue
>     private Long id;
> 
>     @ManyToOne
>     private Event event;
> 
>     @ManyToOne
>     private Account account;
> 
>     private LocalDateTime enrolledAt;
> 
>     private boolean accepted;
> 
>     private boolean attended;
> }
> ```

</details>

## 라이브러리 설치

날짜를 다양한 형태로 표현해주는 라이브러리를 설치합니다.

라이브러리에 대한 자세한 내용은 [여기](https://momentjs.com/)를 참고하세요.

```shell
 ~/git-repo/spring-boot-app/src/main/resources/static/ [feature/93+*] npm install moment --save

added 1 package, and audited 19 packages in 485ms

2 packages are looking for funding
run `npm fund` for details

found 0 vulnerabilities
npm notice
npm notice New minor version of npm available! 8.5.5 -> 8.9.0
npm notice Changelog: https://github.com/npm/cli/releases/tag/v8.9.0
npm notice Run npm install -g npm@8.9.0 to update!
npm notice
 ~/git-repo/spring-boot-app/src/main/resources/static/ [feature/93+*] 
```

## 뷰 작성

`view.html` 파일을 생성해 아래와 같이 입력합니다.

소스 코드가 조금 기네요 😅

`/src/main/resources/templates/event/view.html`

```html
<!DOCTYPE html>
<html lang="en"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head th:replace="fragments.html :: head"></head>
<body>
    <nav th:replace="fragments.html :: navigation-bar"></nav>
    <div th:replace="fragments.html :: study-banner"></div>
    <div class="container">
        <div class="row py-4 text-left justify-content-center bg-light">
            <div class="col-6">
                <span class="h2">
                <a href="#" class="text-decoration-none" th:href="@{'/study/' + ${study.path}}">
                    <span th:text="${study.title}">스터디 이름</span>
                </a> / </span>
                <span class="h2" th:text="${event.title}"></span>
            </div>
            <div class="col-4 text-end justify-content-end">
                <span sec:authorize="isAuthenticated()">
                    <button th:if="${event.isEnrollableFor(#authentication.principal)}"
                            class="btn btn-outline-primary" data-toggle="modal" data-target="#enroll">
                        <i class="fa fa-plus-circle"></i> 참가 신청
                    </button>
                    <button th:if="${event.isDisenrollableFor(#authentication.principal)}"
                            class="btn btn-outline-primary" data-toggle="modal" data-target="#disenroll">
                        <i class="fa fa-minus-circle"></i> 참가 신청 취소
                    </button>
                    <span class="text-success" th:if="${event.isAttended(#authentication.principal)}" disabled>
                        <i class="fa fa-check-circle"></i> 참석 완료
                    </span>
                </span>
            </div>
            <div class="modal fade" id="disenroll" tabindex="-1" role="dialog" aria-labelledby="leaveTitle" aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered" role="document">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title" id="leaveTitle" th:text="${event.title}"></h5>
                            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                                <span aria-hidden="true">&times;</span>
                            </button>
                        </div>
                        <div class="modal-body">
                            <p>모임 참가 신청을 취소하시겠습니까?</p>
                            <p><strong>확인</strong>하시면 본 참가 신청을 취소하고 다른 대기자에게 참석 기회를 줍니다.</p>
                            <p>감사합니다.</p>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-dismiss="modal">닫기</button>
                            <form th:action="@{'/study/' + ${study.path} + '/events/' + ${event.id} + '/leave'}" method="post">
                                <button class="btn btn-primary" type="submit" aria-describedby="submitHelp">확인</button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
            <div class="modal fade" id="enroll" tabindex="-1" role="dialog" aria-labelledby="enrollmentTitle" aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered" role="document">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title" id="enrollmentTitle" th:text="${event.title}"></h5>
                            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                                <span aria-hidden="true">&times;</span>
                            </button>
                        </div>
                        <div class="modal-body">
                            <p>모임에 참석하시겠습니까? 일정을 캘린더에 등록해 두시면 좋습니다.</p>
                            <p><strong>확인</strong> 버튼을 클릭하면 모임 참가 신청을 합니다.</p>
                            <p>감사합니다.</p>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-dismiss="modal">닫기</button>
                            <form th:action="@{'/study/' + ${study.path} + '/events/' + ${event.id} + '/enroll'}" method="post">
                                <button class="btn btn-primary" type="submit" aria-describedby="submitHelp">확인</button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="row px-3 justify-content-center">
            <div class="col-7 pt-3">
                <dt class="font-weight-light">상세 모임 설명</dt>
                <dd th:utext="${event.description}"></dd>

                <dt class="font-weight-light">모임 참가 신청 (<span th:text="${event.enrollments.size()}"></span>)</dt>
                <dd>
                    <table class="table table-borderless table-sm" th:if="${event.enrollments.size() > 0}">
                        <thead>
                        <tr>
                            <th scope="col">#</th>
                            <th scope="col">참석자</th>
                            <th scope="col">참가 신청 일시</th>
                            <th scope="col">참가 상태</th>
                            <th th:if="${study.isManager(#authentication.principal)}" scope="col">
                                참가 신청 관리
                            </th>
                            <th th:if="${study.isManager(#authentication.principal)}" scope="col">
                                출석 체크
                            </th>
                        </tr>
                        </thead>
                        <tbody>
                        <tr th:each="enroll: ${event.enrollments}">
                            <th scope="row" th:text="${enrollStat.count}"></th>
                            <td>
                                <a th:href="@{'/profile/' + ${enroll.account.nickname}}"
                                   class="text-decoration-none">
                                    <svg th:if="${#strings.isEmpty(enroll.account?.profile?.image)}" data-jdenticon-value="nickname"
                                         th:data-jdenticon-value="${enroll.account.nickname}" width="24" height="24" class="rounded border bg-light"></svg>
                                    <img th:if="${!#strings.isEmpty(enroll.account?.profile?.image)}"
                                         th:src="${enroll.account?.profile?.image}" width="24" height="24" class="rounded border"/>
                                    <span th:text="${enroll.account.nickname}"></span>
                                </a>
                            </td>
                            <td>
                                <span class="date-time" th:text="${enroll.enrolledAt}"></span>
                            </td>
                            <td>
                                <span th:if="${enroll.accepted}">확정</span>
                                <span th:if="${!enroll.accepted}">대기중</span>
                            </td>
                            <td th:if="${study.isManager(#authentication.principal)}">
                                <a th:if="${event.isAcceptable(enroll)}" href="#" class="text-decoration-none"
                                   th:href="@{'/study/' + ${study.path} + '/events/' + ${event.id} + '/enrollments/' + ${enroll.id} + '/accept'}" >신청 수락</a>
                                <a th:if="${event.isRejectable(enroll)}" href="#" class="text-decoration-none"
                                   th:href="@{'/study/' + ${study.path} + '/events/' + ${event.id} + '/enrollments/' + ${enroll.id} + '/reject'}">취소</a>
                            </td>
                            <td th:if="${study.isManager(#authentication.principal)}">
                                <a th:if="${enroll.accepted && !enroll.attended}" href="#" class="text-decoration-none"
                                   th:href="@{'/study/' + ${study.path} + '/events/' + ${event.id} + '/enrollments/' + ${enroll.id} + '/checkin'}">체크인</a>
                                <a th:if="${enroll.accepted && enroll.attended}" href="#" class="text-decoration-none"
                                   th:href="@{'/study/' + ${study.path} + '/events/' + ${event.id} + '/enrollments/' + ${enroll.id} + '/cancel-checkin'}">체크인 취소</a>
                            </td>
                        </tr>
                        </tbody>
                    </table>
                </dd>
            </div>
            <dl class="col-3 pt-3 text-right">
                <dt class="font-weight-light">모집 방법</dt>
                <dd>
                    <span th:if="${event.eventType == T(io.lcalmsky.app.event.domain.entity.EventType).FCFS}">선착순</span>
                    <span th:if="${event.eventType == T(io.lcalmsky.app.event.domain.entity.EventType).CONFIRMATIVE}">관리자 확인</span>
                </dd>

                <dt class="font-weight-light">모집 인원</dt>
                <dd>
                    <span th:text="${event.limitOfEnrollments}"></span>명
                </dd>

                <dt class="font-weight-light">참가 신청 마감 일시</dt>
                <dd>
                    <span class="date" th:text="${event.endEnrollmentDateTime}"></span>
                    <span class="weekday" th:text="${event.endEnrollmentDateTime}"></span><br/>
                    <span class="time" th:text="${event.endEnrollmentDateTime}"></span>
                </dd>

                <dt class="font-weight-light">모임 일시</dt>
                <dd>
                    <span class="date" th:text="${event.startDateTime}"></span>
                    <span class="weekday" th:text="${event.startDateTime}"></span><br/>
                    <span class="time" th:text="${event.startDateTime}"></span> -
                    <span class="time" th:text="${event.endDateTime}"></span>
                </dd>

                <dt class="font-weight-light">모임장</dt>
                <dd>
                    <a th:href="@{'/profile/' + ${event.createdBy?.nickname}}" class="text-decoration-none">
                        <svg th:if="${#strings.isEmpty(event.createdBy?.profile?.image)}"
                             th:data-jdenticon-value="${event.createdBy?.nickname}" width="24" height="24" class="rounded border bg-light"></svg>
                        <img th:if="${!#strings.isEmpty(event.createdBy?.profile?.image)}"
                             th:src="${event.createdBy?.profile?.image}" width="24" height="24" class="rounded border"/>
                        <span th:text="${event.createdBy?.nickname}"></span>
                    </a>
                </dd>

                <dt th:if="${study.isManager(#authentication.principal)}" class="font-weight-light">모임 관리</dt>
                <dd th:if="${study.isManager(#authentication.principal)}">
                    <a class="btn btn-outline-primary btn-sm my-1"
                       th:href="@{'/study/' + ${study.path} + '/events/' + ${event.id} + '/edit'}" >
                        모임 수정
                    </a> <br/>
                    <button class="btn btn-outline-danger btn-sm" data-toggle="modal" data-target="#cancel">
                        모임 취소
                    </button>
                </dd>
            </dl>
            <div class="modal fade" id="cancel" tabindex="-1" role="dialog" aria-labelledby="cancelTitle" aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered" role="document">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title" id="cancelTitle" th:text="${event.title}"></h5>
                            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                                <span aria-hidden="true">&times;</span>
                            </button>
                        </div>
                        <div class="modal-body">
                            <p>모임을 취소 하시겠습니까?</p>
                            <p><strong>확인</strong>하시면 본 모임 및 참가 신청 관련 데이터를 삭제합니다.</p>
                            <p>감사합니다.</p>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-dismiss="modal">닫기</button>
                            <form th:action="@{'/study/' + ${study.path} + '/events/' + ${event.id}}" th:method="delete">
                                <button class="btn btn-primary" type="submit" aria-describedby="submitHelp">확인</button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div th:replace="fragments.html :: footer"></div>
    </div>
    <script th:replace="fragments.html :: date-time"></script>
</body>
</html>
```

이렇게 작성하고 실행하면 에러가 나는데요, `fragments`에 `date-time`이라는 `fragment`를 추가해줘야 합니다.

`/src/main/resources/templates/fragments.html`

```html
<!--생략-->
<div th:fragment="date-time">
    <script src="/node_modules/moment/min/moment-with-locales.min.js"></script>
    <script type="application/javascript">
        $(function () {
            moment.locale('ko');
            $(".date-time").text(function(index, dateTime) {
                return moment(dateTime, "YYYY-MM-DD`T`hh:mm").format('LLL');
            });
            $(".date").text(function(index, dateTime) {
                return moment(dateTime, "YYYY-MM-DD`T`hh:mm").format('LL');
            });
            $(".weekday").text(function(index, dateTime) {
                return moment(dateTime, "YYYY-MM-DD`T`hh:mm").format('dddd');
            });
            $(".time").text(function(index, dateTime) {
                return moment(dateTime, "YYYY-MM-DD`T`hh:mm").format('LT');
            });
            $(".calendar").text(function(index, dateTime) {
                return moment(dateTime, "YYYY-MM-DD`T`hh:mm").calendar();
            });
            $(".fromNow").text(function(index, dateTime) {
                return moment(dateTime, "YYYY-MM-DD`T`hh:mm").fromNow();
            });
            $(".date-weekday-time").text(function(index, dateTime) {
                return moment(dateTime, "YYYY-MM-DD`T`hh:mm").format('LLLL');
            });
        })
    </script>
</div>
<!--생략-->
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
            $(".date-time").text(function(index, dateTime) {
                return moment(dateTime, "YYYY-MM-DD`T`hh:mm").format('LLL');
            });
            $(".date").text(function(index, dateTime) {
                return moment(dateTime, "YYYY-MM-DD`T`hh:mm").format('LL');
            });
            $(".weekday").text(function(index, dateTime) {
                return moment(dateTime, "YYYY-MM-DD`T`hh:mm").format('dddd');
            });
            $(".time").text(function(index, dateTime) {
                return moment(dateTime, "YYYY-MM-DD`T`hh:mm").format('LT');
            });
            $(".calendar").text(function(index, dateTime) {
                return moment(dateTime, "YYYY-MM-DD`T`hh:mm").calendar();
            });
            $(".fromNow").text(function(index, dateTime) {
                return moment(dateTime, "YYYY-MM-DD`T`hh:mm").fromNow();
            });
            $(".date-weekday-time").text(function(index, dateTime) {
                return moment(dateTime, "YYYY-MM-DD`T`hh:mm").format('LLLL');
            });
        })
    </script>
</div>

</html>
```

</details>

## 테스트

애플리케이션을 실행한 뒤 스터디 화면에 진입하여 모임 만들기 버튼을 클릭합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/49-01.png)

적절히 입력하여 모임을 생성하면,

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/49-02.png)

이렇게 모임 화면이로 이동됩니다.

---

다음 포스팅에서는 모임 목록 조회 기능을 구현하겠습니다.

> 테스트 코드는 모임 기능이 모두 끝났을 때 따로 포스팅할 예정입니다.