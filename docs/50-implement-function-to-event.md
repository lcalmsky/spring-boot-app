![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 6e10ea8)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 6e10ea8
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

모임 목록 조회 기능을 구현합니다.

## Endpoint 추가

스터디 화면의 모임 탭으로 라우팅해 줄 엔드포인트를 `EventController`에 추가합니다.

`/src/main/java/io/lcalmsky/app/event/endpoint/EventController.java`

```java
// 생략
@Controller
@RequestMapping("/study/{path}")
@RequiredArgsConstructor
public class EventController {
    // 생략
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
}
```

<details>
<summary>EventController.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.event.endpoint;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.support.CurrentUser;
import io.lcalmsky.app.modules.event.application.EventService;
import io.lcalmsky.app.modules.event.domain.entity.Event;
import io.lcalmsky.app.modules.event.endpoint.form.EventForm;
import io.lcalmsky.app.modules.event.infra.repository.EventRepository;
import io.lcalmsky.app.modules.event.validator.EventValidator;
import io.lcalmsky.app.modules.study.application.StudyService;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.infra.repository.StudyRepository;
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
}
```

</details>

## Domain Entity/Repository 수정

위의 컨트롤러에서 모임 정보를 조회하기 위해 Event Entity를 수정합니다.

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
    public int numberOfRemainSpots() {
        int accepted = (int) this.enrollments.stream()
                .filter(Enrollment::isAccepted)
                .count();
        return this.limitOfEnrollments - accepted;
    }
}
```

모임에 자리가 남아있는지 확인하기 위한 메서드를 추가하였고 모임 조회시 참가자 정보를 얻어오기 위해 조회하는 부분에서 `N+1 problem`이 발생할 수 있어 `NamedEntityGraph`를 추가하였습니다.

<details>
<summary>Event.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.event.domain.entity;

import io.lcalmsky.app.modules.account.domain.UserAccount;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.event.endpoint.form.EventForm;
import io.lcalmsky.app.modules.study.domain.entity.Study;
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
}
```

</details>

`EventRepository`에도 마찬가지로 조회할 수 있는 메서드를 생성하고 `EntityGraph`를 설정해줍니다.

`/src/main/java/io/lcalmsky/app/event/infra/repository/EventRepository.java`

```java
package io.lcalmsky.app.modules.event.infra.repository;

import io.lcalmsky.app.modules.event.domain.entity.Event;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
public interface EventRepository extends JpaRepository<Event, Long> {
    @EntityGraph(value = "Event.withEnrollments", type = EntityGraph.EntityGraphType.LOAD)
    List<Event> findByStudyOrderByStartDateTime(Study study);
}
```

## View 작성

`resources/templates/study` 하위에 `events.html` 페이지를 작성합니다.

`/src/main/resources/templates/study/events.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<body>
    <nav th:replace="fragments.html :: navigation-bar"></nav>
    <div th:replace="fragments.html :: study-banner"></div>
    <div class="container">
        <div th:replace="fragments.html :: study-info"></div>
        <div th:replace="fragments.html :: study-menu(studyMenu='events')"></div>
        <div class="row my-3 mx-3 justify-content-center">
            <div class="col-10 px-0 row">
                <div class="col-2 px-0">
                    <ul class="list-group">
                        <a href="#"
                           class="list-group-item list-group-item-action d-flex justify-content-between align-items-center">
                            새 모임
                            <span th:text="${newEvents.size()}">2</span>
                        </a>
                        <a href="#"
                           class="list-group-item list-group-item-action d-flex justify-content-between align-items-center">
                            지난 모임
                            <span th:text="${oldEvents.size()}">5</span>
                        </a>
                    </ul>
                </div>
                <div class="col-10 row row-cols-1 row-cols-md-2">
                    <div th:if="${newEvents.size() == 0}" class="col">
                        새 모임이 없습니다.
                    </div>
                    <div class="col mb-4 pr-0" th:each="event: ${newEvents}">
                        <div class="card">
                            <div class="card-header">
                                <span th:text="${event.title}">title</span>
                            </div>
                            <ul class="list-group list-group-flush">
                                <li class="list-group-item">
                                    <i class="fa fa-calendar"></i>
                                    <span class="calendar" th:text="${event.startDateTime}"></span> 모임 시작
                                </li>
                                <li class="list-group-item">
                                    <i class="fa fa-hourglass-end"></i> <span class="fromNow"
                                                                              th:text="${event.endEnrollmentDateTime}"></span>
                                    모집 마감,
                                    <span th:if="${event.limitOfEnrollments != 0}">
                                    <span th:text="${event.limitOfEnrollments}"></span>명 모집 중
                                    (<span th:text="${event.numberOfRemainSpots()}"></span> 자리 남음)
                                </span>
                                </li>
                                <li class="list-group-item">
                                    <a href="#" th:href="@{'/study/' + ${study.path} + '/events/' + ${event.id}}"
                                       class="card-link">자세히 보기</a>
                                </li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-10 px-0 row">
                <div class="col-2"></div>
                <div class="col-10">
                    <table th:if="${oldEvents.size() > 0}" class="table table-hover">
                        <thead>
                        <tr>
                            <th scope="col">#</th>
                            <th scope="col">지난 모임 이름</th>
                            <th scope="col">모임 종료</th>
                            <th scope="col"></th>
                        </tr>
                        </thead>
                        <tbody th:each="event: ${oldEvents}">
                        <tr>
                            <th scope="row" th:text="${eventStat.count}">1</th>
                            <td th:text="${event.title}">Title</td>
                            <td>
                                <span class="date-weekday-time" th:text="${event.endDateTime}"></span>
                            </td>
                            <td>
                                <a href="#" th:href="@{'/study/' + ${study.path} + '/events/' + ${event.id}}"
                                   class="card-link">자세히 보기</a>
                            </td>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
        <div th:replace="fragments.html :: footer"></div>
    </div>
    <script th:replace="fragments.html :: tooltip"></script>
    <script th:replace="fragments.html :: date-time"></script>
</body>
</html>
```

## 테스트

애플리케이션 실행 후 스터디 화면에 진입하여 모임 탭을 클릭합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/50-01.png)

이전에 테스트를 위해 생성한 모임이 두 개가 조회되었습니다.

모임의 [자세히 보기]를 클릭하면 해당 상세 페이지로 이동하는 것도 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/50-02.png)