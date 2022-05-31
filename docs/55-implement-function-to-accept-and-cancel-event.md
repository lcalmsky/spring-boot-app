![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: ebf7e54)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout ebf7e54
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

모임 참가 신청 수락/취소 기능과 출석 체크 기능을 구현합니다.

## Endpoint 추가

참가 신청 수락, 취소, 출석 체크, 출석 체크 취소 `Endpoint`를 `EventController`에 추가합니다.

`/src/main/java/io/lcalmsky/app/event/endpoint/EventController.java`

그동안 아래 처럼 `Endpoint`를 추가했었는데요,

```java
// 생략
@Controller
@RequestMapping("/study/{path}")
@RequiredArgsConstructor
public class EventController {
    // 생략
    @GetMapping("/events/{eventId}/enrollments/{enrollmentId}/accept")
    public String acceptEnrollment(@CurrentUser Account account, @PathVariable String path, @PathVariable Long eventId, @PathVariable Long enrollmentId) {
        Study study = studyService.getStudyToUpdate(account, path);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("모임이 존재하지 않습니다."));
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("잘못된 참가신청 입니다."));
        eventService.acceptEnrollment(event, enrollment);
        return "redirect:/study/" + study.getEncodedPath() + "/events/" + eventId;
    }
}
```

`JPA`에서 지원하는 기능 중 `Domain Entity Converter` 기능을 사용하면 아래 처럼 간단하게 바꿀 수 있습니다.

```java
// 생략
@Controller
@RequestMapping("/study/{path}")
@RequiredArgsConstructor
public class EventController {
    // 생략
    @GetMapping("/events/{eventId}/enrollments/{enrollmentId}/accept")
    public String acceptEnrollment(@CurrentUser Account account, @PathVariable String path, @PathVariable("eventId") Event event, @PathVariable("enrollmentId") Enrollment enrollment) {
        Study study = studyService.getStudyToUpdate(account, path);
        eventService.acceptEnrollment(event, enrollment);
        return "redirect:/study/" + study.getEncodedPath() + "/events/" + event.getId();
    }

    @GetMapping("/events/{eventId}/enrollments/{enrollmentId}/reject")
    public String rejectEnrollment(@CurrentUser Account account, @PathVariable String path, @PathVariable("eventId") Event event, @PathVariable("enrollmentId") Enrollment enrollment) {
        Study study = studyService.getStudyToUpdate(account, path);
        eventService.rejectEnrollment(event, enrollment);
        return "redirect:/study/" + study.getEncodedPath() + "/events/" + event.getId();
    }

    @GetMapping("/events/{eventId}/enrollments/{enrollmentId}/checkin")
    public String checkInEnrollment(@CurrentUser Account account, @PathVariable String path, @PathVariable("eventId") Event event, @PathVariable("enrollmentId") Enrollment enrollment) {
        Study study = studyService.getStudyToUpdate(account, path);
        eventService.checkInEnrollment(event, enrollment);
        return "redirect:/study/" + study.getEncodedPath() + "/events/" + event.getId();
    }

    @GetMapping("/events/{eventId}/enrollments/{enrollmentId}/cancel-checkin")
    public String cancelCheckInEnrollment(@CurrentUser Account account, @PathVariable String path, @PathVariable("eventId") Event event, @PathVariable("enrollmentId") Enrollment enrollment) {
        Study study = studyService.getStudyToUpdate(account, path);
        eventService.cancelCheckinEnrollment(event, enrollment);
        return "redirect:/study/" + study.getEncodedPath() + "/events/" + event.getId();
    }
}
```

기존에는 `eventId`, `enrollmentId`를 매핑하여 직접 `Repository`에 각각 조회하여 `Entity`를 획득하였는데, `findById`로 바로 찾을 수 있는 값들은 `@PathVariable`에 매핑할 변수 이름을 명시하고 바로 `Entity` 객체로 매핑할 수 있습니다.

위 소스 코드에는 새로 추가한 메서드에만 적용하였지만 전체 코드에 해당하는 부분을 모두 수정해주었습니다.

`study`의 경우도 마찬가지로 `@PathVariable`로 주입받고 있지만 조회를 위해선 `findByPath`를 이용해야하므로 추가적인 조치 없이는 바로 `Domain Entity`에 매핑할 수 없습니다.

