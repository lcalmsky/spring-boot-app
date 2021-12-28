![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: a7de4fe)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout a7de4fe
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

스프링 시큐리티 기능을 활용하여 현재 인증된 사용자 정보를 참조하는 방법을 살펴보겠습니다.

## Implementation

`@AuthenticationPrincipal` 애너테이션은 `Authentication` 객체의 `getPrincipal()`를 가져오기 위해 사용합니다.

직접 객체 의존성을 주입하고 처리할 필요 없이 애너테이션을 사용하여 간단하게 가져올 수 있는데요, 사용 방법은 여러 가지가 있지만 강의에서 소개하는 내용은 Custom Annotation을 생성하는 방법입니다.

여기서 `Principal`은 `Authentication` 객체를 생성할 때 필요한 첫 번째 파라미터로 사용자의 인증 정보를 담고있습니다.

이 프로젝트에서 사용한 `UsernamePasswordAuthenticationToken`(`Authentication`을 상속)을 간단히 살펴보면,

```java
public UsernamePasswordAuthenticationToken(Object principal, Object credentials) {
    super(null);
    this.principal = principal;
    this.credentials = credentials;
    setAuthenticated(false);
}

public UsernamePasswordAuthenticationToken(Object principal, Object credentials,
    Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.principal = principal;
    this.credentials = credentials;
    super.setAuthenticated(true); // must use super, as we override
}
```

두 생성자 모두 첫 번째 파라미터가 `principal`이라고 되어있는 것을 확인할 수 있습니다.

### CurrentUser 애너테이션 작성

`account.support` 패키지 내에 `CurrentUser` 애너테이션을 작성합니다.

```java
package io.lcalmsky.app.account.support;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME) // (1)
@Target(ElementType.PARAMETER) // (2)
@AuthenticationPrincipal(expression = "#this == 'anonymousUser' ? null : account") // (3)
public @interface CurrentUser {

}
```

1. Runtime시 유지되어야 합니다.
2. 파라미터에 사용할 수 있어야 합니다.
3. spEL을 이용하여 인증정보가 존재하지 않으면 `null`을, 존재하면 `account` 라는 property를 반환합니다.

### MainController 작성

처리하는 도메인이 다르므로 `main`이라는 패키지를 생성한 뒤 `endpoint.controller` 패키지 하위에 `MainController` 클래스를 작성합니다.

```java
package io.lcalmsky.app.main.endpoint.controller;

import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.support.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
    @GetMapping("/")
    public String home(@CurrentUser Account account, Model model) { // (1)
        if (account != null) {
            model.addAttribute(account);
        }
        return "index";
    }
}
```

1. // (1) @CurrentUser의 영향을 받아 현재 인증된 사용자 정보에 따라 객체가 할당됩니다.

현재 로그인 할 때 사용한 `Principal`(`UsernamePasswordAuthenticationToken`)에는 `Account` 객체가 없습니다.

> <details>
> <summary>AccountService.login 참조</summary>
>
> ```java
>  public void login(Account account) {
>      UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(account.getNickname(),
>              account.getPassword(), Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")));
>      SecurityContextHolder.getContext().setAuthentication(token); // AuthenticationManager를 쓰는 방법이 정석적인 방ㅇ법
>  }
> ```
>
> </details>

따라서 `Principal`에서 `Account`를 가져오기 위해선 중간 `adaptor` 역할을 하는 객체가 필요합니다.

### UserAccount 클래스 작성

`account.domain` 패키지 내에 `UserAccount` 클래스를 생성합니다.

`UserDetailsService`를 구현하고있는 `User`를 상속해야 합니다.

```java
package io.lcalmsky.app.account.domain;

import io.lcalmsky.app.account.domain.entity.Account;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

public class UserAccount extends User {
    @Getter
    private final Account account; // (1)

    public UserAccount(Account account) {
        super(account.getNickname(), account.getPassword(), List.of(new SimpleGrantedAuthority("ROLE_USER"))); // (2) 
        this.account = account;
    }
}
```

1. `@CurrentUser` 애너테이션에서 `account` 를 반환하도록 하였기 때문에 변수 이름을 반드시 `account`로 설정해야 합니다.
2. User 객체를 생성하기 위해선 `username`, `password`, `authorities`가 필요한데 우리가 사용하는 객체인 `Account`에서 각각 추출해줍니다. (권한은 기존 `AccountService`에서 사용하던 것으로 동일하게 넣어줍니다)

이렇게 수정했으면 `AccountService`에서 로그인 처리하는 부분도 변경해줘야겠죠?

### AccountService 수정

```java
public void login(Account account) {
    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(new UserAccount(account), // (1)
            account.getPassword(), Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(token); // AuthenticationManager를 쓰는 방법이 정석적인 방ㅇ법
}
```

1. 기존에 `nickname`을 전달했던 파라미터를 `UserAccount` 객체로 대체합니다.

---

이렇게 수정해줬다면 `MainController`에서 `@CurrentUser` 애너테이션에 의해 `@AuthenticationPrincipal`이 적용되고, 인증 여부에 따라 `account`를 반환해서 넘겨줄 수 있게 되는 것입니다.

## Test

애플리케이션을 실행한 뒤 회원 가입, 이메일 인증을 하고 다시 루트 (/)로 이동했을 때 로그인 된 상태로 표시되면 성공입니다!

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/11-01.png)