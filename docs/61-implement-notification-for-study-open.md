![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: dc5c662)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout dc5c662
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

스터디 생성 후 공개할 때 알림을 전송합니다.

* 알림 받을 대상: 스터디 주제와 지역에 매칭되는 사용자
* 알림 제목: 스터디 이름
* 알림 메시지: 스터디 짧은 소개

## 서비스 수정

지난 번에 스터디가 생성될 때 이벤트를 발생시켰던 부분을 스터디가 공개될 때 이벤트를 발생시키도록 수정합니다.

`/src/main/java/io/lcalmsky/app/modules/study/application/StudyService.java`

```java
// 생략
@Service
@RequiredArgsConstructor
@Transactional
public class StudyService {
    private final StudyRepository studyRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Study createNewStudy(StudyForm studyForm, Account account) {
        Study study = Study.from(studyForm);
        study.addManager(account);
        return studyRepository.save(study);
    }
    // 생략
    public void publish(Study study) {
        study.publish();
        eventPublisher.publishEvent(new StudyCreatedEvent(study));
    }
    // 생략
}
```

`eventPublisher.publishEvent` 하는 부분을 `createNewStudy`에서 `publish`로 옮겨주었습니다.

<details>
<summary>StudyService.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.study.application;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.domain.entity.Zone;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.endpoint.form.StudyDescriptionForm;
import io.lcalmsky.app.modules.study.endpoint.form.StudyForm;
import io.lcalmsky.app.modules.study.event.StudyCreatedEvent;
import io.lcalmsky.app.modules.study.infra.repository.StudyRepository;
import io.lcalmsky.app.modules.tag.domain.entity.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StudyService {
    private final StudyRepository studyRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Study createNewStudy(StudyForm studyForm, Account account) {
        Study study = Study.from(studyForm);
        study.addManager(account);
        return studyRepository.save(study);
    }

    public Study getStudy(String path) {
        Study study = studyRepository.findByPath(path);
        checkStudyExists(path, study);
        return study;
    }

    public Study getStudyToUpdate(Account account, String path) {
        return getStudy(account, path, studyRepository.findByPath(path));
    }

    public Study getStudyToUpdateTag(Account account, String path) {
        return getStudy(account, path, studyRepository.findStudyWithTagsByPath(path));
    }

    public Study getStudyToUpdateZone(Account account, String path) {
        return getStudy(account, path, studyRepository.findStudyWithZonesByPath(path));
    }

    public Study getStudyToUpdateStatus(Account account, String path) {
        return getStudy(account, path, studyRepository.findStudyWithManagersByPath(path));
    }

    private Study getStudy(Account account, String path, Study studyByPath) {
        checkStudyExists(path, studyByPath);
        checkAccountIsManager(account, studyByPath);
        return studyByPath;
    }

    private void checkStudyExists(String path, Study study) {
        if (study == null) {
            throw new IllegalArgumentException(path + "에 해당하는 스터디가 없습니다.");
        }
    }

    private void checkAccountIsManager(Account account, Study study) {
        if (!study.isManagedBy(account)) {
            throw new AccessDeniedException("해당 기능을 사용할 수 없습니다.");
        }
    }

    public void updateStudyDescription(Study study, StudyDescriptionForm studyDescriptionForm) {
        study.updateDescription(studyDescriptionForm);
    }

    public void updateStudyImage(Study study, String image) {
        study.updateImage(image);
    }

    public void enableStudyBanner(Study study) {
        study.setBanner(true);
    }

    public void disableStudyBanner(Study study) {
        study.setBanner(false);
    }

    public void addTag(Study study, Tag tag) {
        study.addTag(tag);
    }

    public void removeTag(Study study, Tag tag) {
        study.removeTag(tag);
    }

    public void addZone(Study study, Zone zone) {
        study.addZone(zone);
    }

    public void removeZone(Study study, Zone zone) {
        study.removeZone(zone);
    }

    public void publish(Study study) {
        study.publish();
        eventPublisher.publishEvent(new StudyCreatedEvent(study));
    }

    public void close(Study study) {
        study.close();
    }

    public void startRecruit(Study study) {
        study.startRecruit();
    }

    public void stopRecruit(Study study) {
        study.stopRecruit();
    }

    public boolean isValidPath(String newPath) {
        if (!newPath.matches(StudyForm.VALID_PATH_PATTERN)) {
            return false;
        }
        return !studyRepository.existsByPath(newPath);
    }

    public void updateStudyPath(Study study, String newPath) {
        study.updatePath(newPath);
    }

    public boolean isValidTitle(String newTitle) {
        return newTitle.length() <= 50;
    }

    public void updateStudyTitle(Study study, String newTitle) {
        study.updateTitle(newTitle);
    }

    public void remove(Study study) {
        if (!study.isRemovable()) {
            throw new IllegalStateException("스터디를 삭제할 수 없습니다.");
        }
        studyRepository.delete(study);
    }

    public void addMember(Study study, Account account) {
        study.addMember(account);
    }

    public void removeMember(Study study, Account account) {
        study.removeMember(account);
    }

    public Study getStudyToEnroll(String path) {
        return studyRepository.findStudyOnlyByPath(path)
                .orElseThrow(() -> new IllegalArgumentException(path + "에 해당하는 스터디가 존재하지 않습니다."));
    }
}

