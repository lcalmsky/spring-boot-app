![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 0f966b5)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 0f966b5
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

모임 삭제 기능을 구현합니다.

## 엔드포인트 추가

모임 취소 버튼에 매핑되는 엔드포인트를 `EventController`에 추가합니다.

`/src/main/java/io/lcalmsky/app/event/endpoint/EventController.java`

```java
// 생략
@Controller
@RequestMapping("/study/{path}")
@RequiredArgsConstructor
public class EventController {
    // 생략
    @DeleteMapping("/events/{id}")
    public String deleteEvent(@CurrentUser Account account, @PathVariable String path, @PathVariable Long id) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        eventService.deleteEvent(eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("모임이 존재하지 않습니다.")));
        return "redirect:/study/" + study.getEncodedPath() + "/events";
    }
}
```

`@DeleteMapping`을 사용하기 위해선 `application.yml` 파일도 수정해주어야 합니다.

`HTML` <FORM>에서는 `GET`, `POST`밖에 사용할 수 없는데 `thymeleaf`를 사용해 `th:method`를 지정하면 PUT 또는 DELETE를 사용할 수 있습니다.

내부적으로 `_method`를 사용해서 `@PutMapping`과 `@DeleteMapping`으로 요청을 매핑해줍니다.

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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @GetMapping("/events")
    public String viewStudyEvents(@CurrentUser Account account, @PathVariable String path, Model model) {
        Study study = studyService.getStudy(path);
        model.addAttribute(account);
        model.addAttribute(study);
        List<Event> events = eventRepository.findByStudyOrderByStartDateTime(study);
        List<Event> newEvents = new ArrayList<>();
        List<Event> oldEvents = new ArrayList<>();
        for (Event event : events) {
            if (event.getEndDateTime().isBefore(LocalDateTime.now())) {
                oldEvents.add(event);
            } else {
                newEvents.add(event);
            }
        }
        model.addAttribute("newEvents", newEvents);
        model.addAttribute("oldEvents", oldEvents);
        return "study/events";
    }

    @GetMapping("/events/{id}/edit")
    public String updateEventForm(@CurrentUser Account account, @PathVariable String path, @PathVariable Long id, Model model) {
        Study study = studyService.getStudyToUpdate(account, path);
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("모임이 존재하지 않습니다."));
        model.addAttribute(study);
        model.addAttribute(account);
        model.addAttribute(event);
        model.addAttribute(EventForm.from(event));
        return "event/update-form";
    }

    @PostMapping("/events/{id}/edit")
    public String updateEventSubmit(@CurrentUser Account account, @PathVariable String path, @PathVariable Long id, @Valid EventForm eventForm, Errors errors, Model model) {
        Study study = studyService.getStudyToUpdate(account, path);
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("모임이 존재하지 않습니다."));
        eventForm.setEventType(event.getEventType());
        eventValidator.validateUpdateForm(eventForm, event, errors);
        if (errors.hasErrors()) {
            model.addAttribute(account);
            model.addAttribute(study);
            model.addAttribute(event);
            return "event/update-form";
        }
        eventService.updateEvent(event, eventForm);
        return "redirect:/study/" + study.getEncodedPath() +  "/events/" + event.getId();
    }

    @DeleteMapping("/events/{id}")
    public String deleteEvent(@CurrentUser Account account, @PathVariable String path, @PathVariable Long id) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        eventService.deleteEvent(eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("모임이 존재하지 않습니다.")));
        return "redirect:/study/" + study.getEncodedPath() + "/events";
    }
}
```

</details>

## application.yml 수정

아래 설정을 추가하여 `@DeleteMapping`을 사용할 수 있게 해줍니다.

```yaml
spring:
  mvc:
    hiddenmethod:
      filter:
        enabled: true
