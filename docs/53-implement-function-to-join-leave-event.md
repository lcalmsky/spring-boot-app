![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: ebf7e54)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout ebf7e54
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

모임 참가 및 탈퇴 기능을 구현합니다.

기능 구현을 위해 고려해야 할 사항들은 다음과 같습니다.

* 모임 참가 신청 및 취소시 스터디 조회
  * 조회하는 스터디의 경우 관리자 권한 없이 읽어올 수 있어야 하므로 데이터를 필요한 만큼만 조회
* 모임 참가 신청
  * 선착순 모임의 경우 참가 신청이 가능한지 여부 판별 필요
  * 가능할 경우 상태 변경
* 모임 탈퇴
  * 선착순 모임이라면 탈퇴 이후 대기중인 모임 참가 신청 중 가장 빨리 신청한 것을 확정 상태로 변경
* 모임 수정 로직 보완
  * 선착순 모임 수정시 모집 인원이 늘어나도록 수정하였으므로 대기 중인 참가 신청이 있을 때 가능한 만큼 신청을 확정 상태로 변경하는 기능 추가
  
## Endpoint 추가

모임 참가 신청, 참가 신청 취소를 위한 엔드포인트를 추가합니다.

`/src/main/java/io/lcalmsky/app/event/endpoint/EventController.java`

```java
// 생략
@Controller
@RequestMapping("/study/{path}")
@RequiredArgsConstructor
public class EventController {
    // 생략
    @PostMapping("/events/{id}/enroll")
    public String enroll(@CurrentUser Account account, @PathVariable String path, @PathVariable Long id) {
        Study study = studyService.getStudyToEnroll(path);
        eventService.enroll(eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("모임이 존재하지 않습니다.")), account);
        return "redirect:/study/" + study.getEncodedPath() + "/events/" + id;
    }

    @PostMapping("/events/{id}/leave")
    public String leave(@CurrentUser Account account, @PathVariable String path, @PathVariable Long id) {
        Study study = studyService.getStudyToEnroll(path);
        eventService.leave(eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("모임이 존재하지 않습니다.")), account);
        return "redirect:/study/" + study.getEncodedPath() + "/events/" + id;
    }
}
```

구현되지 않은 내용들 때문에 컴파일 에러가 발생합니다.

기존에 구현했던 것과 크게 다르지 않기 때문에 설명은 컴파일 에러를 해결하면서 추가하겠습니다.

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
    return "redirect:/study/" + study.getEncodedPath() + "/events/" + event.getId();
  }

  @DeleteMapping("/events/{id}")
  public String deleteEvent(@CurrentUser Account account, @PathVariable String path, @PathVariable Long id) {
    Study study = studyService.getStudyToUpdateStatus(account, path);
    eventService.deleteEvent(eventRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("모임이 존재하지 않습니다.")));
    return "redirect:/study/" + study.getEncodedPath() + "/events";
  }

  @PostMapping("/events/{id}/enroll")
  public String enroll(@CurrentUser Account account, @PathVariable String path, @PathVariable Long id) {
    Study study = studyService.getStudyToEnroll(path);
    eventService.enroll(eventRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("모임이 존재하지 않습니다.")), account);
    return "redirect:/study/" + study.getEncodedPath() + "/events/" + id;
  }

  @PostMapping("/events/{id}/leave")
  public String leave(@CurrentUser Account account, @PathVariable String path, @PathVariable Long id) {
    Study study = studyService.getStudyToEnroll(path);
    eventService.leave(eventRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("모임이 존재하지 않습니다.")), account);
    return "redirect:/study/" + study.getEncodedPath() + "/events/" + id;
  }
}
```

</details>

## 서비스 기능 추가

모임에 참가하는 기능과 참가를 취소하는 기능을 구현합니다.

`/src/main/java/io/lcalmsky/app/event/application/EventService.java`

```java
// 생략
@Service
@Transactional
@RequiredArgsConstructor
public class EventService {
    // 생략
    private final EnrollmentRepository enrollmentRepository;
    // 생략
    public void updateEvent(Event event, EventForm eventForm) {
        event.updateFrom(eventForm);
        event.acceptWaitingList(); // (1)
    }

