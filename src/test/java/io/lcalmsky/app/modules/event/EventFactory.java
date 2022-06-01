package io.lcalmsky.app.modules.event;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.event.application.EventService;
import io.lcalmsky.app.modules.event.domain.entity.Event;
import io.lcalmsky.app.modules.event.domain.entity.EventType;
import io.lcalmsky.app.modules.event.endpoint.form.EventForm;
import io.lcalmsky.app.modules.event.infra.repository.EventRepository;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.infra.repository.StudyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EventFactory {
    @Autowired EventRepository eventRepository;
    @Autowired StudyRepository studyRepository;
    @Autowired EventService eventService;

    public Event createEvent(EventType eventType, Account account, String studyPath) {
        Study study = studyRepository.findByPath(studyPath);
        LocalDateTime now = LocalDateTime.now();
        EventForm eventForm = EventForm.builder()
                .description("description")
                .eventType(eventType)
                .endDateTime(now.plusWeeks(3))
                .endEnrollmentDateTime(now.plusWeeks(1))
                .limitOfEnrollments(2)
                .startDateTime(now.plusWeeks(2))
                .title("title")
                .build();
        return eventService.createEvent(study, eventForm, account);
    }
}
