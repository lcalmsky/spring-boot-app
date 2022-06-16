![](https://img.shields.io/badge/spring--boot-2.5.4-red) ![](https://img.shields.io/badge/gradle-7.1.1-brightgreen) ![](https://img.shields.io/badge/java-11-blue)

> 본 포스팅은 백기선님의 [스프링과 JPA 기반 웹 애플리케이션 개발](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-JPA-%EC%9B%B9%EC%95%B1/dashboard) 강의를 참고하여 작성하였습니다.  
> 소스 코드는 [여기](https://github.com/lcalmsky/spring-boot-app) 있습니다. (commit hash: e7a6ed7)
> ```shell
> > git clone https://github.com/lcalmsky/spring-boot-app.git
> > git checkout e7a6ed7
> ```
> ℹ️ squash merge를 사용해 기존 branch를 삭제하기로 하여 앞으로는 commit hash로 포스팅 시점의 소스 코드를 공유할 예정입니다.

## Overview

`querydsl`에서 발생하는 n+1 problem을 해결합니다.

## 해결 방법

* left (outer) join
  * fetchJoin을 하기 위해서
  * 첫 번째(left) 테이블에 연관 관계가 있는 모든 데이터를 가져옴
    * 연관 관계가 없으면 null로 채워서
  * 첫 번째 테이블 컬럼만 본다면 중복 row 발생

* fetchJoin
  * join 관계의 데이터도 같이 가져옴

* distinct
  * 중복 제거

왜 이 세 가지를 추가해야하는지 순차적으로 확인해보겠습니다.

## left join만 추가

`StudyRepositoryExtensionImpl` 클래스에 쿼리하는 부분에 `left join`만 추가합니다.

`src/main/java/io/lcalmsky/app/modules/study/infra/repository/StudyRepositoryExtensionImpl.java`

```java
// 생략
public class StudyRepositoryExtensionImpl extends QuerydslRepositorySupport implements StudyRepositoryExtension {
    // 생략
    @Override
    public List<Study> findByKeyword(String keyword) {
        QStudy study = QStudy.study;
        JPQLQuery<Study> query = from(study)
                .where(study.published.isTrue()
                        .and(study.title.containsIgnoreCase(keyword))
                        .or(study.tags.any().title.containsIgnoreCase(keyword))
                        .or(study.zones.any().localNameOfCity.containsIgnoreCase(keyword)))
                .leftJoin(study.tags, QTag.tag); // study 기준으로 tag를 join
        return query.fetch();
    }
}
```

`JPA`나 `querydsl`과 상관없이 `SQL`을 잘 아시는 분들은 저렇게 했을 때 어떤 점이 잘못될 수 있는지 잘 아실 겁니다.

우선 어떤 점이 잘못되었는지 애플리케이션을 실행해서 확인해보겠습니다.

> 관심 분야가 두 개 이상인 `study`를 사전에 준비해야 합니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/67-01.png)

스터디 주제를 `jpa`, `spring` 두 가지로 설정하였습니다.

이제 `jpa`로 검색해보면(현재 DB에는 예시를 위해 `jpa`를 `tags`로 가지는 `study`가 한 개만 존재),

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/67-02.png)

이렇게 같은 스터디가 두 개 중복해서 노출됩니다.

로그에 쿼리를 확인해보면,

<details>
<summary>Study 쿼리</summary>

```text
    select
        study0_.id as id1_7_,
        study0_.closed as closed2_7_,
        study0_.closed_date_time as closed_d3_7_,
        study0_.full_description as full_des4_7_,
        study0_.image as image5_7_,
        study0_.path as path6_7_,
        study0_.published as publishe7_7_,
        study0_.published_date_time as publishe8_7_,
        study0_.recruiting as recruiti9_7_,
        study0_.recruiting_updated_date_time as recruit10_7_,
        study0_.short_description as short_d11_7_,
        study0_.title as title12_7_,
        study0_.use_banner as use_ban13_7_ 
    from
        study study0_ 
    left outer join
        study_tags tags1_ 
            on study0_.id=tags1_.study_id 
    left outer join
        tag tag2_ 
            on tags1_.tags_id=tag2_.id 
    where
        study0_.published=? 
        and (
            lower(study0_.title) like ? escape '!'
        ) 
        or exists (
            select
                1 
            from
                study_tags tags3_,
                tag tag4_ 
            where
                study0_.id=tags3_.study_id 
                and tags3_.tags_id=tag4_.id 
                and (
                    lower(tag4_.title) like ? escape '!'
                )
        ) 
        or exists (
            select
                1 
            from
                study_zones zones5_,
                zone zone6_ 
            where
                study0_.id=zones5_.study_id 
                and zones5_.zones_id=zone6_.id 
                and (
                    lower(zone6_.local_name_of_city) like ? escape '!'
                )
        )
```

