![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 2c78a45)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 2c78a45
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

스터디 설정 중 관심 분야(태그)와 지역을 설정하는 기능을 구현합니다.

기존에 구현했던 내용과 매우 유사하기 때문에 설명보다는 코드 위주로 작성하겠습니다.

## 엔드포인트 수정

`StudySettingsController` 클래스에 새로운 메서드를 추가합니다.

`/src/main/java/io/lcalmsky/app/study/endpoint/StudySettingsController.java`

```java
package io.lcalmsky.app.modules.study.endpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.domain.entity.Zone;
import io.lcalmsky.app.modules.account.support.CurrentUser;
import io.lcalmsky.app.modules.account.endpoint.controller.form.TagForm;
import io.lcalmsky.app.modules.account.endpoint.controller.form.ZoneForm;
import io.lcalmsky.app.modules.study.application.StudyService;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.infra.repository.StudyRepository;
import io.lcalmsky.app.modules.tag.application.TagService;
import io.lcalmsky.app.modules.tag.domain.entity.Tag;
import io.lcalmsky.app.modules.tag.infra.repository.TagRepository;
import io.lcalmsky.app.modules.zone.infra.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@Controller
@RequestMapping("/study/{path}/settings")
@RequiredArgsConstructor
public class StudySettingsController {
    private final StudyService studyService;
    private final TagService tagService;
    private final StudyRepository studyRepository;
    private final TagRepository tagRepository;
    private final ZoneRepository zoneRepository;
    private final ObjectMapper objectMapper;

    @GetMapping("/tags")
    public String studyTagsForm(@CurrentUser Account account, @PathVariable String path, Model model) throws JsonProcessingException {
        Study study = studyService.getStudy(account, path);
        model.addAttribute(account);
        model.addAttribute(study);
        model.addAttribute("tags", study.getTags().stream()
                .map(Tag::getTitle)
                .collect(Collectors.toList()));
        model.addAttribute("whitelist", objectMapper.writeValueAsString(tagRepository.findAll().stream()
                .map(Tag::getTitle)
                .collect(Collectors.toList())));
        return "study/settings/tags";
    }

    @PostMapping("/tags/add")
    @ResponseStatus(HttpStatus.OK)
    public void addTag(@CurrentUser Account account, @PathVariable String path, @RequestBody TagForm tagForm) {
        Study study = studyService.getStudyToUpdateTag(account, path);
        Tag tag = tagService.findOrCreateNew(tagForm.getTagTitle());
        studyService.addTag(study, tag);
    }

    @PostMapping("/tags/remove")
    @ResponseStatus(HttpStatus.OK)
    public void removeTag(@CurrentUser Account account, @PathVariable String path, @RequestBody TagForm tagForm) {
        Study study = studyService.getStudyToUpdateTag(account, path);
        Tag tag = tagRepository.findByTitle(tagForm.getTagTitle())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 태그입니다."));
        studyService.removeTag(study, tag);
    }

    @GetMapping("/zones")
    public String studyZonesForm(@CurrentUser Account account, @PathVariable String path, Model model) throws JsonProcessingException {
        Study study = studyService.getStudy(account, path);
        model.addAttribute(account);
        model.addAttribute(study);
        model.addAttribute("zones", study.getZones().stream()
                .map(Zone::toString)
                .collect(Collectors.toList()));
        model.addAttribute("whitelist", objectMapper.writeValueAsString(zoneRepository.findAll().stream()
                .map(Zone::toString)
                .collect(Collectors.toList())));
        return "study/settings/zones";
    }

    @PostMapping("/zones/add")
    @ResponseStatus(HttpStatus.OK)
    public void addZones(@CurrentUser Account account, @PathVariable String path, @RequestBody ZoneForm zoneForm) {
        Study study = studyService.getStudyToUpdateZone(account, path);
        Zone zone = zoneRepository.findByCityAndProvince(zoneForm.getCityName(), zoneForm.getProvinceName())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지역입니다."));
        studyService.addZone(study, zone);
    }

    @PostMapping("/zones/remove")
    @ResponseStatus(HttpStatus.OK)
    public void removeZones(@CurrentUser Account account, @PathVariable String path, @RequestBody ZoneForm zoneForm) {
        Study study = studyService.getStudyToUpdateZone(account, path);
        Zone zone = zoneRepository.findByCityAndProvince(zoneForm.getCityName(), zoneForm.getProvinceName())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지역입니다."));
        studyService.removeZone(study, zone);
    }
}

```

`TagForm`, `ZoneForm`, `TagRepository`, `ZoneRepository` 의존성을 추가하고 `whitelist`를 작성해서 반환해주기 위해 `ObjectMapper`도 추가하였습니다.

`StudyService`가 `path`를 이용해 `Study`를 조회해오는 부분을 기존의 `getStudy`를 사용하지 않고 `getStudyToUpdateTag`, `getStudyToUpdateZone으로` 나누었습니다.

그 이유는 `StudyService`를 구현하는 부분에서 설명하겠습니다.

