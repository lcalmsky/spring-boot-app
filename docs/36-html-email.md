![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: c4984db)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout c4984db
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

Thymeleaf 템플릿을 이용해 이메일 템플릿을 작성합니다.

## 템플릿 작성

템플릿 위치에 디렉토리를 하나 생성하고 하위에 이메일 템플릿을 HTML로 작성합니다.

`/src/main/resources/templates/mail/simple-link.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8"/>
    <title>Webluxible</title>
</head>

<body>
<div>
    <p>안녕하세요, <span th:text="${nickname}">닉네임</span>님!</p>

    <h3 th:text="${message}">메시지</h3>

    <div>
        <a th:href="${host} + ${link}" th:text="${linkName}">Link</a>
        <p>링크가 동작하지 않는 경우에는 아래 URL을 웹브라우저에 복사해서 붙여 넣으세요.</p>
        <small th:text="${host} + ${link}"></small>
    </div>
</div>
<footer>
    <small>Webluxible&copy; 2021</small>
</footer>
</body>

</html>

```

## Properties 설정

이메일 링크 클릭시 바로 다시 서비스로 진입하기 위해선 호스트 정보를 같이 제공해야 하는데, 환경별로 호스트가 다를 수 있습니다.

따라서 설정파일에 해당 내용을 추가해줍니다.

`/src/main/resources/application-local.yml`
`/src/main/resources/application-local-db.yml`

```yaml
# 생략
app:
  host: http://localhost:8080
```

<details>
<summary>application-local.yml 전체 보기</summary>

```yaml
spring:
  datasource:
    username: sa
    password:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:test
  h2.console:
    enabled: true
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        format_sql: true
logging:
  level:
    org.hibernate:
      SQL: debug
      type.descriptor.sql.BasicBinder: trace
app:
  host: http://localhost:8080
```

</details>

<details>
<summary>application-local-db.yml 전체 보기</summary>

```yaml
spring:
  datasource:
    username: testuser
    password: testpass
    url: jdbc:postgresql://localhost:5432/testdb
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        format_sql: true
  mail:
    host: smtp.gmail.com
    port: 587
    username: account-created
    password: app-password-issued
    properties:
      mail.smtp.auth: true
      mail.smtp.timeout: 5000
      mail.smtp.starttls.enable: true
logging:
  level:
    org.hibernate:
      SQL: debug
      type.descriptor.sql.BasicBinder: trace
app:
  host: http://localhost:8080
```

</details>

## 설정 클래스 추가

애플리케이션 관련 설정을 읽어오는 클래스를 하나 생성합니다.

`/src/main/java/io/lcalmsky/app/config/AppProperties.java`

```java
package io.lcalmsky.app.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("app")
public class AppProperties {
    private String host;
}
```

`@ConfigurationProperties` 애너테이션을 이용해 설정파일의 `app` prefix 하위에 있는 항목들을 주입받아 사용할 수 있습니다.

`@Component`로 등록하여 외부에서 의존성을 주입할 수 있게 하였습니다.

## AccountService 수정

메일 전송하는 부분을 수정합니다.

`/src/main/java/io/lcalmsky/app/account/application/AccountService.java`

```java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AccountService implements UserDetailsService {

    // 생략
    private final TemplateEngine templateEngine; // (1)
    private final AppProperties appProperties; // (2)
    // 생략
    public void sendVerificationEmail(Account newAccount) { // (3)
        Context context = new Context();
        context.setVariable("link", String.format("/check-email-token?token=%s&email=%s", newAccount.getEmailToken(),
                newAccount.getEmail()));
        context.setVariable("nickname", newAccount.getNickname());
        context.setVariable("linkName", "이메일 인증하기");
        context.setVariable("message", "Webluxible 가입 인증을 위해 링크를 클릭하세요.");
        context.setVariable("host", appProperties.getHost());
        String message = templateEngine.process("mail/simple-link", context);
        emailService.sendEmail(EmailMessage.builder()
                .to(newAccount.getEmail())
                .subject("Webluxible 회원 가입 인증")
                .message(message)
                .build());
    }
    // 생략
    public void sendLoginLink(Account account) { // (4)
        Context context = new Context();
        context.setVariable("link", "/login-by-email?token=" + account.getEmailToken() + "&email=" + account.getEmail());
        context.setVariable("nickname", account.getNickname());
        context.setVariable("linkName", "Webluxible 로그인하기");
        context.setVariable("message", "로그인 하려면 아래 링크를 클릭하세요.");
        context.setVariable("host", appProperties.getHost());
        String message = templateEngine.process("mail/simple-link", context);
        account.generateToken();
        emailService.sendEmail(EmailMessage.builder()
                .to(account.getEmail())
                .subject("[Webluxible] 로그인 링크")
                .message(message)
                .build());
    }
    // 생략
}

```

1. HTML 메시지를 생성하기 위해 `TemplateEngine`을 주입받습니다.
2. 호스트 정보를 획득하기 위해 `AppProperties`를 주입받습니다.
3. 가입시 이메일 전송하는 부분을 수정합니다.
4. 이메일로 로그인하는 부분을 수정합니다.

## 테스트

애플리케이션을 실행(local-db 프로파일 사용)한 뒤 유효한 이메일을 사용해 가입합니다.

> ⚠️ **WARNING:**   
> 로컬 DB에 사용하시는 메일을 이용해 이미 가입한 적이 있으면 추가 가입이 안 되므로 테스트를 위해 데이터를 지우고 시작하셔야 합니다.

가입 후 인증 메일을 확인합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/36-01.png)

HTML 포맷이 적용된 것을 확인할 수 있습니다.

메일로 전송된 링크를 클릭하면,

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/36-02.png)

가입이 정상적으로 처리되는 것을 확인할 수 있습니다.

---

여기까지 개인 정보 관리쪽은 거의 완료가 되었습니다.

다음 포스팅부터는 이 애플리케이션의 메인 기능인 스터디 관련 기능 구현을 진행하도록 하겠습니다.