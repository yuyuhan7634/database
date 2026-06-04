package org.example.cloud.demo.controller;

import org.example.cloud.demo.entity.Application;
import org.example.cloud.demo.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/application")
public class ApplicationController {
    @Autowired
    private ApplicationService applicationService;

    @GetMapping("/list")
    public List<Application> list() {
        return applicationService.getAllApplications();
    }
}