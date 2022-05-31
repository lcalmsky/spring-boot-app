![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 4cbf9ad)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 4cbf9ad
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

패키지 구조를 `Domain Entity` 기준으로 다시 정리합니다.

> 그 동안 DDD에 맞게 패키지 구조를 정리하고 싶은 마음이 굴뚝같았으나 강의 뒷부분에 포함되어있길래 참고있었습니다.  
> 사실 일부분은 제가 적용하면서 하고있었기 때문에 강의와 패키지구조가 다르게 되어있었는데 이번 기회에 컨벤션에 맞게 정리할 예정입니다.  
> 패키지 구조는 정답이 정해져있는 것은 아니지만 많이 쓰는 컨벤션이나 사내에서 정한 컨벤션에 맞추는 게 여러 개발자들이 협업을 하는 데 도움이 됩니다.  

## 패키지 구조 정리

### 도메인 설계와 패키지 의존성

`Domain Entity`간의 관계는 아래 그림과 같습니다.

![](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/diagrams/46-01.puml)

`Event`-`Enrollment`를 하나로 묶어 크게 다섯 개로 나눌 수 있습니다.

![](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/diagrams/56-01.puml)

모두 단방향 참조만 존재합니다. 만약 양방향 참조나 순환 참조가 일어나게 되면 모듈화 시키기 어려워지므로 도메인 설계시 패키지간의 의존성도 같이 고려해야 합니다.

### 애플리케이션 패키지 정리

애플리케이션은 크게 두 가지로 나눌 수 있습니다.

* infra
* modules

먼저 설정이나 메일 전송 등의 기능을 `infra` 패키지를 생성해 이동시키겠습니다.

먼저 `app` 패키지 하위에 `infra` 패키지를 생성합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/56-01.png)

다음으로 `config` 패키지와 `mail` 패키지를 `infra` 패키지 하위로 이동시킵니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/56-02.png)

다음으로 `domain` 패키지 하위에 있는 `Auditing` 클래스를 `account` 하위 `domain` 패키지로 이동시킨 후 삭제하겠습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/56-03.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/56-04.png)

다음으로 `app` 패키지 하위에 `modules` 패키지를 생성합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/56-05.png)

그리고 `domain` 모듈에 해당하는 패키지를 모두 `modules` 하위로 이동시킵니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/56-06.png)

> 저는 블로그에 포스팅하는 마크다운 문서들도 모두 같은 프로젝트 안에 포함되어있는데, 이 부분까지 다 같이 리팩터링이 되어 시간이 오래 소모되었습니다.  
> 문서는 제외시키고 리팩터링할까 하다가 어차피 블로그 내용이 변하는 것은 아니라서 그냥 두었습니다.

### module, infra 의존관계 정리

이제 `module`, `infra`가 각각 분리되었는데요, `infra`는 `spring`, `jpa`, `3rd-party` 라이브러리를 참조하도록 수정하고, `module`은 `infra`를 참조하지만 `infra`에서는 `module`을 참조하지 않게 수정이 필요합니다.

현재 `infra.config` 패키지 하위에있는 `SecurityConfig` 클래스에서 `AccountService`를 참조하고있는데, `AccountService`는 `modules.account` 패키지 하위에 존재합니다.

`infra`에서 `modules`을 참조하고있으므로 이 관계를 끊어주겠습니다.

`/src/main/java/io/lcalmsky/app/infra/config/SecurityConfig.java`

```java
package io.lcalmsky.app.infra.config;

// 생략
import org.springframework.security.core.userdetails.UserDetailsService;
// 생략
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private final UserDetailsService userDetailsService;
    // 생략
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .mvcMatchers("/", "/login", "/sign-up", "/check-email-token",
                        "/email-login", "/check-email-login", "/login-link", "/login-by-email").permitAll()
                .mvcMatchers(HttpMethod.GET, "/profile/*").permitAll()
                .anyRequest().authenticated();
        http.formLogin()
                .loginPage("/login")
                .permitAll();
        http.logout()
                .logoutSuccessUrl("/");
        http.rememberMe()
                .userDetailsService(userDetailsService)
                .tokenRepository(tokenRepository());
    }
}
```