<details>
<summary>StudySettingsController.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.study.endpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.domain.entity.Zone;
import io.lcalmsky.app.modules.account.support.CurrentUser;
import io.lcalmsky.app.modules.account.endpoint.controller.form.TagForm;
import io.lcalmsky.app.modules.account.endpoint.controller.form.ZoneForm;
import io.lcalmsky.app.modules.study.application.StudyService;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.endpoint.form.StudyDescriptionForm;
import io.lcalmsky.app.modules.study.infra.repository.StudyRepository;
import io.lcalmsky.app.modules.tag.application.TagService;
import io.lcalmsky.app.modules.tag.domain.entity.Tag;
import io.lcalmsky.app.modules.tag.infra.repository.TagRepository;
import io.lcalmsky.app.modules.zone.infra.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/study/{path}/settings")
@RequiredArgsConstructor
public class StudySettingsController {
    private final StudyService studyService;
    private final TagService tagService;
    private final StudyRepository studyRepository;
    private final TagRepository tagRepository;
    private final ZoneRepository zoneRepository;
    private final ObjectMapper objectMapper;

    @GetMapping("/description")
    public String viewStudySetting(@CurrentUser Account account, @PathVariable String path, Model model) {
        Study study = studyService.getStudy(account, path);
        model.addAttribute(account);
        model.addAttribute(study);
        model.addAttribute(StudyDescriptionForm.builder()
                .shortDescription(study.getShortDescription())
                .fullDescription(study.getFullDescription())
                .build());
        return "study/settings/description";
    }

    @PostMapping("/description")
    public String updateStudy(@CurrentUser Account account, @PathVariable String path, @Valid StudyDescriptionForm studyDescriptionForm, Errors errors, Model model, RedirectAttributes attributes) {
        Study study = studyService.getStudy(account, path);
        if (errors.hasErrors()) {
            model.addAttribute(account);
            model.addAttribute(study);
            return "study/settings/description";
        }
        studyService.updateStudyDescription(study, studyDescriptionForm);
        attributes.addFlashAttribute("message", "스터디 소개를 수정했습니다.");
        return "redirect:/study/" + encode(path) + "/settings/description";
    }

    private String encode(String path) {
        return URLEncoder.encode(path, StandardCharsets.UTF_8);
    }

    @GetMapping("/banner")
    public String studyImageForm(@CurrentUser Account account, @PathVariable String path, Model model) {
        Study study = studyService.getStudy(account, path);
        model.addAttribute(account);
        model.addAttribute(study);
        return "study/settings/banner";
    }

    @PostMapping("/banner")
    public String updateBanner(@CurrentUser Account account, @PathVariable String path, String image, RedirectAttributes attributes) {
        Study study = studyService.getStudy(account, path);
        studyService.updateStudyImage(study, image);
        attributes.addFlashAttribute("message", "스터디 이미지를 수정하였습니다.");
        return "redirect:/study/" + encode(path) + "/settings/banner";
    }

    @PostMapping("/banner/enable")
    public String enableStudyBanner(@CurrentUser Account account, @PathVariable String path) {
        Study study = studyService.getStudy(account, path);
        studyService.enableStudyBanner(study);
        return "redirect:/study/" + encode(path) + "/settings/banner";
    }

    @PostMapping("/banner/disable")
    public String disableStudyBanner(@CurrentUser Account account, @PathVariable String path) {
        Study study = studyService.getStudy(account, path);
        studyService.disableStudyBanner(study);
        return "redirect:/study/" + encode(path) + "/settings/banner";
    }

    @GetMapping("/tags")
    public String studyTagsForm(@CurrentUser Account account, @PathVariable String path, Model model) throws JsonProcessingException {
        Study study = studyService.getStudy(account, path);
        model.addAttribute(account);
        model.addAttribute(study);
        model.addAttribute("tags", study.getTags().stream()
                .map(Tag::getTitle)
                .collect(Collectors.toList()));
        model.addAttribute("whitelist", objectMapper.writeValueAsString(tagRepository.findAll().stream()
                .map(Tag::getTitle)
                .collect(Collectors.toList())));
        return "study/settings/tags";
    }

    @PostMapping("/tags/add")
    @ResponseStatus(HttpStatus.OK)
    public void addTag(@CurrentUser Account account, @PathVariable String path, @RequestBody TagForm tagForm) {
        Study study = studyService.getStudyToUpdateTag(account, path);
        Tag tag = tagService.findOrCreateNew(tagForm.getTagTitle());
        studyService.addTag(study, tag);
    }

    @PostMapping("/tags/remove")
    @ResponseStatus(HttpStatus.OK)
    public void removeTag(@CurrentUser Account account, @PathVariable String path, @RequestBody TagForm tagForm) {
        Study study = studyService.getStudyToUpdateTag(account, path);
        Tag tag = tagRepository.findByTitle(tagForm.getTagTitle())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 태그입니다."));
        studyService.removeTag(study, tag);
    }

