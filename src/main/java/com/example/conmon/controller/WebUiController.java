package com.example.conmon.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebUiController {

    @GetMapping({"/", "/index", "/index.html"})
    public String index() {
        return "index";
    }

    @GetMapping({"/status", "/status.html"})
    public String status() {
        return "status";
    }

    @GetMapping({"/connections", "/connections.html"})
    public String connections() {
        return "connections";
    }

    @GetMapping({"/events", "/events.html"})
    public String events() {
        return "events";
    }

    @GetMapping({"/services", "/services.html"})
    public String services() {
        return "services";
    }

    @GetMapping({"/clients", "/clients.html"})
    public String clients() {
        return "clients";
    }

    @GetMapping({"/config", "/config.html"})
    public String config() {
        return "config";
    }
}
