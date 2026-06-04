package org.example.cloud.demo.service;

// 🌟 1. 统一且干净的 Import 区域
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.cloud.demo.dto.ApplicationSubmitDTO;
import org.example.cloud.demo.entity.Application;
import org.example.cloud.demo.entity.House;
import org.example.cloud.demo.entity.HouseStandard;
import org.example.cloud.demo.entity.RentBill;
import org.example.cloud.demo.entity.Resident;
import org.example.cloud.demo.mapper.ApplicationMapper;
import org.example.cloud.demo.mapper.HouseMapper;
import org.example.cloud.demo.mapper.HouseStandardMapper;
import org.example.cloud.demo.mapper.RentBillMapper;
import org.example.cloud.demo.mapper.ResidentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApplicationService {

    @Autowired
    private ApplicationMapper applicationMapper;
    @Autowired
    private HouseStandardMapper houseStandardMapper;
    @Autowired
    private HouseMapper houseMapper;
    @Autowired
    private ResidentMapper residentMapper;
    @Autowired
    private RentBillMapper rentBillMapper;

    public List<Application> getAllApplications() {
        return applicationMapper.selectList(null);
    }
    public List<Application> getAllocationQueue() {
        QueryWrapper<Application> query = new QueryWrapper<>();
        query.eq("apply_type", 1)
                .eq("apply_status", 0)
                .orderByDesc("score")
                .orderByAsc("create_time");
        return applicationMapper.selectList(query);
    }
    // ================== 1. 分房审批==================
    @Transactional(rollbackFor = Exception.class)
    public String approveHousing(String applyNo) {
        Application application = applicationMapper.selectById(applyNo);
        if (application == null || application.getApplyStatus() != 0 || application.getApplyType() != 1) {
            throw new RuntimeException("非法的排队分房申请单！");
        }
        QueryWrapper<House> houseQuery = new QueryWrapper<>();
        houseQuery.eq("status", 0)
                .ge("area", application.getReqArea())
                .orderByDesc("area")
                .orderByAsc("rent_per_sqm")
                .orderByAsc("house_no");
        List<House> availableHouses = houseMapper.selectList(houseQuery);
        House selectedHouse = null;
        for (House h : availableHouses) {
            QueryWrapper<HouseStandard> stdQuery = new QueryWrapper<>();
            stdQuery.eq("area", h.getArea());
            HouseStandard std = houseStandardMapper.selectOne(stdQuery);
            if (std != null && application.getScore() >= std.getMinScore()) {
                selectedHouse = h;
                break;
            }
        }
        if (selectedHouse == null) {
            throw new RuntimeException("当前空房库中没有符合您面积和分数要求的好房源，请继续在队列中等待。");
        }
        String houseNo = selectedHouse.getHouseNo();
        application.setApplyStatus(1);
        application.setProcessTime(LocalDateTime.now());
        applicationMapper.updateById(application);
        selectedHouse.setStatus(1);
        houseMapper.updateById(selectedHouse);
        Resident resident = new Resident();
        resident.setOwnerName(application.getApplicantName());
        resident.setDepartment(application.getDepartment());
        resident.setHouseNo(houseNo);
        resident.setHouseArea(selectedHouse.getArea());
        resident.setTitle(application.getTitle());
        resident.setFamilySize(application.getFamilySize());
        resident.setScore(application.getScore());
        residentMapper.insert(resident);
        RentBill bill = new RentBill();
        bill.setHouseNo(houseNo);
        bill.setOwnerName(application.getApplicantName());
        String currentMonth = java.time.YearMonth.now().toString();
        bill.setBillMonth(currentMonth);
        java.math.BigDecimal rentAmount = selectedHouse.getArea().multiply(selectedHouse.getRentPerSqm());
        bill.setRentAmount(rentAmount);
        bill.setCreateTime(LocalDate.from(LocalDateTime.now()));
        rentBillMapper.insert(bill);
        String allocationSlip = String.format(
                "【住房分配单】\n住户姓名：%s\n所属部门：%s\n分配房号：%s\n房屋面积：%.2f 平米\n首月账单：%s 月，应缴租金：%.2f 元",
                application.getApplicantName(), application.getDepartment(), houseNo,
                selectedHouse.getArea(), currentMonth, rentAmount
        );
        System.out.println("====== " + allocationSlip + " ======");

        return allocationSlip;
    }
    // ================== 2. 退房审批 (全面加固合法性校验) ==================
    @Transactional(rollbackFor = Exception.class)
    public boolean approveCheckout(String applyNo) {
        // 1. 查出申请单
        Application application = applicationMapper.selectById(applyNo);
        if (application == null || application.getApplyStatus() != 0) {
            throw new RuntimeException("退房申请单不存在或已被处理！");
        }
        // 申请单上的原房号必须和实际居住的房号绝对一致
        if (!resident.getHouseNo().equals(application.getOldHouseNo())) {
            throw new RuntimeException("信息不匹配：申请退房的房号 [" + application.getOldHouseNo() + "] 与该住户实际居住的房号 [" + resident.getHouseNo() + "] 不符！");
        }
        // 申请单上的部门必须和住户登记的部门一致
        if (!resident.getDepartment().equals(application.getDepartment())) {
            throw new RuntimeException("信息不匹配：申请人所属部门与入住登记时的部门不符！");
        }
        String houseNo = resident.getHouseNo();
        House house = houseMapper.selectById(houseNo);
        if (house != null) {
            //如果房屋本身已经空闲，立刻拦截，防止重复回收
            if (house.getStatus() == 0) {
                throw new RuntimeException("该房屋当前已处于【空闲】状态，无需重复办理退房！");
            }
            house.setStatus(0);
            houseMapper.updateById(house);
        }
        QueryWrapper<RentBill> billQuery = new QueryWrapper<>();
        billQuery.eq("owner_name", ownerName);
        rentBillMapper.delete(billQuery);
        residentMapper.deleteById(ownerName);
        application.setApplyStatus(1);
        application.setProcessTime(LocalDateTime.now());
        applicationMapper.updateById(application);
        System.out.println("====== 已为 [" + ownerName + "] 办理退房，名下账单已清理，房产 [" + houseNo + "] 已安全回收 ======");
        return true;
    }
    // ================== 3. 调房审批 (全自动智能找房与置换算法) ==================
    @Transactional(rollbackFor = Exception.class)
    public String approveTransfer(String applyNo) {
        Application application = applicationMapper.selectById(applyNo);
        if (application == null || application.getApplyStatus() != 0 || application.getApplyType() != 2) {
            throw new RuntimeException("非法的排队调房申请单！");
        }
        String ownerName = application.getApplicantName();
        Resident resident = residentMapper.selectById(ownerName);
        if (resident == null) {
            throw new RuntimeException("校验失败：找不到该住户的在住信息，无法调房！");
        }
        if (!resident.getHouseNo().equals(application.getOldHouseNo())) {
            throw new RuntimeException("信息不匹配：申请调房的原房号 [" + application.getOldHouseNo() + "] 与实际居住房号 [" + resident.getHouseNo() + "] 不符！");
        }
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<House> houseQuery = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        houseQuery.eq("status", 0)
                .ge("area", application.getReqArea())
                .orderByDesc("area")
                .orderByAsc("rent_per_sqm")
                .orderByAsc("house_no");
        List<House> availableHouses = houseMapper.selectList(houseQuery);
        House selectedHouse = null;
        for (House h : availableHouses) {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<org.example.cloud.demo.entity.HouseStandard> stdQuery = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            stdQuery.eq("area", h.getArea());
            org.example.cloud.demo.entity.HouseStandard std = houseStandardMapper.selectOne(stdQuery);
            if (std != null && application.getScore() >= std.getMinScore()) {
                selectedHouse = h;
                break;
            }
        }
        if (selectedHouse == null) {
            throw new RuntimeException("当前空房库中没有符合您面积和分数要求的好房源，请继续等待调房。");
        }
        String oldHouseNo = resident.getHouseNo();
        String newHouseNo = selectedHouse.getHouseNo();
        House oldHouse = houseMapper.selectById(oldHouseNo);
        if (oldHouse != null) {
            oldHouse.setStatus(0);
            houseMapper.updateById(oldHouse);
        }
        selectedHouse.setStatus(1);
        houseMapper.updateById(selectedHouse);
        resident.setHouseNo(newHouseNo);
        resident.setHouseArea(selectedHouse.getArea());
        resident.setTitle(application.getTitle());
        resident.setFamilySize(application.getFamilySize());
        resident.setScore(application.getScore());
        residentMapper.updateById(resident);
        application.setApplyStatus(1);
        application.setProcessTime(java.time.LocalDateTime.now());
        applicationMapper.updateById(application);
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RentBill> billQuery = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        billQuery.eq("owner_name", ownerName);
        rentBillMapper.delete(billQuery);
        RentBill newBill = new RentBill();
        newBill.setHouseNo(newHouseNo);
        newBill.setOwnerName(ownerName);
        String currentMonth = java.time.YearMonth.now().toString();
        newBill.setBillMonth(currentMonth);
        java.math.BigDecimal rentAmount = selectedHouse.getArea().multiply(selectedHouse.getRentPerSqm());
        newBill.setRentAmount(rentAmount);
        newBill.setCreateTime(java.time.LocalDateTime.now());
        rentBillMapper.insert(newBill);
        String transferSlip = String.format(
                "【调房分配单】\n住户姓名：%s\n房号变更：[%s] -> 置换为 -> [%s]\n新房面积：%.2f 平米\n新房首月账单：%.2f 元",
                ownerName, oldHouseNo, newHouseNo, selectedHouse.getArea(), rentAmount
        );
        System.out.println("====== 调房业务成功：" + ownerName + " 顺利完成置换 ======");
        return transferSlip;
    }
    // ================== 4. 后端自动算分引擎 ==================
    public Integer calculateHousingScore(String title, Integer familySize) {
        int baseScore = 40;
        if (title != null) {
            if (title.contains("教授") || title.contains("处长")) {
                baseScore = 100;
            } else if (title.contains("科长") || title.contains("研究员")) {
                baseScore = 80;
            } else if (title.contains("讲师") || title.contains("工程师")) {
                baseScore = 60;
            } else if (title.contains("实验员") || title.contains("干事")) {
                baseScore = 50;
            }
        }
        int familyScore = (familySize != null ? familySize : 1) * 10;
        return baseScore + familyScore;
    }

    @Transactional(rollbackFor = Exception.class)
    public String submitApplication(ApplicationSubmitDTO dto) {
        Application app = new Application();
        String applyNo = "APP-" + System.currentTimeMillis();
        app.setApplyNo(applyNo);
        app.setApplicantName(dto.getApplicantName());
        app.setDepartment(dto.getDepartment());
        app.setApplyType(dto.getApplyType());
        app.setCreateTime(LocalDateTime.now());

        if (dto.getApplyType() == 1 || dto.getApplyType() == 2) {
            app.setTitle(dto.getTitle());
            app.setFamilySize(dto.getFamilySize());
            app.setReqArea(dto.getReqArea());
            app.setOldHouseNo(dto.getOldHouseNo());
            app.setOldHouseArea(dto.getOldHouseArea());

            int calculatedScore = calculateHousingScore(dto.getTitle(), dto.getFamilySize());
            app.setScore(calculatedScore);

            QueryWrapper<HouseStandard> standardQuery = new QueryWrapper<>();
            standardQuery.eq("area", dto.getReqArea());
            HouseStandard standard = houseStandardMapper.selectOne(standardQuery);

            if (standard == null) {
                throw new RuntimeException("系统不存在面积为 " + dto.getReqArea() + " 的住房标准，请查证！");
            }
            if (calculatedScore < standard.getMinScore()) {
                app.setApplyStatus(2);
                app.setRejectReason("不合法申请：您的得分为 " + calculatedScore + " 分，未达到 " + dto.getReqArea() + " 平米房屋的最低阈值(" + standard.getMinScore() + "分)");
                applicationMapper.insert(app);
                throw new RuntimeException(app.getRejectReason());
            } else {
                app.setApplyStatus(0);
            }
        } else if (dto.getApplyType() == 3) {
            app.setOldHouseNo(dto.getOldHouseNo());
            app.setApplyStatus(0);

            QueryWrapper<Resident> resQuery = new QueryWrapper<>();
            resQuery.eq("owner_name", dto.getApplicantName()).eq("house_no", dto.getOldHouseNo());
            if (residentMapper.selectCount(resQuery) == 0) {
                throw new RuntimeException("非法操作：系统未查到您在 [" + dto.getOldHouseNo() + "] 的入住登记记录，无法退房！");
            }
        }
        applicationMapper.insert(app);
        return "申请提交成功！您的排队单号为：" + applyNo + (app.getScore() != null ? "，系统评分为：" + app.getScore() : "");
    }

    // ================== 5. 月底自动批量分房算法==================
    @Transactional(rollbackFor = Exception.class)
    public String allocatePendingApplications() {
        List<Application> queue = getAllocationQueue();
        if (queue.isEmpty()) {
            return "当前分房队列为空，无需进行分配。";
        }
        int successCount = 0;
        StringBuilder log = new StringBuilder("【月底自动分房活动报告】\n");
        for (Application app : queue) {
            QueryWrapper<House> houseQuery = new QueryWrapper<>();
            houseQuery.eq("status", 0)
                    .ge("area", app.getReqArea())
                    .orderByDesc("area")
                    .orderByAsc("rent_per_sqm")
                    .orderByAsc("house_no");
            List<House> availableHouses = houseMapper.selectList(houseQuery);
            House selectedHouse = null;
            for (House h : availableHouses) {
                QueryWrapper<HouseStandard> stdQuery = new QueryWrapper<>();
                stdQuery.eq("area", h.getArea());
                HouseStandard std = houseStandardMapper.selectOne(stdQuery);
                if (std != null && app.getScore() >= std.getMinScore()) {
                    selectedHouse = h;
                    break;
                }
            }
            if (selectedHouse != null) {
                app.setApplyStatus(1);
                app.setProcessTime(LocalDateTime.now());
                applicationMapper.updateById(app);
                selectedHouse.setStatus(1);
                houseMapper.updateById(selectedHouse);
                Resident resident = new Resident();
                resident.setOwnerName(app.getApplicantName());
                resident.setDepartment(app.getDepartment());
                resident.setHouseNo(selectedHouse.getHouseNo());
                resident.setHouseArea(selectedHouse.getArea());
                resident.setTitle(app.getTitle());
                resident.setFamilySize(app.getFamilySize());
                resident.setScore(app.getScore());
                residentMapper.insert(resident);
                RentBill bill = new RentBill();
                bill.setHouseNo(selectedHouse.getHouseNo());
                bill.setOwnerName(app.getApplicantName());
                String currentMonth = java.time.YearMonth.now().toString();
                bill.setBillMonth(currentMonth);
                java.math.BigDecimal rentAmount = selectedHouse.getArea().multiply(selectedHouse.getRentPerSqm());
                bill.setRentAmount(rentAmount);
                bill.setCreateTime(LocalDate.from(LocalDateTime.now()));
                rentBillMapper.insert(bill);
                successCount++;
                log.append("--------------------------\n");
                log.append(String.format("【住房分配单】生成完毕！\n入选住户：%s (积分:%d)\n分配房号：%s\n首月租金：%.2f 元\n",
                        app.getApplicantName(), app.getScore(), selectedHouse.getHouseNo(), rentAmount));
            }
        }
        log.append("--------------------------\n");
        log.append("💡 总结：本次活动共扫描排队者 ").append(queue.size()).append(" 人，成功匹配并分配房源 ").append(successCount).append(" 套。");
        System.out.println(log.toString());
        return log.toString();
    }
}