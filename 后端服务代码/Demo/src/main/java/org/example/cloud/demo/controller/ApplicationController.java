package org.example.cloud.demo.controller;

import org.example.cloud.demo.common.Result;
import org.example.cloud.demo.dto.ApplicationSubmitDTO;
import org.example.cloud.demo.entity.Application;
import org.example.cloud.demo.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/application")
public class ApplicationController {
    @Autowired
    private ApplicationService applicationService;
    @GetMapping("/list")
    public List<Application> list() {
        return applicationService.getAllApplications();
    }
    @GetMapping("/queue")
    public Result<List<Application>> getQueue() {
        return Result.success(applicationService.getAllocationQueue());
    }
    @PostMapping("/approve")
    public Result<String> approve(@RequestParam String applyNo) {
        try {
            String houseNo = applicationService.approveHousing(applyNo);
            return Result.success("智能分房成功！已为该住户自动匹配并分配房源：" + houseNo);
        } catch (Exception e) {
            return Result.error("分房失败：" + e.getMessage());
        }
    }
    @PostMapping("/checkout")
    public String checkout(@RequestParam String applyNo) {
        try {
            boolean success = applicationService.approveCheckout(applyNo);
            return success ? "退房审批成功！房屋已回收为空闲状态，名下账单已清理，住户记录已注销。" : "退房失败！";
        } catch (Exception e) {
            return "退房被拦截：" + e.getMessage();
        }
    }
    @PostMapping("/transfer")
    public org.example.cloud.demo.common.Result<String> transfer(@RequestParam String applyNo) {
        try {
            String slip = applicationService.approveTransfer(applyNo);
            return org.example.cloud.demo.common.Result.success("智能调房成功！", slip);
        } catch (Exception e) {
            return org.example.cloud.demo.common.Result.error("调房被拦截：" + e.getMessage());
        }
    }
    @PostMapping("/submit")
    public Result<String> submitApplication(@RequestBody ApplicationSubmitDTO dto) {
        try {
            // 调用 Service 层处理申请单
            String message = applicationService.submitApplication(dto);
            // 处理成功，返回 200 和成功信息
            return Result.success(message);
        } catch (Exception e) {
            // 处理失败（如分数不够被拦截），返回 500 和驳回原因
            return Result.error(e.getMessage());
        }
    }
    @PostMapping("/allocate/monthly")
    public Result<String> manualAllocate() {
        try {
            String resultMsg = applicationService.allocatePendingApplications();
            return Result.success("批量分房执行完毕！", resultMsg);
        } catch (Exception e) {
            return Result.error("批量分房执行失败：" + e.getMessage());
        }
    }
}