```

</details>

## 이벤트 리스너 수정

이벤트 리스너로 전달한 `Study` 객체는 `StudySettingsController`에서 전달한 객체인데요, 아래 코드를 보시면 

```java
@PostMapping("/study/publish")
public String publishStudy(@CurrentUser Account account, @PathVariable String path, RedirectAttributes attributes) {
    Study study = studyService.getStudyToUpdateStatus(account, path);
    studyService.publish(study);
    attributes.addFlashAttribute("message", "스터디를 공개했습니다.");
    return "redirect:/study/" + study.getEncodedPath() + "/settings/study";
}
```

`getStudyToUpdateStatus`를 호출해서 가져온 객체이고, 

```java
public Study getStudyToUpdateStatus(Account account, String path) {
    return getStudy(account, path, studyRepository.findStudyWithManagersByPath(path));
}
```

이렇게 관리자 정보만 `fetch join` 해서 가져온 값입니다.

알림 발생은 관심사와 지역에 해당할 때 발생하기 때문에 관련 정보를 다시 가져와야 합니다.

따라서 `Study`를 조회하는 기능을 추가해야 합니다.

일단 이벤트 리스너에서는 이러한 내용만 알고 먼저 코드를 작성하도록 하겠습니다.

`/src/main/java/io/lcalmsky/app/modules/study/event/StudyEventListener.java`

```java
package io.lcalmsky.app.modules.study.event;

import io.lcalmsky.app.infra.config.AppProperties;
import io.lcalmsky.app.infra.mail.EmailMessage;
import io.lcalmsky.app.infra.mail.EmailService;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.infra.predicates.AccountPredicates;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
import io.lcalmsky.app.modules.notification.domain.entity.Notification;
import io.lcalmsky.app.modules.notification.domain.entity.NotificationType;
import io.lcalmsky.app.modules.notification.infra.repository.NotificationRepository;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.infra.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;

@Slf4j
@Async
@Transactional
@Component
@RequiredArgsConstructor
public class StudyEventListener {

    private final StudyRepository studyRepository;
    private final AccountRepository accountRepository;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;
    private final AppProperties appProperties;

    @EventListener
    public void handleStudyCreatedEvent(StudyCreatedEvent studyCreatedEvent) {
        Study study = studyRepository.findStudyWithTagsAndZonesById(studyCreatedEvent.getStudy().getId()); // (1)
        Iterable<Account> accounts = accountRepository.findAll(AccountPredicates.findByTagsAndZones(study.getTags(), study.getZones())); // (2)
        for (Account account : accounts) { // (3)
            Account.NotificationSetting notificationSetting = account.getNotificationSetting();
            if (notificationSetting.isStudyCreatedByEmail()) {
                sendEmail(study, account);
            }
            if (notificationSetting.isStudyCreatedByWeb()) {
                saveNotification(study, account);
            }
        }
    }

