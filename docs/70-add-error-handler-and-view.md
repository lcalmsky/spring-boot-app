![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 85decd6)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 85decd6
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

스프링 기본 설정을 사용하면 클라이언트가 잘못된 요청을 보냈을 때 404에러 페이지로 자동으로 이동이 되는데요, 이 때 표시할 에러 화면과, 에러 화면을 보여주기 위한 핸들러를 구현합니다.

잘못된 요청의 예는 아래와 같습니다.

* 없는 스터디 페이지 조회 시도
* 없는 프로필 페이지 조회 시도
* 무작위 이벤트 조회 시도
* 허용하지 않는 요청 시도
  * 이미 종료된 스터디의 모임 생성 시도
  * 이미 종료된 모임에 참가 신청 시도
  * 관리자 권한이 없는 스터디 수정 시도
  * 기타 등등

## ControllerAdvice 추가

예외 상황을 처리하여 error 페이지로 리다이렉트 시키는 핸들러를 구현합니다.

`/src/main/java/io/lcalmsky/app/modules/main/endpoint/controller/ExceptionAdvice.java`

```java
package io.lcalmsky.app.modules.main.endpoint.controller;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.support.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@ControllerAdvice
@Slf4j
public class ExceptionAdvice {

    @ExceptionHandler
    public String handleRuntimeException(@CurrentUser Account account, HttpServletRequest request, RuntimeException exception) {
        log.info(getNicknameIfExists(account) + "requested {}", request.getRequestURI());
        log.error("bad request", exception);
        return "error";
    }

    private String getNicknameIfExists(Account account) {
        return Optional.ofNullable(account)
                .map(Account::getNickname)
                .map(s -> s + " ")
                .orElse("");
    }
}
```

`RuntimeException`이 발생했을 때 어떤 사용자가 어떤 요청을 하였는지, 어떤 에러가 발생했는지 로깅합니다.

`@ControllerAdvice` 애너테이션과 `@ExceptionHandler`를 이용해 간단히 구현할 수 있습니다.

이 강의에서는 `CustomException`을 따로 다루고 있지 않기 때문에 중간중간에 예외처리 할 때 발생시킨 `RuntimeException`들을 catch하여 처리하는 부분만 구현하였습니다.

예외처리를 위한 여러 가지 방법이 존재하지만 `@ControllerAdvice`나 `@ExceptionHandler`를 사용할 때 가장 좋은 방법은,

여러 가지 추상 예외 클래스를 정의해놓고, 해당 카테고리에 맞게 상세 예외 클래스들을 구현하는 것입니다.

이 프로젝트를 예로 들자면, 스터디 관련, 모임 관련, 사용자 관련, 태그/위치 관련, 그 밖의 잘못된 요청 등을 나누고 각각에 대해 예외처리를 할 수 있습니다.

특히 화면을 단순히 리다이렉트 하는 것이 아니라 API 요청에 대한 공통 응답 규격을 정의해놓고 응답 규격과 상태 코드(status code)를 반환하는 등 여러 가지 방법으로 처리할 수 있습니다.

이 때 다룰 예외를 메서드 파라미터로 전달해줘야 하고, @ExceptionHandler 애너테이션의 attribute로 추가하여 가독성을 높일 수도 있습니다.

```java
@ExceptionHandler(RuntimeException.class)
```

<details>
<summary>CustomException 처리 예시 보기</summary>

```java
  @ExceptionHandler(CustomException.class)
  public ResponseEntity<CustomError> handleCustomException(CustomException customException,
      ServletWebRequest webRequest) {
    return ResponseEntity
        .status(customException.getStatusCode())
        .body(CustomError.builder()
            .timestamp(LocalDateTime.now().toString())
            .status(customException.getRawStatusCode())
            .error(customException.getStatusCode().getReasonPhrase())
            .message(customException.getMessage())
            .path(webRequest.getRequest().getRequestURI())
            .build());
  }
```

```java
public class CustomException extends HttpStatusCodeException {

  protected CustomException(HttpStatus httpStatus, String message) {
    super(httpStatus, message);
  }
}
```

```java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CustomError {

  private String timestamp;
  private Integer status;
  private String error;
  private String message;
  private String path;
}
```