</details>

<details>
<summary>알림 쿼리</summary>

```text
    select
        count(notificati0_.id) as col_0_0_ 
    from
        notification notificati0_ 
    where
        notificati0_.account_account_id=? 
        and notificati0_.checked=?
```

</details>

<details>
<summary>tags 쿼리</summary>

```text
    select
        tags0_.study_id as study_id1_10_0_,
        tags0_.tags_id as tags_id2_10_0_,
        tag1_.id as id1_12_1_,
        tag1_.title as title2_12_1_ 
    from
        study_tags tags0_ 
    inner join
        tag tag1_ 
            on tags0_.tags_id=tag1_.id 
    where
        tags0_.study_id=?
```

</details>

<details>
<summary>zones 쿼리</summary>

```text
    select
        zones0_.study_id as study_id1_11_0_,
        zones0_.zones_id as zones_id2_11_0_,
        zone1_.id as id1_13_1_,
        zone1_.city as city2_13_1_,
        zone1_.local_name_of_city as local_na3_13_1_,
        zone1_.province as province4_13_1_ 
    from
        study_zones zones0_ 
    inner join
        zone zone1_ 
            on zones0_.zones_id=zone1_.id 
    where
        zones0_.study_id=?
```

</details>

<details>
<summary>members 쿼리</summary>

```text
    select
        members0_.study_id as study_id1_9_0_,
        members0_.members_account_id as members_2_9_0_,
        account1_.account_id as account_1_0_1_,
        account1_.created_date as created_2_0_1_,
        account1_.last_modified_date as last_mod3_0_1_,
        account1_.email as email4_0_1_,
        account1_.email_token as email_to5_0_1_,
        account1_.email_token_generated_at as email_to6_0_1_,
        account1_.is_valid as is_valid7_0_1_,
        account1_.joined_at as joined_a8_0_1_,
        account1_.nickname as nickname9_0_1_,
        account1_.study_created_by_email as study_c10_0_1_,
        account1_.study_created_by_web as study_c11_0_1_,
        account1_.study_registration_result_by_email as study_r12_0_1_,
        account1_.study_registration_result_by_web as study_r13_0_1_,
        account1_.study_updated_by_email as study_u14_0_1_,
        account1_.study_updated_by_web as study_u15_0_1_,
        account1_.password as passwor16_0_1_,
        account1_.bio as bio17_0_1_,
        account1_.company as company18_0_1_,
        account1_.image as image19_0_1_,
        account1_.job as job20_0_1_,
        account1_.location as locatio21_0_1_,
        account1_.url as url22_0_1_ 
    from
        study_members members0_ 
    inner join
        account account1_ 
            on members0_.members_account_id=account1_.account_id 
    where
        members0_.study_id=?
```

</details>

총 다섯 번의 쿼리가 발생했음을 확인할 수 있습니다.

특히 주의해서 봐야 할 쿼리는 첫 번째 쿼리인데요, 해당 쿼리를 DB에 직접 조회해보겠습니다.

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/67-03.png)
![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/67-04.png)
![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/67-05.png)

DB에서 마찬가지로 두 개의 row가 반환되는 것을 확인할 수 있습니다.

`study` 기준으로 `left join`을 하기 때문에 `study`가 가진 `tags`의 개수에 따라 중복된 `row`가 여러 개 발생할 수 있게 됩니다.

더 자세히 확인하려면 `select` 문에 `tag.title`을 추가하여 확인할 수 있습니다.

<details>
<summary>쿼리 및 결과 보기</summary>

