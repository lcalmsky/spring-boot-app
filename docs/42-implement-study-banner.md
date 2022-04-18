![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 1c525c5)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 1c525c5
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

스터디 배너 이미지 사용 여부와 사용할 경우 이미지를 변경할 수 있는 기능을 구현합니다.

## 엔드포인트 추가

배너 관련 페이지 진입 및 업데이트를 할 수 있도록 `StudySettingsController` 클래스에 엔드포인트를 추가합니다.

`/src/main/java/io/lcalmsky/app/study/endpoint/StudySettingsController.java`

```java
// 생략
@Controller
@RequestMapping("/study/{path}/settings")
@RequiredArgsConstructor
public class StudySettingsController {
    // 생략
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
}
```

<details>
<summary>StudySettingsController.java 전체 보기</summary>

```java
package io.lcalmsky.app.study.endpoint;

import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.support.CurrentUser;
import io.lcalmsky.app.study.application.StudyService;
import io.lcalmsky.app.study.domain.entity.Study;
import io.lcalmsky.app.study.form.StudyDescriptionForm;
import io.lcalmsky.app.study.infra.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/study/{path}/settings")
@RequiredArgsConstructor
public class StudySettingsController {
    private final StudyRepository studyRepository;
    private final StudyService studyService;

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
}
```

## 서비스 수정

위 컨트롤러에서 트랜잭션을 사용하기위해 서비스에 위임한 부분을 구현합니다.

`/src/main/java/io/lcalmsky/app/study/application/StudyService.java`

```java
// 생략
@Service
@RequiredArgsConstructor
@Transactional
public class StudyService {
    // 생략
    public void updateStudyImage(Study study, String image) {
        study.updateImage(image);
    }

    public void enableStudyBanner(Study study) {
        study.setBanner(true);
    }

    public void disableStudyBanner(Study study) {
        study.setBanner(false);
    }
}
```

</details>

<details>
<summary>StudyService.java 전체 보기</summary>

```java
package io.lcalmsky.app.study.application;

import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.study.domain.entity.Study;
import io.lcalmsky.app.study.form.StudyDescriptionForm;
import io.lcalmsky.app.study.form.StudyForm;
import io.lcalmsky.app.study.infra.repository.StudyRepository;
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
        Study study = getStudy(path);
        if (!account.isManagerOf(study)) {
            throw new AccessDeniedException("해당 기능을 사용할 수 없습니다.");
        }
        return study;
    }

    private Study getStudy(String path) {
        Study study = studyRepository.findByPath(path);
        if (study == null) {
            throw new IllegalArgumentException(path + "에 해당하는 스터디가 없습니다.");
        }
        return study;
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
}
```

</details>

## Entity 수정

이미지를 업데이트 하는 부분과 배너 사용 유무를 업데이트 하는 부분을 `Study Entity`에 추가해줍니다.

기존에 배너 사용 유무를 조회하는 필드에도 `isUseBanner` 메서드가 생성되는 것을 방지하기 위해 `@Accessors` 애너테이션을 추가하였습니다.

`/src/main/java/io/lcalmsky/app/study/domain/entity/Study.java`

