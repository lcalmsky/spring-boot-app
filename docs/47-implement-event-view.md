![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 6a61511)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 6a61511
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

스터디 내 모임 만들기 뷰를 구현합니다.

## Endpoint 작성

먼저 `event` 패키지를 생성하고 하위에 `EventController` 클래스를 생성합니다.

`/src/main/java/io/lcalmsky/app/event/endpoint/EventController.java`

```java
package io.lcalmsky.app.event.endpoint;

import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.support.CurrentUser;
import io.lcalmsky.app.event.form.EventForm;
import io.lcalmsky.app.study.application.StudyService;
import io.lcalmsky.app.study.domain.entity.Study;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/study/{path}")
@RequiredArgsConstructor
public class EventController {

    private final StudyService studyService; // (1)

    @GetMapping("/new-event")
    public String newEventForm(@CurrentUser Account account, @PathVariable String path, Model model) {
        Study study = studyService.getStudyToUpdateStatus(account, path); // (2)
        model.addAttribute(study); // (3)
        model.addAttribute(account);
        model.addAttribute(new EventForm());
        return "event/form";
    }
}
```

1. 스터디 정보를 불러오기 위해 StudyService를 주입합니다.
2. 단순 스터디 정보만 불러오면 되는데 관리자 정보까지 가져오는 메서드를 '일단' 사용합니다. 나중에 리팩터링을 통해 스터디 정보만 가져올 수 있게 수정해야 할 거 같습니다.
3. 모델에 스터디 정보, 계정 정보, 모임 생성 관련 폼을 전달합니다.

## EventForm 작성

모임 정보를 넘겨줄 EventForm 클래스를 생성하여 아래와 같이 작성합니다.

`/src/main/java/io/lcalmsky/app/event/form/EventForm.java`

```java
package io.lcalmsky.app.event.form;

import io.lcalmsky.app.event.domain.entity.EventType;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
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
    
}
```

모임 생성에 필요한 필드 변수들을 선언하였고 날짜 관련 필드에는 `@DateTimeFormat` 애너테이션을 이용해 `ISO` 표준 타입으로 지정하였습니다.

## 뷰 작성

리소스 경로에 마찬가지로 모임 관련 경로(`event`)를 생성하고 하위에 `form.html` 파일을 작성합니다.

