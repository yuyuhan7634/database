package org.example.cloud.demo.controller;

import org.example.cloud.demo.entity.Resident;
import org.example.cloud.demo.service.ResidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/resident")
public class ResidentController {
    @Autowired
    private ResidentService residentService;

    @GetMapping("/list")
    public List<Resident> list() {
        return residentService.getAllResidents();
    }
}