package org.example.cloud.demo.service;

import org.example.cloud.demo.entity.HouseStandard;
import org.example.cloud.demo.mapper.HouseStandardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class HouseStandardService {

    @Autowired
    private HouseStandardMapper houseStandardMapper;

    public List<HouseStandard> getAllStandards() {
        return houseStandardMapper.selectList(null); // 查询所有住房标准
    }
}