`AccountService`를 참조하고 있던 부분을 `UserDetailsService`로 수정하였습니다.

`AccountService`가 `UserDetailsService`를 구현하고있기 때문에 이렇게 수정하여도 빈을 주입해주는 데는 지장이 없습니다.

<details>
<summary>SecurityConfig.java 전체 보기</summary>

```java
package io.lcalmsky.app.infra.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private final UserDetailsService userDetailsService;
    private final DataSource dataSource;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .mvcMatchers("/", "/login", "/sign-up", "/check-email-token",
                        "/email-login", "/check-email-login", "/login-link", "/login-by-email").permitAll()
                .mvcMatchers(HttpMethod.GET, "/profile/*").permitAll()
                .anyRequest().authenticated();
        http.formLogin()
                .loginPage("/login")
                .permitAll();
        http.logout()
                .logoutSuccessUrl("/");
        http.rememberMe()
                .userDetailsService(userDetailsService)
                .tokenRepository(tokenRepository());
    }

    @Bean
    public PersistentTokenRepository tokenRepository() {
        JdbcTokenRepositoryImpl jdbcTokenRepository = new JdbcTokenRepositoryImpl();
        jdbcTokenRepository.setDataSource(dataSource);
        return jdbcTokenRepository;
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                .mvcMatchers("/node_modules/**", "/images/**")
                .antMatchers("/h2-console/**");
    }
}
```

</details>

> **Info:** 이렇게 수정한 뒤에는 `Optimize Import` 기능을 이용하여 패키지 단위로 최적화시켜주는 것이 좋습니다.

### ArchUnit

아키텍처 테스트 유틸리티의 도움을 받아 modules에서 참조하는 부분을 확인할 수 있습니다.

먼저 build.gradle에 패키지를 추가합니다.

```groovy
{
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
    testImplementation 'com.tngtech.archunit:archunit-junit5-api:0.23.1' // (1)
}
```

1. archiunit 패키지를 test scope로 추가합니다.

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
    testImplementation 'com.tngtech.archunit:archunit-junit5-api:0.23.1'
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

다음으로 테스트 코드를 작성합니다.

`/src/test/java/io/lcalmsky/app/PackageDependencyTests.java`

```java
package io.lcalmsky.app;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packagesOf = App.class)
public class PackageDependencyTests {
    public static final String STUDY = "..modules.study..";
    public static final String EVENT = "..modules.event..";

    @ArchTest
    ArchRule studyPackageRule = classes().that()
            .resideInAPackage(STUDY)
            .should()
            .onlyBeAccessed()
            .byClassesThat()
            .resideInAnyPackage(STUDY, EVENT);

    @Test
    void studyPackageRuleTest() {
        studyPackageRule.check(new ClassFileImporter().importPackagesOf(App.class));
    }
}
```

> `@AnalyzeClasses`, `@ArchTest`만 이용하면 테스트를 바로 실행할 수 있는 거 같은데 제 환경에서는 제대로 동작하지 않아 `@Test`를 사용하였습니다.

영어를 작성하듯이 순차적으로 메서드 체이닝을 이용해 룰을 만들고 어떤 패키지에서 실행할지 파라미터로 전달하여 체크합니다.

위 내용은 `App.class`가 포함된 패키지 내에서 `modules.study` 패키지는 오직 `modules.study`, `modules.event`에서만 참조 가능해야한다는 룰을 정의한 것입니다.

테스트를 실행해보니 아래와 같은 에러가 발생했습니다.

