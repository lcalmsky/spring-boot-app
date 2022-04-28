![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 036e467)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 036e467
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

스터디를 가입하고 탈퇴하는 기능을 구현합니다.

## 엔드포인트 추가

`StudyController`에 가입/삭제 엔드포인트를 추가합니다.

`/src/main/java/io/lcalmsky/app/study/endpoint/StudyController.java`

```java
// 생략
@Controller
@RequiredArgsConstructor
public class StudyController {
    private final StudyService studyService;
    private final StudyRepository studyRepository;
    // 생략
    @GetMapping("/study/{path}/join")
    public String joinStudy(@CurrentUser Account account, @PathVariable String path) {
        Study study = studyRepository.findStudyWithMembersByPath(path);
        studyService.addMember(study, account);
        return "redirect:/study/" + study.getEncodedPath() + "/members";
    }

    @GetMapping("/study/{path}/leave")
    public String leaveStudy(@CurrentUser Account account, @PathVariable String path) {
        Study study = studyRepository.findStudyWithMembersByPath(path);
        studyService.removeMember(study, account);
        return "redirect:/study/" + study.getEncodedPath() + "/members";
    }
}
```

가입과 탈퇴를 처리하기 위한 메서드를 추가하였습니다.

아직은 `Repository`와 `Service`에 미구현된 내용들이 존재하기 때문에 컴파일 에러가 발생합니다.

> 단순 조회가 아닌 상태 변경이 일어나는 API는 `GET`이 아닌 `POST`를 사용하는 것이 맞지만 강의에서 구현한 것을 그대로 작성하였습니다.

<details>
<summary>StudyController.java</summary>

```java
package io.lcalmsky.app.study.endpoint;

import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.support.CurrentUser;
import io.lcalmsky.app.study.application.StudyService;
import io.lcalmsky.app.study.domain.entity.Study;
import io.lcalmsky.app.study.form.StudyForm;
import io.lcalmsky.app.study.form.validator.StudyFormValidator;
import io.lcalmsky.app.study.infra.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
public class StudyController {
    private final StudyService studyService;
    private final StudyFormValidator studyFormValidator;
    private final StudyRepository studyRepository;

    @InitBinder("studyForm")
    public void studyFormInitBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(studyFormValidator);
    }

    @GetMapping("/new-study")
    public String newStudyForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(new StudyForm());
        return "study/form";
    }

    @PostMapping("/new-study")
    public String newStudySubmit(@CurrentUser Account account, @Valid StudyForm studyForm, Errors errors) {
        if (errors.hasErrors()) {
            return "study/form";
        }
        Study newStudy = studyService.createNewStudy(studyForm, account);
        return "redirect:/study/" + URLEncoder.encode(newStudy.getPath(), StandardCharsets.UTF_8);
    }

    @GetMapping("/study/{path}")
    public String viewStudy(@CurrentUser Account account, @PathVariable String path, Model model) {
        model.addAttribute(account);
        model.addAttribute(studyService.getStudy(account, path));
        return "study/view";
    }

    @GetMapping("/study/{path}/members")
    public String viewStudyMembers(@CurrentUser Account account, @PathVariable String path, Model model) {
        model.addAttribute(account);
        model.addAttribute(studyService.getStudy(account, path));
        return "study/members";
    }

    @GetMapping("/study/{path}/join")
    public String joinStudy(@CurrentUser Account account, @PathVariable String path) {
        Study study = studyRepository.findStudyWithMembersByPath(path);
        studyService.addMember(study, account);
        return "redirect:/study/" + study.getEncodedPath() + "/members";
    }

    @GetMapping("/study/{path}/leave")
    public String leaveStudy(@CurrentUser Account account, @PathVariable String path) {
        Study study = studyRepository.findStudyWithMembersByPath(path);
        studyService.removeMember(study, account);
        return "redirect:/study/" + study.getEncodedPath() + "/members";
    }
}
```

</details>

## 서비스 수정

`StudyService`에 회원 가입/탈퇴 기능을 구현합니다.

`/src/main/java/io/lcalmsky/app/study/application/StudyService.java`

```java
// 생략
public class StudyService {
    // 생략
    public void addMember(Study study, Account account) {
        study.addMember(account);
    }

    public void removeMember(Study study, Account account) {
        study.removeMember(account);
    }
}
```

