package io.lcalmsky.app.zone.infra.repository;

import io.lcalmsky.app.account.domain.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZoneRepository extends JpaRepository<Zone, Long> {

}
