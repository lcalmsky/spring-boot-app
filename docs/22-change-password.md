![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: f1e9d3d)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout f1e9d3d
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

비밀번호 변경 기능을 구현합니다.

* 패스워드 탭 활성화 및 구현
* 패스워드, 패스워드 확인 탭 일치 여부
* 패스워드 인코딩
* validation

## 엔드포인트 수정

컨트롤러에서 패스워드 뷰로 라우팅 할 수 있게, 비밀번호 변경 요청을 받아 실제로 수행할 수 있게 기능을 추가합니다.

`/src/main/java/io/lcalmsky/app/settings/controller/SettingsController.java`

```java
// 생략
public class SettingsController {
    // 생략
    static final String SETTINGS_PASSWORD_VIEW_NAME = "settings/password"; // (1)
    static final String SETTINGS_PASSWORD_URL = "/" + SETTINGS_PASSWORD_VIEW_NAME; // (1)

    private final AccountService accountService;

    @InitBinder("passwordForm")
    public void initBinder(WebDataBinder webDataBinder) { // (2) 
        webDataBinder.addValidators(new PasswordFormValidator());
    }
    
    // 생략

    @GetMapping(SETTINGS_PASSWORD_URL) // (3)
    public String passUpdateForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(new PasswordForm());
        return SETTINGS_PASSWORD_VIEW_NAME;
    }

    @PostMapping(SETTINGS_PASSWORD_URL) // (4) 
    public String updatePassword(@CurrentUser Account account, @Valid PasswordForm passwordForm, Errors errors, Model model, RedirectAttributes attributes) {
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS_PASSWORD_VIEW_NAME;
        }
        accountService.updatePassword(account, passwordForm.getNewPassword()); // (5)
        attributes.addFlashAttribute("message", "패스워드를 변경했습니다.");
        return "redirect:" + SETTINGS_PASSWORD_URL;
    }
}
```

