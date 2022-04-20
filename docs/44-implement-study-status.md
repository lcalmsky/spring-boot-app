![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 30781d0)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 30781d0
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

스터디의 상태를 변경할 수 있는 기능을 구현합니다.

> 스터디를 생성한 직후에는 스터디가 공개된 상태가 아닌 DRAFT 상태를 가지게 되는데, 이 상태를 공개로 변환하고, 팀원을 모집중임을 알릴 수 있는 상태로 변경할 수 있도록 합니다.

스터디 경로 및 이름을 수정할 수 있는 기능을 구현합니다.

스터디 삭제 기능을 구현합니다.

## 엔드포인트 추가

스터디 상태 변경, 경로 변경, 이름 변경, 삭제에 대한 엔드포인트를 `StudySettingsController` 클래스에 추가합니다.

`/src/main/java/io/lcalmsky/app/study/endpoint/StudySettingsController.java`

```java
// 생략
public class StudySettingsController {
    // 생략
    @GetMapping("/study") // (1)
    public String studySettingForm(@CurrentUser Account account, @PathVariable String path, Model model) {
        Study study = studyService.getStudy(account, path);
        model.addAttribute(account);
        model.addAttribute(study);
        return "study/settings/study";
    }

    @PostMapping("/study/publish") // (2)
    public String publishStudy(@CurrentUser Account account, @PathVariable String path, RedirectAttributes attributes) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        studyService.publish(study);
        attributes.addFlashAttribute("message", "스터디를 공개했습니다.");
        return "redirect:/study/" + encode(path) + "/settings/study";
    }

    @PostMapping("/study/close") // (3)
    public String closeStudy(@CurrentUser Account account, @PathVariable String path, RedirectAttributes attributes) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        studyService.close(study);
        attributes.addFlashAttribute("message", "스터디를 종료했습니다.");
        return "redirect:/study/" + encode(path) + "/settings/study";
    }

    @PostMapping("/recruit/start") // (4)
    public String startRecruit(@CurrentUser Account account, @PathVariable String path, Model model, RedirectAttributes attributes) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        if (!study.isEnableToRecruit()) {
            attributes.addFlashAttribute("message", "1시간 안에 인원 모집 설정을 여러 번 변경할 수 없습니다.");
            return "redirect:/study/" + encode(path) + "/settings/study";
        }
        studyService.startRecruit(study);
        attributes.addFlashAttribute("message", "인원 모집을 시작합니다.");
        return "redirect:/study/" + encode(path) + "/settings/study";
    }

    @PostMapping("/recruit/stop") // (5)
    public String stopRecruit(@CurrentUser Account account, @PathVariable String path, Model model, RedirectAttributes attributes) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        if (!study.isEnableToRecruit()) {
            attributes.addFlashAttribute("message", "1시간 안에 인원 모집 설정을 여러 번 변경할 수 없습니다.");
            return "redirect:/study/" + encode(path) + "/settings/study";
        }
        studyService.stopRecruit(study);
        attributes.addFlashAttribute("message", "인원 모집을 종료합니다.");
        return "redirect:/study/" + encode(path) + "/settings/study";
    }

    @PostMapping("/study/path") // (6)
    public String updateStudyPath(@CurrentUser Account account, @PathVariable String path, @RequestParam String newPath, Model model, RedirectAttributes attributes) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        if (!studyService.isValidPath(newPath)) {
            model.addAttribute(account);
            model.addAttribute(study);
            model.addAttribute("studyPathError", "사용할 수 없는 스터디 경로입니다.");
            return "study/settings/study";
        }

        studyService.updateStudyPath(study, newPath);
        attributes.addFlashAttribute("message", "스터디 경로를 수정하였습니다.");
        return "redirect:/study/" + encode(newPath) + "/settings/study";
    }

    @PostMapping("/study/title") // (7)
    public String updateStudyTitle(@CurrentUser Account account, @PathVariable String path, String newTitle,
                                   Model model, RedirectAttributes attributes) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        if (!studyService.isValidTitle(newTitle)) {
            model.addAttribute(account);
            model.addAttribute(study);
            model.addAttribute("studyTitleError", "스터디 이름을 다시 입력하세요.");
            return "study/settings/study";
        }

        studyService.updateStudyTitle(study, newTitle);
        attributes.addFlashAttribute("message", "스터디 이름을 수정했습니다.");
        return "redirect:/study/" + encode(path) + "/settings/study";
    }

    @PostMapping("/study/remove") // (8)
    public String removeStudy(@CurrentUser Account account, @PathVariable String path, Model model) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        studyService.remove(study);
        return "redirect:/";
    }
}
```