    private void sendEmail(Study study, Account account) {
        Context context = new Context();
        context.setVariable("link", "/study/" + study.getEncodedPath());
        context.setVariable("nickname", account.getNickname());
        context.setVariable("linkName", study.getTitle());
        context.setVariable("message", "새로운 스터디가 오픈하였습니다.");
        context.setVariable("host", appProperties.getHost());
        String message = templateEngine.process("mail/simple-link", context);
        emailService.sendEmail(EmailMessage.builder()
                .to(account.getEmail())
                .subject("[Webluxible] " + study.getTitle() + " 스터디가 오픈하였습니다.")
                .message(message)
                .build());
    }

    private void saveNotification(Study study, Account account) {
        notificationRepository.save(Notification.from(study.getTitle(), "/study/" + study.getEncodedPath(),
                false, LocalDateTime.now(), study.getShortDescription(), account, NotificationType.STUDY_CREATED));
    }
}
```

1. 관심사와 지역 정보를 추가로 조회합니다.
2. 관심사와 지역정보에 해당하는 모든 계정을 찾습니다.
3. 계정을 순차적으로 탐색하면서 메일 알림 설정을 한 계정에는 메일을 전송하고, 웹 알림 설정을 한 계정은 웹 알림을 저장합니다.

2번에서 `querydsl`의 기능을 사용하는데요, 이 부분도 순서대로 차근차근 살펴보도록 하겠습니다.

## Study Entity, Repository 수정

위에서 `Study` 조회시 관심사와 지역정보를 같이 조회하기 위해 `Entity`에는 `NamedEntityGraph`를, `Repository`에는 `EntityGraph`를 추가해주겠습니다. 

`/src/main/java/io/lcalmsky/app/modules/study/domain/entity/Study.java`

```java
// 생략
@NamedEntityGraph(name = "Study.withTagsAndZones", attributeNodes = {
        @NamedAttributeNode("tags"),
        @NamedAttributeNode("zones")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Study {
    // 생략
}
```

<details>
<summary>Study.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.study.domain.entity;

import io.lcalmsky.app.modules.account.domain.UserAccount;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.domain.entity.Zone;
import io.lcalmsky.app.modules.study.endpoint.form.StudyDescriptionForm;
import io.lcalmsky.app.modules.study.endpoint.form.StudyForm;
import io.lcalmsky.app.modules.tag.domain.entity.Tag;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@NamedEntityGraph(name = "Study.withAll", attributeNodes = {
        @NamedAttributeNode("tags"),
        @NamedAttributeNode("zones"),
        @NamedAttributeNode("managers"),
        @NamedAttributeNode("members")
})
@NamedEntityGraph(name = "Study.withTagsAndManagers", attributeNodes = {
        @NamedAttributeNode("tags"),
        @NamedAttributeNode("managers")
})
@NamedEntityGraph(name = "Study.withZonesAndManagers", attributeNodes = {
        @NamedAttributeNode("zones"),
        @NamedAttributeNode("managers")
})
@NamedEntityGraph(name = "Study.withManagers", attributeNodes = {
        @NamedAttributeNode("managers")
})
@NamedEntityGraph(name = "Study.withMembers", attributeNodes = {
        @NamedAttributeNode("members")
})
@NamedEntityGraph(name = "Study.withTagsAndZones", attributeNodes = {
        @NamedAttributeNode("tags"),
        @NamedAttributeNode("zones")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Study {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToMany
    private Set<Account> managers = new HashSet<>();

    @ManyToMany
    private Set<Account> members = new HashSet<>();

    @Column(unique = true)
    private String path;

    private String title;

    private String shortDescription;

    @Lob @Basic(fetch = FetchType.EAGER)
    private String fullDescription;

    @Lob @Basic(fetch = FetchType.EAGER)
    private String image;

    @ManyToMany
    private Set<Tag> tags = new HashSet<>();

    @ManyToMany
    private Set<Zone> zones = new HashSet<>();

    private LocalDateTime publishedDateTime;

    private LocalDateTime closedDateTime;

    private LocalDateTime recruitingUpdatedDateTime;

    private boolean recruiting;

    private boolean published;

    private boolean closed;

    @Accessors(fluent = true)
    private boolean useBanner;

    public static Study from(StudyForm studyForm) {
        Study study = new Study();
        study.title = studyForm.getTitle();
        study.shortDescription = studyForm.getShortDescription();
        study.fullDescription = studyForm.getFullDescription();
        study.path = studyForm.getPath();
        return study;
    }

    public void addManager(Account account) {
        managers.add(account);
    }

    public boolean isJoinable(UserAccount userAccount) {
        Account account = userAccount.getAccount();
        return this.isPublished() && this.isRecruiting() && !this.members.contains(account) && !this.managers.contains(account);
    }

    public boolean isMember(UserAccount userAccount) {
        return this.members.contains(userAccount.getAccount());
    }

    public boolean isManager(UserAccount userAccount) {
        return this.managers.contains(userAccount.getAccount());
    }

    public void updateDescription(StudyDescriptionForm studyDescriptionForm) {
        this.shortDescription = studyDescriptionForm.getShortDescription();
        this.fullDescription = studyDescriptionForm.getFullDescription();
    }

    public void updateImage(String image) {
        this.image = image;
    }

    public void setBanner(boolean useBanner) {
        this.useBanner = useBanner;
    }

    public void addTag(Tag tag) {
        this.tags.add(tag);
    }

    public void removeTag(Tag tag) {
        this.tags.remove(tag);
    }

    public void addZone(Zone zone) {
        this.zones.add(zone);
    }

    public void removeZone(Zone zone) {
        this.zones.remove(zone);
    }

    public void publish() {
        if (this.closed || this.published) {
            throw new IllegalStateException("스터디를 이미 공개했거나 종료된 스터디 입니다.");
        }
        this.published = true;
        this.publishedDateTime = LocalDateTime.now();
    }

    public void close() {
        if (!this.published || this.closed) {
            throw new IllegalStateException("스터디를 공개하지 않았거나 이미 종료한 스터디 입니다.");
        }
        this.closed = true;
        this.closedDateTime = LocalDateTime.now();
    }

    public boolean isEnableToRecruit() {
        return this.published && this.recruitingUpdatedDateTime == null
                || this.recruitingUpdatedDateTime.isBefore(LocalDateTime.now().minusHours(1));
    }

    public void updatePath(String newPath) {
        this.path = newPath;
    }

    public void updateTitle(String newTitle) {
        this.title = newTitle;
    }

    public boolean isRemovable() {
        return !this.published;
    }

    public void startRecruit() {
        if (!isEnableToRecruit()) {
            throw new RuntimeException("인원 모집을 시작할 수 없습니다. 스터디를 공개하거나 한 시간 뒤 다시 시도하세요.");
        }
        this.recruiting = true;
        this.recruitingUpdatedDateTime = LocalDateTime.now();
    }

    public void stopRecruit() {
        if (!isEnableToRecruit()) {
            throw new RuntimeException("인원 모집을 멈출 수 없습니다. 스터디를 공개하거나 한 시간 뒤 다시 시도하세요.");
        }
        this.recruiting = false;
        this.recruitingUpdatedDateTime = LocalDateTime.now();
    }

    public void addMember(Account account) {
        this.members.add(account);
    }

    public void removeMember(Account account) {
        this.members.remove(account);
    }

    public String getEncodedPath() {
        return URLEncoder.encode(path, StandardCharsets.UTF_8);
    }

    public boolean isManagedBy(Account account) {
        return this.getManagers().contains(account);
    }
}

```

</details>

`/src/main/java/io/lcalmsky/app/modules/study/infra/repository/StudyRepository.java`

```java
// 생략
@Transactional(readOnly = true)
public interface StudyRepository extends JpaRepository<Study, Long> {
    // 생략
    @EntityGraph(value = "Study.withTagsAndZones", type = EntityGraph.EntityGraphType.FETCH)
    Study findStudyWithTagsAndZonesById(Long id);
}
```

<details>
<summary>StudyRepository.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.study.infra.repository;

import io.lcalmsky.app.modules.study.domain.entity.Study;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional(readOnly = true)
public interface StudyRepository extends JpaRepository<Study, Long> {
    boolean existsByPath(String path);

    @EntityGraph(value = "Study.withAll", type = EntityGraph.EntityGraphType.LOAD)
    Study findByPath(String path);

    @EntityGraph(value = "Study.withTagsAndManagers", type = EntityGraph.EntityGraphType.FETCH)
    Study findStudyWithTagsByPath(String path);

    @EntityGraph(value = "Study.withZonesAndManagers", type = EntityGraph.EntityGraphType.FETCH)
    Study findStudyWithZonesByPath(String path);

    @EntityGraph(value = "Study.withManagers", type = EntityGraph.EntityGraphType.FETCH)
    Study findStudyWithManagersByPath(String path);

    @EntityGraph(value = "Study.withMembers", type = EntityGraph.EntityGraphType.FETCH)
    Study findStudyWithMembersByPath(String path);

    Optional<Study> findStudyOnlyByPath(String path);

    @EntityGraph(value = "Study.withTagsAndZones", type = EntityGraph.EntityGraphType.FETCH)
    Study findStudyWithTagsAndZonesById(Long id);
}
```

</details>

## Querydsl 기능 추가

계정 조회시 관심사와 지역정보를 이용해 조회하기 위해 querydsl 기능을 사용할 수 있도록 인터페이스를 상속해줍니다.

`/src/main/java/io/lcalmsky/app/modules/account/infra/repository/AccountRepository.java`

```java
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
// 생략
@Transactional(readOnly = true)
public interface AccountRepository extends JpaRepository<Account, Long>, QuerydslPredicateExecutor<Account> {
    // 생략
}
```

<details>
<summary>AccountRepository.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.account.infra.repository;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface AccountRepository extends JpaRepository<Account, Long>, QuerydslPredicateExecutor<Account> {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Account findByEmail(String email);

    Account findByNickname(String nickname);
}

```

</details>

## Predicate 추가

`QuerydslPredicateExecutor`를 상속하게 되면 아래 메서드들을 사용할 수 있습니다. 

```java
package org.springframework.data.querydsl;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;

public interface QuerydslPredicateExecutor<T> {

	Optional<T> findOne(Predicate predicate);

	Iterable<T> findAll(Predicate predicate);

	Iterable<T> findAll(Predicate predicate, Sort sort);

	Iterable<T> findAll(Predicate predicate, OrderSpecifier<?>... orders);

	Iterable<T> findAll(OrderSpecifier<?>... orders);

	Page<T> findAll(Predicate predicate, Pageable pageable);

	long count(Predicate predicate);

	boolean exists(Predicate predicate);
}

```

`JpaRepository`와 유사하지만 전달하는 파라미터가 `Predicate`인데요, 이는 `querydsl`에서 제공하는 조건절에 해당하는 타입입니다.

따라서 `tags`, `zones`에 포함되는 계정을 찾기위한 `Predicate`를 파라미터로 전달해야 하는데, 이 부분은 `account` 패키지 하위에 `infra` 쪽에 생성하겠습니다.

`/src/main/java/io/lcalmsky/app/modules/account/infra/predicates/AccountPredicates.java`

```java
package io.lcalmsky.app.modules.account.infra.predicates;

import com.querydsl.core.types.Predicate;
import io.lcalmsky.app.modules.account.domain.entity.QAccount;
import io.lcalmsky.app.modules.account.domain.entity.Zone;
import io.lcalmsky.app.modules.tag.domain.entity.Tag;

import java.util.Set;

public class AccountPredicates {
    public static Predicate findByTagsAndZones(Set<Tag> tags, Set<Zone> zones) {
        QAccount account = QAccount.account;
        return account.zones.any().in(zones).and(account.tags.any().in(tags));
    }
}
```

> querydsl 사용법은 이 포스팅에서는 자세히 다루지 않으므로 추가로 궁금하신 분들은 [여기](https://jaime-note.tistory.com/category/Querydsl)를 참조해주세요.

위에서 작성한 내용은 계정이 가진 지역 관련 정보중 어느 하나라도 전달된 지역 정보에 포함되는지, 관심사도 마찬가지인지 확인하는 조건절입니다.

이후 테스트 시 쿼리가 어떻게 생성되는지 확인해보도록 하겠습니다.

## Notification Entity, Repository 수정 및 추가

이제 마지막으로 알림 내역을 저장할 때 static 생성자를 이용해 Entity를 생성하는 부분이 있는데, 그 부분을 추가해주고 NotificationRepository도 생성해주겠습니다.

`/src/main/java/io/lcalmsky/app/modules/notification/domain/entity/Notification.java`

```java
// 생략
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {
    // 생략
    public static Notification from(String title, String link, boolean checked, LocalDateTime created, String message, Account account, NotificationType notificationType) {
        Notification notification = new Notification();
        notification.title = title;
        notification.link = link;
        notification.checked = checked;
        notification.created = created;
        notification.message = message;
        notification.account = account;
        notification.notificationType = notificationType;
        return notification;
    }
}
```

`/src/main/java/io/lcalmsky/app/modules/notification/infra/repository/NotificationRepository.java`

```java
package io.lcalmsky.app.modules.notification.infra.repository;

import io.lcalmsky.app.modules.notification.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
```

## 테스트

이제 컴파일 에러가 모두 사라졌을테니 테스트를 해보겠습니다.

알림을 수신하기 위해선 관심사, 지역 설정을 해야하고, 알림 설정에서도 이메일이나 웹 알림을 켜두어야 합니다.

현재 웹 알림은 따로 구현되어있지 않으므로 이메일 설정을 on 시켜서 확인해보도록 하겠습니다.

애플리케이션 실행 후 프로필로 들어가 설정을 변경하겠습니다.

먼저 관심 주제에 `spring`을 추가하였습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/61-01.png)

다음으로 활동 지역엔 `서울`을 추가하였습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/61-02.png)

알림 설정에서 스터디 만들어질 때 이메일로 받기 설정을 `on` 시켰습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/61-03.png)

