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

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/event/endpoint/EventController.java`

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

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/event/domain/entity/Event.java`

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
> `/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/event/domain/entity/Enrollment.java`
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

