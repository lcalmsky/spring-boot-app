package io.lcalmsky.app.modules.event.validator;

import io.lcalmsky.app.modules.event.domain.entity.Event;
import io.lcalmsky.app.modules.event.endpoint.form.EventForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.LocalDateTime;

@Component
public class EventValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return EventForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        EventForm eventForm = (EventForm) target;
        if (isEarlierThanNow(eventForm.getEndEnrollmentDateTime())) {
            errors.rejectValue("endEnrollmentDateTime", "wrong.datetime", "모임 접수 종료 일시를 정확히 입력하세요.");
        }
        if (isEarlierThan(eventForm.getEndDateTime(), eventForm.getStartDateTime())
                || isEarlierThan(eventForm.getEndDateTime(), eventForm.getEndEnrollmentDateTime())
                || isEarlierThanNow(eventForm.getEndDateTime())) {
            errors.rejectValue("endDateTime", "wrong.datetime", "모임 종료 일시를 정확히 입력하세요.");
        }
        if (isEarlierThanNow(eventForm.getStartDateTime())) {
            errors.rejectValue("startDateTime", "wrong.datetime", "모임 시작 일시를 정확히 입력하세요.");
        }
    }

    private boolean isEarlierThanNow(LocalDateTime time) {
        return time.isBefore(LocalDateTime.now());
    }

    private boolean isEarlierThan(LocalDateTime time, LocalDateTime targetTime) {
        return time.isBefore(targetTime);
    }

    public void validateUpdateForm(EventForm eventForm, Event event, Errors errors) {
        if (eventForm.getLimitOfEnrollments() < event.getNumberOfAcceptedEnrollments()) {
            errors.rejectValue("limitOfEnrollments", "wrong.value", "확인된 참가 신청보다 모집 인원 수가 커야 합니다.");
        }
    }
}
