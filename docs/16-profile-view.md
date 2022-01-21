![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 4213163)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 4213163
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

프로필 뷰를 구현합니다.

## Description

프로필은 누가 보느냐에 따라 달라지고 정보 입력 유무에 따라 달라져야 합니다.

인증된 사용자가 자기 프로필 화면을 조회할 때는 편집 가능해야하고, 다른 사람의 프로필을 조회할 때는 공개된 정보만 볼 수 있어야 합니다.

## Implementation

먼저 `AccountController`를 수정해 프로필 페이지로 진입할 수 있게 해줍니다.

`src/main/java/io/lcalmsky/app/account/endpoint/controller/AccountController.java`

```java
package io.lcalmsky.app.account.endpoint.controller;

import io.lcalmsky.app.account.application.AccountService;
import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.endpoint.controller.validator.SignUpFormValidator;
import io.lcalmsky.app.account.infra.repository.AccountRepository;
import io.lcalmsky.app.account.support.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Controller
@RequiredArgsConstructor
public class AccountController {
    // 생략
    @GetMapping("/profile/{nickname}")
    public String viewProfile(@PathVariable String nickname, Model model, @CurrentUser Account account) {
        Account byNickname = accountRepository.findByNickname(nickname);
        if (byNickname == null) { // (1)
            throw new IllegalArgumentException(nickname + "에 해당하는 사용자가 없습니다.");
        }
        model.addAttribute(byNickname); // (2)
        model.addAttribute("isOwner", byNickname.equals(account)); // (3)
        return "account/profile";
    }
}
```

1. nickname에 해당하는 사용자가 없으면 예외를 던집니다.
2. 키를 생략하면 객체 타입을 camel-case로 전달합니다.
3. 전달된 객체와 DB에서 조회한 객체가 같으면 인증된 사용자입니다.

<details>
<summary>AccountController.java 전체 보기</summary>

```java
package io.lcalmsky.app.account.endpoint.controller;

import io.lcalmsky.app.account.application.AccountService;
import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.endpoint.controller.validator.SignUpFormValidator;
import io.lcalmsky.app.account.infra.repository.AccountRepository;
import io.lcalmsky.app.account.support.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Controller
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final SignUpFormValidator signUpFormValidator;
    private final AccountRepository accountRepository;

    @InitBinder("signUpForm")
    public void initBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(signUpFormValidator);
    }

    @GetMapping("/sign-up")
    public String signUpForm(Model model) {
        model.addAttribute(new SignUpForm());
        return "account/sign-up";
    }

    @PostMapping("/sign-up")
    public String signUpSubmit(@Valid @ModelAttribute SignUpForm signUpForm, Errors errors) {
        if (errors.hasErrors()) {
            return "account/sign-up";
        }
        Account account = accountService.signUp(signUpForm);
        accountService.login(account);
        return "redirect:/";
    }

    @GetMapping("/check-email-token")
    public String verifyEmail(String token, String email, Model model) {
        Account account = accountService.findAccountByEmail(email);
        if (account == null) {
            model.addAttribute("error", "wrong.email");
            return "account/email-verification";
        }
        if (!token.equals(account.getEmailToken())) {
            model.addAttribute("error", "wrong.token");
            return "account/email-verification";
        }
        account.verified();
        accountService.login(account);
        model.addAttribute("numberOfUsers", accountRepository.count());
        model.addAttribute("nickname", account.getNickname());
        return "account/email-verification";
    }

    @GetMapping("/check-email")
    public String checkMail(@CurrentUser Account account, Model model) {
        model.addAttribute("email", account.getEmail());
        return "account/check-email";
    }

    @GetMapping("/resend-email")
    public String resendEmail(@CurrentUser Account account, Model model) {
        if (!account.enableToSendEmail()) {
            model.addAttribute("error", "인증 이메일은 5분에 한 번만 전송할 수 있습니다.");
            model.addAttribute("email", account.getEmail());
            return "account/check-email";
        }
        accountService.sendVerificationEmail(account);
        return "redirect:/";
    }

    @GetMapping("/profile/{nickname}")
    public String viewProfile(@PathVariable String nickname, Model model, @CurrentUser Account account) {
        Account byNickname = accountRepository.findByNickname(nickname);
        if (byNickname == null) { // nickname에 해당하는 사용자가 없으면 예외를 던집니다.
            throw new IllegalArgumentException(nickname + "에 해당하는 사용자가 없습니다.");
        }
        model.addAttribute(byNickname); // 키를 생략하면 객체 타입을 camel-case로 전달합니다.
        model.addAttribute("isOwner", byNickname.equals(account)); // 전달된 객체와 DB에서 조회한 객체가 같으면 인증된 사용자입니다.
        return "account/profile";
    }
}
```

