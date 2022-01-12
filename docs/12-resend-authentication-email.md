![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 64fc2aa)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 64fc2aa
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

가입 이후 이메일 인증을 유도할 수 있는 안내를 추가하고, 이메일 인증 메일을 재전송 할 수 있는 기능을 구현합니다. 

## Implementation

먼저 가입을 모두 마쳤을 때 홈 화면으로 돌아가게 되는데 그 때 이메일 인증을 안내하도록 수정해보겠습니다.

`index.html` 파일에서 내비게이션 바 바로 아래 경고 문구를 삽입해줍니다.

```html
<div class="alert alert-warning" role="alert" th:if="${account != null && !account.isValid()}">
    Webluxible 가입을 완료하려면 <a href="#" th:href="@{/check-email}" class="alert-link">계정 인증 이메일을 확인</a>하세요.
</div>
```

<details>
<summary>
index.html 전체 보기</summary>

```html
<!DOCTYPE html>
<html lang="en"
      xmlns:th="http://www.thymeleaf.org"
>
<head th:replace="fragments.html :: head"></head>
<body class="bg-light">
<div th:replace="fragments.html :: navigation-bar"></div>
<div class="alert alert-warning" role="alert" th:if="${account != null && !account.isValid()}">
    Webluxible 가입을 완료하려면 <a href="#" th:href="@{/check-email}" class="alert-link">계정 인증 이메일을 확인</a>하세요.
</div>

<div class="container">
    <div class="py-5 text-center">
        <h2>Webluxible</h2>
    </div>
    <div th:replace="fragments.html :: footer"></div>
</div>
<script type="application/javascript">
    (function () {

    }())
</script>
</body>
</html>
```

</details>

바로 위에서 **계정 인증 이메일 확인**에 링크가 걸려있는데 해당 부분으로 리다이렉트 되었을 때 화면을 구현하겠습니다.

`src/main/resources/templates/account/check-email.html` 파일을 생성하고 아래와 같이 작성합니다.

`check-email.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<body class="bg-light">
<nav th:replace="fragments.html :: navigation-bar"></nav>

<div class="container">
    <div class="py-5 text-center" th:if="${error != null}">
        <p class="lead">Webluxible 가입</p>
        <div  class="alert alert-danger" role="alert" th:text="${error}"></div>
        <p class="lead" th:text="${email}">your@email.com</p>
    </div>

    <div class="py-5 text-center" th:if="${error == null}">
        <p class="lead">Webluxible 가입</p>

        <h2>Webluxible 서비스를 사용하려면 인증 이메일을 확인하세요.</h2>

        <div>
            <p class="lead" th:text="${email}">your@email.com</p>
            <a class="btn btn-outline-info" th:href="@{/resend-email}">인증 이메일 다시 보내기</a>
        </div>
    </div>
</div>
</body>
</html>
```

에러가 존재할 때는 에러를 출력해주고, 에러가 없다면 이메일 인증을 확인하라는 안내 문구를 노출합니다.

check-email 페이지는 인증이 된 상태에서만 접근 가능해야하기 때문에 `SecurityConfig` 클래스도 수정해줍니다.

`SecurityConfig.java`

```java
package io.lcalmsky.app.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .mvcMatchers("/", "/login", "/sign-up", "/check-email-token", // "/check-email"을 제외하였습니다.
                        "/email-login", "/check-email-login", "/login-link").permitAll()
                .mvcMatchers(HttpMethod.GET, "/profile/*").permitAll()
                .anyRequest().authenticated();
    }
    // 생략
}
```

check-email.html로 리다이렉트 할 수 있게 `Controller`도 수정해줍니다.

