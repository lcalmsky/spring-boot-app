![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: efbc515)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout efbc515
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

관심 주제 추가시 자동완성 기능을 구현합니다.

기존에 있는 태그 목록에서 선택할 수 있습니다.

지난 포스팅에서 사용했던 `tagify` 라이브러리의 기능을 활용합니다.

## 구현

### 엔드포인트 수정

먼저 태그를 조회해오는 시점에 기존 태그 리스트를 같이 전달해줘야 하기 때문에 `SettingsController` 클래스를 수정해줍니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/settings/controller/SettingsController.java`

```java
// 생략
public class SettingsController {
    // 생략
    private final ObjectMapper objectMapper; // (1)
    // 생략
    @GetMapping(SETTINGS_TAGS_URL)
    public String updateTags(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        Set<Tag> tags = accountService.getTags(account);
        model.addAttribute("tags", tags.stream()
                .map(Tag::getTitle)
                .collect(Collectors.toList()));
        List<String> allTags = tagRepository.findAll() // (2)
                .stream()
                .map(Tag::getTitle)
                .collect(Collectors.toList());
        String whitelist = null;
        try {
            whitelist = objectMapper.writeValueAsString(allTags); // (3)
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        model.addAttribute("whitelist", whitelist); // (4)
        return SETTINGS_TAGS_VIEW_NAME;
    }
    // 생략
}
```

1. 스프링이 자동으로 빈으로 등록해주는 `ObjectMapper`를 주입받습니다.
2. `TagRepository`에서 전체 태그 목록을 가져와 리스트로 변환합니다.
3. `List`를 `JSON` 형태로 변환합니다.
4. `whitelist`를 `model`로 전달해줍니다.

<details>
<summary>SettingsController.java 전체 보기</summary>

```java
package io.lcalmsky.app.settings.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lcalmsky.app.account.application.AccountService;
import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.support.CurrentUser;
import io.lcalmsky.app.tag.domain.entity.Tag;
import io.lcalmsky.app.tag.infra.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class SettingsController {

    static final String SETTINGS_PROFILE_VIEW_NAME = "settings/profile";
    static final String SETTINGS_PROFILE_URL = "/" + SETTINGS_PROFILE_VIEW_NAME;
    static final String SETTINGS_PASSWORD_VIEW_NAME = "settings/password";
    static final String SETTINGS_PASSWORD_URL = "/" + SETTINGS_PASSWORD_VIEW_NAME;
    static final String SETTINGS_NOTIFICATION_VIEW_NAME = "settings/notification";
    static final String SETTINGS_NOTIFICATION_URL = "/" + SETTINGS_NOTIFICATION_VIEW_NAME;
    static final String SETTINGS_ACCOUNT_VIEW_NAME = "settings/account";
    static final String SETTINGS_ACCOUNT_URL = "/" + SETTINGS_ACCOUNT_VIEW_NAME;
    static final String SETTINGS_TAGS_VIEW_NAME = "settings/tags";
    static final String SETTINGS_TAGS_URL = "/" + SETTINGS_TAGS_VIEW_NAME;

    private final AccountService accountService;
    private final PasswordFormValidator passwordFormValidator;
    private final NicknameFormValidator nicknameFormValidator;
    private final TagRepository tagRepository;
    private final ObjectMapper objectMapper;

    @InitBinder("passwordForm")
    public void passwordFormValidator(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(passwordFormValidator);
    }

    @InitBinder("nicknameForm")
    public void nicknameFormInitBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(nicknameFormValidator);
    }

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

