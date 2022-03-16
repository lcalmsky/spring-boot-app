![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 83d2d6d)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 83d2d6d
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

앞서서 개인 정보를 다루는 부분을 마무리지었습니다.

이제 원하는 기능을 추가하는 일만 남았는데요, 강의에서는 스터디 관리를 목적으로 둔 애플리케이션을 개발하고 있기 때문에 앞으로 관련 기능을 추가해나갈 예정입니다.

이번 포스팅에서는 관심 주제 도메인 설계를 진행합니다.

## Tag Entity 설계

관심 주제는 `Tag` 형태로 관리될 예정입니다.

`Tag`는 관점에 따라 `Entity`가 될 수도 있고, `Account`가 가지는 Value가 될 수도 있습니다.

하지만 강의에서는 `Tag`를 `Entity`로 취급합니다.

그 이유는 `Tag` 자체의 독자적인 Life Cycle을 가지고 다른 `Entity`의 참조가 필요하기 때문입니다.

`Tag` `Entity`가 가지는 속성은 다음과 같습니다.

* id
* title (unique)

객체 관점에서의 `Account`와 `Tag`의 관계는 아래 다이어그램 처럼 나타낼 수 있습니다.

![](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/diagrams/26-01.puml)

`Account`가 `Tag`를 참조하고 다대다 관계를 가집니다.

Relation DB 관점에서의 관계가 객체간 다대다 관계를 나타내기 위해선 아래와 같은 구조가 되어야 합니다.

![](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/diagrams/26-02.puml)

`Join` 테이블을 이용하여 다대다 관계를 표현합니다. `AccountTag`에서 `Account`와 `Tag`의 `PK`를 참조하는 방식으로 구현할 수 있습니다.

## Tag Entity 구현

Tag 클래스를 생성하고 아래처럼 작성합니다.

`/src/main/java/io/lcalmsky/app/account/domain/entity/Tag.java`

```java
package io.lcalmsky.app.account.domain.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
public class Tag {
    @Id
    @GeneratedValue
    private Long id;
    private String title;
}
```

`Tag`에서 `Account`의 정보를 보여줄 일은 없기 때문에 `Tag` Entity는 최소한의 기능만 가질 수 있게 작성합니다.

## Account Entity 수정

`Account` 클래스에 `Tag` 필드를 추가합니다.

`/src/main/java/io/lcalmsky/app/account/domain/entity/Account.java`

```java
// 생략
public class Account extends AuditingEntity {
    // 생략
    @ManyToMany
    private Set<Tag> tags;
    // 생략
}
```

중복된 `Tag`는 가질 수 없으므로 `Set`을 이용합니다.

관계 설정을 위해 `@ManyToMany` 애너테이션을 사용합니다.

JoinTable 등의 정보를 추가로 줄 수 있지만 기본 애너테이션의 설정을 사용할 예정입니다.

<details>
<summary>Account.java 전체 보기</summary>

```java
package io.lcalmsky.app.account.domain.entity;

import io.lcalmsky.app.domain.entity.AuditingEntity;
import io.lcalmsky.app.settings.controller.NotificationForm;
import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.time.LocalDateTime;
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

    @ManyToMany
    private Set<Tag> tags;

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

## 테이블 확인

애플리케이션을 실행하여 테이블이 정상적으로 생성되는지 확인합니다.

```text
2022-03-16 10:17:46.016 DEBUG 87455 --- [  restartedMain] org.hibernate.SQL                        : 
    
    create table account (
       account_id bigint not null,
        created_date timestamp,
        last_modified_date timestamp,
        email varchar(255),
        email_token varchar(255),
        email_token_generated_at timestamp,
        is_valid boolean not null,
        joined_at timestamp,
        nickname varchar(255),
        study_created_by_email boolean not null,
        study_created_by_web boolean not null,
        study_registration_result_by_email boolean not null,
        study_registration_result_by_web boolean not null,
        study_updated_by_email boolean not null,
        study_updated_by_web boolean not null,
        password varchar(255),
        bio varchar(255),
        company varchar(255),
        image clob,
        job varchar(255),
        location varchar(255),
        url varchar(255),
        primary key (account_id)
    )
2022-03-16 10:17:46.022 DEBUG 87455 --- [  restartedMain] org.hibernate.SQL                        : 
    
    create table account_tags (
       account_account_id bigint not null,
        tags_id bigint not null,
        primary key (account_account_id, tags_id)
    )
2022-03-16 10:17:46.024 DEBUG 87455 --- [  restartedMain] org.hibernate.SQL                        : 
    
    create table persistent_logins (
       series varchar(64) not null,
        last_used timestamp,
        token varchar(64),
        username varchar(64),
        primary key (series)
    )
2022-03-16 10:17:46.025 DEBUG 87455 --- [  restartedMain] org.hibernate.SQL                        : 
    
    create table tag (
       id bigint not null,
        title varchar(255),
        primary key (id)
    )
2022-03-16 10:17:46.026 DEBUG 87455 --- [  restartedMain] org.hibernate.SQL                        : 
    
    alter table account 
       drop constraint if exists UK_q0uja26qgu1atulenwup9rxyr
2022-03-16 10:17:46.027 DEBUG 87455 --- [  restartedMain] org.hibernate.SQL                        : 
    
    alter table account 
       add constraint UK_q0uja26qgu1atulenwup9rxyr unique (email)
2022-03-16 10:17:46.028 DEBUG 87455 --- [  restartedMain] org.hibernate.SQL                        : 
    
    alter table account 
       drop constraint if exists UK_s2a5omeaik0sruawqpvs18qfk
2022-03-16 10:17:46.028 DEBUG 87455 --- [  restartedMain] org.hibernate.SQL                        : 
    
    alter table account 
       add constraint UK_s2a5omeaik0sruawqpvs18qfk unique (nickname)
2022-03-16 10:17:46.028 DEBUG 87455 --- [  restartedMain] org.hibernate.SQL                        : create sequence hibernate_sequence start with 1 increment by 1
2022-03-16 10:17:46.032 DEBUG 87455 --- [  restartedMain] org.hibernate.SQL                        : 
    
    alter table account_tags 
       add constraint FK878dw6wexbmp9hm7kmxsquof3 
       foreign key (tags_id) 
       references tag
2022-03-16 10:17:46.041 DEBUG 87455 --- [  restartedMain] org.hibernate.SQL                        : 
    
    alter table account_tags 
       add constraint FK7fnrqqbmrrif7yqdi0gtty2no 
       foreign key (account_account_id) 
       references account
```

로그를 보시면 의도했던 대로 `account`, `account_tag`, `tag` 테이블이 생성된 것을 확인할 수 있습니다.

[localhost:8080/h2-console](http://localhost:8080/h2-console)에 접속해서 확인하셔도 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/26-01.png)

---

여기까지 비교적 간단한 작업을 통해 관심 주제 도메인 설계를 마쳤습니다.

다음 포스팅부터는 관심 주제 등록을 위한 뷰를 구현하고 기능을 순차적으로 추가해보도록 하겠습니다.