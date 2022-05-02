![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 49137fc)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 49137fc
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

모임(Event)과 참가(Enrollment) 두 개의 `Entity`를 설계하고 기존 `Entity`와의 관계를 설정합니다. 

## 설계

먼저 `Entity` 관계는 아래와 같습니다.

![](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/diagrams/46-01.puml)

`Event`는 `Study`, `Account`를 참조할 수 있는 단방향 연관관계를 가지고 `Enrollment`와는 양방향 연관관계를 가집니다.

`Enrollment`는 `Study`와는 관계를 가질 필요가 없고 `Account`와는 단방향 연관관계를, `Event`와는 양방향 연관관계를 가집니다.

## Entity 작성

속성은 필드를 보고 충분히 파악할 수 있으므로 따로 정리하지 않고 바로 `Entity`를 작성하겠습니다.

`event` 패키지 하위에 모임 정보를 담고있는 `Event Entity`를 작성합니다.

`/src/main/java/io/lcalmsky/app/event/domain/entity/Event.java`

```java
package io.lcalmsky.app.event.domain.entity;

import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.study.domain.entity.Study;
import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
@EqualsAndHashCode(of = "id")
public class Event {
  @Id
  @GeneratedValue
  private Long id;

  @ManyToOne
  private Study study;

  @ManyToOne
  private Account createdBy;

  @Column(nullable = false)
  private String title;

  @Lob
  private String description;

  @Column(nullable = false)
  private LocalDateTime createdDateTime;

  @Column(nullable = false)
  private LocalDateTime endEnrollmentDateTime;

  @Column(nullable = false)
  private LocalDateTime startDateTime;

  @Column(nullable = false)
  private LocalDateTime endDateTime;

  private Integer limitOfEnrollments;

  @OneToMany(mappedBy = "event")
  private List<Enrollment> enrollments;

  @Enumerated(EnumType.STRING)
  private EventType eventType;

}
```

마찬가지로 참가 정보를 담고있는 `Enrollment Entity`를 작성합니다.

`/src/main/java/io/lcalmsky/app/event/domain/entity/Enrollment.java`

```java
package io.lcalmsky.app.event.domain.entity;

import io.lcalmsky.app.account.domain.entity.Account;
import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
@EqualsAndHashCode(of = "id")
public class Enrollment {

  @Id
  @GeneratedValue
  private Long id;

  @ManyToOne
  private Event event;

  @ManyToOne
  private Account account;

  private LocalDateTime enrolledAt;

  private boolean accepted;

  private boolean attend;
}
```

모임과 참가는 서로 양방향 연관관계를 가지고 있으므로 `Event.enrollments`에 `mappedBy`를 이용해 관계를 정의해줍니다.

그 외에 `@ManyToOne` 등의 관계는 기본 값을 사용하므로 단방향 관계를 나타냅니다.

따라서 `Account`나 `Study`에서는 `Event`를 조회할 수 없습니다. (아예 방법이 없는 게 아니라 객체 navigation을 통해서는 불가능합니다.)

---

다음 포스팅부터는 모임에 관련된 기능을 순차적으로 구현하겠습니다.