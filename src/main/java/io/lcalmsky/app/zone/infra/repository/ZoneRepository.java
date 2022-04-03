package io.lcalmsky.app.zone.infra.repository;

import io.lcalmsky.app.account.domain.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ZoneRepository extends JpaRepository<Zone, Long> {

    Optional<Zone> findByCityAndProvince(String cityName, String provinceName);
}