```postgresql
select study0_.id                           as id1_7_,
       study0_.closed                       as closed2_7_,
       study0_.closed_date_time             as closed_d3_7_,
       study0_.full_description             as full_des4_7_,
       study0_.image                        as image5_7_,
       study0_.path                         as path6_7_,
       study0_.published                    as publishe7_7_,
       study0_.published_date_time          as publishe8_7_,
       study0_.recruiting                   as recruiti9_7_,
       study0_.recruiting_updated_date_time as recruit10_7_,
       study0_.short_description            as short_d11_7_,
       study0_.title                        as title12_7_,
       study0_.use_banner                   as use_ban13_7_,
       tag2_.title                          as tag_title /* 추가 */
from study study0_
         left outer join
     study_tags tags1_ on study0_.id = tags1_.study_id
         left outer join
     tag tag2_ on tags1_.tags_id = tag2_.id
where study0_.published = :published
    and (
          lower(study0_.title) like :keyword escape '!'
          )
   or exists(
        select 1
        from study_tags tags3_,
             tag tag4_
        where study0_.id = tags3_.study_id
          and tags3_.tags_id = tag4_.id
          and (
            lower(tag4_.title) like :keyword escape '!'
            )
    )
   or exists(
        select 1
        from study_zones zones5_,
             zone zone6_
        where study0_.id = zones5_.study_id
          and zones5_.zones_id = zone6_.id
          and (
            lower(zone6_.local_name_of_city) like :keyword escape '!'
            )
    )

```

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/67-06.png)

</details>

또한, `study`의 데이터만 가져왔기 때문에(`fetch join`을 하지 않았기 때문에) `tags`, `zones`, `members`에 대한 쿼리가 추가로 발생하고 있는 것도 확인할 수 있습니다.

## fetch join 추가

먼저 동일한 클래스에서 쿼리하는 부분에 fetchJoin()을 추가하여 쿼리가 줄어드는지 확인해보겠습니다. 

```java
// 생략
public class StudyRepositoryExtensionImpl extends QuerydslRepositorySupport implements StudyRepositoryExtension {
    // 생략
    @Override
    public List<Study> findByKeyword(String keyword) {
        QStudy study = QStudy.study;
        JPQLQuery<Study> query = from(study)
                .where(study.published.isTrue()
                        .and(study.title.containsIgnoreCase(keyword))
                        .or(study.tags.any().title.containsIgnoreCase(keyword))
                        .or(study.zones.any().localNameOfCity.containsIgnoreCase(keyword)))
                .leftJoin(study.tags, QTag.tag)
                .fetchJoin(); // fetchJoin 추가
        return query.fetch();
    }
}

```

<details>
<summary>로그(쿼리) 확인</summary>