`Study Entity`에 해당 기능을 위임하였습니다.

<details>
<summary>StudyService.java 전체 보기</summary>

```java
package io.lcalmsky.app.study.application;

import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.domain.entity.Zone;
import io.lcalmsky.app.study.domain.entity.Study;
import io.lcalmsky.app.study.form.StudyDescriptionForm;
import io.lcalmsky.app.study.form.StudyForm;
import io.lcalmsky.app.study.infra.repository.StudyRepository;
import io.lcalmsky.app.tag.domain.entity.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StudyService {
    private final StudyRepository studyRepository;

    public Study createNewStudy(StudyForm studyForm, Account account) {
        Study study = Study.from(studyForm);
        study.addManager(account);
        return studyRepository.save(study);
    }

    public Study getStudy(Account account, String path) {
        Study study = studyRepository.findByPath(path);
        checkStudyExists(path, study);
        return study;
    }

    public Study getStudyToUpdate(Account account, String path) {
        return getStudy(account, path, studyRepository.findByPath(path));
    }

    public Study getStudyToUpdateTag(Account account, String path) {
        return getStudy(account, path, studyRepository.findStudyWithTagsByPath(path));
    }

    public Study getStudyToUpdateZone(Account account, String path) {
        return getStudy(account, path, studyRepository.findStudyWithZonesByPath(path));
    }

    public Study getStudyToUpdateStatus(Account account, String path) {
        return getStudy(account, path, studyRepository.findStudyWithManagersByPath(path));
    }

    private Study getStudy(Account account, String path, Study studyByPath) {
        checkStudyExists(path, studyByPath);
        checkAccountIsManager(account, studyByPath);
        return studyByPath;
    }

    private void checkStudyExists(String path, Study study) {
        if (study == null) {
            throw new IllegalArgumentException(path + "에 해당하는 스터디가 없습니다.");
        }
    }

    private void checkAccountIsManager(Account account, Study study) {
        if (!account.isManagerOf(study)) {
            throw new AccessDeniedException("해당 기능을 사용할 수 없습니다.");
        }
    }

    public void updateStudyDescription(Study study, StudyDescriptionForm studyDescriptionForm) {
        study.updateDescription(studyDescriptionForm);
    }

    public void updateStudyImage(Study study, String image) {
        study.updateImage(image);
    }

    public void enableStudyBanner(Study study) {
        study.setBanner(true);
    }

    public void disableStudyBanner(Study study) {
        study.setBanner(false);
    }

    public void addTag(Study study, Tag tag) {
        study.addTag(tag);
    }

    public void removeTag(Study study, Tag tag) {
        study.removeTag(tag);
    }

    public void addZone(Study study, Zone zone) {
        study.addZone(zone);
    }

    public void removeZone(Study study, Zone zone) {
        study.removeZone(zone);
    }

    public void publish(Study study) {
        study.publish();
    }

    public void close(Study study) {
        study.close();
    }

    public void startRecruit(Study study) {
        study.startRecruit();
    }

    public void stopRecruit(Study study) {
        study.stopRecruit();
    }

    public boolean isValidPath(String newPath) {
        if (!newPath.matches(StudyForm.VALID_PATH_PATTERN)) {
            return false;
        }
        return !studyRepository.existsByPath(newPath);
    }

    public void updateStudyPath(Study study, String newPath) {
        study.updatePath(newPath);
    }

    public boolean isValidTitle(String newTitle) {
        return newTitle.length() <= 50;
    }

    public void updateStudyTitle(Study study, String newTitle) {
        study.updateTitle(newTitle);
    }

    public void remove(Study study) {
        if (!study.isRemovable()) {
            throw new IllegalStateException("스터디를 삭제할 수 없습니다.");
        }
        studyRepository.delete(study);
    }

    public void addMember(Study study, Account account) {
        study.addMember(account);
    }

    public void removeMember(Study study, Account account) {
        study.removeMember(account);
    }
}

```

</details>

## Entity, Repository 수정

먼저 `Study Entity`를 수정합니다.

`/src/main/java/io/lcalmsky/app/study/domain/entity/Study.java`

```java
// 생략
@NamedEntityGraph(name = "Study.withMembers", attributeNodes = {
        @NamedAttributeNode("members")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Study {
    // 생략
    public void addMember(Account account) {
        this.members.add(account);
    }

    public void removeMember(Account account) {
        this.members.remove(account);
    }

    public String getEncodedPath() {
        return URLEncoder.encode(path, StandardCharsets.UTF_8);
    }
}
```