    public void enroll(Event event, Account account) {
        if (!enrollmentRepository.existsByEventAndAccount(event, account)) { // (2)
            Enrollment enrollment = Enrollment.of(LocalDateTime.now(), event.isAbleToAcceptWaitingEnrollment(), account); // (3)
            event.addEnrollment(enrollment); // (4)
            enrollmentRepository.save(enrollment); // (5)
        }
    }

    public void leave(Event event, Account account) {
        Enrollment enrollment = enrollmentRepository.findByEventAndAccount(event, account); // (6)
        event.removeEnrollment(enrollment); // (7)
        enrollmentRepository.delete(enrollment); // (8)
        event.acceptNextIfAvailable(); // (9)
    }
}

```

맨 처음 언급한 내용 처럼, 선착순 모임의 경우 변동이 있을 때마다 대기 인원에 대한 업데이트가 필요합니다.

1. 모임 인원 수정시에도 반영될 수 있게 대기 목록에 있는 사용자들을 추가시켜 줍니다.
2. 모임에 해당 계정이 참가한 내역이 있는지 확인합니다.
3. 참가 내역이 없으므로 참가 정보를 생성합니다.
4. 모임에 참가 정보를 등록합니다.
5. 참가 정보를 저장합니다.
6. 참가 내역을 조회합니다.
7. 모임에서 참가 내역을 삭제합니다.
8. 참가 정보를 삭제합니다.
9. 모임에서 다음 대기자를 참가 상태로 변경해줍니다.

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
    event.removeEnrollment(enrollment);
    enrollmentRepository.delete(enrollment);
    event.acceptNextIfAvailable();
  }
}

```

</details>

수정사항이 많아서 여전히 컴파일 에러가 많이 발생합니다.

도메인에 위임한 내용들을 순차적으로 구현해보도록 하겠습니다.

## 도메인 Entity 수정

먼저 모임에 관련된 내용을 추가하겠습니다.

`/src/main/java/io/lcalmsky/app/event/domain/entity/Event.java`

```java
// 생략
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
public class Event {
    // 생략
    public boolean isAbleToAcceptWaitingEnrollment() { // (1)
        return this.eventType == EventType.FCFS && this.limitOfEnrollments > this.getNumberOfAcceptedEnrollments();
    }

    public void addEnrollment(Enrollment enrollment) { // (2)
        this.enrollments.add(enrollment);
        enrollment.attach(this);
    }

    public void removeEnrollment(Enrollment enrollment) { // (3)
        this.enrollments.remove(enrollment);
        enrollment.detachEvent();
    }

    public void acceptNextIfAvailable() { // (4)
        if (this.isAbleToAcceptWaitingEnrollment()) {
            this.firstWaitingEnrollment().ifPresent(Enrollment::accept);
        }
    }

    private Optional<Enrollment> firstWaitingEnrollment() { // (5)
        return this.enrollments.stream()
                .filter(e -> !e.isAccepted())
                .findFirst();
    }

    public void acceptWaitingList() { // (6)
        if (this.isAbleToAcceptWaitingEnrollment()) {
            List<Enrollment> waitingList = this.enrollments.stream()
                    .filter(e -> !e.isAccepted())
                    .collect(Collectors.toList());
            int numberToAccept = (int) Math.min(limitOfEnrollments - getNumberOfAcceptedEnrollments(), waitingList.size());
            waitingList.subList(0, numberToAccept).forEach(Enrollment::accept);
        }
    }
}
```

1. 모임 유형이 선착순이고 모임 정원이 참가 요청 수보다 큰지 확인합니다.
2. 모임에 참가 내역을 추가합니다. 역으로 참가 내역에도 모임에 대한 레퍼런스를 추가해줘야 합니다.
3. 모임에서 참가 내역을 제거합니다. 역으로 참가 내역에서도 모임
4. 대기중인 있는 참가 신청들이 수용가능한지 확인하여 첫 번째 신청을 가져와 참가 신청 상태로 변경합니다.
5. 참가 신청 중 신청 완료되지 않은 첫 번째 내역을 가져옵니다.
6. 대기중인 참가 신청 수용 가능 여부를 판단하여 참가 가능한 수만큼 대기 리스트의 상태를 참가 신청 완료 상태로 변경합니다.

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
}
```

</details>

다음으로 참가 신청 내역 관련된 내용을 추가합니다.

`/src/main/java/io/lcalmsky/app/event/domain/entity/Enrollment.java`

```java
// 생략
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
public class Enrollment {
    // 생략
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

