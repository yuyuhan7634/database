package org.example.cloud.demo.controller;

import org.example.cloud.demo.common.Result;
import org.example.cloud.demo.entity.RentBill;
import org.example.cloud.demo.entity.Resident;
import org.example.cloud.demo.service.ResidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/resident")
public class ResidentController {

    @Autowired
    private ResidentService residentService;

    // ================== 住户管理 ==================

    @GetMapping("/list")
    public Result<List<Resident>> list() {
        return Result.success(residentService.getAllResidents());
    }

    @PostMapping("/updateInfo")
    public Result<Resident> updateInfo(@RequestParam String ownerName,
                                       @RequestParam(required = false) String department,
                                       @RequestParam(required = false) String title,
                                       @RequestParam(required = false) Integer familySize) {
        Resident updated = residentService.updateResidentInfo(ownerName, department, title, familySize);
        return Result.success("个人信息更新成功！住房分数已重新计算为: " + updated.getScore() + " 分", updated);
    }

    // ================== 房租账单管理 ==================

    @GetMapping("/bill/list")
    public Result<List<RentBill>> billList() {
        return Result.success(residentService.getAllBills());
    }

    @PostMapping("/bill/generate")
    public Result<String> generateBills(@RequestParam String month) {
        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            return Result.error("月份格式错误，应为 yyyy-MM，例如 2026-06");
        }
        int count = residentService.generateMonthlyBills(month);
        return Result.success("执行成功！已生成 " + count + " 份 [" + month + "] 月份的房租账单。");
    }
}
