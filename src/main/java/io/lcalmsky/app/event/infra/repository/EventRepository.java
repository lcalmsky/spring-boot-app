package io.lcalmsky.app.event.infra.repository;

import io.lcalmsky.app.event.domain.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface EventRepository extends JpaRepository<Event, Long> {
}