![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 51d546d)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 51d546d
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

로그인을 유지하기 위한 기능(RememberMe)을 추가합니다.

## Description

로그인 이후 서버에서 `JSESSIONID`를 발급(메모리에 저장)해주게 되고, 클라이언트에서는 그 정보를 쿠키(Cookies)에 저장합니다. 

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/15-01.png)

그리고 클라이언트에서 서버로 요청할 때마다 `JSESSIONID`를 같이 요청하게되면 서버에서는 로그인되어있다고 생각하고 요청을 처리해줍니다.

만약 클라이언트(브라우저)에서 `JSESSIONID`를 지우고 요청하면 어떻게 될까요?

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/15-02.png)

먼저 `JSESSIONID`라는 항목을 지운 뒤 새로고침 해봤더니

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/15-03.png)

이렇게 다시 로그인이 필요한 상태가 됩니다.

서버를 껐다 켰을 때 역시 메모리에서 관리하던 세션이 없어지므로 브라우저에서 어떤 요청을 했을 때 다시 로그인이 필요한 상태가 되겠죠?

결국 로그인이 되어있다는 것은 서버와 클라이언트 간에 주고 받는 키가 동기화되어 관리되고 있다는 뜻이고, 거꾸로 말하자면 로그아웃은 이 키의 동기화 상태를 끊어주는 작업이라고 생각하시면 됩니다.

그렇다면 로그인 한 상태에서 가만히 놔두기만 한다면 시간에 관계없이 로그인 상태가 유지될까요?

이 부분은 서버에서 어떻게 관리하느냐에 따라 다릅니다.

스프링 부트의 경우 Embedded Tomcat 설정에 따르는데 기본 값은 30분동안 세션을 유지하도록 되어있습니다.

> 설정파일에서 수정할 수 있습니다.
> ```properties
> server.sevlet.session.timeout
> ```

즉, 로그인 이후 아무 작업 없이 30분동안 다른 일을 하다가 다시 조작하게되면 로그인을 다시 해줘야 합니다.

세션을 엄청 긴 시간으로 유지하도록 할 수도 있지만 이는 서버 메모리에 영향을 줄 수 있으므로 권장하는 방법은 아닙니다.

그렇다면 어떻게 해야 로그인을 계속 유지할 수 있을까요?

첫 번째 방법으로는 쿠키를 하나 더 사용하는 것입니다.

인증 정보를 담고있는 암호화된 값을 쿠키에 저장해두고 세션이 만료되었을 때 해당 값을 이용해 인증을 시도해 다시 세션을 발급받는 방법이 있습니다.

클라이언트에서 해당 쿠키를 지우기 전까지는 얼마든지 서버에서 세션 만료처리를 하더라도 다시 로그인을 자동적으로 시도할 수 있고, 서버의 메모리를 사용하는 것이 아니라 클라이언트의 메모리를 사용하는 방식이기 때문에 서버에 부담을 주지도 않습니다.

하지만 이 방법의 가장 큰 단점은 언제든지 사용자가 악의적으로 쿠키를 탈취할 수 있다는 점입니다. 특히 여러 사람이 사용하는 공용 컴퓨터에서 로그인 유지 기능을 사용했을 경우 계정을 다른 사람과 공유하는 것과 다름 없는 일이 발생할 수 있습니다.

따라서 공용으로 사용하는 컴퓨터에서는 사용 이후 반드시 로그아웃 처리를 할 필요가 있습니다. (하지만 이 경우에도 서버에서 로그아웃을 제대로 구현하지 않았다면 쿠키나 세션 등을 유지할 가능성이 있으므로 크롬 사용자의 경우 시크릿 모드 등을 활용하시는 게 더 안전합니다.)

이 방법을 안전하게 구현하는 방법은, 쿠키 안에 토큰(랜덤 문자열)을 같이 저장하고 매번 인증할 때마다 바꾸는 방법인데요, 이 방법도 결국은 쿠키를 탈취당했을 때 안전하지 못합니다.

위의 방법을 조금 더 개선한 방법이 있습니다.

랜덤한 토큰 값과 같이 시리즈라고 부르는 랜덤 값을 같이 사용합니다. 시리즈는 랜덤 값이긴 하지만 처음 발급된 이후로는 고정된 값입니다.

쿠키를 탈취당했을 경우 탈취한 쪽에서는 유효한 토큰과 고정된 시리즈를 사용해 접근하게 되고, 탈취 당한 쪽에서는 유효하지 않은 토큰과 고정된 시리즈를 이용해 요청하게 되는데, 이렇게 잘못된 요청이 왔을 경우 기존에 관리하던 모든 토큰을 삭제하여 탈취한 쪽에서도 유효하지 않은 접근이 되도록 만드는 방법입니다.

정리하면, spring security는 해시 기반의 쿠키와 최종 개선된 방법 두 가지를 제공합니다.

이 포스팅에서는 당연히 그 중 가장 안전한 방법으로 설정할 것인데요, 이 부분은 아래에서 살펴보도록 하고 해싱 기반으로 설정하는 방법은 여기서 간단히 소스 코드로 소개하고 넘어가겠습니다.