```java
// 생략
@Controller
@RequiredArgsConstructor
public class AccountController {
    // 생략
    @GetMapping("/check-email")
    public String checkMail(@CurrentUser Account account, Model model) { // (1)
        model.addAttribute("email", account.getEmail());
        return "account/check-email";
    }

    @GetMapping("/resend-email")
    public String resendEmail(@CurrentUser Account account, Model model) { // (2)
        if (!account.enableToSendEmail()) {
            model.addAttribute("error", "인증 이메일은 5분에 한 번만 전송할 수 있습니다.");
            model.addAttribute("email", account.getEmail());
            return "account/check-email";
        }
        accountService.sendVerificationEmail(account);
        return "redirect:/";
    }
}
```

1. 가입한 이후 내비게이션 바 아래 경고창을 클릭했을 때 이동하므로 가입할 때 사용한 email 정보를 넘겨주면서 리다이렉트 합니다.
2. 이메일 재전송할 때 호출되는 부분으로 새로고침이나 악용하지 못하도록 5분에 한 번만 메일을 보낼 수 있도록 방어 로직을 추가합니다. 인증 메일을 보낼 수 있는 시간이 되면 방어로직은 통과하게 되고 이메일을 보내는 메서드가 실행됩니다.

<details>
<summary>AccountContoller.java 전체 보기</summary>

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

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
}
```

</details>

Controller 클래스에서 사용한 메일 체크하는 메서드를 Account 클래스에 추가하고, AccountService 클래스의 메일 전송 메서드를 public으로 수정해 외부에서 호출할 수 있게 해줍니다.

`Account.java`

```java
// 생략
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder @Getter @ToString
public class Account extends AuditingEntity {
    // 생략
    private LocalDateTime emailTokenGeneratedAt; // (1)

    public void generateToken() { // (2)
        this.emailToken = UUID.randomUUID().toString();
        this.emailTokenGeneratedAt = LocalDateTime.now();
    }

    public boolean enableToSendEmail() { // (3)
        return this.emailTokenGeneratedAt.isBefore(LocalDateTime.now().minusMinutes(5));
    }
    // 생략
}
```

1. 이메일 토큰이 발급한 시기를 저장할 수 있는 필드변수를 생성합니다.
2. 토큰을 발급할 때 발급 시기를 업데이트 합니다.
3. 이메일을 보낼 수 있는지 체크합니다. 5분이 지났는지 체크하도록 하였습니다.

<details>
<summary>Account.java 전체 보기</summary>

```java
package io.lcalmsky.app.account.domain.entity;

import io.lcalmsky.app.account.domain.support.ListStringConverter;
import io.lcalmsky.app.domain.entity.AuditingEntity;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
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
        @Convert(converter = ListStringConverter.class)
        private List<String> url;
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
}
```

</details>

`AccountService.java`

```java
// 생략
@Service
@RequiredArgsConstructor
public class AccountService {
    // 생략
    public void sendVerificationEmail(Account newAccount) { // (1)
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(newAccount.getEmail());
        mailMessage.setSubject("Webluxible 회원 가입 인증");
        mailMessage.setText(String.format("/check-email-token?token=%s&email=%s", newAccount.getEmailToken(),
                newAccount.getEmail()));
        mailSender.send(mailMessage);
    }
    // 생략
}
```

1. public으로 바꿔줘서 외부에서 접근할 수 있게 합니다.

<details>
<summary>AccountService.java 전체 보기</summary>

```java
package io.lcalmsky.app.account.application;

import io.lcalmsky.app.account.domain.UserAccount;
import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.endpoint.controller.SignUpForm;
import io.lcalmsky.app.account.infra.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    @Transactional
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
}

```

</details>

## Test

여기까지 구현을 마쳤다면 애플리케이션을 실행한 뒤 가입을 진행합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/12-01.png)

가입하고 나면 아래와 같은 화면이 노출됩니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/12-02.png)

링크를 클릭하면 아래와 같이 화면이 노출됩니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/12-03.png)

인증 메일 다시 보내기 버튼을 클릭하면 메일을 다시 전송하게 되는데 5분이 지나기 전에 클릭했을 때는 아래 처럼 에러가 발생합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/12-04.png)