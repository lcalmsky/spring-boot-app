![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 5adddca)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 5adddca
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

지역 관련 기능을 구현하고 테스트합니다.

## 엔드포인트 수정

SettingsController에 지역 관련 엔드포인트를 추가합니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/settings/controller/SettingsController.java`

```java
// 생략
@Controller
@RequiredArgsConstructor
public class SettingsController {
    // 생략
    static final String SETTINGS_ZONE_VIEW_NAME = "settings/zones";
    static final String SETTINGS_ZONE_URL = "/" + SETTINGS_ZONE_VIEW_NAME;

    private final AccountService accountService;
    // 생략
    private final ZoneRepository zoneRepository;
    private final ObjectMapper objectMapper;
    
    // 생략
    @GetMapping(SETTINGS_ZONE_URL)
    public String updateZonesForm(@CurrentUser Account account, Model model) throws JsonProcessingException {
        model.addAttribute(account);
        Set<Zone> zones = accountService.getZones(account);
        model.addAttribute("zones", zones.stream()
                .map(Zone::toString)
                .collect(Collectors.toList()));
        List<String> allZones = zoneRepository.findAll().stream()
                .map(Zone::toString)
                .collect(Collectors.toList());
        model.addAttribute("whitelist", objectMapper.writeValueAsString(allZones));
        return SETTINGS_ZONE_VIEW_NAME;
    }

    @PostMapping(SETTINGS_ZONE_URL + "/add")
    @ResponseStatus(HttpStatus.OK)
    public void addZone(@CurrentUser Account account, @RequestBody ZoneForm zoneForm) {
        Zone zone = zoneRepository.findByCityAndProvince(zoneForm.getCityName(), zoneForm.getProvinceName())
                .orElseThrow(IllegalArgumentException::new);
        accountService.addZone(account, zone);
    }

    @PostMapping(SETTINGS_ZONE_URL + "/remove")
    @ResponseStatus(HttpStatus.OK)
    public void removeZone(@CurrentUser Account account, @RequestBody ZoneForm zoneForm) {
        Zone zone = zoneRepository.findByCityAndProvince(zoneForm.getCityName(), zoneForm.getProvinceName())
                .orElseThrow(IllegalArgumentException::new);
        accountService.removeZone(account, zone);
    }
}
```

여태까지 개발했던 것들과 동일하기 때문에 자세한 설명은 생략하겠습니다.

특히 이전 포스팅에서 다뤘던 관심 도메인 부분과 매우 유사합니다.

<details>
<summary>SettingsController.java 전체 보기</summary>

```java
package io.lcalmsky.app.settings.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lcalmsky.app.account.application.AccountService;
import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.domain.entity.Zone;
import io.lcalmsky.app.account.support.CurrentUser;
import io.lcalmsky.app.tag.domain.entity.Tag;
import io.lcalmsky.app.tag.infra.repository.TagRepository;
import io.lcalmsky.app.zone.infra.repository.ZoneRepository;
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
    static final String SETTINGS_ZONE_VIEW_NAME = "settings/zones";
    static final String SETTINGS_ZONE_URL = "/" + SETTINGS_ZONE_VIEW_NAME;

    private final AccountService accountService;
    private final PasswordFormValidator passwordFormValidator;
    private final NicknameFormValidator nicknameFormValidator;
    private final TagRepository tagRepository;
    private final ZoneRepository zoneRepository;
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

    @GetMapping(SETTINGS_ZONE_URL)
    public String updateZonesForm(@CurrentUser Account account, Model model) throws JsonProcessingException {
        model.addAttribute(account);
        Set<Zone> zones = accountService.getZones(account);
        model.addAttribute("zones", zones.stream()
                .map(Zone::toString)
                .collect(Collectors.toList()));
        List<String> allZones = zoneRepository.findAll().stream()
                .map(Zone::toString)
                .collect(Collectors.toList());
        model.addAttribute("whitelist", objectMapper.writeValueAsString(allZones));
        return SETTINGS_ZONE_VIEW_NAME;
    }

    @PostMapping(SETTINGS_ZONE_URL + "/add")
    @ResponseStatus(HttpStatus.OK)
    public void addZone(@CurrentUser Account account, @RequestBody ZoneForm zoneForm) {
        Zone zone = zoneRepository.findByCityAndProvince(zoneForm.getCityName(), zoneForm.getProvinceName())
                .orElseThrow(IllegalArgumentException::new);
        accountService.addZone(account, zone);
    }

    @PostMapping(SETTINGS_ZONE_URL + "/remove")
    @ResponseStatus(HttpStatus.OK)
    public void removeZone(@CurrentUser Account account, @RequestBody ZoneForm zoneForm) {
        Zone zone = zoneRepository.findByCityAndProvince(zoneForm.getCityName(), zoneForm.getProvinceName())
                .orElseThrow(IllegalArgumentException::new);
        accountService.removeZone(account, zone);
    }
}
```

</details>

일단 저렇게 작성했을 경우 많은 부분에서 컴파일 에러가 발생할텐데요, 하나씩 수정해보도록 하겠습니다.

## 폼 클래스 작성

지역 정보를 주고 받을 폼 클래스를 생성합니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/settings/controller/ZoneForm.java`

