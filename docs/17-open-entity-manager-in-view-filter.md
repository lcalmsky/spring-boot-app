![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 878b1db)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 878b1db
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

[지난 포스팅](https://jaime-note.tistory.com/216)에서 발생한 버그(가입 후 회원인증을 했음에도 가입 날짜가 업데이트 되지 않던)의 원인을 찾아 수정해봅니다.

## 원인

회원 인증(로그인)시 이메일 인증 날짜를 업데이트하고, 프로필에서 가입날짜를 조회할 때 DB의 날짜를 읽어오는데, DB에 인증날짜가 없기 때문에 발생하는 상황입니다.

그렇다면 로그인을 진행했는데 왜 인증 날짜가 업데이트 되지 않았을까요?

기본적으로 스프링에는 `OpenEntityManagerInViewFilter`가 등록되어있고 활성화되어있습니다.

`OpenEntityManagerInViewFilter`는 JPA의 `EntityManager`를 요청을 처리하는 전체 프로세스에 바인딩해주는 역할을 하는데요, 뷰가 렌더링 될때까지 영속성 컨텍스트를 유지하기 때문에 필요한 데이터를 렌더링하는 시점에 추가로 읽어올 수 있게(지연 로딩, Lazy Laoding) 해줍니다.

따라서 `Entity` 객체가 변경된 사항을 저장하기 위해선 트랜잭션이 종료되어야하는데 현재 소스 코드에서는 그렇게 동작하게되어있지 않습니다.

그 이유는 `Controller` 레이어에서 `Repository`를 사용하는 `Service`를 호출했는데, 해당 `Service` 역시 트랜잭션을 처리하도록 되어있지 않았기 때문입니다.

## 해결 방안

원인을 알아냈으니 해결하는 방법을 알아봅시다.

먼저 `Controller`에 `@Transactional` 애너테이션을 추가하는 방법이 있습니다.

이메일 인증시에만 트랜잭션을 조작할 수 있게 해주면 간단히 해결되지만 설계 측면에서 좋은 방법이라고 할 순 없습니다.

`Controller`가 이미 `Service`의 기능을 이용하고있는데 `Service`의 기능이 수정되거나, 다른 메서드에서 동일한 `Service`를 호출하면서 트랜잭션 처리를 안 하게 되면 예외 상황을 항상 컨트롤해야하는 부담이 생깁니다.

반면 트랜잭션 작업이 `Service` 레이어에서 이루어진다면 `Controller`에서 트랜잭션을 얻어서 작업을 진행해야할 때만 호출해주면 되므로 훨씬 관리가 쉽고 `Service` 레이어의 책임 중 하나가 DB 연동하는 과정이라고 생각하고 유사한 기능을 모두 `Service` 레이어에 구현하게 된다면 코드 응집도 또한 올라가게 됩니다.

## Implementation

먼저 `AccountController` 클래스에서 트랜잭션 없이 진행했던 부분을 찾아보겠습니다.

`src/main/java/io/lcalmsky/app/account/endpoint/controller/AccountController.java`

```java
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
    account.verified(); // (1)
    accountService.login(account); // (2)
    model.addAttribute("numberOfUsers", accountRepository.count());
    model.addAttribute("nickname", account.getNickname());
    return "account/email-verification";
}
```

(1)에서 호출한 메서드를 Account.java 클래스에서 찾아보면,

`/src/main/java/io/lcalmsky/app/account/domain/entity/Account.java`

```java
public void verified() {
    this.isValid = true;
    joinedAt = LocalDateTime.now();
}
```

트랜잭션 없이 진행한 것을 확인할 수 있습니다.

(2)에서 호출한 메서드 역시 AccountService.java에서 찾아보면,

`/src/main/java/io/lcalmsky/app/account/application/AccountService.java`

```java
public void login(Account account) {
    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(new UserAccount(account),
            account.getPassword(), Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(token); // AuthenticationManager를 쓰는 방법이 정석적인 방ㅇ법
}
```

마찬가지로 트랜잭션이 사용되지 않았습니다.

---

그럼 AccountController 부터 차례대로 수정해보겠습니다.

```java
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
    accountService.verify(account); // (1)
    model.addAttribute("numberOfUsers", accountRepository.count());
    model.addAttribute("nickname", account.getNickname());
    return "account/email-verification";
}
```

1. accountService의 verify라는 메서드를 호출해줍니다.

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
}
```

</details>

AccountService도 수정해줍니다.

```java
package io.lcalmsky.app.modules.account.application;

import io.lcalmsky.app.modules.account.domain.UserAccount;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.endpoint.controller.form.SignUpForm;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
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

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional // (1)
public class AccountService implements UserDetailsService {

    private final AccountRepository accountRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    public Account signUp(SignUpForm signUpForm) { // (1)
        Account newAccount = saveNewAccount(signUpForm);
        newAccount.generateToken();
        sendVerificationEmail(newAccount);
        return newAccount;
    }

    // 생략

    @Override
    @Transactional(readOnly = true) // (2)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = Optional.ofNullable(accountRepository.findByEmail(username))
                .orElse(accountRepository.findByNickname(username));
        if (account == null) {
            throw new UsernameNotFoundException(username);
        }
        return new UserAccount(account);
    }

    public void verify(Account account) { // (3)
        account.verified();
        login(account);
    }
}

```

1. `signUp` 메서드에 있던 `@Transactional` 애너테이션을 클래스 레벨로 변경합니다. 이렇게 변경하면 `Service` 내의 모든 메서드가 호출될 때 트랜잭션을 가지게 됩니다.
2. 로그인 시 조회용도로만 사용될 것이기 때문에 `readyOnly` 옵션을 추가합니다.
3. `Controller`에서 호출할 메서드를 추가로 구현합니다.

<details>
<summary>AccountService.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.account.application;

import io.lcalmsky.app.modules.account.domain.UserAccount;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.endpoint.controller.form.SignUpForm;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
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
}

```

</details>

## Test

지난 포스팅에서 테스트했던 순서와 동일하게 테스트해보겠습니다.

먼저 애플리케이션을 실행한 뒤 가입을 진행합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/17-01.png)

가입 후 바로 프로필 하면으로 이동합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/17-02.png)

인증하지 않았기 때문에 아직 가입 날짜가 노출되지 않습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/17-03.png)

로그에서 토큰을 찾아 이메일 인증을 수행합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/17-04.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/17-05.png)

다시 프로필을 눌러서 확인해보면 가입 시기가 노출되는 것을 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/17-06.png)

---

> 여기부터는 위 본문과 상관 없는 내용입니다.

참고로 마지막 스크린샷에 url 부분이 빈 값이지만 노출되는 것을 확인할 수 있는데 이 부분도 버그입니다.

아래 클래스를 수정해주시면 나타나지 않는 것을 확인할 수 있습니다.

`/src/main/java/io/lcalmsky/app/account/domain/support/ListStringConverter.java`

```java
package io.lcalmsky.app.modules.account.domain.support;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Converter
public class ListStringConverter implements AttributeConverter<List<String>, String> {
    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        return Optional.ofNullable(attribute)
                .filter(list -> !list.isEmpty()) // 비어있을 때 아무것도 하지 않도록 수정
                .map(a -> String.join(",", a))
                .orElse(null);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return Collections.emptyList();
        }
        return Stream.of(dbData.split(","))
                .collect(Collectors.toList());
    }
}

```

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/17-07.png)