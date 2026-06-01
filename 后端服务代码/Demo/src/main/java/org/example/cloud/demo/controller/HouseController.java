package org.example.cloud.demo.controller;
import org.example.cloud.demo.entity.House;
import org.example.cloud.demo.service.HouseService; // 导入新的路径
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
@RestController
@RequestMapping("/house")
public class HouseController {
    @Autowired
    private HouseService houseService;
    @GetMapping("/list")
    public List<House> list() {
        return houseService.getAllHouses();
    }
}