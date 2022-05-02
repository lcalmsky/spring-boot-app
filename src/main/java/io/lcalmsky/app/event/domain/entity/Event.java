package io.lcalmsky.app.event.domain.entity;

import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.study.domain.entity.Study;
import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
@EqualsAndHashCode(of = "id")
public class Event {
  @Id
  @GeneratedValue
  private Long id;

  @ManyToOne
  private Study study;

  @ManyToOne
  private Account createdBy;

  @Column(nullable = false)
  private String title;

  @Lob
  private String description;

  @Column(nullable = false)
  private LocalDateTime createdDateTime;

  @Column(nullable = false)
  private LocalDateTime endEnrollmentDateTime;

  @Column(nullable = false)
  private LocalDateTime startDateTime;

  @Column(nullable = false)
  private LocalDateTime endDateTime;

  private Integer limitOfEnrollments;

  @OneToMany(mappedBy = "event")
  private List<Enrollment> enrollments;

  @Enumerated(EnumType.STRING)
  private EventType eventType;

}
