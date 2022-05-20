![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 0f966b5)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 0f966b5
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

모임 수정 기능을 구현합니다.

모집 수정 기능 중 제약을 두어야 할 것들이 있습니다.

선착순과 관리자 승인 두 가지의 성격이 너무 다르기 때문에 모집 방법은 수정할 수 없습니다.  

그리고 모집 인원은 확정된 참가 신청 수 보다는 커야 합니다. 10명이었을 때 가득 찼는데 5명으로 줄이게되면 기존에 참가가 완료되었던 사용자를 제외시켜야 하기 때문입니다.

모입 수정의 경우 모임 개설 화면과 유사한 부분이 많기 때문에 최대한 많은 코드를 재사용하였습니다.

## Endpoint 추가

수정 화면에 진입할 수 있는 엔드포인트와 수정 기능을 구현할 엔드포인트를 `EventController`에 추가합니다.

`/src/main/java/io/lcalmsky/app/event/endpoint/EventController.java`

```java
// 생략
@Controller
@RequestMapping("/study/{path}")
@RequiredArgsConstructor
public class EventController {
    // 생략
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
}
```

기존에 구현했던 내용들과 크게 다르진 않은데, 수정 시 `Form`을 검증하는 부분을 `EventValidator`에 추가하였습니다.

맨 처음에 언급한 것처럼 모집 방법에 대한 수정을 할 수 없으므로 `Entity`가 원래 가지고 있던 값으로 업데이트 해주고 있습니다.

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
}
```

</details>

모임 수정 화면에서 기존 모임 내용을 표시해줘야 하므로 `Event Entity`의 값으로 `EventForm`을 채워서 전달해줘야 합니다.

이 역할을 `EventForm`에게 위임하였습니다.

`/src/main/java/io/lcalmsky/app/event/form/EventForm.java`

```java
// 생략
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventForm {
    // 생략
    public static EventForm from(Event event) {
        EventForm eventForm = new EventForm();
        eventForm.title = event.getTitle();
        eventForm.description = event.getDescription();
        eventForm.eventType = event.getEventType();
        eventForm.endEnrollmentDateTime = event.getEndEnrollmentDateTime();
        eventForm.startDateTime = event.getStartDateTime();
        eventForm.endDateTime = event.getEndDateTime();
        eventForm.limitOfEnrollments = event.getLimitOfEnrollments();
        return eventForm;
    }
}
```

<details>
<summary>EventForm.java 전체 보기</summary>

```java
package io.lcalmsky.app.event.form;

import io.lcalmsky.app.event.domain.entity.Event;
import io.lcalmsky.app.event.domain.entity.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventForm {
    @NotBlank
    @Length(max = 50)
    private String title;

    private String description;

    private EventType eventType = EventType.FCFS;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endEnrollmentDateTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDateTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDateTime;

    @Min(2)
    private Integer limitOfEnrollments = 2;

    public static EventForm from(Event event) {
        EventForm eventForm = new EventForm();
        eventForm.title = event.getTitle();
        eventForm.description = event.getDescription();
        eventForm.eventType = event.getEventType();
        eventForm.endEnrollmentDateTime = event.getEndEnrollmentDateTime();
        eventForm.startDateTime = event.getStartDateTime();
        eventForm.endDateTime = event.getEndDateTime();
        eventForm.limitOfEnrollments = event.getLimitOfEnrollments();
        return eventForm;
    }
}
```

</details>

## 서비스 수정

모임을 수정하는 기능을 `EventService`에 구현합니다.

`/src/main/java/io/lcalmsky/app/event/application/EventService.java`

```java
// 생략
@Service
@Transactional
@RequiredArgsConstructor
public class EventService {
    // 생략
    public void updateEvent(Event event, EventForm eventForm) {
        event.updateFrom(eventForm);
    }
}
```

`Event Entity`에서 `Form`으로 전달받은 내용을 직접 업데이트 하도록 위임하였습니다.

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
}
```

</details>

## Domain Entity 수정

`Event Entity`를 수정합니다.

`/src/main/java/io/lcalmsky/app/event/domain/entity/Event.java`