```java
// 생략
@Entity
@NamedEntityGraph(name = "Study.withAll", attributeNodes = {
        @NamedAttributeNode("tags"),
        @NamedAttributeNode("zones"),
        @NamedAttributeNode("managers"),
        @NamedAttributeNode("members")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Study {
    // 생략
    @Accessors(fluent = true)
    private boolean useBanner;
    // 생략
    public void updateImage(String image) {
        this.image = image;
    }

    public void setBanner(boolean useBanner) {
        this.useBanner = useBanner;
    }
}
```

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
}
```

</details>

## 뷰 작성

스터디 설정 화면에서 배너로 진입했을 때 뷰를 작성합니다.

지난 포스팅 때 `fragments`에 배너 관련된 것들을 미리 추가해뒀기 때문에 그 부분은 생략하도록 하겠습니다.

`/src/main/resources/templates/study/settings/banner.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<body>
<nav th:replace="fragments.html :: navigation-bar"></nav>
<div th:replace="fragments.html :: study-banner"></div>
<svg th:replace="fragments.html :: svg-symbols"/>
<div class="container">
    <div th:replace="fragments.html :: study-info"></div>
    <div th:replace="fragments.html :: study-menu(studyMenu='settings')"></div>
    <div class="row mt-3 justify-content-center">
        <div class="col-2">
            <div th:replace="fragments.html :: study-settings-menu(currentMenu='image')"></div>
        </div>
            <div class="col-8">
                <div th:replace="fragments.html :: message"></div>
                <div class="row">
                    <h2 class="col-sm-12">배너 이미지 사용</h2>
                </div>
                <form th:if="${!study.useBanner}" action="#" th:action="@{'/study/' + ${study.getPath()} + '/settings/banner/enable'}" method="post" novalidate>
                    <div class="alert alert-primary" role="alert">
                        <svg th:replace="fragments::symbol-info" />
                        스터디 메뉴에서 스터디 배너 이미지를 사용합니다. 스터디 배너 이미지를 설정하지 않았다면 기본 배너 이미지를 사용합니다.
                    </div>
                    <div class="form-group">
                        <button class="btn btn-outline-primary btn-block" type="submit" aria-describedby="submitHelp">배너 이미지 사용하기</button>
                    </div>
                </form>
                <form th:if="${study.useBanner}" action="#" th:action="@{'/study/' + ${study.getPath()} + '/settings/banner/disable'}" method="post" novalidate>
                    <div class="alert alert-primary" role="alert">
                        <svg th:replace="fragments::symbol-info" />
                        스터디 메뉴에서 스터디 배너 이미지를 사용하지 않습니다. 스터디 목록에서는 배너 이미지를 사용합니다.
                    </div>
                    <div class="form-group">
                        <button class="btn btn-outline-primary btn-block" type="submit" aria-describedby="submitHelp">배너 이미지 사용하지 않기</button>
                    </div>
                </form>
                <hr/>
                <div class="row">
                    <h2 class="col-sm-12">배너 이미지 변경</h2>
                </div>
                <form id="imageForm" action="#" th:action="@{'/study/' + ${study.getPath()} + '/settings/banner'}" method="post" novalidate>
                    <div class="form-group">
                        <input id="studyImage" type="hidden" name="image" class="form-control" />
                    </div>
                </form>
                <div class="card text-center">
                    <div id="current-study-image" class="mt-3">
                        <img class="rounded" th:src="${study.image}" width="640" alt="name" th:alt="${study.title}"/>
                    </div>
                    <div id="new-study-image" class="mt-3"></div>
                    <div class="card-body">
                        <div class="custom-file">
                            <input type="file" class="form-control" id="study-image-file">
                        </div>
                        <div id="new-profile-image-control" class="mt-3 d-grid gap-2">
                            <button class="btn btn-outline-primary" id="cut-button">자르기</button>
                            <button class="btn btn-outline-success" id="confirm-button">확인</button>
                            <button class="btn btn-primary" id="save-button">저장</button>
                            <button class="btn btn-outline-warning" id="reset-button">취소</button>
                        </div>
                        <div id="cropped-new-profile-image" class="mt-3"></div>
                    </div>
                </div>
            </div>
        </div>
        <div th:replace="fragments.html :: footer"></div>
    </div>
    <script th:replace="fragments.html :: tooltip"></script>
    <link  href="/node_modules/cropper/dist/cropper.min.css" rel="stylesheet">
    <script src="/node_modules/cropper/dist/cropper.min.js"></script>
    <script src="/node_modules/jquery-cropper/dist/jquery-cropper.min.js"></script>
    <script type="application/javascript">
        $(function() {
            cropper = '';
            let $confirmBtn = $("#confirm-button");
            let $resetBtn = $("#reset-button");
            let $cutBtn = $("#cut-button");
            let $saveBtn = $("#save-button");
            let $newStudyImage = $("#new-study-image");
            let $currentStudyImage = $("#current-study-image");
            let $resultImage = $("#cropped-new-study-image");
            let $studyImage = $("#studyImage");

            $newStudyImage.hide();
            $cutBtn.hide();
            $resetBtn.hide();
            $confirmBtn.hide();
            $saveBtn.hide();

            $("#study-image-file").change(function(e) {
                if (e.target.files.length === 1) {
                    const reader = new FileReader();
                    reader.onload = e => {
                        if (e.target.result) {
                            if (!e.target.result.startsWith("data:image")) {
                                alert("이미지 파일을 선택하세요.");
                                return;
                            }

                            let img = document.createElement("img");
                            img.id = 'new-study';
                            img.src = e.target.result;
                            img.setAttribute('width', '100%');

                            $newStudyImage.html(img);
                            $newStudyImage.show();
                            $currentStudyImage.hide();

                            let $newImage = $(img);
                            $newImage.cropper({aspectRatio: 13/2});
                            cropper = $newImage.data('cropper');

                            $cutBtn.show();
                            $confirmBtn.hide();
                            $resetBtn.show();
                        }
                    };

                    reader.readAsDataURL(e.target.files[0]);
                }
            });

            $resetBtn.click(function() {
                $currentStudyImage.show();
                $newStudyImage.hide();
                $resultImage.hide();
                $resetBtn.hide();
                $cutBtn.hide();
                $confirmBtn.hide();
                $saveBtn.hide();
                $studyImage.val('');
            });

            $cutBtn.click(function () {
                let dataUrl = cropper.getCroppedCanvas().toDataURL();

                if (dataUrl.length > 1000 * 2048) {
                    alert("이미지 파일이 너무 큽니다. 2MB 보다 작은 파일을 사용하세요. 현재 이미지 사이즈 " + dataUrl.length);
                    return;
                }

                let newImage = document.createElement("img");
                newImage.id = "cropped-new-study-image";
                newImage.src = dataUrl;
                newImage.width = 640;
                $resultImage.html(newImage);
                $resultImage.show();
                $confirmBtn.show();

                $confirmBtn.click(function () {
                    $newStudyImage.html(newImage);
                    $cutBtn.hide();
                    $confirmBtn.hide();
                    $studyImage.val(dataUrl);
                    $saveBtn.show();
                });
            });

            $saveBtn.click(function() {
                $("#imageForm").submit();
            })
        });
    </script>
