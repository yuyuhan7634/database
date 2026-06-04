package org.example.cloud.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.cloud.demo.entity.House;
import org.example.cloud.demo.entity.RentBill;
import org.example.cloud.demo.entity.Resident;
import org.example.cloud.demo.mapper.HouseMapper;
import org.example.cloud.demo.mapper.RentBillMapper;
import org.example.cloud.demo.mapper.ResidentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class RentBillService {

    @Autowired
    private RentBillMapper rentBillMapper;
    @Autowired
    private HouseMapper houseMapper;
    @Autowired
    private ResidentMapper residentMapper;
    public List<RentBill> getAllBills() {
        return rentBillMapper.selectList(null);
    }
    @Transactional(rollbackFor = Exception.class)
    public int generateMonthlyBills(String month) {
        int count = 0;
        QueryWrapper<House> houseQuery = new QueryWrapper<>();
        houseQuery.eq("status", 1);
        List<House> occupiedHouses = houseMapper.selectList(houseQuery);

        for (House house : occupiedHouses) {
            QueryWrapper<Resident> residentQuery = new QueryWrapper<>();
            residentQuery.eq("house_no", house.getHouseNo());
            Resident resident = residentMapper.selectOne(residentQuery);

            if (resident != null) {
                QueryWrapper<RentBill> billQuery = new QueryWrapper<>();
                billQuery.eq("house_no", house.getHouseNo())
                        .eq("bill_month", month);
                if (rentBillMapper.selectCount(billQuery) == 0) {
                    BigDecimal rentAmount = house.getArea().multiply(house.getRentPerSqm());
                    RentBill bill = new RentBill();
                    bill.setHouseNo(house.getHouseNo());
                    bill.setOwnerName(resident.getOwnerName());
                    bill.setBillMonth(month);
                    bill.setRentAmount(rentAmount);
                    bill.setCreateTime(LocalDate.now()); // 记录生成时间
                    rentBillMapper.insert(bill);
                    count++;
                }
            }
        }
        return count;
    }
}