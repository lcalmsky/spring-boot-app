![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 9e6cfe6)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 9e6cfe6
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

스터디 변경시 알림 기능을 구현합니다.

스터디 공개 때 알림을 보내는 것과 마찬가지로 특정 시점에 이벤트를 발생시키는 방법으로 구현할 수 있습니다.

알림을 전송하는 시점은 다음과 같습니다.

* 스터디 소개를 업데이트 했을 때
* 스터디가 종료되었을 때
* 스터디 팀원을 모집할 때, 모집이 종료 되었을 때

## 이벤트 생성

스터디 수정시 발생시킬 이벤트 클래스를 생성합니다.

`/src/main/java/io/lcalmsky/app/modules/study/event/StudyUpdateEvent.java`

```java
package io.lcalmsky.app.modules.study.event;

import io.lcalmsky.app.modules.study.domain.entity.Study;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class StudyUpdateEvent {
    private final Study study;
    private final String message;
}
```

`study`와 `message`를 생성자로 받을 수 있도록 `@RequiredArgsConstructor`를 사용하였고, 이벤트 처리시 사용할 수 있게 `@Getter`를 추가하였습니다.

## 서비스 수정

`Study`가 수정될 때 이벤트를 발생시킬 수 있도록 `StudyService`를 수정합니다.

`/src/main/java/io/lcalmsky/app/modules/study/application/StudyService.java`

```java
// 생략
@Service
@RequiredArgsConstructor
@Transactional
public class StudyService {
    // 생략
    public void updateStudyDescription(Study study, StudyDescriptionForm studyDescriptionForm) {
        study.updateDescription(studyDescriptionForm);
        eventPublisher.publishEvent(new StudyUpdateEvent(study, "스터디 소개를 수정했습니다."));
    }
    // 생략
    public void close(Study study) {
        study.close();
        eventPublisher.publishEvent(new StudyUpdateEvent(study, "스터디를 종료했습니다."));
    }

    public void startRecruit(Study study) {
        study.startRecruit();
        eventPublisher.publishEvent(new StudyUpdateEvent(study, "팀원 모집을 시작합니다."));
    }

    public void stopRecruit(Study study) {
        study.stopRecruit();
        eventPublisher.publishEvent(new StudyUpdateEvent(study, "팀원 모집을 종료했습니다."));
    }
    // 생략
}
```

스터디 설명이 수정될 때, 스터디가 종료될 때, 팀원 모집을 시작/종료할 때 이벤트가 발생하도록 수정하였습니다.

## Repository 수정

그동안은 `EntityGraph` 사용을 위해 `Entity`에 `@NamedEntityGraph`를 지정하고 `Repository`에서 `@EntityGraph` 참조했었는데요, 이번엔 `Repository`에서 한번에 처리하는 방법으로 추가해보겠습니다.

먼저 `Study`가 업데이트 될 때 알림을 보내야 할 대상은 관리자와 멤버입니다. 따라서 `fetch join`으로 같이 조회해야 할 대상이 되는데 아래와 같이 간단하게 구현할 수 있습니다.

`/src/main/java/io/lcalmsky/app/modules/study/infra/repository/StudyRepository.java`

```java
// 생략
@Transactional(readOnly = true)
public interface StudyRepository extends JpaRepository<Study, Long> {
    // 생략
    @EntityGraph(attributePaths = {"managers", "members"})
    Study findStudyWithManagersAndMembersById(Long id);
}
```

`@EntityGraph`의 `attribute`로 `attributePaths`를 바로 지정할 수 있습니다. 이 때 `type`은 기본이 `FETCH` 이므로 생략할 수 있습니다.

## EventListener 수정

마지막으로 스터디 수정에 대한 이벤트 처리를 추가합니다.

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
import java.util.HashSet;
import java.util.Set;

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
        Study study = studyRepository.findStudyWithTagsAndZonesById(studyCreatedEvent.getStudy().getId());
        Iterable<Account> accounts = accountRepository.findAll(AccountPredicates.findByTagsAndZones(study.getTags(), study.getZones()));
        for (Account account : accounts) {
            Account.NotificationSetting notificationSetting = account.getNotificationSetting();
            if (notificationSetting.isStudyCreatedByEmail()) {
                sendEmail(study, account, "새로운 스터디가 오픈하였습니다.", "[Webluxible] " + study.getTitle() + " 스터디가 오픈하였습니다.");
            }
            if (notificationSetting.isStudyCreatedByWeb()) {
                saveNotification(study, account, NotificationType.STUDY_CREATED, study.getShortDescription());
            }
        }
    }

    @EventListener
    public void handleStudyUpdateEvent(StudyUpdateEvent studyUpdateEvent) {
        Study study = studyRepository.findStudyWithManagersAndMembersById(studyUpdateEvent.getStudy().getId());
        Set<Account> accounts = new HashSet<>();
        accounts.addAll(study.getManagers());
        accounts.addAll(study.getMembers());
        accounts.forEach(account -> {
            if (account.getNotificationSetting().isStudyUpdatedByEmail()) {
                sendEmail(study, account, studyUpdateEvent.getMessage(), "[Webluxible] " + study.getTitle() + " 스터디에 새소식이 있습니다.");
            }
            if (account.getNotificationSetting().isStudyUpdatedByWeb()) {
                saveNotification(study, account, NotificationType.STUDY_UPDATED, studyUpdateEvent.getMessage());
            }
        });
    }

    private void sendEmail(Study study, Account account, String contextMessage, String emailSubject) {
        Context context = new Context();
        context.setVariable("link", "/study/" + study.getEncodedPath());
        context.setVariable("nickname", account.getNickname());
        context.setVariable("linkName", study.getTitle());
        context.setVariable("message", contextMessage);
        context.setVariable("host", appProperties.getHost());
        String message = templateEngine.process("mail/simple-link", context);
        emailService.sendEmail(EmailMessage.builder()
                .to(account.getEmail())
                .subject(emailSubject)
                .message(message)
                .build());
    }

    private void saveNotification(Study study, Account account, NotificationType notificationType, String message) {
        notificationRepository.save(Notification.from(study.getTitle(), "/study/" + study.getEncodedPath(),
                false, LocalDateTime.now(), message, account, notificationType));
    }
}
```

기존에 사용하던 메일 전송기능과 알람 저장 기능을 리팩토링하여 재사용하였습니다.

## 테스트

대부분 기능이 동일하므로 간단히 테스트 해보겠습니다.

스터디 설정 화면에서 스터디 설명을 수정합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/64-01.png)

수정시 이메일로 알림을 받도록 설정하지 않았으므로 기본 값인 웹 알림만 동작합니다.

아래 그림 처럼 알림 버튼 색상이 변경된 것을 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/64-02.png)

알림 버튼을 클릭해보면 알림이 정상적으로 저장된 것을 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/64-03.png)