package org.example.cloud.demo.controller;

import org.example.cloud.demo.entity.HouseStandard;
import org.example.cloud.demo.service.HouseStandardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/standard") // 定义这一组接口的路由前缀
public class HouseStandardController {

    @Autowired
    private HouseStandardService houseStandardService;

    @GetMapping("/list")
    public List<HouseStandard> list() {
        return houseStandardService.getAllStandards();
    }
}