package org.example.cloud.demo.service;

import org.example.cloud.demo.entity.RentBill;
import org.example.cloud.demo.mapper.RentBillMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RentBillService {
    @Autowired
    private RentBillMapper rentBillMapper;

    public List<RentBill> getAllBills() {
        return rentBillMapper.selectList(null);
    }
}