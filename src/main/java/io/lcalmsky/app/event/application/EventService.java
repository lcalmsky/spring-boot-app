package io.lcalmsky.app.event.application;

import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.event.domain.entity.Event;
import io.lcalmsky.app.event.form.EventForm;
import io.lcalmsky.app.event.infra.repository.EventRepository;
import io.lcalmsky.app.study.domain.entity.Study;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    public Event createEvent(Study study, EventForm eventForm, Account account) {
        Event event = Event.from(eventForm, account, study);
        return eventRepository.save(event);
    }

    public void updateEvent(Event event, EventForm eventForm) {
        event.updateFrom(eventForm);
    }

    public void deleteEvent(Event event) {
        eventRepository.delete(event);
    }
}
