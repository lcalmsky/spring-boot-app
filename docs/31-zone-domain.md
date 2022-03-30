![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: 8a278b6)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout 8a278b6
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

지역 도메인을 설계합니다.

태그와 마찬가지로 `Value` 타입이 아닌 `Entity` 타입으로 설계(JPA 관점)해야 합니다.

## 도메인 설계

지역(Zone)은 아래와 같은 속성을 가집니다.

* `city`: 영문 도시 이름
* `localNameOfCity`: 한국어 도시 이름
* `province`: 주(도) 이름, nullable

`Account`와 `Zone`의 객체간 관계는 아래와 같습니다.

![](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/diagrams/31-01.puml)

단방향 다대다 관계를 가집니다.

`Account`와 `Zone`의 관계형 관계는 아래와 같습니다.

![](http://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/diagrams/31-02.puml)

조인 테이블을 사용하고 1대다 관계입니다.

## Entity 생성

`Zone` 클래스를 생성하여 아래 내용을 작성합니다.

`/src/main/java/io/lcalmsky/app/account/domain/entity/Zone.java`

```java
package io.lcalmsky.app.account.domain.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
public class Zone {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String localNameOfCity;

    private String province;

    public static Zone map(String line) {
        String[] split = line.split(",");
        Zone zone = new Zone();
        zone.city = split[0];
        zone.localNameOfCity = split[1];
        zone.province = split[2];
        return zone;
    }
}
```

컬럼 별로 `nullable` 설정 외에는 따로 특이한 사항이 없습니다.

문자열을 받아 `Zone`으로 매핑해주는 `static` 메서드를 미리 작성하였는데 이 부분은 아래서 설명하겠습니다.

## 초기 데이터 설정

[위키 피디아](https://en.wikipedia.org/wiki/List_of_cities_in_South_Korea)에서 데이터를 복사합니다.

필요한 데이터인 `City`, `Hangul`, `Province` 컬럼만 추출하여 CSV 파일(zones_kr.csv)을 만들고 `resources`에 추가합니다.

귀찮으신 분들은 아래 내용을 복사하여 파일을 생성하시면 됩니다.

`/src/main/resources/zones_kr.csv`

```
Andong,안동시,North Gyeongsang
Ansan,안산시,Gyeonggi
Anseong,안성시,Gyeonggi
Anyang,안양시,Gyeonggi
Asan,아산시,South Chungcheong
Boryeong,보령시,South Chungcheong
Bucheon,부천시,Gyeonggi
Busan,부산광역시,none
Changwon,창원시,South Gyeongsang
Cheonan,천안시,South Chungcheong
Cheongju,청주시,North Chungcheong
Chuncheon,춘천시,Gangwon
Chungju,충주시,North Chungcheong
Daegu,대구광역시,none
Daejeon,대전광역시,none
Dangjin,당진시,South Chungcheong
Dongducheon,동두천시,Gyeonggi
Donghae,동해시,Gangwon
Gangneung,강릉시,Gangwon
Geoje,거제시,South Gyeongsang
Gimcheon,김천시,North Gyeongsang
Gimhae,김해시,South Gyeongsang
Gimje,김제시,North Jeolla
Gimpo,김포시,Gyeonggi
Gongju,공주시,South Chungcheong
Goyang,고양시,Gyeonggi
Gumi,구미시,North Gyeongsang
Gunpo,군포시,Gyeonggi
Gunsan,군산시,North Jeolla
Guri,구리시,Gyeonggi
Gwacheon,과천시,Gyeonggi
Gwangju,광주광역시,none
Gwangju,광주시,Gyeonggi
Gwangmyeong,광명시,Gyeonggi
Gwangyang,광양시,South Jeolla
Gyeongju,경주시,North Gyeongsang
Gyeongsan,경산시,North Gyeongsang
Gyeryong,계룡시,South Chungcheong
Hanam,하남시,Gyeonggi
Hwaseong,화성시,Gyeonggi
Icheon,이천시,Gyeonggi
Iksan,익산시,North Jeolla
Incheon,인천광역시,none
Jecheon,제천시,North Chungcheong
Jeongeup,정읍시,North Jeolla
Jeonju,전주시,North Jeolla
Jeju,제주시,Jeju
Jinju,진주시,South Gyeongsang
Naju,나주시,South Jeolla
Namyangju,남양주시,Gyeonggi
Namwon,남원시,North Jeolla
Nonsan,논산시,South Chungcheong
Miryang,밀양시,South Gyeongsang
Mokpo,목포시,South Jeolla
Mungyeong,문경시,North Gyeongsang
Osan,오산시,Gyeonggi
Paju,파주시,Gyeonggi
Pocheon,포천시,Gyeonggi
Pohang,포항시,North Gyeongsang
Pyeongtaek,평택시,Gyeonggi
Sacheon,사천시,South Gyeongsang
Sangju,상주시,North Gyeongsang
Samcheok,삼척시,Gangwon
Sejong,세종특별자치시,none
Seogwipo,서귀포시,Jeju
Seongnam,성남시,Gyeonggi
Seosan,서산시,South Chungcheong
Seoul,서울특별시,none
Siheung,시흥시,Gyeonggi
Sokcho,속초시,Gangwon
Suncheon,순천시,South Jeolla
Suwon,수원시,Gyeonggi
Taebaek,태백시,Gangwon
Tongyeong,통영시,South Gyeongsang
Uijeongbu,의정부시,Gyeonggi
Uiwang,의왕시,Gyeonggi
Ulsan,울산광역시,none
Wonju,원주시,Gangwon
Yangju,양주시,Gyeonggi
Yangsan,양산시,South Gyeongsang
Yeoju,여주시,Gyeonggi
Yeongcheon,영천시,North Gyeongsang
Yeongju,영주시,North Gyeongsang
Yeosu,여수시,South Jeolla
Yongin,용인시,Gyeonggi
```

## ZoneRepository 생성

위에서 `Zone` `Entity`를 생성했으니 `Repository`도 생성해줍니다.

도메인을 따로 다룰 예정이기 때문에 `zone` 패키지를 생성하였습니다.

`/src/main/java/io/lcalmsky/app/zone/infra/repository/ZoneRepository.java`

```java
package io.lcalmsky.app.zone.infra.repository;

import io.lcalmsky.app.account.domain.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZoneRepository extends JpaRepository<Zone, Long> {

}
```

## ZoneService 생성

애플리케이션이 실행될 때 지역 정보를 미리 가지고 있어야 합니다.

따라서 서비스가 빈 등록되는 시점에 위에서 미리 작성해놓은 파일을 읽어 DB에 저장할 수 있도록 합니다.

`/src/main/java/io/lcalmsky/app/zone/application/ZoneService.java`

```java
package io.lcalmsky.app.zone;

import io.lcalmsky.app.account.domain.entity.Zone;
import io.lcalmsky.app.zone.infra.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ZoneService {
    private final ZoneRepository zoneRepository;

    @PostConstruct
    public void initZoneData() throws IOException {
        if (zoneRepository.count() == 0) {
            Resource resource = new ClassPathResource("zones_kr.csv");
            List<String> allLines = Files.readAllLines(resource.getFile().toPath(), StandardCharsets.UTF_8);
            List<Zone> zones = allLines.stream().map(Zone::map).collect(Collectors.toList());
            zoneRepository.saveAll(zones);
        }
    }
}
```

`@PostConstruct` 애너테이션을 이용하면 빈 등록 이후에 해당 메서드가 실행됩니다.

[Entity 생성](#Entity-생성)의 맨 아랫 부분에 문자열을 `Zone` `Entity`로 매핑해주는 static 메서드를 구현했었는데 파일을 읽어와 각 라인을 바로 매핑하기 위함이었습니다.

매핑한 결과를 `ZoneRepository.saveAll` 메서드를 이용해 모두 저장해주면 지역 데이터 초기화가 완료됩니다.

---

여기까지 도메인 설계와 데이터 초기화를 모두 마쳤습니다.

다음 포스팅에서는 관심 지역 때와 유사하게 지역 정보를 추가하고 삭제하는 기능을 구현하도록 하겠습니다.