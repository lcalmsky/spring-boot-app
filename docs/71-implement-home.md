![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 316856a)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 316856a
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

이제 모든 기능이 구현됐으므로 마지막남은 첫 화면을 구현합니다.

로그인 전과 후로 나뉘는데 먼저 로그인하기 전 화면을 구현해보겠습니다.

## 로그인 전 화면 구현

### MainController 수정

첫 화면에 전달할 데이터를 추가해주기 위해 `MainController`를 수정합니다. 

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/modules/main/endpoint/controller/MainController.java`

```java
// 생략
@Controller
@RequiredArgsConstructor
public class MainController {
    // 생략
    @GetMapping("/")
    public String home(@CurrentUser Account account, Model model) {
        if (account != null) {
            model.addAttribute(account);
        }
        model.addAttribute("studyList", studyRepository.findFirst9ByPublishedAndClosedOrderByPublishedDateTimeDesc(true, false));
        return "index";
    }
    // 생략
}
```

최근 공개된 스터디를 9개 조회하기 위해 JPA 쿼리 메서드를 사용하였습니다.

`findFirst9ByPublishedAndClosedOrderByPublishedDateTimeDesc`

이런식으로 findFirst{number}By{condition} 표현하면 `limit`를 사용한 것처럼 number에 해당하는 숫자만큼만 조회하게 됩니다.

<details>
<summary>MainController.java</summary>

```java
package io.lcalmsky.app.modules.main.endpoint.controller;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.support.CurrentUser;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.infra.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final StudyRepository studyRepository;

    @GetMapping("/")
    public String home(@CurrentUser Account account, Model model) {
        if (account != null) {
            model.addAttribute(account);
        }
        model.addAttribute("studyList", studyRepository.findFirst9ByPublishedAndClosedOrderByPublishedDateTimeDesc(true, false));
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/search/study")
    public String searchStudy(String keyword, Model model,
                              @PageableDefault(size = 9, sort = "publishedDateTime", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<Study> studyPage = studyRepository.findByKeyword(keyword, pageable);
        model.addAttribute("studyPage", studyPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortProperty", pageable.getSort().toString().contains("publishedDateTime")
                ? "publishedDateTime"
                : "memberCount");
        return "search";
    }
}
```

</details>

### StudyRepository 수정

`MainController`에서 스터디를 조회하기 위해 사용한 메서드를 `StudyRepository`에 정의합니다.

```java
// 생략
@Transactional(readOnly = true)
public interface StudyRepository extends JpaRepository<Study, Long>, StudyRepositoryExtension {
    // 생략
    @EntityGraph(attributePaths = {"tags", "zones"})
    List<Study> findFirst9ByPublishedAndClosedOrderByPublishedDateTimeDesc(boolean published, boolean closed);
}
```

`tags`와 `zones`를 `fetchJoin`으로 가져와야하기 때문에 `@EntityGraph`의 `attributePaths`로 지정하였습니다.

<details>
<summary>StudyRepository.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.study.infra.repository;

import io.lcalmsky.app.modules.study.domain.entity.Study;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
public interface StudyRepository extends JpaRepository<Study, Long>, StudyRepositoryExtension {
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

    @EntityGraph(attributePaths = {"managers", "members"})
    Study findStudyWithManagersAndMembersById(Long id);

    @EntityGraph(attributePaths = {"tags", "zones"})
    List<Study> findFirst9ByPublishedAndClosedOrderByPublishedDateTimeDesc(boolean published, boolean closed);

}
```

</details>

### View 수정

기존 홈 화면으로 사용하던 `index.html`을 수정합니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/resources/templates/index.html`

```html
<!DOCTYPE html>
<html lang="en"
      xmlns:th="http://www.thymeleaf.org"
>
<head th:replace="fragments.html::head"></head>
<body class="bg-light">
    <div th:replace="fragments.html::navigation-bar"></div>
    <section class="jumbotron text-center">
        <div class="container">
            <h1>Webluxible</h1>
            <p class="lead text-muted">
                관심 주제와 지역으로 스터디를 찾아 참여하세요.<br/>
                스터디 모임 관리 기능을 제공합니다.
            </p>
            <p>
                <a th:href="@{/sign-up}" class="btn btn-primary my-2">회원 가입</a>
            </p>
        </div>
    </section>
    <div class="container">
        <div class="row justify-content-center pt-3">
            <div th:replace="fragments.html::study-list (studyList=${studyList})"></div>
        </div>
    </div>
    <div th:replace="fragments.html::footer"></div>
    <div th:replace="fragments.html::date-time"></div>
</body>
</html>
```

`studyList`를 표현하는 부분은 `search.html`과 동일하기 때문에 공통으로 사용할 수 있도록 `fragments`로 빼주었습니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/resources/templates/fragments.html`

```html
<!DOCTYPE html>
<html lang="en"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<!--생략-->
<div th:fragment="study-list (studyList)" class="col-sm-10">
    <div class="row">
        <div class="col-md-4" th:each="study: ${studyList}">
            <div class="card mb-4 shadow-sm">
                <div class="card-body">
                    <a th:href="@{'/study/' + ${study.path}}" class="text-decoration-none">
                        <h5 class="card-title context" th:text="${study.title}"></h5>
                    </a>
                    <p class="card-text" th:text="${study.shortDescription}">Short description</p>
                    <p class="card-text context">
                                <span th:each="tag: ${study.tags}" class="font-weight-light text-monospace badge badge-pill badge-info mr-3">
                                    <a th:href="@{'/search/tag/' + ${tag.title}}" class="text-decoration-none text-white">
                                        <i class="fa fa-tag"></i> <span th:text="${tag.title}">Tag</span>
                                    </a>
                                </span>
                        <span th:each="zone: ${study.zones}" class="font-weight-light text-monospace badge badge-primary mr-3">
                                    <a th:href="@{'/search/zone/' + ${zone.id}}" class="text-decoration-none text-white">
                                        <i class="fa fa-globe"></i> <span th:text="${zone.localNameOfCity}" class="text-white">City</span>
                                    </a>
                                </span>
                    </p>
                    <div class="d-flex justify-content-between align-items-center">
                        <small class="text-muted">
                            <i class="fa fa-user-circle"></i>
                            <span th:text="${study.memberCount}"></span>명
                        </small>
                        <small class="text-muted date" th:text="${study.publishedDateTime}">9 mins</small>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<!--생략-->
</html>
```

### 테스트

애플리케이션을 실행한 뒤 홈 화면으로 진입하면,

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/71-01.png)

