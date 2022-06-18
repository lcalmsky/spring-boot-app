![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 022e586)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 022e586
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

querydsl에 페이징(Paging)을 적용합니다.

## 테스트 데이터 생성

직접 랜덤한 값을 이용해 쿼리를 해도 되지만 현 시점에서 더 간단하게 데이터를 추가해주기 위해 `Controller`와 `Service`에 기능을 추가해보도록 하겠습니다.

먼저 `StudyController`에 테스트용 엔드포인트를 추가합니다.

`/src/main/java/io/lcalmsky/app/modules/study/endpoint/StudyController.java`

```java
// 생략
@Controller
@RequiredArgsConstructor
public class StudyController {
    // 생략
    @GetMapping("/study/data")
    public String generateTestData(@CurrentUser Account account) {
        studyService.generateTestStudies(account);
        return "redirect:/";
    }
}
```

테스트 데이터를 생성하고 홈 화면으로 돌아가는 엔드포인트를 추가하였습니다.

<details>
<summary>StudyController.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.study.endpoint;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.support.CurrentUser;
import io.lcalmsky.app.modules.study.application.StudyService;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.endpoint.form.StudyForm;
import io.lcalmsky.app.modules.study.endpoint.form.validator.StudyFormValidator;
import io.lcalmsky.app.modules.study.infra.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
public class StudyController {
    private final StudyService studyService;
    private final StudyFormValidator studyFormValidator;
    private final StudyRepository studyRepository;

    @InitBinder("studyForm")
    public void studyFormInitBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(studyFormValidator);
    }

    @GetMapping("/new-study")
    public String newStudyForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(new StudyForm());
        return "study/form";
    }

    @PostMapping("/new-study")
    public String newStudySubmit(@CurrentUser Account account, @Valid StudyForm studyForm, Errors errors) {
        if (errors.hasErrors()) {
            return "study/form";
        }
        Study newStudy = studyService.createNewStudy(studyForm, account);
        return "redirect:/study/" + URLEncoder.encode(newStudy.getPath(), StandardCharsets.UTF_8);
    }

    @GetMapping("/study/{path}")
    public String viewStudy(@CurrentUser Account account, @PathVariable String path, Model model) {
        model.addAttribute(account);
        model.addAttribute(studyService.getStudy(path));
        return "study/view";
    }

    @GetMapping("/study/{path}/members")
    public String viewStudyMembers(@CurrentUser Account account, @PathVariable String path, Model model) {
        model.addAttribute(account);
        model.addAttribute(studyService.getStudy(path));
        return "study/members";
    }

    @GetMapping("/study/{path}/join")
    public String joinStudy(@CurrentUser Account account, @PathVariable String path) {
        Study study = studyRepository.findStudyWithMembersByPath(path);
        studyService.addMember(study, account);
        return "redirect:/study/" + study.getEncodedPath() + "/members";
    }

    @GetMapping("/study/{path}/leave")
    public String leaveStudy(@CurrentUser Account account, @PathVariable String path) {
        Study study = studyRepository.findStudyWithMembersByPath(path);
        studyService.removeMember(study, account);
        return "redirect:/study/" + study.getEncodedPath() + "/members";
    }

    @GetMapping("/study/data")
    public String generateTestData(@CurrentUser Account account) {
        studyService.generateTestStudies(account);
        return "redirect:/";
    }
}
```

</details>

다음으로 `StudyService`에 스터디를 랜덤으로 생성하는 메서드를 구현합니다.

`/src/main/java/io/lcalmsky/app/modules/study/application/StudyService.java`

```java
// 생략   
@Service
@RequiredArgsConstructor
@Transactional
public class StudyService {
    // 생략   
    private final TagRepository tagRepository;
    // 생략
    public void generateTestStudies(Account account) {
        for (int i = 0; i < 30; i++) {
            String randomValue = RandomString.make(5);
            Study study = createNewStudy(StudyForm.builder()
                    .title("테스트 스터디 " + randomValue)
                    .path("test-" + randomValue)
                    .shortDescription("테스트용 스터디 입니다.")
                    .fullDescription("test")
                    .build(), account);
            study.publish();
            Tag jpa = tagRepository.findByTitle("jpa").orElse(null);
            study.getTags().add(jpa);
        }
    }
}
```

`RandomString`은 `net.bytebuddy` 패키지 안에 있는데 `querydsl` 패키지 내부에 포함되어 있습니다.

스터디를 30개 생성하고 검색 가능하도록 `publish` 상태로 바꿔준 뒤 `jpa` 태그를 추가해주었습니다.

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
import io.lcalmsky.app.modules.study.event.StudyUpdateEvent;
import io.lcalmsky.app.modules.study.infra.repository.StudyRepository;
import io.lcalmsky.app.modules.tag.domain.entity.Tag;
import io.lcalmsky.app.modules.tag.infra.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.utility.RandomString;
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
    private final TagRepository tagRepository;

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
        eventPublisher.publishEvent(new StudyUpdateEvent(study, "스터디 소개를 수정했습니다."));
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

    public void generateTestStudies(Account account) {
        for (int i = 0; i < 30; i++) {
            String randomValue = RandomString.make(5);
            Study study = createNewStudy(StudyForm.builder()
                    .title("테스트 스터디 " + randomValue)
                    .path("test-" + randomValue)
                    .shortDescription("테스트용 스터디 입니다.")
                    .fullDescription("test")
                    .build(), account);
            study.publish();
            Tag jpa = tagRepository.findByTitle("jpa").orElse(null);
            study.getTags().add(jpa);
        }
    }
}
```