    public void attach(Event event) {
        this.event = event;
    }

    public void detachEvent() {
        this.event = null;
    }
}
```

`static` 생성자와 참가 수락여부, 모임과의 참조를 업데이트 할 수 있는 기능을 추가하였습니다.

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

  public void attach(Event event) {
    this.event = event;
  }

  public void detachEvent() {
    this.event = null;
  }
}

```

</details>

## Repository 수정

먼저 참가 내역이 존재하는지 확인하고 조회해오기 위한 메서드를 `EnrollmentRepository`에 추가하였습니다.

`/src/main/java/io/lcalmsky/app/event/infra/repository/EnrollmentRepository.java`

```java
package io.lcalmsky.app.modules.event.infra.repository;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.event.domain.entity.Enrollment;
import io.lcalmsky.app.modules.event.domain.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
  boolean existsByEventAndAccount(Event event, Account account);

  Enrollment findByEventAndAccount(Event event, Account account);
}
```

다음으로 `EventController`에서 `Study`를 조회하기 위한 메서드를 `StudyRepository`에 추가하였습니다.

```java
// 생략
@Transactional(readOnly = true)
public interface StudyRepository extends JpaRepository<Study, Long> {
    // 생략
    Optional<Study> findStudyOnlyByPath(String path);
}
```

모임 조회시에는 관심사, 지역, 관리자, 회원 등을 가져올 필요가 전혀 없으므로 아무런 `EntityGraph`도 추가해주지 않았습니다.

따라서 `findStudyOnlyByPath`로 조회했을 경우 위에 나열한 속성들은 `Lazy Loading` 하게 되는데 실제론 참조하지 않으므로 `join` 쿼리가 발생하지 않습니다.

<details>
<summary>StudyRepository.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.study.infra.repository;

import io.lcalmsky.app.modules.study.domain.entity.Study;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional(readOnly = true)
public interface StudyRepository extends JpaRepository<Study, Long> {
  boolean existsByPath(String path);

  @EntityGraph(value = "Study.withAll", type = EntityGraph.EntityGraphType.LOAD)
  Study findByPath(String path);

  @EntityGraph(value = "Study.withTagsAndManagers", type = EntityGraph.EntityGraphType.FETCH)
  Study findStudyWithTagsByPath(String path);

  @EntityGraph(value = "Study.withZonesAndManagers", type = EntityGraph.EntityGraphType.FETCH)
  Study findStudyWithZonesByPath(String path);

  @EntityGraph(value = "Study.withManagers", type = EntityGraph.EntityGraphType.FETCH)
  Study findStudyWithManagersByPath(String path);

  @EntityGraph(value = "Study.withMembers", type = EntityGraph.EntityGraphType.FETCH)
  Study findStudyWithMembersByPath(String path);

