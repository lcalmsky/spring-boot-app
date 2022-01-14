![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: f4673f8)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout f4673f8
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

이전 포스팅에서 구현한 로그인과 로그아웃 기능을 테스트합니다. 

## Tips

테스트를 작성하기에 앞서 이전 포스팅에서 다뤘던 내용 중 `/login`을 호출할 때 반드시 `username`과 `password` 파라미터를 전달해야 한다는 부분이 있었는데요, 이는 spring security 기본 설정이고 파라미터명을 변경하기 위해선 `SecurityConfig` 클래스를 수정해줘야 합니다.

`/src/main/java/io/lcalmsky/app/config/SecurityConfig.java`

```java
// 생략
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .mvcMatchers("/", "/login", "/sign-up", "/check-email-token",
                        "/email-login", "/check-email-login", "/login-link").permitAll()
                .mvcMatchers(HttpMethod.GET, "/profile/*").permitAll()
                .anyRequest().authenticated();
        http.formLogin()
                .loginPage("/login")
                .usernameParameter("id") // (1)
                .passwordParameter("pw") // (2)
                .permitAll();
        http.logout()
                .logoutSuccessUrl("/");
    }
    // 생략
}
```

1. `username`을 `id`로 바꿀 수 있습니다.
2. `password`를 `pw`로 바꿀 수 있습니다.

## Test

먼저 이메일로 로그인 테스트를 작성해보겠습니다.

```java
package io.lcalmsky.app.main.endpoint.controller;

import io.lcalmsky.app.account.application.AccountService;
import io.lcalmsky.app.account.endpoint.controller.SignUpForm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MainControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired AccountService accountService;

    @Test
    @DisplayName("이메일로 로그인: 성공")
    void login_with_email() throws Exception {
        SignUpForm signUpForm = new SignUpForm();
        signUpForm.setNickname("jaime");
        signUpForm.setEmail("lcalmsky@gmail.com");
        signUpForm.setPassword("test1234");
        accountService.signUp(signUpForm); // (1)
        mockMvc.perform(post("/login") // (2)
                        .param("username", "lcalmsky@gmail.com") // (2)
                        .param("password", "test1234") // (2)
                        .with(csrf())) // (3)
                .andExpect(status().is3xxRedirection()) // (4)
                .andExpect(redirectedUrl("/")) // (5)
                .andExpect(authenticated().withUsername("jaime")); // (6)
    }
}
```

1. `AccountService`를 이용해 테스트할 계정을 가입시킵니다.
2. `/login`을 호출합니다. `parameter`로 `username`과 `password`를 전달합니다.
3. `spring security`를 사용했기 때문에 `csrf` 요청이 필요합니다.
4. 결과는 로그인 된 이후 redirect 응답을 받아야 합니다.
5. redirect된 url은 루트("/")가 되어야 합니다.
6. 인증이 되어야 하고 이 때 `username`은 `nickname`이 되어야 합니다. 그 이유는 `UserAccount` 클래스에서 부모 클래스의 생성자를 호출할 때 이메일이 아닌 `nickname`을 전달했기 때문입니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/14-01.png)

성공한 것을 확인할 수 있습니다.

다음으로 nickname으로 로그인 테스트를 작성해보겠습니다.

이전 코드와 매우 유사하기 때문에 나머지 부분은 생략하겠습니다.

```java
@Test
@DisplayName("닉네임으로 로그인: 성공")
void login_with_nickname() throws Exception {
    SignUpForm signUpForm = new SignUpForm();
    signUpForm.setNickname("jaime");
    signUpForm.setEmail("lcalmsky@gmail.com");
    signUpForm.setPassword("test1234");
    accountService.signUp(signUpForm);
    mockMvc.perform(post("/login")
                    .param("username", "jaime")
                    .param("password", "test1234")
                    .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"))
            .andExpect(authenticated().withUsername("jaime"));
}
```

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/14-02.png)

마찬가지로 성공한 것을 확인할 수 있습니다.

---

방금 두 코드는 메서드로 나눠져있지만 매우 많은 부분의 중복된 코드가 존재합니다.

그래서 계정을 가입시키는 부분을 따로 메서드로 추출하겠습니다.

`@BeforeEach`를 이용해 매 테스트 이전 반복적으로 수행되게 할 수 있습니다.

대신 이렇게 진행했을 경우 다음 테스트에 영향을 줄 수 있기 때문에 `@AfterEach`를 이용해 DB의 데이터를 모두 지워줍니다.

최종적으로 아래 처럼 수정하였습니다.

```java
package io.lcalmsky.app.main.endpoint.controller;

import io.lcalmsky.app.account.application.AccountService;
import io.lcalmsky.app.account.endpoint.controller.SignUpForm;
import io.lcalmsky.app.account.infra.repository.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MainControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired AccountService accountService;
    @Autowired AccountRepository accountRepository;

    @BeforeEach
    void beforeEach() {
        SignUpForm signUpForm = new SignUpForm();
        signUpForm.setNickname("jaime");
        signUpForm.setEmail("lcalmsky@gmail.com");
        signUpForm.setPassword("test1234");
        accountService.signUp(signUpForm);
    }

    @AfterEach
    void afterEach() {
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("이메일로 로그인: 성공")
    void login_with_email() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "lcalmsky@gmail.com")
                        .param("password", "test1234")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("jaime"));
    }

    @Test
    @DisplayName("닉네임으로 로그인: 성공")
    void login_with_nickname() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "jaime")
                        .param("password", "test1234")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("jaime"));
    }
}
```

다음으로 로그인 실패 테스트를 작성해보겠습니다.

```java
@Test
@DisplayName("로그인 실패")
void login_fail() throws Exception {
    mockMvc.perform(post("/login")
            .param("username", "test") // (1)
            .param("password", "test1234")
            .with(csrf()))
        .andExpect(status().is3xxRedirection()) // (2)
        .andExpect(redirectedUrl("/login?error")) // (3)
        .andExpect(unauthenticated()); // (4)
}
```

1. 가입할 때 사용하지 않은 `nickname`을 적어줍니다. 
2. 실패시에도 redirect 됩니다.
3. redirect 되는 url은 /login?error 인데요, 이는 spring security에서 자동으로 처리해주는 부분입니다.
4. 로그인에 실패하였기 때문에 인증되지 않은 상태로 남아있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/14-03.png)

마찬가지로 성공한 것을 확인할 수 있습니다.

마지막으로 로그아웃 테스트를 해보겠습니다.

```java
@Test
@DisplayName("로그아웃: 성공")
void logout() throws Exception {
    mockMvc.perform(post("/logout") // (1)
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/")) // (2)
        .andExpect(unauthenticated()); // (3)
}
```

1. /logout 요청을 합니다.
2. logout 이후에는 루트("/")로 redirect 되어야 합니다.
3. logout 하였기 때문에 인증되지 않은 상태가 되어야 합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/14-04.png)

성공한 것을 확인할 수 있습니다.