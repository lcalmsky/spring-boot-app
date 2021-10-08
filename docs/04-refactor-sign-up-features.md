![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app/tree/feature/6) 있습니다. (branch: `feature/6`)

## Overview

지금까지 작성한 코드를 리펙터링합니다.

리펙터링 전에 테스트 코드를 작성하면 리펙터링 이후에도 견고한 테스트 코드를 작성했는지 추가로 확인할 수 있습니다.

테스트 할 것을 정의합니다.

* 회원 가입시 이상한 값이 입력된 경우
  * 다시 회원 가입 화면으로 리다이렉트 하는지 확인
  * 에러가 잘 노출 되는지 확인
* 회원 가입시 정상적인 값이 입력된 경우
  * 가입한 회원 데이터가 존재하는지 확인
  * 이메일이 보내지는지 확인

리팩터링시 고려해야할 부분입니다.

* 메서드의 길이
  * 너무 길면 메서드를 나눔
* 코드 가독성
* 코드의 위치
  * 객체들 사이의 의존 관계
  * 클래스의 책임이 너무 많지는 않은지

## Prerequisite

`dependency`에 security test package를 추가해줍니다.

```groovy
dependencies {
    // 생략
    // test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
}
```

## 테스트 코드 작성

전체 코드입니다.

`src/test/java/io/lcalmsky/server/account/endpoint/controller/AccountControllerTest.java`

```java
package io.lcalmsky.server.account.endpoint.controller;

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

import static org.junit.jupiter.api.Assertions.assertTrue;
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
                        .param("email", "email@gmail.com")
                        .param("password", "1234!@#$")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().is3xxRedirection()) // redirection 응답
                .andExpect(view().name("redirect:/")); // 후 리다이렉트

        assertTrue(accountRepository.existsByEmail("lcalmsky@gmail.com")); // 메일이 DB에 저장되었는지 확인

        then(mailSender) // 모킹 된 mailSender로 send가 호출되었고 SimpleMailMessage가 전달되었는지 확인
                .should()
                .send(any(SimpleMailMessage.class));
    }
}
```

**회원 가입 처리: 입력값 오류**
```java
@Test
@DisplayName("회원 가입 처리: 입력값 오류")
void signUpSubmitWithError() throws Exception {
    mockMvc.perform(post("/sign-up")
                    .param("nickname", "nickname")
                    .param("email", "email@gmail") // (1)
                    .param("password", "1234!") // (2)
                    .with(csrf()))
            .andDo(print())
            .andExpect(status().isOk()) // (3)
            .andExpect(view().name("account/sign-up")); // (4)
}
```
1. 이메일을 일부러 포맷에 맞지 않게 입력했습니다.
2. 비밀번호를 일부러 8자리가 안 되도록 입력했습니다.
3. 상태는 처리 여부와 상관없이 `200 OK` 를 반환합니다. `AccountController`에 페이지를 이동시키도록 구현되어있기 때문입니다.
4. 입력값이 잘못되었기 때문에 /sign-up 페이지로 되돌아가 에러를 노출합니다.

**회원 가입 처리: 입력값 정상**
```java
package io.lcalmsky.server.account.endpoint.controller;

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

import static org.junit.jupiter.api.Assertions.assertTrue;
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
    @Autowired AccountRepository accountRepository; // (1)
    @MockBean JavaMailSender mailSender; // (6)
    
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

        then(mailSender)
                .should()
                .send(any(SimpleMailMessage.class));
    }
}
```
1. 회원 가입 이후 이메일 검증을 위해 `AccountRepository`를 주입해줍니다.
2. 모든 필드의 값을 정상적으로 입력하고 `csrf` 설정을 해줍니다. `security`, `thymeleaf`를 같이 사용하면 `thymeleaf`에서 `csrf` 토큰을 임의로 생성해서 넣어주기 때문에 `csrf()` 없이 수행할 경우 403 에러가 발생합니다.
3. 모두 정상적으로 입력했을 경우 `redirect` 하도록 되어있어 해당 상태를 반환하는지 확인합니다.
4. redirect 되어 루트 페이지로 이동했는지 확인합니다.
5. 이메일이 정상적으로 저장되었는지 확인합니다.
6. 메일을 전송했는지 확인합니다. 실제로 전송 여부를 확인하기 어렵기 때문에 JavaMailSender를 @MockBean을 이용해 주입하고, mailSender가 send라는 메서드를 호출했고 그 때 전달된 타입이 SimpleMailMessage 타입인지 확인합니다.

