![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app/tree/feature/8) 있습니다. (branch: `feature/8`)

## Overview

회원 가입시 전송한 이메일을 확인하여 회원을 인증하는 절차를 개발합니다.

회원 가입시 다시 서버로 요청할 수 있게 토큰을 포함한 링크를 전송하고 사용자가 해당 링크를 클릭했을 때 토큰이 일치하면 가입 완료 처리합니다.

이메일 인증을 하는 이유는 무작위로 생성하는 이메일 계정을 허용하지 않기 위함이고, 서비스 내에서의 메일 전송 기능을 제대로 활용할 수 없기 때문입니다.

이메일 인증을 대체할 수 있는 방법은 소셜 인증 등이 있습니다.

인증 링크로 접근했을 때 노출될 화면과 인증 로직을 개발해야 합니다. 

## 이메일 인증 개발

입력 값에 오류가 있는 경우 에러 문구를 출력합니다.

이 때 오류 문구는 모호하게 노출시키는 게 좋습니다.

힌트를 주는 형태의 오류 문구를 노출하게 되면 어뷰징에 도움이 될 수 있는 등 보안 측면으로 좋은 방법이라고 볼 수 없습니다.

인증이 완료된 경우 환영 문구를 출력합니다.

먼저 컨트롤러를 개발해보겠습니다.

`src/main/java/io/lcalmsky/server/account/endpoint/controller/AccountController.java`

```java
@Controller
@RequiredArgsConstructor
public class AccountController {
    // 생략
    @GetMapping("/check-email-token")
    public String verifyEmail(String token, String email, Model model) { // (1)
        Account account = accountService.findAccountByEmail(email); // (2)
        if (account == null) { // (3)
            model.addAttribute("error", "wrong.email");
            return "account/email-verification";
        }
        if (token.equals(account.getEmailToken())) { // (4)
            model.addAttribute("error", "wrong.token");
            return "account/email-verification";
        }
        account.verified(); // (5)
        model.addAttribute("numberOfUsers", accountRepository.count()); // (6)
        model.addAttribute("nickname", account.getNickname()); // (6)
        return "account/email-verification"; // (7)
    }
}
```

1. 이메일 링크를 클릭하면 해당 메서드로 진입하게 되고 그 때 `email`과 `token`을 파라미터로 전달받습니다.
2. `AccountService`에게 `email`을 이용해 계정 정보를 가져오도록 위임합니다. (`AccountService` 수정 필요)
3. 계정정보가 없으면 기존에 가입한 사용자가 아니므로 모델 객체에 에러를 전달합니다.
4. 계정정보가 있지만 기존에 발급한 `token`과 일치하지 않는 경우 모델 객체에 에러를 전달합니다.
5. `email`과 `token`이 모두 유효하므로 인증 완료 처리를 합니다. (`Account` 수정 필요)
6. 인증에 성공했으므로 성공시 보여줄 데이터를 모델 객체에 전달합니다.
7. 이메일 인증 화면으로 리다이렉트 합니다.

<details>
  <summary>AccountController 전체 소스 코드 보기</summary>

```java
package io.lcalmsky.server.account.endpoint.controller;

import io.lcalmsky.server.account.application.AccountService;
import io.lcalmsky.server.account.domain.entity.Account;
import io.lcalmsky.server.account.endpoint.controller.validator.SignUpFormValidator;
import io.lcalmsky.server.account.infra.repository.AccountRepository;
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
        accountService.signUp(signUpForm);
        return "redirect:/";
    }

    private final AccountRepository accountRepository;

    @GetMapping("/check-email-token")
    public String verifyEmail(String token, String email, Model model) {
        Account account = accountService.findAccountByEmail(email);
        if (account == null) {
            model.addAttribute("error", "wrong.email");
            return "account/email-verification";
        }
        if (token.equals(account.getEmailToken())) {
            model.addAttribute("error", "wrong.token");
            return "account/email-verification";
        }
        account.verified();
        model.addAttribute("numberOfUsers", accountRepository.count());
        model.addAttribute("nickname", account.getNickname());
        return "account/email-verification";
    }
}
```

</details>

2, 5번에 설명된대로 `AccountService`와 `Account` 클래스를 수정하도록 하겠습니다.

먼저 `AccountService`에 메서드를 하나 추가합니다.

`src/main/java/io/lcalmsky/server/account/application/AccountService.java`