</details>

애플리케이션을 실행하고 `/study/data`에 진입하면 데이터를 생성한 뒤 홈 화면으로 돌아오는 것을 확인할 수 있고 검색창에 `jpa`를 검색하면 다음과 같이 생성한 스터디가 검색됩니다.
(저는 이전에 생성한 스터디를 포함하여 31개가 검색되었습니다.)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/68-01.png)

이제 테스트를 위해 생성했던 코드를 걷어낼 차례인데요, git을 사용하고 있기 때문에 간단히 해결할 수 있습니다.

```shell
> git reset --hard
```

최종 커밋 상태로 되돌립니다.

```shell
> git stash
```

최종 커밋 이후 작업 내용을 따로 보관합니다.

전 문서를 작성하고 있기 때문에 위의 두 명령어를 사용하게 되면 문서까지 날라가기 때문에

```shell
> git checkout -- <filename>
```

을 사용하였습니다.

## 페이징 적용

고전적인 방식으로 SQL의 `limit`과 `offset`을 사용할 수도 있고, `JPA`가 제공하는 `Pageable`을 사용할 수도 있습니다.

우리는 당연히 `Pageable`을 사용하는 방식으로 구현해야겠죠?

먼저 `MainController`를 수정합니다.

`/src/main/java/io/lcalmsky/app/modules/main/endpoint/controller/MainController.java`

```java
// 생략
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
// 생략
@Controller
@RequiredArgsConstructor
public class MainController {
    // 생략
    @GetMapping("/search/study")
    public String searchStudy(String keyword, Model model,
                              @PageableDefault(size = 9, sort = "publishedDateTime", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<Study> studyPage = studyRepository.findByKeyword(keyword, pageable);
        model.addAttribute("studyPage", studyPage);
        model.addAttribute("keyword", keyword);
        return "search";
    }
}
```

기존 스터디 검색 `API`에 `Pageable` 파라미터를 추가하고 `Study`를 `List` 타입이 아닌 `Page` 타입으로 변경합니다.

`@PageableDefault`를 이용하여 페이지 사이즈와 페이지, 정렬 방식 등의 기본 값을 지정할 수 있습니다.

