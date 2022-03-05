![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 9fa5095)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 9fa5095
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

알림 설정 기능을 구현합니다.

앞에서 프로필 수정을 구현한 것과 동일한 방식으로 구현하면 되기 때문에 설명보다는 코드 위주로 작성하겠습니다.

## 엔드포인트 수정

알림 설정 뷰로 라우팅 해줄 수 있는 엔드포인트와 설정 요청을 처리해주기 위한 엔드포인트를 추가합니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/settings/controller/SettingsController.java`

```java
package io.lcalmsky.app.settings.controller;

import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.support.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

@Controller
@RequiredArgsConstructor
public class SettingsController {

    // 생략
    static final String SETTINGS_NOTIFICATION_VIEW_NAME = "settings/notification";
    static final String SETTINGS_NOTIFICATION_URL = "/" + SETTINGS_NOTIFICATION_VIEW_NAME;

    // 생략
    @GetMapping(SETTINGS_NOTIFICATION_URL)
    public String notificationForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(NotificationForm.from(account));
        return SETTINGS_NOTIFICATION_VIEW_NAME;
    }

    @PostMapping(SETTINGS_NOTIFICATION_URL)
    public String updateNotification(@CurrentUser Account account, @Valid NotificationForm notificationForm, Errors errors, Model model, RedirectAttributes attributes) {
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS_NOTIFICATION_URL;
        }
        accountService.updateNotification(account, notificationForm);
        attributes.addFlashAttribute("message", "알림설정을 수정하였습니다.");
        return "redirect:" + SETTINGS_NOTIFICATION_URL;
    }
}
```

이전 포스팅에서 다뤘던 [프로필 수정](https://jaime-note.tistory.com/246)과 동일하게 필요한 부분을 추가해주었습니다.

이렇게만 작성하면 당연히 컴파일 에러가 발생하기 때문에 순차적으로 다음 단계를 구현해보겠습니다.

<details>
<summary>SettingsController.java 전체 보기</summary>

```java
package io.lcalmsky.app.settings.controller;

import io.lcalmsky.app.account.application.AccountService;
import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.support.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

@Controller
@RequiredArgsConstructor
public class SettingsController {

    static final String SETTINGS_PROFILE_VIEW_NAME = "settings/profile";
    static final String SETTINGS_PROFILE_URL = "/" + SETTINGS_PROFILE_VIEW_NAME;
    static final String SETTINGS_PASSWORD_VIEW_NAME = "settings/password";
    static final String SETTINGS_PASSWORD_URL = "/" + SETTINGS_PASSWORD_VIEW_NAME;
    static final String SETTINGS_NOTIFICATION_VIEW_NAME = "settings/notification";
    static final String SETTINGS_NOTIFICATION_URL = "/" + SETTINGS_NOTIFICATION_VIEW_NAME;

    private final AccountService accountService;

    @InitBinder("passwordForm")
    public void initBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(new PasswordFormValidator());
    }

    @GetMapping(SETTINGS_PROFILE_URL)
    public String profileUpdateForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(Profile.from(account));
        return SETTINGS_PROFILE_VIEW_NAME;
    }

    @PostMapping(SETTINGS_PROFILE_URL)
    public String updateProfile(@CurrentUser Account account, @Valid Profile profile, Errors errors, Model model, RedirectAttributes attributes) {
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS_PROFILE_VIEW_NAME;
        }
        accountService.updateProfile(account, profile);
        attributes.addFlashAttribute("message", "프로필을 수정하였습니다.");
        return "redirect:" + SETTINGS_PROFILE_URL;
    }

    @GetMapping(SETTINGS_PASSWORD_URL)
    public String passwordUpdateForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(new PasswordForm());
        return SETTINGS_PASSWORD_VIEW_NAME;
    }

    @PostMapping(SETTINGS_PASSWORD_URL)
    public String updatePassword(@CurrentUser Account account, @Valid PasswordForm passwordForm, Errors errors, Model model, RedirectAttributes attributes) {
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS_PASSWORD_VIEW_NAME;
        }
        accountService.updatePassword(account, passwordForm.getNewPassword());
        attributes.addFlashAttribute("message", "패스워드를 변경했습니다.");
        return "redirect:" + SETTINGS_PASSWORD_URL;
    }

    @GetMapping(SETTINGS_NOTIFICATION_URL)
    public String notificationForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(NotificationForm.from(account));
        return SETTINGS_NOTIFICATION_VIEW_NAME;
    }

    @PostMapping(SETTINGS_NOTIFICATION_URL)
    public String updateNotification(@CurrentUser Account account, @Valid NotificationForm notificationForm, Errors errors, Model model, RedirectAttributes attributes) {
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS_NOTIFICATION_URL;
        }
        accountService.updateNotification(account, notificationForm);
        attributes.addFlashAttribute("message", "알림설정을 수정하였습니다.");
        return "redirect:" + SETTINGS_NOTIFICATION_URL;
    }
}