정상적으로 스터디가 노출되는 것을 확인할 수 있습니다.

## 로그인 후 화면 구현

로그인 전 화면을 구현한 것과 마찬가지로 순서대로 구현해보겠습니다.

다 비슷한 내용이라 코드 위주로 작성해보겠습니다.

### MainController 수정

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/modules/main/endpoint/controller/MainController.java`

```java
@Controller
@RequiredArgsConstructor
public class MainController {

  private final StudyRepository studyRepository;
  private final AccountRepository accountRepository;
  private final EnrollmentRepository enrollmentRepository;

  @GetMapping("/")
  public String home(@CurrentUser Account account, Model model) {
    if (account != null) {
      Account accountLoaded = accountRepository.findAccountWithTagsAndZonesById(account.getId());
      model.addAttribute(accountLoaded);
      model.addAttribute("enrollmentList",
          enrollmentRepository.findByAccountAndAcceptedOrderByEnrolledAtDesc(accountLoaded, true));
      model.addAttribute("studyList",
          studyRepository.findByAccount(accountLoaded.getTags(), accountLoaded.getZones()));
      model.addAttribute("studyManagerOf",
          studyRepository.findFirst5ByManagersContainingAndClosedOrderByPublishedDateTimeDesc(
              account, false));
      model.addAttribute("studyMemberOf",
          studyRepository.findFirst5ByMembersContainingAndClosedOrderByPublishedDateTimeDesc(
              account, false));
      return "home";
    }
    model.addAttribute("studyList",
        studyRepository.findFirst9ByPublishedAndClosedOrderByPublishedDateTimeDesc(true, false));
    return "index";
  }
}
```

account 정보가 있을 때는 home으로 리다이렉트 되도록 하였고, home에서 표시하기위해 필요한 정보들을 조회할 수 있는 기능들을 추가하였습니다.

<details>
<summary>MainController.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.main.endpoint.controller;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
import io.lcalmsky.app.modules.account.support.CurrentUser;
import io.lcalmsky.app.modules.event.infra.repository.EnrollmentRepository;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.infra.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MainController {

  private final StudyRepository studyRepository;
  private final AccountRepository accountRepository;
  private final EnrollmentRepository enrollmentRepository;

  @GetMapping("/")
  public String home(@CurrentUser Account account, Model model) {
    if (account != null) {
      Account accountLoaded = accountRepository.findAccountWithTagsAndZonesById(account.getId());
      model.addAttribute(accountLoaded);
      model.addAttribute("enrollmentList",
          enrollmentRepository.findByAccountAndAcceptedOrderByEnrolledAtDesc(accountLoaded, true));
      model.addAttribute("studyList",
          studyRepository.findByAccount(accountLoaded.getTags(), accountLoaded.getZones()));
      model.addAttribute("studyManagerOf",
          studyRepository.findFirst5ByManagersContainingAndClosedOrderByPublishedDateTimeDesc(
              account, false));
      model.addAttribute("studyMemberOf",
          studyRepository.findFirst5ByMembersContainingAndClosedOrderByPublishedDateTimeDesc(
              account, false));
      return "home";
    }
    model.addAttribute("studyList",
        studyRepository.findFirst9ByPublishedAndClosedOrderByPublishedDateTimeDesc(true, false));
    return "index";
  }

  @GetMapping("/login")
  public String login() {
    return "login";
  }

  @GetMapping("/search/study")
  public String searchStudy(String keyword, Model model,
      @PageableDefault(size = 9, sort = "publishedDateTime", direction = Sort.Direction.ASC) Pageable pageable) {
    Page<Study> studyPage = studyRepository.findByKeyword(keyword, pageable);
    model.addAttribute("studyPage", studyPage);
    model.addAttribute("keyword", keyword);
    model.addAttribute("sortProperty", pageable.getSort().toString().contains("publishedDateTime")
        ? "publishedDateTime"
        : "memberCount");
    return "search";
  }
}
```

