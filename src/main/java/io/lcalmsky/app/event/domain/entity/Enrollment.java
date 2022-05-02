package io.lcalmsky.app.event.domain.entity;

import io.lcalmsky.app.account.domain.entity.Account;
import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
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
public class Enrollment {

  @Id
  @GeneratedValue
  private Long id;

  @ManyToOne
  private Event event;

  @ManyToOne
  private Account account;

  private LocalDateTime enrolledAt;

  private boolean accepted;

  private boolean attend;
}
