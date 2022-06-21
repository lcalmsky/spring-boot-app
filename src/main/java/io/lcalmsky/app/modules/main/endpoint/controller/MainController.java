package io.lcalmsky.app.modules.main.endpoint.controller;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
import io.lcalmsky.app.modules.account.support.CurrentUser;
import io.lcalmsky.app.modules.event.infra.repository.EnrollmentRepository;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.infra.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MainController {

  private final StudyRepository studyRepository;
  private final AccountRepository accountRepository;
  private final EnrollmentRepository enrollmentRepository;

  @GetMapping("/")
  public String home(@CurrentUser Account account, Model model) {
    if (account != null) {
      Account accountLoaded = accountRepository.findAccountWithTagsAndZonesById(account.getId());
      model.addAttribute(accountLoaded);
      model.addAttribute("enrollmentList",
          enrollmentRepository.findByAccountAndAcceptedOrderByEnrolledAtDesc(accountLoaded, true));
      model.addAttribute("studyList",
          studyRepository.findByAccount(accountLoaded.getTags(), accountLoaded.getZones()));
      model.addAttribute("studyManagerOf",
          studyRepository.findFirst5ByManagersContainingAndClosedOrderByPublishedDateTimeDesc(
              account, false));
      model.addAttribute("studyMemberOf",
          studyRepository.findFirst5ByMembersContainingAndClosedOrderByPublishedDateTimeDesc(
              account, false));
      return "home";
    }
    model.addAttribute("studyList",
        studyRepository.findFirst9ByPublishedAndClosedOrderByPublishedDateTimeDesc(true, false));
    return "index";
  }

  @GetMapping("/login")
  public String login() {
    return "login";
  }

  @GetMapping("/search/study")
  public String searchStudy(String keyword, Model model,
      @PageableDefault(size = 9, sort = "publishedDateTime", direction = Sort.Direction.ASC) Pageable pageable) {
    Page<Study> studyPage = studyRepository.findByKeyword(keyword, pageable);
    model.addAttribute("studyPage", studyPage);
    model.addAttribute("keyword", keyword);
    model.addAttribute("sortProperty", pageable.getSort().toString().contains("publishedDateTime")
        ? "publishedDateTime"
        : "memberCount");
    return "search";
  }
}