```

## Service 수정

`EventService`에 모임을 삭제하는 기능을 추가합니다.

`/src/main/java/io/lcalmsky/app/event/application/EventService.java`

```java
// 생략
@Service
@Transactional
@RequiredArgsConstructor
public class EventService {
    // 생략
    public void deleteEvent(Event event) {
        eventRepository.delete(event);
    }
}
```

<details>
<summary>EventService.java 전체 보기</summary>

```java
package io.lcalmsky.app.event.application;

import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.event.domain.entity.Event;
import io.lcalmsky.app.event.form.EventForm;
import io.lcalmsky.app.event.infra.repository.EventRepository;
import io.lcalmsky.app.study.domain.entity.Study;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    public Event createEvent(Study study, EventForm eventForm, Account account) {
        Event event = Event.from(eventForm, account, study);
        return eventRepository.save(event);
    }

    public void updateEvent(Event event, EventForm eventForm) {
        event.updateFrom(eventForm);
    }

    public void deleteEvent(Event event) {
        eventRepository.delete(event);
    }
}
```
</details>

## 뷰 수정

기존에 다 구현해놓았지만 부트스트랩 버전 차이로 인해 동작하지 않는 부분이 있습니다.

`event/view.html` 파일에서 찾기 바꾸기로 한 번에 바꾸시면 됩니다.

`/src/main/resources/templates/event/view.html`

* data-toggle -> data-bs-toggle
* data-target -> data-bs-target
* data-dismiss -> data-bs-dismiss

<details>
<summary>view.html 전체 보기</summary>

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
                            class="btn btn-outline-primary" data-bs-toggle="modal" data-bs-target="#enroll">
                        <i class="fa fa-plus-circle"></i> 참가 신청
                    </button>
                    <button th:if="${event.isDisenrollableFor(#authentication.principal)}"
                            class="btn btn-outline-primary" data-bs-toggle="modal" data-bs-target="#disenroll">
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
                            <button type="button" class="close" data-bs-dismiss="modal" aria-label="Close">
                                <span aria-hidden="true">&times;</span>
                            </button>
                        </div>
                        <div class="modal-body">
                            <p>모임 참가 신청을 취소하시겠습니까?</p>
                            <p><strong>확인</strong>하시면 본 참가 신청을 취소하고 다른 대기자에게 참석 기회를 줍니다.</p>
                            <p>감사합니다.</p>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">닫기</button>
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
                            <button type="button" class="close" data-bs-dismiss="modal" aria-label="Close">
                                <span aria-hidden="true">&times;</span>
                            </button>
                        </div>
                        <div class="modal-body">
                            <p>모임에 참석하시겠습니까? 일정을 캘린더에 등록해 두시면 좋습니다.</p>
                            <p><strong>확인</strong> 버튼을 클릭하면 모임 참가 신청을 합니다.</p>
                            <p>감사합니다.</p>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">닫기</button>
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
                    <button class="btn btn-outline-danger btn-sm" data-bs-toggle="modal" data-bs-target="#cancel">
                        모임 취소
                    </button>
                </dd>
            </dl>
            <div class="modal fade" id="cancel" tabindex="-1" role="dialog" aria-labelledby="cancelTitle" aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered" role="document">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title" id="cancelTitle" th:text="${event.title}"></h5>
                            <button type="button" class="close" data-bs-dismiss="modal" aria-label="Close">
                                <span aria-hidden="true">&times;</span>
                            </button>
                        </div>
                        <div class="modal-body">
                            <p>모임을 취소 하시겠습니까?</p>
                            <p><strong>확인</strong>하시면 본 모임 및 참가 신청 관련 데이터를 삭제합니다.</p>
                            <p>감사합니다.</p>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">닫기</button>
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

</details>

## 테스트

애플리케이션 실행 후 스터디의 모임 탭에 진입합니다.

삭제할 모임의 [자세히 보기] 버튼을 클릭합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/52-01.png)

모임 상세 화면에서 모임 취소 버튼을 클릭합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/52-02.png)

`modal`이 노출되는 것을 확인하고 확인 버튼으로 모임을 삭제합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/52-03.png)

다시 모임탭으로 돌아가면서 기존 모임이 삭제된 것을 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/52-04.png)