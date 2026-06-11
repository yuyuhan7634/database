package org.example.cloud.demo.controller;

import org.example.cloud.demo.common.Result;
import org.example.cloud.demo.entity.House;
import org.example.cloud.demo.entity.HouseStandard;
import org.example.cloud.demo.service.HouseService;
import org.example.cloud.demo.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private HouseService houseService;

    @Autowired
    private StatisticsService statisticsService;

    // ================== 1. 综合统计表接口 ==================

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = statisticsService.generateStatistics();
        return Result.success("统计数据获取成功", stats);
    }

    // ================== 2. 阈值分数查询 ==================

    @GetMapping("/threshold")
    public Result<List<HouseStandard>> getThresholds() {
        List<HouseStandard> list = houseService.getAllStandards();
        return Result.success("阈值查询成功", list);
    }

    // ================== 3. 房源详情查询 ==================

    @GetMapping("/house-info")
    public Result<House> getHouseInfo(@RequestParam String houseNo) {
        House house = houseService.getHouseByNo(houseNo);
        if (house == null) {
            return Result.error("未找到该房源信息！");
        }
        return Result.success("房源详情查询成功", house);
    }

    // ================== 4. 住房条件查询 ==================

    @GetMapping("/housing-conditions")
    public Result<List<HouseStandard>> getHousingConditions(
            @RequestParam(required = false) BigDecimal minArea,
            @RequestParam(required = false) BigDecimal maxArea) {
        List<HouseStandard> standards = houseService.getStandardsByAreaRange(minArea, maxArea);
        return Result.success("住房条件查询成功", standards);
    }

    // ================== 5. 修改住房标准 ==================

    @PostMapping("/updateStandard")
    public Result<String> updateStandard(@RequestParam BigDecimal area, @RequestParam Integer minScore) {
        HouseStandard standard = houseService.getStandardByArea(area);
        if (standard == null) {
            return Result.error("错误：面积为 " + area + " 的住房标准不存在！");
        }
        standard.setMinScore(minScore);
        houseService.updateStandard(standard);
        return Result.success("住房标准更新成功！面积 " + area + " ㎡ 的最低分数已调整为: " + minScore + " 分");
    }

    // ================== 6. 租金调整接口 ==================

    @PostMapping("/updateRent")
    public Result<String> updateHouseRent(@RequestParam String houseNo, @RequestParam BigDecimal newRent) {
        House house = houseService.getHouseByNo(houseNo);
        if (house == null) {
            return Result.error("错误：找不到该房号！");
        }
        house.setRentPerSqm(newRent);
        houseService.updateHouse(house);
        return Result.success("参数更新成功！房号 [" + houseNo + "] 的每平米租金已调整为: " + newRent);
    }

    // ================== 7. 房屋增删改接口 ==================

    @PostMapping("/addHouse")
    public Result<String> addHouse(@RequestParam String houseNo,
                                   @RequestParam BigDecimal area,
                                   @RequestParam BigDecimal rentPerSqm) {
        House existing = houseService.getHouseByNo(houseNo);
        if (existing != null) {
            return Result.error("错误：房号 [" + houseNo + "] 已存在！");
        }
        House house = new House();
        house.setHouseNo(houseNo);
        house.setArea(area);
        house.setStatus(0); // 默认空闲
        house.setRentPerSqm(rentPerSqm);
        houseService.addHouse(house);
        return Result.success("房屋添加成功！房号: " + houseNo);
    }

    @PostMapping("/updateHouse")
    public Result<String> updateHouse(@RequestParam String houseNo,
                                      @RequestParam(required = false) BigDecimal area,
                                      @RequestParam(required = false) BigDecimal rentPerSqm,
                                      @RequestParam(required = false) Integer status) {
        House house = houseService.getHouseByNo(houseNo);
        if (house == null) {
            return Result.error("错误：找不到该房号！");
        }
        if (area != null) house.setArea(area);
        if (rentPerSqm != null) house.setRentPerSqm(rentPerSqm);
        if (status != null) house.setStatus(status);
        houseService.updateHouse(house);
        return Result.success("房屋信息更新成功！房号: " + houseNo);
    }

    @PostMapping("/deleteHouse")
    public Result<String> deleteHouse(@RequestParam String houseNo) {
        House house = houseService.getHouseByNo(houseNo);
        if (house == null) {
            return Result.error("错误：找不到该房号！");
        }
        if (house.getStatus() == 1) {
            return Result.error("错误：该房屋当前有人居住，无法删除！");
        }
        houseService.deleteHouse(houseNo);
        return Result.success("房屋删除成功！房号: " + houseNo);
    }
}