```java
// 생략
@NamedEntityGraph(
        name = "Event.withEnrollments",
        attributeNodes = @NamedAttributeNode("enrollments")
)
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
public class Event {
    // 생략
    public Long getNumberOfAcceptedEnrollments() {
        return this.enrollments.stream()
                .filter(Enrollment::isAccepted)
                .count();
    }

    public void updateFrom(EventForm eventForm) {
        this.title = eventForm.getTitle();
        this.description = eventForm.getDescription();
        this.eventType = eventForm.getEventType();
        this.startDateTime = eventForm.getStartDateTime();
        this.endDateTime = eventForm.getEndDateTime();
        this.limitOfEnrollments = eventForm.getLimitOfEnrollments();
        this.endEnrollmentDateTime = eventForm.getEndEnrollmentDateTime();
    }
}
```

모집 인원 수를 체크하기위한 기능과 모임을 업데이트 하는 기능을 추가하였습니다.

<details>
<summary>Event.java 전체 보기</summary>

```java
package io.lcalmsky.app.event.domain.entity;

import io.lcalmsky.app.account.domain.UserAccount;
import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.event.form.EventForm;
import io.lcalmsky.app.study.domain.entity.Study;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@NamedEntityGraph(
        name = "Event.withEnrollments",
        attributeNodes = @NamedAttributeNode("enrollments")
)
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
public class Event {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Study study;

    @ManyToOne
    private Account createdBy;

    @Column(nullable = false)
    private String title;

    @Lob
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdDateTime;

    @Column(nullable = false)
    private LocalDateTime endEnrollmentDateTime;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    private LocalDateTime endDateTime;

    private Integer limitOfEnrollments;

    @OneToMany(mappedBy = "event") @ToString.Exclude
    private List<Enrollment> enrollments;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    public static Event from(EventForm eventForm, Account account, Study study) {
        Event event = new Event();
        event.eventType = eventForm.getEventType();
        event.description = eventForm.getDescription();
        event.endDateTime = eventForm.getEndDateTime();
        event.endEnrollmentDateTime = eventForm.getEndEnrollmentDateTime();
        event.limitOfEnrollments = eventForm.getLimitOfEnrollments();
        event.startDateTime = eventForm.getStartDateTime();
        event.title = eventForm.getTitle();
        event.createdBy = account;
        event.study = study;
        event.createdDateTime = LocalDateTime.now();
        return event;
    }

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

    public int numberOfRemainSpots() {
        int accepted = (int) this.enrollments.stream()
                .filter(Enrollment::isAccepted)
                .count();
        return this.limitOfEnrollments - accepted;
    }

    public Long getNumberOfAcceptedEnrollments() {
        return this.enrollments.stream()
                .filter(Enrollment::isAccepted)
                .count();
    }

    public void updateFrom(EventForm eventForm) {
        this.title = eventForm.getTitle();
        this.description = eventForm.getDescription();
        this.eventType = eventForm.getEventType();
        this.startDateTime = eventForm.getStartDateTime();
        this.endDateTime = eventForm.getEndDateTime();
        this.limitOfEnrollments = eventForm.getLimitOfEnrollments();
        this.endEnrollmentDateTime = eventForm.getEndEnrollmentDateTime();
    }
}
```

</details>

## Validator 수정

`EventForm`을 검증할 `validator`에 모임 인원수를 조작했을 때 기존 멤버 수 보다 적어지지 않게 검증하는 로직을 추가합니다.

`/src/main/java/io/lcalmsky/app/event/validator/EventValidator.java`

```java
// 생략
@Component
public class EventValidator implements Validator {
    // 생략
    public void validateUpdateForm(EventForm eventForm, Event event, Errors errors) {
        if (eventForm.getLimitOfEnrollments() < event.getNumberOfAcceptedEnrollments()) {
            errors.rejectValue("limitOfEnrollments", "wrong.value", "확인된 참가 신청보다 모집 인원 수가 커야 합니다.");
        }
    }
}
```

이미 컨트롤러에 주입되어있기 때문에 DTO를 검증하는 기능을 역할에 맞게 `validator`에 추가하였습니다.

<details>
<summary>EventValidator.java 전체 보기</summary>