```java
// 생략
@Service
@RequiredArgsConstructor
public class AccountService {
    // 생략
    private final AccountRepository accountRepository;

    @Transactional
    public void signUp(SignUpForm signUpForm) {
        Account newAccount = saveNewAccount(signUpForm);
        newAccount.generateToken();
        sendVerificationEmail(newAccount);
    }

    // 생략
    public Account findAccountByEmail(String email) {
        return accountRepository.findByEmail(email); // (1)
    }
}
```

1. `AccountRepository`에서 `mail을` 이용해 `Account Entity`를 가져옵니다.

> 이전 포스팅에서 빼먹은 부분인데 `signUp` 메서드에 `@Transactional` 애너테이션을 추가해줘야 토큰을 발급한 내용이 `DB`에 저장됩니다.

<details>
  <summary>Account Service 전체 소스 코드 보기</summary>

```java
package io.lcalmsky.server.account.application;

import io.lcalmsky.server.account.domain.entity.Account;
import io.lcalmsky.server.account.endpoint.controller.SignUpForm;
import io.lcalmsky.server.account.infra.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signUp(SignUpForm signUpForm) {
        Account newAccount = saveNewAccount(signUpForm);
        newAccount.generateToken();
        sendVerificationEmail(newAccount);
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

    private void sendVerificationEmail(Account newAccount) {
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
}
```

</details>

다음은 `Account` 클래스에도 가입 일시를 나타내는 필드와 메서드를 추가해줍니다.

```java
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder @Getter @ToString
public class Account extends AuditingEntity {
    // 생략
    private LocalDateTime joinedAt;

    public void verified() { // (1)
        this.isValid = true;
        joinedAt = LocalDateTime.now();
    }
}
```

1. 계정이 유효함을 알 수 있게 isValid 항목을 true로, 가입 일시를 현재 시간으로 업데이트합니다.

<details>
<summary>Account 전체 소스 코드 보기</summary>

```java
package io.lcalmsky.server.account.domain.entity;

import io.lcalmsky.server.account.domain.support.ListStringConverter;
import io.lcalmsky.server.domain.entity.AuditingEntity;
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

    public void generateToken() {
        this.emailToken = UUID.randomUUID().toString();
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

---

여기까지 완료되었다면 로컬에서 애플리케이션을 실행하고 테스트 해볼까요?

1. `http://localhost:8080/sign-up`에 먼저 진입하여 가입을 진행합니다.
![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/06-01.png)

2. 가입하기를 누른 뒤 로그에 나타나는 이메일 인증 링크를 확인하고 해당 링크로 다시 요청합니다.  
ex) `http://localhost:8080/check-email-token?token=0b0e52f0-6fd3-4444-b7e3-d04532a3cdee&email=lcalmsky@gmail.com`
![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/06-02.png)
3. 정상적으로 가입된 것을 확인할 수 있습니다. 
![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/06-03.png)

2번에서 다시 요청할 때 token 값이나 email 값을 수정하면 에러가 노출되는 것을 확인할 수 있습니다.

* 일치하지 않는 이메일로 수정했을 때
![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/06-04.png)

* 일치하지 않는 토큰으로 수정했을 때
![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/06-05.png)

## Test 작성

애플리케이션을 실행해서는 확인해봤지만 앞으로 소스 코드를 잘 유지하기위해 테스트 코드도 추가해보도록 하겠습니다.

`src/test/java/io/lcalmsky/server/account/endpoint/controller/AccountControllerTest.java`

```java
// 생략
@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {
    // 생략
    @DisplayName("인증 메일 확인: 잘못된 링크")
    @Test
    void verifyEmailWithWrongLink() throws Exception {
        mockMvc.perform(get("/check-email-token")
                        .param("token", "token") // (1)
                        .param("email", "email")) // (1)
                .andExpect(status().isOk()) // (2)
                .andExpect(view().name("account/email-verification")) // (2)
                .andExpect(model().attributeExists("error")); // (3)
    }

    @DisplayName("인증 메일 확인: 유효한 링크")
    @Test
    @Transactional // (1)
    void verifyEmail() throws Exception {
        Account account = Account.builder() // (2)
                .email("email@email.com")
                .password("1234!@#$")
                .nickname("nickname")
                .notificationSetting(Account.NotificationSetting.builder()
                        .studyCreatedByWeb(true)
                        .studyUpdatedByWeb(true)
                        .studyRegistrationResultByWeb(true)
                        .build())
                .build();
        Account newAccount = accountRepository.save(account); // (3)
        newAccount.generateToken(); // (4)
        mockMvc.perform(get("/check-email-token")
                        .param("token", newAccount.getEmailToken()) // (5)
                        .param("email", newAccount.getEmail())) // (5)
                .andExpect(status().isOk()) // (6)
                .andExpect(view().name("account/email-verification")) // (6)
                .andExpect(model().attributeDoesNotExist("error")) // (7)
                .andExpect(model().attributeExists("numberOfUsers", "nickname")); // (8)
    }
}
```