    @GetMapping(SETTINGS_PASSWORD_URL)
    public String passwordUpdateForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(new PasswordForm());
        return SETTINGS_PASSWORD_VIEW_NAME;
    }

    @PostMapping(SETTINGS_PASSWORD_URL)
    public String updatePassword(@CurrentUser Account account, @Valid PasswordForm passwordForm, Errors errors, Model model, RedirectAttributes attributes) {
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS_PASSWORD_VIEW_NAME;
        }
        accountService.updatePassword(account, passwordForm.getNewPassword());
        attributes.addFlashAttribute("message", "패스워드를 변경했습니다.");
        return "redirect:" + SETTINGS_PASSWORD_URL;
    }

    @GetMapping(SETTINGS_NOTIFICATION_URL)
    public String notificationForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(NotificationForm.from(account));
        return SETTINGS_NOTIFICATION_VIEW_NAME;
    }

    @PostMapping(SETTINGS_NOTIFICATION_URL)
    public String updateNotification(@CurrentUser Account account, @Valid NotificationForm notificationForm, Errors errors, Model model, RedirectAttributes attributes) {
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS_NOTIFICATION_URL;
        }
        accountService.updateNotification(account, notificationForm);
        attributes.addFlashAttribute("message", "알림설정을 수정하였습니다.");
        return "redirect:" + SETTINGS_NOTIFICATION_URL;
    }

    @GetMapping(SETTINGS_ACCOUNT_URL)
    public String nicknameForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(new NicknameForm(account.getNickname()));
        return SETTINGS_ACCOUNT_VIEW_NAME;
    }

    @PostMapping(SETTINGS_ACCOUNT_URL)
    public String updateNickname(@CurrentUser Account account, @Valid NicknameForm nicknameForm, Errors errors, Model model, RedirectAttributes attributes) {
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS_ACCOUNT_VIEW_NAME;
        }
        accountService.updateNickname(account, nicknameForm.getNickname());
        attributes.addFlashAttribute("message", "닉네임을 수정하였습니다.");
        return "redirect:" + SETTINGS_ACCOUNT_URL;
    }

    @GetMapping(SETTINGS_TAGS_URL)
    public String updateTags(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        Set<Tag> tags = accountService.getTags(account);
        model.addAttribute("tags", tags.stream()
                .map(Tag::getTitle)
                .collect(Collectors.toList()));
        List<String> allTags = tagRepository.findAll()
                .stream()
                .map(Tag::getTitle)
                .collect(Collectors.toList());
        String whitelist = null;
        try {
            whitelist = objectMapper.writeValueAsString(allTags);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        model.addAttribute("whitelist", whitelist);
        return SETTINGS_TAGS_VIEW_NAME;
    }

    @PostMapping(SETTINGS_TAGS_URL + "/add")
    @ResponseStatus(HttpStatus.OK)
    public void addTag(@CurrentUser Account account, @RequestBody TagForm tagForm) {
        String title = tagForm.getTagTitle();
        Tag tag = tagRepository.findByTitle(title)
                .orElseGet(() -> tagRepository.save(Tag.builder()
                        .title(title)
                        .build()));
        accountService.addTag(account, tag);
    }

    @PostMapping(SETTINGS_TAGS_URL + "/remove")
    @ResponseStatus(HttpStatus.OK)
    public void removeTag(@CurrentUser Account account, @RequestBody TagForm tagForm) {
        String title = tagForm.getTagTitle();
        Tag tag = tagRepository.findByTitle(title)
                .orElseThrow(IllegalArgumentException::new);
        accountService.removeTag(account, tag);
    }
}
```

</details>

### 뷰 수정

tags.html 파일을 수정해줍니다.

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<body class="bg-light">
<div th:replace="fragments.html :: navigation-bar"></div>
<svg th:replace="fragments.html::svg-symbols"/>
<div class="container">
    <div class="row mt-5 justify-content-center">
        <div class="col-2" ...>
        <div class="col-8">
            <div class="row" ...>
            <div class="row">
                <div class="col-12">
                    <div class="alert alert-info" role="alert">
                        <svg th:replace="fragments.html::symbol-info"/>
                        참여하고 싶은 스터디 주제를 입력해 주세요. 해당 주제의 스터디가 생기면 알림을 받을 수 있습니다. 태그를 입력하고 쉼표 또는 엔터를 입력하세요.
                    </div>
                    <!-- 컨트롤러에서 전달한 whitelist 데이터를 받아 ID를 할당하고 hidden 처리합니다. -->
                    <div id="whitelist" th:text="${whitelist}" hidden></div>
                    <input id="tags" type="text" name="tags" th:value="${#strings.listJoin(tags, ',')}" ...>
                </div>
            </div>
        </div>
    </div>
</div>
<script src="/node_modules/@yaireo/tagify/dist/tagify.min.js"></script>
<script type="application/javascript" th:inline="javascript" ...>
<script type="application/javascript">
    $(function () {
        function tagRequest(url, tagTitle) { ... }

        function onAdd(e) { ... }

        function onRemove(e) { ...}

        let tagInput = document.querySelector("#tags");
        let tagify = new Tagify(tagInput, {
            pattern: /^.{0,20}$/,
            // 위에서 지정한 whitelist를 가져와서 tagify의 생성자 파라미터로 넘겨줍니다.
            whitelist: JSON.parse(document.querySelector("#whitelist").textContent),
            dropdown: {
                enabled: 1
            }
        });

        tagify.on("add", onAdd);
        tagify.on("remove", onRemove);

        tagify.DOM.input.classList.add('form-control');
        tagify.DOM.scope.parentNode.insertBefore(tagify.DOM.input, tagify.DOM.scope);
    });
</script>
</body>
</html>
```