  Optional<Study> findStudyOnlyByPath(String path);
}
```

</details>

바로 위에 구현한 내용을 사용하는 `StudyService`에도 기능을 추가해주겠습니다.

`/src/main/java/io/lcalmsky/app/study/application/StudyService.java`

```java
// 생략
@Service
@RequiredArgsConstructor
@Transactional
public class StudyService {
    // 생략
    public Study getStudyToEnroll(String path) {
        return studyRepository.findStudyOnlyByPath(path)
                .orElseThrow(() -> new IllegalArgumentException(path + "에 해당하는 스터디가 존재하지 않습니다."));
    }
}
```

이제 EventController에서 발생하던 모든 컴파일 에러들은 해소가 되었습니다.

## View 수정

기존에 `event/view.html`에 작성했던 내용 중 `Bootstrap5` 버전에 맞게 수정한 내용입니다.

`/src/main/resources/templates/event/view.html`

* class="close" -> class="btn-close"
* close 버튼 아래 span 항목 삭제
* disenroll -> leave로 변경

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
                            class="btn btn-outline-primary" data-bs-toggle="modal" data-bs-target="#leave">
                        <i class="fa fa-minus-circle"></i> 참가 신청 취소
                    </button>
                    <span class="text-success" th:if="${event.isAttended(#authentication.principal)}" disabled>
                        <i class="fa fa-check-circle"></i> 참석 완료
                    </span>
                </span>
      </div>
      <div class="modal fade" id="leave" tabindex="-1" role="dialog" aria-labelledby="leaveTitle" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered" role="document">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title" id="leaveTitle" th:text="${event.title}"></h5>
              <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close">
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
      <div class="modal fade" id="enroll" tabindex="-1" role="dialog" aria-labelledby="enrollmentTitle"
           aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered" role="document">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title" id="enrollmentTitle" th:text="${event.title}"></h5>
              <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close">
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
                       th:data-jdenticon-value="${enroll.account.nickname}" width="24" height="24"
                       class="rounded border bg-light"></svg>
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
                   th:href="@{'/study/' + ${study.path} + '/events/' + ${event.id} + '/enrollments/' + ${enroll.id} + '/accept'}">신청
                  수락</a>
                <a th:if="${event.isRejectable(enroll)}" href="#" class="text-decoration-none"
                   th:href="@{'/study/' + ${study.path} + '/events/' + ${event.id} + '/enrollments/' + ${enroll.id} + '/reject'}">취소</a>
              </td>
              <td th:if="${study.isManager(#authentication.principal)}">
                <a th:if="${enroll.accepted && !enroll.attended}" href="#" class="text-decoration-none"
                   th:href="@{'/study/' + ${study.path} + '/events/' + ${event.id} + '/enrollments/' + ${enroll.id} + '/checkin'}">체크인</a>
                <a th:if="${enroll.accepted && enroll.attended}" href="#" class="text-decoration-none"
                   th:href="@{'/study/' + ${study.path} + '/events/' + ${event.id} + '/enrollments/' + ${enroll.id} + '/cancel-checkin'}">체크인
                  취소</a>
              </td>
            </tr>
            </tbody>
          </table>
        </dd>
      </div>
      <dl class="col-3 pt-3 text-right">
        <dt class="font-weight-light">모집 방법</dt>
        <dd>
          <span th:if="${event.eventType == T(io.lcalmsky.app.modules.event.domain.entity.EventType).FCFS}">선착순</span>
          <span th:if="${event.eventType == T(io.lcalmsky.app.modules.event.domain.entity.EventType).CONFIRMATIVE}">관리자 확인</span>
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
                 th:data-jdenticon-value="${event.createdBy?.nickname}" width="24" height="24"
                 class="rounded border bg-light"></svg>
            <img th:if="${!#strings.isEmpty(event.createdBy?.profile?.image)}"
                 th:src="${event.createdBy?.profile?.image}" width="24" height="24" class="rounded border"/>
            <span th:text="${event.createdBy?.nickname}"></span>
          </a>
        </dd>

        <dt th:if="${study.isManager(#authentication.principal)}" class="font-weight-light">모임 관리</dt>
        <dd th:if="${study.isManager(#authentication.principal)}">
          <a class="btn btn-outline-primary btn-sm my-1"
             th:href="@{'/study/' + ${study.path} + '/events/' + ${event.id} + '/edit'}">
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
              <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close">
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

> 테스트에 앞서 실수로 컬럼명을 수정하여 에러가 발생하길래 `spring.jpa.hibernate.ddl-auto=create-drop` 설정으로 변경하고 애플리케이션을 실행했다가 기존 데이터를 다 날려먹었습니다 ㅜㅜ
> 
> 계정, 스터디, 모임 등을 다시 생성하고 진행했으니 참고 부탁드립니다.

애프릴케이션 실행 후 로그인 후 스터디 화면의 모입 탭에 진입해 모임의 자세히보기를 클릭합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/53-01.png)

모임 화면에서 참가 신청을 클릭합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/53-02.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/53-03.png)

참가 신청이 완료된 것을 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/53-04.png)

다시 참가 신청을 취소해보면,

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/53-05.png)

취소가 완료되었습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/53-06.png)

다시 참가 신청을 해놓고, 다른 계정으로 참가신청합니다. 

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/53-08.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/53-09.png)

또 다른 계정으로 참가신청합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/53-10.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/53-11.png)

대기중 상태가 되는 것을 확인할 수 있습니다.

마지막으로 이전에 참가되었던 계정에서 참가 신청 취소를 해보면,

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/53-12.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/53-13.png)

기존 대기중인 멤버가 참가 확정되는 것을 확인할 수 있습니다.

---

이번 기능은 로직이 복잡한 편이라 반드시 테스트 코드 작성을 해야합니다.

다음 포스팅에서 이전에 미뤄뒀던 테스트 코드와 함께 작성해보도록 하겠습니다.