다음으로 새로운 스터디를 개설했습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/61-04.png)

다음으로 스터디 설정에서 주제를 `spring`으로 추가하였습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/61-05.png)

활동 지역을 `서울`로 설정하였습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/61-06.png)

마지막으로 스터디를 공개하였습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/61-07.png)

여기까지 진행한 뒤 로그를 확인해봤습니다.

먼저 스터디 조회를 위한 쿼리입니다. tag, zone 정보를 같이 조회해 온 것을 확인할 수 있습니다.

```text
2022-06-05 21:23:14.898 DEBUG 46126 --- [AsyncExecutor-1] org.hibernate.SQL                        : 
    select
        study0_.id as id1_7_0_,
        tag2_.id as id1_12_1_,
        zone4_.id as id1_13_2_,
        study0_.closed as closed2_7_0_,
        study0_.closed_date_time as closed_d3_7_0_,
        study0_.full_description as full_des4_7_0_,
        study0_.image as image5_7_0_,
        study0_.path as path6_7_0_,
        study0_.published as publishe7_7_0_,
        study0_.published_date_time as publishe8_7_0_,
        study0_.recruiting as recruiti9_7_0_,
        study0_.recruiting_updated_date_time as recruit10_7_0_,
        study0_.short_description as short_d11_7_0_,
        study0_.title as title12_7_0_,
        study0_.use_banner as use_ban13_7_0_,
        tag2_.title as title2_12_1_,
        tags1_.study_id as study_id1_10_0__,
        tags1_.tags_id as tags_id2_10_0__,
        zone4_.city as city2_13_2_,
        zone4_.local_name_of_city as local_na3_13_2_,
        zone4_.province as province4_13_2_,
        zones3_.study_id as study_id1_11_1__,
        zones3_.zones_id as zones_id2_11_1__ 
    from
        study study0_ 
    left outer join
        study_tags tags1_ 
            on study0_.id=tags1_.study_id 
    left outer join
        tag tag2_ 
            on tags1_.tags_id=tag2_.id 
    left outer join
        study_zones zones3_ 
            on study0_.id=zones3_.study_id 
    left outer join
        zone zone4_ 
            on zones3_.zones_id=zone4_.id 
    where
        study0_.id=?
```

