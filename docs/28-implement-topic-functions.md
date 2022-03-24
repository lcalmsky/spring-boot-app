![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: efbc515)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout efbc515
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

지난 포스팅에 이어서 관심 주제 등록, 조회, 삭제 기능을 구현합니다.

## 패키지 리패터링

시작에 앞서 도메인 주도 개발 컨벤션에 맞게 기존 패키지를 수정해주겠습니다.

이전에 `Tag` Entity가 `account` 패키지 하위에 존재했었는데 `tag`라는 패키지를 생성해 해당 패키지 하위로 이동시키고 간단히 `@AllArgsConstructor`, `@Builder`를 추가해주도록 하겠습니다. 

`/src/main/java/io/lcalmsky/app/tag/domain/entity/Tag.java`

```java
package io.lcalmsky.app.tag.domain.entity;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@ToString
public class Tag {
    @Id
    @GeneratedValue
    private Long id;
    @Column(unique = true, nullable = false)
    private String title;
}

```

## Endpoint 수정

> 지난 포스팅을 모두 읽어보시고 따라오셨다면 이젠 익숙하실테니 강의나 포스팅을 보기 전에 먼저 예습해보시는 것을 추천드립니다.

SettingsController 클래스를 수정합니다.

`/src/main/java/io/lcalmsky/app/settings/controller/SettingsController.java`

```java
// 생략
public class SettingsController {
    // 생략
    static final String SETTINGS_TAGS_VIEW_NAME = "settings/tags";
    static final String SETTINGS_TAGS_URL = "/" + SETTINGS_TAGS_VIEW_NAME;
    // 생략
    private final TagRepository tagRepository;
    // 생략

    @GetMapping(SETTINGS_TAGS_URL)
    public String updateTags(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        Set<Tag> tags = accountService.getTags(account);
        model.addAttribute("tags", tags.stream()
                .map(Tag::getTitle)
                .collect(Collectors.toList()));
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

구현 순서대로 소스 코드를 따로 확인해보겠습니다.

### 조회

먼저 조회 기능은 저번 포스팅에서 관심 주제 뷰를 보여주는 부분에 추가하였습니다. 기존에 등록한 태그들이 존재하면 바로 보여줘야하기 때문입니다.

```java
@GetMapping(SETTINGS_TAGS_URL)
public String updateTags(@CurrentUser Account account, Model model) {
    model.addAttribute(account);
    Set<Tag> tags = accountService.getTags(account);
    model.addAttribute("tags", tags.stream()
            .map(Tag::getTitle)
            .collect(Collectors.toList()));
    return SETTINGS_TAGS_VIEW_NAME;
}
```

`AccountService`에 태그 조회를 위임하고, 받아온 태그를 모델에 `List<String>` 형태로 넘겨줍니다.

아직 `AccountService.getTags()` 는 구현하지 않았으므로 컴파일 에러가 발생합니다. 

### 등록

`Tag` 정보 전달을 위한 `TagForm` 클래스를 먼저 작성합니다.

`/src/main/java/io/lcalmsky/app/settings/controller/TagForm.java`

```java
package io.lcalmsky.app.settings.controller;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TagForm {
    private String tagTitle;
}
```

아주 간단하게 `tagTitle`만 전달할 수 있는 객체입니다.

> 이전 포스팅에서 뷰를 구현하면서 아래 스크립트를 추가할 때 ajax 요청을 하게 하였고, 그 때 전달하는 키 값이 tagTitle이라 일치시켰습니다.

`Tag` `Entity`를 조회해 오기 위해선 `TagRepository`가 필요합니다.

`TagRepository`를 처음에 생성한 `tag` 패키지 하위에 생성합니다.

`/src/main/java/io/lcalmsky/app/tag/infra/repository/TagRepository.java`

```java
package io.lcalmsky.app.tag.infra.repository;