</details>

다음으로 `profile.html` 파일을 생성하고 내용을 작성합니다.

`src/main/resources/templates/account/profile.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<body class="bg-light">
<div th:replace="fragments.html :: navigation-bar"></div>
<div class="container">
    <div class="row mt-5 justify-content-center">
        <div class="col-2">
            <!-- avatar -->
            <svg th:if="${#strings.isEmpty(account.profile.image)}" class="img-fluid float-left rounded img-thumbnail"
                 th:data-jdenticon-value="${account.nickname}" width="125" height="125"></svg>
            <svg th:if="${!#strings.isEmpty(account.profile.image)}" class="img-fluid float-left rounded img-thumbnail"
                 th:src="${account.profile.image}" width="125" height="125"></svg>
        </div>
        <div class="col-8">
            <!-- nickname-->
            <h1 class="display-4" th:text="${account.nickname}">nickname</h1>
            <!-- bio -->
            <p class="lead" th:if="${!#strings.isEmpty(account.profile.bio)}" th:text="${account.profile.bio}">bio</p>
            <p class="lead" th:if="${#strings.isEmpty(account.profile.bio) && isOwner}" >한 줄 소개를 추가해주세요.</p>
        </div>
        <div class="row mt-3 justify-content-center">
            <div class="col-2">
                <div class="nav flex-column nav-pills" id="v-pills-tab" role="tablist" aria-orientation="vertical">
                    <a class="nav-link active" id="v-pills-intro-tab" data-bs-toggle="pill" href="#v-pills-profile"
                       role="tab" aria-controls="v-pills-profile" aria-selected="true">소개</a>
                    <a class="nav-link" id="v-pills-study-tab" data-bs-toggle="pill" href="#v-pills-study" role="tab"
                       aria-controls="v-pills-study" aria-selected="false">스터디</a>
                </div>
            </div>
            <div class="col-8">
                <div class="tab-content" id="v-pills-tabContent">
                    <div class="tab-pane fade show active" id="v-pills-profile" role="tabpanel"
                         aria-labelledby="v-pills-home-tab">Profile
                    </div>
                    <div class="tab-pane fade" id="v-pills-study" role="tabpanel" aria-labelledby="v-pills-profile-tab">
                        Study
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
```

여기까지 작성한 뒤 애플리케이션을 실행하여 회원가입 후 프로필 메뉴를 눌러보면 "한 줄 소개를 추가해주세요" 라는 문구가 노출되지 않는 것을 확인할 수 있습니다.

그 이유는 `AccountController`에서 `isOwner`로 넘겨준 값이 `false`이기 때문인데요, 컨트롤러로 전달된 객체와 DB에서 찾은 객체가 `equals`하지 않기 때문입니다.

따라서 `Account` 클래스에 `equals`와 `hashcode` 메서드를 `override` 해주어야 합니다.

`src/main/java/io/lcalmsky/app/account/domain/entity/Account.java`

```java
// 생략
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder @Getter @ToString
public class Account extends AuditingEntity {
    // 생략
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

`id`가 동일하면 같은 객체로 판별하도록 구현하였습니다.

<details>
<summary>Account.java 전체 보기</summary>

```java
package io.lcalmsky.app.account.domain.entity;

import io.lcalmsky.app.domain.entity.AuditingEntity;
import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
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
    private Profile profile;

    @Embedded
    private NotificationSetting notificationSetting;

    private LocalDateTime emailTokenGeneratedAt;

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
        private boolean studyCreatedByEmail;
        private boolean studyCreatedByWeb;
        private boolean studyRegistrationResultByEmail;
        private boolean studyRegistrationResultByWeb;
        private boolean studyUpdatedByEmail;
        private boolean studyUpdatedByWeb;
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

이제 다시 애플리케이션을 실행해서 확인해보면 아래와 같이 제대로 노출되는 것을 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/16-01.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/16-02.png)

정상적으로 구현된 것을 확인하였으니 이제 다시 `profile.html`로 돌아가서 나머지 항목을 채워볼까요?

