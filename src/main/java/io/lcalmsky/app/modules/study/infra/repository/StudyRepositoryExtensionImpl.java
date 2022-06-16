package io.lcalmsky.app.modules.study.infra.repository;

import com.querydsl.jpa.JPQLQuery;
import io.lcalmsky.app.modules.account.domain.entity.QAccount;
import io.lcalmsky.app.modules.account.domain.entity.QZone;
import io.lcalmsky.app.modules.study.domain.entity.QStudy;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.tag.domain.entity.QTag;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;

public class StudyRepositoryExtensionImpl extends QuerydslRepositorySupport implements StudyRepositoryExtension {

    public StudyRepositoryExtensionImpl() {
        super(Study.class);
    }

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
                .distinct();
        return query.fetch();
    }
}
