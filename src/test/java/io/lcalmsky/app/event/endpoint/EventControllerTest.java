package io.lcalmsky.app.event.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lcalmsky.app.WithAccount;
import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.infra.repository.AccountRepository;
import io.lcalmsky.app.event.domain.entity.EventType;
import io.lcalmsky.app.event.form.EventForm;
import io.lcalmsky.app.event.infra.repository.EventRepository;
import io.lcalmsky.app.study.application.StudyService;
import io.lcalmsky.app.study.form.StudyForm;
import io.lcalmsky.app.study.infra.repository.StudyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class EventControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired AccountRepository accountRepository;
    @Autowired StudyService studyService;
    @Autowired StudyRepository studyRepository;
    @Autowired EventRepository eventRepository;
    @Autowired ObjectMapper objectMapper;
    private final String studyPath = "study-path";

    @BeforeEach
    void beforeEach() {
        Account account = accountRepository.findByNickname("jaime");
        studyService.createNewStudy(StudyForm.builder()
                .path(studyPath)
                .shortDescription("short-description")
                .fullDescription("full-description")
                .title("title")
                .build(), account);
    }

    @AfterEach
    void afterEach() {
        studyRepository.deleteAll();
    }

    @Test
    @DisplayName("이벤트 폼")
    @WithAccount("jaime")
    void eventForm() throws Exception {
        mockMvc.perform(get("/study/" + studyPath + "/new-event"))
                .andExpect(status().isOk())
                .andExpect(view().name("event/form"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("study"))
                .andExpect(model().attributeExists("eventForm"));

    }

//    @Test
//    @DisplayName("모임 생성 성공")
//    @WithAccount("jaime")
//    void createEvent() throws Exception {
//        LocalDateTime now = LocalDateTime.now();
//        EventForm eventForm = EventForm.builder()
//                .description("description")
//                .eventType(EventType.FCFS)
//                .endDateTime(now.plusWeeks(3))
//                .endEnrollmentDateTime(now.plusWeeks(1))
//                .limitOfEnrollments(5)
//                .startDateTime(now.plusWeeks(2))
//                .title("title")
//                .build();
//        mockMvc.perform(post("/study/" + studyPath + "/new-event")
//                        .content(objectMapper.writeValueAsString(eventForm))
//                        .with(csrf()))
//                .andExpect(status().is3xxRedirection());
//    }
//
//    @Test
//    @DisplayName("모임 생성 실패")
//    @WithAccount("jaime")
//    void createEventWithErrors() throws Exception {
//        LocalDateTime now = LocalDateTime.now();
//        EventForm eventForm = EventForm.builder()
//                .description("description")
//                .eventType(EventType.FCFS)
//                .endDateTime(now.plusWeeks(3))
//                .endEnrollmentDateTime(now.plusWeeks(1))
//                .limitOfEnrollments(5)
//                .startDateTime(now.plusWeeks(4))
//                .title("title")
//                .build();
//        mockMvc.perform(post("/study/" + studyPath + "/new-event")
//                        .content(objectMapper.writeValueAsString(eventForm))
//                        .with(csrf()))
//                .andExpect(status().isOk());
//    }
}