package org.example.cloud.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.cloud.demo.entity.House;
import org.example.cloud.demo.entity.Resident;
import org.example.cloud.demo.mapper.HouseMapper;
import org.example.cloud.demo.mapper.ResidentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private HouseMapper houseMapper;
    @Autowired
    private ResidentMapper residentMapper;

    // ================== 1. 住房情况统计大屏接口 ==================
    @GetMapping("/stats")
    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();

        // 统计 1：全校房源总数、已分配数量、空闲数量
        long totalHouses = houseMapper.selectCount(null);
        long occupiedHouses = houseMapper.selectCount(new QueryWrapper<House>().eq("status", 1));
        long emptyHouses = houseMapper.selectCount(new QueryWrapper<House>().eq("status", 0));

        stats.put("total_houses", totalHouses);
        stats.put("occupied_houses", occupiedHouses);
        stats.put("empty_houses", emptyHouses);

        // 统计 2：利用 MyBatis-Plus 的聚合查询，统计各学院/部门的入住人数
        // 对应的原生 SQL: SELECT department, COUNT(*) as count FROM resident GROUP BY department
        QueryWrapper<Resident> deptQuery = new QueryWrapper<>();
        deptQuery.select("department", "count(*) as count").groupBy("department");
        List<Map<String, Object>> deptStats = residentMapper.selectMaps(deptQuery);

        stats.put("department_stats", deptStats);

        return stats;
    }

    // ================== 2. 系统参数修改接口 ==================
    /**
     * 允许房产科修改某一套房屋的单价
     * @param houseNo 房号
     * @param newRent 新的每平米租金单价
     */
    @PostMapping("/updateRent")
    public String updateHouseRent(@RequestParam String houseNo, @RequestParam BigDecimal newRent) {
        House house = houseMapper.selectById(houseNo);
        if (house == null) {
            return "错误：找不到该房号！";
        }

        // 修改并落库
        house.setRentPerSqm(newRent);
        houseMapper.updateById(house);

        return "参数更新成功！房号 [" + houseNo + "] 的每平米租金已调整为: " + newRent;
    }
}