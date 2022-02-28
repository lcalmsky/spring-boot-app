![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 9c46a61)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 9c46a61
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

프로필 업데이트 중 마지막 기능으로 프로필 이미지 업데이트를 구현합니다.

## 라이브러리 설치

`/src/main/resources/static` 경로로 이동해 라이브러리를 설치해줍니다.

```shell
> cd /src/main/resources/static
> npm install cropper
> npm install jquery-cropper
```

## Profile 폼 수정

프로필 이미지를 주고받기위해 폼 클래스를 수정해줍니다.

`/src/main/java/io/lcalmsky/app/settings/controller/Profile.java`

```java
package io.lcalmsky.app.settings.controller;

import io.lcalmsky.app.account.domain.entity.Account;
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
    private String image; // 추가

    public static Profile from(Account account) {
        return new Profile(account);
    }

    protected Profile(Account account) {
        this.bio = Optional.ofNullable(account.getProfile()).map(Account.Profile::getBio).orElse(null);
        this.job = Optional.ofNullable(account.getProfile()).map(Account.Profile::getJob).orElse(null);
        this.url = Optional.ofNullable(account.getProfile()).map(Account.Profile::getUrl).orElse(null);
        this.location = Optional.ofNullable(account.getProfile()).map(Account.Profile::getLocation).orElse(null);
        this.image = Optional.ofNullable(account.getProfile()).map(Account.Profile::getImage).orElse(null); // 추가
    }
}
```

## Profile 뷰 수정

다음으로 기존 프로필 뷰를 수정해 프로필 이미지를 업로드하고 변경할 수 있게 합니다.

