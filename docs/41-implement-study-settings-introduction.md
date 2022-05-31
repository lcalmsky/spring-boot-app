![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 207c09b)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 207c09b
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

이번 포스팅 부터는 스터디 설정 기능을 차례대로 구현합니다.

먼저 소개 수정 기능을 구현합니다.

이전에 구현했던 프로필 설정(수정)과 유사합니다.

## 엔드포인트 추가

스터디 설정 뷰로 진입할 수 있는 `StudySettingsController`를 추가합니다.

`/src/main/java/io/lcalmsky/app/study/endpoint/StudySettingsController.java`

```java
package io.lcalmsky.app.modules.study.endpoint;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.support.CurrentUser;
import io.lcalmsky.app.modules.study.application.StudyService;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.endpoint.form.StudyDescriptionForm;
import io.lcalmsky.app.modules.study.infra.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/study/{path}/settings")
@RequiredArgsConstructor
public class StudySettingsController {
    private final StudyRepository studyRepository;
    private final StudyService studyService;

    @GetMapping("/description")
    public String viewStudySetting(@CurrentUser Account account, @PathVariable String path, Model model) {
        Study study = studyService.getStudy(account, path);
        model.addAttribute(account);
        model.addAttribute(study);
        model.addAttribute(StudyDescriptionForm.builder()
                .shortDescription(study.getShortDescription())
                .fullDescription(study.getFullDescription())
                .build());
        return "study/settings/description";
    }

    @PostMapping("/description")
    public String updateStudy(@CurrentUser Account account, @PathVariable String path, @Valid StudyDescriptionForm studyDescriptionForm, Errors errors, Model model, RedirectAttributes attributes) {
        Study study = studyService.getStudy(account, path);
        if (errors.hasErrors()) {
            model.addAttribute(account);
            model.addAttribute(study);
            return "study/settings/description";
        }
        studyService.updateStudyDescription(study, studyDescriptionForm);
        attributes.addFlashAttribute("message", "스터디 소개를 수정했습니다.");
        return "redirect:/study/" + encode(path) + "/settings/description";
    }

    private String encode(String path) {
        return URLEncoder.encode(path, StandardCharsets.UTF_8);
    }
}
```

대부분 이전 내용과 중복되는 내용이라 따로 설명할 것은 없을 거 같네요.

현재 상태에서는 컴파일 에러가 많이 발생할텐데 미 구현된 부분을 차례대로 구현해보도록 하겠습니다.

> Study를 조회하는 기능을 `StudyService`에 위임했는데, 이전에 `StudyController`에서도 동일한 기능이 있으므로 추후 수정해주도록 하겠습니다.

## 폼 클래스 추가

스터디 소개 화면에서 스터디 소개 관련 파라미터들을 받기위해 클래스를 생성합니다.

`/src/main/java/io/lcalmsky/app/study/form/StudyDescriptionForm.java`

```java
package io.lcalmsky.app.modules.study.endpoint.form;

import lombok.*;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class StudyDescriptionForm {
    @NotBlank
    @Length(max = 100)
    private String shortDescription;

    @NotBlank
    private String fullDescription;
}
```

## 서비스 수정

`StudyService`에 `Study`를 조회해오는 기능과 `Study`를 업데이트 하는 기능이 필요합니다.

`/src/main/java/io/lcalmsky/app/study/application/StudyService.java`

```java
// 생략
@Service
@RequiredArgsConstructor
@Transactional
public class StudyService {
    private final StudyRepository studyRepository;
    // 생략
    public Study getStudy(Account account, String path) { // (2)
        Study study = getStudy(path);
        if (!account.isManagerOf(study)) {
            throw new AccessDeniedException("해당 기능을 사용할 수 없습니다.");
        }
        return study;
    }

    private Study getStudy(String path) { // (1)
        Study study = studyRepository.findByPath(path);
        if (study == null) {
            throw new IllegalArgumentException(path + "에 해당하는 스터디가 없습니다.");
        }
        return study;
    }

    public void updateStudyDescription(Study study, StudyDescriptionForm studyDescriptionForm) { // (3)
        study.updateDescription(studyDescriptionForm);
    }
}

```

1. 계정과 스터디 path를 전달받아 DB에 조회하고 해당 스터디가 존재하지 않으면 예외를 던집니다.
2. 해당 계정이 스터디의 관리자가 아니어도 예외를 던집니다.
3. 스터디 소개 관련 파라미터를 전달받아 업데이트 합니다.