관리자만 참가 신청을 관리할 수 있기 때문에 관리자 권한까지 같이 조회해오기 위해 `studyService.getStudyToUpdate` 메서드를 사용하였습니다.

> 마지막 테스트 코드 부분에서 다시 언급하겠지만 지난 포스팅에서 추가해주었던 `@RequestBody` 애너테이션을 다시 제거하였습니다. 

<details>
<summary>EventController.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.event.endpoint;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.support.CurrentUser;
import io.lcalmsky.app.modules.event.application.EventService;
import io.lcalmsky.app.modules.event.domain.entity.Enrollment;
import io.lcalmsky.app.modules.event.domain.entity.Event;
import io.lcalmsky.app.modules.event.endpoint.form.EventForm;
import io.lcalmsky.app.modules.event.infra.repository.EnrollmentRepository;
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
    private final EnrollmentRepository enrollmentRepository;
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
    public String getEvent(@CurrentUser Account account, @PathVariable String path, @PathVariable("id") Event event, Model model) {
        model.addAttribute(account);
        model.addAttribute(event);
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
    public String updateEventForm(@CurrentUser Account account, @PathVariable String path, @PathVariable("id") Event event, Model model) {
        model.addAttribute(studyService.getStudyToUpdate(account, path));
        model.addAttribute(account);
        model.addAttribute(event);
        model.addAttribute(EventForm.from(event));
        return "event/update-form";
    }

    @PostMapping("/events/{id}/edit")
    public String updateEventSubmit(@CurrentUser Account account, @PathVariable String path, @PathVariable("id") Event event, @Valid EventForm eventForm, Errors errors, Model model) {
        Study study = studyService.getStudyToUpdate(account, path);
        eventForm.setEventType(event.getEventType());
        eventValidator.validateUpdateForm(eventForm, event, errors);
        if (errors.hasErrors()) {
            model.addAttribute(account);
            model.addAttribute(study);
            model.addAttribute(event);
            return "event/update-form";
        }
        eventService.updateEvent(event, eventForm);
        return "redirect:/study/" + study.getEncodedPath() + "/events/" + event.getId();
    }

    @DeleteMapping("/events/{id}")
    public String deleteEvent(@CurrentUser Account account, @PathVariable String path, @PathVariable("id") Event event) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        eventService.deleteEvent(event);
        return "redirect:/study/" + study.getEncodedPath() + "/events";
    }

    @PostMapping("/events/{id}/enroll")
    public String enroll(@CurrentUser Account account, @PathVariable String path, @PathVariable("id") Event event) {
        Study study = studyService.getStudyToEnroll(path);
        eventService.enroll(event, account);
        return "redirect:/study/" + study.getEncodedPath() + "/events/" + event.getId();
    }

    @PostMapping("/events/{id}/leave")
    public String leave(@CurrentUser Account account, @PathVariable String path, @PathVariable("id") Event event) {
        Study study = studyService.getStudyToEnroll(path);
        eventService.leave(event, account);
        return "redirect:/study/" + study.getEncodedPath() + "/events/" + event.getId();
    }

    @GetMapping("/events/{eventId}/enrollments/{enrollmentId}/accept")
    public String acceptEnrollment(@CurrentUser Account account, @PathVariable String path, @PathVariable("eventId") Event event, @PathVariable("enrollmentId") Enrollment enrollment) {
        Study study = studyService.getStudyToUpdate(account, path);
        eventService.acceptEnrollment(event, enrollment);
        return "redirect:/study/" + study.getEncodedPath() + "/events/" + event.getId();
    }

    @GetMapping("/events/{eventId}/enrollments/{enrollmentId}/reject")
    public String rejectEnrollment(@CurrentUser Account account, @PathVariable String path, @PathVariable("eventId") Event event, @PathVariable("enrollmentId") Enrollment enrollment) {
        Study study = studyService.getStudyToUpdate(account, path);
        eventService.rejectEnrollment(event, enrollment);
        return "redirect:/study/" + study.getEncodedPath() + "/events/" + event.getId();
    }

    @GetMapping("/events/{eventId}/enrollments/{enrollmentId}/checkin")
    public String checkInEnrollment(@CurrentUser Account account, @PathVariable String path, @PathVariable("eventId") Event event, @PathVariable("enrollmentId") Enrollment enrollment) {
        Study study = studyService.getStudyToUpdate(account, path);
        eventService.checkInEnrollment(event, enrollment);
        return "redirect:/study/" + study.getEncodedPath() + "/events/" + event.getId();
    }

    @GetMapping("/events/{eventId}/enrollments/{enrollmentId}/cancel-checkin")
    public String cancelCheckInEnrollment(@CurrentUser Account account, @PathVariable String path, @PathVariable("eventId") Event event, @PathVariable("enrollmentId") Enrollment enrollment) {
        Study study = studyService.getStudyToUpdate(account, path);
        eventService.cancelCheckinEnrollment(event, enrollment);
        return "redirect:/study/" + study.getEncodedPath() + "/events/" + event.getId();
    }
}
```

</details>

## Service 수정

`EventController`에서 모임/참가 상태 수정을 위해 `EventService`에 위임한 부분을 구현합니다.

`/src/main/java/io/lcalmsky/app/event/application/EventService.java`

```java
// 생략
@Service
@Transactional
@RequiredArgsConstructor
public class EventService {
    // 생략
    public void leave(Event event, Account account) { // (1)
        Enrollment enrollment = enrollmentRepository.findByEventAndAccount(event, account);
        if (!enrollment.isAttended()) {
            event.removeEnrollment(enrollment);
            enrollmentRepository.delete(enrollment);
            event.acceptNextIfAvailable();
        }
    }