```text
2022-06-17 03:20:44.316 DEBUG 46618 --- [nio-8080-exec-9] org.hibernate.SQL                        : 
    select
        study0_.id as id1_7_0_,
        tag2_.id as id1_12_1_,
        study0_.closed as closed2_7_0_,
        study0_.closed_date_time as closed_d3_7_0_,
        study0_.full_description as full_des4_7_0_,
        study0_.image as image5_7_0_,
        study0_.path as path6_7_0_,
        study0_.published as publishe7_7_0_,
        study0_.published_date_time as publishe8_7_0_,
        study0_.recruiting as recruiti9_7_0_,
        study0_.recruiting_updated_date_time as recruit10_7_0_,
        study0_.short_description as short_d11_7_0_,
        study0_.title as title12_7_0_,
        study0_.use_banner as use_ban13_7_0_,
        tag2_.title as title2_12_1_,
        tags1_.study_id as study_id1_10_0__,
        tags1_.tags_id as tags_id2_10_0__ 
    from
        study study0_ 
    left outer join
        study_tags tags1_ 
            on study0_.id=tags1_.study_id 
    left outer join
        tag tag2_ 
            on tags1_.tags_id=tag2_.id 
    where
        study0_.published=? 
        and (
            lower(study0_.title) like ? escape '!'
        ) 
        or exists (
            select
                1 
            from
                study_tags tags3_,
                tag tag4_ 
            where
                study0_.id=tags3_.study_id 
                and tags3_.tags_id=tag4_.id 
                and (
                    lower(tag4_.title) like ? escape '!'
                )
        ) 
        or exists (
            select
                1 
            from
                study_zones zones5_,
                zone zone6_ 
            where
                study0_.id=zones5_.study_id 
                and zones5_.zones_id=zone6_.id 
                and (
                    lower(zone6_.local_name_of_city) like ? escape '!'
                )
        )
2022-06-17 03:20:44.317 TRACE 46618 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BOOLEAN] - [true]
2022-06-17 03:20:44.318 TRACE 46618 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [2] as [VARCHAR] - [%jpa%]
2022-06-17 03:20:44.318 TRACE 46618 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [3] as [VARCHAR] - [%jpa%]
2022-06-17 03:20:44.318 TRACE 46618 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [4] as [VARCHAR] - [%jpa%]
2022-06-17 03:20:44.325 DEBUG 46618 --- [nio-8080-exec-9] org.hibernate.SQL                        : 
    select
        count(notificati0_.id) as col_0_0_ 
    from
        notification notificati0_ 
    where
        notificati0_.account_account_id=? 
        and notificati0_.checked=?
2022-06-17 03:20:44.325 TRACE 46618 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [86]
2022-06-17 03:20:44.325 TRACE 46618 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [2] as [BOOLEAN] - [false]
2022-06-17 03:20:44.337 DEBUG 46618 --- [nio-8080-exec-9] org.hibernate.SQL                        : 
    select
        zones0_.study_id as study_id1_11_0_,
        zones0_.zones_id as zones_id2_11_0_,
        zone1_.id as id1_13_1_,
        zone1_.city as city2_13_1_,
        zone1_.local_name_of_city as local_na3_13_1_,
        zone1_.province as province4_13_1_ 
    from
        study_zones zones0_ 
    inner join
        zone zone1_ 
            on zones0_.zones_id=zone1_.id 
    where
        zones0_.study_id=?
2022-06-17 03:20:44.338 TRACE 46618 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [99]
2022-06-17 03:20:44.344 DEBUG 46618 --- [nio-8080-exec-9] org.hibernate.SQL                        : 
    select
        members0_.study_id as study_id1_9_0_,
        members0_.members_account_id as members_2_9_0_,
        account1_.account_id as account_1_0_1_,
        account1_.created_date as created_2_0_1_,
        account1_.last_modified_date as last_mod3_0_1_,
        account1_.email as email4_0_1_,
        account1_.email_token as email_to5_0_1_,
        account1_.email_token_generated_at as email_to6_0_1_,
        account1_.is_valid as is_valid7_0_1_,
        account1_.joined_at as joined_a8_0_1_,
        account1_.nickname as nickname9_0_1_,
        account1_.study_created_by_email as study_c10_0_1_,
        account1_.study_created_by_web as study_c11_0_1_,
        account1_.study_registration_result_by_email as study_r12_0_1_,
        account1_.study_registration_result_by_web as study_r13_0_1_,
        account1_.study_updated_by_email as study_u14_0_1_,
        account1_.study_updated_by_web as study_u15_0_1_,
        account1_.password as passwor16_0_1_,
        account1_.bio as bio17_0_1_,
        account1_.company as company18_0_1_,
        account1_.image as image19_0_1_,
        account1_.job as job20_0_1_,
        account1_.location as locatio21_0_1_,
        account1_.url as url22_0_1_ 
    from
        study_members members0_ 
    inner join
        account account1_ 
            on members0_.members_account_id=account1_.account_id 
    where
        members0_.study_id=?
2022-06-17 03:20:44.344 TRACE 46618 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [99]

```

</details>

로그가 길어서 접어두었지만 `tags`에 관한 추가 쿼리가 발생하지 않은 것을 확인할 수 있습니다.

이렇게 `fetchJoin`은 `join`한 결과를 한 번에 같이 가져올 수 있게 해줍니다.

나머지 `zones`, `members` 또한 추가해 준 뒤 확인해보겠습니다.

