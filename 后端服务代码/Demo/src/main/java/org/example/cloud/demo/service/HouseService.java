package org.example.cloud.demo.service;

import org.example.cloud.demo.entity.House;
import org.example.cloud.demo.mapper.HouseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class HouseService {

    @Autowired
    private HouseMapper houseMapper;

    public List<House> getAllHouses() {
        return houseMapper.selectList(null);
    }
}