1. password url과 view를 상수로 지정합니다.
2. 패스워드 폼을 검증하기위한 validator를 추가합니다. 아주 예전에 [회원 가입 폼을 검증할 때 사용했던 방법](https://jaime-note.tistory.com/126?category=1008890)과 동일합니다.
3. 패스워드 수정 뷰로 라우팅해줍니다. 현재 계정 정보를 `Model`로 넘겨줍니다.
4. 패스워드 폼을 전달받아 해당 패스워드로 업데이트 합니다. 에러가 있을 경우 다시 페이지를 띄우고 그렇지 않을 경우 피드백 메시지와 함께 리다이랙트합니다.
5. 비밀번호 변경은 `Service`에게 위임합니다.

<details>
<summary>SettingsController.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.settings.controller;

import io.lcalmsky.app.modules.account.application.AccountService;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.support.CurrentUser;
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
    public String passUpdateForm(@CurrentUser Account account, Model model) {
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
}
```

</details>

위 같이 수정하고나면 구현되지 않은 부분에서 컴파일에러가 발생할텐데오 차근차근 하나씩 작성해봅시다.

## PasswordValidator 구현

먼저 `@InitBinder`를 이용해 주입해주었던 validator를 구현하겠습니다.

`/src/main/java/io/lcalmsky/app/settings/controller/PasswordFormValidator.java`

```java
package io.lcalmsky.app.modules.settings.controller;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class PasswordFormValidator implements Validator { // (1)
    @Override
    public boolean supports(Class<?> clazz) { // (2)
        return PasswordForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) { // (3) 
        PasswordForm passwordForm = (PasswordForm) target;
        if (!passwordForm.getNewPassword().equals(passwordForm.getNewPasswordConfirm())) {
            errors.rejectValue("newPassword", "wrong.value", "입력한 새 패스워드가 일치하지 않습니다.");
        }
    }
}
```

1. Validator를 구현합니다.
2. 어떤 타입에 대해 validate 할지 결정합니다.
3. 2번에서 PasswordForm 타입에 할당할 수 있는 타입만 받도록 하였기 때문에 target 객체는 PasswordForm으로 캐스팅 할 수 있습니다. 그 이후 새로운 비밀번호와 비밀번호 확인이 동일한지 체크하여 동일하지 않을 경우 에러 객체에 에러 문구를 전달합니다.

## PasswordForm 생성

변경할 비밀번호 전달받을 Form 클래스를 생성합니다.

`/src/main/java/io/lcalmsky/app/settings/controller/PasswordForm.java`

```java
package io.lcalmsky.app.modules.settings.controller;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordForm {
    @Length(min = 8, max = 50)
    private String newPassword;
    @Length(min = 8, max = 50)
    private String newPasswordConfirm;
}
```

새로운 비밀번호와, 비밀번호 확인 두 필드만 있으면 되고, 길이에 대한 validation을 추가하였습니다.

## AccountService 수정

비밀번호 변경을 서비스에 위임했기 때문에 해당 기능을 구현해야 합니다.

`/src/main/java/io/lcalmsky/app/account/application/AccountService.java`

```java
// 생략
public class AccountService implements UserDetailsService {
    // 생략
    public void updatePassword(Account account, String newPassword) {
        account.updatePassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
    }
}
```

account 객체가 비밀번호를 직접 업데이트 하도록 하였고, 새로운 비밀번호 역시 인코딩하여 전달하였습니다.

[이전 포스팅(문제점 및 해결 방법 참조)](https://jaime-note.tistory.com/246?category=1008890)과 마찬가지로 updatePassword 메서드로 전달된 Account는 영속성을 가진 객체가 아니므로 수정 후 `repository`에서 `save` 메서드를 호출해야합니다.

<details>
<summary>AccountService.java 전체보기</summary>

```java
package io.lcalmsky.app.modules.account.application;

import io.lcalmsky.app.modules.account.domain.UserAccount;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.endpoint.controller.form.SignUpForm;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
import io.lcalmsky.app.modules.account.endpoint.controller.form.Profile;
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
}
```

</details>

## Account Entity 수정

위에서 Account 객체에게 비밀번호 업데이트를 또 위임하였기 때문에 이 부분 역시 수정해주어야 합니다.

`/src/main/java/io/lcalmsky/app/account/domain/entity/Account.java`

```java
// 생략
public class Account extends AuditingEntity {
    // 생략
    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }
}
```

간단히 필드를 업데이트 해주었습니다.

<details>
<summary>Account.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.account.domain.entity;

import io.lcalmsky.app.modules.account.domain.entity.AuditingEntity;
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

## 패스워드 변경 뷰

마지막으로 패스워드 변경 뷰를 추가합니다.

`/src/main/resources/templates/settings/password.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<body class="bg-light">
<div th:replace="fragments.html :: navigation-bar"></div>
<div class="container">
    <div class="row mt-5 justify-content-center">
        <div class="col-2">
            <div th:replace="fragments.html::settings-menu (currentMenu='profile')"></div>
        </div>
        <div class="col-8">
            <div th:if="${message}" class="alert alert-info alert-dismissible fade show mt-3" role="alert">
                <span th:text="${message}">수정 완료</span>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
            <h2 class="col-sm-12">패스워드 변경</h2>
            <div class="row mt-3">
                <form class="needs-validation col-12" action="#" th:action="@{/settings/password}"
                      th:object="${passwordForm}" method="post"
                      novalidate>
                    <div class="form-group mt-3">
                        <label for="newPassword">새 패스워드</label>
                        <input id="newPassword" type="password" th:field="*{newPassword}" class="form-control"
                               aria-describedby="newPasswordHelp" required min="8" max="50">
                        <small id="newPasswordHelp" class="form-text text-muted">
                            새 패스워드를 입력하세요.
                        </small>
                        <small class="invalid-feedback">패스워드를 입력하세요.</small>
                        <small class="form-text text-danger" th:if="${#fields.hasErrors('newPassword')}"
                               th:errors="*{newPassword}">new password error</small>
                    </div>
                    <div class="form-group mt-3">
                        <label for="newPasswordConfirm">새 패스워드</label>
                        <input id="newPasswordConfirm" type="password" th:field="*{newPasswordConfirm}"
                               class="form-control"
                               aria-describedby="newPasswordConfirmHelp" required min="8" max="50">
                        <small id="newPasswordConfirmHelp" class="form-text text-muted">
                            새 패스워드를 다시 한번 입력하세요.
                        </small>
                        <small class="invalid-feedback">패스워드를 다시 입력하세요.</small>
                        <small class="form-text text-danger" th:if="${#fields.hasErrors('newPasswordConfirm')}"
                               th:errors="*{newPasswordConfirm}">new password confirm error</small>
                    </div>
                    <div class="form-group mt-3">
                        <button class="btn btn-outline-primary" type="submit" aria-describedby="submitHelp">패스워드 변경하기</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