```java
// 생략
public class StudyRepositoryExtensionImpl extends QuerydslRepositorySupport implements StudyRepositoryExtension {
    // 생략
    @Override
    public List<Study> findByKeyword(String keyword) {
        QStudy study = QStudy.study;
        JPQLQuery<Study> query = from(study)
                .where(study.published.isTrue()
                        .and(study.title.containsIgnoreCase(keyword))
                        .or(study.tags.any().title.containsIgnoreCase(keyword))
                        .or(study.zones.any().localNameOfCity.containsIgnoreCase(keyword)))
                .leftJoin(study.tags, QTag.tag).fetchJoin()
                .leftJoin(study.zones, QZone.zone).fetchJoin() // zones join 및 fetchJoin
                .leftJoin(study.members, QAccount.account).fetchJoin(); // members join 및 fetchJoin
        return query.fetch();
    }
}
```

로그를 다시 확인해보면,

<details>
<summary>로그(쿼리) 전체 보기</summary>

```text
2022-06-17 03:24:04.045 DEBUG 46618 --- [nio-8080-exec-9] org.hibernate.SQL                        : 
    select
        study0_.id as id1_7_0_,
        tag2_.id as id1_12_1_,
        zone4_.id as id1_13_2_,
        account6_.account_id as account_1_0_3_,
        study0_.closed as closed2_7_0_,
        study0_.closed_date_time as closed_d3_7_0_,
        study0_.full_description as full_des4_7_0_,
        study0_.image as image5_7_0_,
        study0_.path as path6_7_0_,
        study0_.published as publishe7_7_0_,
        study0_.published_date_time as publishe8_7_0_,
        study0_.recruiting as recruiti9_7_0_,
        study0_.recruiting_updated_date_time as recruit10_7_0_,
        study0_.short_description as short_d11_7_0_,
        study0_.title as title12_7_0_,
        study0_.use_banner as use_ban13_7_0_,
        tag2_.title as title2_12_1_,
        tags1_.study_id as study_id1_10_0__,
        tags1_.tags_id as tags_id2_10_0__,
        zone4_.city as city2_13_2_,
        zone4_.local_name_of_city as local_na3_13_2_,
        zone4_.province as province4_13_2_,
        zones3_.study_id as study_id1_11_1__,
        zones3_.zones_id as zones_id2_11_1__,
        account6_.created_date as created_2_0_3_,
        account6_.last_modified_date as last_mod3_0_3_,
        account6_.email as email4_0_3_,
        account6_.email_token as email_to5_0_3_,
        account6_.email_token_generated_at as email_to6_0_3_,
        account6_.is_valid as is_valid7_0_3_,
        account6_.joined_at as joined_a8_0_3_,
        account6_.nickname as nickname9_0_3_,
        account6_.study_created_by_email as study_c10_0_3_,
        account6_.study_created_by_web as study_c11_0_3_,
        account6_.study_registration_result_by_email as study_r12_0_3_,
        account6_.study_registration_result_by_web as study_r13_0_3_,
        account6_.study_updated_by_email as study_u14_0_3_,
        account6_.study_updated_by_web as study_u15_0_3_,
        account6_.password as passwor16_0_3_,
        account6_.bio as bio17_0_3_,
        account6_.company as company18_0_3_,
        account6_.image as image19_0_3_,
        account6_.job as job20_0_3_,
        account6_.location as locatio21_0_3_,
        account6_.url as url22_0_3_,
        members5_.study_id as study_id1_9_2__,
        members5_.members_account_id as members_2_9_2__ 
    from
        study study0_ 
    left outer join
        study_tags tags1_ 
            on study0_.id=tags1_.study_id 
    left outer join
        tag tag2_ 
            on tags1_.tags_id=tag2_.id 
    left outer join
        study_zones zones3_ 
            on study0_.id=zones3_.study_id 
    left outer join
        zone zone4_ 
            on zones3_.zones_id=zone4_.id 
    left outer join
        study_members members5_ 
            on study0_.id=members5_.study_id 
    left outer join
        account account6_ 
            on members5_.members_account_id=account6_.account_id 
    where
        study0_.published=? 
        and (
            lower(study0_.title) like ? escape '!'
        ) 
        or exists (
            select
                1 
            from
                study_tags tags7_,
                tag tag8_ 
            where
                study0_.id=tags7_.study_id 
                and tags7_.tags_id=tag8_.id 
                and (
                    lower(tag8_.title) like ? escape '!'
                )
        ) 
        or exists (
            select
                1 
            from
                study_zones zones9_,
                zone zone10_ 
            where
                study0_.id=zones9_.study_id 
                and zones9_.zones_id=zone10_.id 
                and (
                    lower(zone10_.local_name_of_city) like ? escape '!'
                )
        )
2022-06-17 03:24:04.046 TRACE 46618 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BOOLEAN] - [true]
2022-06-17 03:24:04.046 TRACE 46618 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [2] as [VARCHAR] - [%jpa%]
2022-06-17 03:24:04.047 TRACE 46618 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [3] as [VARCHAR] - [%jpa%]
2022-06-17 03:24:04.047 TRACE 46618 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [4] as [VARCHAR] - [%jpa%]
2022-06-17 03:24:04.058 DEBUG 46618 --- [nio-8080-exec-9] org.hibernate.SQL                        : 
    select
        count(notificati0_.id) as col_0_0_ 
    from
        notification notificati0_ 
    where
        notificati0_.account_account_id=? 
        and notificati0_.checked=?
2022-06-17 03:24:04.059 TRACE 46618 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [1] as [BIGINT] - [86]
2022-06-17 03:24:04.059 TRACE 46618 --- [nio-8080-exec-9] o.h.type.descriptor.sql.BasicBinder      : binding parameter [2] as [BOOLEAN] - [false]

```

