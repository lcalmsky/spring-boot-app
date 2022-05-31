![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 83d2d6d)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 83d2d6d
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

패스워드를 잊은 경우 로그인 할 수 있는 링크를 이메일로 전송합니다.

이메일로 전송된 링크를 클릭하면 로그인됩니다.

구현할 API는 총 세 개로 아래와 같습니다.

* `GET /email-login`: 이메일 입력 폼 제공
* `POST /email-login`: 이메일에 해당하는 계정 찾기, 계정이 존재하는 경우 로그인 가능한 링크를 이메일로 전송
* `GET /login-by-email`: 토큰과 이메일을 확인한 뒤 해당 계정으로 로그인

## 엔드포인트 수정

이메일을 통한 로그인을 제공해야 하기 때문에 `AccountController` 클래스를 수정해줍니다.

`/src/main/java/io/lcalmsky/app/account/endpoint/controller/AccountController.java`

```java
// 생략
public class AccountController {
    // 생략
    @GetMapping("/email-login")
    public String emailLoginForm() { // (1)
        return "account/email-login";
    }

    @PostMapping("/email-login")
    public String sendLinkForEmailLogin(String email, Model model, RedirectAttributes attributes) { // (2)
        Account account = accountRepository.findByEmail(email);
        if (account == null) {
            model.addAttribute("error", "유효한 이메일 주소가 아닙니다.");
            return "account/email-login";
        }
        if (!account.enableToSendEmail()) {
            model.addAttribute("error", "너무 잦은 요청입니다. 5분 뒤에 다시 시도하세요.");
            return "account/email-login";
        }
        accountService.sendLoginLink(account);
        attributes.addFlashAttribute("message", "로그인 가능한 링크를 이메일로 전송하였습니다.");
        return "redirect:/email-login";
    }

    @GetMapping("/login-by-email")
    public String loginByEmail(String token, String email, Model model) { // (3)
        Account account = accountRepository.findByEmail(email);
        if (account == null || !account.isValid(token)) {
            model.addAttribute("error", "로그인할 수 없습니다.");
            return "account/logged-in-by-email";
        }
        accountService.login(account);
        return "account/logged-in-by-email";
    }
}
```

1. 이메일 로그인 뷰 페이지로 라우팅 합니다.
2. 이메일 폼을 통해 입력받은 정보로 계정을 찾아 메일을 전송하고 다시 리다이렉트합니다. 계정이 존재하지 않을 경우 에러를 전달합니다.
3. 링크를 통해 전달한 토큰과 이메일정보를 가지고 토큰의 유효성을 판단하고 유효한 경우 로그인을 수행해 인증정보를 업데이트 하고 페이지를 이동합니다. 토큰이나 이메일이 유효하지 않을 경우 에러를 전달합니다.

<details>
<summary>AccountController.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.account.endpoint.controller;