소스 코드 내 inline으로 설명을 첨부하였습니다.

뚜 줄만 추가된 것으로, 생략해야되는 부분은 ... 으로 표기하였습니다.

<details>
<summary>tags.html 전체 보기</summary>

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<body class="bg-light">
<div th:replace="fragments.html :: navigation-bar"></div>
<svg th:replace="fragments.html::svg-symbols"/>
<div class="container">
    <div class="row mt-5 justify-content-center">
        <div class="col-2">
            <div th:replace="fragments.html::settings-menu (currentMenu='tags')"></div>
        </div>
        <div class="col-8">
            <div class="row">
                <h2 class="col-12">관심있는 스터디 주제</h2>
            </div>
            <div class="row">
                <div class="col-12">
                    <div class="alert alert-info" role="alert">
                        <svg th:replace="fragments.html::symbol-info"/>
                        참여하고 싶은 스터디 주제를 입력해 주세요. 해당 주제의 스터디가 생기면 알림을 받을 수 있습니다. 태그를 입력하고 쉼표 또는 엔터를 입력하세요.
                    </div>
                    <div id="whitelist" th:text="${whitelist}" hidden></div>
                    <input id="tags" type="text" name="tags" th:value="${#strings.listJoin(tags, ',')}"
                           class="tagify-outside" aria-describedby="tagHelp"/>
                </div>
            </div>
        </div>
    </div>
</div>
<script src="/node_modules/@yaireo/tagify/dist/tagify.min.js"></script>
<script type="application/javascript" th:inline="javascript">
    $(function () {
        let csrfToken = /*[[${_csrf.token}]]*/ null;
        let csrfHeader = /*[[${_csrf.headerName}]]*/ null;
        $(document).ajaxSend(function (e, xhr, options) {
            xhr.setRequestHeader(csrfHeader, csrfToken);
        });
    });
</script>
<script type="application/javascript">
    $(function () {
        function tagRequest(url, tagTitle) {
            $.ajax({
                dataType: "json",
                autocomplete: {
                    enabled: true,
                    rightKey: true
                },
                contentType: "application/json; charset=utf-8",
                method: "POST",
                url: "/settings/tags" + url,
                data: JSON.stringify({'tagTitle': tagTitle})
            }).done(function (data, status) {
                console.log("${data} and status is #{status}")
            })
        }

        function onAdd(e) {
            tagRequest("/add", e.detail.data.value);
        }

        function onRemove(e) {
            tagRequest("/remove", e.detail.data.value);
        }

        let tagInput = document.querySelector("#tags");
        let tagify = new Tagify(tagInput, {
            pattern: /^.{0,20}$/,
            whitelist: JSON.parse(document.querySelector("#whitelist").textContent),
            dropdown: {
                enabled: 1
            }
        });

        tagify.on("add", onAdd);
        tagify.on("remove", onRemove);

        tagify.DOM.input.classList.add('form-control');
        tagify.DOM.scope.parentNode.insertBefore(tagify.DOM.input, tagify.DOM.scope);
    });
</script>
</body>
</html>
```

</details>

## 테스트

애플리케이션을 실행한 후 [회원 가입] - [프로필] - [프로필 수정] - [관심 주제] 화면에 진입해 여러 개의 태그를 추가합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/29-01.png)

다시 태그를 모두 삭제한 뒤 아무 글자나 입력해보면 자동완성이 생각처럼 동작하진 않는데요, 그 이유는 관심 주제 회면에 들어올 때 tag 전체 목록을 불러오기 때문입니다.

따라서 다른 화면(알림 설정 등)에 진입했다가 다시 관심 주제 화면으로 돌아와 테스트해보면,

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/29-02.png)

이런 식으로 자동완성이 표현되는 것을 확인할 수 있습니다.