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
        String slip = applicationService.approveHousing(applyNo);
        return Result.success("智能分房成功！", slip);
    }

    @PostMapping("/checkout")
    public Result<String> checkout(@RequestParam String applyNo) {
        String message = applicationService.approveCheckout(applyNo);
        return Result.success(message);
    }

    @PostMapping("/transfer")
    public Result<String> transfer(@RequestParam String applyNo) {
        String slip = applicationService.approveTransfer(applyNo);
        return Result.success("智能调房成功！", slip);
    }

    @PostMapping("/submit")
    public Result<String> submitApplication(@RequestBody ApplicationSubmitDTO dto) {
        String message = applicationService.submitApplication(dto);
        return Result.success(message);
    }

    @PostMapping("/reject")
    public Result<String> reject(@RequestParam String applyNo, @RequestParam String reason) {
        String message = applicationService.rejectApplication(applyNo, reason);
        return Result.success(message);
    }

    @PostMapping("/cancel")
    public Result<String> cancel(@RequestParam String applyNo, @RequestParam String applicantName) {
        String message = applicationService.cancelApplication(applyNo, applicantName);
        return Result.success(message);
    }

    @PostMapping("/allocate/monthly")
    public Result<String> manualAllocate() {
        String resultMsg = applicationService.allocatePendingApplications();
        return Result.success("批量分房执行完毕！", resultMsg);
    }
}