<details>
<summary>StudyService.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.study.application;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.endpoint.form.StudyDescriptionForm;
import io.lcalmsky.app.modules.study.endpoint.form.StudyForm;
import io.lcalmsky.app.modules.study.infra.repository.StudyRepository;
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
        Study study = getStudy(path);
        if (!account.isManagerOf(study)) {
            throw new AccessDeniedException("해당 기능을 사용할 수 없습니다.");
        }
        return study;
    }

    private Study getStudy(String path) {
        Study study = studyRepository.findByPath(path);
        if (study == null) {
            throw new IllegalArgumentException(path + "에 해당하는 스터디가 없습니다.");
        }
        return study;
    }

    public void updateStudyDescription(Study study, StudyDescriptionForm studyDescriptionForm) {
        study.updateDescription(studyDescriptionForm);
    }
}
```

</details>

마찬가지로 컴파일에러가 발생하는 부분을 아래 챕터에서 추가하겠습니다.

> 그 전에 `StudyController`에서 `Study`를 조회하는 기능도 `StudyService`로 위임해주겠습니다. 
> 오늘 구현할 기능과는 상관 없는 부분이라 아래를 열어서 확인해주세요.
> 
> `/src/main/java/io/lcalmsky/app/study/endpoint/StudyController.java`
> 
> <details>
> <summary>StudyController.java 수정</summary>
> 
> ```java
> package io.lcalmsky.app.modules.study.endpoint;
> 
> import io.lcalmsky.app.modules.account.domain.entity.Account;
> import io.lcalmsky.app.modules.account.support.CurrentUser;
> import io.lcalmsky.app.modules.study.application.StudyService;
> import io.lcalmsky.app.modules.study.domain.entity.Study;
> import io.lcalmsky.app.modules.study.endpoint.form.StudyForm;
> import io.lcalmsky.app.modules.study.endpoint.form.validator.StudyFormValidator;
> import io.lcalmsky.app.modules.study.infra.repository.StudyRepository;
> import lombok.RequiredArgsConstructor;
> import org.springframework.stereotype.Controller;
> import org.springframework.ui.Model;
> import org.springframework.validation.Errors;
> import org.springframework.web.bind.WebDataBinder;
> import org.springframework.web.bind.annotation.GetMapping;
> import org.springframework.web.bind.annotation.InitBinder;
> import org.springframework.web.bind.annotation.PathVariable;
> import org.springframework.web.bind.annotation.PostMapping;
> 
> import javax.validation.Valid;
> import java.net.URLEncoder;
> import java.nio.charset.StandardCharsets;
> 
> @Controller
> @RequiredArgsConstructor
> public class StudyController {
>     private final StudyService studyService;
>     private final StudyFormValidator studyFormValidator;
> 
>     @InitBinder("studyForm")
>     public void studyFormInitBinder(WebDataBinder webDataBinder) {
>         webDataBinder.addValidators(studyFormValidator);
>     }
> 
>     @GetMapping("/new-study")
>     public String newStudyForm(@CurrentUser Account account, Model model) {
>         model.addAttribute(account);
>         model.addAttribute(new StudyForm());
>         return "study/form";
>     }
> 
>     @PostMapping("/new-study")
>     public String newStudySubmit(@CurrentUser Account account, @Valid StudyForm studyForm, Errors errors) {
>         if (errors.hasErrors()) {
>             return "study/form";
>         }
>         Study newStudy = studyService.createNewStudy(studyForm, account);
>         return "redirect:/study/" + URLEncoder.encode(newStudy.getPath(), StandardCharsets.UTF_8);
>     }
> 
>     @GetMapping("/study/{path}")
>     public String viewStudy(@CurrentUser Account account, @PathVariable String path, Model model) {
>         model.addAttribute(account);
>         model.addAttribute(studyService.getStudy(account, path));
>         return "study/view";
>     }
> 
>     @GetMapping("/study/{path}/members")
>     public String viewStudyMembers(@CurrentUser Account account, @PathVariable String path, Model model) {
>         model.addAttribute(account);
>         model.addAttribute(studyService.getStudy(account, path));
>         return "study/members";
>     }
> }
> ```
> 
> </details>
> 
> 
> 유사한 기능이 `AccountController`와 `AccountService`에도 있는데요, `AccountController`에 있던 내용을 `AccountService`로 위임하겠습니다.
> 
> `/src/main/java/io/lcalmsky/app/account/endpoint/controller/AccountController.java`
> 
> ```java
> // 생략
> @Controller
> @RequiredArgsConstructor
> public class AccountController {
> 
>     private final AccountService accountService;
>     private final SignUpFormValidator signUpFormValidator;
>     private final AccountRepository accountRepository;
>     // 생략
>     @GetMapping("/profile/{nickname}")
>     public String viewProfile(@PathVariable String nickname, Model model, @CurrentUser Account account) {
>         Account accountByNickname = accountService.getAccountBy(nickname);
>         model.addAttribute(accountByNickname);
>         model.addAttribute("isOwner", accountByNickname.equals(account));
>         return "account/profile";
>     }
> }
> ```
> 
> `/src/main/java/io/lcalmsky/app/account/application/AccountService.java`
> 
> ```java
> // 생략
> @Service
> @RequiredArgsConstructor
> @Transactional
> @Slf4j
> public class AccountService implements UserDetailsService {
>     // 생략
>     public Account getAccountBy(String nickname) {
>         return Optional.ofNullable(accountRepository.findByNickname(nickname))
>                 .orElseThrow(() -> new IllegalArgumentException(nickname + "에 해당하는 사용자가 없습니다."));
>     }
> }
> ```

## Entity 수정

`StudyService`에서 `Account`와 `Study`에 위임하는 메서드들을 구현합니다.

`/src/main/java/io/lcalmsky/app/account/domain/entity/Account.java`

```java
// 생략
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder @Getter @ToString
public class Account extends AuditingEntity {
    // 생략
    public boolean isManagerOf(Study study) {
        return study.getManagers().contains(this);
    }
    // 생략
}
```

현재 계정이 `Study`의 관리자인지 확인합니다.

<details>
<summary>Account.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.account.domain.entity;

import io.lcalmsky.app.modules.account.domain.entity.AuditingEntity;
import io.lcalmsky.app.modules.account.endpoint.controller.form.NotificationForm;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.tag.domain.entity.Tag;
import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder @Getter @ToString
public class Account extends AuditingEntity {

    @Id @GeneratedValue
    @Column(name = "account_id")
    private Long id;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String nickname;

    private String password;

    private boolean isValid;

    private String emailToken;

    private LocalDateTime joinedAt;

    @Embedded
    private Profile profile = new Profile();

    @Embedded
    private NotificationSetting notificationSetting = new NotificationSetting();

    private LocalDateTime emailTokenGeneratedAt;

    @ManyToMany @ToString.Exclude
    private Set<Tag> tags = new HashSet<>();

    @ManyToMany @ToString.Exclude
    private Set<Zone> zones = new HashSet<>();

    public static Account with(String email, String nickname, String password) {
        Account account = new Account();
        account.email = email;
        account.nickname = nickname;
        account.password = password;
        return account;
    }

    public void generateToken() {
        this.emailToken = UUID.randomUUID().toString();
        this.emailTokenGeneratedAt = LocalDateTime.now();
    }

    public boolean enableToSendEmail() {
        return this.emailTokenGeneratedAt.isBefore(LocalDateTime.now().minusMinutes(5));
    }

    public void verified() {
        this.isValid = true;
        joinedAt = LocalDateTime.now();
    }

    @PostLoad
    private void init() {
        if (profile == null) {
            profile = new Profile();
        }
        if (notificationSetting == null) {
            notificationSetting = new NotificationSetting();
        }
    }

    public void updateProfile(io.lcalmsky.app.modules.account.endpoint.controller.form.Profile profile) {
        if (this.profile == null) {
            this.profile = new Profile();
        }
        this.profile.bio = profile.getBio();
        this.profile.url = profile.getUrl();
        this.profile.job = profile.getJob();
        this.profile.location = profile.getLocation();
        this.profile.image = profile.getImage();
    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    public void updateNotification(NotificationForm notificationForm) {
        this.notificationSetting.studyCreatedByEmail = notificationForm.isStudyCreatedByEmail();
        this.notificationSetting.studyCreatedByWeb = notificationForm.isStudyCreatedByWeb();
        this.notificationSetting.studyUpdatedByWeb = notificationForm.isStudyUpdatedByWeb();
        this.notificationSetting.studyUpdatedByEmail = notificationForm.isStudyUpdatedByEmail();
        this.notificationSetting.studyRegistrationResultByEmail = notificationForm.isStudyRegistrationResultByEmail();
        this.notificationSetting.studyRegistrationResultByWeb = notificationForm.isStudyRegistrationResultByWeb();
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public boolean isValid(String token) {
        return this.emailToken.equals(token);
    }

    public boolean isManagerOf(Study study) {
        return study.getManagers().contains(this);
    }

    @Embeddable
    @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder @Getter @ToString
    public static class Profile {
        private String bio;
        private String url;
        private String job;
        private String location;
        private String company;

        @Lob @Basic(fetch = FetchType.EAGER)
        private String image;
    }

    @Embeddable
    @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder @Getter @ToString
    public static class NotificationSetting {
        private boolean studyCreatedByEmail = false;
        private boolean studyCreatedByWeb = true;
        private boolean studyRegistrationResultByEmail = false;
        private boolean studyRegistrationResultByWeb = true;
        private boolean studyUpdatedByEmail = false;
        private boolean studyUpdatedByWeb = true;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        Account account = (Account) o;
        return id != null && Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
```

