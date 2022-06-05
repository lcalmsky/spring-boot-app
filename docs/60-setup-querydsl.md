![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 37f3309)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 37f3309
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

앞으로 사용하게 될 querydsl을 설정합니다.

## build.gradle 수정

```groovy
plugins {
    // 생략
    id 'com.ewerk.gradle.plugins.querydsl' version '1.0.10'
}
// 생략
dependencies {
    // 생략
    implementation 'com.querydsl:querydsl-jpa'
    // 생략
}
// 생략
def querydslDir = "$buildDir/generated/querydsl"

querydsl {
    jpa = true
    querydslSourcesDir = querydslDir
}

sourceSets {
    main.java.srcDir querydslDir
}

configurations {
    querydsl.extendsFrom compileClasspath
}

compileQuerydsl {
    options.annotationProcessorPath = configurations.querydsl
}
```

`plugins`, `dependencies`에 `querydsl` 관련 항목을 추가하고 아래 쪽에 스크립트를 추가해주었습니다.

<details>
<summary>build.gradle 전체 보기</summary>

```groovy
plugins {
    id 'org.springframework.boot' version '2.5.4'
    id 'io.spring.dependency-management' version '1.0.11.RELEASE'
    id 'java'
    id 'com.github.node-gradle.node' version '2.2.3'
    id 'com.ewerk.gradle.plugins.querydsl' version '1.0.10'
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
    implementation 'com.querydsl:querydsl-jpa'
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

def querydslDir = "$buildDir/generated/querydsl"

querydsl {
    jpa = true
    querydslSourcesDir = querydslDir
}

sourceSets {
    main.java.srcDir querydslDir
}

configurations {
    querydsl.extendsFrom compileClasspath
}

compileQuerydsl {
    options.annotationProcessorPath = configurations.querydsl
}
```

</details>

## 컴파일

아래 명령어를 수행하여 Q클래스들을 생성합니다.

```shell
./gradlew compileQuerydsl
```

`IDE`를 사용하는 경우 `gradle` 탭에서 `compileQuerydsl`을 수행하셔도 동일한 결과를 얻을 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/60-02.png)

## Q 클래스 확인

`build.gradle`에 `querydslDir`로 지정한 곳을 확인해보면 `Entity`들이 모두 생성된 것을 확인할 수 있습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/60-01.png)

---

build 디렉토리는 .gitignore 파일에 추가되어있으므로 git에 저장되지 않습니다.

git을 clone한 뒤 직접 다시 빌드해야 컴파일에러 없이 querydsl을 사용할 수 있습니다.
(아직은 querydsl을 이용해 코드를 작성하지 않았기 때문에 에러가 발생하지 않습니다.)