1. 스터디 설정 내 메뉴 중 스터디 메뉴에 진입했을 때 보여줄 뷰로 이동합니다.
2. 스터디 공개 버튼과 연동합니다.
3. 스터디 종료 버튼과 연동합니다.
4. 팀원 모집 시작 버튼과 연동합니다.
5. 팀원 모집 중단 버튼과 연동합니다.
6. 스터디 경로 수정 버튼과 연동합니다.
7. 스터디 이름 수정 버튼과 연동합니다.
8. 스터디 삭제 버튼과 연동합니다.

모든 기능이 이전에 구현했던 기능들과 매우 유사하고, `Transaction` 처리는 `Service` 레이어에 위임했음을 알 수 있습니다.

현재 구현되지 않은 부분들이 많아 컴파일 에러가 발생하므로 순차적으로 기능들을 구현하겠습니다.

<details>
<summary>StudySettingsController.java 전체 보기</summary>

```java
package io.lcalmsky.app.study.endpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.domain.entity.Zone;
import io.lcalmsky.app.account.support.CurrentUser;
import io.lcalmsky.app.settings.controller.TagForm;
import io.lcalmsky.app.settings.controller.ZoneForm;
import io.lcalmsky.app.study.application.StudyService;
import io.lcalmsky.app.study.domain.entity.Study;
import io.lcalmsky.app.study.form.StudyDescriptionForm;
import io.lcalmsky.app.study.infra.repository.StudyRepository;
import io.lcalmsky.app.tag.application.TagService;
import io.lcalmsky.app.tag.domain.entity.Tag;
import io.lcalmsky.app.tag.infra.repository.TagRepository;
import io.lcalmsky.app.zone.infra.repository.ZoneRepository;
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
        Study study = studyService.getStudyToUpdate(account, path);
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
        Study study = studyService.getStudyToUpdate(account, path);
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
        Study study = studyService.getStudyToUpdate(account, path);
        model.addAttribute(account);
        model.addAttribute(study);
        return "study/settings/banner";
    }

    @PostMapping("/banner")
    public String updateBanner(@CurrentUser Account account, @PathVariable String path, String image, RedirectAttributes attributes) {
        Study study = studyService.getStudyToUpdate(account, path);
        studyService.updateStudyImage(study, image);
        attributes.addFlashAttribute("message", "스터디 이미지를 수정하였습니다.");
        return "redirect:/study/" + encode(path) + "/settings/banner";
    }

    @PostMapping("/banner/enable")
    public String enableStudyBanner(@CurrentUser Account account, @PathVariable String path) {
        Study study = studyService.getStudyToUpdate(account, path);
        studyService.enableStudyBanner(study);
        return "redirect:/study/" + encode(path) + "/settings/banner";
    }

    @PostMapping("/banner/disable")
    public String disableStudyBanner(@CurrentUser Account account, @PathVariable String path) {
        Study study = studyService.getStudyToUpdate(account, path);
        studyService.disableStudyBanner(study);
        return "redirect:/study/" + encode(path) + "/settings/banner";
    }

    @GetMapping("/tags")
    public String studyTagsForm(@CurrentUser Account account, @PathVariable String path, Model model) throws JsonProcessingException {
        Study study = studyService.getStudyToUpdate(account, path);
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
        Study study = studyService.getStudyToUpdate(account, path);
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

    @GetMapping("/study")
    public String studySettingForm(@CurrentUser Account account, @PathVariable String path, Model model) {
        Study study = studyService.getStudyToUpdate(account, path);
        model.addAttribute(account);
        model.addAttribute(study);
        return "study/settings/study";
    }

    @PostMapping("/study/publish")
    public String publishStudy(@CurrentUser Account account, @PathVariable String path, RedirectAttributes attributes) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        studyService.publish(study);
        attributes.addFlashAttribute("message", "스터디를 공개했습니다.");
        return "redirect:/study/" + encode(path) + "/settings/study";
    }

    @PostMapping("/study/close")
    public String closeStudy(@CurrentUser Account account, @PathVariable String path, RedirectAttributes attributes) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        studyService.close(study);
        attributes.addFlashAttribute("message", "스터디를 종료했습니다.");
        return "redirect:/study/" + encode(path) + "/settings/study";
    }

    @PostMapping("/recruit/start")
    public String startRecruit(@CurrentUser Account account, @PathVariable String path, Model model, RedirectAttributes attributes) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        if (!study.isEnableToRecruit()) {
            attributes.addFlashAttribute("message", "1시간 안에 인원 모집 설정을 여러 번 변경할 수 없습니다.");
            return "redirect:/study/" + encode(path) + "/settings/study";
        }
        studyService.startRecruit(study);
        attributes.addFlashAttribute("message", "인원 모집을 시작합니다.");
        return "redirect:/study/" + encode(path) + "/settings/study";
    }

    @PostMapping("/recruit/stop")
    public String stopRecruit(@CurrentUser Account account, @PathVariable String path, Model model, RedirectAttributes attributes) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        if (!study.isEnableToRecruit()) {
            attributes.addFlashAttribute("message", "1시간 안에 인원 모집 설정을 여러 번 변경할 수 없습니다.");
            return "redirect:/study/" + encode(path) + "/settings/study";
        }
        studyService.stopRecruit(study);
        attributes.addFlashAttribute("message", "인원 모집을 종료합니다.");
        return "redirect:/study/" + encode(path) + "/settings/study";
    }

    @PostMapping("/study/path")
    public String updateStudyPath(@CurrentUser Account account, @PathVariable String path, @RequestParam String newPath, Model model, RedirectAttributes attributes) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        if (!studyService.isValidPath(newPath)) {
            model.addAttribute(account);
            model.addAttribute(study);
            model.addAttribute("studyPathError", "사용할 수 없는 스터디 경로입니다.");
            return "study/settings/study";
        }

        studyService.updateStudyPath(study, newPath);
        attributes.addFlashAttribute("message", "스터디 경로를 수정하였습니다.");
        return "redirect:/study/" + encode(newPath) + "/settings/study";
    }

    @PostMapping("/study/title")
    public String updateStudyTitle(@CurrentUser Account account, @PathVariable String path, String newTitle,
                                   Model model, RedirectAttributes attributes) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        if (!studyService.isValidTitle(newTitle)) {
            model.addAttribute(account);
            model.addAttribute(study);
            model.addAttribute("studyTitleError", "스터디 이름을 다시 입력하세요.");
            return "study/settings/study";
        }

        studyService.updateStudyTitle(study, newTitle);
        attributes.addFlashAttribute("message", "스터디 이름을 수정했습니다.");
        return "redirect:/study/" + encode(path) + "/settings/study";
    }

    @PostMapping("/study/remove")
    public String removeStudy(@CurrentUser Account account, @PathVariable String path, Model model) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        studyService.remove(study);
        return "redirect:/";
    }
}

```