</details>

`Study Entity`에 스터디 수정 내용을 업데이트하는 메서드를 추가합니다.

`/src/main/java/io/lcalmsky/app/study/domain/entity/Study.java`

```java
// 생략
public class Study {
    // 생략
    public void updateDescription(StudyDescriptionForm studyDescriptionForm) {
        this.shortDescription = studyDescriptionForm.getShortDescription();
        this.fullDescription = studyDescriptionForm.getFullDescription();
    }
}
```

<details>
<summary>`Study`.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.study.domain.entity;

import io.lcalmsky.app.modules.account.domain.UserAccount;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.domain.entity.Zone;
import io.lcalmsky.app.modules.study.endpoint.form.StudyDescriptionForm;
import io.lcalmsky.app.modules.study.endpoint.form.StudyForm;
import io.lcalmsky.app.modules.tag.domain.entity.Tag;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
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
}

```

</details>

## 뷰 작성

먼저 공통으로 사용할 부분을 `fragments.html`에 작성하겠습니다.

`/src/main/resources/templates/fragments.html`

```html
<!DOCTYPE html>
<html lang="en"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<!-- 생략 -->
<div th:fragment="study-settings-menu (currentMenu)" class="list-group">
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'description'}? active"
       href="#" th:href="@{'/study/' + ${study.path} + '/settings/description'}">소개</a>
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'image'}? active"
       href="#" th:href="@{'/study/' + ${study.path} + '/settings/banner'}">배너 이미지</a>
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'tags'}? active"
       href="#" th:href="@{'/study/' + ${study.path} + '/settings/tags'}">스터디 주제</a>
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'zones'}? active"
       href="#" th:href="@{'/study/' + ${study.path} + '/settings/zones'}">활동 지역</a>
    <a class="list-group-item list-group-item-action list-group-item-danger" th:classappend="${currentMenu == 'study'}? active"
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