이전 포스팅에서 다뤘던 `@NamedEntityGraph`를 스터디의 멤버만 fetch join 할 수 있게 추가해주었습니다.

스터디 멤버에 추가/삭제하는 기능과 `url`을 인코딩하여 반환하는 기능을 추가하였습니다.

> `StudySettingsController`에서 `encode`라는 메서드를 사용했었는데 `Study Entity`로 기능을 이전하였으니 이 부분도 같이 수정해주시면 됩니다.

<details>
<summary>Study.java 전체 보기</summary>

```java
package io.lcalmsky.app.study.domain.entity;

import io.lcalmsky.app.account.domain.UserAccount;
import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.domain.entity.Zone;
import io.lcalmsky.app.study.form.StudyDescriptionForm;
import io.lcalmsky.app.study.form.StudyForm;
import io.lcalmsky.app.tag.domain.entity.Tag;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@NamedEntityGraph(name = "Study.withAll", attributeNodes = {
        @NamedAttributeNode("tags"),
        @NamedAttributeNode("zones"),
        @NamedAttributeNode("managers"),
        @NamedAttributeNode("members")
})
@NamedEntityGraph(name = "Study.withTagsAndManagers", attributeNodes = {
        @NamedAttributeNode("tags"),
        @NamedAttributeNode("managers")
})
@NamedEntityGraph(name = "Study.withZonesAndManagers", attributeNodes = {
        @NamedAttributeNode("zones"),
        @NamedAttributeNode("managers")
})
@NamedEntityGraph(name = "Study.withManagers", attributeNodes = {
        @NamedAttributeNode("managers")
})
@NamedEntityGraph(name = "Study.withMembers", attributeNodes = {
        @NamedAttributeNode("members")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Study {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToMany
    private Set<Account> managers = new HashSet<>();

    @ManyToMany
    private Set<Account> members = new HashSet<>();

    @Column(unique = true)
    private String path;

    private String title;

    private String shortDescription;

    @Lob @Basic(fetch = FetchType.EAGER)
    private String fullDescription;

    @Lob @Basic(fetch = FetchType.EAGER)
    private String image;

    @ManyToMany
    private Set<Tag> tags = new HashSet<>();

    @ManyToMany
    private Set<Zone> zones = new HashSet<>();

    private LocalDateTime publishedDateTime;

    private LocalDateTime closedDateTime;

    private LocalDateTime recruitingUpdatedDateTime;

    private boolean recruiting;

    private boolean published;

    private boolean closed;

    @Accessors(fluent = true)
    private boolean useBanner;

    public static Study from(StudyForm studyForm) {
        Study study = new Study();
        study.title = studyForm.getTitle();
        study.shortDescription = studyForm.getShortDescription();
        study.fullDescription = studyForm.getFullDescription();
        study.path = studyForm.getPath();
        return study;
    }

    public void addManager(Account account) {
        managers.add(account);
    }

    public boolean isJoinable(UserAccount userAccount) {
        Account account = userAccount.getAccount();
        return this.isPublished() && this.isRecruiting() && !this.members.contains(account) && !this.managers.contains(account);
    }

    public boolean isMember(UserAccount userAccount) {
        return this.members.contains(userAccount.getAccount());
    }

    public boolean isManager(UserAccount userAccount) {
        return this.managers.contains(userAccount.getAccount());
    }

    public void updateDescription(StudyDescriptionForm studyDescriptionForm) {
        this.shortDescription = studyDescriptionForm.getShortDescription();
        this.fullDescription = studyDescriptionForm.getFullDescription();
    }

    public void updateImage(String image) {
        this.image = image;
    }

    public void setBanner(boolean useBanner) {
        this.useBanner = useBanner;
    }

    public void addTag(Tag tag) {
        this.tags.add(tag);
    }

    public void removeTag(Tag tag) {
        this.tags.remove(tag);
    }

    public void addZone(Zone zone) {
        this.zones.add(zone);
    }

    public void removeZone(Zone zone) {
        this.zones.remove(zone);
    }

    public void publish() {
        if (this.closed || this.published) {
            throw new IllegalStateException("스터디를 이미 공개했거나 종료된 스터디 입니다.");
        }
        this.published = true;
        this.publishedDateTime = LocalDateTime.now();
    }

    public void close() {
        if (!this.published || this.closed) {
            throw new IllegalStateException("스터디를 공개하지 않았거나 이미 종료한 스터디 입니다.");
        }
        this.closed = true;
        this.closedDateTime = LocalDateTime.now();
    }

    public boolean isEnableToRecruit() {
        return this.published && this.recruitingUpdatedDateTime == null
                || this.recruitingUpdatedDateTime.isBefore(LocalDateTime.now().minusHours(1));
    }

    public void updatePath(String newPath) {
        this.path = newPath;
    }

    public void updateTitle(String newTitle) {
        this.title = newTitle;
    }

    public boolean isRemovable() {
        return !this.published;
    }

    public void startRecruit() {
        if (!isEnableToRecruit()) {
            throw new RuntimeException("인원 모집을 시작할 수 없습니다. 스터디를 공개하거나 한 시간 뒤 다시 시도하세요.");
        }
        this.recruiting = true;
        this.recruitingUpdatedDateTime = LocalDateTime.now();
    }

    public void stopRecruit() {
        if (!isEnableToRecruit()) {
            throw new RuntimeException("인원 모집을 멈출 수 없습니다. 스터디를 공개하거나 한 시간 뒤 다시 시도하세요.");
        }
        this.recruiting = false;
        this.recruitingUpdatedDateTime = LocalDateTime.now();
    }

    public void addMember(Account account) {
        this.members.add(account);
    }

    public void removeMember(Account account) {
        this.members.remove(account);
    }

    public String getEncodedPath() {
        return URLEncoder.encode(path, StandardCharsets.UTF_8);
    }
}

```