> **⚠️ Warning:** 기존 로컬 설정처럼 H2 데이터베이스를 사용하면서 파일 DB 형태로 테스트하게되면 기존에 추가한 값이 존재할 경우 제대로된 테스트가 이루어지지 않을 수 있습니다.  
> DB에 없는 값으로 테스트하는 방법과 테스트 시 설정 파일을 추가하여 다른 DB를 사용할 수 있게하는 방법이 있습니다.  
> 전자의 경우 언젠간 해당하는 값이 실제 DB에 반영되면 테스트가 얼마든지 깨질 수 있으므로 후자 방법으로 진행하시는 것을 권장드립니다.  

> **💡Tip:** 테스트 패키지에 설정 추가하는 방법
> `src/test/resources/application.yml`
> ```yaml
> spring:
>   datasource:
>     url: jdbc:h2:mem:testdb
> ```

## 리팩터링

먼저 기존 `AccountController` 클래스를 확인해볼까요?

`src/main/java/io/lcalmsky/server/account/endpoint/controller/AccountController.java`

```java
package io.lcalmsky.server.account.endpoint.controller;

import io.lcalmsky.server.account.domain.entity.Account;
import io.lcalmsky.server.account.endpoint.controller.validator.SignUpFormValidator;
import io.lcalmsky.server.account.infra.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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

    private final SignUpFormValidator signUpFormValidator;
    private final AccountRepository accountRepository;
    private final JavaMailSender mailSender;

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
        Account account = Account.builder()
                .email(signUpForm.getEmail())
                .nickname(signUpForm.getNickname())
                .password(signUpForm.getPassword())
                .notificationSetting(Account.NotificationSetting.builder()
                        .studyCreatedByWeb(true)
                        .studyUpdatedByWeb(true)
                        .studyRegistrationResultByWeb(true)
                        .build())
                .build();
        Account newAccount = accountRepository.save(account);

        newAccount.generateToken();
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(newAccount.getEmail());
        mailMessage.setSubject("Webluxible 회원 가입 인증");
        mailMessage.setText(String.format("/check-email-token?token=%s&email=%s", newAccount.getEmailToken(),
                newAccount.getEmail()));
        mailSender.send(mailMessage);

        return "redirect:/";
    }
}
```

현재 `AccountController`는 너무 많은 일을 하고있습니다.

보통 컨트롤러가 가지는 책임 뿐만 아니라 실제 비즈니스 로직까지 모두 처리하고 있는데요, 리팩터링 과정을 보여드리면서 수행해보도록 하겠습니다.

### 메서드 추출 (Method Extraction)

일단 `signUpSubmit` 메서드 자체가 너무 길기 때문에 메서드로 추출해보도록 하겠습니다.

이 때 메서드 명은 주석이 없어도 읽고 파악하기 쉽게 작명하는 것이 중요합니다.

```java
@PostMapping("/sign-up")
public String signUpSubmit(@Valid @ModelAttribute SignUpForm signUpForm, Errors errors) {
    if (errors.hasErrors()) {
        return "account/sign-up";
    }
    Account newAccount = saveNewAccount(signUpForm); // (1)
    newAccount.generateToken();
    sendVerificationEmail(newAccount); // (2)
    return "redirect:/";
}

private Account saveNewAccount(SignUpForm signUpForm) {
    Account account = Account.builder()
            .email(signUpForm.getEmail())
            .nickname(signUpForm.getNickname())
            .password(signUpForm.getPassword())
            .notificationSetting(Account.NotificationSetting.builder()
                    .studyCreatedByWeb(true)
                    .studyUpdatedByWeb(true)
                    .studyRegistrationResultByWeb(true)
                    .build())
            .build();
    Account newAccount = accountRepository.save(account);
    return newAccount;
}

private void sendVerificationEmail(Account newAccount) {
    SimpleMailMessage mailMessage = new SimpleMailMessage();
    mailMessage.setTo(newAccount.getEmail());
    mailMessage.setSubject("Webluxible 회원 가입 인증");
    mailMessage.setText(String.format("/check-email-token?token=%s&email=%s", newAccount.getEmailToken(),
            newAccount.getEmail()));
    mailSender.send(mailMessage);
}
```

1. `Entity`를 생성하고 저장하는 부분을 분리하였습니다.
2. 검증 메일을 전송하는 부분을 분리하였습니다.

