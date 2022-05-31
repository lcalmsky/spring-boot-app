![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: f4673f8)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout f4673f8
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

로그인과 로그아웃 기능을 구현합니다. 

로그인 화면을 작성하고 spring security 패키지를 활용해 로그인을 처리합니다.

## Implementation

먼저 `SecurityConfig` 클래스를 수정하여 로그인, 로그아웃 관련 설정을 추가합니다.

`src/main/java/io/lcalmsky/app/config/SecurityConfig.java`

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
        http.formLogin() // (1)
                .loginPage("/login") // (2)
                .permitAll(); // (3)
        http.logout() // (4)
                .logoutSuccessUrl("/"); // (5)
    }
    // 생략
}
```

1. formLogin()을 설정하면 form 기반 인증을 지원합니다. 2번의 loginPage를 지정하지 않으면 스프링이 기본으로 로그인 페이지를 생성해줍니다.
2. loginPage로 로그인 페이지를 지정할 수 있습니다.
3. 로그인 페이지에는 인증하지 않아도 접근할 수 있게 해줍니다.
4. logout 시 설정을 지원합니다.
5. logout 성공시 루트(/)로 이동하도록 설정하였습니다.

<details>
<summary>SecurityConfig.java 전체 보기</summary>

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

로그인 시 login 페이지로 이동하게 했으니 컨트롤러에도 추가해줘야겠죠?

MainController 클래스를 아래 처럼 수정해줍니다.

`src/main/java/io/lcalmsky/app/main/endpoint/controller/MainController.java`

```java
package io.lcalmsky.app.modules.main.endpoint.controller;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.support.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
    @GetMapping("/")
    public String home(@CurrentUser Account account, Model model) {
        if (account != null) {
            model.addAttribute(account);
        }
        return "index";
    }

    @GetMapping("/login") // (1)
    public String login() {
        return "login";
    }
}
```

다음은 login 페이지를 만들어보겠습니다.

`src/main/resources/templates/login.html`

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

여기까지 작성했다면 /login을 호출했을 때 login.html이 실행되고 form 태그 부분에서 다시 /login을 호출하게 되는데, 이 때는 spring security가 제공하는 /login이 호출되게 됩니다.

따라서 따로 로그인 핸들러를 구현할 필요가 없으나 username, password 처럼 spring security가 구현한 로그인 핸들러에서 사용하는 변수 이름은 동일하게 설정해줘야 합니다. 자세한 내용은 아래서 설명하겠습니다.

마지막에 script form-validation는 index.html에서 사용했던 script와 동일한 코드를 fragment 쪽으로 옮긴 뒤 참조한 것인데요, fragment.html 파일을 아래처럼 수정해주신 뒤 해당 script 부분을 바로 위에서 참조한 것처럼 수정해주시면 됩니다.

`src/main/resources/templates/fragments.html`

```html
<!DOCTYPE html>
<html lang="en"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<!--생략-->
<script type="application/javascript" th:fragment="form-validation">
    (function () {
        'use strict';

        window.addEventListener('load', function () {
            // Fetch all the forms we want to apply custom Bootstrap validation styles to
            const forms = document.getElementsByClassName('needs-validation');

            // Loop over them and prevent submission
            Array.prototype.filter.call(forms, function (form) {
                form.addEventListener('submit', function (event) {
                    if (form.checkValidity() === false) {
                        event.preventDefault();
                        event.stopPropagation();
                    }
                    form.classList.add('was-validated')
                }, false)
            })
        }, false)
    }())
</script>

</html>
```

<details>
<summary>fragment.html 전체 보기</summary>

```html
<!DOCTYPE html>
<html lang="en"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head th:fragment="head">
    <meta charset="UTF-8">
    <title>Webluxible</title>
    <link rel="stylesheet" href="/node_modules/bootstrap/dist/css/bootstrap.min.css"/>
    <link rel="stylesheet" href="/node_modules/font-awesome/css/font-awesome.min.css"/> <!--font-awesome 추가-->
    <script src="/node_modules/jdenticon/dist/jdenticon.min.js"></script> <!--jdenticon script 추가-->
    <script src="/node_modules/jquery/dist/jquery.min.js"></script> <!--index.html에서 옮김-->
    <script src="/node_modules/bootstrap/dist/js/bootstrap.bundle.min.js"></script> <!--index.html에서 옮김-->
    <style>
        .container {
            max-width: 100%;
        }
    </style>