    public void acceptEnrollment(Event event, Enrollment enrollment) { // (2)
        event.accept(enrollment);
    }

    public void rejectEnrollment(Event event, Enrollment enrollment) { // (2)
        event.reject(enrollment);
    }

    public void checkInEnrollment(Event event, Enrollment enrollment) { // (2)
        enrollment.attend();
    }

    public void cancelCheckinEnrollment(Event event, Enrollment enrollment) { // (2)
        enrollment.absent();
    }
}
```

1. 출석 체크 이후 참가 신청을 취소할 수 있는 버그가 있었는데 그 부분을 해결하기 위해 예외처리를 추가하였습니다.
2. 나머지 참가 신청/거절, 출석 체크/취소 기능은 `entity`에 위임하였습니다.

<details>
<summary>EventService.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.event.application;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.event.domain.entity.Enrollment;
import io.lcalmsky.app.modules.event.domain.entity.Event;
import io.lcalmsky.app.modules.event.endpoint.form.EventForm;
import io.lcalmsky.app.modules.event.infra.repository.EnrollmentRepository;
import io.lcalmsky.app.modules.event.infra.repository.EventRepository;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EnrollmentRepository enrollmentRepository;

    public Event createEvent(Study study, EventForm eventForm, Account account) {
        Event event = Event.from(eventForm, account, study);
        return eventRepository.save(event);
    }

    public void updateEvent(Event event, EventForm eventForm) {
        event.updateFrom(eventForm);
        event.acceptWaitingList();
    }

    public void deleteEvent(Event event) {
        eventRepository.delete(event);
    }

    public void enroll(Event event, Account account) {
        if (!enrollmentRepository.existsByEventAndAccount(event, account)) {
            Enrollment enrollment = Enrollment.of(LocalDateTime.now(), event.isAbleToAcceptWaitingEnrollment(), account);
            event.addEnrollment(enrollment);
            enrollmentRepository.save(enrollment);
        }
    }

    public void leave(Event event, Account account) {
        Enrollment enrollment = enrollmentRepository.findByEventAndAccount(event, account);
        if (!enrollment.isAttended()) {
            event.removeEnrollment(enrollment);
            enrollmentRepository.delete(enrollment);
            event.acceptNextIfAvailable();
        }
    }

    public void acceptEnrollment(Event event, Enrollment enrollment) {
        event.accept(enrollment);
    }

    public void rejectEnrollment(Event event, Enrollment enrollment) {
        event.reject(enrollment);
    }

    public void checkInEnrollment(Event event, Enrollment enrollment) {
        enrollment.attend();
    }

    public void cancelCheckinEnrollment(Event event, Enrollment enrollment) {
        enrollment.absent();
    }
}

```