`src/main/resources/templates/account/profile.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<body class="bg-light">
<div th:replace="fragments.html :: navigation-bar"></div>
<div class="container">
    <div class="row mt-5 justify-content-center">
        <div class="col-2">
            <!-- avatar -->
            <svg th:if="${#strings.isEmpty(account.profile.image)}" class="img-fluid float-left rounded img-thumbnail"
                 th:data-jdenticon-value="${account.nickname}" width="125" height="125"></svg>
            <svg th:if="${!#strings.isEmpty(account.profile.image)}" class="img-fluid float-left rounded img-thumbnail"
                 th:src="${account.profile.image}" width="125" height="125"></svg>
        </div>
        <div class="col-8">
            <!-- nickname-->
            <h1 class="display-4" th:text="${account.nickname}">nickname</h1>
            <!-- bio -->
            <p class="lead" th:if="${!#strings.isEmpty(account.profile.bio)}" th:text="${account.profile.bio}">bio</p>
            <p class="lead" th:if="${#strings.isEmpty(account.profile.bio) && isOwner}">한 줄 소개를 추가해주세요.</p>
        </div>
        <div class="row mt-3 justify-content-center">
            <div class="col-2">
                <div class="nav flex-column nav-pills" id="v-pills-tab" role="tablist" aria-orientation="vertical">
                    <a class="nav-link active" id="v-pills-intro-tab" data-bs-toggle="pill" href="#v-pills-profile"
                       role="tab" aria-controls="v-pills-profile" aria-selected="true">소개</a>
                    <a class="nav-link" id="v-pills-study-tab" data-bs-toggle="pill" href="#v-pills-study" role="tab"
                       aria-controls="v-pills-study" aria-selected="false">스터디</a>
                </div>
            </div>
            <div class="col-8">
                <div class="tab-content" id="v-pills-tabContent">
                    <div class="tab-pane fade show active" id="v-pills-profile" role="tabpanel"
                         aria-labelledby="v-pills-home-tab">
                        <p th:if="${!#lists.isEmpty(account.profile.url)}">
                            <span style="...">
                                <i class="fa fa-link col-1"></i>
                            </span>
                            <span th:text="${account.profile.url}" class="col-11"></span>
                        </p>
                        <p th:if="${!#strings.isEmpty(account.profile.job)}">
                            <span style="...">
                                <i class="fa fa-briefcase col-1"></i>
                            </span>
                            <span th:text="${account.profile.job}" class="col-9"></span>
                        </p>
                        <p th:if="${!#strings.isEmpty(account.profile.location)}">
                            <span style="...">
                                <i class="fa fa-location-arrow col-1"></i>
                            </span>
                            <span th:text="${account.profile.location}" class="col-9"></span>
                        </p>
                        <p th:if="${isOwner}">
                            <span style="font-size: 20px">
                                <i class="fa fa-envelope-o col-1"></i>
                            </span>
                            <span th:text="${account.email}" class="col-9"></span>
                        </p>
                        <p th:if="${isOwner || account.valid}">
                            <span style="...">
                                <i class="fa fa-calender-o col-1"></i>
                            </span>
                            <span th:if="${isOwner && !account.valid}" class="col-9">
                                <a href="#"
                                   th:href="@{'/check-email?email=' + ${account.email}}">가입을 완료하려면 이메일을 확인하세요.</a>
                            </span>
                            <span th:text="${#temporals.format(account.joinedAt, 'yyyy년 M월 가입')}" class="col-9"></span>
                        </p>
                        <div th:if="${isOwner}">
                            <a class="btn btn-outline-primary" href="#" th:href="@{/settings/profile}">프로필 수정</a>
                        </div>
                    </div>

                    <div class="tab-pane fade" id="v-pills-study" role="tabpanel" aria-labelledby="v-pills-profile-tab">
                        Study
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
```

작성한 내용들에 대해 일일히 설명을 추가하진 않을 예정입니다. (워낙 단순하고 노가다 작업이고 저 또한 제대로 알지 못하기 때문에..)

계정 인증 유무, 프로필 데이터 유무에 따라 표시하는 내용이 바뀔 수 있게 작성되었습니다.

## Test

여기까지 작성이 되었다면 애플리케이션을 다시 실행한 뒤 확인해봅시다.

먼저 회원가입을 하고,

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/16-03.png)

프로필 아바타를 눌러 프로필 메뉴에 진입합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/16-04.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/16-05.png)

작성한대로 제대로 나타나는 것을 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/16-06.png)

이후 로그에 있는 인증메일 url을 찾아 접속하여 이메일 인증이 완료된 상태로 만든 뒤,

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/16-07.png)

다시 프로필 화면을 진입하면 가입한 날짜가 나타나야하는데요, 정상동작하지 않습니다.

---

다음 포스팅에서 왜 이런 버그가 발생하는지 확인해보겠습니다.