</details>

## 서비스 수정

컨트롤러에서 위임한 기능들을 구현하기 위해 `StudyService` 클래스를 수정합니다.

`/src/main/java/io/lcalmsky/app/study/application/StudyService.java`

```java
// 생략
public class StudyService {
    // 생략
    public Study getStudy(Account account, String path) { // (1) 
        Study study = studyRepository.findByPath(path);
        checkStudyExists(path, study);
        return study;
    }

    public Study getStudyToUpdate(Account account, String path) { // (1) 
        return getStudy(account, path, studyRepository.findByPath(path));
    }
    // 생략
    public void publish(Study study) { // (2)
        study.publish();
    }

    public void close(Study study) { // (3)
        study.close();
    }

    public void startRecruit(Study study) { // (4)
        study.startRecruit();
    }

    public void stopRecruit(Study study) { // (5)
        study.stopRecruit();
    }

    public boolean isValidPath(String newPath) { // (6) 
        if (!newPath.matches(StudyForm.VALID_PATH_PATTERN)) {
            return false;
        }
        return !studyRepository.existsByPath(newPath);
    }

    public void updateStudyPath(Study study, String newPath) { // (7)
        study.updatePath(newPath);
    }

    public boolean isValidTitle(String newTitle) { // (8)
        return newTitle.length() <= 50;
    }

    public void updateStudyTitle(Study study, String newTitle) { // (9)
        study.updateTitle(newTitle);
    }

    public void remove(Study study) { // (10)
        if (!study.isRemovable()) {
            throw new IllegalStateException("스터디를 삭제할 수 없습니다.");
        }
        studyRepository.delete(study);
    }
}
```

1. 일반 사용자의 접근과 관리자가 수정하기 위해 접근할 때를 구분해 주었습니다.
2. 스터디를 공개합니다.
3. 스터디를 종료합니다.
4. 팀원 모집을 시작합니다.
5. 팀원 모집을 중단합니다.
6. 스터디 경로가 유효한지 판단합니다. 정규표현식을 이용한 패턴 검사는 StudyForm 에서 사용한 패턴을 상수로 추출하여 동일하게 사용하였습니다. 기존에 존재하는 경로를 사용해선 안 되므로 해당 경로를 사용하는 스터디가 있는지도 확인해줍니다.
7. 스터디 경로를 업데이트 합니다.
8. 스터디 아름의 유효성을 검사합니다.
9. 스터디 이름을 업데이트 합니다.
10. 스터디를 삭제합니다.

유효성 검사를 제외하고 스터디의 상태를 바꾸는 내용은 대부분 도메인 `Entity`가 직접 변경할 수 있게 위임하였습니다.

<details>
<summary>StudyService.java 전체 보기</summary>

```java
package io.lcalmsky.app.study.application;

import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.domain.entity.Zone;
import io.lcalmsky.app.study.domain.entity.Study;
import io.lcalmsky.app.study.form.StudyDescriptionForm;
import io.lcalmsky.app.study.form.StudyForm;
import io.lcalmsky.app.study.infra.repository.StudyRepository;
import io.lcalmsky.app.tag.domain.entity.Tag;
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
}

```

</details>