</head>

<footer th:fragment="footer">
    <div class="row justify-content-center">
        <small class="d-flex mb-3 text-muted" style="justify-content: center">Webluxible &copy; 2021</small>
    </div>
</footer>

<nav th:fragment="navigation-bar" class="navbar navbar-expand-sm navbar-dark bg-dark">
    <a class="navbar-brand" href="/" th:href="@{/}">
        <img src="/images/logo.png" width="30" height="30">
    </a>
    <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-target="#navbarSupportedContent"
            aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
        <span class="navbar-toggler-icon"></span>
    </button>

    <div class="collapse navbar-collapse" id="navbarSupportedContent">
        <ul class="navbar-nav mr-auto">
            <li class="nav-item">
                <form th:action="@{/search/study}" class="form-inline" method="get">
                    <input class="form-control mr-sm-2" name="keyword" type="search" placeholder="스터디 찾기"
                           aria-label="Search"/>
                </form>
            </li>
        </ul>

        <ul class="navbar-nav justify-content-end">
            <li class="nav-item" sec:authorize="!isAuthenticated()">
                <a class="nav-link" th:href="@{/login}">로그인</a>
            </li>
            <li class="nav-item" sec:authorize="!isAuthenticated()">
                <a class="nav-link" th:href="@{/sign-up}">가입</a>
            </li>
            <li class="nav-item" sec:authorize="isAuthenticated()">
                <a class="nav-link" th:href="@{/notifications}">
                    <i class="fa fa-bell-o" aria-hidden="true"></i> <!--"알림" 문자열을 종 모양 아이콘으로 수정-->
                </a>
            </li>
            <li class="nav-item" sec:authorize="isAuthenticated()">
                <a class="nav-link btn btn-outline-primary" th:href="@{/notifications}">
                    <i class="fa fa-plus" aria-hidden="true"></i> 스터디 개설 <!--"스터디 개설" 문자열 앞에 플러스 아이콘 추가-->
                </a>
            </li>
            <li class="nav-item dropdown" sec:authorize="isAuthenticated()">
                <a class="nav-link dropdown-toggle" href="#" id="userDropdown" role="button" data-bs-toggle="dropdown"
                   aria-haspopup="true" aria-expanded="false">
                    <svg data-jdenticon-value="user127" th:data-jdenticon-value="${#authentication.name}" width="24"
                         height="24" class="rounded border bg-light"></svg><!--"프로필" 대신 아바타 이미지를 보여줌-->
                </a>
                <div class="dropdown-menu dropdown-menu-sm-right" aria-labelledby="userDropdown">
                    <h6 class="dropdown-header">
                        <span sec:authentication="name">Username</span>
                    </h6>
                    <a class="dropdown-item" th:href="@{'/profile/' + ${#authentication.name}}">프로필</a>
                    <a class="dropdown-item">스터디</a>
                    <div class="dropdown-divider"></div>
                    <a class="dropdown-item" href="#" th:href="@{'/settings/profile'}">설정</a>
                    <form class="form-inline my-2 my-lg-0" action="#" th:action="@{/logout}" method="post">
                        <button class="dropdown-item" type="submit">로그아웃</button>
                    </form>
                </div>
            </li>
        </ul>
    </div>
</nav>

<script type="application/javascript" th:fragment="form-validation">
    (function () {
        'use strict';

        window.addEventListener('load', function () {
            // Fetch all the forms we want to apply custom Bootstrap validation styles to
            const forms = document.getElementsByClassName('needs-validation');

            // Loop over them and prevent submission
            Array.prototype.filter.call(forms, function (form) {
                form.addEventListener('submit', function (event) {
                    if (form.checkValidity() === false) {
                        event.preventDefault();
                        event.stopPropagation();
                    }
                    form.classList.add('was-validated')
                }, false)
            })
        }, false)
    }())
</script>