<script th:replace="fragments.html::form-validation"></script>
</body>
</html>
```

기존 profile.html 파일을 복사하여 사용하였습니다.

> HTML과 bootstrap 등 FE 관련 기술들에 문외한인 저도 이제 슬슬 이런 코드들에 익숙해지고 있습니다!

## 테스트

여기까지 모든 소스 코드 작성을 완료했는데요, 애플리케이션을 실행하고 [가입] - [프로필] - [패스워드]까지 진입하여 뷰를 먼저 확인합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/22-01.png)

먼저 8~50자 범위를 벗어나게 입력하여 에러를 확인해보겠습니다.

각각 1234, 1234를 입력하고 패스워드 변경하기 버튼을 클릭했을 때 결과입니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/22-02.png)

다음은 위 아래 비밀번호를 다르게 입력했을 때 에러를 확인해보겠습니다.

각각 12345678, 11111111을 입력한 뒤 패스워드 변경하기 버튼을 클릭했을 때 결과입니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/22-03.png)

마지막으로 정상적으로 변경되었을 때 피드백 화면입니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/22-04.png)

## 테스트 코드 작성

`/src/test/java/io/lcalmsky/app/settings/controller/SettingsControllerTest.java`

```java
// 생략
class SettingsControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired AccountRepository accountRepository;

    @AfterEach
    void afterEach() {
        accountRepository.deleteAll();
    }

    // 생략
    
    @Autowired PasswordEncoder passwordEncoder; // (1)

    @Test
    @DisplayName("패스워드 수정 폼")
    @WithAccount("jaime") // (2)
    void updatePasswordForm() throws Exception { // (3)
        mockMvc.perform(get(SettingsController.SETTINGS_PASSWORD_URL))
                .andExpect(status().isOk())
                .andExpect(view().name(SettingsController.SETTINGS_PASSWORD_VIEW_NAME))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("passwordForm"));
    }

    @Test
    @DisplayName("패스워드 수정: 입력값 정상")
    @WithAccount("jaime")
    void updatePassword() throws Exception { // (4)  
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
    void updatePasswordWithNotMatchedError() throws Exception { // (5)
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
    void updatePasswordWithLengthError() throws Exception { // (6)
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
}
```

1. 비밀번호 검증을 위해 주입해줍니다.
2. [이전 포스팅의 SecurityContext 설정 항목](https://jaime-note.tistory.com/250?category=1008890)을 참조해주세요 :)
3. 패스워드 수정 뷰에 진입했을 때 정확하게 동작하는지 확인합니다.
4. 입력값이 정상일 때 정상적으로 리다이렉트되는지, flashAttribute로 메시지 피드백이 전달 되는지, 비밀번호 저장이 정확하게 동작했는지 확인합니다.
5. 비밀번호 불일치시 200OK 응답 후 다시 패스워드 뷰를 보여주면서 에러가 전달되는지 확인합니다.
6. 비밀번호 길이가 유효하지 않을 때 200OK 응답 후 다시 패스워드 뷰를 보여주면서 에러가 전달되는지 확인합니다.

<details>
<summary>SettingsControllerTest.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.settings.controller;

import io.lcalmsky.app.modules.account.WithAccount;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
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
}
```

</details>