`/src/main/resources/templates/settings/profile.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<body class="bg-light">
<div th:replace="fragments.html :: navigation-bar"></div>
<div class="container">
    <div class="row mt-5 justify-content-center">
        <div class="col-2">
            <div th:replace="fragments.html::settings-menu (currentMenu='profile')"></div>
        </div>
        <div class="col-8">
            <div th:if="${message}" class="alert alert-info alert-dismissible fade show mt-3" role="alert">
                <span th:text="${message}">수정 완료</span>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
            <h2 class="col-sm-12" th:text="${account.nickname}">nickname</h2>
            <div class="row mt-3">
                <form class="col-sm-6" action="#" th:action="@{/settings/profile}" th:object="${profile}" method="post"
                      novalidate>
                    <div class="form-group">
                        <label for="bio">한 줄 소개</label>
                        <input id="bio" type="text" th:field="*{bio}" class="form-control"
                               placeholder="간략한 한 줄을 추가해 보세요." aria-describedby="bioHelp" required/>
                        <small id="bioHelp" class="form-text text-muted">
                            35자 이내로 입력하세요.
                        </small>
                        <small class="form-text text-danger" th:if="${#fields.hasErrors('bio')}" th:errors="*{bio}">
                            35자를 초과하였습니다.
                        </small>
                    </div>
                    <div class="form-group">
                        <label for="url">링크</label>
                        <input id="url" type="text" th:field="*{url}" class="form-control"
                               placeholder="http://www.example.com" aria-describedby="urlHelp" required/>
                        <small id="urlHelp" class="form-text text-muted">
                            블로그, GitHub 등 본인을 표현할 수 있는 링크를 추가하세요.
                        </small>
                        <small class="form-text text-danger" th:if="${#fields.hasErrors('url')}" th:errors="*{bio}">
                            올바른 URL이 아닙니다.
                        </small>
                    </div>
                    <div class="form-group">
                        <label for="job">직업</label>
                        <input id="job" type="text" th:field="*{job}" class="form-control"
                               placeholder="어떤 일을 하고 계신가요?" aria-describedby="jobHelp" required/>
                        <small id="jobHelp" class="form-text text-muted">
                            ex) 개발자, 학생, 취준생, ...
                        </small>
                    </div>
                    <div class="form-group">
                        <label for="location">활동 지역</label>
                        <input id="location" type="text" th:field="*{location}" class="form-control"
                               placeholder="서울, 경기 등" aria-describedby="locationHelp" required/>
                        <small id="locationHelp" class="form-text text-muted">
                            주요 활동 지역(사는 곳 또는 직장 위치)의 도시 이름을 입력하세요.
                        </small>
                    </div>
                    <!-- bootstrap 5에 맞게 업데이트(시작)-->
                    <div class="form-group">
                        <input id="image" type="hidden" th:field="*{image}" class="form-control"/>
                    </div>
                    <div class="form-group d-grid">
                        <button class="button btn-primary" type="submit" aria-describedby="submitHelp">수정하기</button>
                    </div>
                    <!-- bootstrap 5에 맞게 업데이트(끝)-->
                </form>
                <!-- 프로필 이미지 변경하는 부분(시작) -->
                <div class="col-sm-6">
                    <div class="card text-center">
                        <div class="card-header">
                            프로필 이미지
                        </div>
                        <div id="current-profile-image" class="mt-3">
                            <svg th:if="${#strings.isEmpty(profile.image)}" class="rounded"
                                 th:data-jdenticon-value="${account.nickname}" width="125" height="125"></svg>
                            <img th:if="${!#strings.isEmpty(profile.image)}" class="rounded"
                                 th:src="${profile.image}"
                                 width="125" height="125" alt="name" th:alt="${account.nickname}"/>
                        </div>
                        <div id="new-profile-image" class="mt-3"></div>
                        <div class="card-body">
                            <div class="input-group">
                                <input type="file" class="form-control" id="profile-image-file">
                            </div>
                            <div id="new-profile-image-control" class="mt-3 d-grid gap-2">
                                <button class="btn btn-outline-primary" id="cut-button">자르기</button>
                                <button class="btn btn-outline-success" id="confirm-button">확인</button>
                                <button class="btn btn-outline-warning" id="reset-button">취소</button>
                            </div>
                            <div id="cropped-new-profile-image" class="mt-3"></div>
                        </div>
                    </div>
                </div>
                <!-- 프로필 이미지 변경하는 부분(끝) -->
            </div>
        </div>
    </div>
</div>
<!-- 프로필 이미지 잘라내기 스크립트(시작) -->
<link href="/node_modules/cropper/dist/cropper.min.css" rel="stylesheet"/>
<script src="/node_modules/cropper/dist/cropper.min.js"></script>
<script src="/node_modules/jquery-cropper/dist/jquery-cropper.min.js"></script>
<script type="application/javascript">
    $(function () {
        cropper = '';
        let $confirmBtn = $("#confirm-button");
        let $resetBtn = $("#reset-button");
        let $cutBtn = $("#cut-button");
        let $newProfileImage = $("#new-profile-image");
        let $currentProfileImage = $("#current-profile-image");
        let $resultImage = $("#cropped-new-profile-image");
        let $profileImage = $("#image");

        $newProfileImage.hide();
        $cutBtn.hide();
        $resetBtn.hide();
        $confirmBtn.hide();

        $("#profile-image-file").change(function (e) {
            if (e.target.files.length === 1) {
                const reader = new FileReader();
                reader.onload = e => {
                    if (e.target.result) {
                        let img = document.createElement("img");
                        img.id = 'new-profile';
                        img.src = e.target.result;
                        img.width = 250;

                        $newProfileImage.html(img);
                        $newProfileImage.show();
                        $currentProfileImage.hide();

                        let $newImage = $(img);
                        $newImage.cropper({aspectRatio: 1});
                        cropper = $newImage.data('cropper');

                        $cutBtn.show();
                        $confirmBtn.hide();
                        $resetBtn.show();
                    }
                };

                reader.readAsDataURL(e.target.files[0]);
            }
        });

        $resetBtn.click(function () {
            $currentProfileImage.show();
            $newProfileImage.hide();
            $resultImage.hide();
            $resetBtn.hide();
            $cutBtn.hide();
            $confirmBtn.hide();
            $profileImage.val('');
        });

        $cutBtn.click(function () {
            let dataUrl = cropper.getCroppedCanvas().toDataURL();
            let newImage = document.createElement("img");
            newImage.id = "cropped-new-profile-image";
            newImage.src = dataUrl;
            newImage.width = 125;
            $resultImage.html(newImage);
            $resultImage.show();
            $confirmBtn.show();

            $confirmBtn.click(function () {
                $newProfileImage.html(newImage);
                $cutBtn.hide();
                $confirmBtn.hide();
                $profileImage.val(dataUrl);
            });
        });
    });
</script>
<!-- 프로필 이미지 잘라내기 스크립트(끝) -->
</body>
</html>
```

많은 부분이 수정되어 주석으로 수정한 부분을 표기하였습니다.

## 내비게이션 바 수정

프로필이 업데이트 될 때 내비게이션 프로필 이미지도 바뀔 수 있게 수정해줍니다.