</html>
```

스터디 설정 메뉴와 성공시 피드백을 줄 메시지, 그리고 WYSIWYG 에디터 스크립트 부분을 `fragments`에 추가하였습니다.

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
        <div class="col-4 text-right justify-content-end">
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
                <a class="btn btn-outline-warning" th:href="@{'/study/' + ${study.path} + '/leave'}">
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
                    <i class>a fa-plus"></i> 모임 만들기
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
        <a class="nav-item nav-link" href="#" th:classappend="${studyMenu == 'info'}? active" th:href="@{'/study/' + ${study.path}}">
            <i class="fa fa-info-circle"></i> 소개
        </a>
        <a class="nav-item nav-link" href="#" th:classappend="${studyMenu == 'members'}? active" th:href="@{'/study/' + ${study.path} + '/members'}">
            <i class="fa fa-user"></i> 구성원
        </a>
        <a class="nav-item nav-link" th:classappend="${studyMenu == 'events'}? active" href="#" th:href="@{'/study/' + ${study.path} + '/events'}">
            <i class="fa fa-calendar"></i> 모임
        </a>
        <a sec:authorize="isAuthenticated()" th:if="${study.isManager(#authentication.principal)}"
           class="nav-item nav-link" th:classappend="${studyMenu == 'settings'}? active" href="#" th:href="@{'/study/' + ${study.path} + '/settings/description'}">
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
                <svg th:if="${#strings.isEmpty(member?.profile?.image)}" th:data-jdenticon-value="${member.nickname}" width="64" height="64" class="rounded border bg-light me-3"></svg>
                <img th:if="${!#strings.isEmpty(member?.profile?.image)}" th:src="${member?.profile?.image}" width="64" height="64" class="rounded border me-3"/>
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
    <a class="list-group-item list-group-item-action list-group-item-danger" th:classappend="${currentMenu == 'study'}? active"
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

</html>
```

</details>