```

</details>

## NotificationForm 구현

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/settings/controller/NotificationForm.java`

```java
package io.lcalmsky.app.settings.controller;

import io.lcalmsky.app.account.domain.entity.Account;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationForm {
    private boolean studyCreatedByEmail;
    private boolean studyCreatedByWeb;
    private boolean studyRegistrationResultByEmail;
    private boolean studyRegistrationResultByWeb;
    private boolean studyUpdatedByEmail;
    private boolean studyUpdatedByWeb;

    protected NotificationForm(Account account) {
        this.studyCreatedByEmail = account.getNotificationSetting().isStudyCreatedByEmail();
        this.studyCreatedByWeb = account.getNotificationSetting().isStudyCreatedByWeb();
        this.studyUpdatedByEmail = account.getNotificationSetting().isStudyUpdatedByEmail();
        this.studyUpdatedByWeb = account.getNotificationSetting().isStudyUpdatedByWeb();
        this.studyRegistrationResultByEmail = account.getNotificationSetting().isStudyRegistrationResultByEmail();
        this.studyRegistrationResultByWeb = account.getNotificationSetting().isStudyRegistrationResultByWeb();
    }

    public static NotificationForm from(Account account) {
        return new NotificationForm(account);
    }
}
```

계정정보에 존재하는 알림 설정 부분을 그대로 설정한 객체를 반환하도록 하였습니다.

## AccountService 수정

AccountService에서 알림 설정을 업데이트 하도록 해줍니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/account/application/AccountService.java`

```java
// 생략
public class AccountService implements UserDetailsService {
    // 생략
    public void updateNotification(Account account, NotificationForm notificationForm) {
        account.updateNotification(notificationForm);
        accountRepository.save(account);
    }
}
```

`account`가 직접 업데이트 하도록 위임하고 `repository`에 수정된 정보를 업데이트 합니다.

<details>
<summary>AccountService.java 전체 보기</summary>

```java
package io.lcalmsky.app.account.application;

import io.lcalmsky.app.account.domain.UserAccount;
import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.endpoint.controller.SignUpForm;
import io.lcalmsky.app.account.infra.repository.AccountRepository;
import io.lcalmsky.app.settings.controller.NotificationForm;
import io.lcalmsky.app.settings.controller.Profile;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService implements UserDetailsService {

    private final AccountRepository accountRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    public Account signUp(SignUpForm signUpForm) {
        Account newAccount = saveNewAccount(signUpForm);
        newAccount.generateToken();
        sendVerificationEmail(newAccount);
        return newAccount;
    }

    private Account saveNewAccount(SignUpForm signUpForm) {
        Account account = Account.builder()
                .email(signUpForm.getEmail())
                .nickname(signUpForm.getNickname())
                .password(passwordEncoder.encode(signUpForm.getPassword()))
                .notificationSetting(Account.NotificationSetting.builder()
                        .studyCreatedByWeb(true)
                        .studyUpdatedByWeb(true)
                        .studyRegistrationResultByWeb(true)
                        .build())
                .build();
        return accountRepository.save(account);
    }

    public void sendVerificationEmail(Account newAccount) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(newAccount.getEmail());
        mailMessage.setSubject("Webluxible 회원 가입 인증");
        mailMessage.setText(String.format("/check-email-token?token=%s&email=%s", newAccount.getEmailToken(),
                newAccount.getEmail()));
        mailSender.send(mailMessage);
    }

    public Account findAccountByEmail(String email) {
        return accountRepository.findByEmail(email);
    }

    public void login(Account account) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(new UserAccount(account),
                account.getPassword(), Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(token); // AuthenticationManager를 쓰는 방법이 정석적인 방ㅇ법
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = Optional.ofNullable(accountRepository.findByEmail(username))
                .orElse(accountRepository.findByNickname(username));
        if (account == null) {
            throw new UsernameNotFoundException(username);
        }
        return new UserAccount(account);
    }

    public void verify(Account account) {
        account.verified();
        login(account);
    }

    public void updateProfile(Account account, Profile profile) {
        account.updateProfile(profile);
        accountRepository.save(account);
    }

    public void updatePassword(Account account, String newPassword) {
        account.updatePassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
    }

    public void updateNotification(Account account, NotificationForm notificationForm) {
        account.updateNotification(notificationForm);
        accountRepository.save(account);
    }
}
```
</details>

## Account Entity 수정

`Account`에서 직접 전달받은 설정들을 업데이트 합니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/account/domain/entity/Account.java`