</details>

다음으로 `StudyRepository`도 수정해줍니다.

`/src/main/java/io/lcalmsky/app/study/infra/repository/StudyRepository.java`

```java
// 생략
@Transactional(readOnly = true)
public interface StudyRepository extends JpaRepository<Study, Long> {
    // 생략
    @EntityGraph(value = "Study.withMembers", type = EntityGraph.EntityGraphType.FETCH)
    Study findStudyWithMembersByPath(String path);
}
```

`Study Entity`에 명시한 `NamedEntityGraph`를 사용하도록 설정하였습니다.

<details>
<summary>StudyRepository.java 전체 보기</summary>

```java
package io.lcalmsky.app.study.infra.repository;

import io.lcalmsky.app.study.domain.entity.Study;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

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
}
```

</details>

## 뷰 수정

이전 포스팅에서 이미 가입/탈퇴 버튼을 `fragments.html`에 만들어 놓았습니다.

따라서 별도로 구현할 필요는 없는데 버튼 css class를 수정하였습니다.

`/src/main/resources/templates/fragments.html`

```html
<a class="btn btn-outline-danger" th:href="@{'/study/' + ${study.path} + '/leave'}">
    스터디 탈퇴
</a>
```

`btn btn-outline-warning` -> `btn btn-outline-danger` 이렇게 수정하였는데 굳이 수정하지 않아도 잘 동작하므로 취향에 맞게 사용하시면 될 거 같습니다.

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

</html>
```

</details>

## 테스트

스터디 가입을 위해선 새로운 계정을 생성해야 합니다.

가입 및 이메일 인증까지 쭉쭉 진행해봅시다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/45-01.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/45-02.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/45-03.png)

그리고 미리 생성해 둔 스터디 화면에 진입하여 스터디 가입 버튼을 눌러 가입을 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/45-04.png)

가입 이후 구성원 화면으로 이동하고 관리자 밑에 가입한 계정이 추가되었음을 확인할 수 있습니다.

그리고 가입버튼이 탈퇴버튼으로 변경되었고 옆에는 스터디 구성원이 몇 명인지 숫자로 나타내주고 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/45-05.png)

탈퇴버튼을 누르면 다시 구성원에서 삭제되고 버튼도 가입버튼으로 돌아감을 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/45-06.png)

## 테스트 코드 작성

테스트 코드 작성에 앞서 앞으로 여러 계정을 생성해서 사용해야 하므로 `@WithAccount` 애너테이션을 수정하겠습니다.

`/src/test/java/io/lcalmsky/app/WithAccount.java`

```java
package io.lcalmsky.app;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithAccountSecurityContextFactory.class)
public @interface WithAccount {
    String[] value() default "";
}
```

value() 메서드의 반환 타입을 배열로 수정하였고 기본 값도 지정하였습니다.

다행히 배열로 수정하더라도 이전에 파라미터로 넘겨줬던 값들을 수정할 필요는 없습니다.

대신 `WithAccountSecurityContextFactory`에서는 에러가 발생하기 때문에 해당 클래스도 수정해주어야 합니다.

`/src/test/java/io/lcalmsky/app/WithAccountSecurityContextFactory.java`

```java
package io.lcalmsky.app;

