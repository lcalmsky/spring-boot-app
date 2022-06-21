package io.lcalmsky.app.modules.study.infra.repository;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface StudyRepository extends JpaRepository<Study, Long>, StudyRepositoryExtension {

  boolean existsByPath(String path);

  @EntityGraph(value = "Study.withAll", type = EntityGraph.EntityGraphType.LOAD)
  Study findByPath(String path);

  @EntityGraph(value = "Study.withTagsAndManagers", type = EntityGraph.EntityGraphType.FETCH)
  Study findStudyWithTagsByPath(String path);

  @EntityGraph(value = "Study.withZonesAndManagers", type = EntityGraph.EntityGraphType.FETCH)
  Study findStudyWithZonesByPath(String path);

  @EntityGraph(value = "Study.withManagers", type = EntityGraph.EntityGraphType.FETCH)
  Study findStudyWithManagersByPath(String path);

  @EntityGraph(value = "Study.withMembers", type = EntityGraph.EntityGraphType.FETCH)
  Study findStudyWithMembersByPath(String path);

  Optional<Study> findStudyOnlyByPath(String path);

  @EntityGraph(value = "Study.withTagsAndZones", type = EntityGraph.EntityGraphType.FETCH)
  Study findStudyWithTagsAndZonesById(Long id);

  @EntityGraph(attributePaths = {"managers", "members"})
  Study findStudyWithManagersAndMembersById(Long id);

  @EntityGraph(attributePaths = {"tags", "zones"})
  List<Study> findFirst9ByPublishedAndClosedOrderByPublishedDateTimeDesc(boolean published,
      boolean closed);

  List<Study> findFirst5ByManagersContainingAndClosedOrderByPublishedDateTimeDesc(Account account,
      boolean closed);

  List<Study> findFirst5ByMembersContainingAndClosedOrderByPublishedDateTimeDesc(Account account,
      boolean closed);
}