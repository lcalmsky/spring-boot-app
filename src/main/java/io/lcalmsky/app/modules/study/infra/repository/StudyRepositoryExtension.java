package io.lcalmsky.app.modules.study.infra.repository;

import io.lcalmsky.app.modules.study.domain.entity.Study;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
public interface StudyRepositoryExtension {
    List<Study> findByKeyword(String keyword);
}