```java
package io.lcalmsky.app.settings.controller;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ZoneForm {

    private String zoneName;

    public String getCityName() {
        return zoneName.substring(0, zoneName.indexOf("("));
    }

    public String getProvinceName() {
        return zoneName.substring(zoneName.indexOf("/") + 1);
    }
}
```

실제로 입력은 zoneName 하나의 필드로 받아 cityName, provinceName을 substring을 통해 획득할 수 있게 하였습니다.

## Account Entity 수정

이전 도메인 설계 때 했어야 하는 부분인데 누락되었네요.

Account와 Zone의 관계 설정을 해줍니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/account/domain/entity/Account.java`

```java
// 생략
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder @Getter @ToString
public class Account extends AuditingEntity {
    // 생략
    @ManyToMany @ToString.Exclude
    private Set<Zone> zones = new HashSet<>();
}
```

<details>
<summary>Account.java 전체 보기</summary>

```java
package io.lcalmsky.app.account.domain.entity;

import io.lcalmsky.app.domain.entity.AuditingEntity;
import io.lcalmsky.app.settings.controller.NotificationForm;
import io.lcalmsky.app.tag.domain.entity.Tag;
import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder @Getter @ToString
public class Account extends AuditingEntity {

    @Id @GeneratedValue
    @Column(name = "account_id")
    private Long id;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String nickname;

    private String password;

    private boolean isValid;

    private String emailToken;

    private LocalDateTime joinedAt;

    @Embedded
    private Profile profile = new Profile();

    @Embedded
    private NotificationSetting notificationSetting = new NotificationSetting();

    private LocalDateTime emailTokenGeneratedAt;

    @ManyToMany @ToString.Exclude
    private Set<Tag> tags = new HashSet<>();

    @ManyToMany @ToString.Exclude
    private Set<Zone> zones = new HashSet<>();

    public static Account with(String email, String nickname, String password) {
        Account account = new Account();
        account.email = email;
        account.nickname = nickname;
        account.password = password;
        return account;
    }

    public void generateToken() {
        this.emailToken = UUID.randomUUID().toString();
        this.emailTokenGeneratedAt = LocalDateTime.now();
    }

    public boolean enableToSendEmail() {
        return this.emailTokenGeneratedAt.isBefore(LocalDateTime.now().minusMinutes(5));
    }

    public void verified() {
        this.isValid = true;
        joinedAt = LocalDateTime.now();
    }

    @PostLoad
    private void init() {
        if (profile == null) {
            profile = new Profile();
        }
        if (notificationSetting == null) {
            notificationSetting = new NotificationSetting();
        }
    }

