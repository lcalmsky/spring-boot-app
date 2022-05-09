package io.lcalmsky.app.event.infra.repository;

import io.lcalmsky.app.event.domain.entity.Event;
import io.lcalmsky.app.study.domain.entity.Study;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByStudyOrderByStartDateTime(Study study);
}