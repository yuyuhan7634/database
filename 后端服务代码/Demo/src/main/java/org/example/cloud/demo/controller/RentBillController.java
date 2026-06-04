package org.example.cloud.demo.controller;

import org.example.cloud.demo.entity.RentBill;
import org.example.cloud.demo.service.RentBillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bill")
public class RentBillController {

    @Autowired
    private RentBillService rentBillService;

    // 接口 1：查询所有账单列表
    @GetMapping("/list")
    public List<RentBill> list() {
        return rentBillService.getAllBills();
    }

    // 🌟 接口 2：触发一键生成账单
    @PostMapping("/generate")
    public String generateBills(@RequestParam String month) {
        try {
            int count = rentBillService.generateMonthlyBills(month);
            return "执行成功！已为全校完成结算，共生成了 " + count + " 份 [" + month + "] 月份的房租账单。";
        } catch (Exception e) {
            return "账单生成失败：" + e.getMessage();
        }
    }
}