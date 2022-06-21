package io.lcalmsky.app.modules.study.infra.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.JPQLQuery;
import io.lcalmsky.app.modules.account.domain.entity.QAccount;
import io.lcalmsky.app.modules.account.domain.entity.QZone;
import io.lcalmsky.app.modules.account.domain.entity.Zone;
import io.lcalmsky.app.modules.study.domain.entity.QStudy;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.tag.domain.entity.QTag;
import io.lcalmsky.app.modules.tag.domain.entity.Tag;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

public class StudyRepositoryExtensionImpl extends QuerydslRepositorySupport implements
    StudyRepositoryExtension {

  public StudyRepositoryExtensionImpl() {
    super(Study.class);
  }

  @Override
  public Page<Study> findByKeyword(String keyword, Pageable pageable) {
    QStudy study = QStudy.study;
    JPQLQuery<Study> query = from(study)
        .where(study.published.isTrue()
            .and(study.title.containsIgnoreCase(keyword))
            .or(study.tags.any().title.containsIgnoreCase(keyword))
            .or(study.zones.any().localNameOfCity.containsIgnoreCase(keyword)))
        .leftJoin(study.tags, QTag.tag).fetchJoin()
        .leftJoin(study.zones, QZone.zone).fetchJoin()
        .leftJoin(study.members, QAccount.account).fetchJoin()
        .distinct();
    JPQLQuery<Study> pageableQuery = getQuerydsl().applyPagination(pageable, query);
    QueryResults<Study> fetchResults = pageableQuery.fetchResults();
    return new PageImpl<>(fetchResults.getResults(), pageable, fetchResults.getTotal());
  }

  @Override
  public List<Study> findByAccount(Set<Tag> tags, Set<Zone> zones) {
    QStudy study = QStudy.study;
    JPQLQuery<Study> query = from(study).where(study.published.isTrue()
            .and(study.closed.isFalse())
            .and(study.tags.any().in(tags))
            .and(study.zones.any().in(zones)))
        .leftJoin(study.tags, QTag.tag).fetchJoin()
        .leftJoin(study.zones, QZone.zone).fetchJoin()
        .orderBy(study.publishedDateTime.desc())
        .distinct()
        .limit(9);
    return query.fetch();
  }
}
