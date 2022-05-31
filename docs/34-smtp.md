![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 6ebecd9)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 6ebecd9
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

Gmail을 SMTP 서버로 활용해 메일을 전송하는 기능을 구현합니다.

## 개발용 계정 만들기

개인 계정을 이용해도 되지만, 앞으로 개발할 때 또 이용할 수도 있으므로 개발용 계정을 생성합니다.

[Gmail](https://mail.google.com/)로 접속한 뒤 우측 상단의 프로필 사진을 클릭하여 `다른 계정 추가`를 클릭합니다.

> 앞으로 등장하는 모든 캡처들에 포함된 개인 정보는 이메일 주소를 제외하고 모두 삭제하였습니다.  
> 공백처럼 보일 수 있으나 실제로는 값을 입력하였으므로 헷갈리지 마시길 바랍니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/34-01.png)

로그인 화면 좌측 하단에 `계정 만들기`를 클릭하고 `본인 계정`을 클릭합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/34-02.png)

성, 이름, 메일 주소, 비밀번호를 차례로 입력한 뒤 다음을 누릅니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/34-03.png)

추가 정보를 입력한 뒤 다음을 누릅니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/34-04.png)

전화번호 인증을 수행합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/34-05.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/34-06.png)

이 계정으로는 전화번호를 다양하게 활용할 일이 없어서 다음 내용은 건너뛰었습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/34-07.png)

다음으로 약관 화면에서 동의 항목을 체크한 뒤 `계정만들기`를 클릭하면 계정 생성이 완료됩니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/34-08.png)

## 2단계 인증 설정

~를 위해선 2단계 인증 설정이 필요합니다.

생성된 계정의 메일 화면에서 마찬가지로 우측 상단의 프로필 이미지를 클릭한 뒤 `계정 관리`를 클릭합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/34-09.png)

왼쪽에 `보안` 탭을 클릭하고 `Google에 로그인`항목에 있는 `2단계 인증`을 클릭합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/34-10.png)

시작하기를 누르면 다시 로그인해야 합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/34-11.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/34-12.png)

로그인하면 휴대전화 설정 화면이 나오는데, 가입시 입력했던 번호 말고 다른 번호를 사용하고 싶으시면 수정하시면 됩니다.

문자와 전화 중 코드 받는 방법을 선택합니다. 전 문자를 선택하였습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/34-13.png)

아래와 같이 인증 코드를 입력하고나면

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/34-14.png)

2단계 인증을 활성화 할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/34-15.png)

## 앱 비밀번호 설정

다시 보안 화면으로 돌아와서 `Google에 로그인` 항목에 있는 `앱 비밀번호`를 클릭합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/34-16.png)

다시 한번 로그인한 뒤 `앱 선택`에서 `기타(맞춤 이름)`를 선택하여 `SMTP`를 입력합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/34-17.png)

그럼 아래와 같이 기기용 앱 비밀번호가 생성됩니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/34-18.png)

## Property 설정

application-dev.yml 파일을 수정합니다.

`/src/main/resources/application-local-db.yml`

```yaml
spring:
  # 생략
  mail:
    host: smtp.gmail.com
    port: 587
    username: 생성한 계정
    password: 발급받은 비밀번호 16자리
    properties:
      mail.smtp.auth: true
      mail.smtp.timeout: 5000
      mail.smtp.starttls.enable: true
# 생략
```

방금 설정한 계정과 앱 비밀번호를 입력하고 나머지 메일 관련 설정을 변경합니다.

> 비밀번호의 경우 이 포스팅 이후 재발급 받을 예정입니다.  
> 어차피 소스 코드에 노출되기 때문에 .gitignore에 등록하여 프로파일을 별도로 관리할 예정입니다.

`spring.mail` 하위 항목들을 설정 파일에 작성하면 스프링이 실행될 때 자동으로 빈에 등록해줍니다.

## ConsoleMailSender 수정

이전 포스팅에서 `local-db`를 프로파일을 사용할 때도 `ConsoleMailSender`를 사용하도록 하였는데 이 설정을 다시 제거해줍니다.

`/src/main/java/io/lcalmsky/app/account/infra/email/ConsoleMailSender.java`

```java
// 생략
@Profile("local")
@Component
@Slf4j
public class ConsoleMailSender implements JavaMailSender {
    // 생략
}
```

<details>
<summary>ConsoleMailSender.java 전체 보기</summary>

```java
package io.lcalmsky.app.modules.account.infra.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;
import java.io.InputStream;

@Profile("local")
@Component
@Slf4j
public class ConsoleMailSender implements JavaMailSender {
    @Override public MimeMessage createMimeMessage() {
        return null;
    }

    @Override public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
        return null;
    }

    @Override public void send(MimeMessage mimeMessage) throws MailException {

    }

    @Override public void send(MimeMessage... mimeMessages) throws MailException {

    }

    @Override public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {

    }

    @Override public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {

    }

    @Override public void send(SimpleMailMessage simpleMessage) throws MailException {
        log.info("{}", simpleMessage);
    }

    @Override public void send(SimpleMailMessage... simpleMessages) throws MailException {

    }
}
```

</details>

## 테스트

`postgreSQL`을 실행한 뒤 애플리케이션 `local-db` 프로파일로 동작하도록 실행해줍니다.

> **⚠️ WARNING**: 혹시 소스 코드를 clone 받아서 실행하시는 경우 반드시 spring.mail.username, spring.mail.password 항목을 자신의 것으로 작성하셔야 합니다.

메일 전송을 테스트하기 위해선 가입을 먼저 진행해야 하는데, 그동안 아무 이메일이나 적었다면, 이번엔 메일을 수신할 수 있는 이메일을 입력해야 합니다.

입력한 뒤 가입을 하게되면,

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/34-19.png)

가입한 메일로 가입 인증 메일이 정상적으로 전달된 것을 확인할 수 있습니다.

아직은 메일 서식을 따로 작성하지 않았기 때문에 주소 한 줄 딸랑 오긴 하는데요, 이 주소를 localhost:8080 뒤에 붙여 이동해보면,

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/34-20.png)

회원 가입이 정상처리 되는 것을 알 수 있습니다.

> 실제 운영환경에서는 개인 Gmail 계정을 사용할 순 없습니다.  
> AWS를 쓰는 경우 SES (Simple Email Service)를 활용할 수 있고, 온프레미스 환경이라면 직접 SMTP 서버를 구축하기도 합니다.