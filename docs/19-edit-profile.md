![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 0fa1c18)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 0fa1c18
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

프로필 수정 기능을 구현합니다.

기존 객체 정보를 변경할 때 스프링 MVC와 JPA에서 고려해야 할 사항에 대해 알아봅니다.

## SettingsController 수정

Profile 폼을 통해 받은 데이터로 프로필을 수정하고 페이지를 리다이렉트 시켜주기 위해 컨트롤러를 수정합니다.

`/src/main/java/io/lcalmsky/app/settings/controller/SettingsController.java`

```java
package io.lcalmsky.app.modules.settings.controller;

import io.lcalmsky.app.modules.account.application.AccountService;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.support.CurrentUser;
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

    public static final String SETTINGS_PROFILE_VIEW_NAME = "settings/profile"; // (1)
    public static final String SETTINGS_PROFILE_URL = "/" + SETTINGS_PROFILE_VIEW_NAME; // (1)

    private final AccountService accountService; // (5)

    @GetMapping(SETTINGS_PROFILE_URL) // (2)
    public String profileUpdateForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(Profile.from(account));
        return SETTINGS_PROFILE_VIEW_NAME; // (2)
    }

    @PostMapping(SETTINGS_PROFILE_URL) // (2)
    public String updateProfile(@CurrentUser Account account, @Valid Profile profile, Errors errors, Model model) { // (3)
        if (errors.hasErrors()) { // (4) 
            model.addAttribute(account);
            return SETTINGS_PROFILE_VIEW_NAME;
        }
        accountService.updateProfile(account, profile); // (5)
        return "redirect:" + SETTINGS_PROFILE_URL; // (6)
    }
}
```

1. 자주 사용되는 값들을 상수로 정의합니다.
2. 기존 문자열을 상수 변수로 대체합니다.
3. 현재 사용자의 계정 정보와 프로필 폼을 통해 전달된 정보를 받습니다. Profile 폼을 validation 할 때 발생하는 에러들을 Errors 객체를 통해 전달받습니다. 다시 뷰로 데이터를 전달하기 위한 model 객체도 주입받습니다.
4. 에러가 있으면 model에 폼을 채웠던 데이터와 에러 관련된 데이터는 자동으로 전달되므로 계정정보만 추가로 전달하고 다시 해당 뷰를 보여줍니다.
5. AccountService에게 프로필 업데이트를 위임합니다.
6. 사용자가 화면을 refresh 하더라도 form submit이 다시 발생하지 않도록 redirect 합니다.

## AccountService 수정

서비스 수정사항은 매우 간단합니다. Profile 폼의 데이터를 전달받아 account 객체에 프로필 업데이트를 위임합니다.

`/src/main/java/io/lcalmsky/app/account/application/AccountService.java`

```java
// 생략
public class AccountService implements UserDetailsService {
    // 생략
    public void updateProfile(Account account, Profile profile) {
        account.updateProfile(profile);
    }
}
```

<details>
<summary>AccountService.java 전체 보기</summary>

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
    }
}
```

</details>

## Account Entity 수정

마찬가지로 서비스로부터 프로필 데이터를 전달받아 필드를 업데이트 합니다.

`/src/main/java/io/lcalmsky/app/account/domain/entity/Account.java`

```java
// 생략
public class Account extends AuditingEntity {
    // 생략
    public void updateProfile(io.lcalmsky.app.modules.account.endpoint.controller.form.Profile profile) {
        if (this.profile == null) {
            this.profile = new Profile();
        }
        this.profile.bio = profile.getBio();
        this.profile.url = profile.getUrl();
        this.profile.job = profile.getJob();
        this.profile.location = profile.getLocation();
    }
    // 생략
}
```

---

여기까지 아주 간단하게 수정해봤는데 실제로 프로필을 수정했을 때 제대로 적용되지 않는 것을 알 수 있습니다.

지금부터 그 이유를 알아보겠습니다.

## 문제점 및 해결 방법

> 원본 강의에서는 Profile 폼 클래스에서 기본 생성자가 없어 발생하는 에러도 다룹니다만, 저는 처음부터 기본생성자를 생성하였기 때문에 해당 에러가 발생하지 않았습니다.  
> 스프링에서 컨트롤러로 해당 객체를 주입해줄 때 기본 생성자가 `protected` 레벨 이상으로 존재하지 않는 경우 객체를 생성할 수 없어 `NPE`가 발생합니다.  
> 저는 기본적으로 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`를 사용해 사용자가 직접 `new`를 통해 객체 생성하는 것을 막고, 역직렬화가 필요한 시점에서는 내부적으로 `new`를 통해 객체를 생성할 수 있게 합니다.