</details>

`study`와 `notification`에 대한 쿼리 딱 두 개만 발생하는 것을 확인할 수 있습니다.

`fetchJoin`을 사용하지 않았다면 `tags`, `zones`, `members`를 접근할 때마다 쿼리가 발생하게 되므로 스터디의 개수가 n일 때 3n + 1개의 쿼리가 발생하게 됩니다.

## distinct 추가

쿼리의 수는 줄였지만 아직도 애플리케이션에서는 중복된 스터디를 보여주고 있습니다.

마지막에 distinct()를 추가하여 중복된 row를 제거할 수 있습니다.

```java
// 생략
public class StudyRepositoryExtensionImpl extends QuerydslRepositorySupport implements StudyRepositoryExtension {
    // 생략
    @Override
    public List<Study> findByKeyword(String keyword) {
        QStudy study = QStudy.study;
        JPQLQuery<Study> query = from(study)
                .where(study.published.isTrue()
                        .and(study.title.containsIgnoreCase(keyword))
                        .or(study.tags.any().title.containsIgnoreCase(keyword))
                        .or(study.zones.any().localNameOfCity.containsIgnoreCase(keyword)))
                .leftJoin(study.tags, QTag.tag).fetchJoin()
                .leftJoin(study.zones, QZone.zone).fetchJoin()
                .leftJoin(study.members, QAccount.account).fetchJoin()
                .distinct(); // distinct 추가
        return query.fetch();
    }
}

```

애플리케이션에서 결과를 확인해보면,