```text
java.lang.AssertionError: Architecture Violation [Priority: MEDIUM] - Rule 'classes that reside in a package '..modules.study..' should only be accessed by classes that reside in any package ['..modules.study..', '..modules.event..']' was violated (1 times):
Method <io.lcalmsky.app.modules.account.domain.entity.Account.isManagerOf(io.lcalmsky.app.modules.study.domain.entity.Study)> calls method <io.lcalmsky.app.modules.study.domain.entity.Study.getManagers()> in (Account.java:118)

// 생략
```

`account` 패키지 하위의 클래스인 `Account.isManagerOf`에서 `study` 패키지의 `Study` 클래스를 참조하고있는 것이 확인되었습니다.

`isManagerOf`의 사용처를 확인해보니 `StudyService` 내에서 호출하고 있었습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/56-07.png)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/56-08.png)

```java
// 생략
public class StudyService {
    // 생략
    private void checkAccountIsManager(Account account, Study study) {
        if (!account.isManagerOf(study)) {
            throw new AccessDeniedException("해당 기능을 사용할 수 없습니다.");
        }
    }
    // 생략
}
```

`Study`는 `Account`를 참조할 수 있으므로 관계를 반대로 바꿔주겠습니다.

`/src/main/java/io/lcalmsky/app/modules/study/application/StudyService.java`

```java
// 생략
public class StudyService {
    // 생략
    private void checkAccountIsManager(Account account, Study study) {
        if (!study.isManagedBy(account)) { // study에서 호출하도록 수정
            throw new AccessDeniedException("해당 기능을 사용할 수 없습니다.");
        }
    }
    // 생략
}
```

`Study`에 메서드를 추가해주었으므로 `Entity`도 수정해주겠습니다.

`/src/main/java/io/lcalmsky/app/modules/study/domain/entity/Study.java`

```java
// 생략
public class Study {
    // 생략
    public boolean isManagedBy(Account account) {
        return this.getManagers().contains(account);
    }
}
```

`Account.isManagerOf` 메서드는 이제 사용하지 않으므로 삭제해주시면 됩니다.

다시 테스트 해보면,

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/56-09.png)

성공적으로 수행된 것을 확인할 수 있습니다.

이런식으로 다른 테스트도 추가하여 패키지간 참조관계를 모두 확인해보겠습니다.

```java
package io.lcalmsky.app;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packagesOf = App.class)
public class PackageDependencyTests {
    public static final String STUDY = "..modules.study..";
    public static final String EVENT = "..modules.event..";
    public static final String ACCOUNT = "..modules.account..";
    public static final String TAG = "..modules.tag..";
    public static final String ZONE = "..modules.zone..";
    public static final String MODULES = "io.lcalmsky.app.modules..";
    public static final JavaClasses CLASS = new ClassFileImporter().importPackagesOf(App.class);

    @ArchTest
    ArchRule studyPackageRule = classes().that()
            .resideInAPackage(STUDY)
            .should()
            .onlyBeAccessed()
            .byClassesThat()
            .resideInAnyPackage(STUDY, EVENT);

    @ArchTest
    ArchRule eventPackageRule = classes().that()
            .resideInAnyPackage(EVENT)
            .should()
            .accessClassesThat()
            .resideInAnyPackage(STUDY, ZONE, EVENT);

    @ArchTest
    ArchRule accountPackageRule = classes().that()
            .resideInAnyPackage(ACCOUNT)
            .should()
            .accessClassesThat()
            .resideInAnyPackage(TAG, ZONE, ACCOUNT);

    @ArchTest
    ArchRule cycleRule = slices().matching("io.lcalmsky.app.modules.(*)..")
            .should()
            .beFreeOfCycles();

    @ArchTest
    ArchRule modulesPackageRule = classes().that()
            .resideInAPackage(MODULES)
            .should()
            .onlyBeAccessed()
            .byClassesThat()
            .resideInAnyPackage(MODULES);

    @Test
    void studyPackageRuleTest() {
        studyPackageRule.check(CLASS);
    }

    @Test
    void eventPackageRuleTest() {
        eventPackageRule.check(CLASS);
    }

    @Test
    void accountPackageRuleTest() {
        accountPackageRule.check(CLASS);
    }

    @Test
    void cycleRuleTest() {
        cycleRule.check(CLASS);
    }

    @Test
    void modulesPackageRuleTest() {
        modulesPackageRule.check(CLASS);
    }
}
```