</details>

## Entity 수정

바로 위에서 `Entity`에 위임한 기능들을 구현합니다.

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
    @OneToMany(mappedBy = "event") @ToString.Exclude
    @OrderBy("enrolledAt") // (1)
    private List<Enrollment> enrollments = new ArrayList<>();

    public boolean isEnrollableFor(UserAccount userAccount) { // (2)
        return isNotClosed() && !isAttended(userAccount) && !isAlreadyEnrolled(userAccount);
    }

    public boolean isDisenrollableFor(UserAccount userAccount) { // (2)
        return isNotClosed() && !isAttended(userAccount) && isAlreadyEnrolled(userAccount);
    }

    public void accept(Enrollment enrollment) { // (3)
        if (this.eventType == EventType.CONFIRMATIVE && this.limitOfEnrollments > this.getNumberOfAcceptedEnrollments()) {
            enrollment.accept();
        }
    }

    public void reject(Enrollment enrollment) { // (4)
        if (this.eventType == EventType.CONFIRMATIVE) {
            enrollment.reject();
        }
    }

    public boolean isAcceptable(Enrollment enrollment) { // (5)
        return this.eventType == EventType.CONFIRMATIVE
                && this.enrollments.contains(enrollment)
                && this.limitOfEnrollments > this.getNumberOfAcceptedEnrollments()
                && !enrollment.isAttended()
                && !enrollment.isAccepted();
    }

    public boolean isRejectable(Enrollment enrollment) { // (6)
        return this.eventType == EventType.CONFIRMATIVE
                && this.enrollments.contains(enrollment)
                && !enrollment.isAttended()
                && enrollment.isAccepted();
    }
}
```

1. 참석 리스트가 조회될 때 참석날짜 기준으로 정렬합니다. 이렇게 하지 않으면 참가 상태가 바뀔 때마다 리스트의 순서가 랜덤으로 바뀔 수 있습니다.
2. 참석하지 않은 상태인지 확인하는 부분을 추가하였습니다.
3. 관리자 확인 모임이고 참석 가능할 경우 참가의 상태를 변경합니다.
4. 관리자 확인 모임인 경우 참가의 상태를 변경합니다.
5. 수락 가능 상태인지 확인합니다.
6. 거절 가능 상태인지 확인합니다.

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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    @OrderBy("enrolledAt")
    private List<Enrollment> enrollments = new ArrayList<>();

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
        return isNotClosed() && !isAttended(userAccount) && !isAlreadyEnrolled(userAccount);
    }

    public boolean isDisenrollableFor(UserAccount userAccount) {
        return isNotClosed() && !isAttended(userAccount) && isAlreadyEnrolled(userAccount);
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

    public boolean isAbleToAcceptWaitingEnrollment() {
        return this.eventType == EventType.FCFS && this.limitOfEnrollments > this.getNumberOfAcceptedEnrollments();
    }

    public void addEnrollment(Enrollment enrollment) {
        this.enrollments.add(enrollment);
        enrollment.attach(this);
    }

    public void removeEnrollment(Enrollment enrollment) {
        this.enrollments.remove(enrollment);
        enrollment.detachEvent();
    }

    public void acceptNextIfAvailable() {
        if (this.isAbleToAcceptWaitingEnrollment()) {
            this.firstWaitingEnrollment().ifPresent(Enrollment::accept);
        }
    }

    private Optional<Enrollment> firstWaitingEnrollment() {
        return this.enrollments.stream()
                .filter(e -> !e.isAccepted())
                .findFirst();
    }

    public void acceptWaitingList() {
        if (this.isAbleToAcceptWaitingEnrollment()) {
            List<Enrollment> waitingList = this.enrollments.stream()
                    .filter(e -> !e.isAccepted())
                    .collect(Collectors.toList());
            int numberToAccept = (int) Math.min(limitOfEnrollments - getNumberOfAcceptedEnrollments(), waitingList.size());
            waitingList.subList(0, numberToAccept).forEach(Enrollment::accept);
        }
    }

    public void accept(Enrollment enrollment) {
        if (this.eventType == EventType.CONFIRMATIVE && this.limitOfEnrollments > this.getNumberOfAcceptedEnrollments()) {
            enrollment.accept();
        }
    }

    public void reject(Enrollment enrollment) {
        if (this.eventType == EventType.CONFIRMATIVE) {
            enrollment.reject();
        }
    }

    public boolean isAcceptable(Enrollment enrollment) {
        return this.eventType == EventType.CONFIRMATIVE
                && this.enrollments.contains(enrollment)
                && this.limitOfEnrollments > this.getNumberOfAcceptedEnrollments()
                && !enrollment.isAttended()
                && !enrollment.isAccepted();
    }

    public boolean isRejectable(Enrollment enrollment) {
        return this.eventType == EventType.CONFIRMATIVE
                && this.enrollments.contains(enrollment)
                && !enrollment.isAttended()
                && enrollment.isAccepted();
    }
}
```

