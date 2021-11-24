![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app/tree/feature/12) 있습니다. (branch: `feature/12`)

## Overview

기존 `View` 코드의 중복된 내용을 제거합니다.

`Thymeleaf`의 `Fragment`를 사용합니다.

## Fragment

`Fragment`에 대한 자세한 내용은 [여기](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#fragments) 나와있습니다.

간략하게 설명하자면, Fragment는 마크업에서의 fragment를 표현하고 템플릿을 표현하는 또 다른 방법입니다. 템플릿을 복제하고 다른 템플릿에 파라미터로 전달하는 등의 작업을 수행할 수 있습니다.

Fragment를 사용하기 위해선 아래와 같은 attribute를 사용합니다.

* `th:fragment`: fragment 정의
* `th:insert`: fragment 삽입
* `th:replace`: fragment 대체

현재 뷰에서 사용되고 있는 중복된 코드는 메인 내비게이션 바, footer, header가 있습니다.

이를 fragment를 이용해 더 간단히 표현할 수 있습니다.

## 중복 제거

먼저 fragment를 모아둘 파일을 `templates` 디렉토리 하위에 생성합니다.

`resources/templates/fragments.html`

다음으로 `index.html`에 있는 `head`, `footer`, `nav` 부분을 복사해옵니다.

```html
<!DOCTYPE html>
<html lang="en"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head th:fragment="head">
    <meta charset="UTF-8">
    <title>Webluxible</title>
    <link rel="stylesheet" href="/node_modules/bootstrap/dist/css/bootstrap.min.css">
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
            <li class="nav-item" sec:authorize="!isAuthenticated()">
                <a class="nav-link" th:href="@{/login}">로그인</a>
            </li>
            <li class="nav-item" sec:authorize="!isAuthenticated()">
                <a class="nav-link" th:href="@{/sign-up}">가입</a>
            </li>
            <li class="nav-item" sec:authorize="isAuthenticated()">
                <a class="nav-link" th:href="@{/notifications}">알림</a>
            </li>
            <li class="nav-item" sec:authorize="isAuthenticated()">
                <a class="nav-link btn btn-outline-primary" th:href="@{/notifications}">스터디 개설</a>
            </li>
            <li class="nav-item dropdown" sec:authorize="isAuthenticated()">
                <a class="nav-link dropdown-toggle" href="#" id="userDropdown" role="button" data-toggle="dropdown"
                   aria-haspopup="true" aria-expanded="false">
                    프로필
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

</html>
```

> `branch`가 꼬였는지 이미 `fragment`를 정의해 둔 부분도 있네요 😞    
> 일부 소스 코드가 옮긴 이후 수정되었습니다.

`head`, `footer`, `nav` 태그의 `attribute`로 `th:fragment`를 사용해 각각 이름을 지정해줍니다.

이후 `index.html` 파일의 `head`, `footer`, `nav` 태그 부분을 수정해줍니다.

`resources/templates/index.html`

```html
<!DOCTYPE html>
<html lang="en"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head th:replace="fragments.html::head"></head>
<body class="bg-light">
<div th:replace="fragments.html::navigation-bar"></div>
<div class="container">
    <div class="py-5 text-center">
        <h2>Webluxible</h2>
    </div>
    <div th:replace="fragments.html::footer"></div>
</div>
<script src="/node_modules/jquery/dist/jquery.min.js"></script>
<script src="/node_modules/bootstrap/dist/js/bootstrap.bundle.min.js"></script>
<script type="application/javascript">
    (function () {

    }())
</script>
</body>
</html>
```

이후 `sign-up.html,` `email-verification.html` 파일도 마찬가지로 수정해줍니다.

<details>
<summary>sign-up.html 전체 보기</summary>

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html::head"></head>
<body class="bg-light">
<div th:replace="fragments.html::navigation-bar"></div>
<div class="container">
    <div class="py-5 text-center">
        <h2>계정 생성</h2>
    </div>
    <div class="row justify-content-center">
        <form class="needs-validation col-sm-6" action="#"
              th:action="@{/sign-up}" th:object="${signUpForm}" method="post" novalidate>
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

    <div th:replace="fragments::footer"></div>
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

</details>

<details>
<summary>email-verification.html 전체 보기</summary>

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html::head"></head>
<body class="bg-light">
<div th:replace="fragments.html::navigation-bar"></div>
<div class="py-5 text-center" th:if="${error}">
    <p class="lead">Webluxible 이메일 확인</p>
    <div class="alert alert-danger" role="alert">
        이메일 확인 링크가 정확하지 않습니다.
    </div>
</div>

<div class="py-5 text-center" th:if="${error == null}">
    <p class="lead">Webluxible 이메일 확인</p>
    <h2>
        이메일을 확인했습니다. <span th:text="${nickname}">이정민</span>님 가입을 축하합니다.
    </h2>
    <small class="text-warning"><span th:text="${numberOfUsers}">10</span>번째 회원</small><br>
    <small class="text-info">이제부터 가입할 때 사용한 이메일 또는 닉네임과 패스워드로 로그인 할 수 있습니다.</small>
</div>
<div th:replace="fragments.html::footer"></div>
</body>
</html>
```

> 기존에 누락되어있던 `footer`를 추가하였습니다.

</details>

여기서 주의할 점은, `fragments.html` 파일과 다른 디렉토리에 있는 경우에도 마찬가지로 `fragments.html::foo` 로 접근해야 한다는 점입니다.

`account` 디렉토리 안 쪽에 있는 `sign-up.html`, `email-verification.html` 파일에 작성할 때도 `../fragments.html::foo` 이런식으로 접근하시면 안 됩니다.

`thymeleaf` 템플릿을 찾을 때는 기준이 항상 `static`, `templates` 임을 잊지 마세요!!

---

이렇게 여러 페이지에 중복으로 사용되는 태그들은 `fragment`를 이용해 다른 파일로 추출한 뒤 사용될 곳에 불러와 간단히 추가할 수 있습니다.