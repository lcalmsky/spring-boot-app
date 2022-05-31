![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 9c46a61)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 9c46a61
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

지난 포스팅에서 구현한 프로필 수정에 대한 테스트 코드를 작성합니다.

기존에 작성했던 테스트와는 다르게 인증된 사용자가 있는 상태에서 테스트 코드를 작성해야 합니다.

대부분의 테스트의 경우 사실 인증된 사용자에 대해 작성해야 할 때가 더 많기 때문에 기존과 중복되는 부분이 없으니 평소 통합 테스트 작성하시는데 어렵다고 느끼시는 분들에게는 도움이 될 거 같네요😄

## SettingsController 수정

테스트 코드를 작성해야하는데 기존 컨트롤러를 왜 수정하는지 의아하신 분들이 있으실 거 같은데 대단한 건 아니고 상수를 같이 쓰기 위함입니다.

기존 public에서 default 레벨로 수정해주겠습니다.

`/src/main/java/io/lcalmsky/app/settings/controller/SettingsController.java`

```java
// 생략
public class SettingsController {
    static final String SETTINGS_PROFILE_VIEW_NAME = "settings/profile";
    static final String SETTINGS_PROFILE_URL = "/" + SETTINGS_PROFILE_VIEW_NAME;
    // 생략
}
```

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

@Controller
@RequiredArgsConstructor
public class SettingsController {

    static final String SETTINGS_PROFILE_VIEW_NAME = "settings/profile";
    static final String SETTINGS_PROFILE_URL = "/" + SETTINGS_PROFILE_VIEW_NAME;

    private final AccountService accountService;

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
}
```

</details>

## Validation 코드 추가

테스트 코드 작성 전에 `Profile` 클래스에 누락된 `validation`을 추가하겠습니다.

`/src/main/java/io/lcalmsky/app/settings/controller/Profile.java`

```java
// 생략
public class Profile {
    @Length(max = 35)
    private String bio;
    @Length(max = 50)
    private String url;
    @Length(max = 50)
    private String job;
    @Length(max = 50)
    private String location;
    // 생략
}
```

간단하게 길이에 대한 `validation`만 추가하였습니다.

<details>
<summary>Profile.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.settings.controller;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.util.Optional;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile {
    @Length(max = 35)
    private String bio;
    @Length(max = 50)
    private String url;
    @Length(max = 50)
    private String job;
    @Length(max = 50)
    private String location;

    public static Profile from(Account account) {
        return new Profile(account);
    }

    protected Profile(Account account) {
        this.bio = Optional.ofNullable(account.getProfile()).map(Account.Profile::getBio).orElse(null);
        this.job = Optional.ofNullable(account.getProfile()).map(Account.Profile::getJob).orElse(null);
        this.url = Optional.ofNullable(account.getProfile()).map(Account.Profile::getUrl).orElse(null);
        this.location = Optional.ofNullable(account.getProfile()).map(Account.Profile::getLocation).orElse(null);
    }
}
```

</details>

## 테스트 코드 작성

이제 본격적으로 테스트 코드를 작성해보겠습니다.

`/src/test/java/io/lcalmsky/app/settings/controller/SettingsControllerTest.java`

먼저 전체 코드를 확인해보겠습니다.

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
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    @DisplayName("프로필 수정 폼")
    @WithAccount("jaime")
    void updateProfileForm() throws Exception {
        String bio = "한 줄 소개";
        mockMvc.perform(get(SettingsController.SETTINGS_PROFILE_URL))
                .andExpect(status().isOk())
                .andExpect(view().name(SettingsController.SETTINGS_PROFILE_VIEW_NAME))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("profile"));
    }
}
```

파트별로 나눠서 살펴보도록 하겠습니다.

### SecurityContext 설정

인증된 사용자는 곧 `SecurityContext`가 인증정보를 가지고 있다는 것을 의미하고, 이렇게 설정하기 위한 여러 가지 방법을 스프링이 제공합니다.

여기서는 `@WithSecurityContext` 애너테이션을 사용해 `SecurityContext`에 인증정보를 주입해보겠습니다.

먼저 `WithAccount`라는 애너테이션을 생성합니다.

`/src/test/java/io/lcalmsky/app/WithAccount.java`

```java
package io.lcalmsky.app;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME) // (1)
@WithSecurityContext(factory = WithAccountSecurityContextFactory.class) // (2) 
public @interface WithAccount {
    String value(); // (3)
}
```

1. 런타임시 동작하도록 설정합니다.
2. `SecurityContext`를 설정해줄 클래스를 지정합니다.
3. 하나의 값을 `attribute`로 전달 받기위해 메서드를 명시하였습니다. `nickname`을 주입받을 예정입니다.

위 2번의 `@WithSecurityContext`의 `attribute`로 전달하고 있는 팩토리 클래스도 생성해줍니다.

`/src/test/java/io/lcalmsky/app/WithAccountSecurityContextFactory.java`

```java
package io.lcalmsky.app;

