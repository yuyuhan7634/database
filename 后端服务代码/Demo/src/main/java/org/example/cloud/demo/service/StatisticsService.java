package org.example.cloud.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.cloud.demo.entity.Application;
import org.example.cloud.demo.entity.House;
import org.example.cloud.demo.entity.Resident;
import org.example.cloud.demo.mapper.ApplicationMapper;
import org.example.cloud.demo.mapper.HouseMapper;
import org.example.cloud.demo.mapper.ResidentMapper;
import org.example.cloud.demo.util.AesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 统计报表服务
 * 从 ApplicationService 拆分出来，专门负责数据统计，降低单体 Service 的复杂度
 */
@Service
public class StatisticsService {

    @Autowired
    private HouseMapper houseMapper;

    @Autowired
    private ResidentMapper residentMapper;

    @Autowired
    private ApplicationMapper applicationMapper;

    // ================== 综合统计报表 ==================

    public Map<String, Object> generateStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();

        // 房屋统计
        long totalHouses = houseMapper.selectCount(null);
        long emptyHouses = houseMapper.selectCount(new QueryWrapper<House>().eq("status", 0));
        long occupiedHouses = totalHouses - emptyHouses;
        stats.put("totalHouses", totalHouses);
        stats.put("emptyHouses", emptyHouses);
        stats.put("occupiedHouses", occupiedHouses);
        stats.put("occupancyRate", String.format("%.2f%%",
                totalHouses > 0 ? (double) occupiedHouses / totalHouses * 100 : 0));

        // 按部门统计
        QueryWrapper<Resident> deptQuery = new QueryWrapper<>();
        deptQuery.select("department", "count(*) as count").groupBy("department");
        List<Map<String, Object>> deptStats = residentMapper.selectMaps(deptQuery);
        deptStats.forEach(m -> {
            Object dept = m.get("department");
            if (dept != null) m.put("department", AesUtil.decrypt(dept.toString()));
        });
        stats.put("departmentStats", deptStats);

        // 按职称统计
        QueryWrapper<Resident> titleQuery = new QueryWrapper<>();
        titleQuery.select("title", "count(*) as count").groupBy("title");
        stats.put("titleStats", residentMapper.selectMaps(titleQuery));

        // 按面积区间统计
        List<Resident> allResidents = residentMapper.selectList(null);
        allResidents.forEach(r -> {
            r.setOwnerName(AesUtil.decrypt(r.getOwnerName()));
            r.setDepartment(AesUtil.decrypt(r.getDepartment()));
        });
        long smallHouse = allResidents.stream()
                .filter(r -> r.getHouseArea() != null && r.getHouseArea().compareTo(new BigDecimal("60")) <= 0).count();
        long mediumHouse = allResidents.stream()
                .filter(r -> r.getHouseArea() != null
                        && r.getHouseArea().compareTo(new BigDecimal("60")) > 0
                        && r.getHouseArea().compareTo(new BigDecimal("100")) <= 0).count();
        long largeHouse = allResidents.stream()
                .filter(r -> r.getHouseArea() != null && r.getHouseArea().compareTo(new BigDecimal("100")) > 0).count();
        stats.put("smallHouseCount", smallHouse);
        stats.put("mediumHouseCount", mediumHouse);
        stats.put("largeHouseCount", largeHouse);

        // 空闲房屋列表
        List<House> emptyHouseList = houseMapper.selectList(
                new QueryWrapper<House>().eq("status", 0).orderByAsc("house_no"));
        stats.put("emptyHouseList", emptyHouseList);

        // 申请量统计
        long pendingApps = applicationMapper.selectCount(new QueryWrapper<Application>().eq("apply_status", 0));
        long approvedApps = applicationMapper.selectCount(new QueryWrapper<Application>().eq("apply_status", 1));
        long rejectedApps = applicationMapper.selectCount(new QueryWrapper<Application>().eq("apply_status", 2));
        stats.put("pendingApplications", pendingApps);
        stats.put("approvedApplications", approvedApps);
        stats.put("rejectedApplications", rejectedApps);

        return stats;
    }
}
