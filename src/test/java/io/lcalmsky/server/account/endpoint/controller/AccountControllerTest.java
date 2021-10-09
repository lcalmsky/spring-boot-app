package io.lcalmsky.server.account.endpoint.controller;

import io.lcalmsky.server.account.domain.entity.Account;
import io.lcalmsky.server.account.infra.repository.AccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired AccountRepository accountRepository;
    @MockBean JavaMailSender mailSender;

    @Test
    @DisplayName("회원 가입 화면 진입 확인")
    void signUpForm() throws Exception {
        mockMvc.perform(get("/sign-up"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("account/sign-up"))
                .andExpect(model().attributeExists("signUpForm"));
    }

    @Test
    @DisplayName("회원 가입 처리: 입력값 오류")
    void signUpSubmitWithError() throws Exception {
        mockMvc.perform(post("/sign-up")
                        .param("nickname", "nickname")
                        .param("email", "email@gmail")
                        .param("password", "1234!")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("account/sign-up"));
    }

    @Test
    @DisplayName("회원 가입 처리: 입력값 정상")
    void signUpSubmit() throws Exception {
        mockMvc.perform(post("/sign-up")
                        .param("nickname", "nickname")
                        .param("email", "email@email.com")
                        .param("password", "1234!@#$")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/"));
        assertTrue(accountRepository.existsByEmail("email@email.com"));
        Account account = accountRepository.findByEmail("email@email.com");
        assertNotEquals(account.getPassword(), "1234!@#$");
        assertNotNull(account.getEmailToken());
        then(mailSender)
                .should()
                .send(any(SimpleMailMessage.class));
    }

    @DisplayName("인증 메일 확인: 잘못된 링크")
    @Test
    void verifyEmailWithWrongLink() throws Exception {
        mockMvc.perform(get("/check-email-token")
                        .param("token", "token")
                        .param("email", "email"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/email-verification"))
                .andExpect(model().attributeExists("error"));
    }

    @DisplayName("인증 메일 확인: 유효한 링크")
    @Test
    @Transactional
    void verifyEmail() throws Exception {
        Account account = Account.builder()
                .email("email@email.com")
                .password("1234!@#$")
                .nickname("nickname")
                .notificationSetting(Account.NotificationSetting.builder()
                        .studyCreatedByWeb(true)
                        .studyUpdatedByWeb(true)
                        .studyRegistrationResultByWeb(true)
                        .build())
                .build();
        Account newAccount = accountRepository.save(account);
        newAccount.generateToken();
        mockMvc.perform(get("/check-email-token")
                        .param("token", newAccount.getEmailToken())
                        .param("email", newAccount.getEmail()))
                .andExpect(status().isOk())
                .andExpect(view().name("account/email-verification"))
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attributeExists("numberOfUsers", "nickname"));
    }
}