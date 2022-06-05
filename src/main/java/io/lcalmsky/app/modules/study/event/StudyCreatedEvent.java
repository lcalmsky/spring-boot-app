package io.lcalmsky.app.modules.study.event;

import io.lcalmsky.app.modules.study.domain.entity.Study;
import lombok.Getter;

@Getter
public class StudyCreatedEvent {

    private final Study study;

    public StudyCreatedEvent(Study study) {
        this.study = study;
    }
}
