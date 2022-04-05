![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: ac91413)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout ac91413
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

이메일 전송 서비스를 추상화하고 그 구현체를 구현합니다.

프로파일별로 서로 다른 구현체가 실행되게 하여 로그만 출력하는 기능과 메일을 실제로 전송하는 기능이 제대로 동작하는지 확인합니다.

## EmailService 인터페이스 생성

`mail` 패키지를 생성하고 하위에 `EmailService` 인터페이스를 생성합니다.

`/src/main/java/io/lcalmsky/app/mail/EmailService.java`

```java
package io.lcalmsky.app.mail;

public interface EmailService {
    void sendEmail(EmailMessage emailMessage);
}
```

## EmailMessage 클래스 생성

같은 패키지에 EmailMessage 클래스를 생성합니다.

`/src/main/java/io/lcalmsky/app/mail/EmailMessage.java`

```java
package io.lcalmsky.app.mail;

import lombok.*;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EmailMessage {
    private String to;
    private String subject;
    private String message;
}
```

메일을 전송하기 위해 전달할 객체입니다.

## EmailService 구현체 작성

현재 `local` 프로파일일 때는 로그 출력을, `local-db` 프로파일일 때는 메일 전송을하고있는데 이 두 가지 기능을 각각 구현한 구현체를 작성합니다.

### ConsoleEmailService

`EmailService`를 구현하여 로그로 메일을 출력하는 `ConsoleEmailService`를 작성합니다.

```/src/main/java/io/lcalmsky/app/mail/`ConsoleEmailServi`ce.java`

```java
package io.lcalmsky.app.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("local")
@Service
@Slf4j
public class ConsoleEmailService implements EmailService {
    @Override
    public void sendEmail(EmailMessage emailMessage) {
        log.info("sent email: {}", emailMessage.getMessage());
    }
}
```

`local` 프로파일일 때 `sendEmail` 호출시 메일 내용을 로그로 출력하게 하였습니다.

### HtmlEmailService

`EmailService`를 구현하여 메일을 전송하는 `HtmlEmailService`를 작성합니다.

`/src/main/java/io/lcalmsky/app/mail/HtmlEmailService.java`

```java
package io.lcalmsky.app.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Profile("!local")
@Service
@RequiredArgsConstructor
@Slf4j
public class HtmlEmailService implements EmailService {

    private final JavaMailSender javaMailSender;

    @Override
    public void sendEmail(EmailMessage emailMessage) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper;
        try {
            mimeMessageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            mimeMessageHelper.setTo(emailMessage.getTo());
            mimeMessageHelper.setSubject(emailMessage.getSubject());
            mimeMessageHelper.setText(emailMessage.getMessage(), false);
            javaMailSender.send(mimeMessage);
            log.info("sent email: {}", emailMessage.getMessage());
        } catch (MessagingException e) {
            log.error("failed to send email", e);
        }
    }
}

```

`local` 프로파일이 아닐 때 `sendEmail`을 호출하면 직접 메일을 전송하도록 하였습니다.

> 이름은 `HtmlMailService`이지만 현재 `HTML`을 전송하는 기능이 존재하지 않아 `HTML` 여부를 나타내는 `setText`의 두 번째 파라미터를 `false`로 넘겨주고 있습니다.

---

여기까지 작성하였다면 기존에 사용하였던 `ConsoleMailSender`는 더 이상 필요 없으니 삭제합니다.

기존에 `account.infra.mail` 패키지 내부에 존재했었는데 `mail` 패키지 자체를 삭제하였습니다.

**삭제**
`src/main/java/io/lcalmsky/app/account/infra/email/`
`src/main/java/io/lcalmsky/app/account/infra/email/ConsoleMailSender.java`

## AccountService 수정

바로 위에서 패키지를 삭제하였기 때문에 `AccountService`에 의존성이 주입되어있는 `JavaMailSender`는 더 이상 제대로 동작하지 않습니다.

`JavaMailSender`를 대신해 `EmailService`가 그 역할을 대신할 수 있게 소스 코드를 수정해줍니다.

