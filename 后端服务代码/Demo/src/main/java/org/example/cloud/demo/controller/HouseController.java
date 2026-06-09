package org.example.cloud.demo.controller;

import org.example.cloud.demo.common.Result;
import org.example.cloud.demo.entity.House;
import org.example.cloud.demo.entity.HouseStandard;
import org.example.cloud.demo.service.HouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/house")
public class HouseController {

    @Autowired
    private HouseService houseService;

    // ================== 房屋管理 ==================

    @GetMapping("/list")
    public Result<List<House>> list() {
        return Result.success(houseService.getAllHouses());
    }

    @GetMapping("/available")
    public Result<List<House>> available() {
        return Result.success(houseService.getAvailableHouses());
    }

    @GetMapping("/info")
    public Result<House> info(@RequestParam String houseNo) {
        House house = houseService.getHouseByNo(houseNo);
        if (house == null) {
            return Result.error("未找到该房源信息！");
        }
        return Result.success(house);
    }

    // ================== 住房标准管理 ==================

    @GetMapping("/standard/list")
    public Result<List<HouseStandard>> standardList() {
        return Result.success(houseService.getAllStandards());
    }

    @GetMapping("/standard/info")
    public Result<HouseStandard> standardInfo(@RequestParam java.math.BigDecimal area) {
        HouseStandard standard = houseService.getStandardByArea(area);
        if (standard == null) {
            return Result.error("未找到该面积的住房标准！");
        }
        return Result.success(standard);
    }
}
