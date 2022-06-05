package io.lcalmsky.app.modules.notification.infra.repository;

import io.lcalmsky.app.modules.notification.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