</html>
```

</details>

여기까지 작성했다면 요청에 대한 핸들링은 spring security가 알아서 처리해주지만, 로그인 절차는 구현해줘야 하는데 그 절차를 구현할 수 있는 인터페이스가 바로 `UserDetailsService` 입니다.

기존에 `AccountService`가 `UserDetailsService`를 `implements`하게 수정하여 소스 코드를 작성해보겠습니다.

`/src/main/java/io/lcalmsky/app/account/application/AccountService.java`

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
public class AccountService implements UserDetailsService { // (1)

    private final AccountRepository accountRepository;

    // 생략

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException { // (2)
        Account account = Optional.ofNullable(accountRepository.findByEmail(username)) // (3)
                .orElse(accountRepository.findByNickname(username));
        if (account == null) { // (4)
            throw new UsernameNotFoundException(username);
        }
        return new UserAccount(account); // (5)
    }
}
```

> import 문도 생략하려고 했으나 혹시 따라서 작성하다보면 IDE가 여러 클래스를 같이 제안해주는 경우가 있어 지저분하지만(?) 남겨놓았습니다.

1. `AccountService`가 `UserDetailsService`를 구현하게 합니다. `UserDetailsService`의 구현체가 존재하고 구현체가 `Bean`으로 등록되어있을 경우 spring security 설정을 추가로 수정할 필요가 없습니다.
2. `UserDetailsService`가 제공하는 인터페이스를 재정의 합니다. 메서드 이름을 보시면 아시겠지만 username을 불러오는 방식만 구현해주면 됩니다. 회원 정보를 DB로 관리하는지, 메모리로 관리하는지, 파일로 관리하는지 알지 못하기 때문에 사용자가 존재하는지 확인하여 사용자 정보를 반환해주면 나머지는 spring security가 알아서 처리해줍니다. (참 쉽쥬?🤩)
3. 이메일 또는 닉네임이 존재하는지 확인해야 하기 때문에 두 가지 정보를 모두 확인합니다.
4. 둘 다 확인했을 때도 계정이 검색되지 않는 경우 메서드 시그니처에서 가이드하고있는 `UsernameNotFoundException`을 규격(username을 생성자로 전달)에 맞게 생성하여 던져줍니다.
5. 계정이 존재할 경우 `UserDetails` 인터페이스 구현체를 반환합니다. 이전 포스팅에서 `UserAccount` 클래스가 `UserDetails` 인터페이스를 구현하게 했으므로 해당 객체를 반환해주면 됩니다.

---

이번 포스팅을 하다가 내비게이션 바의 드랍다운 메뉴가 정상적으로 동작하지 않는다는 것을 알아냈는데 2시간이 넘는 삽질 끝에(HTML 너무 어려워어어😩) 부트스트랩 버전 때문임을 알아냈습니다.

강의에서 몇 버전을 쓰는지는 다시 확인하기 귀찮아서 찾아보진 않았지만 제가 사용하는 버전인 5버전에서는 dropdown-toggle 클래스가 dropdown-bs-toggle로 바뀌었음을 확인할 수 있었습니다.

이전 포스팅을 수정할까 하다가 이번 포스팅부터 드랍다운 메뉴를 사용할 것이기 때문에 그냥 이전 포스팅은 수정하지 않기로 하였습니다.

> 마찬가지로 justify-content-end가 제대로 동작하지 않는데 이 부분은 추가로 확인되는대로 고쳐놓을 생각입니다.

---

## Test

먼저 가입 후 로그아웃을 테스트 해보았습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/13-01.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/13-02.png)

로그아웃 버튼을 클릭했을 때 정상적으로 동작함을 확인할 수 있습니다.

다음은 로그인 기능을 테스트 해보았습니다.

먼저 로그인에 실패했을 때 경고창이 에러 문구가 정확하게 노출되는 것을 확인할 수 있었습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/13-03.png)

그리고 정확한 정보로 로그인했을 때

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/13-04.png)

로그인 처리가 되는 것을 확인할 수 있습니다.

---

이번 포스팅에서는 로그인, 로그아웃 기능을 구현한 뒤 직접 테스트를 해보았는데, 다음 포스팅에서는 테스트 코드를 통해 정상 동작하는지를 확인해보도록 하겠습니다.