import io.lcalmsky.app.modules.account.application.AccountService;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.endpoint.controller.validator.SignUpFormValidator;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
import io.lcalmsky.app.modules.account.support.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        accountService.verify(account);
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
        if (byNickname == null) {
            throw new IllegalArgumentException(nickname + "에 해당하는 사용자가 없습니다.");
        }
        model.addAttribute(byNickname);
        model.addAttribute("isOwner", byNickname.equals(account));
        return "account/profile";
    }

    @GetMapping("/email-login")
    public String emailLoginForm() {
        return "account/email-login";
    }

    @PostMapping("/email-login")
    public String sendLinkForEmailLogin(String email, Model model, RedirectAttributes attributes) {
        Account account = accountRepository.findByEmail(email);
        if (account == null) {
            model.addAttribute("error", "유효한 이메일 주소가 아닙니다.");
            return "account/email-login";
        }
        if (!account.enableToSendEmail()) {
            model.addAttribute("error", "너무 잦은 요청입니다. 5분 뒤에 다시 시도하세요.");
            return "account/email-login";
        }
        accountService.sendLoginLink(account);
        attributes.addFlashAttribute("message", "로그인 가능한 링크를 이메일로 전송하였습니다.");
        return "redirect:/email-login";
    }

    @GetMapping("/login-by-email")
    public String loginByEmail(String token, String email, Model model) {
        Account account = accountRepository.findByEmail(email);
        if (account == null || !account.isValid(token)) {
            model.addAttribute("error", "로그인할 수 없습니다.");
            return "account/logged-in-by-email";
        }
        accountService.login(account);
        return "account/logged-in-by-email";
    }
}
```

</details>

## AccountService 수정

컨트롤러에서 `AccountService`에 위임한 기능들을 구현합니다.

`/src/main/java/io/lcalmsky/app/account/application/AccountService.java`

```java
// 생략
public class AccountService implements UserDetailsService {
    // 생략
    public void sendLoginLink(Account account) {
        account.generateToken();
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(account.getEmail());
        mailMessage.setSubject("[Webluxible] 로그인 링크");
        mailMessage.setText("/login-by-email?token=" + account.getEmailToken() + "&email=" + account.getEmail());
        mailSender.send(mailMessage);
    }
}
```

가입시 메일을 전달하는 기능과 유사한데 링크에 포함되는 요청 경로가 달라집니다.

<details>
<summary>AccountService.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.account.application;

import io.lcalmsky.app.modules.account.domain.UserAccount;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.endpoint.controller.form.SignUpForm;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
import io.lcalmsky.app.modules.account.endpoint.controller.form.NotificationForm;
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

    public void updateNotification(Account account, NotificationForm notificationForm) {
        account.updateNotification(notificationForm);
        accountRepository.save(account);
    }

    public void updateNickname(Account account, String nickname) {
        account.updateNickname(nickname);
        accountRepository.save(account);
        login(account);
    }

    public void sendLoginLink(Account account) {
        account.generateToken();
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(account.getEmail());
        mailMessage.setSubject("[Webluxible] 로그인 링크");
        mailMessage.setText("/login-by-email?token=" + account.getEmailToken() + "&email=" + account.getEmail());
        mailSender.send(mailMessage);
    }
}
```

</details>

## Account Entity 수정

토큰이 유효한지 확인하는 기능을 추가합니다.

`/src/main/java/io/lcalmsky/app/account/domain/entity/Account.java`

```java
// 생략
public class Account extends AuditingEntity {
    // 생략
    public boolean isValid(String token) {
        return this.emailToken.equals(token);
    }
    // 생략
}
```

<details>
<summary>Account.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.account.domain.entity;

import io.lcalmsky.app.modules.account.domain.entity.AuditingEntity;
import io.lcalmsky.app.modules.account.endpoint.controller.form.NotificationForm;
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

## SecurityConfig 수정

메일로 전달한 링크에 포함되어있는 URL은 인증정보 없이 진입할 수 있어야하므로 Security 설정을 수정해줍니다.

`/src/main/java/io/lcalmsky/app/config/SecurityConfig.java`

```java
// 생략
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    // 생략
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .mvcMatchers("/", "/login", "/sign-up", "/check-email-token",
                        "/email-login", "/check-email-login", "/login-link", "/login-by-email").permitAll() // "/login-by-email"을 추가해줍니다.
                .mvcMatchers(HttpMethod.GET, "/profile/*").permitAll()
                .anyRequest().authenticated();
        http.formLogin()
                .loginPage("/login")
                .permitAll();
        http.logout()
                .logoutSuccessUrl("/");
        http.rememberMe()
                .userDetailsService(accountService)
                .tokenRepository(tokenRepository());
    }
    // 생략
}
```

## View 추가

`email-login.html`과 `logged-in-by-email.html` 두 개의 페이지를 account 하위에 생성합니다.