    public void updateProfile(io.lcalmsky.app.settings.controller.Profile profile) {
        if (this.profile == null) {
            this.profile = new Profile();
        }
        this.profile.bio = profile.getBio();
        this.profile.url = profile.getUrl();
        this.profile.job = profile.getJob();
        this.profile.location = profile.getLocation();
        this.profile.image = profile.getImage();
    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    public void updateNotification(NotificationForm notificationForm) {
        this.notificationSetting.studyCreatedByEmail = notificationForm.isStudyCreatedByEmail();
        this.notificationSetting.studyCreatedByWeb = notificationForm.isStudyCreatedByWeb();
        this.notificationSetting.studyUpdatedByWeb = notificationForm.isStudyUpdatedByWeb();
        this.notificationSetting.studyUpdatedByEmail = notificationForm.isStudyUpdatedByEmail();
        this.notificationSetting.studyRegistrationResultByEmail = notificationForm.isStudyRegistrationResultByEmail();
        this.notificationSetting.studyRegistrationResultByWeb = notificationForm.isStudyRegistrationResultByWeb();
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public boolean isValid(String token) {
        return this.emailToken.equals(token);
    }

    @Embeddable
    @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder @Getter @ToString
    public static class Profile {
        private String bio;
        private String url;
        private String job;
        private String location;
        private String company;

        @Lob @Basic(fetch = FetchType.EAGER)
        private String image;
    }

    @Embeddable
    @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder @Getter @ToString
    public static class NotificationSetting {
        private boolean studyCreatedByEmail = false;
        private boolean studyCreatedByWeb = true;
        private boolean studyRegistrationResultByEmail = false;
        private boolean studyRegistrationResultByWeb = true;
        private boolean studyUpdatedByEmail = false;
        private boolean studyUpdatedByWeb = true;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        Account account = (Account) o;
        return id != null && Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
```

</details>

## AccountService 수정

지역 정보를 조회하고 추가하고 삭제하기 위한 기능을 추가합니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/account/application/AccountService.java`

```java
// 생략
@Service
@RequiredArgsConstructor
@Transactional
public class AccountService implements UserDetailsService {

    // 생략
    private final AccountRepository accountRepository;
    // 생략
    public Set<Zone> getZones(Account account) {
        return accountRepository.findById(account.getId())
                .orElseThrow()
                .getZones();
    }

    public void addZone(Account account, Zone zone) {
        accountRepository.findById(account.getId())
                .ifPresent(a -> a.getZones().add(zone));
    }

    public void removeZone(Account account, Zone zone) {
        accountRepository.findById(account.getId())
                .ifPresent(a -> a.getZones().remove(zone));
    }
}
```

<details>
<summary>AccountService.java 전체 보기</summary>

```java
package io.lcalmsky.app.account.application;

import io.lcalmsky.app.account.domain.UserAccount;
import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.domain.entity.Zone;
import io.lcalmsky.app.account.endpoint.controller.SignUpForm;
import io.lcalmsky.app.account.infra.repository.AccountRepository;
import io.lcalmsky.app.settings.controller.NotificationForm;
import io.lcalmsky.app.settings.controller.Profile;
import io.lcalmsky.app.tag.domain.entity.Tag;
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
        sendVerificationEmail(newAccount);
        return newAccount;
    }

    private Account saveNewAccount(SignUpForm signUpForm) {
        Account account = Account.with(signUpForm.getEmail(), signUpForm.getNickname(), passwordEncoder.encode(signUpForm.getPassword()));
        account.generateToken();
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

    public Set<Zone> getZones(Account account) {
        return accountRepository.findById(account.getId())
                .orElseThrow()
                .getZones();
    }

    public void addZone(Account account, Zone zone) {
        accountRepository.findById(account.getId())
                .ifPresent(a -> a.getZones().add(zone));
    }

    public void removeZone(Account account, Zone zone) {
        accountRepository.findById(account.getId())
                .ifPresent(a -> a.getZones().remove(zone));
    }
}

```

</details>

## Zone Entity 수정

@Builder와 생성자, toString을 추가로 구현합니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/account/domain/entity/Zone.java`

```java
package io.lcalmsky.app.account.domain.entity;

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
public class Zone {
 
    // 생략
    @Override
    public String toString() {
        return String.format("%s(%s)/%s", city, localNameOfCity, province);
    }
}
```

<details>
<summary>Zone.java 전체 보기</summary>

```java
package io.lcalmsky.app.account.domain.entity;

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
public class Zone {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String localNameOfCity;

    private String province;

    public static Zone map(String line) {
        String[] split = line.split(",");
        Zone zone = new Zone();
        zone.city = split[0];
        zone.localNameOfCity = split[1];
        zone.province = split[2];
        return zone;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)/%s", city, localNameOfCity, province);
    }
}
```

</details>

## ZoneRepository 수정

시와 도로 지역을 찾을 수 있는 메서드를 추가합니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/zone/infra/repository/ZoneRepository.java`

```java
package io.lcalmsky.app.zone.infra.repository;

import io.lcalmsky.app.account.domain.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ZoneRepository extends JpaRepository<Zone, Long> {

    Optional<Zone> findByCityAndProvince(String cityName, String provinceName);
}

```

## 뷰 작성

tags.html을 복사하여 zones.html을 생성합니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/resources/templates/settings/zones.html`

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
            <div th:replace="fragments.html::settings-menu (currentMenu='zones')"></div>
        </div>
        <div class="col-8">
            <div class="row">
                <h2 class="col-12">주요 활동 지역</h2>
            </div>
            <div class="row">
                <div class="col-12">
                    <div class="alert alert-info" role="alert">
                        <svg th:replace="fragments.html::symbol-info"/>
                        스터디를 참가할 수 있는 지역을 등록하세요. 해당 지역에 스터디가 등록되면 알림을 받을 수 있습니다. 시스템에 등록된 지역 외에는 등록되지 않습니다. 반드시 자동완성을 통해 입력해주세요.
                    </div>
                    <div id="whitelist" th:text="${whitelist}" hidden></div>
                    <input id="tags" type="text" name="tags" th:value="${#strings.listJoin(zones, ',')}"
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
        function tagRequest(url, zoneName) {
            $.ajax({
                dataType: "json",
                autocomplete: {
                    enabled: true,
                    rightKey: true
                },
                contentType: "application/json; charset=utf-8",
                method: "POST",
                url: "/settings/zones" + url,
                data: JSON.stringify({'zoneName': zoneName})
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

tags.html에서 일부만 수정하면 간단히 구현할 수 있습니다.

## 테스트

애플리케이션 실행 후 회원 가입 - 프로필 - 프로필 수정 - 활동 지역으로 진입합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/32-01.png)

자동완성을 이용해 지역을 등록합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/32-02.png)

관심 주제나 다른 메뉴를 클릭했다가 활동 지역을 다시 클릭해도 해당 지역들이 다시 노출되는 것을 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/32-03.png)

x 버튼을 눌러 관심 지역을 삭제합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/32-04.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/32-05.png)

정상 동작을 모두 확인하였습니다.

## 테스트 코드 작성

SettingsControllerTest 클래스에 추가된 기능에 대한 테스트를 작성합니다.

`/Users/jaime/git-repo/spring-boot-app/src/test/java/io/lcalmsky/app/settings/controller/SettingsControllerTest.java`

```java
// 생략
@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class SettingsControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired AccountRepository accountRepository;
    @Autowired ZoneRepository zoneRepository;
    @Autowired TagRepository tagRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;
    @Autowired AccountService accountService;

    @AfterEach
    void afterEach() {
        accountRepository.deleteAll();
        zoneRepository.deleteAll();
    }

    // 생략
    @Test
    @DisplayName("계정의 지역 정보 수정 폼")
    @WithAccount("jaime")
    void updateZonesForm() throws Exception {
        mockMvc.perform(get(SettingsController.SETTINGS_ZONE_URL))
                .andExpect(view().name(SettingsController.SETTINGS_ZONE_VIEW_NAME))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("whitelist"))
                .andExpect(model().attributeExists("zones"));
    }

    @Test
    @DisplayName("계정의 지역 정보 추가")
    @WithAccount("jaime")
    void addZone() throws Exception {
        Zone testZone = Zone.builder().city("test").localNameOfCity("테스트시").province("테스트주").build();
        zoneRepository.save(testZone);
        ZoneForm zoneForm = new ZoneForm();
        zoneForm.setZoneName(testZone.toString());
        mockMvc.perform(post(SettingsController.SETTINGS_ZONE_URL + "/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zoneForm))
                        .with(csrf()))
                .andExpect(status().isOk());
        Account account = accountRepository.findByNickname("jaime");
        assertTrue(account.getZones().contains(testZone));
    }

    @Test
    @DisplayName("계정의 지역 정보 삭제")
    @WithAccount("jaime")
    void removeZone() throws Exception {
        Account jaime = accountRepository.findByNickname("jaime");
        Zone testZone = Zone.builder().city("test").localNameOfCity("테스트시").province("테스트주").build();
        zoneRepository.save(testZone);
        accountService.addZone(jaime, testZone);
        assertTrue(jaime.getZones().contains(testZone));
        ZoneForm zoneForm = new ZoneForm();
        zoneForm.setZoneName(testZone.toString());
        mockMvc.perform(post(SettingsController.SETTINGS_ZONE_URL + "/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zoneForm))
                        .with(csrf()))
                .andExpect(status().isOk());
        assertFalse(jaime.getZones().contains(testZone));
    }
}
```

afterEach 메서드에서 매 테스트 종료 후 zoneRepository를 clear 해주도록 하였습니다.

나머지 뷰 조회, 지역 정보 추가, 지역 정보 삭제는 이전 포스팅에서 다뤘던 태그와 매우 유사하기 때문에 설명은 생략하도록 하겠습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/32-06.png)

모두 성공하였습니다!

---

다음 포스팅에서는 이전에 로그 출력으로 대체했던 메일 전송 기능을 제대로 구현하기 위해 SMTP 설정을 해보도록 하겠습니다.