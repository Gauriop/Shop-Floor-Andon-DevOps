package com.andon.dashboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class LoginController {

    @GetMapping("/")
    public String root() {
        return "redirect:/events";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/health")
    @ResponseBody
    public String health() {
        return "OK";
    }
}