</details>

### Repository 수정

`MainController`에서 로그인 후 홈 화면 진입시 계정, 스터디, 모임 정보를 조회할 수 있게 쿼리 메서드 및 querydsl을 사용해 구현합니다.

먼저 `AccountRepository`를 수정합니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/modules/account/infra/repository/AccountRepository.java`

```java
// 생략
@Transactional(readOnly = true)
public interface AccountRepository extends JpaRepository<Account, Long>,
    QuerydslPredicateExecutor<Account> {
  // 생략
  @EntityGraph(attributePaths = {"tags", "zones"})
  Account findAccountWithTagsAndZonesById(Long id);
}

```

다음으로 `EnrollmentRepository`에도 쿼리메서드를 추가합니다. 

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/modules/event/infra/repository/EnrollmentRepository.java`

```java
// 생략
@Transactional(readOnly = true)
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
  // 생략
  @EntityGraph("Enrollment.withEventAndStudy")
  List<Enrollment> findByAccountAndAcceptedOrderByEnrolledAtDesc(Account account, boolean accepted);
}
```

<details>
<summary>EnrollmentRepository.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.event.infra.repository;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.event.domain.entity.Enrollment;
import io.lcalmsky.app.modules.event.domain.entity.Event;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

  boolean existsByEventAndAccount(Event event, Account account);

  Enrollment findByEventAndAccount(Event event, Account account);

  @EntityGraph("Enrollment.withEventAndStudy")
  List<Enrollment> findByAccountAndAcceptedOrderByEnrolledAtDesc(Account account, boolean accepted);
}
```

</details>

`@EntityGraph`를 추가했으므로 `Enrollment Entity`도 수정해주어야 합니다.

```java
// 생략
@NamedEntityGraph(
    name = "Enrollment.withEventAndStudy",
    attributeNodes = {
        @NamedAttributeNode(value = "event", subgraph = "study")
    },
    subgraphs = @NamedSubgraph(name = "study", attributeNodes = @NamedAttributeNode("study"))
)
public class Enrollment {
  // 생략
}
```

<details>
<summary>Enrollment.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.event.domain.entity;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
@NamedEntityGraph(
    name = "Enrollment.withEventAndStudy",
    attributeNodes = {
        @NamedAttributeNode(value = "event", subgraph = "study")
    },
    subgraphs = @NamedSubgraph(name = "study", attributeNodes = @NamedAttributeNode("study"))
)

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

  private boolean attended;


  public static Enrollment of(LocalDateTime enrolledAt, boolean isAbleToAcceptWaitingEnrollment,
      Account account) {
    Enrollment enrollment = new Enrollment();
    enrollment.enrolledAt = enrolledAt;
    enrollment.accepted = isAbleToAcceptWaitingEnrollment;
    enrollment.account = account;
    return enrollment;
  }

  public void accept() {
    this.accepted = true;
  }

  public void reject() {
    this.accepted = false;
  }

  public void attach(Event event) {
    this.event = event;
  }

  public void detachEvent() {
    this.event = null;
  }

  public void attend() {
    this.attended = true;
  }

  public void absent() {
    this.attended = false;
  }
}

```

</details>

`@NamedSubgraph`라는 새로운 애너테이션을 사용했는데, 정의된 하위 그래프를 참조할 수 있게 해줍니다.

`Enrollment`가 `Study`를 참조하고 있지 않기 때문에 `event`를 통해 `Study`를 참조하기위해 사용하였습니다.

