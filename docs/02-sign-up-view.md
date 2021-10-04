![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app/) 있습니다. (branch: feature/4)

## Overview

* `Bootstrap`, `Thymeleaf`, `HTML`, `CSS`를 사용하여 회원 가입 페이지를 작성하고 요청시 보여줍니다.
* 회원 가입시 받을 수 있는 정보를 폼 객체로 제공합니다.

## Prerequisite

### 부트스트랩(Bootstrap) 설정

[comment]: <> (![Quick Start]&#40;https://getbootstrap.com/docs/5.1/getting-started/introduction/&#41;)

```html
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.1/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-F3w7mX95PdgyTmZZMECAngseQB83DfGTowi0iMjiWaeVhAn4FJkqJByhZMI3AhiU" crossorigin="anonymous">
```

위 값을 복사해서 `sign-up.html` 파일의 `head` 태그 안쪽에 추가하고 `title` 태그 안쪽의 값을 원하는 제목으로 수정합니다.

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Sign up</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.1/dist/css/bootstrap.min.css" rel="stylesheet"
          integrity="sha384-F3w7mX95PdgyTmZZMECAngseQB83DfGTowi0iMjiWaeVhAn4FJkqJByhZMI3AhiU" crossorigin="anonymous">
</head>
... 생략
```

그리고 body 태그 안쪽에 아래 스크립트를 추가합니다.

```html
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.1/dist/js/bootstrap.bundle.min.js"
        integrity="sha384-/bQdsTh/da6pkI1MST/rWKFNjaCP5gBSY4sEBT38Q/9RBh9AH40zEOg7Hlq2THRZ"
        crossorigin="anonymous"></script>
```

위 스크립트는 부트스트랩에서 필요로하는 JS function 들을 사용할 수 있게 해줍니다. 예전에는 여러 개로 나눠져 있었지만 이제 bundle 형태로 제공해 조금 더 편리합니다.

만약에 일부 기능만 사용하실 거라면 아래 스크립트를 선택적으로 사용하셔도 됩니다.

```html
<script src="https://cdn.jsdelivr.net/npm/@popperjs/core@2.9.3/dist/umd/popper.min.js"
        integrity="sha384-W8fXfP3gkOKtndU4JGtKDvXbO53Wy8SZCQHczT5FMiiqmQfUpWbYdTil/SxwZgAN"
        crossorigin="anonymous"></script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.1/dist/js/bootstrap.min.js"
        integrity="sha384-skAcpIdS7UcVUC05LJ9Dxay8AXcDYfBJqt1CJ85S/CFujBsIzCIv+l9liuYLaMQ/"
        crossorigin="anonymous"></script>
```

저는 간단하게 bundle 형태로 sign-up.html 파일에 추가하였습니다.

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.1/dist/css/bootstrap.min.css" rel="stylesheet"
          integrity="sha384-F3w7mX95PdgyTmZZMECAngseQB83DfGTowi0iMjiWaeVhAn4FJkqJByhZMI3AhiU" crossorigin="anonymous">
</head>
<body>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.1/dist/js/bootstrap.bundle.min.js"
        integrity="sha384-/bQdsTh/da6pkI1MST/rWKFNjaCP5gBSY4sEBT38Q/9RBh9AH40zEOg7Hlq2THRZ"
        crossorigin="anonymous"></script>
</body>
</html>
```

### 로고 준비

페이지에 노출될 로고를 만들어줍니다. 각자 편하신 방법으로 만드시면 됩니다.

저는 Webluxible 이라는 스터디 그룹에 속해있어서 로고를 Webluxible을 이용해 만들었습니다.

## Implementation

### 페이지 작성

저는 HTML, CSS, Thymeleaf를 모두 잘 몰라서 소스 코드 복붙으로 대신하려고 합니다.

`src/main/resources/static/images/logo.png` 에 로고 파일을 추가한 뒤, 

`src/main/resources/templates/account/sign-up.html` 파일을 아래와같이 작성합니다.

```html
<!DOCTYPE html>
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Sign up</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.1/dist/css/bootstrap.min.css" rel="stylesheet"
          integrity="sha384-F3w7mX95PdgyTmZZMECAngseQB83DfGTowi0iMjiWaeVhAn4FJkqJByhZMI3AhiU" crossorigin="anonymous">
    <style>
        .container {
            max-width: 100%;
        }
    </style>
</head>
<body class="bg-light">
<nav class="navbar navbar-expand-sm navbar-dark bg-dark">
    <a class="navbar-brand" href="/" th:href="@{/}">
        <img src="images/logo.png" width="30" height="30" alt="webluxible" style="margin-left: 10px">
    </a>
    <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarSupportedContent"
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
            <li class="nav-item">
                <a class="nav-link" href="#" th:href="@{/login}">로그인</a>
            </li>
            <li class="nav-item">
                <a class="nav-link" href="#" th:href="@{/signup}">가입</a>
            </li>
        </ul>
    </div>
</nav>

<div class="container">
    <div class="py-5 text-center">
        <h2>계정 생성</h2>
    </div>
    <div class="row justify-content-center">
        <form class="needs-validation col-sm-6" action="#"
              th:action="@{/signup}" th:object="${signUpForm}" method="post" novalidate>
            <div class="form-group">
                <label for="nickname">닉네임</label>
                <input id="nickname" type="text" th:field="*{nickname}" class="form-control"
                       placeholder="ex) jaime" aria-describedby="nicknameHelp" required minlength="3" maxlength="20">
                <small id="nicknameHelp" class="form-text text-muted">
                    공백없이 문자와 숫자로만 3자 이상 20자 이내로 입력하세요. 가입후에 변경할 수 있습니다.
                </small>
                <small class="invalid-feedback">닉네임을 입력하세요.</small>
                <small class="form-text text-danger" th:if="${#fields.hasErrors('nickname')}" th:errors="*{nickname}">Nickname
                    Error</small>
            </div>

            <div class="form-group">
                <label for="email">이메일</label>
                <input id="email" type="email" th:field="*{email}" class="form-control"
                       placeholder="ex) abc@example.com" aria-describedby="emailHelp" required>
                <small id="emailHelp" class="form-text text-muted">
                    Webluxible은 사용자의 이메일을 공개하지 않습니다.
                </small>
                <small class="invalid-feedback">이메일을 입력하세요.</small>
                <small class="form-text text-danger" th:if="${#fields.hasErrors('email')}" th:errors="*{email}">Email
                    Error</small>
            </div>

            <div class="form-group">
                <label for="password">패스워드</label>
                <input id="password" type="password" th:field="*{password}" class="form-control"
                       aria-describedby="passwordHelp" required minlength="8" maxlength="50">
                <small id="passwordHelp" class="form-text text-muted">
                    8자 이상 50자 이내로 입력하세요. 영문자, 숫자, 특수기호를 사용할 수 있으며 공백은 사용할 수 없습니다.
                </small>
                <small class="invalid-feedback">패스워드를 입력하세요.</small>
                <small class="form-text text-danger" th:if="${#fields.hasErrors('password')}" th:errors="*{password}">Password
                    Error</small>
            </div>

            <div class="form-group">
                <button class="btn btn-primary btn-block" type="submit"
                        aria-describedby="submitHelp">가입하기
                </button>
                <small id="submitHelp" class="form-text text-muted">
                    <a href="#">약관</a>에 동의하시면 가입하기 버튼을 클릭하세요.
                </small>
            </div>
        </form>
    </div>

    <footer th:fragment="footer">
        <div class="row justify-content-center">
            <small class="d-flex mb-3 text-muted" style="justify-content: center">Webluxible &copy; 2021</small>
        </div>
    </footer>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.1/dist/js/bootstrap.bundle.min.js"
        integrity="sha384-/bQdsTh/da6pkI1MST/rWKFNjaCP5gBSY4sEBT38Q/9RBh9AH40zEOg7Hlq2THRZ"
        crossorigin="anonymous"></script>
<script type="application/javascript">
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
</body>
</html>
```

스프링 부트는 기본적으로 `static resource`를 저장할 수 있는 경로를 가지고 있습니다.

* classpath:/META-INF/resources/
* classpath:/resources/
* classpath:/static/
* classpath:/public/

`static resource`는 말 그대로 정적인 자원으로 요청이 들어왔을 때 바로 응답할 수 있게 준비되어있는 자원이라고 생각하시면 됩니다.

보통 이미지 파일이나 템플릿 파일들이 이에 해당하므로 해당 위치에 필요한 자원들을 저장하시면 됩니다.

위 경로들 외에 다른 경로를 `static`으로 지정하고 싶으시다면 `application.yml` 파일을 수정해주셔야 합니다.

```yaml
spring:
  web:
    resources:
      static-locations:
        - classpath:foo
```

### 컨트롤러 수정

이제 페이지가 동작할 수 있도록 컨트롤러를 수정해보겠습니다.

`src/main/java/io/lcalmsky/server/account/endpoint/controller/AccountController.java`

```java
package io.lcalmsky.server.account.endpoint.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccountController {

    @GetMapping("/sign-up")
    public String signUpForm(Model model) {
        model.addAttribute(new SignUpForm()); // (1)
        return "account/sign-up";
    }
}
```

(1) `attribute`를 추가할 때 클래스의 `camel-case`와 동일한 키를 사용하는 경우 키를 별도로 지정할 필요가 없습니다.

브라우저에 `/sign-up`을 입력했을 때 해당 컨트롤러가 호출되고 `SignUpForm`이라는 객체를 생성해 전달해준 뒤 `account/sign-up` 페이지로 `redirect` 해줍니다.

객체를 전달해주기 위해 해당 클래스를 작성해보겠습니다.

`src/main/java/io/lcalmsky/server/account/endpoint/controller/SignUpForm.java`

```java
package io.lcalmsky.server.account.endpoint.controller;

import lombok.Data;

@Data
public class SignUpForm {
    private String nickname;
    private String email;
    private String password;
}
```

이제 애플리케이션을 실행시켜 `http://localhost:8080/sign-up` 으로 접속해볼까요?

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/02-01.png)