    @GetMapping("/zones")
    public String studyZonesForm(@CurrentUser Account account, @PathVariable String path, Model model) throws JsonProcessingException {
        Study study = studyService.getStudy(account, path);
        model.addAttribute(account);
        model.addAttribute(study);
        model.addAttribute("zones", study.getZones().stream()
                .map(Zone::toString)
                .collect(Collectors.toList()));
        model.addAttribute("whitelist", objectMapper.writeValueAsString(zoneRepository.findAll().stream()
                .map(Zone::toString)
                .collect(Collectors.toList())));
        return "study/settings/zones";
    }

    @PostMapping("/zones/add")
    @ResponseStatus(HttpStatus.OK)
    public void addZones(@CurrentUser Account account, @PathVariable String path, @RequestBody ZoneForm zoneForm) {
        Study study = studyService.getStudyToUpdateZone(account, path);
        Zone zone = zoneRepository.findByCityAndProvince(zoneForm.getCityName(), zoneForm.getProvinceName())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지역입니다."));
        studyService.addZone(study, zone);
    }

    @PostMapping("/zones/remove")
    @ResponseStatus(HttpStatus.OK)
    public void removeZones(@CurrentUser Account account, @PathVariable String path, @RequestBody ZoneForm zoneForm) {
        Study study = studyService.getStudyToUpdateZone(account, path);
        Zone zone = zoneRepository.findByCityAndProvince(zoneForm.getCityName(), zoneForm.getProvinceName())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지역입니다."));
        studyService.removeZone(study, zone);
    }
}
```

</details>

## Service 추가 및 수정

먼저 Tag 관련 `Transaction`을 다룰 `TagService`를 추가합니다.

`/src/main/java/io/lcalmsky/app/tag/application/TagService.java`

```java
package io.lcalmsky.app.modules.tag.application;

import io.lcalmsky.app.modules.tag.domain.entity.Tag;
import io.lcalmsky.app.modules.tag.infra.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TagService {
    private final TagRepository tagRepository;

    public Tag findOrCreateNew(String tagTitle) {
        return tagRepository.findByTitle(tagTitle).orElseGet(
                () -> tagRepository.save(Tag.builder()
                        .title(tagTitle)
                        .build())
        );
    }
}
```

태그가 존재하는지 찾아서 반환하는데 존재하지 않는 경우 `TagRepository`에 저장 후 반환합니다.

그리고 `StudyService`에도 메서드를 추가해줍니다.

`/src/main/java/io/lcalmsky/app/study/application/StudyService.java`

```java
package io.lcalmsky.app.modules.study.application;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.domain.entity.Zone;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.infra.repository.StudyRepository;
import io.lcalmsky.app.modules.tag.domain.entity.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StudyService {
    private final StudyRepository studyRepository;

    // 생략

    public Study getStudy(Account account, String path) {
        Study study = studyRepository.findByPath(path);
        checkStudyExists(path, study);
        checkAccountIsManager(account, study);
        return study;
    }

    public Study getStudyToUpdateTag(Account account, String path) {
        Study study = studyRepository.findStudyWithTagsByPath(path);
        checkStudyExists(path, study);
        checkAccountIsManager(account, study);
        return study;
    }

    public Study getStudyToUpdateZone(Account account, String path) {
        Study study = studyRepository.findStudyWithZonesByPath(path);
        checkStudyExists(path, study);
        checkAccountIsManager(account, study);
        return study;
    }

    private void checkStudyExists(String path, Study study) {
        if (study == null) {
            throw new IllegalArgumentException(path + "에 해당하는 스터디가 없습니다.");
        }
    }

    private void checkAccountIsManager(Account account, Study study) {
        if (!account.isManagerOf(study)) {
            throw new AccessDeniedException("해당 기능을 사용할 수 없습니다.");
        }
    }

    // 생략

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
}
```

기존에 `path`를 이용해 `Study`를 가져오는 메서드인 `getStudy(path)`를 제거하고 `getStudy(account, path)` 메서드를 리팩터링 하였습니다.

다른 메서드에서도 사용할 유효성 검증 메서드들(스터디 존재 여부, 관리자 여부)을 추출하였습니다.

그리고 `StudySettingsController`에서 `Study`를 조회할 때 사용하였던 `getStudyToUpdateTag`, `getStudyToUpdateZone` 메서드를 추가하였습니다.

대부분의 내용은 유사한데 `StudyRepository`에 조회할 때 사용하는 메서드가 상이합니다.

그래서 이 부분을 한 번 더 리팩토링 하겠습니다.

```java
// 생략
public class StudyService {
    private final StudyRepository studyRepository;
    // 생략
    public Study getStudy(Account account, String path) {
        return getStudy(account, path, studyRepository.findByPath(path));
    }

    public Study getStudyToUpdateTag(Account account, String path) {
        return getStudy(account, path, studyRepository.findStudyWithTagsByPath(path));
    }