import io.lcalmsky.app.tag.domain.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional(readOnly = true)
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByTitle(String title);
}
```

태그 제목으로 태그를 찾을 수 있는 메서드를 하나 가지고 있습니다.

다음으로 등록하는 메서드를 살펴보면,

```java
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
```

태그가 존재하는지 먼저 확인하고 전재하지 않는다면 `TagRepository`를 이용해 저장합니다.

그리고 계정 정보에 태그를 추가해주어야 하므로 `AccountService`에게 태그 추가를 위임합니다.

이 부분도 역시 아직 구현되지 않아 컴파일 에러가 발생합니다.

### 삭제

```java
@PostMapping(SETTINGS_TAGS_URL + "/remove")
@ResponseStatus(HttpStatus.OK)
public void removeTag(@CurrentUser Account account, @RequestBody TagForm tagForm) {
    String title = tagForm.getTagTitle();
    Tag tag = tagRepository.findByTitle(title)
            .orElseThrow(IllegalArgumentException::new);
    accountService.removeTag(account, tag);
}
```

태그가 존재하는지 확인하고 존재하지 않으면 예외를 던집니다. (존재하지 않는 태그는 삭제할 수 없기 때문)

그리고 나서 `AccountService`에게 태그 삭제를 위임합니다.

마찬가지로 아직은 컴파일 에러가 발생합니다.

---

등록, 삭제의 경우 응답 값이 필요 없으므로 `@ResponseStatus`를 이용해 정상처리 됐을 때 응답 코드만 정의하였습니다.

예외나 에러가 발생했을 때 응답코드를 매핑하는 부분은 아직 구현하지 않았습니다.

<details>
<summary>SettingsController.java 전체 보기</summary>

```java
package io.lcalmsky.app.settings.controller;

import io.lcalmsky.app.account.application.AccountService;
import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.tag.domain.entity.Tag;
import io.lcalmsky.app.account.support.CurrentUser;
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

## AccountService 수정

다음은 위에서 발생하는 컴파일에러를 해결하기 위해 `AccountService`를 수정합니다.

`/src/main/java/io/lcalmsky/app/account/application/AccountService.java`

```java
// 생략
public class AccountService implements UserDetailsService {
    // 생략
    public void addTag(Account account, Tag tag) { // (1)
        accountRepository.findById(account.getId())
                .ifPresent(a -> a.getTags().add(tag));
    }

    public Set<Tag> getTags(Account account) { // (2) 
        return accountRepository.findById(account.getId()).orElseThrow().getTags();
    }

    public void removeTag(Account account, Tag tag) { // (3)
        accountRepository.findById(account.getId())
                .map(Account::getTags)
                .ifPresent(tags -> tags.remove(tag));
    }
}
```

1. 계정 정보를 먼저 찾은뒤 계정이 존재하면 태그를 추가합니다.
2. 계정 정보를 찾은뒤 계정 정보가 존재하지 않으면 예외를 던지고, 존재하면 계정이 가진 태그를 반환합니다.
3. 계정 정보를 찾은 뒤 계정 정보가 존재하면 그 계정이 가지는 태그 정보를 가져와 전달한 태그를 삭제합니다.

<details>
<summary>AccountService.java</summary>

```java
package io.lcalmsky.app.account.application;

import io.lcalmsky.app.account.domain.UserAccount;
import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.tag.domain.entity.Tag;
import io.lcalmsky.app.account.endpoint.controller.SignUpForm;
import io.lcalmsky.app.account.infra.repository.AccountRepository;
import io.lcalmsky.app.settings.controller.NotificationForm;
import io.lcalmsky.app.settings.controller.Profile;
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

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService implements UserDetailsService {

    private final AccountRepository accountRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    public Account signUp(SignUpForm signUpForm) {
        Account newAccount = saveNewAccount(signUpForm);
        newAccount.generateToken();
        sendVerificationEmail(newAccount);
        return newAccount;
    }

    private Account saveNewAccount(SignUpForm signUpForm) {
        Account account = Account.builder()
                .email(signUpForm.getEmail())
                .nickname(signUpForm.getNickname())
                .password(passwordEncoder.encode(signUpForm.getPassword()))
                .notificationSetting(Account.NotificationSetting.builder()
                        .studyCreatedByWeb(true)
                        .studyUpdatedByWeb(true)
                        .studyRegistrationResultByWeb(true)
                        .build())
                .build();
        return accountRepository.save(account);
    }

    public void sendVerificationEmail(Account newAccount) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(newAccount.getEmail());
        mailMessage.setSubject("Webluxible 회원 가입 인증");
        mailMessage.setText(String.format("/check-email-token?token=%s&email=%s", newAccount.getEmailToken(),
                newAccount.getEmail()));
        mailSender.send(mailMessage);
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
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(account.getEmail());
        mailMessage.setSubject("[Webluxible] 로그인 링크");
        mailMessage.setText("/login-by-email?token=" + account.getEmailToken() + "&email=" + account.getEmail());
        mailSender.send(mailMessage);
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
}
```

</details>

## csrf 설정

여기까지만 구현한 뒤 애플리케이션을 실행해 테스트해보면 에러가 발생하는데 `ajax` 연동을 하면서 `csrf` 토큰을 설정해주지 않았기 때문입니다.