</details>

`/src/main/java/io/lcalmsky/app/event/domain/entity/Enrollment.java`

```java
// 생략
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
public class Enrollment {
    // 생략
    public void accept() {
        this.accepted = true;
    }

    public void reject() {
        this.accepted = false;
    }
    // 생략
    public void attend() {
        this.attended = true;
    }

    public void absent() {
        this.attended = false;
    }
}
```

수락 상태와 참석 상태를 변경하는 메서드를 추가하였습니다.

<details>
<summary>Enrollment.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.event.domain.entity;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
public class Enrollment {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Event event;

    @ManyToOne
    private Account account;

    private LocalDateTime enrolledAt;

    private boolean accepted;

    private boolean attended;


    public static Enrollment of(LocalDateTime enrolledAt, boolean isAbleToAcceptWaitingEnrollment, Account account) {
        Enrollment enrollment = new Enrollment();
        enrollment.enrolledAt = enrolledAt;
        enrollment.accepted = isAbleToAcceptWaitingEnrollment;
        enrollment.account = account;
        return enrollment;
    }

    public void accept() {
        this.accepted = true;
    }

    public void reject() {
        this.accepted = false;
    }

    public void attach(Event event) {
        this.event = event;
    }

    public void detachEvent() {
        this.event = null;
    }

    public void attend() {
        this.attended = true;
    }

    public void absent() {
        this.attended = false;
    }
}