</details>

그리고 스프링에서 발생시키는 에러에 대한 예외처리를 추가하고 싶다면 `ResponseEntityExceptionHandler`를 상속받아서 필요한 메서드를 `override` 하면 됩니다.

<details>
<summary>스프링 에러 예외처리 예시 보기</summary>

```java
package io.lcalmsky.szs.infra.exception;

import io.lcalmsky.szs.infra.event.CustomError;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

  @Override
  protected ResponseEntity<Object> handleServletRequestBindingException(
      ServletRequestBindingException ex, HttpHeaders headers, HttpStatus status,
      WebRequest request) {
    return ResponseEntity
        .status(status)
        .body(CustomError.builder()
            .timestamp(LocalDateTime.now().toString())
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(ex.getMessage())
            .path(((ServletWebRequest) request).getRequest().getRequestURI())
            .build());
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status,
      WebRequest request) {
    return ResponseEntity
        .status(status)
        .body(CustomError.builder()
            .timestamp(LocalDateTime.now().toString())
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(ex.getMessage())
            .path(((ServletWebRequest) request).getRequest().getRequestURI())
            .build());
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
      HttpHeaders headers, HttpStatus status, WebRequest request) {
    List<FieldError> fieldErrors = ex.getFieldErrors();
    return ResponseEntity
        .status(status)
        .body(CustomError.builder()
            .timestamp(LocalDateTime.now().toString())
            .status(status.value())
            .error(ex.getMessage())
            .message(fieldErrors.stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse(ex.getMessage()))
            .path(((ServletWebRequest) request).getRequest().getRequestURI())
            .build());
  }
}
```

</details>

## Error 뷰 작성

에러가 발생했을 때 리다이렉트 될 페이지를 작성합니다.

`/src/main/resources/templates/error.html`

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments.html :: head"></head>
<body>
    <section class="jumbotron text-center">
        <div class="container">
            <h1>Webluxible</h1>
            <p class="lead text-muted">
                잘못된 요청입니다.<br/>
            </p>
            <p>
                <a th:href="@{/}" class="btn btn-primary my-2">첫 페이지로 이동</a>
            </p>
        </div>
    </section>
</body>
</html>
```

## 테스트

애플리케이션을 실행해 잘못된 스터디 주소로 진입해보면,

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/70-01.png)

에러 페이지가 노출되는 것을 확인할 수 있습니다.

그리고 로그를 확인해보면,

```text
2022-06-21 17:59:36.971  INFO 5163 --- [nio-8080-exec-9] i.l.a.m.m.e.controller.ExceptionAdvice   : jaime requested /study/wrong-study-path
2022-06-21 17:59:36.975 ERROR 5163 --- [nio-8080-exec-9] i.l.a.m.m.e.controller.ExceptionAdvice   : bad request

java.lang.IllegalArgumentException: wrong-study-path에 해당하는 스터디가 없습니다.
	at io.lcalmsky.app.modules.study.application.StudyService.checkStudyExists(StudyService.java:61) ~[classes/:na]
	at io.lcalmsky.app.modules.study.application.StudyService.getStudy(StudyService.java:33) ~[classes/:na]
	at io.lcalmsky.app.modules.study.application.StudyService$$FastClassBySpringCGLIB$$94d69493.invoke(<generated>) ~[classes/:na]
```

제가 로그인한 `jaime` 계정으로 어떤 요청을 했는지, 어떤 에러가 발생하였는지 로깅되는 것을 확인할 수 있습니다.

## 개선 방향

위에서 언급하긴 했지만 현재 상황에서 바로 개선할만한 점이 있습니다.

로그에서 `IllegalArgumentException`을 던질 때 `wrong-study-path에 해당하는 스터디가 없습니다.` 이런 메시지를 포함하고 있는데요, `Model`이나 `Errors`를 통해 에러 메시지를 전달하면 에러 화면에서 어떠한 이유로 리다이렉트 되었는지 표시해 줄 수 있습니다.

---

드디어 다음 포스팅이 마지막이 될 거 같네요!👏

로그인 전/후 홈 화면을 구현하는 것으로 대단원의 막을 내리도록 하겠습니다!