> **💡 Tip:** IntelliJ IDEA나 Eclipse 같은 IDE에서는 메서드를 추출하는 기능을 제공합니다. 추출할 부분을 블럭지정한 뒤 refactor 메뉴를 찾아보세요.  
> 참고로 macOS + IntelliJ IDEA 조합을 쓰시는 분들은 블럭 지정 후 `⌥` + `⌘` + `M` 단축키를 사용하시면 됩니다.  
> 또는 `⌃` + `T` 입력 후 컨텍스트 메뉴에서 method를 검색하시거나 숫자를 누르셔도 됩니다.

### 책임 분리

위에서 메서드로 추출했지만 컨트롤러가 아직 너무 많은 책임을 가지고 있는 것은 변함이 없습니다.

분리한 메서드를 서비스 레이어로 이동시키도록 하겠습니다.

`src/main/java/io/lcalmsky/server/account/application/AccountService.java`

```java
package io.lcalmsky.server.account.application;

import io.lcalmsky.server.account.domain.entity.Account;
import io.lcalmsky.server.account.endpoint.controller.SignUpForm;
import io.lcalmsky.server.account.infra.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final JavaMailSender mailSender;

    public Account saveNewAccount(SignUpForm signUpForm) {
        Account account = Account.builder()
                .email(signUpForm.getEmail())
                .nickname(signUpForm.getNickname())
                .password(signUpForm.getPassword())
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
}
```

분리한 메서드를 옮겨오면서 필요한 의존관계 또한 같이 옮겨왔고 기존에 private 이었던 것들을 public으로 바꿔줬습니다.

`AccountController` 역시 수정해줘야겠죠?

```java
package io.lcalmsky.server.account.endpoint.controller;

import io.lcalmsky.server.account.application.AccountService;
import io.lcalmsky.server.account.domain.entity.Account;
import io.lcalmsky.server.account.endpoint.controller.validator.SignUpFormValidator;
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

        Account newAccount = accountService.saveNewAccount(signUpForm);
        newAccount.generateToken();
        accountService.sendVerificationEmail(newAccount);
        return "redirect:/";
    }
}
```

이전보다 깔끔해진 것을 알 수 있습니다.

### 캡슐화

여기서 한 차례 더 리팩터링을 할 수 있는 방법이 있습니다.

서비스 내부 로직이 굳이 컨트롤러 레이어에 노출될 필요가 없습니다.

만약에 여러 메서드에서 공통으로 사용될 부분이 아니라면 어떤 절차로 어떻게 진행되는지 컨트롤러에서 알 필요가 없으므로 메서드를 수정해주도록 하겠습니다.

먼저 `AccountController` 입장에서 `AccountService`가 하는 일은 결국 회원 가입(signUp)을 하는 것이므로 아래 처럼 수정해주겠습니다.

```java
@PostMapping("/sign-up")
public String signUpSubmit(@Valid @ModelAttribute SignUpForm signUpForm, Errors errors) {
    if (errors.hasErrors()) {
        return "account/sign-up";
    }
    accountService.signUp(signUpForm);
    return "redirect:/";
}
```

다음은 `AccountService`에서 기존과 동일하게 처리할 수 있게 수정해주겠습니다.

```java
package io.lcalmsky.server.account.application;

import io.lcalmsky.server.account.domain.entity.Account;
import io.lcalmsky.server.account.endpoint.controller.SignUpForm;
import io.lcalmsky.server.account.infra.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final JavaMailSender mailSender;

    public void signUp(SignUpForm signUpForm) {
        Account newAccount = saveNewAccount(signUpForm);
        newAccount.generateToken();
        sendVerificationEmail(newAccount);
    }

    private Account saveNewAccount(SignUpForm signUpForm) {
        Account account = Account.builder()
                .email(signUpForm.getEmail())
                .nickname(signUpForm.getNickname())
                .password(signUpForm.getPassword())
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
}
```

이렇게 수정하면 외부에서는 `signUp`만 호출하면 되므로 `saveNewAccount`와 `sendVerificationEmail`은 다시 `private` 레벨로 변경해 줄 수 있습니다.

---

여기까지 리팩터링을 완료했으면 기존 테스트 코드를 수행해 동일하게 동작하는지 확인합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/04-01.png)

모두 정상 동작하는 것을 확인할 수 있습니다.

---

다음 포스팅에서는 비밀번호를 인코딩하는 방법을 다뤄보도록 하겠습니다. 