```java
// 생략
public class Account extends AuditingEntity {
    // 생략
    public void updateNotification(NotificationForm notificationForm) {
        this.notificationSetting.studyCreatedByEmail = notificationForm.isStudyCreatedByEmail();
        this.notificationSetting.studyCreatedByWeb = notificationForm.isStudyCreatedByWeb();
        this.notificationSetting.studyUpdatedByWeb = notificationForm.isStudyUpdatedByWeb();
        this.notificationSetting.studyUpdatedByEmail = notificationForm.isStudyUpdatedByEmail();
        this.notificationSetting.studyRegistrationResultByEmail = notificationForm.isStudyRegistrationResultByEmail();
        this.notificationSetting.studyRegistrationResultByWeb = notificationForm.isStudyRegistrationResultByWeb();
    }
    // 생략
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
    // 생략
}
```

NotificationSetting 클래스에서 기본 설정(웹 알림 on, 이메일 알림 off)을 할 수 있도록 초기값을 설정하였습니다.

<details>
<summary>Account.java 전체 보기</summary>

```java
package io.lcalmsky.app.account.domain.entity;

import io.lcalmsky.app.domain.entity.AuditingEntity;
import io.lcalmsky.app.settings.controller.NotificationForm;
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
    private Profile profile = new Profile();

    @Embedded
    private NotificationSetting notificationSetting = new NotificationSetting();

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

    @PostLoad
    private void init() {
        if (profile == null) {
            profile = new Profile();
        }
        if (notificationSetting == null) {
            notificationSetting = new NotificationSetting();
        }
    }

    public void updateProfile(io.lcalmsky.app.settings.controller.Profile profile) {
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

## View 작성

알림 설정 뷰를 구현합니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/resources/templates/settings/notification.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<body class="bg-light">
    <nav th:replace="fragments.html :: navigation-bar"></nav>
    <div class="container">
        <div class="row mt-5 justify-content-center">
            <div class="col-2">
                <div th:replace="fragments.html :: settings-menu(currentMenu='notification')"></div>
            </div>
            <div class="col-8">
                <div th:if="${message}" class="alert alert-info alert-dismissible fade show mt-3" role="alert">
                    <span th:text="${message}">완료</span>
                    <button type="button" class="btn-close" data-dismiss="alert" aria-label="Close"></button>
                </div>
                <div class="row">
                    <h2 class="col-12">알림 설정</h2>
                </div>
                <div class="row mt-3" th:fragment="profile-form">
                    <form class="col-12" action="#" th:action="@{/settings/notification}" th:object="${notificationForm}" method="post" novalidate>
                        <div class="alert alert-light" role="alert">
                            <strong><a href="#" th:href="@{/settings/locations}">주요 활동 지역</a>에
                            <a href="#" th:href="@{/settings/keywords}">관심있는 주제</a>의 스터디가 만들어졌을 때</strong> 알림을 받을 방법을 설정하세요.
                        </div>
                        <div class="form-group">
                            <div class="form-check form-switch form-check-inline">
                                <input type="checkbox" th:field="*{studyCreatedByEmail}" class="form-check-input" id="studyCreatedByEmail">
                                <label class="form-check-label" for="studyCreatedByEmail">이메일로 받기</label>
                            </div>
                            <div class="form-check form-switch form-check-inline">
                                <input type="checkbox" th:field="*{studyCreatedByWeb}" class="form-check-input" id="studyCreatedByWeb">
                                <label class="form-check-label" for="studyCreatedByWeb">웹으로 받기</label>
                            </div>
                        </div>
                        <div class="alert alert-light" role="alert">
                            <strong>스터디 모임 참가 신청</strong> 결과 알림을 받을 방법을 설정하세요.
                        </div>
                        <div class="form-group">
                            <div class="form-check form-switch form-check-inline">
                                <input type="checkbox" th:field="*{studyRegistrationResultByEmail}" class="form-check-input" id="studyRegistrationResultByEmil">
                                <label class="form-check-label" for="studyRegistrationResultByEmil">이메일로 받기</label>
                            </div>
                            <div class="form-check form-switch form-check-inline">
                                <input type="checkbox" th:field="*{studyRegistrationResultByWeb}" class="form-check-input" id="studyRegistrationResultByWeb">
                                <label class="form-check-label" for="studyRegistrationResultByWeb">웹으로 받기</label>
                            </div>
                        </div>
                        <div class="alert alert-light" role="alert">
                            <strong>참여중인 스터디</strong>에 대한 알림을 받을 방법을 설정하세요.
                        </div>
                        <div class="form-group">
                            <div class="form-check form-switch form-check-inline">
                                <input type="checkbox" th:field="*{studyUpdatedByEmail}" class="form-check-input" id="studyWatchByEmail">
                                <label class="form-check-label" for="studyWatchByEmail">이메일로 받기</label>
                            </div>
                            <div class="form-check form-switch form-check-inline">
                                <input type="checkbox" th:field="*{studyUpdatedByWeb}" class="form-check-input" id="studyWatchByWeb">
                                <label class="form-check-label" for="studyWatchByWeb">웹으로 받기</label>
                            </div>
                        </div>
                        <div class="form-group">
                            <button class="btn btn-outline-primary" type="submit" aria-describedby="submitHelp">저장하기</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>

        <div th:replace="fragments.html :: footer"></div>
    </div>
</body>
</html>
```