`/src/main/java/io/lcalmsky/app/account/application/AccountService.java`

```java
// 생략
@Slf4j
public class AccountService implements UserDetailsService {
    // 생략
    private final EmailService emailService; // (1)
    // 생략
    public void sendVerificationEmail(Account newAccount) { // (2) 
        emailService.sendEmail(EmailMessage.builder()
                .to(newAccount.getEmail())
                .subject("Webluxible 회원 가입 인증")
                .message(String.format("/check-email-token?token=%s&email=%s", newAccount.getEmailToken(),
                        newAccount.getEmail()))
                .build());
    }
    // 생략    
    public void sendLoginLink(Account account) { // (2)
        account.generateToken();
        emailService.sendEmail(EmailMessage.builder()
                .to(account.getEmail())
                .subject("[Webluxible] 로그인 링크")
                .message("/login-by-email?token=" + account.getEmailToken() + "&email=" + account.getEmail())
                .build());
    }
}

```

1. `JavaMailSender` 대신 `EmailService`를 주입받습니다.
2. `EmailService`로 기존과 동일하게 작성하여 메일을 전송합니다.

<details>
<summary>AccountService.java 전체 보기</summary>

```java
package io.lcalmsky.app.account.application;

import io.lcalmsky.app.account.domain.UserAccount;
import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.domain.entity.Zone;
import io.lcalmsky.app.account.endpoint.controller.SignUpForm;
import io.lcalmsky.app.account.infra.repository.AccountRepository;
import io.lcalmsky.app.mail.EmailMessage;
import io.lcalmsky.app.mail.EmailService;
import io.lcalmsky.app.settings.controller.NotificationForm;
import io.lcalmsky.app.settings.controller.Profile;
import io.lcalmsky.app.tag.domain.entity.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AccountService implements UserDetailsService {

    private final AccountRepository accountRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public Account signUp(SignUpForm signUpForm) {
        Account newAccount = saveNewAccount(signUpForm);
        sendVerificationEmail(newAccount);
        return newAccount;
    }

    private Account saveNewAccount(SignUpForm signUpForm) {
        Account account = Account.with(signUpForm.getEmail(), signUpForm.getNickname(), passwordEncoder.encode(signUpForm.getPassword()));
        account.generateToken();
        return accountRepository.save(account);
    }

    public void sendVerificationEmail(Account newAccount) {
        emailService.sendEmail(EmailMessage.builder()
                .to(newAccount.getEmail())
                .subject("Webluxible 회원 가입 인증")
                .message(String.format("/check-email-token?token=%s&email=%s", newAccount.getEmailToken(),
                        newAccount.getEmail()))
                .build());
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
        emailService.sendEmail(EmailMessage.builder()
                .to(account.getEmail())
                .subject("[Webluxible] 로그인 링크")
                .message("/login-by-email?token=" + account.getEmailToken() + "&email=" + account.getEmail())
                .build());
    }

    public void addTag(Account account, Tag tag) {
        accountRepository.findById(account.getId())
                .ifPresent(a -> a.getTags().add(tag));
    }

    public Set<Tag> getTags(Account account) {
        return accountRepository.findById(account.getId()).orElseThrow().getTags();
    }

    public void removeTag(Account account, Tag tag) {
        accountRepository.findById(account.getId())
                .map(Account::getTags)
                .ifPresent(tags -> tags.remove(tag));
    }

    public Set<Zone> getZones(Account account) {
        return accountRepository.findById(account.getId())
                .orElseThrow()
                .getZones();
    }

    public void addZone(Account account, Zone zone) {
        accountRepository.findById(account.getId())
                .ifPresent(a -> a.getZones().add(zone));
    }

    public void removeZone(Account account, Zone zone) {
        accountRepository.findById(account.getId())
                .ifPresent(a -> a.getZones().remove(zone));
    }
}
```

</details>

## 테스트 코드 수정

여기까지 모두 작성하였으면 `test` 패키지를 우클릭하여 모든 테스트를 수행합니다.

> 리팩토링을 했을 경우 꼭 테스트를 돌려보시는 게 좋습니다.

