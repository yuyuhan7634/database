package org.example.cloud.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.cloud.demo.entity.House;
import org.example.cloud.demo.entity.RentBill;
import org.example.cloud.demo.entity.Resident;
import org.example.cloud.demo.mapper.HouseMapper;
import org.example.cloud.demo.mapper.RentBillMapper;
import org.example.cloud.demo.mapper.ResidentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 住户服务（住户信息管理 + 房租账单管理）
 */
@Service
public class ResidentService {

    @Autowired
    private ResidentMapper residentMapper;

    @Autowired
    private RentBillMapper rentBillMapper;

    @Autowired
    private HouseMapper houseMapper;

    // ================== 住户管理 ==================

    @Cacheable(value = "residents", key = "'all'")
    public List<Resident> getAllResidents() {
        return residentMapper.selectList(null);
    }

    public Resident getResidentByName(String ownerName) {
        QueryWrapper<Resident> query = new QueryWrapper<>();
        query.eq("owner_name", ownerName);
        return residentMapper.selectOne(query);
    }

    public Resident getResidentByHouseNo(String houseNo) {
        QueryWrapper<Resident> query = new QueryWrapper<>();
        query.eq("house_no", houseNo);
        return residentMapper.selectOne(query);
    }

    @CacheEvict(value = "residents", allEntries = true)
    public void updateResident(Resident resident) {
        residentMapper.updateById(resident);
    }

    /**
     * 更新住户个人信息（部门、职称、家庭人口），并重新计算住房分数
     */
    @CacheEvict(value = "residents", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public Resident updateResidentInfo(String ownerName, String department, String title, Integer familySize) {
        Resident resident = getResidentByName(ownerName);
        if (resident == null) {
            throw new RuntimeException("未找到该住户信息！");
        }
        if (department != null && !department.trim().isEmpty()) {
            resident.setDepartment(department);
        }
        if (title != null && !title.trim().isEmpty()) {
            resident.setTitle(title);
        }
        if (familySize != null && familySize > 0) {
            resident.setFamilySize(familySize);
        }
        // 重新计算住房分数
        int newScore = calculateHousingScore(resident.getTitle(), resident.getFamilySize());
        resident.setScore(newScore);
        residentMapper.updateById(resident);
        return resident;
    }

    /** 计算住房分数（与 ApplicationService 保持一致） */
    private int calculateHousingScore(String title, Integer familySize) {
        int baseScore = 40;
        if (title != null) {
            if (title.contains("教授") || title.contains("处长")) baseScore = 100;
            else if (title.contains("科长") || title.contains("研究员")) baseScore = 80;
            else if (title.contains("讲师") || title.contains("工程师")) baseScore = 60;
            else if (title.contains("实验员") || title.contains("干事")) baseScore = 50;
        }
        return baseScore + (familySize != null ? familySize : 1) * 10;
    }

    @CacheEvict(value = "residents", allEntries = true)
    public void saveResident(Resident resident) {
        residentMapper.insert(resident);
    }

    @CacheEvict(value = "residents", allEntries = true)
    public void removeResidentByNameAndHouse(String ownerName, String houseNo) {
        QueryWrapper<Resident> query = new QueryWrapper<>();
        query.eq("owner_name", ownerName).eq("house_no", houseNo);
        residentMapper.delete(query);
    }

    // ================== 房租账单管理 ==================

    @Cacheable(value = "bills", key = "'all'")
    public List<RentBill> getAllBills() {
        return rentBillMapper.selectList(
                new QueryWrapper<RentBill>().orderByDesc("bill_month").orderByAsc("house_no")
        );
    }

    @CacheEvict(value = "bills", allEntries = true)
    public void deleteBillsByNameAndHouse(String ownerName, String houseNo) {
        QueryWrapper<RentBill> query = new QueryWrapper<>();
        query.eq("owner_name", ownerName).eq("house_no", houseNo);
        rentBillMapper.delete(query);
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "bills", allEntries = true)
    public int generateMonthlyBills(String month) {
        int count = 0;
        QueryWrapper<House> houseQuery = new QueryWrapper<>();
        houseQuery.eq("status", 1);
        List<House> occupiedHouses = houseMapper.selectList(houseQuery);

        for (House house : occupiedHouses) {
            Resident resident = getResidentByHouseNo(house.getHouseNo());
            if (resident != null) {
                QueryWrapper<RentBill> billQuery = new QueryWrapper<>();
                billQuery.eq("house_no", house.getHouseNo()).eq("bill_month", month);
                if (rentBillMapper.selectCount(billQuery) == 0) {
                    BigDecimal rentAmount = house.getArea().multiply(house.getRentPerSqm());
                    RentBill bill = new RentBill();
                    bill.setHouseNo(house.getHouseNo());
                    bill.setOwnerName(resident.getOwnerName());
                    bill.setBillMonth(month);
                    bill.setRentAmount(rentAmount);
                    bill.setCreateTime(LocalDate.now());
                    rentBillMapper.insert(bill);
                    count++;
                }
            }
        }
        return count;
    }
}