## 테스트

애플리케이션을 실행한 뒤 회원 가입 후 [프로필] - [프로필 수정 ]- [알림 설정] 메뉴에 순차적으로 진입합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/23-01.md)

알림 설정을 수정한 뒤 저장하기 버튼을 눌렀을 때 아래 화면처럼 동작하면 성공입니다!

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/23-02.md)

## 테스트 코드 작성

기존 테스트처럼 테스트를 추가해줍니다.

전문 테스트는 따로 추가하지 않았습니다.

`/Users/jaime/git-repo/spring-boot-app/src/test/java/io/lcalmsky/app/settings/controller/SettingsControllerTest.java`

```java
// 생략
class SettingsControllerTest {
    // 생략
    @Test
    @DisplayName("알림 설정 수정 폼")
    @WithAccount("jaime")
    void updateNotificationForm() throws Exception {
        mockMvc.perform(get(SettingsController.SETTINGS_NOTIFICATION_URL))
                .andExpect(status().isOk())
                .andExpect(view().name(SettingsController.SETTINGS_NOTIFICATION_VIEW_NAME))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("notificationForm"));
    }

    @Test
    @DisplayName("알림 설정 수정: 입력값 정상")
    @WithAccount("jaime")
    void updateNotification() throws Exception {
        mockMvc.perform(post(SettingsController.SETTINGS_NOTIFICATION_URL)
                        .param("studyCreatedByEmail", "true")
                        .param("studyCreatedByWeb", "true")
                        .param("studyRegistrationResultByEmail", "true")
                        .param("studyRegistrationResultByWeb", "true")
                        .param("studyUpdatedByEmail", "true")
                        .param("studyUpdatedByWeb", "true")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(SettingsController.SETTINGS_NOTIFICATION_URL))
                .andExpect(flash().attributeExists("message"));
        Account account = accountRepository.findByNickname("jaime");
        assertTrue(account.getNotificationSetting().isStudyCreatedByEmail());
        assertTrue(account.getNotificationSetting().isStudyCreatedByWeb());
        assertTrue(account.getNotificationSetting().isStudyRegistrationResultByEmail());
        assertTrue(account.getNotificationSetting().isStudyRegistrationResultByWeb());
        assertTrue(account.getNotificationSetting().isStudyUpdatedByEmail());
        assertTrue(account.getNotificationSetting().isStudyUpdatedByWeb());
    }
}
```

<details>
<summary>SettingsControllerTest.java 전체 보기</summary>

