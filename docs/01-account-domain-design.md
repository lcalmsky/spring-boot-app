![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app/) 있습니다. (branch: feature/2)

---

## Overview

이번 포스팅에서는 회원 가입, 탈퇴, 로그인 등의 기능을 개발하기 위한 도메인 설계와 이에 해당하는 부분을 구현할 예정입니다.

제작할 웹 애플리케이션은 스터디를 관리하는 웹 입니다.

## Requirement

스터디 관리를 위해 가장 선행되어야 할 것은 회원을 관리하는 것인데요, 회원과 관련된 필요한 기능을 정리하면 아래와 같습니다.

* 로그인
* 프로필 (TMI: 독일어, 프랑스어로 읽으면 프로필, 영어로 읽으면 프로파일 입니다)
* 알림

위 세 가지 기능을 구현하기 위해 필요한 데이터를 정리해보겠습니다.

### 로그인

로그인을 하기 위해선 보통 ID, 비밀번호, 이메일 등을 저장하는데요, 구현할 기능에 필요한 항목들을 정의하면 아래와 같습니다.

* email: 아이디 대신 사용, 유니크 해야함
* nickname: 아이디 대신 사용, 유니크 해야함, 다른 사람에게 노출
* password: 비밀번호
* whether certified: 인증 여부
* email token: 이메일 토큰

### Profile

프로필에 사용할 항목입니다.

* bio: 개인적인 정보를 추가하기위한 항목(github에서 bio 참조)
* urls: 개인이 운영하는 웹 페이지 url
* job: 직업
* location: 위치
* company: 회사
* image: 프로필에 사용할 이미지

### Notification

알람 설정에 사용할 항목입니다.

* created: 스터디 생성 알람
* joined: 스터디 참여 알람
* updated: 스터디 업데이트 알람

## 구현

### Entity & Converter

위의 데이터들을 종합하여 `Account`라는 `Entity`를 구현합니다.

```java
package io.lcalmsky.server.account.domain.entity;

import io.lcalmsky.server.account.domain.support.ListStringConverter;
import io.lcalmsky.server.domain.entity.AuditingEntity;
import lombok.*;

import javax.persistence.*;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)  // (1)
@Builder @Getter @ToString                                                                              // (1)
public class Account extends AuditingEntity {

    @Id @GeneratedValue
    @Column(name = "account_id")
    private Long id;                                                                                    // (2)

    @Column(unique = true)                                                                              // (3)
    private String email;

    @Column(unique = true)                                                                              // (3)
    private String nickname;

    private String password;

    private boolean isValid;

    private String emailToken;

    @Embedded                                                                                           // (4)
    private Profile profile;

    @Embedded                                                                                           // (4)
    private NotificationSetting notificationSetting;

    @Embeddable                                                                                         // (5)
    @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder @Getter @ToString
    public static class Profile {
        private String bio;
        @Convert(converter = ListStringConverter.class)                                                 // (6)
        private List<String> url;
        private String job;
        private String location;
        private String company;
        @Lob @Basic(fetch = FetchType.EAGER)
        private String image;
    }

    @Embeddable                                                                                         // (5)
    @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder @Getter @ToString
    public static class NotificationSetting {
        private boolean studyCreatedByEmail;
        private boolean studyCreatedByWeb;
        private boolean studyRegistrationResultByEmailByEmail;
        private boolean studyRegistrationResultByEmailByWeb;
        private boolean studyUpdatedByEmail;
        private boolean studyUpdatedByWeb;
    }
}
```