업데이트가 되지 않았다는 것은 DB와 연동하는 부분에 문제가 있다는 것이고, 그 중 가장 유력한 부분은 JPA의 영속성과 관련되어 있습니다.

하지만 기존에도 AccountService의 메서드들을 이용했는데 그 때는 발생하지 않고 이번엔 발생했습니다.

지금부터 그 원인에 대해 알아보겠습니다.

먼저 컨트롤러로 주입받는 Account는 JPA 입장에서 관리되고있는(영속성을 가진) 객체가 아닙니다.

영속성을 가지기 위해선 영속성을 관리하는 주체가 DB와 연동된 적이 있다는 것을 알고 있어야 합니다.

예를 들어, Repository를 통해 조회해 온 경우, Entity를 생성한 뒤 save 메서드를 통해 DB의 저장한 경우가 이에 해당합니다.

컨트롤러로 주입받는 Account는 세션에 존재하는 값이지 영속성을 가진 객체가 아닙니다.

따라서 영속성을 가지게 하기 위해선 위에서 예를 든 것 처럼 DB에서 조회해오거나 DB로 저장이 필요합니다.

이미 가지고 있는 정보를 다시 조회할 필요는 없고 Entity에서 필드를 업데이트 해준 뒤 Service 레이어에서 repository로 저장해주면 문제가 해결됩니다.

`/src/main/java/io/lcalmsky/app/account/application/AccountService.java`

```java
// 생략
public class AccountService implements UserDetailsService {
    // 생략
    public void updateProfile(Account account, Profile profile) {
        account.updateProfile(profile);
        accountRepository.save(account); // 수정한 정보를 Repository를 통해 저장합니다.
    }
}
```

<details>
<summary>AccountService.java 전체 보기</summary>

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
}
```

</details>

## 테스트 및 UX 수정

여기까지 완료가 되었다면 앱을 실행한 뒤 프로필 수정 메뉴에서 값을 입력하여 결과를 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/19-01.png)

다만, 프로필 수정 후 수정하기 버튼을 눌렀을 때 아무런 리액션이 없이 리다이렉트 되기 때문에 수정이 된 것인지 정확한 확인이 어렵습니다.

컨트롤러와 뷰를 수정해 UX 피드백을 추가해보겠습니다.

`/src/main/java/io/lcalmsky/app/settings/controller/SettingsController.java`

```java
// 생략
public class SettingsController {
    public static final String SETTINGS_PROFILE_VIEW_NAME = "settings/profile";
    public static final String SETTINGS_PROFILE_URL = "/" + SETTINGS_PROFILE_VIEW_NAME;
    // 생략
    @PostMapping(SETTINGS_PROFILE_URL)
    public String updateProfile(@CurrentUser Account account, @Valid Profile profile, Errors errors, Model model, RedirectAttributes attributes) { // (1)
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS_PROFILE_VIEW_NAME;
        }
        accountService.updateProfile(account, profile);
        attributes.addFlashAttribute("message", "프로필을 수정하였습니다."); // (2)
        return "redirect:" + SETTINGS_PROFILE_URL;
    }
}

```

1. RedirectAttributes는 리다이렉트 시 1회성 데이터를 전달할 수 있는 객체이므로 컨트롤러로 주입해줍니다.
2. 리다이렉트 시 `addFlashAttribute`를 이용해 1회성 데이터를 전달합니다. 앞서 에러인 경우에 대해 처리했기 때문에 성공했을 때 전달할 메시지를 attribute로 추가합니다.

`/src/main/resources/templates/settings/profile.html`

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
        <!-- 생략-->
        </div>
    </div>
</div>
</body>
</html>
```

폼 입력받는 곳 상단에 info 레벨의 닫기가 가능한 alert을 추가합니다.

---

다시 애플리케이션을 실행하고 테스트했을 때 아래처럼 나타나면 정상입니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/19-02.png)

프로필 조회 화면으로 다시 이동했을 때 데이터가 저장되어있음을 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/19-03.png)

> 이메일 인증까지 완료하면 가입일도 확인할 수 있습니다.  
> ![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/19-04.png)

---

다음 포스팅에서는 프로필 수정 테스트를 작성해보겠습니다.

그리고 아직 프로필이 끝난 게 아닙니다! 프로필 이미지 변경 기능 구현도 남아있는데 이는 조금 더 뒤에 다뤄보겠습니다.