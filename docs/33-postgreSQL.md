![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 5adddca)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 5adddca
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

로컬에 postgreSQL을 설치하고 설정합니다.

## postgreSQL 다운로드 및 설치

[postgreSQL 공식 웹사이트](https://www.postgresql.org/download/)에 접속하여 자신의 운영체제에 맞는 버전을 다운받아 설치합니다.

macOS를 사용하시는 분들은 터미널에서 설치하시는 게 더 편리하실 수도 있습니다.

```shell
> brew install postgresql
```

또는 [이곳](https://postgresapp.com/downloads.html)에서 다운 받을 수 있습니다.

앱으로 설치하면 간단한 UI도 제공해주기 떄문에 저는 이 방법을 선택하였습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/33-01.png)

설치가 완료된 뒤 터미널에서 아래 명령어를 입력하면 `Shell`이 실행됩니다.

```shell
> ~/ psql
psql (14.2)
Type "help" for help.

jaime=#
```

## DB 생성 및 사용자 설정

`psql` 내에서 아래 커맨드를 입력하여 DB와 사용자를 생성하고 사용자의 권한을 설정합니다.

```shell
> ~/ psql
psql (14.2)
Type "help" for help.

jaime=# create database testdb;
CREATE DATABASE
jaime=# create user testuser with encrypted password 'testpass';
CREATE ROLE
jaime=# grant all privileges on database testdb to testuser;
GRANT
jaime=#
```

## build.gradle 수정

postgreSQL 의존성을 추가해줍니다.

`/spring-boot-app/build.gradle`

```groovy
dependencies {
    // 생략
    // db
    runtimeOnly 'com.h2database:h2'
    runtimeOnly 'org.postgresql:postgresql'
    // 생략
}
// 생략
```

<details>
<summary>build.gradle 전체 보기</summary>

```groovy
plugins {
    id 'org.springframework.boot' version '2.5.4'
    id 'io.spring.dependency-management' version '1.0.11.RELEASE'
    id 'java'
    id 'com.github.node-gradle.node' version '2.2.3'
}

group = 'io.lcalmsky'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '11'

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // spring
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-mail'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.thymeleaf.extras:thymeleaf-extras-springsecurity5'
    // devtools
    compileOnly 'org.projectlombok:lombok'
    runtimeOnly 'org.springframework.boot:spring-boot-devtools'
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
    annotationProcessor 'org.projectlombok:lombok'
    // db
    runtimeOnly 'com.h2database:h2'
    runtimeOnly 'org.postgresql:postgresql'
    // test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
}

test {
    useJUnitPlatform()
}

node {
    version = '16.9.1'
    download = true
    nodeModulesDir = file("${projectDir}/src/main/resources/static")
}

task copyFrontLib(type: Copy) {
    from "${projectDir}/src/main/resources/static"
    into "${projectDir}/build/resources/main/static/."
}

copyFrontLib.dependsOn npmInstall
compileJava.dependsOn copyFrontLib
```

</details>

## Property 설정

application-local-db.yml 파일을 생성합니다.

`/src/main/resources/application-local-db.yml`

```yaml
spring:
  datasource:
    username: testuser # 위에서 설정한 사용자 정보 입니다.
    password: testpass # 위에서 설정한 사용자 정보 입니다.
    url: jdbc:postgresql://localhost:5432/testdb # jdbc url을 설정합니다. 위에서 생성한 testdb를 사용합니다.
    driver-class-name: org.postgresql.Driver # postgreSQL 드라이버를 사용합니다.
  jpa:
    hibernate:
      ddl-auto: update # create-drop이 아닌 update를 사용하여 스키마가 변경되지 않는 이상 기존 데이터를 삭제하지 않습니다.
    properties:
      hibernate:
        format_sql: true
logging:
  level:
    org.hibernate:
      SQL: debug
      type.descriptor.sql.BasicBinder: trace
```

## 애플리케이션 Configuration 수정

여태까지는 아무런 `profile` 없이 동작했기 때문에 `application.yml` 설정을 읽어서 시작하였고, 해당 설정에 `spring.profiles.active: local`로 되어있어 `application-local.yml` 설정을 `override`하여 실행이 되었습니다.

방금 추가한 설정을 적용하기 위해 IDE의 `configuration` 메뉴에서 `local-db` 프로파일로 실행되도록 합니다.

`IntelliJ`의 경우 `⌘ + ⌥ + R`을 누르면 맨 위에 `Edit Configurations` 라는 메뉴가 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/33-02.png)

단축키가 잘 외워지지 않는 분들은 ⇧를 두 번 연타한 뒤 `Edit Configurations`를 검색하셔도 되고,

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/33-03.png)

위에 `Run` 메뉴에서도 찾을 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/33-04.png)

메뉴에 진입하면 기존 프로파일을 복사(⌘ + D)하여 `local-db`를 사용함을 알 수 있는 이름으로 설정하고 아래 `active profiles`도 `local-db`로 설정합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/33-05.png)

---

여기까지 설정 후 설정한 `configuration`을 이용해 `local-db`로 애플리케이션을 실행하면 에러가 발생합니다.

그 이유는 `JavaMailSender`의 구현체인 `ConsoleMailSender`를 `local` 프로파일에서만 동작하게 했기 때문인데요, `local-db`에서도 동작할 수 있게 수정해줍니다.

## ConsoleMailSender 수정

`ConsoleMailSender` 클래스의 `@Profile` 애노테이션의 `attribute`에 `local-db`를 추가합니다.

`/spring-boot-app/src/main/java/io/lcalmsky/app/account/infra/email/ConsoleMailSender.java`

```java
// 생략
@Profile({"local", "local-db"})
@Component
@Slf4j
public class ConsoleMailSender implements JavaMailSender {
    // 생략
}
```

<details>
<summary>ConsoleMailSender.java 전체 보기</summary>

```java
package io.lcalmsky.app.account.infra.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;
import java.io.InputStream;

@Profile({"local", "local-db"})
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

## 확인

다시 `local-db` 프로파일로 동작하게 실행하면 앱이 정상적으로 실행됩니다.

그리고 로컬 DB를 사용하고 `spring.jpa.hibernate.ddl-auto: update` 설정 때문에 몇 차례 반복해서 실행하더라도 이전 처럼 테이블을 생성하는 쿼리가 계속 나타나거나, 지역 정보 등을 미리 입력하는 쿼리가 실행되지 않게 됩니다.

## IntelliJ Database 설정

위 설정에서 url 부분을 복사합니다.

`jdbc:postgresql://localhost:5432/testdb`

복사한 상태에서 `Database` 탭을 클릭하고 `Data Source from URL`을 클릭하면,

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/33-06.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/33-07.png)

자동으로 입력이 되고 OK 버튼을 누르면, 아래와 같은 메뉴가 나타납니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/33-08.png)

`User`, `Password`를 설정과 동일하게 `testuser`, `testpass`로 입력하고, 드라이버가 없는 경우 다운로드(Test Connection 위치 쯤 드라이버 다운로드 기능을 제공) 받으신다음 `OK` 버튼을 클릭하면,

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/33-09.png)

데이터베이스 탭에서 직접 확인할 수 있습니다.

테이블 6개가 생성되어있다면 모두 정상적으로 수행된 것입니다.

---

다음 포스팅에서는 `ConsoleMailSender`를 대신하여 실제로 메일을 전송할 수 있도록 `SMTP` 설정을 하도록 하겠습니다. 