페이지가 잘 노출되는 거 같지만 로고 이미지가 깨져있습니다.

이전 포스팅에서 설정한 `SecurityConfig` 때문인데요, `static resource`를 접근할 때 인증하지 않아도 되도록 설정을 추가해줘야 합니다.

`src/main/java/io/lcalmsky/server/config/SecurityConfig.java`

```java
package io.lcalmsky.server.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .mvcMatchers("/", "/login", "/sign-up", "/check-email", "/check-email-token",
                        "/email-login", "/check-email-login", "/login-link").permitAll()
                .mvcMatchers(HttpMethod.GET, "/profile/*").permitAll()
                .anyRequest().authenticated();
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring() // (1)
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations());
    }
}
```

(1) `static resource`에 해당하는 위치에 대해 인증을 무시하도록 설정합니다.

이후 다시 실행해보면,

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/02-02.png)

로고가 잘 출력되는 것을 확인할 수 있습니다.

## Test

이미 브라우저를 이용해 페이지를 확인했지만 기존에 작성한 테스트 코드도 수정해보겠습니다.

`src/test/java/io/lcalmsky/server/account/endpoint/controller/AccountControllerTest.java`

```java
package io.lcalmsky.server.account.endpoint.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {
    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("회원 가입 화면 진입 확인")
    void signUpForm() throws Exception {
        mockMvc.perform(get("/sign-up"))
                .andDo(print())
                .andExpect(status().isOk()) // (1)
                .andExpect(view().name("account/sign-up")) // (2)
                .andExpect(model().attributeExists("signUpForm")); // (3)
    }
}
```

(1) `HTTP Status`가 `200 OK`인지 확인합니다.  
(2) `view`가 제대로 이동했는지 확인합니다.  
(3) 객체로 전달했던 `attribute`가 존재하는지 확인합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/02-03.png)

테스트가 정상적으로 수행된 것을 확인할 수 있습니다.

---

다음 포스팅에서는 회원 가입 폼에 입력된 값을 검증하고 전달하는 과정을 다뤄보겠습니다.