`tags.html` 파일을 수정해줍니다.

`/src/main/resources/templates/settings/tags.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<body class="bg-light">
<div th:replace="fragments.html :: navigation-bar"></div>
<svg th:replace="fragments.html::svg-symbols"/>
<div class="container">
    /*생략*/
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
    /*생략*/
</script>
</body>
</html>
```

## 테스트

애플리케이션 실행 후 [가입] - [프로필] - [프로필 수정] - [관심 주제] 화면까지 진입해서 스터디 주제를 몇 가지 입력해봅니다.

뷰와 로그를 보면 정상적으로 등록된 것을 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/28-01.png)

```text
2022-03-25 02:20:13.512 DEBUG 49765 --- [nio-8080-exec-3] org.hibernate.SQL                        : 
    select
        tag0_.id as id1_3_,
        tag0_.title as title2_3_ 
    from
        tag tag0_ 
    where
        tag0_.title=?
2022-03-25 02:20:13.512 TRACE 49765 --- [nio-8080-exec-3] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [VARCHAR] - [spring boot]
2022-03-25 02:20:13.520 DEBUG 49765 --- [nio-8080-exec-3] org.hibernate.SQL                        : 
    call next value for hibernate_sequence
2022-03-25 02:20:13.520 DEBUG 49765 --- [nio-8080-exec-3] org.hibernate.SQL                        : 
    insert 
    into
        tag
        (title, id) 
    values
        (?, ?)
2022-03-25 02:20:13.521 TRACE 49765 --- [nio-8080-exec-3] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [VARCHAR] - [spring boot]
2022-03-25 02:20:13.521 TRACE 49765 --- [nio-8080-exec-3] o.h.type.descriptor.sql.BasicBinder      : binding parameter [2] as [BIGINT] - [2]
2022-03-25 02:20:13.522 DEBUG 49765 --- [nio-8080-exec-3] org.hibernate.SQL                        : 
    select
        account0_.account_id as account_1_0_0_,
        account0_.created_date as created_2_0_0_,
        account0_.last_modified_date as last_mod3_0_0_,
        account0_.email as email4_0_0_,
        account0_.email_token as email_to5_0_0_,
        account0_.email_token_generated_at as email_to6_0_0_,
        account0_.is_valid as is_valid7_0_0_,
        account0_.joined_at as joined_a8_0_0_,
        account0_.nickname as nickname9_0_0_,
        account0_.study_created_by_email as study_c10_0_0_,
        account0_.study_created_by_web as study_c11_0_0_,
        account0_.study_registration_result_by_email as study_r12_0_0_,
        account0_.study_registration_result_by_web as study_r13_0_0_,
        account0_.study_updated_by_email as study_u14_0_0_,
        account0_.study_updated_by_web as study_u15_0_0_,
        account0_.password as passwor16_0_0_,
        account0_.bio as bio17_0_0_,
        account0_.company as company18_0_0_,
        account0_.image as image19_0_0_,
        account0_.job as job20_0_0_,
        account0_.location as locatio21_0_0_,
        account0_.url as url22_0_0_ 
    from
        account account0_ 
    where
        account0_.account_id=?
2022-03-25 02:20:13.522 TRACE 49765 --- [nio-8080-exec-3] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [1]
2022-03-25 02:20:13.524 DEBUG 49765 --- [nio-8080-exec-3] org.hibernate.SQL                        : 
    select
        tags0_.account_account_id as account_1_1_0_,
        tags0_.tags_id as tags_id2_1_0_,
        tag1_.id as id1_3_1_,
        tag1_.title as title2_3_1_ 
    from
        account_tags tags0_ 
    inner join
        tag tag1_ 
            on tags0_.tags_id=tag1_.id 
    where
        tags0_.account_account_id=?
2022-03-25 02:20:13.524 TRACE 49765 --- [nio-8080-exec-3] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [1]
2022-03-25 02:20:13.528 DEBUG 49765 --- [nio-8080-exec-3] org.hibernate.SQL                        : 
    insert 
    into
        account_tags
        (account_account_id, tags_id) 
    values
        (?, ?)
2022-03-25 02:20:13.529 TRACE 49765 --- [nio-8080-exec-3] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [1]
2022-03-25 02:20:13.529 TRACE 49765 --- [nio-8080-exec-3] o.h.type.descriptor.sql.BasicBinder      : binding parameter [2] as [BIGINT] - [2]
2022-03-25 02:20:15.027 DEBUG 49765 --- [nio-8080-exec-7] org.hibernate.SQL                        : 
    select
        tag0_.id as id1_3_,
        tag0_.title as title2_3_ 
    from
        tag tag0_ 
    where
        tag0_.title=?
2022-03-25 02:20:15.027 TRACE 49765 --- [nio-8080-exec-7] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [VARCHAR] - [docker]
2022-03-25 02:20:15.033 DEBUG 49765 --- [nio-8080-exec-7] org.hibernate.SQL                        : 
    call next value for hibernate_sequence
2022-03-25 02:20:15.037 DEBUG 49765 --- [nio-8080-exec-7] org.hibernate.SQL                        : 
    insert 
    into
        tag
        (title, id) 
    values
        (?, ?)
2022-03-25 02:20:15.037 TRACE 49765 --- [nio-8080-exec-7] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [VARCHAR] - [docker]
2022-03-25 02:20:15.037 TRACE 49765 --- [nio-8080-exec-7] o.h.type.descriptor.sql.BasicBinder      : binding parameter [2] as [BIGINT] - [3]
2022-03-25 02:20:15.041 DEBUG 49765 --- [nio-8080-exec-7] org.hibernate.SQL                        : 
    select
        account0_.account_id as account_1_0_0_,
        account0_.created_date as created_2_0_0_,
        account0_.last_modified_date as last_mod3_0_0_,
        account0_.email as email4_0_0_,
        account0_.email_token as email_to5_0_0_,
        account0_.email_token_generated_at as email_to6_0_0_,
        account0_.is_valid as is_valid7_0_0_,
        account0_.joined_at as joined_a8_0_0_,
        account0_.nickname as nickname9_0_0_,
        account0_.study_created_by_email as study_c10_0_0_,
        account0_.study_created_by_web as study_c11_0_0_,
        account0_.study_registration_result_by_email as study_r12_0_0_,
        account0_.study_registration_result_by_web as study_r13_0_0_,
        account0_.study_updated_by_email as study_u14_0_0_,
        account0_.study_updated_by_web as study_u15_0_0_,
        account0_.password as passwor16_0_0_,
        account0_.bio as bio17_0_0_,
        account0_.company as company18_0_0_,
        account0_.image as image19_0_0_,
        account0_.job as job20_0_0_,
        account0_.location as locatio21_0_0_,
        account0_.url as url22_0_0_ 
    from
        account account0_ 
    where
        account0_.account_id=?
2022-03-25 02:20:15.041 TRACE 49765 --- [nio-8080-exec-7] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [1]
2022-03-25 02:20:15.045 DEBUG 49765 --- [nio-8080-exec-7] org.hibernate.SQL                        : 
    select
        tags0_.account_account_id as account_1_1_0_,
        tags0_.tags_id as tags_id2_1_0_,
        tag1_.id as id1_3_1_,
        tag1_.title as title2_3_1_ 
    from
        account_tags tags0_ 
    inner join
        tag tag1_ 
            on tags0_.tags_id=tag1_.id 
    where
        tags0_.account_account_id=?
2022-03-25 02:20:15.045 TRACE 49765 --- [nio-8080-exec-7] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [1]
2022-03-25 02:20:15.048 DEBUG 49765 --- [nio-8080-exec-7] org.hibernate.SQL                        : 
    insert 
    into
        account_tags
        (account_account_id, tags_id) 
    values
        (?, ?)
2022-03-25 02:20:15.049 TRACE 49765 --- [nio-8080-exec-7] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [1]
2022-03-25 02:20:15.049 TRACE 49765 --- [nio-8080-exec-7] o.h.type.descriptor.sql.BasicBinder      : binding parameter [2] as [BIGINT] - [3]
```