## Entity 수정

위에서 위임한 내용을 `Study` `Entity`에 구현합니다.

`/src/main/java/io/lcalmsky/app/study/domain/entity/Study.java`

```java
// 생략
@NamedEntityGraph(name = "Study.withManagers", attributeNodes = { // (1)
        @NamedAttributeNode("managers")
})
public class Study {
    // 생략
    public void publish() { // (2)
        if (this.closed || this.published) {
            throw new IllegalStateException("스터디를 이미 공개했거나 종료된 스터디 입니다.");
        }
        this.published = true;
        this.publishedDateTime = LocalDateTime.now();
    }

    public void close() { // (3)
        if (!this.published || this.closed) {
            throw new IllegalStateException("스터디를 공개하지 않았거나 이미 종료한 스터디 입니다.");
        }
        this.closed = true;
        this.closedDateTime = LocalDateTime.now();
    }

    public boolean isEnableToRecruit() { // (4)
        return this.published && this.recruitingUpdatedDateTime == null
                || this.recruitingUpdatedDateTime.isBefore(LocalDateTime.now().minusHours(1));
    }

    public void updatePath(String newPath) { // (5)
        this.path = newPath;
    }

    public void updateTitle(String newTitle) { // (6)
        this.title = newTitle;
    }

    public boolean isRemovable() { // (7)
        return !this.published;
    }

    public void startRecruit() { // (8)
        if (!isEnableToRecruit()) {
            throw new RuntimeException("인원 모집을 시작할 수 없습니다. 스터디를 공개하거나 한 시간 뒤 다시 시도하세요.");
        }
        this.recruiting = true;
        this.recruitingUpdatedDateTime = LocalDateTime.now();
    }

    public void stopRecruit() { // (9)
        if (!isEnableToRecruit()) {
            throw new RuntimeException("인원 모집을 멈출 수 없습니다. 스터디를 공개하거나 한 시간 뒤 다시 시도하세요.");
        }
        this.recruiting = false;
        this.recruitingUpdatedDateTime = LocalDateTime.now();
    }
}
```

1. 관리자 정보 fetch join 하도록 NamedEntityGraph를 추가하였습니다.
2. 스터디의 이전 상태를 검사하여 예외를 던지거나 공개된 상태로 변경합니다.
3. 스터디의 이전 상태를 검사하여 예외를 던지거나 종료된 상태로 변경합니다.
4. 팀원 모집이 가능한 상태인지 검사합니다.
5. 스터디 경로를 변경합니다.
6. 스터디 이름을 변경합니다.
7. 스터디를 제거할 수 있는 상태인지 확인합니다.
8. 스터디 상태를 검사하여 예외를 던지거나 팀원 모집 상태로 변경합니다.
9. 스터디 상태를 검사하여 예외를 던지거나 팀원 모집 중단 상태로 변경합니다.

<details>
<summary>Study.java 전체 보기</summary>

```java
package io.lcalmsky.app.study.domain.entity;

import io.lcalmsky.app.account.domain.UserAccount;
import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.domain.entity.Zone;
import io.lcalmsky.app.study.form.StudyDescriptionForm;
import io.lcalmsky.app.study.form.StudyForm;
import io.lcalmsky.app.tag.domain.entity.Tag;
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
        @NamedAttributeNode("managers")
})
@NamedEntityGraph(name = "Study.withZonesAndManagers", attributeNodes = {
        @NamedAttributeNode("zones"),
        @NamedAttributeNode("managers")
})
@NamedEntityGraph(name = "Study.withManagers", attributeNodes = {
        @NamedAttributeNode("managers")
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
}

```

</details>

## Repository 수정

`StudyService`에서 관리자 정보만 포함한 스터디를 조회하기 위해 사용하는 메서드를 `StudyRepository`에 추가해줍니다.

`/src/main/java/io/lcalmsky/app/study/infra/repository/StudyRepository.java`

```java
// 생략
public interface StudyRepository extends JpaRepository<Study, Long> {
    // 생략
    @EntityGraph(value = "Study.withManagers", type = EntityGraph.EntityGraphType.FETCH)
    Study findStudyWithManagersByPath(String path);
}
```

메서드를 추가하고 `Study Entity`에 추가한 `NamedEntityGraph`를 `EntityGraph`로 설정해주었습니다.

<details>
<summary>StudyRepository.java 전체 보기</summary>

```java
package io.lcalmsky.app.study.infra.repository;

import io.lcalmsky.app.study.domain.entity.Study;
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

    @EntityGraph(value = "Study.withManagers", type = EntityGraph.EntityGraphType.FETCH)
    Study findStudyWithManagersByPath(String path);
}
```

</details>

## StudyForm 수정

`StudyService`에서 스터디 경로의 유효성을 확인하기 위한 부분에서 사용했던 정규표현식 패턴을 상수로 변경해 외부에서 접근할 수 있게 하였습니다.

