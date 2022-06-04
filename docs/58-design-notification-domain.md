![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 95e0d48)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 95e0d48
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

알림 도메인을 설계합니다.

## Design

`Entity`간의 관계를 살펴보면 아래와 같습니다.

![](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/diagrams/58-01.puml)

`Notification`은 `Account`와 단방향 `ManyToOne` 관계를 가집니다.

알림(Notification)이 가지는 속성은 다음과 같습니다.

* 제목
* 링크
* 짧은 메시지
* 확인 여부
* 사용자
* 시간
* 알림 타입
  * 새 스터디
  * 참여중인 스터디
  * 모임 참가 신청 결과

## Entity 작성

`notification` 패키지를 생성하고 하위에 `Notification` 클래스를 생성합니다.

`/src/main/java/io/lcalmsky/app/modules/notification/domain/entity/Notification.java`

```java
package io.lcalmsky.app.modules.notification.domain.entity;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {
    @Id @GeneratedValue
    private Long id;

    private String title;

    private String link;

    private String message;

    private boolean checked;

    @ManyToOne
    private Account account;

    private LocalDateTime created;

    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;
}
```

`Account`와 `@ManyToOne` 관계를 지정하였고, 알림 유형은 `enum` 타입으로 지정하였습니다.

같은 패키지 안에 `NotificationType` 클래스를 생성합니다.

`/src/main/java/io/lcalmsky/app/modules/notification/domain/entity/NotificationType.java`

```java
package io.lcalmsky.app.modules.notification.domain.entity;

public enum NotificationType {
    STUDY_CREATED,
    STUDY_UPDATED,
    EVENT_ENROLLMENT,
}
```