다른 화면에 진입했다가 관심 주제 화면으로 다시 진입했을 때 조회 기능이 정상적으로 동작하는 것을 확인할 수 있습니다.

결과 화면이 위 캡쳐와 동일하므로 로그만 추가하겠습니다.

```text
2022-03-25 02:22:08.768 DEBUG 49765 --- [nio-8080-exec-5] org.hibernate.SQL                        : 
    select
        account0_.account_id as account_1_0_0_,
        account0_.created_date as created_2_0_0_,
        account0_.last_modified_date as last_mod3_0_0_,
        account0_.email as email4_0_0_,
        account0_.email_token as email_to5_0_0_,
        account0_.email_token_generated_at as email_to6_0_0_,
        account0_.is_valid as is_valid7_0_0_,
        account0_.joined_at as joined_a8_0_0_,
        account0_.nickname as nickname9_0_0_,
        account0_.study_created_by_email as study_c10_0_0_,
        account0_.study_created_by_web as study_c11_0_0_,
        account0_.study_registration_result_by_email as study_r12_0_0_,
        account0_.study_registration_result_by_web as study_r13_0_0_,
        account0_.study_updated_by_email as study_u14_0_0_,
        account0_.study_updated_by_web as study_u15_0_0_,
        account0_.password as passwor16_0_0_,
        account0_.bio as bio17_0_0_,
        account0_.company as company18_0_0_,
        account0_.image as image19_0_0_,
        account0_.job as job20_0_0_,
        account0_.location as locatio21_0_0_,
        account0_.url as url22_0_0_ 
    from
        account account0_ 
    where
        account0_.account_id=?
2022-03-25 02:22:08.769 TRACE 49765 --- [nio-8080-exec-5] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [1]
2022-03-25 02:22:08.773 DEBUG 49765 --- [nio-8080-exec-5] org.hibernate.SQL                        : 
    select
        tags0_.account_account_id as account_1_1_0_,
        tags0_.tags_id as tags_id2_1_0_,
        tag1_.id as id1_3_1_,
        tag1_.title as title2_3_1_ 
    from
        account_tags tags0_ 
    inner join
        tag tag1_ 
            on tags0_.tags_id=tag1_.id 
    where
        tags0_.account_account_id=?
2022-03-25 02:22:08.774 TRACE 49765 --- [nio-8080-exec-5] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [1]
```