```java
package io.lcalmsky.app.event.validator;

import io.lcalmsky.app.event.domain.entity.Event;
import io.lcalmsky.app.event.form.EventForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.LocalDateTime;

@Component
public class EventValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return EventForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        EventForm eventForm = (EventForm) target;
        if (isEarlierThanNow(eventForm.getEndEnrollmentDateTime())) {
            errors.rejectValue("endEnrollmentDateTime", "wrong.datetime", "모임 접수 종료 일시를 정확히 입력하세요.");
        }
        if (isEarlierThan(eventForm.getEndDateTime(), eventForm.getStartDateTime())
                || isEarlierThan(eventForm.getEndDateTime(), eventForm.getEndEnrollmentDateTime())
                || isEarlierThanNow(eventForm.getEndDateTime())) {
            errors.rejectValue("endDateTime", "wrong.datetime", "모임 종료 일시를 정확히 입력하세요.");
        }
        if (isEarlierThanNow(eventForm.getStartDateTime())) {
            errors.rejectValue("startDateTime", "wrong.datetime", "모임 시작 일시를 정확히 입력하세요.");
        }
    }

    private boolean isEarlierThanNow(LocalDateTime time) {
        return time.isBefore(LocalDateTime.now());
    }

    private boolean isEarlierThan(LocalDateTime time, LocalDateTime targetTime) {
        return time.isBefore(targetTime);
    }

    public void validateUpdateForm(EventForm eventForm, Event event, Errors errors) {
        if (eventForm.getLimitOfEnrollments() < event.getNumberOfAcceptedEnrollments()) {
            errors.rejectValue("limitOfEnrollments", "wrong.value", "확인된 참가 신청보다 모집 인원 수가 커야 합니다.");
        }
    }
}
```

</details>

## 뷰 구현

모임 생성 화면과 매우 유사하지만 모집 방법을 수정할 수 없게 해야합니다.

따라서 모임 생성화면의 내용을 먼저 `fragment`로 만들고, 특정 조건에 의해 표시 여부를 결정하게 하면 됩니다.

먼저 `event/form.html` 파일 중 공통된 부분을 `fragment`로 추출하겠습니다.

`/src/main/resources/templates/event/form.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<body class="bg-light">
    <nav th:replace="fragments.html :: navigation-bar"></nav>
    <div th:replace="fragments.html :: study-banner"></div>
    <div class="container">
        <div th:replace="fragments.html::event-form (mode='new', action='/study/' + ${study.path} + '/new-event/')"></div>
        <div th:replace="fragments.html::footer"></div>
    </div>
    <script th:replace="fragments.html :: form-validation"></script>
    <script th:replace="fragments.html :: editor-script"></script>
</body>
</html>
```

`container` 클래스 안쪽 부분을 모두 추출하였고 `mode`, `action` 파라미터로 현재 어떤 모드인지, 이후 어떤 `url`을 호출해야 하는지 전달하도록 하였습니다.

모임 수정 화면도 매우 유사하므로 복사하여 한 줄만 수정하면 됩니다.

`event` 경로 하위에 `form.html`을 복사하여 `update-form.html`을 생성합니다.

`/src/main/resources/templates/event/update-form.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html::head"></head>
<body class="bg-light">
    <nav th:replace="fragments.html::navigation-bar"></nav>
    <div th:replace="fragments.html::study-banner"></div>
    <div class="container">
        <div th:replace="fragments.html::event-form (mode='edit', action='/study/' + ${study.path} + '/events/' + ${event.id} + '/edit')"></div>
        <div th:replace="fragments.html::footer"></div>
    </div>
    <script th:replace="fragments.html::form-validation"></script>
    <script th:replace="fragments.html::editor-script"></script>
</body>
</html>
```

`event-form` fragment를 사용할 때 전달하는 파라미터 값만 수정해줍니다.

마지막으로 기존 내용을 `fragments.html` 파일로 추출합니다.

`/src/main/resources/templates/fragments.html`

```html
<!DOCTYPE html>
<html lang="en"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<!--생략-->
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

</html>
```

파일의 마지막 부분에 기존 `event/form.html`의 내용을 파라미터에 따라 다르게 보여줄 수 있게 수정하여 추가하였습니다.

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

</html>
```

</details>

## 테스트

앱 실행 후 스터디 내 모임 화면에 진입하여 수정할 모임의 **자세히 보기** 버튼을 클릭합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/51-01.png)

우측에 있는 **모임 수정** 버튼을 클릭합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/51-02.png)

내용을 수정합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/51-03.png)

수정한 내용을 확인합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/51-04.png)