제가 캡쳐는 따로 못했지만 `AccountControllerTest`에서 회원 가입 처리 후 이메일 전송이 제대로 되었는지 확인하는 부분에서 테스트에 실패하게 됩니다.

마찬가지로 수정해주겠습니다.

`/src/test/java/io/lcalmsky/app/account/endpoint/controller/AccountControllerTest.java`

```java
// 생략
class AccountControllerTest {
    // 생략
    @MockBean EmailService emailService; // (1)
    // 생략
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
                .andExpect(view().name("redirect:/"))
                .andExpect(authenticated().withUsername("nickname"));
        assertTrue(accountRepository.existsByEmail("email@email.com"));
        Account account = accountRepository.findByEmail("email@email.com");
        assertNotEquals(account.getPassword(), "1234!@#$");
        assertNotNull(account.getEmailToken());
        then(emailService) // (2) 
                .should()
                .sendEmail(any(EmailMessage.class));
    }
    // 생략
}
```

1. `JavaMailService`를 `EmailService`로 변경해줍니다.
2. `emailService`가 `EmailMessage`를 이용해 `sendEmail`을 호출하는지 확인합니다.

<details>
<summary>AccountControllerTest.java 전체 보기</summary>

```java
package io.lcalmsky.app.account.endpoint.controller;

import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.infra.repository.AccountRepository;
import io.lcalmsky.app.mail.EmailMessage;
import io.lcalmsky.app.mail.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired AccountRepository accountRepository;
    @MockBean EmailService emailService;

    @Test
    @DisplayName("회원 가입 화면 진입 확인")
    void signUpForm() throws Exception {
        mockMvc.perform(get("/sign-up"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("account/sign-up"))
                .andExpect(model().attributeExists("signUpForm"))
                .andExpect(unauthenticated());
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
                .andExpect(view().name("redirect:/"))
                .andExpect(authenticated().withUsername("nickname"));
        assertTrue(accountRepository.existsByEmail("email@email.com"));
        Account account = accountRepository.findByEmail("email@email.com");
        assertNotEquals(account.getPassword(), "1234!@#$");
        assertNotNull(account.getEmailToken());
        then(emailService)
                .should()
                .sendEmail(any(EmailMessage.class));
    }

    @DisplayName("인증 메일 확인: 잘못된 링크")
    @Test
    void verifyEmailWithWrongLink() throws Exception {
        mockMvc.perform(get("/check-email-token")
                        .param("token", "token")
                        .param("email", "email"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/email-verification"))
                .andExpect(model().attributeExists("error"))
                .andExpect(unauthenticated());
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
                .andExpect(model().attributeExists("numberOfUsers", "nickname"))
                .andExpect(authenticated().withUsername("nickname"));
    }
}
```

</details>

## 테스트

먼저 `local` 프로파일을 이용해 애플리케이션을 실행한 뒤, 가입을 진행해 로그로 메일 내용이 출력되는지 확인합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/35-01.png)

로그로 출력된 `url`을 이용해 다시 브라우저로 접속해보면,

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/35-02.png)

정상적으로 가입되었음을 확인할 수 있습니다.

다음으로 `local-db` 프로파일을 이용해 애플리케이션을 실행한 뒤 동일하게 가입을 진행하여 메일을 수신하는지 확인합니다.

> 로컬 DB에 이미 사용하시는 메일을 이용해 가입한 적이 있으면 추가 가입이 안 되므로 테스트를 위해 데이터를 지우고 시작하셔도 됩니다.

마찬가지로 로그에 메일 전송 내용이 적힌 것을 확인할 수 있고,

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/35-03.png)

메일 수신함에 들어가보면 메일 또한 정상적으로(로그 시간과 메일 수신 시간을 확인) 수신한 것을 확인할 수 있습니다. 

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/35-04.png)

메일로 전송된 링크로 접속하면 마찬가지로 정상적으로 가입처리가 됩니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/35-05.png)

---

다음 포스팅에서는 `HTML`로 메일을 전송하도록 수정하겠습니다.