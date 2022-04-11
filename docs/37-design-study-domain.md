![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: c4984db)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout c4984db
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

애플리케이션의 메인 기능인 스터디 관리를 위해 스터디 도메인을 설계합니다.

## 도메인 설계

`Study` `Entity`의 속성 중 관계를 가지는 속성은 아래와 같습니다.

* `Set<Account> managers`: 관리자
* `Set<Account> members`: 회원
* `Set<Tag> tags`: 관심주제
* `Set<Zone> zones`: 활동지역

객체 관점에서 `Study`와 다른 `Entity`의 관계는 다음과 같습니다.

![](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/lcalmsky/spring-boot-app/resources/diagrams/37-01.puml)

* `Study`에서 `Account` 쪽으로 `@ManyToMany` 단방향 관계 두 개(managers, members)
* `Study`에서 `Zone`으로 `@ManyToMany` 단방향 관계
* `Study`에서 `Tag`로 `@ManyToMany` 단방향 관계

## Entity 작성

`study` 패키지를 생성하고 하위에 `Entity` 클래스를 작성합니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/study/domain/entity/Study.java`

```java
package io.lcalmsky.app.study.domain.entity;

import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.domain.entity.Zone;
import io.lcalmsky.app.tag.domain.entity.Tag;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Study {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToMany
    private Set<Account> managers; // (1)

    @ManyToMany
    private Set<Account> members;

    @Column(unique = true)
    private String path; // (2)

    private String title;

    private String shortDescription;

    @Lob @Basic(fetch = FetchType.EAGER)
    private String fullDescription; // (3) 

    @Lob @Basic(fetch = FetchType.EAGER) // (4) 
    private String image;

    @ManyToMany
    private Set<Tag> tags;

    @ManyToMany
    private Set<Zone> zones;

    private LocalDateTime publishedDateTime;

    private LocalDateTime closedDateTime;

    private LocalDateTime recruitingUpdatedDateTime;

    private boolean recruiting;

    private boolean published;

    private boolean closed;

    private boolean useBanner;
}
```

1. 관리자 변경 등을 고려해 관리자를 여러 명을 설정할 수 있게 합니다.
2. 스터디 페이지 경로이므로 유니크해야 합니다.
3. 긴 설명은 255자를 넘어갈 수 있으므로 @Lob을 사용합니다. @Lob은 원래 fetch 설정이 Eager이지만 명시적으로 적어줬습니다.
4. 프로필 사진 때와 마찬가지로 @Lob으로 설정합니다.

---

다음 포스팅부터 본격적으로 스터디 기능을 구현해보겠습니다.