    public Study getStudyToUpdateZone(Account account, String path) {
        return getStudy(account, path, studyRepository.findStudyWithZonesByPath(path));
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
        if (!account.isManagerOf(study)) {
            throw new AccessDeniedException("해당 기능을 사용할 수 없습니다.");
        }
    }
    // 생략
}
```

`StudyRepository`에 조회하는 기능을 세 가지로 나눈 이유는 [지난 번 포스팅](https://jaime-note.tistory.com/308)에서 다뤘던 N+1 문제를 보다 효율적으로 해결하기 위해서입니다.

스터디 설정 내에서 관심 주제를 변경하거나 지역을 변경할 때는 각각 관심 주제(tag)와 지역(zone) 정보가 스터디와 함께 조회되어야 합니다.

기존에 사용했던 `studyRepository.findByPath` 메서드는 주제와 지역 모두를 가져오기위해 각각의 테이블과 join 하는 과정을 거치는데요, 메서드를 분리하여 해당하는 테이블만 join 하도록하였습니다.

더 자세한 내용은 다음 항목에서 다루겠습니다.

<details>
<summary>StudyService.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.study.application;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.domain.entity.Zone;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.endpoint.form.StudyDescriptionForm;
import io.lcalmsky.app.modules.study.endpoint.form.StudyForm;
import io.lcalmsky.app.modules.study.infra.repository.StudyRepository;
import io.lcalmsky.app.modules.tag.domain.entity.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StudyService {
    private final StudyRepository studyRepository;

    public Study createNewStudy(StudyForm studyForm, Account account) {
        Study study = Study.from(studyForm);
        study.addManager(account);
        return studyRepository.save(study);
    }

    public Study getStudy(Account account, String path) {
        return getStudy(account, path, studyRepository.findByPath(path));
    }

    public Study getStudyToUpdateTag(Account account, String path) {
        return getStudy(account, path, studyRepository.findStudyWithTagsByPath(path));
    }

    public Study getStudyToUpdateZone(Account account, String path) {
        return getStudy(account, path, studyRepository.findStudyWithZonesByPath(path));
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
        if (!account.isManagerOf(study)) {
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
}

```

</details>

## Entity, Repository 수정

효율적인 쿼리를 위해 `Study Entity`에 `@NamedEntityGraph`를 추가합니다.

`/src/main/java/io/lcalmsky/app/study/domain/entity/Study.java`

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