```

</details>

## 테스트

애플리케이션을 실행한 뒤 스터디 관리자 계정으로 로그인하여 관리자 확인 유형의 모임을 생성합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/55-01.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/55-02.png)

참가 신청을 클릭합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/55-03.png)

다른 계정으로 로그인하여 해당 모임 화면에 진입합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/55-04.png)

마찬가지로 참가 신청을 클릭하면 아래 처럼 참가 대기 상태가 됩니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/55-05.png)

다시 관리자 계정에서 확인해보면 참가 신청 관리 탭에서 신청을 수락할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/55-06.png)

모두 수락하면 아래와 같이 취소 가능한 상태가 됩니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/55-07.png)

모임에 참석했을 경우 관리자가 체크인 버튼을 클릭하여 출석 체크를 할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/55-08.png)

잘못 클릭하였을 경우 체크인 취소를 클릭해 이전 상태로 되돌릴 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/55-09.png)

## 테스트 코드 작성

이전 포스팅에서 `@RequestBody` 애너테이션을 사용하지 않으면 매핑이 되지 않는 에러가 있다고 적었었는데, `param` 메서드를 이용하니 해결이 되더군요.. 이전에도 `form`으로 전달할 때 사용했던 방법인데 왜 작성 당시에는 깔끔하게 까먹었는지 ㅜㅜ

테스트 코드 추가만 하는 게 아니라 기존 코드도 `param`을 이용해 수정해주었습니다.

`/src/test/java/io/lcalmsky/app/event/endpoint/EventControllerTest.java`

소스 코드가 길긴 하지만 전체적으로 수정했기 때문에 전체 코드를 첨부합니다.

기존에 `content()`를 이용하던 부분은 `param()`으로 바뀌었고, Event를 생성하는 메서드에 모집 방법과 관리자 계정을 받을 수 있도록 수정하였습니다.

참가 신청/거절, 출석 체크/취소 관련 테스트도 지난 테스트들과 마찬가지로 관리자와 회원의 역할을 구분하여 정확하게 작성해야 합니다.

```java
package io.lcalmsky.app.modules.event.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lcalmsky.app.modules.account.WithAccount;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
import io.lcalmsky.app.modules.event.application.EventService;
import io.lcalmsky.app.modules.event.domain.entity.Enrollment;
import io.lcalmsky.app.modules.event.domain.entity.Event;
import io.lcalmsky.app.modules.event.domain.entity.EventType;
import io.lcalmsky.app.modules.event.endpoint.form.EventForm;
import io.lcalmsky.app.modules.event.infra.repository.EnrollmentRepository;
import io.lcalmsky.app.modules.event.infra.repository.EventRepository;
import io.lcalmsky.app.modules.study.application.StudyService;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.endpoint.form.StudyForm;
import io.lcalmsky.app.modules.study.infra.repository.StudyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class EventControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired StudyService studyService;
    @Autowired EventService eventService;
    @Autowired AccountRepository accountRepository;
    @Autowired StudyRepository studyRepository;
    @Autowired EventRepository eventRepository;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired ObjectMapper objectMapper;
    private final String studyPath = "study-path";
    private Study study;

    @BeforeEach
    void beforeEach() {
        Account account = accountRepository.findByNickname("jaime");
        this.study = studyService.createNewStudy(StudyForm.builder()
                .path(studyPath)
                .shortDescription("short-description")
                .fullDescription("full-description")
                .title("title")
                .build(), account);
    }

    @AfterEach
    void afterEach() {
        studyRepository.deleteAll();
    }

    @Test
    @DisplayName("이벤트 폼")
    @WithAccount("jaime")
    void eventForm() throws Exception {
        mockMvc.perform(get("/study/" + studyPath + "/new-event"))
                .andExpect(status().isOk())
                .andExpect(view().name("event/form"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("study"))
                .andExpect(model().attributeExists("eventForm"));

    }

    @Test
    @DisplayName("모임 생성 성공")
    @WithAccount("jaime")
    void createEvent() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        ResultActions resultActions = mockMvc.perform(post("/study/" + studyPath + "/new-event")
                .param("description", "description")
                .param("eventType", EventType.FCFS.name())
                .param("endDateTime", now.plusWeeks(3).toString())
                .param("endEnrollmentDateTime", now.plusWeeks(1).toString())
                .param("limitOfEnrollments", "2")
                .param("startDateTime", now.plusWeeks(2).toString())
                .param("title", "title")
                .with(csrf()));
        Event event = eventRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("등록된 모임이 없습니다."));
        resultActions.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/events/" + event.getId()));
    }

    @Test
    @DisplayName("모임 생성 실패")
    @WithAccount("jaime")
    void createEventWithErrors() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        mockMvc.perform(post("/study/" + studyPath + "/new-event")
                        .param("description", "description")
                        .param("eventType", EventType.FCFS.name())
                        .param("endDateTime", now.plusWeeks(3).toString())
                        .param("endEnrollmentDateTime", now.plusWeeks(1).toString())
                        .param("limitOfEnrollments", "2")
                        .param("startDateTime", now.plusWeeks(2).toString())
                        .param("title", "")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("event/form"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("study"));
    }

    @Test
    @DisplayName("모임 뷰")
    @WithAccount("jaime")
    void eventView() throws Exception {
        Event event = stubbingEvent(EventType.FCFS);
        mockMvc.perform(get("/study/" + studyPath + "/events/" + event.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("study"))
                .andExpect(view().name("event/view"));
    }

    @Test
    @DisplayName("모임 리스트 뷰")
    @WithAccount("jaime")
    void eventListView() throws Exception {
        stubbingEvent(EventType.FCFS);
        mockMvc.perform(get("/study/" + studyPath + "/events"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("study"))
                .andExpect(model().attributeExists("newEvents"))
                .andExpect(model().attributeExists("oldEvents"))
                .andExpect(view().name("study/events"));
    }

    @Test
    @DisplayName("모임 수정 뷰")
    @WithAccount("jaime")
    void eventEditView() throws Exception {
        Event event = stubbingEvent(EventType.FCFS);
        mockMvc.perform(get("/study/" + studyPath + "/events/" + event.getId() + "/edit"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("study"))
                .andExpect(model().attributeExists("event"))
                .andExpect(model().attributeExists("eventForm"))
                .andExpect(view().name("event/update-form"));
    }

    @Test
    @DisplayName("모임 수정")
    @WithAccount("jaime")
    void editEvent() throws Exception {
        Event event = stubbingEvent(EventType.FCFS);
        LocalDateTime now = LocalDateTime.now();
        mockMvc.perform(post("/study/" + studyPath + "/events/" + event.getId() + "/edit")
                        .param("description", "description")
                        .param("eventType", EventType.FCFS.name())
                        .param("endDateTime", now.plusWeeks(3).toString())
                        .param("endEnrollmentDateTime", now.plusWeeks(1).toString())
                        .param("limitOfEnrollments", "2")
                        .param("startDateTime", now.plusWeeks(2).toString())
                        .param("title", "anotherTitle")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/events/" + event.getId()));
    }

    @Test
    @DisplayName("모임 삭제")
    @WithAccount("jaime")
    void deleteEvent() throws Exception {
        Event event = stubbingEvent(EventType.FCFS);
        mockMvc.perform(delete("/study/" + studyPath + "/events/" + event.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/events"));
        Optional<Event> byId = eventRepository.findById(event.getId());
        assertEquals(Optional.empty(), byId);
    }

    @Test
    @DisplayName("선착순 모임에 참가 신청 - 자동 수락")
    @WithAccount("jaime")
    void enroll() throws Exception {
        Event event = stubbingEvent(EventType.FCFS);
        mockMvc.perform(post("/study/" + study.getPath() + "/events/" + event.getId() + "/enroll")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/events/" + event.getId()));
        Account account = accountRepository.findByNickname("jaime");
        isAccepted(account, event);
    }

    @Test
    @DisplayName("선착순 모임에 참가 신청 - 대기중")
    @WithAccount("jaime")
    void enroll_with_waiting() throws Exception {
        Event event = stubbingEvent(EventType.FCFS);
        Account tester1 = createAccount("tester1");
        Account tester2 = createAccount("tester2");
        eventService.enroll(event, tester1);
        eventService.enroll(event, tester2);
        mockMvc.perform(post("/study/" + study.getPath() + "/events/" + event.getId() + "/enroll")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/events/" + event.getId()));
        Account jaime = accountRepository.findByNickname("jaime");
        isNotAccepted(jaime, event);
    }

    @Test
    @DisplayName("참가신청 확정자가 취소하는 경우: 다음 대기자 자동 신청")
    @WithAccount("jaime")
    void leave_auto_enroll() throws Exception {
        Account jaime = accountRepository.findByNickname("jaime");
        Account tester1 = createAccount("tester1");
        Account tester2 = createAccount("tester2");
        Event event = stubbingEvent(EventType.FCFS);
        eventService.enroll(event, tester1);
        eventService.enroll(event, jaime);
        eventService.enroll(event, tester2);
        isAccepted(tester1, event);
        isAccepted(jaime, event);
        isNotAccepted(tester2, event);
        mockMvc.perform(post("/study/" + study.getPath() + "/events/" + event.getId() + "/leave")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/events/" + event.getId()));
        isAccepted(tester1, event);
        isAccepted(tester2, event);
        assertNull(enrollmentRepository.findByEventAndAccount(event, jaime));
    }

    @Test
    @DisplayName("참가신청 비확정자가 참가 신청을 취소하는 경우: 변화 없음")
    @WithAccount("jaime")
    void leave() throws Exception {
        Account jaime = accountRepository.findByNickname("jaime");
        Account tester1 = createAccount("tester1");
        Account tester2 = createAccount("tester2");
        Event event = stubbingEvent(EventType.FCFS);
        eventService.enroll(event, tester2);
        eventService.enroll(event, tester1);
        eventService.enroll(event, jaime);
        isAccepted(tester1, event);
        isAccepted(tester2, event);
        isNotAccepted(jaime, event);
        mockMvc.perform(post("/study/" + study.getPath() + "/events/" + event.getId() + "/leave")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/events/" + event.getId()));
        isAccepted(tester1, event);
        isAccepted(tester2, event);
        assertNull(enrollmentRepository.findByEventAndAccount(event, jaime));
    }

    @Test
    @DisplayName("참가 신청 수락")
    @WithAccount("jaime")
    void accept() throws Exception {
        Account manager = accountRepository.findByNickname("jaime");
        Account account = createAccount("member");
        Event event = stubbingEvent(EventType.CONFIRMATIVE, manager);
        eventService.enroll(event, account);
        Enrollment enrollment = enrollmentRepository.findByEventAndAccount(event, account);

        mockMvc.perform(get("/study/" + study.getPath() + "/events/" + event.getId() + "/enrollments/" + enrollment.getId() + "/accept"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getEncodedPath() + "/events/" + event.getId()));

        assertTrue(enrollment.isAccepted());
    }

    @Test
    @DisplayName("참가 신청 거절")
    @WithAccount("jaime")
    void reject() throws Exception {
        Account manager = accountRepository.findByNickname("jaime");
        Account account = createAccount("member");
        Event event = stubbingEvent(EventType.CONFIRMATIVE, manager);
        eventService.enroll(event, account);
        Enrollment enrollment = enrollmentRepository.findByEventAndAccount(event, account);

        mockMvc.perform(get("/study/" + study.getPath() + "/events/" + event.getId() + "/enrollments/" + enrollment.getId() + "/reject"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getEncodedPath() + "/events/" + event.getId()));

        assertFalse(enrollment.isAccepted());
    }

    @Test
    @DisplayName("출석 체크")
    @WithAccount("jaime")
    void checkin() throws Exception {
        Account manager = accountRepository.findByNickname("jaime");
        Account account = createAccount("member");
        Event event = stubbingEvent(EventType.CONFIRMATIVE, manager);
        eventService.enroll(event, account);
        Enrollment enrollment = enrollmentRepository.findByEventAndAccount(event, account);
        eventService.acceptEnrollment(event, enrollment);

        mockMvc.perform(get("/study/" + study.getPath() + "/events/" + event.getId() + "/enrollments/" + enrollment.getId() + "/checkin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getEncodedPath() + "/events/" + event.getId()));

        assertTrue(enrollment.isAttended());
    }

    @Test
    @DisplayName("출석 체크 취소")
    @WithAccount("jaime")
    void cancelCheckin() throws Exception {
        Account manager = accountRepository.findByNickname("jaime");
        Account account = createAccount("member");
        Event event = stubbingEvent(EventType.CONFIRMATIVE, manager);
        eventService.enroll(event, account);
        Enrollment enrollment = enrollmentRepository.findByEventAndAccount(event, account);
        eventService.acceptEnrollment(event, enrollment);

        mockMvc.perform(get("/study/" + study.getPath() + "/events/" + event.getId() + "/enrollments/" + enrollment.getId() + "/cancel-checkin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getEncodedPath() + "/events/" + event.getId()));

        assertFalse(enrollment.isAttended());
    }

    private void isNotAccepted(Account account, Event event) {
        assertFalse(enrollmentRepository.findByEventAndAccount(event, account).isAccepted());
    }

    private void isAccepted(Account account, Event event) {
        assertTrue(enrollmentRepository.findByEventAndAccount(event, account).isAccepted());
    }

    private Event stubbingEvent(EventType eventType, Account... accounts) {
        Study study = studyRepository.findByPath(studyPath);
        Account account = null;
        if (accounts == null || accounts.length == 0) {
            account = createAccount("manager");
        } else {
            account = accounts[0];
        }
        LocalDateTime now = LocalDateTime.now();
        EventForm eventForm = EventForm.builder()
                .description("description")
                .eventType(eventType)
                .endDateTime(now.plusWeeks(3))
                .endEnrollmentDateTime(now.plusWeeks(1))
                .limitOfEnrollments(2)
                .startDateTime(now.plusWeeks(2))
                .title("title")
                .build();
        return eventService.createEvent(study, eventForm, account);
    }

    private Account createAccount(String nickname) {
        return accountRepository.save(Account.with(nickname + "@example.com", nickname, "password"));
    }
}
```

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/55-10.png)

모든 테스트가 정상적으로 수행되었습니다!