마지막으로 스터디와 관련된 `Repository`들을 수정해보겠습니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/modules/study/infra/repository/StudyRepository.java`

```java
// 생략
@Transactional(readOnly = true)
public interface StudyRepository extends JpaRepository<Study, Long>, StudyRepositoryExtension {
  // 생략
  @EntityGraph(attributePaths = {"tags", "zones"})
  List<Study> findFirst9ByPublishedAndClosedOrderByPublishedDateTimeDesc(boolean published,
      boolean closed);

  List<Study> findFirst5ByManagersContainingAndClosedOrderByPublishedDateTimeDesc(Account account,
      boolean closed);

  List<Study> findFirst5ByMembersContainingAndClosedOrderByPublishedDateTimeDesc(Account account,
      boolean closed);
}
```

<details>
<summary>StudyRepository.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.study.infra.repository;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface StudyRepository extends JpaRepository<Study, Long>, StudyRepositoryExtension {

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

  @EntityGraph(attributePaths = {"managers", "members"})
  Study findStudyWithManagersAndMembersById(Long id);

  @EntityGraph(attributePaths = {"tags", "zones"})
  List<Study> findFirst9ByPublishedAndClosedOrderByPublishedDateTimeDesc(boolean published,
      boolean closed);

  List<Study> findFirst5ByManagersContainingAndClosedOrderByPublishedDateTimeDesc(Account account,
      boolean closed);

  List<Study> findFirst5ByMembersContainingAndClosedOrderByPublishedDateTimeDesc(Account account,
      boolean closed);
}
```

</details>

로그인 전 스터디 조회를 위한 메서드 포함 총 세 개의 쿼리 메서드를 추가로 정의하였습니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/modules/study/infra/repository/StudyRepositoryExtension.java`

```java
// 생략
@Transactional(readOnly = true)
public interface StudyRepositoryExtension {
  // 생략
  List<Study> findByAccount(Set<Tag> tags, Set<Zone> zones);
}
```

<details>
<summary>StudyRepositoryExtension.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.study.infra.repository;

import io.lcalmsky.app.modules.account.domain.entity.Zone;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.tag.domain.entity.Tag;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface StudyRepositoryExtension {

  Page<Study> findByKeyword(String keyword, Pageable pageable);

  List<Study> findByAccount(Set<Tag> tags, Set<Zone> zones);
}

```

</details>