```java
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import java.util.UUID;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.rememberMe().key(UUID.randomUUID().toString());
    }
}
```

아주 간단하죠? 하지만 이 방법은 위에서도 설명했듯이 안전한 방법이 아니므로 가장 안전한 방법으로 구현해보도록 하겠습니다.

## Implementation

앞에서 설명한 방법을 설정하기 위해서 `SecurityConfig` 클래스를 수정합니다.

`/src/main/java/io/lcalmsky/app/config/SecurityConfig.java`

```java
// 생략
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private final AccountService accountService; // (1)
    private final DataSource dataSource; // (2)

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .mvcMatchers("/", "/login", "/sign-up", "/check-email-token",
                        "/email-login", "/check-email-login", "/login-link").permitAll()
                .mvcMatchers(HttpMethod.GET, "/profile/*").permitAll()
                .anyRequest().authenticated();
        http.formLogin()
                .loginPage("/login")
                .permitAll();
        http.logout()
                .logoutSuccessUrl("/");
        http.rememberMe() // (3)
                .userDetailsService(accountService)
                .tokenRepository(tokenRepository());
    }

    @Bean
    public PersistentTokenRepository tokenRepository() { // (4)
        JdbcTokenRepositoryImpl jdbcTokenRepository = new JdbcTokenRepositoryImpl();
        jdbcTokenRepository.setDataSource(dataSource);
        return jdbcTokenRepository;
    }
    // 생략   
}

```

1. `userDetailsService`에 설정하기 위해 주입해줍니다.
2. 토큰 저장소를 설정하기 위해 주입해줍니다.
3. `userDetailsService`와 `token`을 관리할 `repository`를 설정해줍니다.
4. 토큰 관리를 위한 `repository` 구현체를 추가하는데 직접 구현할 필요가 없습니다. `dataSource`만 설정해주면 됩니다.

<details>
<summary>SecurityConfig.java 전체 보기</summary>

```java
package io.lcalmsky.app.config;

import io.lcalmsky.app.account.application.AccountService;
import lombok.RequiredArgsConstructor;
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
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private final AccountService accountService;
    private final DataSource dataSource;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .mvcMatchers("/", "/login", "/sign-up", "/check-email-token",
                        "/email-login", "/check-email-login", "/login-link").permitAll()
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

    @Bean
    public PersistentTokenRepository tokenRepository() {
        JdbcTokenRepositoryImpl jdbcTokenRepository = new JdbcTokenRepositoryImpl();
        jdbcTokenRepository.setDataSource(dataSource);
        return jdbcTokenRepository;
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                .mvcMatchers("/node_modules/**", "/images/**")
                .antMatchers("/h2-console/**");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
```

</details>

tokenRepository로 설정한 빈에서 반환해주는 구현체 내부적으로 사용하는 테이블이 따로 있습니다.

```java
// 생략
public class JdbcTokenRepositoryImpl extends JdbcDaoSupport implements PersistentTokenRepository {

    /** Default SQL for creating the database table to store the tokens */
    public static final String CREATE_TABLE_SQL = "create table persistent_logins (username varchar(64) not null, series varchar(64) primary key, "
            + "token varchar(64) not null, last_used timestamp not null)";
    // 생략
}
```

현재 H2를 사용하고 있으므로 `Entity` 클래스만 생성해주면 테이블을 자동으로 생성해주겠죠?

`PersistentLogins` 클래스를 생성합니다.

`/src/main/java/io/lcalmsky/app/account/domain/entity/PersistentLogins.java`

```java
package io.lcalmsky.app.account.domain.entity;

import lombok.Getter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Table(name = "persistent_logins")
@Entity
@Getter
public class PersistentLogins {
    @Id
    @Column(length = 64)
    private String series;

    @Column(length = 64)
    private String username;

    @Column(length = 64)
    private String token;

    @Column(name = "last_used", length = 64)
    private LocalDateTime lastUsed;
    
}
```

다음은 로그인 유지 체크박스 버튼을 생성해줍니다.

이전 `username`, `password`와 마찬가지로 정해진 키 값이 있고 그대로 사용하게 되면 자동으로 처리해줍니다.

