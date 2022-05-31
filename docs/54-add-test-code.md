![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: ebf7e54)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout ebf7e54
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

모임 관련 테스트 코드를 작성합니다.

## 오류 수정

테스트 코드 작성 중 발생한 오류들을 수정하겠습니다.

먼저 `Entity`의 `Collection` 필드를 초기화하지 않아 에러가 발생하였습니다.

`Event.java`의 필드를 초기화해 줍니다.

`src/main/java/io/lcalmsky/app/event/domain/entity/Event.java`

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
    private List<Enrollment> enrollments = new ArrayList<>();
    // 생략
}
```

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

다음으로 `EventController`에서 `@RequestBody`를 지정하지 않았을 때 제대로 매핑되지 않는 현상이 있었습니다.

일단 저는 `@RequestBody` 애너테이션을 추가하니 해결되었는데, 이런 현상이 왜 테스트 코드에서 발생하는지 아시는 분 제보 부탁드립니다.

> 제 개인적인 생각으로는 애플리케이션 실행시에는 thymeleaf 엔진이 같이 동작하기 때문에 알아서 매핑을 잘 해주는 거 같은데 테스트 코드만 실행했을 때는 그런 부분이 자동으로 이루어지지 않는 것 같습니다.

`src/main/java/io/lcalmsky/app/event/endpoint/EventController.java`

```java
// 생략
@Controller
@RequestMapping("/study/{path}")
@RequiredArgsConstructor
public class EventController {
    // 생략
    @PostMapping("/new-event")
    public String createNewEvent(@CurrentUser Account account, @PathVariable String path, @Valid @RequestBody EventForm eventForm, Errors errors, Model model) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        if (errors.hasErrors()) {
            model.addAttribute(account);
            model.addAttribute(study);
            return "event/form";
        }
        Event event = eventService.createEvent(study, eventForm, account);
        return "redirect:/study/" + study.getEncodedPath() + "/events/" + event.getId();
    }
    // 생략
    @PostMapping("/events/{id}/edit")
    public String updateEventSubmit(@CurrentUser Account account, @PathVariable String path, @PathVariable Long id, @Valid @RequestBody EventForm eventForm, Errors errors, Model model) {
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
    // 생략
}
```

`PostMapping` 중 실제로 `JSON` request body를 받는 `API`만 수정하였더니 정상동작 하였습니다.

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
    public String createNewEvent(@CurrentUser Account account, @PathVariable String path, @Valid @RequestBody EventForm eventForm, Errors errors, Model model) {
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
    public String updateEventSubmit(@CurrentUser Account account, @PathVariable String path, @PathVariable Long id, @Valid @RequestBody EventForm eventForm, Errors errors, Model model) {
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

## 테스트 코드 작성

미뤄왔던 테스트 코드라 양이 아주 많지만 그냥 쭉 읽기에 어려운 점은 딱히 없을 거 같습니다.

계정이 여러 개 필요하므로 API를 호출하는 주 계정(jaime)을 잘 생각해서 작성해야 합니다.

```java
package io.lcalmsky.app.modules.event.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lcalmsky.app.modules.account.WithAccount;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
import io.lcalmsky.app.modules.event.application.EventService;
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
import org.springframework.http.MediaType;
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
        EventForm eventForm = EventForm.builder()
                .description("description")
                .eventType(EventType.FCFS)
                .endDateTime(now.plusWeeks(3))
                .endEnrollmentDateTime(now.plusWeeks(1))
                .limitOfEnrollments(5)
                .startDateTime(now.plusWeeks(2))
                .title("title")
                .build();
        ResultActions resultActions = mockMvc.perform(post("/study/" + studyPath + "/new-event")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(eventForm))
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
        EventForm eventForm = EventForm.builder()
                .description("description")
                .eventType(EventType.FCFS)
                .endDateTime(now.plusWeeks(3))
                .endEnrollmentDateTime(now.plusWeeks(1))
                .limitOfEnrollments(5)
                .startDateTime(now.plusWeeks(4))
                .title("")
                .build();
        mockMvc.perform(post("/study/" + studyPath + "/new-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventForm))
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
        Event event = stubbingEvent();
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
        stubbingEvent();
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
        Event event = stubbingEvent();
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
        Event event = stubbingEvent();
        EventForm eventForm = EventForm.from(event);
        eventForm.setTitle("another");
        mockMvc.perform(post("/study/" + studyPath + "/events/" + event.getId() + "/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(eventForm))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/events/" + event.getId()));
    }

    @Test
    @DisplayName("모임 삭제")
    @WithAccount("jaime")
    void deleteEvent() throws Exception {
        Event event = stubbingEvent();
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
        Event event = stubbingEvent();
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
        Event event = stubbingEvent();
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
        Event event = stubbingEvent();
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
        Event event = stubbingEvent();
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

    private void isNotAccepted(Account account, Event event) {
        assertFalse(enrollmentRepository.findByEventAndAccount(event, account).isAccepted());
    }

    private void isAccepted(Account account, Event event) {
        assertTrue(enrollmentRepository.findByEventAndAccount(event, account).isAccepted());
    }

    private Event stubbingEvent() {
        Study study = studyRepository.findByPath(studyPath);
        Account account = createAccount("manager");
        LocalDateTime now = LocalDateTime.now();
        EventForm eventForm = EventForm.builder()
                .description("description")
                .eventType(EventType.FCFS)
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

## 테스트

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/54-01.png)

모든 테스트를 성공하였습니다!