`event`, `account` 패키지의 참조와 사이클 참조, 모듈간 참조까지 테스트하기 위한 코드를 추가한 뒤 다시 실행해봤더니,

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/56-10.png)

사이클 참조와 모듈 참조 부분에 에러가 발생하였습니다.

먼저 사이클 참조 에러 로그입니다.

```text
java.lang.AssertionError: Architecture Violation [Priority: MEDIUM] - Rule 'slices matching 'io.lcalmsky.app.modules.(*)..' should be free of cycles' was violated (2 times):
Cycle detected: Slice account -> 
                Slice settings -> 
                Slice account
```

`account` -> `settings` -> `account`로 순환참조가 일어나고 있습니다.

사실 `Settings`가 모두 `Account`에 관련된 내용이라 참조가 일어날 수 밖에 없는 구조인데요, 그렇다면 `settings`에 있는 내용을 `account` 하위로 이동시켜도 문제가 없을 거 같습니다.

(테스트 패키지도 동일하게 이동시켜주었습니다.)

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/56-11.png)

패키지를 이동시키고나니 `PasswordForm`, `TagForm`, `ZoneForm` 에서 `protected` 레벨로 선언한 생성자들에 접근하지 못하는 에러가 발생하여 생성자의 레벨을 수정해주었습니다.

`@NoArgsConstructor(access = AccessLevel.PROTECTED)` -> `@NoArgsConstructor`

> 이후에도 계속 사이클 참조 에러가 발생했는데, 원인을 모두 제거해도 동일한 에러가 발생하여 결국은 주석처리 하였습니다. 😭  
> account -> zone -> account로 순환참조가 발생한다는 내용이었는데, zone에서는 account를 참조하는 게 하나도 남아있지 않았고 IDE 캐시도 지우고 이것 저것 시도해보다가 도저히 에러가 사라지지 않아서 극단의 조치를..  

다음으로 모듈 참조 에러 로그입니다.

```text
java.lang.AssertionError: Architecture Violation [Priority: MEDIUM] - Rule 'classes that reside in a package 'io.lcalmsky.app.modules..' should only be accessed by classes that reside in any package ['io.lcalmsky.app.modules..']' was violated (6 times):
Method <io.lcalmsky.app.modules.account.WithAccountSecurityContextFactory.createSecurityContext(io.lcalmsky.app.modules.account.WithAccount)> calls constructor <io.lcalmsky.app.modules.account.endpoint.controller.form.SignUpForm.<init>()> in (WithAccountSecurityContextFactory.java:25)
```

테스트를 위해 사용하는 `@WithAccount,` `@WithAccountSecurityContextFactory`가 모두 `account` 외부 패키지에 있어서 발생한 에러로 두 클래스 모두 `account` 패키지로 이동시켜주겠습니다.

이후 모든 테스트가 잘 통과되었습니다.

## 최종 패키지 구조

앞서서 `infra`와 `modules`를 분리해주었는데, `modules` 내부 패키지 구조 또한 중요합니다.

강의에서는 이 부분은 소개하고있지 않고 `service`, `controller`, `repository` 등이 `modules/module` 하위에 위치하고 있는데 제가 적용한 컨벤션은 아래와 같습니다. 

```text
modules
ㄴ module
  ㄴ application
  ㄴ endpoint
  ㄴ domain
  ㄴ infra
  ㄴ ...
```

`application` 하위에는 비즈니스 로직을 실행할 `service layer`가 위치하고 `endpoint` 하위에는 `controller`와 관련된 클래스들이 위치합니다.