다음은 tags, zones를 이용해 조회한 쿼리입니다.

```text
2022-06-05 21:23:15.014 DEBUG 46126 --- [AsyncExecutor-1] org.hibernate.SQL                        : 
    select
        account0_.account_id as account_1_0_,
        account0_.created_date as created_2_0_,
        account0_.last_modified_date as last_mod3_0_,
        account0_.email as email4_0_,
        account0_.email_token as email_to5_0_,
        account0_.email_token_generated_at as email_to6_0_,
        account0_.is_valid as is_valid7_0_,
        account0_.joined_at as joined_a8_0_,
        account0_.nickname as nickname9_0_,
        account0_.study_created_by_email as study_c10_0_,
        account0_.study_created_by_web as study_c11_0_,
        account0_.study_registration_result_by_email as study_r12_0_,
        account0_.study_registration_result_by_web as study_r13_0_,
        account0_.study_updated_by_email as study_u14_0_,
        account0_.study_updated_by_web as study_u15_0_,
        account0_.password as passwor16_0_,
        account0_.bio as bio17_0_,
        account0_.company as company18_0_,
        account0_.image as image19_0_,
        account0_.job as job20_0_,
        account0_.location as locatio21_0_,
        account0_.url as url22_0_ 
    from
        account account0_ 
    where
        (
            exists (
                select
                    1 
                from
                    account_zones zones1_,
                    zone zone2_ 
                where
                    account0_.account_id=zones1_.account_account_id 
                    and zones1_.zones_id=zone2_.id 
                    and zone2_.id=?
            )
        ) 
        and (
            exists (
                select
                    1 
                from
                    account_tags tags3_,
                    tag tag4_ 
                where
                    account0_.account_id=tags3_.account_account_id 
                    and tags3_.tags_id=tag4_.id 
                    and tag4_.id=?
            )
        )
2022-06-05 21:23:15.015 TRACE 46126 --- [AsyncExecutor-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [68]
2022-06-05 21:23:15.015 TRACE 46126 --- [AsyncExecutor-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [2] as [BIGINT] - [100]
```