![](https://raw.githubusercontent.com/lcalmsky/spring-boot-app/master/resources/images/67-07.png)

하나의 고유한 결과를 가져오는 것을 확인할 수 있습니다.

쿼리를 확인해보면

```postgresql
    select
        distinct study0_.id as id1_7_0_,
        tag2_.id as id1_12_1_,
        zone4_.id as id1_13_2_,
        account6_.account_id as account_1_0_3_,
        study0_.closed as closed2_7_0_,
        study0_.closed_date_time as closed_d3_7_0_,
        study0_.full_description as full_des4_7_0_,
        study0_.image as image5_7_0_,
        study0_.path as path6_7_0_,
        study0_.published as publishe7_7_0_,
        study0_.published_date_time as publishe8_7_0_,
        study0_.recruiting as recruiti9_7_0_,
        study0_.recruiting_updated_date_time as recruit10_7_0_,
        study0_.short_description as short_d11_7_0_,
        study0_.title as title12_7_0_,
        study0_.use_banner as use_ban13_7_0_,
        tag2_.title as title2_12_1_,
        tags1_.study_id as study_id1_10_0__,
        tags1_.tags_id as tags_id2_10_0__,
        zone4_.city as city2_13_2_,
        zone4_.local_name_of_city as local_na3_13_2_,
        zone4_.province as province4_13_2_,
        zones3_.study_id as study_id1_11_1__,
        zones3_.zones_id as zones_id2_11_1__,
        account6_.created_date as created_2_0_3_,
        account6_.last_modified_date as last_mod3_0_3_,
        account6_.email as email4_0_3_,
        account6_.email_token as email_to5_0_3_,
        account6_.email_token_generated_at as email_to6_0_3_,
        account6_.is_valid as is_valid7_0_3_,
        account6_.joined_at as joined_a8_0_3_,
        account6_.nickname as nickname9_0_3_,
        account6_.study_created_by_email as study_c10_0_3_,
        account6_.study_created_by_web as study_c11_0_3_,
        account6_.study_registration_result_by_email as study_r12_0_3_,
        account6_.study_registration_result_by_web as study_r13_0_3_,
        account6_.study_updated_by_email as study_u14_0_3_,
        account6_.study_updated_by_web as study_u15_0_3_,
        account6_.password as passwor16_0_3_,
        account6_.bio as bio17_0_3_,
        account6_.company as company18_0_3_,
        account6_.image as image19_0_3_,
        account6_.job as job20_0_3_,
        account6_.location as locatio21_0_3_,
        account6_.url as url22_0_3_,
        members5_.study_id as study_id1_9_2__,
        members5_.members_account_id as members_2_9_2__ 
    from
        study study0_ 
    left outer join
        study_tags tags1_ 
            on study0_.id=tags1_.study_id 
    left outer join
        tag tag2_ 
            on tags1_.tags_id=tag2_.id 
    left outer join
        study_zones zones3_ 
            on study0_.id=zones3_.study_id 
    left outer join
        zone zone4_ 
            on zones3_.zones_id=zone4_.id 
    left outer join
        study_members members5_ 
            on study0_.id=members5_.study_id 
    left outer join
        account account6_ 
            on members5_.members_account_id=account6_.account_id 
    where
        study0_.published=? 
        and (
            lower(study0_.title) like ? escape '!'
        ) 
        or exists (
            select
                1 
            from
                study_tags tags7_,
                tag tag8_ 
            where
                study0_.id=tags7_.study_id 
                and tags7_.tags_id=tag8_.id 
                and (
                    lower(tag8_.title) like ? escape '!'
                )
        ) 
        or exists (
            select
                1 
            from
                study_zones zones9_,
                zone zone10_ 
            where
                study0_.id=zones9_.study_id 
                and zones9_.zones_id=zone10_.id 
                and (
                    lower(zone10_.local_name_of_city) like ? escape '!'
                )
        )
```

`select` 이후 `distinct`가 추가된 것을 확인할 수 있습니다.

하지만 이 쿼리를 DB에 조회해보면 이전과 동일하게 두 개의 row를 반환하는데요, 실제로 동작하는 방식은 `distinct`가 쿼리에 영향을 주지 않고 `ResultTransformer`를 이용해 결과에서 고유한 값을 걸러냈기 때문입니다.

쿼리를 자세히 보시면 `distinct`가 `study`에 대해 적용된 것이 아니라 전체 컬럼에 적용되었기 때문인데요, 결과 조회 후 걸러내는 방식이 맘에 들지 않는다면 여기서 최적화를 적용할 수 있습니다.

## 최적화 방법은?

먼저 쿼리에서는 `distinct`를 제거하고 `ResultTransformer`를 제공하는 방법이 있습니다. 쿼리에 `distinct`가 포함되어있으면 성능에 영향을 주기 때문입니다.

(특히 study의 description까지 모두 비교해가며 고유의 값을 찾아야하기 때문에)

이를 적용하기 위해선 `projection`을 사용해야 하는데 강의에서는 다루고 있지 않습니다.

관심있으신 분들은 [이 포스팅](https://jaime-note.tistory.com/75?category=994945)을 참고해주세요.

> 저도 나중에 시간이 되면 추가 포스팅을 통해 최적화 해보고, 데이터를 충분히 늘려 놓은 상태에서 쿼리 시간을 비교해 볼 예정입니다.