`/src/main/resources/templates/event/form.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<body class="bg-light">
    <nav th:replace="fragments.html :: navigation-bar"></nav>
    <div th:replace="fragments.html :: study-banner"></div>
    <div class="container">
        <div class="py-5 text-center">
            <h2><a th:href="@{'/study/' + ${study.path}}"><span th:text="${study.title}">스터디</span></a> / 새 모임 만들기</h2>
        </div>
        <div class="row justify-content-center">
            <form class="needs-validation col-sm-10" th:action="@{'/study/' + ${study.path} + '/new-event'}"
                  th:object="${eventForm}" method="post" novalidate>
                <div class="form-group mt-3">
                    <label for="title">모임 이름</label>
                    <input id="title" type="text" th:field="*{title}" class="form-control"
                           placeholder="모임 이름" aria-describedby="titleHelp" required>
                    <small id="titleHelp" class="form-text text-muted">
                        모임 이름을 50자 이내로 입력하세요.
                    </small>
                    <small class="invalid-feedback">모임 이름을 입력하세요.</small>
                    <small class="form-text text-danger" th:if="${#fields.hasErrors('title')}" th:errors="*{title}">Error</small>
                </div>
                <div class="form-group mt-3">
                    <label for="eventType">모집 방법</label>
                    <select th:field="*{eventType}"  class="form-select me-sm-2" id="eventType" aria-describedby="eventTypeHelp">
                        <option th:value="FCFS">선착순</option>
                        <option th:value="CONFIRMATIVE">관리자 확인</option>
                    </select>
                    <small id="eventTypeHelp" class="form-text text-muted">
                        두가지 모집 방법이 있습니다.<br/>
                        <strong>선착순</strong>으로 모집하는 경우, 모집 인원 이내의 접수는 자동으로 확정되며, 제한 인원을 넘는 신청은 대기 신청이 되며 이후에 확정된 신청 중에 취소가 발생하면 선착순으로 대기 신청자를 확정 신청자도 변경합니다. 단, 등록 마감일 이후에는 취소해도 확정 여부가 바뀌지 않습니다.<br/>
                        <strong>관리자 확인</strong>으로 모집하는 경우, 모임 및 스터디 관리자가 모임 신청 목록을 조회하고 직접 확정 여부를 정할 수 있습니다. 등록 마감일 이후에는 변경할 수 없습니다.
                    </small>
                </div>
                <div class="row">
                    <div class="form-group col-md-3 mt-3">
                        <label for="limitOfEnrollments">모집 인원</label>
                        <input id="limitOfEnrollments" type="number" th:field="*{limitOfEnrollments}" class="form-control" placeholder="0"
                               aria-describedby="limitOfEnrollmentsHelp">
                        <small id="limitOfEnrollmentsHelp" class="form-text text-muted">
                            최대 수용 가능한 모임 참석 인원을 설정하세요. 최소 2인 이상 모임이어야 합니다.
                        </small>
                        <small class="invalid-feedback">모임 신청 마감 일시를 입력하세요.</small>
                        <small class="form-text text-danger" th:if="${#fields.hasErrors('limitOfEnrollments')}" th:errors="*{limitOfEnrollments}">Error</small>
                    </div>
                    <div class="form-group col-md-3 mt-3">
                        <label for="endEnrollmentDateTime">등록 마감 일시</label>
                        <input id="endEnrollmentDateTime" type="datetime-local" th:field="*{endEnrollmentDateTime}" class="form-control"
                               aria-describedby="endEnrollmentDateTimeHelp" required>
                        <small id="endEnrollmentDateTimeHelp" class="form-text text-muted">
                            등록 마감 이전에만 스터디 모임 참가 신청을 할 수 있습니다.
                        </small>
                        <small class="invalid-feedback">모임 신청 마감 일시를 입력하세요.</small>
                        <small class="form-text text-danger" th:if="${#fields.hasErrors('endEnrollmentDateTime')}" th:errors="*{endEnrollmentDateTime}">Error</small>
                    </div>
                    <div class="form-group col-md-3 mt-3">
                        <label for="startDateTime">모임 시작 일시</label>
                        <input id="startDateTime" type="datetime-local" th:field="*{startDateTime}" class="form-control"
                               aria-describedby="startDateTimeHelp" required>
                        <small id="startDateTimeHelp" class="form-text text-muted">
                            모임 시작 일시를 입력하세요. 상세한 모임 일정은 본문에 적어주세요.
                        </small>
                        <small class="invalid-feedback">모임 시작 일시를 입력하세요.</small>
                        <small class="form-text text-danger" th:if="${#fields.hasErrors('startDateTime')}" th:errors="*{startDateTime}">Error</small>
                    </div>
                    <div class="form-group col-md-3 mt-3">
                        <label for="startDateTime">모임 종료 일시</label>
                        <input id="endDateTime" type="datetime-local" th:field="*{endDateTime}" class="form-control"
                               aria-describedby="endDateTimeHelp" required>
                        <small id="endDateTimeHelp" class="form-text text-muted">
                            모임 종료 일시가 지나면 모임은 자동으로 종료 상태로 바뀝니다.
                        </small>
                        <small class="invalid-feedback">모임 종료 일시를 입력하세요.</small>
                        <small class="form-text text-danger" th:if="${#fields.hasErrors('endDateTime')}" th:errors="*{endDateTime}">Error</small>
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
                    <small class="form-text text-danger" th:if="${#fields.hasErrors('description')}" th:errors="*{description}">Error</small>
                </div>
                <div class="form-group mt-3 d-grid">
                    <button class="btn btn-primary btn-block" type="submit"
                            aria-describedby="submitHelp">모임 만들기</button>
                </div>
            </form>
        </div>
        <div th:replace="fragments.html :: footer"></div>
    </div>
    <script th:replace="fragments.html :: form-validation"></script>
    <script th:replace="fragments.html :: editor-script"></script>
</body>
</html>
```

모집 방법에 `form-select`를 사용하여 선택할 수 있게 하였고, 날짜 관련해서는 `datetime-local` 타입을 이용해 브라우저에서 지원해주는 입력 타입을 사용하였습니다.

나머지는 기존에 작성했던 것과 동일합니다.

## 테스트

애플리케이션 실행 후 스터디 화면으로 진입합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/47-01.md)

모임 만들기 버튼을 클릭하여 뷰를 확인합니다. 

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/47-02.md)

모집 방법을 클릭했을 때 동작을 확인합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/47-03.md)

날짜 관련 입력을 위해 달력 모양 버튼을 클릭했을 때 동작을 확인합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/47-04.md)

---

테스트 코드는 실제 생성 기능 구현 이후 같이 추가할 예정입니다.