> endpoint의 경우 controller, endpoint, api 등으로 표현할 수 있고 페이지 이동에 관련된 것을 controller, REST API가 관련된 것을 endpoint, api로 구분해서 사용하는 곳도 있습니다.

`domain` 하위에는 `entity`와 `entity`를 구성하는 `class`, `enum`, `converter` 등을 위치시켰습니다.

> 이 부분에 대해서는 회사별로 천차만별이라 회사의 룰을 따르는 게 마음이 편할 거 같습니다.

`infra`는 상위의 `infra`와 마찬가지로 `DB`와 통신하거나 설정이나 기타 `module`에 필요한 것들이 위치합니다.

최종 리팩터링 된 구조는 아래와 같습니다.

```text
└── app
    ├── App.java
    ├── infra
    │   ├── config
    │   │   ├── AppConfig.java
    │   │   ├── AppProperties.java
    │   │   └── SecurityConfig.java
    │   └── mail
    │       ├── ConsoleEmailService.java
    │       ├── EmailMessage.java
    │       ├── EmailService.java
    │       └── HtmlEmailService.java
    └── modules
        ├── account
        │   ├── application
        │   │   └── AccountService.java
        │   ├── domain
        │   │   ├── UserAccount.java
        │   │   ├── entity
        │   │   │   ├── Account.java
        │   │   │   ├── AuditingEntity.java
        │   │   │   ├── PersistentLogins.java
        │   │   │   └── Zone.java
        │   │   └── support
        │   │       └── ListStringConverter.java
        │   ├── endpoint
        │   │   └── controller
        │   │       ├── AccountController.java
        │   │       ├── SettingsController.java
        │   │       ├── form
        │   │       │   ├── NicknameForm.java
        │   │       │   ├── NotificationForm.java
        │   │       │   ├── PasswordForm.java
        │   │       │   ├── Profile.java
        │   │       │   ├── SignUpForm.java
        │   │       │   ├── TagForm.java
        │   │       │   └── ZoneForm.java
        │   │       └── validator
        │   │           ├── NicknameFormValidator.java
        │   │           ├── PasswordFormValidator.java
        │   │           └── SignUpFormValidator.java
        │   ├── infra
        │   │   └── repository
        │   │       └── AccountRepository.java
        │   └── support
        │       └── CurrentUser.java
        ├── event
        │   ├── application
        │   │   └── EventService.java
        │   ├── domain
        │   │   └── entity
        │   │       ├── Enrollment.java
        │   │       ├── Event.java
        │   │       └── EventType.java
        │   ├── endpoint
        │   │   ├── EventController.java
        │   │   └── form
        │   │       └── EventForm.java
        │   ├── infra
        │   │   └── repository
        │   │       ├── EnrollmentRepository.java
        │   │       └── EventRepository.java
        │   └── validator
        │       └── EventValidator.java
        ├── main
        │   └── endpoint
        │       └── controller
        │           └── MainController.java
        ├── study
        │   ├── application
        │   │   └── StudyService.java
        │   ├── domain
        │   │   └── entity
        │   │       └── Study.java
        │   ├── endpoint
        │   │   ├── StudyController.java
        │   │   ├── StudySettingsController.java
        │   │   └── form
        │   │       ├── StudyDescriptionForm.java
        │   │       ├── StudyForm.java
        │   │       └── validator
        │   │           └── StudyFormValidator.java
        │   └── infra
        │       └── repository
        │           └── StudyRepository.java
        ├── tag
        │   ├── application
        │   │   └── TagService.java
        │   ├── domain
        │   │   └── entity
        │   │       └── Tag.java
        │   └── infra
        │       └── repository
        │           └── TagRepository.java
        └── zone
            ├── application
            │   └── ZoneService.java
            └── infra
                └── repository
                    └── ZoneRepository.java
```