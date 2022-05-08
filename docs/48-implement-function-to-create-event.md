![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 6a61511)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 6a61511
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

모임 만들기 기능을 구현합니다.

## Endpoint 수정

모임 생성 API를 `EventController`에 추가합니다.

`/src/main/java/io/lcalmsky/app/event/endpoint/EventController.java`

```java
package io.lcalmsky.app.event.endpoint;

import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.support.CurrentUser;
import io.lcalmsky.app.event.application.EventService;
import io.lcalmsky.app.event.domain.entity.Event;
import io.lcalmsky.app.event.form.EventForm;
import io.lcalmsky.app.event.validator.EventValidator;
import io.lcalmsky.app.study.application.StudyService;
import io.lcalmsky.app.study.domain.entity.Study;
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
    private final EventValidator eventValidator;

    @InitBinder("eventForm")
    public void initBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(eventValidator);
    }
    
    // 생략

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
}

```

폼 검증을 위해 EventValidator를 추가하였고, @InitBinder를 이용해 주입해주었습니다. 그리고 EventService를 만들어 모임 생성에 대한 기능을 위임하였습니다. 모두 성공했을 때 event의 ID를 이용해 리다이렉트 해주도록 하였습니다.

<details>
<summary>EventController.java 전체 보기</summary>

```java
package io.lcalmsky.app.event.endpoint;

import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.support.CurrentUser;
import io.lcalmsky.app.event.application.EventService;
import io.lcalmsky.app.event.domain.entity.Event;
import io.lcalmsky.app.event.form.EventForm;
import io.lcalmsky.app.event.validator.EventValidator;
import io.lcalmsky.app.study.application.StudyService;
import io.lcalmsky.app.study.domain.entity.Study;
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
}

```

</details>

## EventService 생성

새로운 모임을 생성하는 기능을 `EventService`에 구현합니다.

`/src/main/java/io/lcalmsky/app/event/application/EventService.java`

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
}
```

`Event` `Entity`에 static 생성자를 추가하여 객체 생성을 위임하였고, 객체 생성 후 `EventRepository`에 저장 후 `Entity`를 반환하도록 하였습니다.

## 도메인 Entity, Repository 수정

`Event` `Entity`에 static 생성자를 추가합니다.

`/src/main/java/io/lcalmsky/app/event/domain/entity/Event.java`

```java
// 생략
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
public class Event {
    // 생략
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
}
```

<details>
<summary>Event.java 전체 보기</summary>

```java
package io.lcalmsky.app.event.domain.entity;

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

    public void setEnrollments(List<Enrollment> enrollments) {
        this.enrollments = enrollments;
    }

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
}
```

</details>

`EventRepository`를 생성합니다.

`/src/main/java/io/lcalmsky/app/event/infra/repository/EventRepository.java`

```java
package io.lcalmsky.app.event.infra.repository;

import io.lcalmsky.app.event.domain.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface EventRepository extends JpaRepository<Event, Long> {
}
```

## EventValidator 구현

단순 애너테이션 만으로는 날짜에 대한 validation을 정확하기 할 수 없으므로 `Validator`를 생성하여 구현합니다.

`/src/main/java/io/lcalmsky/app/event/validator/EventValidator.java`

```java
package io.lcalmsky.app.event.validator;

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
}
```

날짜가 세 가지 종류가 있는데 모두 현재보다 이전 시점이 아닌지, 서로 관계에 맞는지 검증하는 로직을 추가해주었습니다.

## 테스트

애플리케이션 실행 후 스터디 페이지에 들어가 모임 만들기 버튼을 클릭합니다.

각각 데이터를 입력하고 검증이 제대로 되는지 모임 만들기 버튼을 클릭하여 확인합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/48-01.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/48-02.png)

최종적으로 validation을 모두 통과했을 때는 아직 뷰를 구현하지 않았기 때문에 아래 처럼 에러가 발생합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/48-03.png)

---

테스트 코드는 아직 구현되지 않은 기능들이 많아 조회 기능과 함께 추가하겠습니다.