`/src/main/java/io/lcalmsky/app/study/form/StudyForm.java`

```java
// 생략
public class StudyForm {

    public static final String VALID_PATH_PATTERN = "^[ㄱ-ㅎ가-힣a-z0-9_-]{2,20}$";

    @NotBlank
    @Length(min = 2, max = 20)
    @Pattern(regexp = VALID_PATH_PATTERN)
    private String path;
    // 생략
}

```

<details>
<summary>StudyForm.java 전체 보기</summary>

```java
package io.lcalmsky.app.study.form;

import lombok.*;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyForm {

    public static final String VALID_PATH_PATTERN = "^[ㄱ-ㅎ가-힣a-z0-9_-]{2,20}$";

    @NotBlank
    @Length(min = 2, max = 20)
    @Pattern(regexp = VALID_PATH_PATTERN)
    private String path;

    @NotBlank
    @Length(max = 50)
    private String title;

    @NotBlank
    @Length(max = 100)
    private String shortDescription;

    @NotBlank
    private String fullDescription;
}

```

</details>

## 뷰 작성

스터디 설정 내 스터디 메뉴 화면을 작성합니다.

`/src/main/resources/templates/study/settings/study.html`

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
                <div th:replace="fragments.html :: study-settings-menu(currentMenu='study')"></div>
            </div>
            <div class="col-8">
                <div th:replace="fragments.html :: message"></div>
                <div class="row">
                    <h5 class="col-sm-12">스터디 공개 및 종료</h5>
                    <form th:if="${!study.published && !study.closed}" class="col-sm-12" action="#"
                          th:action="@{'/study/' + ${study.getPath()} + '/settings/study/publish'}" method="post"
                          novalidate>
                        <div class="alert alert-info" role="alert">
                            <svg th:replace="fragments.html::symbol-info"/>
                            스터디를 다른 사용자에게 공개할 준비가 되었다면 버튼을 클릭하세요.<br/>
                            소개, 배너 이미지, 스터디 주제 및 활동 지역을 등록했는지 확인하세요.<br/>
                            스터디를 공개하면 주요 활동 지역과 스터디 주제에 관심있는 다른 사용자에게 알림을 전송합니다.
                        </div>
                        <div class="form-group">
                            <button class="btn btn-outline-primary" type="submit" aria-describedby="submitHelp">스터디 공개
                            </button>
                        </div>
                    </form>
                    <form th:if="${study.published && !study.closed}" class="col-sm-12" action="#"
                          th:action="@{'/study/' + ${study.getPath()} + '/settings/study/close'}" method="post"
                          novalidate>
                        <div class="alert alert-warning" role="alert">
                            <svg th:replace="fragments.html::symbol-warning"/>
                            스터디 활동을 마쳤다면 스터디를 종료하세요.<br/>
                            스터디를 종료하면 더이상 팀원을 모집하거나 모임을 만들 수 없으며, 스터디 경로와 이름을 수정할 수 없습니다.<br/>
                            스터디 모임과 참여한 팀원의 기록은 그대로 보관합니다.
                        </div>
                        <div class="form-group">
                            <button class="btn btn-outline-warning" type="submit" aria-describedby="submitHelp">스터디 종료
                            </button>
                        </div>
                    </form>
                    <div th:if="${study.closed}" class="col-sm-12 alert alert-info">
                        이 스터디는 <span class="date-time" th:text="${study.closedDateTime}"></span>에 종료됐습니다.<br/>
                        다시 스터디를 진행하고 싶다면 새로운 스터디를 만드세요.<br/>
                    </div>
                </div>

                <hr th:if="${!study.closed && !study.recruiting && study.published}"/>
                <div class="row" th:if="${!study.closed && !study.recruiting && study.published}">
                    <h5 class="col-sm-12">팀원 모집</h5>
                    <form class="col-sm-12" action="#"
                          th:action="@{'/study/' + ${study.getPath()} + '/settings/recruit/start'}" method="post"
                          novalidate>
                        <div class="alert alert-info" role="alert">
                            <svg th:replace="fragments.html::symbol-info"/>
                            팀원을 모집합니다.<br/>
                            충분한 스터디 팀원을 모집했다면 모집을 멈출 수 있습니다.<br/>
                            팀원 모집 정보는 1시간에 한번만 바꿀 수 있습니다.
                        </div>
                        <div class="form-group">
                            <button class="btn btn-outline-primary" type="submit" aria-describedby="submitHelp">
                                팀원 모집 시작
                            </button>
                        </div>
                    </form>
                </div>

                <hr th:if="${!study.closed && study.recruiting && study.published}"/>
                <div class="row" th:if="${!study.closed && study.recruiting && study.published}">
                    <h5 class="col-sm-12">팀원 모집</h5>
                    <form class="col-sm-12" action="#"
                          th:action="@{'/study/' + ${study.getPath()} + '/settings/recruit/stop'}" method="post"
                          novalidate>
                        <div class="alert alert-primary" role="alert">
                            <svg th:replace="fragments.html::symbol-info"/>
                            팀원 모집을 중답합니다.<br/>
                            팀원 충원이 필요할 때 다시 팀원 모집을 시작할 수 있습니다.<br/>
                            팀원 모집 정보는 1시간에 한번만 바꿀 수 있습니다.
                        </div>
                        <div class="form-group">
                            <button class="btn btn-outline-primary" type="submit" aria-describedby="submitHelp">
                                팀원 모집 중단
                            </button>
                        </div>
                    </form>
                </div>

                <hr th:if="${!study.closed}"/>
                <div class="row" th:if="${!study.closed}">
                    <h5 class="col-sm-12">스터디 경로</h5>
                    <form class="col-sm-12 needs-validation" action="#"
                          th:action="@{'/study/' + ${study.path} + '/settings/study/path'}" method="post" novalidate>
                        <div class="alert alert-warning" role="alert">
                            <svg th:replace="fragments.html::symbol-warning"/>
                            스터디 경로를 수정하면 이전에 사용하던 경로로 스터디에 접근할 수 없으니 주의하세요. <br/>
                        </div>
                        <div class="form-group">
                            <input id="path" type="text" name="newPath" th:value="${newPath}" class="form-control"
                                   placeholder="예) study-path" aria-describedby="pathHelp" required>
                            <small id="pathHelp" class="form-text text-muted">
                                공백없이 문자, 숫자, 대시(-)와 언더바(_)만 3자 이상 20자 이내로 입력하세요. 스터디 홈 주소에 사용합니다. 예) /study/<b>study-path</b>
                            </small>
                            <small class="invalid-feedback">스터디 경로를 입력하세요.</small>
                            <small class="form-text text-danger" th:if="${studyPathError}" th:text="${studyPathError}">Path
                                Error</small>
                        </div>
                        <div class="form-group mt-3">
                            <button class="btn btn-outline-warning" type="submit" aria-describedby="submitHelp">경로 수정
                            </button>
                        </div>
                    </form>
                </div>

                <hr th:if="${!study.closed}"/>
                <div class="row" th:if="${!study.closed}">
                    <h5 class="col-12">스터디 이름</h5>
                    <form class="needs-validation col-12" action="#"
                          th:action="@{'/study/' + ${study.path} + '/settings/study/title'}" method="post" novalidate>
                        <div class="alert alert-warning" role="alert">
                            <svg th:replace="fragments.html::symbol-warning"/>
                            스터디 이름을 수정합니다.<br/>
                        </div>
                        <div class="form-group">
                            <label for="title">스터디 이름</label>
                            <input id="title" type="text" name="newTitle" th:value="${study.title}" class="form-control"
                                   placeholder="스터디 이름" aria-describedby="titleHelp" required maxlength="50">
                            <small id="titleHelp" class="form-text text-muted">
                                스터디 이름을 50자 이내로 입력하세요.
                            </small>
                            <small class="invalid-feedback">스터디 이름을 입력하세요.</small>
                            <small class="form-text text-danger" th:if="${studyTitleError}"
                                   th:text="${studyTitleError}">Title Error</small>
                        </div>
                        <div class="form-group mt-3">
                            <button class="btn btn-outline-warning" type="submit" aria-describedby="submitHelp">스터디 이름
                                수정
                            </button>
                        </div>
                    </form>
                </div>

                <hr/>
                <div class="row">
                    <h5 class="col-sm-12 text-danger">스터디 삭제</h5>
                    <form class="col-sm-12" action="#"
                          th:action="@{'/study/' + ${study.getPath()} + '/settings/study/remove'}" method="post"
                          novalidate>
                        <div class="alert alert-danger" role="alert">
                            <svg th:replace="fragments.html::symbol-danger"/>
                            스터디를 삭제하면 스터디 관련 모든 기록을 삭제하며 복구할 수 없습니다. <br/>
                            <b>다음에 해당하는 스터디는 자동으로 삭제 됩니다.</b>
                            <ul>
                                <li>만든지 1주일이 지난 비공개 스터디</li>
                                <li>스터디 공개 이후, 한달 동안 모임을 만들지 않은 스터디</li>
                                <li>스터디 공개 이후, 모임을 만들지 않고 종료한 스터디</li>
                            </ul>
                        </div>
                        <div class="form-group">
                            <button class="btn btn-outline-danger" type="submit" aria-describedby="submitHelp">스터디 삭제
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
        <div th:replace="fragments.html :: footer"></div>
    </div>
    <script th:replace="fragments.html :: tooltip"></script>
    <script th:replace="fragments.html :: form-validation"></script>