`account`가 가진 `tags`와 `zones`를 이용해 `study`를 조회하기 위해 Extension에 정의하였습니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/java/io/lcalmsky/app/modules/study/infra/repository/StudyRepositoryExtensionImpl.java`

```java
// 생략
public class StudyRepositoryExtensionImpl extends QuerydslRepositorySupport implements
    StudyRepositoryExtension {
  // 생략
  @Override
  public List<Study> findByAccount(Set<Tag> tags, Set<Zone> zones) {
    QStudy study = QStudy.study;
    JPQLQuery<Study> query = from(study).where(study.published.isTrue()
            .and(study.closed.isFalse())
            .and(study.tags.any().in(tags))
            .and(study.zones.any().in(zones)))
        .leftJoin(study.tags, QTag.tag).fetchJoin()
        .leftJoin(study.zones, QZone.zone).fetchJoin()
        .orderBy(study.publishedDateTime.desc())
        .distinct()
        .limit(9);
    return query.fetch();
  }
}
```

Extension에 정의한 내용을 구현하였습니다.

### View 구현

`home.html` 파일을 생성하고 작성합니다.

`/Users/jaime/git-repo/spring-boot-app/src/main/resources/templates/home.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html::head"></head>
<body class="bg-light">
    <div th:replace="fragments.html::navigation-bar"></div>
    <div class="alert alert-warning" role="alert" th:if="${account != null && !account?.isValid()}">
        Webluxible 가입을 완료하려면 <a href="#" th:href="@{/check-email(email=${account.email})}" class="alert-link">계정 인증 이메일을 확인</a>하세요.
    </div>
    <div class="container mt-4">
        <div class="row">
            <div class="col-md-2">
                <h5 class="font-weight-light">관심 스터디 주제</h5>
                <ul class="list-group list-group-flush">
                    <li class="list-group-item" th:each="tag: ${account.tags}">
                        <i class="fa fa-tag"></i> <span th:text="${tag.title}"></span>
                    </li>
                    <li class="list-group-item" th:if="${account.tags.size() == 0}">
                        <a th:href="@{/settings/tags}" class="btn-text">관심 스터디 주제</a>를 등록하세요.
                    </li>
                </ul>
                <h5 class="mt-3 font-weight-light">주요 활동 지역</h5>
                <ul class="list-group list-group-flush">
                    <li class="list-group-item" th:each="zone: ${account.zones}">
                        <i class="fa fa-globe"></i> <span th:text="${zone.getLocalNameOfCity()}">Zone</span>
                    </li>
                    <li class="list-group-item" th:if="${account.zones.size() == 0}">
                        <a th:href="@{/settings/zones}" class="btn-text">주요 활동 지역</a>을 등록하세요.
                    </li>
                </ul>
            </div>
            <div class="col-md-7">
                <h5 th:if="${#lists.isEmpty(enrollmentList)}" class="font-weight-light">참석할 모임이 없습니다.</h5>
                <h5 th:if="${!#lists.isEmpty(enrollmentList)}" class="font-weight-light">참석할 모임</h5>
                <div class="row row-cols-1 row-cols-md-2" th:if="${!#lists.isEmpty(enrollmentList)}">
                    <div class="col mb-4" th:each="enrollment: ${enrollmentList}">
                        <div class="card">
                            <div class="card-body">
                                <h5 class="card-title" th:text="${enrollment.event.title}">Event title</h5>
                                <h6 class="card-subtitle mb-2 text-muted" th:text="${enrollment.event.study.title}">Study title</h6>
                                <p class="card-text">
                                <span>
                                    <i class="fa fa-calendar-o"></i>
                                    <span class="calendar" th:text="${enrollment.event.startDateTime}">Last updated 3 mins ago</span>
                                </span>
                                </p>
                                <a th:href="@{'/study/' + ${enrollment.event.study.path} + '/events/' + ${enrollment.event.id}}" class="card-link">모임 조회</a>
                                <a th:href="@{'/study/' + ${enrollment.event.study.path}}" class="card-link">스터디 조회</a>
                            </div>
                        </div>
                    </div>
                </div>
                <h5 class="font-weight-light mt-3" th:if="${#lists.isEmpty(studyList)}">관련 스터디가 없습니다.</h5>
                <h5 class="font-weight-light mt-3" th:if="${!#lists.isEmpty(studyList)}">주요 활동 지역의 관심 주제 스터디</h5>
                <div class="row justify-content-center">
                    <div th:replace="fragments.html::study-list (studyList=${studyList})"></div>
                </div>
            </div>
            <div class="col-md-3">
                <h5 class="font-weight-light" th:if="${#lists.isEmpty(studyManagerOf)}">관리중인 스터디가 없습니다.</h5>
                <h5 class="font-weight-light" th:if="${!#lists.isEmpty(studyManagerOf)}">관리중인 스터디</h5>
                <div class="list-group" th:if="${!#lists.isEmpty(studyManagerOf)}">
                    <a href="#" th:href="@{'/study/' + ${study.path}}" th:text="${study.title}"
                       class="list-group-item list-group-item-action" th:each="study: ${studyManagerOf}">
                        Study title
                    </a>
                </div>

                <h5 class="font-weight-light mt-3" th:if="${#lists.isEmpty(studyMemberOf)}">참여중인 스터디가 없습니다.</h5>
                <h5 class="font-weight-light mt-3" th:if="${!#lists.isEmpty(studyMemberOf)}">참여중인 스터디</h5>
                <div class="list-group" th:if="${!#lists.isEmpty(studyMemberOf)}">
                    <a href="#" th:href="@{'/study/' + ${study.path}}" th:text="${study.title}"
                       class="list-group-item list-group-item-action" th:each="study: ${studyManagerOf}">
                        Study title
                    </a>
                </div>
            </div>
        </div>
    </div>
    <div th:replace="fragments.html::footer"></div>
    <div th:replace="fragments.html::date-time"></div>
</body>
</html>
```

### 테스트

애플리케이션 실행 후 로그인한 뒤 홈 화면으로 이동합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/71-02.png)

제대로 구현된 것을 확인할 수 있습니다.

부트스트랩 버전 차이로 결과물이 이쁘진 않네요 ㅜㅜ 직접 이쁘게 수정해보시기 바랍니다!

---

처음 각오와는 다르게 시간에 치이고 업무에 치이다보니 엄청 오랜 시간이 걸려서 겨우 완성했네요.

특히 frontend 부분을 최신 기술들을 이용해 다시 구현해보고 싶었는데 무기한 연기될 거 같습니다😭

강의에서 구현하는 부분은 여기까지이고 이후에 배포에 고려해야 할 것들에 대해 정리하는 부분이 있는데, 이 부분도 수강해 본 뒤 정리할 필요가 느껴지면 포스팅 할 예정입니다!