다음으로 `study` 하위에 `settings` 디렉토리를 생성하고 `description.html` 파일을 생성합니다.

`/src/main/resources/templates/study/settings/description.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<body>
    <svg th:replace="fragments.html::svg-symbols"/>
    <nav th:replace="fragments.html :: navigation-bar"></nav>
    <div th:replace="fragments.html :: study-banner"></div>
    <div class="container">
        <div th:replace="fragments.html :: study-info"></div>
        <div th:replace="fragments.html :: study-menu(studyMenu='settings')"></div>
        <div class="row mt-3 justify-content-center">
            <div class="col-2">
                <div th:replace="fragments.html :: study-settings-menu(currentMenu='description')"></div>
            </div>
            <div class="col-8">
                <div th:replace="fragments.html :: message"></div>
                <form class="needs-validation" th:action="@{'/study/' + ${study.getPath()} + '/settings/description'}"
                      th:object="${studyDescriptionForm}" method="post" novalidate>
                    <div class="form-group my-3">
                        <label for="shortDescription">짧은 소개</label>
                        <textarea id="shortDescription" type="text" th:field="*{shortDescription}" class="form-control"
                                  placeholder="짧은 소개" aria-describedby="shortDescriptionHelp" required maxlength="100"></textarea>
                        <small id="shortDescriptionHelp" class="form-text text-muted">
                            100자 이하로 스터디를 짧게 소개해 주세요.
                        </small>
                        <small class="invalid-feedback">짧은 소개를 입력하세요.</small>
                        <small class="form-text text-danger" th:if="${#fields.hasErrors('shortDescription')}"
                               th:errors="*{shortDescription}">ShortDescription Error</small>
                    </div>

                    <div class="form-group my-3">
                        <label for="fullDescription">상세 소개</label>
                        <textarea id="fullDescription" type="text" th:field="*{fullDescription}" class="form-control"
                                  placeholder="스터디를 자세히 설명해 주세요." aria-describedby="fullDescriptionHelp" required></textarea>
                        <small class="invalid-feedback">상세 소개를 입력하세요.</small>
                        <small class="form-text text-danger" th:if="${#fields.hasErrors('fullDescription')}"
                               th:errors="*{fullDescription}">FullDescription Error</small>
                    </div>
                    <div class="form-group">
                        <button class="btn btn-primary btn-block" type="submit"
                                aria-describedby="submitHelp">수정하기</button>
                    </div>
                </form>
            </div>
        </div>
        <div th:replace="fragments.html :: footer"></div>
    </div>
    <script th:replace="fragments.html :: tooltip"></script>
    <script th:replace="fragments.html :: form-validation"></script>
    <script th:replace="fragments.html :: editor-script"></script>
</body>
</html>
```

스터디 생성할 때 쓰였던 `form.html`에 WYSIWYG 에디터 스크립트 부분도 `fragments`를 이용하도록 수정하였습니다.

`/src/main/resources/templates/study/form.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html::head">
</head>
<body class="bg-light">
<div th:replace="fragments.html::navigation-bar"></div>
<div class="container">
    <!-- 생략 -->
    <script th:replace="fragments::form-validation"></script>
    <script th:replace="fragments.html::editor-script"></script>
</div>
</body>
</html>
```