</body>
</html>
```

> 기존 fragments.html 파일에 오타가 있어 수정한 부분입니다.
> * text-right -> text-end로 수정
> * <i class>a fa-plus"> -> <i class="fa fa-plus"> 오타 수정

## 테스트

애플리케이션 실행 후 스터디 설정 내 스터디 메뉴에 진입해 구현한 내용을 테스트합니다.

먼저 화면에 진입합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/44-01.png)

전체적인 뷰가 제대로 구현되었고, 상태를 나타내는 아이콘도 `DRAFT`, `OFF`를 나타내고 있습니다.

스터디 공개 버튼을 클릭하고 변화를 확인합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/44-02.png)

우측 상단에 `DRAFT` 아이콘이 사라졌고 스터디가 공개되었다는 안내 문구가 노출되고 버튼도 스터디 종료로 변경되었습니다.

다음으로는 팀원 모집 시작 버튼을 클릭하고 변화를 확인합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/44-03.png)

설명과 버튼에 변화가 있는 것을 확인할 수 있습니다.

다음으로 스터디 경로를 변경해보겠습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/44-04.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/44-05.png)

주소 표시줄을 확인해보면 실제 경로가 변경된 것을 확인할 수 있습니다.

> 테스트 이후 다시 `spring-boot`로 원복하였습니다.

이름 변경을 테스트한 결과도 마찬가지로 잘 적용되었습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/44-06.png)

> 테스트 이후 다시 `스프링 부트 스터디`로 원복하였습니다.

스터디 삭제를 테스트하기 전에 새로운 계정을 만들어서 스터디의 상태를 확인해보겠습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/44-07.png)

스터디 가입 버튼이 노출되어있는 것을 확인할 수 있습니다. (아직 스터디 가입 기능은 구현되어있지 않습니다.)

마지막으로, 스터디 삭제 기능을 테스트하기 위해 임시로 스터디 하나를 생성해보도록 하겠습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/44-08.png)

생성한 뒤 다시 스터디 설정 메뉴로 들어가서 삭제 버튼을 클릭하면

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/44-09.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/44-10.png)

삭제된 뒤 홈으로 돌아간 것을 확인할 수 있습니다.

## 테스트 코드 작성

계속 반복되는 내용이므로 소스 코드로 대체합니다.

`/src/test/java/io/lcalmsky/app/study/endpoint/StudySettingsControllerTest.java`

```java
// 생략
class StudySettingsControllerTest {
    // 생략
    @Test
    @DisplayName("스터디 세팅 폼 조회(스터디)")
    @WithAccount("jaime")
    void studySettingFormStudy() throws Exception {
        mockMvc.perform(get("/study/" + studyPath + "/settings/study"))
                .andExpect(status().isOk())
                .andExpect(view().name("study/settings/study"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("study"));
    }

    @Test
    @DisplayName("스터디 공개")
    @WithAccount("jaime")
    void publishStudy() throws Exception {
        mockMvc.perform(post("/study/" + studyPath + "/settings/study/publish")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/settings/study"))
                .andExpect(flash().attributeExists("message"));
        Study study = studyRepository.findByPath(studyPath);
        assertTrue(study.isPublished());
    }

    @Test
    @DisplayName("스터디 종료")
    @WithAccount("jaime")
    void closeStudy() throws Exception {
        Study study = studyRepository.findByPath(studyPath);
        studyService.publish(study);
        mockMvc.perform(post("/study/" + studyPath + "/settings/study/close")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/settings/study"))
                .andExpect(flash().attributeExists("message"));
        assertTrue(study.isClosed());
    }

    @Test
    @DisplayName("스터디 팀원 모집 시작")
    @WithAccount("jaime")
    void startRecruit() throws Exception {
        Study study = studyRepository.findByPath(studyPath);
        studyService.publish(study);
        mockMvc.perform(post("/study/" + studyPath + "/settings/recruit/start")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/settings/study"))
                .andExpect(flash().attributeExists("message"));
        assertTrue(study.isRecruiting());
    }

    @Test
    @DisplayName("스터디 팀원 모집 중지: 1시간 이내 시도 -> 실패")
    @WithAccount("jaime")
    void stopRecruit() throws Exception {
        Study study = studyRepository.findByPath(studyPath);
        studyService.publish(study);
        studyService.startRecruit(study);
        mockMvc.perform(post("/study/" + studyPath + "/settings/recruit/stop")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/settings/study"))
                .andExpect(flash().attributeExists("message"));
        assertTrue(study.isRecruiting());
    }

    @Test
    @DisplayName("스터디 경로 변경")
    @WithAccount("jaime")
    void updateStudyPath() throws Exception {
        String newPath = "new-path";
        mockMvc.perform(post("/study/" + studyPath + "/settings/study/path")
                        .param("newPath", newPath)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + newPath + "/settings/study"))
                .andExpect(flash().attributeExists("message"));
        Study study = studyRepository.findByPath(newPath);
        assertEquals(newPath, study.getPath());
    }

    @Test
    @DisplayName("스터디 이름 변경")
    @WithAccount("jaime")
    void updateStudyTitle() throws Exception {
        String newTitle = "newTitle";
        mockMvc.perform(post("/study/" + studyPath + "/settings/study/title")
                        .param("newTitle", newTitle)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/settings/study"))
                .andExpect(flash().attributeExists("message"));
        Study study = studyRepository.findByPath(studyPath);
        assertEquals(newTitle, study.getTitle());
    }

    @Test
    @DisplayName("스터디 삭제")
    @WithAccount("jaime")
    void removeStudy() throws Exception {
        mockMvc.perform(post("/study/" + studyPath + "/settings/study/remove")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        assertNull(studyRepository.findByPath(studyPath));
    }
}
```

<details>
<summary>StudySettingsControllerTest.java 전체 보기</summary>

```java
package io.lcalmsky.app.study.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lcalmsky.app.WithAccount;
import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.domain.entity.Zone;
import io.lcalmsky.app.account.infra.repository.AccountRepository;
import io.lcalmsky.app.settings.controller.TagForm;
import io.lcalmsky.app.settings.controller.ZoneForm;
import io.lcalmsky.app.study.application.StudyService;
import io.lcalmsky.app.study.domain.entity.Study;
import io.lcalmsky.app.study.form.StudyForm;
import io.lcalmsky.app.study.infra.repository.StudyRepository;
import io.lcalmsky.app.tag.domain.entity.Tag;
import io.lcalmsky.app.tag.infra.repository.TagRepository;
import io.lcalmsky.app.zone.infra.repository.ZoneRepository;
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
        Study study = studyService.getStudyToUpdate(account, studyPath);
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

    @Test
    @DisplayName("스터디 세팅 폼 조회(스터디)")
    @WithAccount("jaime")
    void studySettingFormStudy() throws Exception {
        mockMvc.perform(get("/study/" + studyPath + "/settings/study"))
                .andExpect(status().isOk())
                .andExpect(view().name("study/settings/study"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("study"));
    }

    @Test
    @DisplayName("스터디 공개")
    @WithAccount("jaime")
    void publishStudy() throws Exception {
        mockMvc.perform(post("/study/" + studyPath + "/settings/study/publish")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/settings/study"))
                .andExpect(flash().attributeExists("message"));
        Study study = studyRepository.findByPath(studyPath);
        assertTrue(study.isPublished());
    }

    @Test
    @DisplayName("스터디 종료")
    @WithAccount("jaime")
    void closeStudy() throws Exception {
        Study study = studyRepository.findByPath(studyPath);
        studyService.publish(study);
        mockMvc.perform(post("/study/" + studyPath + "/settings/study/close")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/settings/study"))
                .andExpect(flash().attributeExists("message"));
        assertTrue(study.isClosed());
    }

    @Test
    @DisplayName("스터디 팀원 모집 시작")
    @WithAccount("jaime")
    void startRecruit() throws Exception {
        Study study = studyRepository.findByPath(studyPath);
        studyService.publish(study);
        mockMvc.perform(post("/study/" + studyPath + "/settings/recruit/start")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/settings/study"))
                .andExpect(flash().attributeExists("message"));
        assertTrue(study.isRecruiting());
    }

    @Test
    @DisplayName("스터디 팀원 모집 중지: 1시간 이내 시도 -> 실패")
    @WithAccount("jaime")
    void stopRecruit() throws Exception {
        Study study = studyRepository.findByPath(studyPath);
        studyService.publish(study);
        studyService.startRecruit(study);
        mockMvc.perform(post("/study/" + studyPath + "/settings/recruit/stop")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/settings/study"))
                .andExpect(flash().attributeExists("message"));
        assertTrue(study.isRecruiting());
    }

    @Test
    @DisplayName("스터디 경로 변경")
    @WithAccount("jaime")
    void updateStudyPath() throws Exception {
        String newPath = "new-path";
        mockMvc.perform(post("/study/" + studyPath + "/settings/study/path")
                        .param("newPath", newPath)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + newPath + "/settings/study"))
                .andExpect(flash().attributeExists("message"));
        Study study = studyRepository.findByPath(newPath);
        assertEquals(newPath, study.getPath());
    }

    @Test
    @DisplayName("스터디 이름 변경")
    @WithAccount("jaime")
    void updateStudyTitle() throws Exception {
        String newTitle = "newTitle";
        mockMvc.perform(post("/study/" + studyPath + "/settings/study/title")
                        .param("newTitle", newTitle)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPath + "/settings/study"))
                .andExpect(flash().attributeExists("message"));
        Study study = studyRepository.findByPath(studyPath);
        assertEquals(newTitle, study.getTitle());
    }

    @Test
    @DisplayName("스터디 삭제")
    @WithAccount("jaime")
    void removeStudy() throws Exception {
        mockMvc.perform(post("/study/" + studyPath + "/settings/study/remove")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        assertNull(studyRepository.findByPath(studyPath));
    }
}
```

</details>