`/src/main/resources/templates/account/email-login.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<body class="bg-light">
<div th:replace="fragments.html :: navigation-bar"></div>
<svg th:replace="fragments.html :: svg-symbols"/>
<div class="container">
    <div class="py-5 text-center">
        <p class="lead">Webluxible</p>
        <h2>패스워드 없이 로그인하기</h2>
    </div>
    <div class="row justify-content-center">
        <div th:if="${error}" class="alert alert-danger alert-dismissible fade show mt-3" role="alert">
            <svg th:replace="fragments.html::symbol-danger"/>
            <span th:text="${error}">error</span>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
        <div th:if="${message}" class="alert alert-info alert-dismissible fade show mt-3" role="alert">
            <svg th:replace="fragments.html::symbol-success"/>
            <span th:text="${message}">완료</span>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
        <form class="needs-validation col-sm-6" action="#" th:action="@{/email-login}" method="post" novalidate>
            <div class="form-group mb-3">
                <label for="email">가입 할 때 사용한 이메일</label>
                <input id="email" type="email" name="email" class="form-control"
                       placeholder="your@email.com" aria-describedby="emailHelp" required>
                <small id="emailHelp" class="form-text text-muted">
                    가입할 때 사용한 이메일을 입력하세요.
                </small>
                <small class="invalid-feedback">이메일을 입력하세요.</small>
            </div>

            <div class="form-group">
                <button class="btn btn-success btn-block" type="submit" aria-describedby="submitHelp">
                    로그인 링크 보내기
                </button>
                <small id="submitHelp" class="form-text text-muted">
                    Webluxible에 처음 오신거라면 <a href="#" th:href="@{/sign-up}">계정을 먼저 만드세요.</a>
                </small>
            </div>
        </form>
    </div>

    <div th:replace="fragments.html :: footer"></div>
</div>
<script th:replace="fragments.html :: form-validation"></script>
</body>
</html>
```

`/src/main/resources/templates/account/logged-in-by-email.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<body class="bg-light">
<nav th:replace="fragments.html :: navigation-bar"></nav>
<div class="container">
    <div class="py-5 text-center" th:if="${error}">
        <p class="lead">Webluxible 이메일 로그인</p>
        <div class="alert alert-danger" role="alert" th:text="${error}">
            로그인 할 수 없습니다.
        </div>
    </div>
    <div class="py-5 text-center" th:if="${error == null}">
        <p class="lead">Webluxible 이메일 로그인</p>
        <h2>이메일로 로그인 했습니다. <a th:href="@{/settings/password}">패스워드를 변경</a>하세요.</h2>
    </div>
</div>
</body>
</html>
```

이전 포스팅에서 다뤘던 내용이 대부분이라 소스 코드만 첨부하였습니다.

## 테스트

애플리케이션 실행 후 [가입] - [로그아웃] - [로그인] 순으로 진행합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/25-01.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/25-02.png)

이메일로 로그인하기를 클릭하여 해당 페이지로 진입 합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/25-03.png)

가입시 입력했던 이메일을 잘못 입력했을 때 피드백을 확인합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/25-04.png)

다시 제대로 입력하더라도 5분이 지나지 않았기 때문에 에러 피드백을 받게 됩니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/25-05.png)

테스트 할 때는 5분을 기다리는 게 너무 낭비일 수 있으므로 AccountController.sendLinkForEmailLogin 메서드에서 메일 전송시간을 체크하는 부분에 잠시 주석 처리를 해줍니다.

```java
// 생략
public class AccountController {
    // 생략
    @PostMapping("/email-login")
    public String sendLinkForEmailLogin(String email, Model model, RedirectAttributes attributes) {
        Account account = accountRepository.findByEmail(email);
        if (account == null) {
            model.addAttribute("error", "유효한 이메일 주소가 아닙니다.");
            return "account/email-login";
        }
//        if (!account.enableToSendEmail()) {
//            model.addAttribute("error", "너무 잦은 요청입니다. 5분 뒤에 다시 시도하세요.");
//            return "account/email-login";
//        }
        accountService.sendLoginLink(account);
        attributes.addFlashAttribute("message", "로그인 가능한 링크를 이메일로 전송하였습니다.");
        return "redirect:/email-login";
    }
    // 생략
}
```

이렇게 주석처리를 했을 경우 앱을 다시 시작해야 하므로 귀찮으신 분들은 5분을 기다리셔도 됩니다 😬

유효한 이메일을 넣고 버튼을 클릭하면 정상 피드백을 받을 수 있고, 로그에서 토큰이 첨부된 요청을 찾을 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/25-06.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/25-07.png)

해당 링크를 복사하여 브라우저 주소창에 붙였을 때 아래와 같은 화면이 노출되면 성공입니다!

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/25-08.png)

토큰이나 이메일을 변조할 경우 실패 피드백을 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/25-09.png)