마지막으로 태그를 삭제할 때도 뷰와 로그로 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/28-02.png)

```text
2022-03-25 02:22:39.837 DEBUG 49765 --- [nio-8080-exec-5] org.hibernate.SQL                        : 
    select
        tag0_.id as id1_3_,
        tag0_.title as title2_3_ 
    from
        tag tag0_ 
    where
        tag0_.title=?
2022-03-25 02:22:39.838 TRACE 49765 --- [nio-8080-exec-5] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [VARCHAR] - [docker]
2022-03-25 02:22:39.850 DEBUG 49765 --- [nio-8080-exec-5] org.hibernate.SQL                        : 
    select
        account0_.account_id as account_1_0_0_,
        account0_.created_date as created_2_0_0_,
        account0_.last_modified_date as last_mod3_0_0_,
        account0_.email as email4_0_0_,
        account0_.email_token as email_to5_0_0_,
        account0_.email_token_generated_at as email_to6_0_0_,
        account0_.is_valid as is_valid7_0_0_,
        account0_.joined_at as joined_a8_0_0_,
        account0_.nickname as nickname9_0_0_,
        account0_.study_created_by_email as study_c10_0_0_,
        account0_.study_created_by_web as study_c11_0_0_,
        account0_.study_registration_result_by_email as study_r12_0_0_,
        account0_.study_registration_result_by_web as study_r13_0_0_,
        account0_.study_updated_by_email as study_u14_0_0_,
        account0_.study_updated_by_web as study_u15_0_0_,
        account0_.password as passwor16_0_0_,
        account0_.bio as bio17_0_0_,
        account0_.company as company18_0_0_,
        account0_.image as image19_0_0_,
        account0_.job as job20_0_0_,
        account0_.location as locatio21_0_0_,
        account0_.url as url22_0_0_ 
    from
        account account0_ 
    where
        account0_.account_id=?
2022-03-25 02:22:39.851 TRACE 49765 --- [nio-8080-exec-5] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [1]
2022-03-25 02:22:39.856 DEBUG 49765 --- [nio-8080-exec-5] org.hibernate.SQL                        : 
    select
        tags0_.account_account_id as account_1_1_0_,
        tags0_.tags_id as tags_id2_1_0_,
        tag1_.id as id1_3_1_,
        tag1_.title as title2_3_1_ 
    from
        account_tags tags0_ 
    inner join
        tag tag1_ 
            on tags0_.tags_id=tag1_.id 
    where
        tags0_.account_account_id=?
2022-03-25 02:22:39.856 TRACE 49765 --- [nio-8080-exec-5] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [1]
2022-03-25 02:22:39.863 DEBUG 49765 --- [nio-8080-exec-5] org.hibernate.SQL                        : 
    delete 
    from
        account_tags 
    where
        account_account_id=? 
        and tags_id=?
2022-03-25 02:22:39.867 TRACE 49765 --- [nio-8080-exec-5] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [1]
2022-03-25 02:22:39.867 TRACE 49765 --- [nio-8080-exec-5] o.h.type.descriptor.sql.BasicBinder      : binding parameter [2] as [BIGINT] - [3]

```