(1) `Entity`는 직렬화를 위해 반드시 기본 생성자가 존재해야 합니다. 외부에서 `new` 할 수 없도록 `protected` 레벨로 생성자를 선언하였습니다. `@Builder`를 사용하기 위해서는 마찬가지로 생성자가 필요한데, 모든 필드를 다 받을 수 있는 생성자(`@AllArgsConstructor`)를 마찬가지로 `protected` 레벨로 선언하였습니다. 값을 조작할 수 없게 `@Setter`는 사용하지 않았고 `@Getter`와 `@ToString`만 사용하였습니다. 값을 조작해야한다면 그 때 그 때 새로운 메서드를 통해 조작하게 할 예정입니다.  
(2) 여기서 사용할 `ID`는 가입시 입력하는 값이 아니라 DB 내부에서 사용하기 위한 시퀀스 값 입니다.  
(3) `@Column`의 속성 중 `unique` 값을 추가해 고유의 값만 추가할 수 있도록 하였습니다.  
(4) `@Embedded` 애너테이션을 사용하면 해당 클래스의 필드들이 DB에서는 개별 컬럼에 매핑됩니다.  
(5) `@Embedded`와 매핑되는 에너테이션으로 해당 클래스가 개별 `Entity`가 아닌 다른 `Entity`에 귀속될 수 있음을 의미합니다.  
(6) `List`를 `DB` 컬럼 하나에 매핑하기 위해 `Converter`를 사용하였습니다.

```java
package io.lcalmsky.server.account.domain.support;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Converter
public class ListStringConverter implements AttributeConverter<List<String>, String> {
    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        return Optional.ofNullable(attribute)
                .map(a -> String.join(",", a))
                .orElse("");
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        return Stream.of(dbData.split(","))
                .collect(Collectors.toList());
    }
}
```

(1) `Converter`로 사용할 클래스임을 나타냅니다.  
(2) `AttributeConverter` 인터페이스를 구현해야 합니다.

이렇게 계정 정보를 담을 `Entity`와 `Entity`에서 사용될 `Converter`를 구현해봤습니다.

### Controller & Templates

이제 회원 가입창으로 이동해주기 위한 `Controller`를 작성해보겠습니다.

Controller 작성에 앞서 페이지 파일을 먼저 생성해 줄 건데요, `resources/templates/account/sign-up.html` 이 경로에 회원가입 페이지를 미리 생성해놓겠습니다.

* sign-up.html
```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
</head>
<body>
</body>
</html>
```

그리고 회원 가입 페이지로 redirect 시켜줄 수 있는 `Controller`를 작성합니다.

```java
package io.lcalmsky.server.account.endpoint.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccountController {

    @GetMapping("/sign-up")
    public String signUpForm(Model model) {
        return "account/sign-up";
    }
}
```

여기까지 작성했으면 잘 되는지 확인해봐야겠죠?

테스트 클래스를 작성합니다.

> 💡Tip: macOS + IntelliJ 기준 AccountController 클래스에서 `⌥` + `⏎`를 누르면 테스트 클래스를 자동으로 생성할 수 있습니다.

```java
package io.lcalmsky.server.account.endpoint.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {
    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("회원 가입 화면 진입 확인")
    void signUpForm() throws Exception {
        mockMvc.perform(get("/sign-up"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("account/sign-up"));
    }
}
```

이렇게 작성한 뒤 테스트를 실행해보면,

```text
java.lang.AssertionError: Status expected:<200> but was:<401>
Expected :200
Actual   :401
```

이렇게 에러가 발생하는 것을 확인할 수 있습니다.

에러가 발생한 이유는 바로 `spring-boot-starter-security` 패키지 때문인데요, 이 패키지를 추가하게되면 기본적으로 인증 없이는 접근할 수 없게 됩니다.

따라서 `Security` 관련 설정을 추가해줘야 합니다.

```java
package io.lcalmsky.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .mvcMatchers("/", "/login", "/sign-up", "/check-email", "/check-email-token",
                        "/email-login", "/check-email-login", "/login-link").permitAll()
                .mvcMatchers(HttpMethod.GET, "/profile/*").permitAll()
                .anyRequest().authenticated();
    }
}
```

루트 페이지 `/`, 로그인 페이지, 회원 가입 페이지, 이메일 체크하는 페이지 등 인증이 없어도 접근할 수 있는 url을 모두 등록했습니다.

`profile`의 경우 다른 사람의 것도 조회할 수 있어야 하므로 `GET` 메서드를 사용하고 `/profile`로 시작하는 모든 `url` 또한 인증 없이 접근할 수 있게 하였습니다.

그 외에 나머지 `url`은 모두 인증해야만 접근할 수 있게 했습니다.

다시 테스트를 실행해 볼까요?

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/01-01.png)

성공한 것을 확인할 수 있습니다.

---

다음 포스팅에서는 회원 가입 뷰를 작성해보도록 하겠습니다.