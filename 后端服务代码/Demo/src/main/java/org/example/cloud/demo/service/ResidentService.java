package org.example.cloud.demo.service;

import org.example.cloud.demo.entity.Resident;
import org.example.cloud.demo.mapper.ResidentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ResidentService {
    @Autowired
    private ResidentMapper residentMapper;

    public List<Resident> getAllResidents() {
        return residentMapper.selectList(null);
    }
}