<details>
<summary>form.html 전체 보기</summary>

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html::head">
</head>
<body class="bg-light">
<div th:replace="fragments.html::navigation-bar"></div>
<div class="container">
    <div class="py-5 text-center">
        <h2>스터디 개설</h2>
    </div>
    <div class="row justify-content-center">
        <form class="needs-validation col-sm-10" action="#"
              th:action="@{/new-study}" th:object="${studyForm}" method="post" novalidate>
            <div class="form-group my-3">
                <label for="path">스터디 URL</label>
                <input id="path" type="text" th:field="*{path}" class="form-control"
                       placeholder="예) study-path" aria-describedby="pathHelp" required min="2" max="20"/>
                <small id="pathHelp" class="form-text text-muted">
                    공백 없이 문자, 숫자, 대시(-)와 언더스코어(_)만 2자 이상 20자 이하로 입력하세요. 스터디 홈 주소로 사용됩니다. 예) /study/<B>study-path</b>
                </small>
                <small class="invalid-feedback">스터디 경로를 입력하세요.</small>
                <small class="form-text text-danger" th:if="${#fields.hasErrors('path')}" th:errors="*{path}">Path
                    Error</small>
            </div>

            <div class="form-group my-3">
                <label for="title">스터디 이름</label>
                <input id="title" type="text" th:field="*{title}" class="form-control"
                       placeholder="스터디 이름" aria-describedby="titleHelp" required max="50"/>
                <small id="titleHelp" class="form-text text-muted">
                    스터디 이름을 50자 이하로 입력하세요.
                </small>
                <small class="invalid-feedback">스터디 이름을 입력하세요.</small>
                <small class="form-text text-danger" th:if="${#fields.hasErrors('title')}" th:errors="*{title}">Title
                    Error</small>
            </div>

            <div class="form-group my-3">
                <label for="shortDescription">짧은 소개</label>
                <textarea id="shortDescription" type="text" th:field="*{shortDescription}" class="form-control"
                          placeholder="짧은 소개" aria-describedby="shortDescriptionHelp" required maxlength="100"></textarea>
                <small id="shortDescriptionHelp" class="form-text text-muted">
                    100자 이하로 스터디를 짧게 소개해 주세요.
                </small>
                <small class="invalid-feedback">짧은 소개를 입력하세요.</small>
                <small class="form-text text-danger" th:if="${#fields.hasErrors('shortDescription')}"
                       th:errors="*{shortDescription}">ShortDescription Error</small>
            </div>

            <div class="form-group my-3">
                <label for="fullDescription">상세 소개</label>
                <textarea id="fullDescription" type="text" th:field="*{fullDescription}" class="form-control"
                          placeholder="스터디를 자세히 설명해 주세요." aria-describedby="fullDescriptionHelp" required></textarea>
                <small class="invalid-feedback">상세 소개를 입력하세요.</small>
                <small class="form-text text-danger" th:if="${#fields.hasErrors('fullDescription')}"
                       th:errors="*{fullDescription}">FullDescription Error</small>
            </div>

            <div class="form-group my-3 d-grid">
                <button class="btn btn-primary btn-block" type="submit" aria-describedby="submitHelp">스터디 생성</button>
            </div>

        </form>
    </div>
    <div th:replace="fragments::footer"></div>
    <script th:replace="fragments::form-validation"></script>
    <script th:replace="fragments.html::editor-script"></script>
</div>
</body>
</html>
```

</details>

## 테스트

여기까지 소스 코드를 수정 및 추가하였다면 애플리케이션을 실행한 뒤 스터디 화면에 진입한 뒤 설정 탭을 눌러 소개를 수정하는 페이지를 확인합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/41-01.png)


상세 소개를 수정한 뒤 수정하기 버튼을 클릭하여 소개 수정을 완료하였습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/41-02.png)

다시 소개 탭에 진입하여서 정상적으로 수정된 것을 추가로 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/41-03.png)

## 테스트 코드 작성

`StudySettingControllerTest` 클래스를 생성하고 테스트 코드를 작성합니다.

`/src/test/java/io/lcalmsky/app/study/endpoint/StudySettingsControllerTest.java`

```java
package io.lcalmsky.app.modules.study.endpoint;

import io.lcalmsky.app.modules.account.WithAccount;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
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
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class StudySettingsControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired AccountRepository accountRepository;
    @Autowired StudyRepository studyRepository;
    @Autowired StudyService studyService;
    private final String studyPath = "study-test";

    @BeforeEach
    void beforeEach() {
        Account account = accountRepository.findByNickname("jaime");
        studyService.createNewStudy(StudyForm.builder()
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
    @DisplayName("스터디 세팅 폼 조회")
    @WithAccount("jaime")
    void studyForm() throws Exception {
        mockMvc.perform(get("/study/" + studyPath + "/settings/description"))
                .andExpect(status().isOk())
                .andExpect(view().name("study/settings/description"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("studyDescriptionForm"));
    }

    @Test
    @DisplayName("스터디 세팅 수정: 정상")
    @WithAccount("jaime")
    void createStudy() throws Exception {
        Account account = accountRepository.findByNickname("jaime");
        String shortDescriptionToBeUpdated = "short-description-test";
        String fullDescriptionToBeUpdated = "full-description-test";
        mockMvc.perform(post("/study/" + studyPath + "/settings/description")
                        .param("shortDescription", shortDescriptionToBeUpdated)
                        .param("fullDescription", fullDescriptionToBeUpdated)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/settings/description"));
        Study study = studyService.getStudy(account, studyPath);
        assertEquals(shortDescriptionToBeUpdated, study.getShortDescription());
        assertEquals(fullDescriptionToBeUpdated, study.getFullDescription());
    }
}
```

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/41-04.png)

모든 테스트를 통과하였습니다!