import io.lcalmsky.app.modules.account.application.AccountService;
import io.lcalmsky.app.modules.account.endpoint.controller.form.SignUpForm;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithAccountSecurityContextFactory implements WithSecurityContextFactory<WithAccount> { // (1)

    private final AccountService accountService;

    public WithAccountSecurityContextFactory(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public SecurityContext createSecurityContext(WithAccount annotation) { // (2)
        String nickname = annotation.value(); // (3)

        SignUpForm signUpForm = new SignUpForm(); // (4)
        signUpForm.setNickname(nickname);
        signUpForm.setEmail(nickname + "@gmail.com");
        signUpForm.setPassword("1234asdf");
        accountService.signUp(signUpForm);

        UserDetails principal = accountService.loadUserByUsername(nickname); // (5)
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities()); // (6)
        SecurityContext context = SecurityContextHolder.createEmptyContext(); // (7)
        context.setAuthentication(authentication);
        return context;
    }
}

```

1. `WithSecurityContextFactory` 인터페이스를 구현해야하고 이 때 전달할 타입은 이전에 생성한 애너테이션과 동일해야 합니다.
2. 메서드 이름에서 이미 알려주고 있지만 `SecurityContext`를 생성하기위한 메서드를 구현합니다.
3. `@WithAccount` 애너테이션의 `attribute`로 주입받은 `nickname`을 사용합니다.
4. 가입을 위해 `SignUpForm` 객체를 생성 및 설정하고 `accountService`를 이용해 가입시킵니다.
5. 가입 후 DB에 저장된 정보를 불러옵니다.
6. `Authentication` 구현체 중 하나인 토큰 객체를 생성해 DB에서 읽어온 값으로 설정해줍니다.
7. `SecurityContext` 객체를 가져와 인증 정보를 설정해 반환합니다.

여기까지 작성하였다면, 테스트 코드에 간단히 애너테이션 추가를 통해 인증된 사용자 정보를 전달할 수 있습니다.

### 정상 케이스

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
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SettingsControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired AccountRepository accountRepository;

    @AfterEach
    void afterEach() { // (1)
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("프로필 수정: 입력값 정상")
    @WithAccount("jaime")
    void updateProfile() throws Exception {
        String bio = "한 줄 소개";
        mockMvc.perform(post(SettingsController.SETTINGS_PROFILE_URL) // (2)
                        .param("bio", bio)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection()) // (3)
                .andExpect(redirectedUrl(SettingsController.SETTINGS_PROFILE_URL)) // (3)
                .andExpect(flash().attributeExists("message")); // (4)
        Account jaime = accountRepository.findByNickname("jaime"); // (5)
        assertEquals(bio, jaime.getProfile().getBio()); // (5)
    }
}
```

1. `WithAccount` 애너테이션을 통해 인증 정보를 주입할 때 DB에 해당 정보가 저장되므로 테스트가 끝나면 반드시 삭제해줘야 다른 테스트에 영향을 미치지 않습니다.
2. `SettingsController`의 상수 접근 레벨을 수정한 이유입니다. URL을 상수로 전달하면 오타를 방지할 수 있습니다.
3. 정상 처리 되었을 경우 다시 동일한 페이지로 리다이렉트합니다.
4. 정상일 경우 `flashAttribute`로 메시지를 전달하여 수정 완료되었다는 UI 피드백을 전달하므로 해당 키가 존재하는지 확인합니다.
5. DB에 저장된 사용자 정보를 불러와 `profile`이 정확하게 업데이트 되었는지 확인합니다.

### 비정상 케이스(입력값)

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
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
    @DisplayName("프로필 수정: 입력값 에러")
    @WithAccount("jaime")
    void updateProfileWithError() throws Exception {
        String bio = "35자 넘으면 에러35자 넘으면 에러35자 넘으면 에러35자 넘으면 에러"; // (1)
        mockMvc.perform(post(SettingsController.SETTINGS_PROFILE_URL)
                        .param("bio", bio)
                        .with(csrf()))
                .andExpect(status().isOk()) // (2)
                .andExpect(view().name(SettingsController.SETTINGS_PROFILE_VIEW_NAME)) // (3)
                .andExpect(model().hasErrors()) // (4)
                .andExpect(model().attributeExists("account")) // (5)
                .andExpect(model().attributeExists("profile"));
        Account jaime = accountRepository.findByNickname("jaime");
        assertNull(jaime.getProfile().getBio()); // (6)
    }
}
```

1. `Profile` 클래스에 추가한 `validation`을 테스트하기 위해 한 줄 소개를 35자보다 길게 설정합니다.
2. 응답은 200 OK 지만 에러를 전달합니다.
3. 리다이렉트 되는 것이 아니라 해당 뷰를 다시 보여줍니다.
4. 에러 객체가 있는지 확인합니다.
5. `SettingsController`에서 에러일 경우 `account`와 `profile` 객체를 전달하도록 작성하였는데 제대로 동작하는지 확인합니다.
6. DB에 소개가 업데이트 되지 않았을 것이므로 null이어야 합니다.

### 프로필 조회

```java
package io.lcalmsky.app.modules.settings.controller;

import io.lcalmsky.app.modules.account.WithAccount;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}
```

프로필 조회 API를 호출하였을 때 `view`가 제대로 호출되는지, 그리고 `model`로 `account`객체와 `profile` 객체를 잘 전달하는지 확인합니다.

## 테스트 결과 확인

모두 정상적으로 테스트 되었음을 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/20-01.png)