의도한 대로 쿼리가 잘 수행된 것을 확인할 수 있습니다.

그리고 마지막으로 메일 전송이 되었는지 확인해보면,

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/61-08.png)

정확히 전달된 것을 확인할 수 있습니다.

웹 알림 설정도 켜두었기 때문에 알림 내역이 저장되었을텐데요, 이 부분도 로그와 DB로 확인할 수 있습니다.

```text
2022-06-05 21:23:19.660 DEBUG 46126 --- [AsyncExecutor-1] org.hibernate.SQL                        : 
    insert 
    into
        notification
        (account_account_id, checked, created, link, message, notification_type, title, id) 
    values
        (?, ?, ?, ?, ?, ?, ?, ?)
2022-06-05 21:23:19.661 TRACE 46126 --- [AsyncExecutor-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [86]
2022-06-05 21:23:19.661 TRACE 46126 --- [AsyncExecutor-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [2] as [BOOLEAN] - [false]
2022-06-05 21:23:19.661 TRACE 46126 --- [AsyncExecutor-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [3] as [TIMESTAMP] - [2022-06-05T21:23:19.651341]
2022-06-05 21:23:19.662 TRACE 46126 --- [AsyncExecutor-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [4] as [VARCHAR] - [/study/noti-test]
2022-06-05 21:23:19.662 TRACE 46126 --- [AsyncExecutor-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [5] as [VARCHAR] - [알림 테스트를 위한 스터디 입니다.]
2022-06-05 21:23:19.662 TRACE 46126 --- [AsyncExecutor-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [6] as [VARCHAR] - [STUDY_CREATED]
2022-06-05 21:23:19.662 TRACE 46126 --- [AsyncExecutor-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [7] as [VARCHAR] - [알림 테스트 스터디]
2022-06-05 21:23:19.662 TRACE 46126 --- [AsyncExecutor-1] o.h.type.descriptor.sql.BasicBinder      : binding parameter [8] as [BIGINT] - [102]
```

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/61-09.png)

정상적으로 추가된 것을 확인하였습니다.