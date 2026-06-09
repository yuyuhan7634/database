package org.example.cloud.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.cloud.demo.entity.House;
import org.example.cloud.demo.entity.HouseStandard;
import org.example.cloud.demo.mapper.HouseMapper;
import org.example.cloud.demo.mapper.HouseStandardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class HouseService {

    @Autowired
    private HouseMapper houseMapper;

    @Autowired
    private HouseStandardMapper houseStandardMapper;

    @Cacheable(value = "houses", key = "'all'")
    public List<House> getAllHouses() {
        return houseMapper.selectList(
                new QueryWrapper<House>().orderByAsc("house_no")
        );
    }

    @Cacheable(value = "houses", key = "'available'")
    public List<House> getAvailableHouses() {
        return houseMapper.selectList(
                new QueryWrapper<House>().eq("status", 0).orderByAsc("house_no")
        );
    }

    @CacheEvict(value = "houses", allEntries = true)
    public void addHouse(House house) {
        houseMapper.insert(house);
    }

    @CacheEvict(value = "houses", allEntries = true)
    public void updateHouse(House house) {
        houseMapper.updateById(house);
    }

    @CacheEvict(value = "houses", allEntries = true)
    public void deleteHouse(String houseNo) {
        houseMapper.deleteById(houseNo);
    }

    public House getHouseByNo(String houseNo) {
        return houseMapper.selectById(houseNo);
    }

    // ================== 住房标准管理 ==================

    @Cacheable(value = "standards", key = "'all'")
    public List<HouseStandard> getAllStandards() {
        return houseStandardMapper.selectList(
                new QueryWrapper<HouseStandard>().orderByAsc("area")
        );
    }

    public List<HouseStandard> getStandardsByAreaRange(BigDecimal minArea, BigDecimal maxArea) {
        QueryWrapper<HouseStandard> query = new QueryWrapper<>();
        if (minArea != null) {
            query.ge("area", minArea);
        }
        if (maxArea != null) {
            query.le("area", maxArea);
        }
        query.orderByAsc("area");
        return houseStandardMapper.selectList(query);
    }

    @Cacheable(value = "standards", key = "#area")
    public HouseStandard getStandardByArea(BigDecimal area) {
        QueryWrapper<HouseStandard> query = new QueryWrapper<>();
        query.eq("area", area);
        return houseStandardMapper.selectOne(query);
    }

    @CacheEvict(value = "standards", allEntries = true)
    public void updateStandard(HouseStandard standard) {
        houseStandardMapper.updateById(standard);
    }

    @CacheEvict(value = "standards", allEntries = true)
    public void saveStandard(HouseStandard standard) {
        houseStandardMapper.insert(standard);
    }
}
