package io.lcalmsky.app.event.application;

import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.event.domain.entity.Enrollment;
import io.lcalmsky.app.event.domain.entity.Event;
import io.lcalmsky.app.event.form.EventForm;
import io.lcalmsky.app.event.infra.repository.EnrollmentRepository;
import io.lcalmsky.app.event.infra.repository.EventRepository;
import io.lcalmsky.app.study.domain.entity.Study;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EnrollmentRepository enrollmentRepository;

    public Event createEvent(Study study, EventForm eventForm, Account account) {
        Event event = Event.from(eventForm, account, study);
        return eventRepository.save(event);
    }

    public void updateEvent(Event event, EventForm eventForm) {
        event.updateFrom(eventForm);
        event.acceptWaitingList();
    }

    public void deleteEvent(Event event) {
        eventRepository.delete(event);
    }

    public void enroll(Event event, Account account) {
        if (!enrollmentRepository.existsByEventAndAccount(event, account)) {
            Enrollment enrollment = Enrollment.of(LocalDateTime.now(), event.isAbleToAcceptWaitingEnrollment(), account);
            event.addEnrollment(enrollment);
            enrollmentRepository.save(enrollment);
        }
    }

    public void leave(Event event, Account account) {
        Enrollment enrollment = enrollmentRepository.findByEventAndAccount(event, account);
        event.removeEnrollment(enrollment);
        enrollmentRepository.delete(enrollment);
        event.acceptNextIfAvailable();
    }
}