**인증 메일 확인: 잘못된 링크**

1. 유효하지 않은 토큰과 이메일을 입력합니다.
2. 상태 자체는 200 OK 에서 변함이 없고 `view`도 유지되어야 합니다.
3. `error` 객체가 `model` 객체를 통해 전달되어야 합니다.

**인증 메일 확인: 유효한 링크**

1. DB 트랜잭션이 발생하기 때문에 `@Transactional` 애너테이션을 사용합니다.
2. 토큰을 생성하고 DB와 비교해야 하기 때문에 `Account Entity`를 생성합니다.
3. `Account Entity`를 저장합니다.
4. 토큰을 생성합니다.
5. 요청시 전달할 토큰과 이메일을 계정 생성시 사용한 것과 동일한 것으로 넣어줍니다.
6. 상태와 `view`는 변함이 없어야 합니다.
7. `error` 객체가 포함되면 안 됩니다.
8. `numberOfUsers`와 `nickname`이 `model`을 통해 전달되어야 합니다.

<details>
<summary>AccountControllerTest 전체 소스 코드 보기</summary>

```java
package io.lcalmsky.server.account.endpoint.controller;

import io.lcalmsky.server.account.domain.entity.Account;
import io.lcalmsky.server.account.infra.repository.AccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired AccountRepository accountRepository;
    @MockBean JavaMailSender mailSender;

    @Test
    @DisplayName("회원 가입 화면 진입 확인")
    void signUpForm() throws Exception {
        mockMvc.perform(get("/sign-up"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("account/sign-up"))
                .andExpect(model().attributeExists("signUpForm"));
    }

    @Test
    @DisplayName("회원 가입 처리: 입력값 오류")
    void signUpSubmitWithError() throws Exception {
        mockMvc.perform(post("/sign-up")
                        .param("nickname", "nickname")
                        .param("email", "email@gmail")
                        .param("password", "1234!")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("account/sign-up"));
    }

    @Test
    @DisplayName("회원 가입 처리: 입력값 정상")
    void signUpSubmit() throws Exception {
        mockMvc.perform(post("/sign-up")
                        .param("nickname", "nickname")
                        .param("email", "email@email.com")
                        .param("password", "1234!@#$")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/"));
        assertTrue(accountRepository.existsByEmail("email@email.com"));
        Account account = accountRepository.findByEmail("email@email.com");
        assertNotEquals(account.getPassword(), "1234!@#$");
        assertNotNull(account.getEmailToken());
        then(mailSender)
                .should()
                .send(any(SimpleMailMessage.class));
    }

    @DisplayName("인증 메일 확인: 잘못된 링크")
    @Test
    void verifyEmailWithWrongLink() throws Exception {
        mockMvc.perform(get("/check-email-token")
                        .param("token", "token")
                        .param("email", "email"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/email-verification"))
                .andExpect(model().attributeExists("error"));
    }

    @DisplayName("인증 메일 확인: 유효한 링크")
    @Test
    @Transactional
    void verifyEmail() throws Exception {
        Account account = Account.builder()
                .email("email@email.com")
                .password("1234!@#$")
                .nickname("nickname")
                .notificationSetting(Account.NotificationSetting.builder()
                        .studyCreatedByWeb(true)
                        .studyUpdatedByWeb(true)
                        .studyRegistrationResultByWeb(true)
                        .build())
                .build();
        Account newAccount = accountRepository.save(account);
        newAccount.generateToken();
        mockMvc.perform(get("/check-email-token")
                        .param("token", newAccount.getEmailToken())
                        .param("email", newAccount.getEmail()))
                .andExpect(status().isOk())
                .andExpect(view().name("account/email-verification"))
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attributeExists("numberOfUsers", "nickname"));
    }
}
```

</details>

기존 테스트를 포함해 모두 정상적으로 수행된 것을 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/06-06.png)

---

다음 포스팅에서는 자동 로그인 기능을 다룰 예정입니다.