import io.lcalmsky.app.account.application.AccountService;
import io.lcalmsky.app.account.endpoint.controller.SignUpForm;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithAccountSecurityContextFactory implements WithSecurityContextFactory<WithAccount> {

    private final AccountService accountService;

    public WithAccountSecurityContextFactory(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public SecurityContext createSecurityContext(WithAccount annotation) {
        String[] nicknames = annotation.value();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        for (String nickname : nicknames) {
            SignUpForm signUpForm = new SignUpForm();
            signUpForm.setNickname(nickname);
            signUpForm.setEmail(nickname + "@gmail.com");
            signUpForm.setPassword("1234asdf");
            accountService.signUp(signUpForm);
            UserDetails principal = accountService.loadUserByUsername(nickname);
            Authentication authentication = new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities());
            context.setAuthentication(authentication);
        }
        return context;
    }
}
```

그동안 `annotation.value()`를 통해 `nickname` 하나를 받아와 가입 및 `SecurityContext` 등록을 해주었었는데, 이제 `nickname`이 여러 개 전달될 수 있으므로 수정해주었습니다.

그리고 테스트 주체는 마지막에 등록되는 `nickname` 입니다. `SecurityContext`에 최종적으로 `setAuthentication`을 통해 인증 정보가 등록됩니다.

따라서 사용할 계정을 `@WithAccount(value = {"a", "b"})` 이런식으로 추가하여 사용한다고 하면 `test` 메서드 내에서 전달되는 `@CurrentUser`는 "b" 닉네임을 사용하는 `Account`가 됩니다.

이 점을 유의해서 테스트를 작성해보겠습니다.

`/src/test/java/io/lcalmsky/app/study/endpoint/StudyControllerTest.java`

```java
// 생략
class StudyControllerTest {
    // 생략
    @Test
    @DisplayName("스터디 가입")
    @WithAccount(value = {"jaime", "test"})
    void joinStudy() throws Exception {
        // 스터디 생성
        Account manager = accountRepository.findByNickname("jaime");
        String studyPath = "study-path";
        Study study = studyService.createNewStudy(StudyForm.builder()
                .path(studyPath)
                .title("study-title")
                .shortDescription("short-description")
                .fullDescription("full-description")
                .build(), manager);
        // 스터디 가입
        mockMvc.perform(get("/study/" + studyPath + "/join"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/members"));
        Account member = accountRepository.findByNickname("test");
        assertTrue(study.getMembers().contains(member));
    }

    @Test
    @DisplayName("스터디 탈퇴")
    @WithAccount(value = {"jaime", "test"})
    void leaveStudy() throws Exception {
        // 스터디 생성
        Account manager = accountRepository.findByNickname("jaime");
        String studyPath = "study-path";
        Study study = studyService.createNewStudy(StudyForm.builder()
                .path(studyPath)
                .title("study-title")
                .shortDescription("short-description")
                .fullDescription("full-description")
                .build(), manager);
        // 스터디 가입
        Account member = accountRepository.findByNickname("test");
        studyService.addMember(study, member);
        // 스터디 탈퇴
        mockMvc.perform(get("/study/" + studyPath + "/leave"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/members"));
        assertFalse(study.getMembers().contains(member));
    }
}
```

jaime 계정이 스터디를 생성한 관리자이고 test 계정이 스터디의 멤버가 될 계정입니다.

`StudyController`로 전달되는 `@CurrentUser`는 `@WithAccount`의 `attribute` 중 마지막으로 전달한 `test` 계정이 됩니다.

기존 테스틀 포함 스터디 가입 및 탈퇴 테스트가 정상적으로 수행되었습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/45-07.png)