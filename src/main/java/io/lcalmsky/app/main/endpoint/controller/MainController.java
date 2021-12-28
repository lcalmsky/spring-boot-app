package io.lcalmsky.app.main.endpoint.controller;

import io.lcalmsky.app.account.domain.entity.Account;
import io.lcalmsky.app.account.support.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
    @GetMapping("/")
    public String home(@CurrentUser Account account, Model model) {
        if (account != null) {
            model.addAttribute(account);
        }
        return "index";
    }
}
