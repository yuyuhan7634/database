package org.example.cloud.demo.controller;

import org.example.cloud.demo.entity.RentBill;
import org.example.cloud.demo.service.RentBillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/bill")
public class RentBillController {
    @Autowired
    private RentBillService rentBillService;

    @GetMapping("/list")
    public List<RentBill> list() {
        return rentBillService.getAllBills();
    }
}