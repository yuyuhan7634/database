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
    public Result<List<Application>> list() {
        return Result.success(applicationService.getAllApplications());
    }

    @GetMapping("/queue")
    public Result<List<Application>> getQueue() {
        return Result.success(applicationService.getAllocationQueue());
    }

    @PostMapping("/approve")
    public Result<String> approve(@RequestParam String applyNo) {
        try {
            String slip = applicationService.approveHousing(applyNo);
            return Result.success("智能分房成功！", slip);
        } catch (Exception e) {
            return Result.error("分房失败：" + e.getMessage());
        }
    }

    @PostMapping("/checkout")
    public Result<String> checkout(@RequestParam String applyNo) {
        try {
            String message = applicationService.approveCheckout(applyNo);
            return Result.success(message);
        } catch (Exception e) {
            return Result.error("退房被拦截：" + e.getMessage());
        }
    }

    @PostMapping("/transfer")
    public Result<String> transfer(@RequestParam String applyNo) {
        try {
            String slip = applicationService.approveTransfer(applyNo);
            return Result.success("智能调房成功！", slip);
        } catch (Exception e) {
            return Result.error("调房被拦截：" + e.getMessage());
        }
    }

    @PostMapping("/submit")
    public Result<String> submitApplication(@RequestBody ApplicationSubmitDTO dto) {
        try {
            String message = applicationService.submitApplication(dto);
            return Result.success(message);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/cancel")
    public Result<String> cancel(@RequestParam String applyNo, @RequestParam String applicantName) {
        try {
            String message = applicationService.cancelApplication(applyNo, applicantName);
            return Result.success(message);
        } catch (Exception e) {
            return Result.error("撤回失败：" + e.getMessage());
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