```java
package io.lcalmsky.app.settings.controller;

import io.lcalmsky.app.WithAccount;
import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.infra.repository.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SettingsControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired AccountRepository accountRepository;

    @AfterEach
    void afterEach() {
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("프로필 수정: 입력값 정상")
    @WithAccount("jaime")
    void updateProfile() throws Exception {
        String bio = "한 줄 소개";
        mockMvc.perform(post(SettingsController.SETTINGS_PROFILE_URL)
                        .param("bio", bio)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(SettingsController.SETTINGS_PROFILE_URL))
                .andExpect(flash().attributeExists("message"));
        Account jaime = accountRepository.findByNickname("jaime");
        assertEquals(bio, jaime.getProfile().getBio());
    }


    @Test
    @DisplayName("프로필 수정: 입력값 에러")
    @WithAccount("jaime")
    void updateProfileWithError() throws Exception {
        String bio = "35자 넘으면 에러35자 넘으면 에러35자 넘으면 에러35자 넘으면 에러";
        mockMvc.perform(post(SettingsController.SETTINGS_PROFILE_URL)
                        .param("bio", bio)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name(SettingsController.SETTINGS_PROFILE_VIEW_NAME))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("profile"));
        Account jaime = accountRepository.findByNickname("jaime");
        assertNull(jaime.getProfile().getBio());
    }

    @Test
    @DisplayName("프로필 조회")
    @WithAccount("jaime")
    void updateProfileForm() throws Exception {
        String bio = "한 줄 소개";
        mockMvc.perform(get(SettingsController.SETTINGS_PROFILE_URL))
                .andExpect(status().isOk())
                .andExpect(view().name(SettingsController.SETTINGS_PROFILE_VIEW_NAME))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("profile"));
    }

    @Autowired PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("패스워드 수정 폼")
    @WithAccount("jaime")
    void updatePasswordForm() throws Exception {
        mockMvc.perform(get(SettingsController.SETTINGS_PASSWORD_URL))
                .andExpect(status().isOk())
                .andExpect(view().name(SettingsController.SETTINGS_PASSWORD_VIEW_NAME))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("passwordForm"));
    }

    @Test
    @DisplayName("패스워드 수정: 입력값 정상")
    @WithAccount("jaime")
    void updatePassword() throws Exception {
        mockMvc.perform(post(SettingsController.SETTINGS_PASSWORD_URL)
                        .param("newPassword", "12341234")
                        .param("newPasswordConfirm", "12341234")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(SettingsController.SETTINGS_PASSWORD_URL))
                .andExpect(flash().attributeExists("message"));
        Account account = accountRepository.findByNickname("jaime");
        assertTrue(passwordEncoder.matches("12341234", account.getPassword()));
    }

    @Test
    @DisplayName("패스워드 수정: 입력값 에러(불일치)")
    @WithAccount("jaime")
    void updatePasswordWithNotMatchedError() throws Exception {
        mockMvc.perform(post(SettingsController.SETTINGS_PASSWORD_URL)
                        .param("newPassword", "12341234")
                        .param("newPasswordConfirm", "12121212")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name(SettingsController.SETTINGS_PASSWORD_VIEW_NAME))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeExists("passwordForm"))
                .andExpect(model().attributeExists("account"));
    }

    @Test
    @DisplayName("패스워드 수정: 입력값 에러(길이)")
    @WithAccount("jaime")
    void updatePasswordWithLengthError() throws Exception {
        mockMvc.perform(post(SettingsController.SETTINGS_PASSWORD_URL)
                        .param("newPassword", "1234")
                        .param("newPasswordConfirm", "1234")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name(SettingsController.SETTINGS_PASSWORD_VIEW_NAME))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeExists("passwordForm"))
                .andExpect(model().attributeExists("account"));
    }

    @Test
    @DisplayName("알림 설정 수정 폼")
    @WithAccount("jaime")
    void updateNotificationForm() throws Exception {
        mockMvc.perform(get(SettingsController.SETTINGS_NOTIFICATION_URL))
                .andExpect(status().isOk())
                .andExpect(view().name(SettingsController.SETTINGS_NOTIFICATION_VIEW_NAME))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("notificationForm"));
    }

    @Test
    @DisplayName("알림 설정 수정: 입력값 정상")
    @WithAccount("jaime")
    void updateNotification() throws Exception {
        mockMvc.perform(post(SettingsController.SETTINGS_NOTIFICATION_URL)
                        .param("studyCreatedByEmail", "true")
                        .param("studyCreatedByWeb", "true")
                        .param("studyRegistrationResultByEmail", "true")
                        .param("studyRegistrationResultByWeb", "true")
                        .param("studyUpdatedByEmail", "true")
                        .param("studyUpdatedByWeb", "true")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(SettingsController.SETTINGS_NOTIFICATION_URL))
                .andExpect(flash().attributeExists("message"));
        Account account = accountRepository.findByNickname("jaime");
        assertTrue(account.getNotificationSetting().isStudyCreatedByEmail());
        assertTrue(account.getNotificationSetting().isStudyCreatedByWeb());
        assertTrue(account.getNotificationSetting().isStudyRegistrationResultByEmail());
        assertTrue(account.getNotificationSetting().isStudyRegistrationResultByWeb());
        assertTrue(account.getNotificationSetting().isStudyUpdatedByEmail());
        assertTrue(account.getNotificationSetting().isStudyUpdatedByWeb());
    }
}
```

</details>

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/23-03.md)

모두 성공하였습니다!