package io.lcalmsky.app.modules.event.domain.entity;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
@NamedEntityGraph(
    name = "Enrollment.withEventAndStudy",
    attributeNodes = {
        @NamedAttributeNode(value = "event", subgraph = "study")
    },
    subgraphs = @NamedSubgraph(name = "study", attributeNodes = @NamedAttributeNode("study"))
)

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

  private boolean attended;


  public static Enrollment of(LocalDateTime enrolledAt, boolean isAbleToAcceptWaitingEnrollment,
      Account account) {
    Enrollment enrollment = new Enrollment();
    enrollment.enrolledAt = enrolledAt;
    enrollment.accepted = isAbleToAcceptWaitingEnrollment;
    enrollment.account = account;
    return enrollment;
  }

  public void accept() {
    this.accepted = true;
  }

  public void reject() {
    this.accepted = false;
  }

  public void attach(Event event) {
    this.event = event;
  }

  public void detachEvent() {
    this.event = null;
  }

  public void attend() {
    this.attended = true;
  }

  public void absent() {
    this.attended = false;
  }
}