@Entity
@NamedEntityGraph(name = "Study.withAll", attributeNodes = {
        @NamedAttributeNode("tags"),
        @NamedAttributeNode("zones"),
        @NamedAttributeNode("managers"),
        @NamedAttributeNode("members")
})
@NamedEntityGraph(name = "Study.withTagsAndManagers", attributeNodes = {
        @NamedAttributeNode("tags"),
        @NamedAttributeNode("managers"),
})
@NamedEntityGraph(name = "Study.withZonesAndManagers", attributeNodes = {
        @NamedAttributeNode("zones"),
        @NamedAttributeNode("managers"),
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Study {
    // 생략
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
}
```

클래스 내부에는 `tag`와 `zone`을 추가하고 삭제하기 위한 메서드를 추가하였습니다.

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
        @NamedAttributeNode("managers"),
})
@NamedEntityGraph(name = "Study.withZonesAndManagers", attributeNodes = {
        @NamedAttributeNode("zones"),
        @NamedAttributeNode("managers"),
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
}

```

</details>

다음으로 `StudyRepository`에는 `@EntityGraph`를 설정해 준 메서드를 추가하였습니다.

`/src/main/java/io/lcalmsky/app/study/infra/repository/StudyRepository.java`

```java
package io.lcalmsky.app.modules.study.infra.repository;

import io.lcalmsky.app.modules.study.domain.entity.Study;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface StudyRepository extends JpaRepository<Study, Long> {
    boolean existsByPath(String path);

    @EntityGraph(value = "Study.withAll", type = EntityGraph.EntityGraphType.LOAD)
    Study findByPath(String path);

    @EntityGraph(value = "Study.withTagsAndManagers", type = EntityGraph.EntityGraphType.FETCH)
    Study findStudyWithTagsByPath(String path);

    @EntityGraph(value = "Study.withZonesAndManagers", type = EntityGraph.EntityGraphType.FETCH)
    Study findStudyWithZonesByPath(String path);
}
```

스프링 부트 JPA 에서 메서드쿼리를 작성할 때 적용되는 문법이 있습니다.

```
TableType findByColumnName(ColumnType columnValue);
``` 

이런식으로 메서드를 작성했다면 실제로 변환되는 SQL 문은

```sql
select * from TableName where ColumnName = columnValue;
```

이런식으로 적용 됩니다.

`find`, `ByColumnName` 과 같은 정해진 문구는 SQL문을 생성할 때 영향을 주지만 그 사이에 있는 값은 메서드를 구분하는 기능만 가지고 있을 뿐 쿼리에는 영향을 주지 않습니다.

따라서 `findByPath`, `findStudyWithTagsByPath`, `findStudyWithZonesByPath`이 세 가지 쿼리는 `@EntityGraph` 설정이 없다면 동일한 쿼리(findByPath)를 나타냅니다.

하지만 `@EntityGraph` 내에서 설정한 `@NamedEntityGraph`를 따르기 때문에 세 가지 쿼리는 달라지게 됩니다.

`@EntityGraph` 애너테이션의 `attribute`인 `type`에 들어갈 수 있는 타입은 `EntityGraph.EntityGraphType`  타입으로 해당 타입은 두 가지의 값을 가집니다.

* `EntityGraph.EntityGraphType.LOAD`: `Entity` 그래프의 속성 노드에의해 지정된 속성은 `FetchType.EAGER`로 처리되고, 그렇지 않은 속성은 지정되어있는 속성으로, 지정되어있지 않다면 기본 `FetchType`에 따라 처리
* `EntityGraph.EntityGraphType.FETCH`: `Entity` 그래프의 속성 노드에의해 지정된 속성은 `FetchType.EAGER`로 처리되고, 그렇지 않은 속성은 `FetchType.LAZY`로 처리 

미세한 차이지만 따로 `FetchType`을 지정한 경우 `LOAD`를 쓰고, 그렇지 않은 경우 `FETCH`를 쓴다고 생각하면 얼추 대다수 상황에 적용할 수 있습니다.

`fetch join`을 사용하기 위해 `EntityGraph`를 사용하는 것이므로 대부분의 경우 `FETCH`를 사용해 지정되지 않은 필드는 모두 `LAZY`로 가져오는 방식으로 사용해도 구현에 전혀 지장이 없습니다.

## 뷰 구현

추가로 페이지를 구현하기에 앞서 프로필 설정에서의 `tags`, `zones`와 매우 유사하기 때문에 중복되는 내용을 먼저 `fragments.html` 파일로 추출하겠습니다.

`/src/main/resources/templates/fragments.html`

```html
<script type="application/javascript" th:inline="javascript" th:fragment="ajax-csrf-header">
    $(function () {
        var csrfToken = /*[[${_csrf.token}]]*/ null;
        var csrfHeader = /*[[${_csrf.headerName}]]*/ null;
        $(document).ajaxSend(function (e, xhr, options) {
            xhr.setRequestHeader(csrfHeader, csrfToken);
        });
    });
</script>

<div th:fragment="update-tags (baseUrl)">
    <script src="/node_modules/@yaireo/tagify/dist/tagify.min.js"></script>
    <script type="application/javascript" th:inline="javascript">
        $(function () {
            function tagRequest(url, tagTitle) {
                $.ajax({
                    dataType: "json",
                    autocomplete: {
                        enabled: true,
                        rightKey: true,
                    },
                    contentType: "application/json; charset=utf-8",
                    method: "POST",
                    url: "[(${baseUrl})]" + url,
                    data: JSON.stringify({'tagTitle': tagTitle})
                }).done(function (data, status) {
                    console.log("${data} and status is ${status}");
                });
            }

            function onAdd(e) {
                tagRequest("/add", e.detail.data.value);
            }

            function onRemove(e) {
                tagRequest("/remove", e.detail.data.value);
            }

            var tagInput = document.querySelector("#tags");
            var tagify = new Tagify(tagInput, {
                pattern: /^.{0,20}$/,
                whitelist: JSON.parse(document.querySelector("#whitelist").textContent),
                dropdown: {
                    enabled: 1,
                }
            });
            tagify.on("add", onAdd);
            tagify.on("remove", onRemove);
            tagify.DOM.input.classList.add('form-control');
            tagify.DOM.scope.parentNode.insertBefore(tagify.DOM.input, tagify.DOM.scope);
        });
    </script>
</div>

<div th:fragment="update-zones (baseUrl)">
    <script src="/node_modules/@yaireo/tagify/dist/tagify.min.js"></script>
    <script type="application/javascript">
        $(function () {
            function tagRequest(url, zoneName) {
                $.ajax({
                    dataType: "json",
                    autocomplete: {
                        enabled: true,
                        rightKey: true,
                    },
                    contentType: "application/json; charset=utf-8",
                    method: "POST",
                    url: "[(${baseUrl})]" + url,
                    data: JSON.stringify({'zoneName': zoneName})
                }).done(function (data, status) {
                    console.log("${data} and status is ${status}");
                });
            }

            function onAdd(e) {
                tagRequest("/add", e.detail.data.value);
            }

            function onRemove(e) {
                tagRequest("/remove", e.detail.data.value);
            }

            var tagInput = document.querySelector("#zones");

            var tagify = new Tagify(tagInput, {
                enforceWhitelist: true,
                whitelist: JSON.parse(document.querySelector("#whitelist").textContent),
                dropdown: {
                    enabled: 1, // suggest tags after a single character input
                } // map tags
            });

            tagify.on("add", onAdd);
            tagify.on("remove", onRemove);

            // add a class to Tagify's input element
            tagify.DOM.input.classList.add('form-control');
            // re-place Tagify's input element outside of the  element (tagify.DOM.scope), just before it
            tagify.DOM.scope.parentNode.insertBefore(tagify.DOM.input, tagify.DOM.scope);
        });
    </script>
</div>
```

`csrf header`를 설정하는 부분과, `tags`, `zones`를 업데이트 하는 부분을 `fragment`로 추출하였습니다.

기존에 `/settings/tags.html,` `/settings/zones.html` 파일도 `fragment`를 `replace` 하는 방식으로 바꿔주었으나 이번 포스팅과는 관련이 없으므로 간단히 코드만 공유하겠습니다.

<details>
<summary>/settings/tags.html 전체 보기</summary>

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
                <div th:replace="fragments.html::settings-menu (currentMenu='tags')"></div>
            </div>
            <div class="col-8">
                <div class="row">
                    <h2 class="col-12">관심있는 스터디 주제</h2>
                </div>
                <div class="row">
                    <div class="col-12">
                        <div class="alert alert-info" role="alert">
                            <svg th:replace="fragments.html::symbol-info"/>
                            참여하고 싶은 스터디 주제를 입력해 주세요. 해당 주제의 스터디가 생기면 알림을 받을 수 있습니다. 태그를 입력하고 쉼표 또는 엔터를 입력하세요.
                        </div>
                        <div id="whitelist" th:text="${whitelist}" hidden></div>
                        <input id="tags" type="text" name="tags" th:value="${#strings.listJoin(tags, ',')}"
                               class="tagify-outside" aria-describedby="tagHelp"/>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <script th:replace="fragments.html :: ajax-csrf-header"></script>
    <script th:replace="fragments.html :: update-tags(baseUrl='/settings/tags')"></script>
</body>
</html>
```

</details>

<details>
<summary>/settings/zones.html.html 전체 보기</summary>

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
                            스터디를 참가할 수 있는 지역을 등록하세요. 해당 지역에 스터디가 등록되면 알림을 받을 수 있습니다. 시스템에 등록된 지역 외에는 등록되지 않습니다. 반드시
                            자동완성을 통해 입력해주세요.
                        </div>
                        <div id="whitelist" th:text="${whitelist}" hidden></div>
                        <input id="zones" type="text" name="zones" th:value="${#strings.listJoin(zones, ',')}"
                               class="tagify-outside" aria-describedby="tagHelp"/>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <script th:replace="fragments.html :: ajax-csrf-header"></script>
    <script th:replace="fragments.html :: update-zones(baseUrl='/settings/zones')"></script>
</body>
</html>
```

</details>

위의 두 파일을 복사하여 `study` 경로 하위에 동일한 이름으로 파일을 생성합니다.

두 파일 역시 대부분 기능이 유사하고 옆에 메뉴만 조금씩 달라졌으므로 코드로 설명을 대체하겠습니다.

`/src/main/resources/templates/study/settings/tags.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<svg th:replace="fragments.html :: svg-symbols"/>
<body>
    <nav th:replace="fragments.html :: navigation-bar"></nav>
    <div th:replace="fragments.html :: study-banner"></div>
    <div class="container">
        <div th:replace="fragments.html :: study-info"></div>
        <div th:replace="fragments.html :: study-menu(studyMenu='settings')"></div>
        <div class="row mt-3 justify-content-center">
            <div class="col-2">
                <div th:replace="fragments.html :: study-settings-menu(currentMenu='tags')"></div>
            </div>
            <div class="col-8">
                <div class="row">
                    <h2 class="col-sm-12">스터디 주제</h2>
                </div>
                <div class="row">
                    <div class="col-sm-12">
                        <div class="alert alert-info" role="alert">
                            <svg th:replace="fragments::symbol-info"/>
                            스터디에서 주로 다루는 주제를 태그로 등록하세요. 태그를 입력하고 콤마(,) 또는 엔터를 입력하세요.
                        </div>
                        <div id="whitelist" th:text="${whitelist}" hidden>
                        </div>
                        <input id="tags" type="text" name="tags" th:value="${#strings.listJoin(tags, ',')}"
                               class="tagify-outside" aria-describedby="tagHelp">
                    </div>
                </div>
            </div>
        </div>
        <div th:replace="fragments.html :: footer"></div>
    </div>
    <script th:replace="fragments.html :: ajax-csrf-header"></script>
    <script th:replace="fragments.html :: update-tags(baseUrl='/study/' + ${study.path} + '/settings/tags')"></script>
</body>
</html>
```

`/src/main/resources/templates/study/settings/zones.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<svg th:replace="fragments.html :: svg-symbols"/>
<body>
    <nav th:replace="fragments.html :: navigation-bar"></nav>
    <div th:replace="fragments.html :: study-banner"></div>
    <div class="container">
        <div th:replace="fragments.html :: study-info"></div>
        <div th:replace="fragments.html :: study-menu(studyMenu='settings')"></div>
        <div class="row mt-3 justify-content-center">
            <div class="col-2">
                <div th:replace="fragments.html :: study-settings-menu(currentMenu='zones')"></div>
            </div>
            <div class="col-8">
                <div class="row">
                    <h2 class="col-sm-12">주요 활동 지역</h2>
                </div>
                <div class="row">
                    <div class="col-sm-12">
                        <div class="alert alert-info" role="alert">
                            <svg th:replace="fragments::symbol-info"/>
                            주로 스터디를 진행하는 지역을 등록하세요. 시스템에 등록된 지역만 선택할 수 있습니다.
                        </div>
                        <div id="whitelist" th:text="${whitelist}" hidden></div>
                        <input id="zones" type="text" name="zones" th:value="${#strings.listJoin(zones, ',')}"
                               class="tagify-outside">
                    </div>
                </div>
            </div>
        </div>
        <div th:replace="fragments.html :: footer"></div>
    </div>
    <script th:replace="fragments.html :: ajax-csrf-header"></script>
    <script th:replace="fragments.html :: update-zones(baseUrl='/study/' + ${study.path} + '/settings/zones')"></script>
</body>
</html>
```

## 테스트

애플리케이션을 실행한 뒤 스터디에 진입하고 설정 탭에서 스터디 주제를 클릭합니다.

기존에 태그를 테스트했던 것과 동일한 방식으로 테스트하여 정상 동작을 확인합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/43-01.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/43-02.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/43-03.png)

## 테스트 코드 작성

테스트 코드 작성에 앞서 `TagForm`, `ZoneForm`의 경우 패키지가 달라 객체를 생성할 수 없어 각각 클래스에 `@AllArgsConstructor,` `@Builder` 애너테이션을 추가하였습니다.

```java
// 생략
@AllArgsConstructor
@Builder
public class TagForm {
    // 생략
}
```

```java
// 생략
@AllArgsConstructor
@Builder
public class ZoneForm {
    // 생략
}
```

`/src/test/java/io/lcalmsky/app/study/endpoint/StudySettingsControllerTest.java`

```java
package io.lcalmsky.app.modules.study.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lcalmsky.app.modules.account.WithAccount;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.domain.entity.Zone;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
import io.lcalmsky.app.modules.account.endpoint.controller.form.TagForm;
import io.lcalmsky.app.modules.account.endpoint.controller.form.ZoneForm;
import io.lcalmsky.app.modules.study.application.StudyService;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.endpoint.form.StudyForm;
import io.lcalmsky.app.modules.study.infra.repository.StudyRepository;
import io.lcalmsky.app.modules.tag.domain.entity.Tag;
import io.lcalmsky.app.modules.tag.infra.repository.TagRepository;
import io.lcalmsky.app.modules.zone.infra.repository.ZoneRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class StudySettingsControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired AccountRepository accountRepository;
    @Autowired StudyRepository studyRepository;
    @Autowired TagRepository tagRepository;
    @Autowired ZoneRepository zoneRepository;
    @Autowired StudyService studyService;
    @Autowired ObjectMapper objectMapper;
    private final String studyPath = "study-test";

    @BeforeEach
    void beforeEach() {
        Account account = accountRepository.findByNickname("jaime");
        studyService.createNewStudy(StudyForm.builder()
                .path(studyPath)
                .shortDescription("short-description")
                .fullDescription("full-description")
                .title("title")
                .build(), account);
    }

    @AfterEach
    void afterEach() {
        studyRepository.deleteAll();
    }

    @Test
    @DisplayName("스터디 세팅 폼 조회(소개)")
    @WithAccount("jaime")
    void studySettingFormDescription() throws Exception {
        mockMvc.perform(get("/study/" + studyPath + "/settings/description"))
                .andExpect(status().isOk())
                .andExpect(view().name("study/settings/description"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("studyDescriptionForm"));
    }

    @Test
    @DisplayName("스터디 세팅 수정: 정상")
    @WithAccount("jaime")
    void updateStudyDescription() throws Exception {
        Account account = accountRepository.findByNickname("jaime");
        String shortDescriptionToBeUpdated = "short-description-test";
        String fullDescriptionToBeUpdated = "full-description-test";
        mockMvc.perform(post("/study/" + studyPath + "/settings/description")
                        .param("shortDescription", shortDescriptionToBeUpdated)
                        .param("fullDescription", fullDescriptionToBeUpdated)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/settings/description"));
        Study study = studyService.getStudy(account, studyPath);
        assertEquals(shortDescriptionToBeUpdated, study.getShortDescription());
        assertEquals(fullDescriptionToBeUpdated, study.getFullDescription());
    }

    @Test
    @DisplayName("스터디 세팅 폼 조회(배너)")
    @WithAccount("jaime")
    void studySettingFormBanner() throws Exception {
        mockMvc.perform(get("/study/" + studyPath + "/settings/banner"))
                .andExpect(status().isOk())
                .andExpect(view().name("study/settings/banner"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("study"));
    }

    @Test
    @DisplayName("스터디 배너 업데이트")
    @WithAccount("jaime")
    void updateStudyBanner() throws Exception {
        mockMvc.perform(post("/study/" + studyPath + "/settings/banner")
                        .param("image", "image-test")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/settings/banner"));
    }


    @Test
    @DisplayName("스터디 배너 사용")
    @WithAccount("jaime")
    void enableStudyBanner() throws Exception {
        mockMvc.perform(post("/study/" + studyPath + "/settings/banner/enable")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/settings/banner"));
        Study study = studyRepository.findByPath(studyPath);
        assertTrue(study.useBanner());
    }

    @Test
    @DisplayName("스터디 배너 미사용")
    @WithAccount("jaime")
    void disableStudyBanner() throws Exception {
        mockMvc.perform(post("/study/" + studyPath + "/settings/banner/disable")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/settings/banner"));
        Study study = studyRepository.findByPath(studyPath);
        assertFalse(study.useBanner());
    }

    @Test
    @DisplayName("스터디 세팅 폼 조회(스터디 주제)")
    @WithAccount("jaime")
    void studySettingFormTag() throws Exception {
        mockMvc.perform(get("/study/" + studyPath + "/settings/tags"))
                .andExpect(status().isOk())
                .andExpect(view().name("study/settings/tags"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("study"))
                .andExpect(model().attributeExists("tags"))
                .andExpect(model().attributeExists("whitelist"));
    }

    @Test
    @DisplayName("스터디 태그 추가")
    @WithAccount("jaime")
    void addStudyTag() throws Exception {
        String tagTitle = "newTag";
        TagForm tagForm = TagForm.builder()
                .tagTitle(tagTitle)
                .build(); // 패키지가 달라 객체 생성이 되지 않아 TagForm에 @AllArgsConstructor, @Builder 추가
        mockMvc.perform(post("/study/" + studyPath + "/settings/tags/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tagForm))
                        .with(csrf()))
                .andExpect(status().isOk());
        Study study = studyRepository.findStudyWithTagsByPath(studyPath);
        Tag tag = tagRepository.findByTitle(tagTitle).orElse(null);
        assertNotNull(tag);
        assertTrue(study.getTags().contains(tag));
    }

    @Test
    @DisplayName("스터디 태그 삭제")
    @WithAccount("jaime")
    void removeStudyTag() throws Exception {
        Study study = studyRepository.findStudyWithTagsByPath(studyPath);
        String tagTitle = "newTag";
        Tag tag = tagRepository.save(Tag.builder()
                .title(tagTitle)
                .build());
        studyService.addTag(study, tag);
        TagForm tagForm = TagForm.builder()
                .tagTitle(tagTitle)
                .build(); // 패키지가 달라 객체 생성이 되지 않아 TagForm에 @AllArgsConstructor, @Builder 추가
        mockMvc.perform(post("/study/" + studyPath + "/settings/tags/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tagForm))
                        .with(csrf()))
                .andExpect(status().isOk());
        assertFalse(study.getTags().contains(tag));
    }

    @Test
    @DisplayName("스터디 세팅 폼 조회(활동 지역)")
    @WithAccount("jaime")
    void studySettingFormZone() throws Exception {
        mockMvc.perform(get("/study/" + studyPath + "/settings/zones"))
                .andExpect(status().isOk())
                .andExpect(view().name("study/settings/zones"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("study"))
                .andExpect(model().attributeExists("zones"))
                .andExpect(model().attributeExists("whitelist"));
    }

    @Test
    @DisplayName("스터디 지역 추가")
    @WithAccount("jaime")
    void addStudyZone() throws Exception {
        Zone testZone = Zone.builder().city("test").localNameOfCity("테스트시").province("테스트주").build();
        zoneRepository.save(testZone);
        ZoneForm zoneForm = ZoneForm.builder()
                .zoneName(testZone.toString())
                .build();
        zoneForm.setZoneName(testZone.toString());
        mockMvc.perform(post("/study/" + studyPath + "/settings/zones/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zoneForm))
                        .with(csrf()))
                .andExpect(status().isOk());
        Study study = studyRepository.findStudyWithZonesByPath(studyPath);
        assertTrue(study.getZones().contains(testZone));
    }

    @Test
    @DisplayName("스터디 지역 삭제")
    @WithAccount("jaime")
    void removeStudyZone() throws Exception {
        Study study = studyRepository.findStudyWithZonesByPath(studyPath);
        Zone testZone = Zone.builder().city("test").localNameOfCity("테스트시").province("테스트주").build();
        zoneRepository.save(testZone);
        studyService.addZone(study, testZone);
        ZoneForm zoneForm = ZoneForm.builder()
                .zoneName(testZone.toString())
                .build();
        mockMvc.perform(post("/study/" + studyPath + "/settings/zones/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zoneForm))
                        .with(csrf()))
                .andExpect(status().isOk());
        assertFalse(study.getZones().contains(testZone));
    }
}
```

구현한 기능들을 테스트할 수 있도록 작성하여 테스트를 실행하였고, 기존 기능을 포함하여 모두 정상적으로 동작하였습니다!

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/43-04.png)