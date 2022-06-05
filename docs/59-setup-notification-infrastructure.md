![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 32ca10d)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 32ca10d
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

알림을 처리하기위한 인프라를 설정합니다.

알림 처리를 위해 고려해야 할 사항은 다음과 같습니다.

* 비동기 처리
  * 애플리케이션 메인 기능에 영향을 줘선 안 됨
    * ex) 알림 처리시 에러 발생하여 rollback이 발생하여 기존 기능에 영향을 주는 경우
  * 응답 시간에 영향을 주면 안 됨
* 주요 로직에 영향을 줘선 안 됨
  * 알림 처리 로직 분리

## 비동기 설정

비동기 설정을 위해 `Configuration` 클래스를 추가합니다.

`/src/main/java/io/lcalmsky/app/infra/config/AsyncConfig.java`

```java
package io.lcalmsky.app.infra.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // (1)
@Slf4j
public class AsyncConfig implements AsyncConfigurer { // (2)
    @Override
    public Executor getAsyncExecutor() { // (3)
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int processors = Runtime.getRuntime().availableProcessors();
        log.info("processor count {}", processors);
        executor.setCorePoolSize(processors);
        executor.setMaxPoolSize(processors * 2);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("AsyncExecutor-");
        executor.initialize();
        return executor;
    }
}
```

1. 비동기 처리를 위한 기본 설정을 제공합니다.
2. `AsyncConfigurer`를 구현하여 커스텀 설정을 추가할 수 있습니다.
3. 스레드 풀을 직접 지정합니다.

3번 관련하여 추가로 고려해야할 사항입니다.

* CorePoolSize, MaxPoolSize, QueueCapacity 세 가지를 고려
* 처리할 태스크(이벤트)가 생겼을 때
  * 현재 일하고 있는 쓰레드 개수(active thread)가 코어 개수(core pool size)보다 작으면 남아있는 쓰레드를 사용한다.
  * 현재 일하고 있는 쓰레드 개수가 코어 개수만큼 차있으면 큐 용량(queue capacity)이 찰때까지 큐에 쌓아둔다.
  * 큐 용량이 다 차면, 코어 개수를 넘어서 맥스 개수(max pool size)에 다르기 전까지 새로운 쓰레드를 만들어 처리한다.
  * 맥스 개수를 넘기면 태스크를 처리하지 못한다.

## 이벤트 생성

스터디 생성시 발생시킬 이벤트를 `study` 도메인 패키지 하위에 생성합니다.

`/src/main/java/io/lcalmsky/app/modules/study/event/StudyCreatedEvent.java`

```java
package io.lcalmsky.app.modules.study.event;

import io.lcalmsky.app.modules.study.domain.entity.Study;
import lombok.Getter;

@Getter
public class StudyCreatedEvent {

    private final Study study;

    public StudyCreatedEvent(Study study) {
        this.study = study;
    }
}
```

이벤트는 `ApplicationEvent`를 구현해도 되고, 그냥 `Object` 형태로도 전달할 수 있습니다.

지금은 알림 인프라가 잘 구축되었는지 확인하는 차원이므로 간단하게 구현하였습니다.

## 서비스 수정

스터디 생성시에 이벤트를 발생시킬 수 있도록 `StudyService`를 수정합니다.

`/src/main/java/io/lcalmsky/app/modules/study/application/StudyService.java`

```java
// 생략
@Service
@RequiredArgsConstructor
@Transactional
public class StudyService {
    private final StudyRepository studyRepository;
    private final ApplicationEventPublisher eventPublisher; // (1)

    public Study createNewStudy(StudyForm studyForm, Account account) {
        Study study = Study.from(studyForm);
        study.addManager(account);
        eventPublisher.publishEvent(new StudyCreatedEvent(study)); // (2)
        return studyRepository.save(study);
    }
    // 생략
}
```

1. 이벤트를 발생시키기 위해 빈을 주입합니다.
2. 스터디가 만들어지는 시점에 이벤트를 발생시킵니다. 맨 처음에 다뤘듯이 비동기처리(다른 스레드에서 처리)를 하지 않으면 여기서 `RuntimeException`이 발생했을 경우 `@Transactional`의 영향을 받게되어 `rollback`이 발생하므로 주의해야 합니다.

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
        eventPublisher.publishEvent(new StudyCreatedEvent(study));
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

## 이벤트 리스너 구현

이벤트를 처리할 리스너를 구현합니다.

스터디 생성시 발생하는 이벤트를 처리할 것이므로 `study` 패키지 하위에 생성하였습니다.

`/src/main/java/io/lcalmsky/app/modules/study/event/StudyEventListener.java`

```java
package io.lcalmsky.app.modules.study.event;

import io.lcalmsky.app.modules.study.domain.entity.Study;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Async
@Transactional(readOnly = true)
@Component
public class StudyEventListener {
    @EventListener // (1)
    public void handleStudyCreatedEvent(StudyCreatedEvent studyCreatedEvent) { // (2)
        Study study = studyCreatedEvent.getStudy();
        log.info(study.getTitle() + " is created.");
        // TODO 이메일 보내거나 DB에 Notification 정보 저장
    }
}

```

1. `@EventListener` 애너테이션을 이용해 이벤트 리스너를 명시합니다.
2. EventPublisher를 통해 이벤트가 발생될 때 전달한 파라미터가 `StudyCreatedEvent`일 때 해당 메서드가 호출됩니다.

나중에 알림 기능을 제대로 구현할 예정이라 일단 로그를 남기도록 처리하였습니다.

## 테스트

애플리케이션을 실행한 뒤 스터디를 생성합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/59-01.png)

로그를 확인해보면 별도의 스레드(AsyncExecutor-1)에서 이벤트 처리를 진행했음을 알 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/59-02.png)

---

다음 포스팅부터 알림 기능을 제대로 구현해보도록 하겠습니다.