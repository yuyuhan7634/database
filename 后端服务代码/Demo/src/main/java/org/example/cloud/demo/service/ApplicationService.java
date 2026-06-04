package org.example.cloud.demo.service;

import org.example.cloud.demo.entity.Application;
import org.example.cloud.demo.mapper.ApplicationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ApplicationService {
    @Autowired
    private ApplicationMapper applicationMapper;

    public List<Application> getAllApplications() {
        return applicationMapper.selectList(null);
    }
}