내비게이션 관련 내용은 fragment로 만들었기 때문에 해당 파일을 수정해야 합니다.

`/src/main/resources/templates/fragments.html`

```html
<!-- 생략-->
<nav th:fragment="navigation-bar" class="navbar navbar-expand-sm navbar-dark bg-dark">
    <!-- 생략-->
    <div class="collapse navbar-collapse" id="navbarSupportedContent">
        <!-- 생략-->
        <ul class="navbar-nav justify-content-end">
            <!-- 생략-->
            <li class="nav-item dropdown" sec:authorize="isAuthenticated()">
                <a class="nav-link dropdown-toggle" href="#" id="userDropdown" role="button" data-bs-toggle="dropdown"
                   aria-haspopup="true" aria-expanded="false">
                    <!-- 이미지가 존재하면 이미지를, 그렇지 않으면 아바타를 보여주도록 수정(시작) -->
                    <svg th:if="${#strings.isEmpty(account?.profile?.image)}"
                         th:data-jdenticon-value="${#authentication.name}" width="24" height="24" class="rounded border bg-light"></svg><!--"프로필" 대신 아바타 이미지를 보여줌-->
                    <img th:if="${!#strings.isEmpty(account?.profile?.image)}"
                         th:src="${account.profile.image}" width="24" height="24" class="rounded border"/>
                    <!-- 이미지가 존재하면 이미지를, 그렇지 않으면 아바타를 보여주도록 수정(끝) -->
                </a>
                <!-- 생략-->
            </li>
        </ul>
    </div>
</nav>
<!--생략-->
</html>
```

<details>
<summary>fragments.html 전체 보기</summary>

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

<div th:fragment="settings-menu (currentMenu)" class="list-group">
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'profile'} ? active" href="#" th:href="@{/settings/profile}">프로필</a>
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'password'} ? active" href="#" th:href="@{/settings/password}">패스워드</a>
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'notification'} ? active" href="#" th:href="@{/settings/notification}">알림 설정</a>
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'tags'} ? active" href="#" th:href="@{/settings/tags}">관심 주제</a>
    <a class="list-group-item list-group-item-action" th:classappend="${currentMenu == 'zones'} ? active" href="#" th:href="@{/settings/zones}">활동 지역</a>
</div>

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
                    <!-- 이미지가 존재하면 이미지를, 그렇지 않으면 아바타를 보여주도록 수정(시작) -->
                    <svg th:if="${#strings.isEmpty(account?.profile?.image)}"
                         th:data-jdenticon-value="${#authentication.name}" width="24" height="24" class="rounded border bg-light"></svg><!--"프로필" 대신 아바타 이미지를 보여줌-->
                    <img th:if="${!#strings.isEmpty(account?.profile?.image)}"
                         th:src="${account.profile.image}" width="24" height="24" class="rounded border"/>
                    <!-- 이미지가 존재하면 이미지를, 그렇지 않으면 아바타를 보여주도록 수정(끝) -->
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

## 계정 정보 업데이트 기능 수정

벡엔드에서도 프로필 이미지에 해당하는 값을 받았을 때 DB에 저장해줘야 하므로 해당 부분을 수정해줍니다.

`/src/main/java/io/lcalmsky/app/account/domain/entity/Account.java`

```java
// 생략
public class Account extends AuditingEntity {
    // 생략
    public void updateProfile(io.lcalmsky.app.settings.controller.Profile profile) {
        if (this.profile == null) {
            this.profile = new Profile();
        }
        this.profile.bio = profile.getBio();
        this.profile.url = profile.getUrl();
        this.profile.job = profile.getJob();
        this.profile.location = profile.getLocation();
        this.profile.image = profile.getImage(); // 업데이트 시 이미지 필드에 값을 할당해줍니다.
    }
    // 생략
}
```

## 테스트

애플리케이션을 실행하고 가입한 뒤 `프로필 수정`에 진입하면 다음과 같은 화면이 노출됩니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/21-01.png)

파일을 선택하면 이미지를 조절할 수 있는 창이 나타나고, 줌인/줌아웃 및 crop 할 위치를 이동시킨 뒤 자르기 기능을 사용할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/21-02.png)

자르기 버튼을 누르면 확인 버튼이 아래 처럼 활성화되고

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/21-03.png)

확인 후 수정하기 버튼을 누르면

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/21-04.png)

아래와 같이 내비게이션 바와 프로필 이미지가 업데이트 된 것을 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/21-05.png)