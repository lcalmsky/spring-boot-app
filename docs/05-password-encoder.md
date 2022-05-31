![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app/tree/feature/7) 있습니다. (branch: `feature/7`)

## Overview

비밀번호를 평문 그대로 저장하는 서비스는 세상 어디에도 없습니다. 특히 전자금융권과 같이 민감한 개인 정보를 다루는 쪽에서는 망을 분리해여 저장하고 접근하기도 합니다.

마찬가지로 지금 개발하는 서비스에도 비밀번호 인코딩 기능을 추가해줘야 합니다.

## Implementation

스프링 시큐리티에서 권장하는 방법은 PasswordEncoder를 사용하는 것입니다.

사용 방법은 매우 간단합니다.

PasswordEncoder를 빈 등록해주면 되는데 직접 사용할 알고리즘을 구현해도 되고 기본값을 사용해도 됩니다.

기본값을 사용할 경우 BCrypt 알고리즘을 사용합니다.

`SecurityConfig` 클래스에 `PasswordEncoder` 빈을 등록하도록 하겠습니다.

`src/main/java/io/lcalmsky/app/config/SecurityConfig.java`

```java
package io.lcalmsky.app.infra.config;

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

    // 생략

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder(); // (1)
    }
}
```

1. 기본 인코더를 빈으로 등록해줍니다.

다음은 인코딩을 적용하기 위해 `AccountService`를 수정해보도록 하겠습니다.

`src/main/java/io/lcalmsky/app/account/application/AccountService.java`

```java
package io.lcalmsky.app.modules.account.application;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.endpoint.controller.form.SignUpForm;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder; // (1)

    public void signUp(SignUpForm signUpForm) {
        Account newAccount = saveNewAccount(signUpForm);
        newAccount.generateToken();
        sendVerificationEmail(newAccount);
    }

    private Account saveNewAccount(SignUpForm signUpForm) {
        Account account = Account.builder()
                .email(signUpForm.getEmail())
                .nickname(signUpForm.getNickname())
                .password(passwordEncoder.encode(signUpForm.getPassword())) // (2)
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

1. 인코더 빈을 주입합니다.
2. 비밀번호를 인코딩한 뒤 저장합니다.

다음은 정상적으로 동작하는지 확인하기위해 기존 테스트를 수정해보겠습니다.

`src/test/java/io/lcalmsky/server/account/endpoint/controller/AccountControllerTest.java`

```java
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
    Account account = accountRepository.findByEmail("email@email.com"); // (1)
    assertNotEquals(account.getPassword(), "1234!@#$"); // (2)

    then(mailSender)
            .should()
            .send(any(SimpleMailMessage.class));
}
```

1. Account Entity 조회를 위해 AccountRepository에 findByEmail 메서드를 추가하고 조회합니다.
2. 조회한 Account Entity의 비밀번호와 실제 입력한 비밀번호가 다른지 검증합니다. 비밀번호 인코딩이 수행됐다면 두 값이 서로 달라야 정상입니다.

`src/main/java/io/lcalmsky/app/account/infra/repository/AccountRepository.java`

```java
package io.lcalmsky.app.modules.account.infra.repository;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Account findByEmail(String email); // (1)
}
```

1. 이메일로 회원 정보를 조회할 수 있게 메서드를 추가해줍니다.

---

모든 수정이 완료됐다면 테스트를 실행해봅시다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/05-01.png)

성공적으로 수행된 것을 확인할 수 있습니다.

---

다음 포스팅에서는 회원 가입 인증메일을 확인하는 기능을 구현해보도록 하겠습니다.