```html
<!DOCTYPE html>
<html lang="en"
      xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<body class="bg-light">
<div th:replace="fragments.html :: navigation-bar"></div>

<div class="container">
    <!-- p: padding, y: 위 아래, 0~5까지 설정 가능 -->
    <div class="py-5 text-center">
        <p class="lead">Webluxible</p>
        <h2>로그인</h2>
    </div>
    <div class="row justify-content-center">
        <!-- error라는 파라미터가 있으면 아래 화면을 보여줌 -->
        <div th:if="${param.error}" class="ui-icon-alert alert-danger" role="alert">
            <p class="text-center">로그인 정보가 정확하지 않습니다.</p>
            <p class="text-center"><a href="#" th:href="@{/find-password}">패스워드 찾기</a></p>
        </div>

        <form class="needs-validation col-sm-6" action="#" th:action="@{/login}" method="post" novalidate>
            <div class="form-group">
                <label for="username">이메일 또는 닉네임</label>
                <input id="username" type="text" name="username" class="form-control" placeholder="your@email.com"
                       aria-describedby="emailHelp" required>
                <small id="emailHelp" class="form-text text-muted">
                    가입할 때 사용한 이메일 또는 닉네임을 입력하세요.
                </small>
                <small class="invalid-feedback">이메일을 입력하세요.</small>
            </div>
            <div class="form-group">
                <label for="password">패스워드</label>
                <input id="password" type="password" name="password" class="form-control"
                       aria-describedby="passwordHelp" required>
                <small id="passwordHelp" class="form-text text-muted">
                    패스워드가 기억나지 않으시나요? <a href="#" th:href="@{/email-login}">이메일로 로그인하기</a>
                </small>
                <small class="invalid-feedback">패스워드를 입력하세요.</small>
            </div>
            <!-- 이 부분 추가 시작 -->
            <div class="form-group form-check">
                <input type="checkbox" class="form-check-input" id="rememberMe" name="remember-me" checked>
                <label class="form-check-label" for="rememberMe" aria-describedby="rememberMeHelp">로그인 유지</label>
           </div>
            <!-- 이 부분 추가 끝 -->
            <div class="form-group">
                <button class="btn btn-success btn-block" type="submit" aria-describedby="submitHelp">로그인</button>
                <small id="submitHelp" class="form-text text-muted">
                    아직 회원이 아니신가요? <a href="#" th:href="@{/signup}">가입하기</a>
                </small>
            </div>
        </form>
    </div>
    <div th:replace="fragments::footer"></div>
</div>
<script th:replace="fragments::form-validation"></script>
</body>
</html>
```

추가해야 할 부분에 주석처리 하였습니다.

---
여기까지 작성한 뒤 애플리케이션을 실행해보면 아래와 같은 에러가 발생합니다.

```text
***************************
APPLICATION FAILED TO START
***************************

Description:

The dependencies of some of the beans in the application context form a cycle:

┌─────┐
|  accountService defined in file [/Users/jaime/git-repo/spring-boot-app/out/production/classes/io/lcalmsky/app/account/application/AccountService.class]
↑     ↓
|  securityConfig defined in file [/Users/jaime/git-repo/spring-boot-app/out/production/classes/io/lcalmsky/app/config/SecurityConfig.class]
└─────┘

```

로그에 잘 나와있지만 `AccountService`와 `SecurityConfig`가 서로 순환참조하고있는데요, 그 이유는 `AccountService`에서 `PasswordEncoder`를 주입받아서 사용하는데 `PasswordEncoder`를 주입받기 위해서는 `SecurityConfig` 빈이 먼저 생성되어야 합니다.

하지만 `SecurityConfig`를 생성하려면 `AccountService`를 주입받아야하므로 순환 참조가 발생하는 것입니다.

따라서 이 원인이 되는 `PasswordEncoder` 빈을 다른 설정으로 옮겨줍니다.

`AppConfig` 클래스를 생성해 `PasswordEncoder` 빈을 추가합니다.

`/src/main/java/io/lcalmsky/app/config/AppConfig.java`

```java
package io.lcalmsky.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
```

`SecurityConfig` 클래스에서 `PasswordEncoder` 빈 추가하는 부분을 삭제합니다.

/src/main/java/io/lcalmsky/app/config/SecurityConfig.java

```java
package io.lcalmsky.app.config;

import io.lcalmsky.app.account.application.AccountService;
import lombok.RequiredArgsConstructor;
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
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private final AccountService accountService;
    private final DataSource dataSource;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .mvcMatchers("/", "/login", "/sign-up", "/check-email-token",
                        "/email-login", "/check-email-login", "/login-link").permitAll()
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

    @Bean
    public PersistentTokenRepository tokenRepository() {
        JdbcTokenRepositoryImpl jdbcTokenRepository = new JdbcTokenRepositoryImpl();
        jdbcTokenRepository.setDataSource(dataSource);
        return jdbcTokenRepository;
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                .mvcMatchers("/node_modules/**", "/images/**")
                .antMatchers("/h2-console/**");
    }
    
    // PasswordEncoder 빈 삭제
}
```

## Test

소스 코드 작성을 완료했다면 앱을 실행하고 가입 후 로그아웃 한 뒤 다시 로그인 해줍니다. 이 때 로그인 유지 체크 박스를 체크해줘야 합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/15-04.png)

그리고 쿠키를 확인해보면 `remember-me`라는 키를 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/15-05.png)

로그인 유지 기능이 동작하는지 확인하기 위해 `JSESSIONID`를 삭제해 줍니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/15-06.png)

다시 새로고침을 눌러보면 사용자가 추가로 인증할 필요 없이 새로운 `JSESSIONID`를 발급해준 것을 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/15-07.png)