페이지 사이즈는 9로, 정렬은 공개 날짜가 오래된 순으로 기본 값을 지정하였습니다.

> 관련해서 자세한 내용은 아래 포스팅을 참고해주시기 바랍니다.
> 
> * [[Querydsl] Spring Data JPA와 같이 사용하기](https://jaime-note.tistory.com/79?category=994945)
> * [스프링 데이터 JPA - 페이징과 정렬](https://jaime-note.tistory.com/52?category=849450)
> * [스프링 데이터 JPA - 페이징과 정렬2](https://jaime-note.tistory.com/61?category=849450)

<details>
<summary>MainController.java 전체 보기</summary>

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
        return "search";
    }
}
```

</details>

다음으로 `StudyRepositoryExtenstion`과 `Impl` 클래스를 수정합니다.

`/src/main/java/io/lcalmsky/app/modules/study/infra/repository/StudyRepositoryExtension.java`

```java
package io.lcalmsky.app.modules.study.infra.repository;

import io.lcalmsky.app.modules.study.domain.entity.Study;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface StudyRepositoryExtension {
    Page<Study> findByKeyword(String keyword, Pageable pageable);
}
```

`Pageable` 파라미터를 추가하였습니다.

`/src/main/java/io/lcalmsky/app/modules/study/infra/repository/StudyRepositoryExtensionImpl.java`

```java
package io.lcalmsky.app.modules.study.infra.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.JPQLQuery;
import io.lcalmsky.app.modules.account.domain.entity.QAccount;
import io.lcalmsky.app.modules.account.domain.entity.QZone;
import io.lcalmsky.app.modules.study.domain.entity.QStudy;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.tag.domain.entity.QTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

public class StudyRepositoryExtensionImpl extends QuerydslRepositorySupport implements StudyRepositoryExtension {

    public StudyRepositoryExtensionImpl() {
        super(Study.class);
    }

    @Override
    public Page<Study> findByKeyword(String keyword, Pageable pageable) {
        QStudy study = QStudy.study;
        JPQLQuery<Study> query = from(study)
                .where(study.published.isTrue()
                        .and(study.title.containsIgnoreCase(keyword))
                        .or(study.tags.any().title.containsIgnoreCase(keyword))
                        .or(study.zones.any().localNameOfCity.containsIgnoreCase(keyword)))
                .leftJoin(study.tags, QTag.tag).fetchJoin()
                .leftJoin(study.zones, QZone.zone).fetchJoin()
                .leftJoin(study.members, QAccount.account).fetchJoin()
                .distinct();
        JPQLQuery<Study> pageableQuery = getQuerydsl().applyPagination(pageable, query);
        QueryResults<Study> fetchResults = pageableQuery.fetchResults();
        return new PageImpl<>(fetchResults.getResults(), pageable, fetchResults.getTotal());
    }
}
```

`getQuerydsl()`을 이용해 `QuerydslRepositorySupport`가 제공하는 기능을 사용할 수 있는데 페이징을 적용하기 위해 `applyPagination`을 호출합니다.

그리고 `fetchResults`를 이용해 조회한 결과를 얻을 수 있습니다.

반환해야 할 타입이 `Page` 이므로 구현체인 `PageImpl`을 이용해 반환합니다.

결과 데이터, `pageable`, 전체 데이터 수를 생성자로 전달해주어야 합니다.

마지막으로 `MainController`에서 `view`로 전달해주는 이름이 바뀌었기 떄문에 `search.html` 파일도 수정해주어야 합니다.

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html::head"></head>
<body class="bg-light">
    <div th:replace="fragments.html::navigation-bar"></div>
    <div class="container">
        <div class="py-5 text-center">
            <p class="lead" th:if="${studyPage.getTotalElements() == 0}">
                <strong th:text="${keyword}" id="keyword" class="context"></strong>에 해당하는 스터디가 없습니다.
            </p>
            <p class="lead" th:if="${studyPage.getTotalElements() > 0}">
                <strong th:text="${keyword}" id="keyword" class="context"></strong>에 해당하는 스터디를
                <span th:text="${studyPage.getTotalElements()}"></span>개 찾았습니다.
            </p>
        </div>
        <div class="row justify-content-center">
            <div class="col-sm-10">
                <div class="row">
                    <div class="col-md-4" th:each="study: ${studyPage.getContent()}">
                    <!-- 생략-->
                    </div>
                </div>
            </div>
        </div>
</body>
</html>
```

기존에 studyList를 사용하는 곳을 studyPage를 사용하도록 수정하였고 Page에서 제공하는 API를 이용해 비어있는지 확인하는 방식과 전체 개수를 획득하는 방식을 수정하였습니다.

<details>
<summary>search.html 전체 보기</summary>

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html::head"></head>
<body class="bg-light">
    <div th:replace="fragments.html::navigation-bar"></div>
    <div class="container">
        <div class="py-5 text-center">
            <p class="lead" th:if="${studyPage.getTotalElements() == 0}">
                <strong th:text="${keyword}" id="keyword" class="context"></strong>에 해당하는 스터디가 없습니다.
            </p>
            <p class="lead" th:if="${studyPage.getTotalElements() > 0}">
                <strong th:text="${keyword}" id="keyword" class="context"></strong>에 해당하는 스터디를
                <span th:text="${studyPage.getTotalElements()}"></span>개 찾았습니다.
            </p>
        </div>
        <div class="row justify-content-center">
            <div class="col-sm-10">
                <div class="row">
                    <div class="col-md-4" th:each="study: ${studyPage.getContent()}">
                        <div class="card mb-4 shadow-sm">
                            <div class="card-body">
                                <a th:href="@{'/study/' + ${study.path}}" class="text-decoration-none">
                                    <h5 class="card-title context" th:text="${study.title}"></h5>
                                </a>
                                <p class="card-text" th:text="${study.shortDescription}">Short description</p>
                                <p class="card-text context">
                                    <span th:each="tag: ${study.tags}"
                                          class="font-weight-light font-monospace badge rounded-pill bg-success mr-3">
                                        <a th:href="@{'/search/study?keyword=' + ${tag.title}}"
                                           class="text-decoration-none text-white">
                                            <i class="fa fa-tag"></i> <span th:text="${tag.title}">Tag</span>
                                        </a>
                                    </span>
                                    <span th:each="zone: ${study.zones}"
                                          class="font-weight-light font-monospace badge rounded-pill bg-primary mr-3">
                                        <a th:href="@{'/search/study?keyword=' + ${zone.localNameOfCity}}"
                                           class="text-decoration-none text-white">
                                            <i class="fa fa-globe"></i> <span th:text="${zone.localNameOfCity}"
                                                                              class="text-white">City</span>
                                        </a>
                                    </span>
                                </p>
                                <div class="d-flex justify-content-between align-items-center">
                                    <small class="text-muted">
                                        <i class="fa fa-user-circle"></i>
                                        <span th:text="${study.members.size()}"></span>명
                                    </small>
                                    <small class="text-muted date" th:text="${study.publishedDateTime}">9 mins</small>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div th:replace="fragments.html::footer"></div>
    <script th:replace="fragments.html::date-time"></script>
</body>
</html>
```

</details>

## 테스트

앞서 데이터를 추가한 뒤 jpa를 검색하였을 땐 31개가 모두 노출되었는데요, 애플리케이션을 재시작하여 동일하게 jpa를 검색해보면

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/68-02.png)

원하는대로 9개만 노출되는 것을 확인할 수 있습니다.

하지만 현재가 몇 페이지인지 나타나지 않아 다음 검색 결과를 확인할 수 없는데요, 다음 포스팅에서 검색 뷰를 개선해보도록 하겠습니다.