package org.example.cloud.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.cloud.demo.entity.House;
import org.example.cloud.demo.entity.RentBill;
import org.example.cloud.demo.entity.Resident;
import org.example.cloud.demo.mapper.HouseMapper;
import org.example.cloud.demo.mapper.RentBillMapper;
import org.example.cloud.demo.mapper.ResidentMapper;
import org.example.cloud.demo.util.AesUtil;
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
 * 敏感字段（ownerName, department）写入数据库前 AES 加密，读取后解密。
 */
@Service
public class ResidentService {

    @Autowired
    private ResidentMapper residentMapper;

    @Autowired
    private RentBillMapper rentBillMapper;

    @Autowired
    private HouseMapper houseMapper;

    // ================== 加解密工具方法 ==================

    /** 写入前加密敏感字段 */
    private void encryptResident(Resident r) {
        if (r == null) return;
        r.setOwnerName(AesUtil.encrypt(r.getOwnerName()));
        r.setDepartment(AesUtil.encrypt(r.getDepartment()));
    }

    /** 读取后解密敏感字段 */
    private void decryptResident(Resident r) {
        if (r == null) return;
        r.setOwnerName(AesUtil.decrypt(r.getOwnerName()));
        r.setDepartment(AesUtil.decrypt(r.getDepartment()));
    }

    // ================== 住户管理 ==================

    @Cacheable(value = "residents", key = "'all'")
    public List<Resident> getAllResidents() {
        List<Resident> list = residentMapper.selectList(null);
        list.forEach(this::decryptResident);
        return list;
    }

    public Resident getResidentByName(String ownerName) {
        QueryWrapper<Resident> query = new QueryWrapper<>();
        query.eq("owner_name", AesUtil.encrypt(ownerName));
        Resident r = residentMapper.selectOne(query);
        decryptResident(r);
        return r;
    }

    public Resident getResidentByHouseNo(String houseNo) {
        QueryWrapper<Resident> query = new QueryWrapper<>();
        query.eq("house_no", houseNo);
        Resident r = residentMapper.selectOne(query);
        decryptResident(r);
        return r;
    }

    @CacheEvict(value = "residents", allEntries = true)
    public void updateResident(Resident resident) {
        encryptResident(resident);
        residentMapper.updateById(resident);
        decryptResident(resident);
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
        int newScore = ScoreCalculator.calculate(resident.getTitle(), resident.getFamilySize());
        resident.setScore(newScore);
        encryptResident(resident);
        residentMapper.updateById(resident);
        decryptResident(resident);
        return resident;
    }

    @CacheEvict(value = "residents", allEntries = true)
    public void saveResident(Resident resident) {
        encryptResident(resident);
        residentMapper.insert(resident);
        decryptResident(resident);
    }

    @CacheEvict(value = "residents", allEntries = true)
    public void removeResidentByNameAndHouse(String ownerName, String houseNo) {
        QueryWrapper<Resident> query = new QueryWrapper<>();
        query.eq("owner_name", AesUtil.encrypt(ownerName)).eq("house_no", houseNo);
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