</body>
</html>
```

프로필 이미지 수정할 때의 내용과 유사합니다.

배너 이미지는 프로필 이미지보다 크기가 훨씬 크므로 프론트엔드에서 사이즈를 체크하는 기능이 추가되었습니다.

## 설정 수정

서버에서 요청의 크기를 제한할 수 있는 설정 값이 있는데 `server.tomcat.max-http-form-post-size`이 키 값을 적절하게 수정해줍니다.

기본 값이 2MB이므로 2MB 이하의 이미지를 받기에는 큰 문제가 없으나 이 때 사이즈는 서버에서 수신하는 요청 전체 크기이기 때문에 넉넉하게 5MB로 수정하였습니다.

`local-db` 설정으로 실행할 것이므로 `local-db` 설정 파일을 수정하였습니다.

`/src/main/resources/application-local-db.yml`

```yaml
# 생략
server:
  tomcat:
    max-http-form-post-size: 5MB
```

## 테스트

애플리케이션을 실행한 뒤 `study` 화면으로 진입합니다.

> 저는 사전에 `spring-boot` path를 가지는 스터디를 생성해 두었습니다.  
> http://localhost:8080/study/spring-boot

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/42-01.png)

설정 - 배너이미지로 진입하여 프로필 이미지를 첨부했던 것처럼 파일을 업로드합니다.

파일 크기가 너무 클 때 잘라낸 파일 기준으로도 2MB가 넘을 경우 에러 문구가 잘 노출 되는지, 자르기, 저장, 확인, 취소 버튼이 상황에 맞게 잘 나타나는지 확인합니다.

아래는 모든 확인을 마친 후 이미지를 잘라내 저장하기까지 진행한 화면입니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/42-02.png)

위에 배너 이미지 사용하기 버튼을 클릭하여 배너 이미지가 정상적으로 적용되는지 확인합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/42-03.png)

사용하지 않기를 클릭했을 때 배너 이미지가 사라지는지도 확인합니다.

> 두 번째 이미지와 동일하므로 캡쳐는 따로 추가하지 않았습니다.

## 테스트 코드 작성

배너 이미지 수정 화면 조회, 수정, 사용 여부 변경 등을 테스트할 수 있도록 테스트 케이스들을 작성합니다.

```java
package io.lcalmsky.app.study.endpoint;

import io.lcalmsky.app.WithAccount;
import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.infra.repository.AccountRepository;
import io.lcalmsky.app.study.application.StudyService;
import io.lcalmsky.app.study.domain.entity.Study;
import io.lcalmsky.app.study.form.StudyForm;
import io.lcalmsky.app.study.infra.repository.StudyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
    @Autowired StudyService studyService;
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
    
    // 생략

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
}
```

<details>
<summary>StudySettingsControllerTest.java 전체 보기</summary>

```java
package io.lcalmsky.app.study.endpoint;

import io.lcalmsky.app.WithAccount;
import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.infra.repository.AccountRepository;
import io.lcalmsky.app.study.application.StudyService;
import io.lcalmsky.app.study.domain.entity.Study;
import io.lcalmsky.app.study.form.StudyForm;
import io.lcalmsky.app.study.infra.repository.StudyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
    @Autowired StudyService studyService;
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
}
```

</details>

기존 테스트를 포함하여 모든 테스트에 통과하였습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/42-04.png)