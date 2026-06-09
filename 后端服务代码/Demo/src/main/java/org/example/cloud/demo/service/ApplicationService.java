package org.example.cloud.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.cloud.demo.dto.ApplicationSubmitDTO;
import org.example.cloud.demo.entity.*;
import org.example.cloud.demo.mapper.*;
import org.example.cloud.demo.util.DistributedLockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 申请单服务（分房/调房/退房业务逻辑）
 *
 * 纯 MyBatis-Plus 方式，所有查询使用 BaseMapper + QueryWrapper，无需 XML。
 */
@Slf4j
@Service
public class ApplicationService {

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private HouseMapper houseMapper;

    @Autowired
    private HouseStandardMapper houseStandardMapper;

    @Autowired
    private ResidentMapper residentMapper;

    @Autowired
    private RentBillMapper rentBillMapper;

    @Autowired
    private DistributedLockUtil distributedLockUtil;

    // ================== 基础查询（带 Redis 缓存） ==================

    @Cacheable(value = "applications", key = "'all'")
    public List<Application> getAllApplications() {
        log.info("从数据库查询所有申请单（缓存未命中）");
        return applicationMapper.selectList(
                new QueryWrapper<Application>().orderByDesc("create_time")
        );
    }

    @Cacheable(value = "applications", key = "'queue'")
    public List<Application> getAllocationQueue() {
        log.info("从数据库查询分房队列（缓存未命中）");
        QueryWrapper<Application> query = new QueryWrapper<>();
        query.eq("apply_type", 1)
                .eq("apply_status", 0)
                .orderByDesc("score")
                .orderByAsc("create_time");
        return applicationMapper.selectList(query);
    }

    // ================== 智能找房算法 ==================

    /**
     * 查找最匹配的空房
     */
    private House findBestMatchHouse(BigDecimal reqArea, Integer userScore) {
        QueryWrapper<House> houseQuery = new QueryWrapper<>();
        houseQuery.eq("status", 0)
                .ge("area", reqArea)
                .orderByDesc("area")
                .orderByAsc("rent_per_sqm")
                .orderByAsc("house_no")
                .last("FOR UPDATE");

        List<House> availableHouses = houseMapper.selectList(houseQuery);

        List<HouseStandard> standards = houseStandardMapper.selectList(null);
        Map<BigDecimal, Integer> standardMap = standards.stream()
                .collect(Collectors.toMap(HouseStandard::getArea, HouseStandard::getMinScore));

        for (House h : availableHouses) {
            Integer minScoreRequired = standardMap.get(h.getArea());
            if (minScoreRequired != null && userScore >= minScoreRequired) {
                return h;
            }
        }
        return null;
    }

    // ================== 公共分配方法 ==================

    @Transactional(rollbackFor = Exception.class)
    public AllocationResult allocateHouseToApplicant(Application application, House selectedHouse) {
        String houseNo = selectedHouse.getHouseNo();

        application.setApplyStatus(1);
        application.setAllocatedHouseNo(houseNo);
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

        RentBill bill = createRentBill(houseNo, application.getApplicantName(),
                selectedHouse.getArea(), selectedHouse.getRentPerSqm());
        rentBillMapper.insert(bill);

        return new AllocationResult(application, resident, bill, selectedHouse);
    }

    public static class AllocationResult {
        private Application application;
        private Resident resident;
        private RentBill bill;
        private House house;

        public AllocationResult(Application application, Resident resident, RentBill bill, House house) {
            this.application = application;
            this.resident = resident;
            this.bill = bill;
            this.house = house;
        }

        public String toSlip() {
            return String.format(
                    "【住房分配单】\n住户姓名：%s\n所属部门：%s\n分配房号：%s\n房屋面积：%.2f 平米\n首月账单：%s 月，应缴租金：%.2f 元",
                    application.getApplicantName(), application.getDepartment(),
                    house.getHouseNo(), house.getArea(),
                    bill.getBillMonth(), bill.getRentAmount()
            );
        }
    }

    // ================== 1. 分房审批 ==================

    @CacheEvict(value = {"applications", "houses", "residents", "bills"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public String approveHousing(String applyNo) {
        Application application = applicationMapper.selectById(applyNo);
        if (application == null || application.getApplyStatus() != 0 || application.getApplyType() != 1) {
            throw new RuntimeException("非法的排队分房申请单！");
        }

        List<Application> queue = getAllocationQueue();
        if (queue.isEmpty()) {
            throw new RuntimeException("分房队列为空！");
        }
        if (!queue.get(0).getApplyNo().equals(applyNo)) {
            throw new RuntimeException("分房队列有先后顺序，请先处理排在前面的申请（当前队首："
                    + queue.get(0).getApplicantName() + "）！");
        }

        House selectedHouse = findBestMatchHouse(application.getReqArea(), application.getScore());
        if (selectedHouse == null) {
            throw new RuntimeException("当前空房库中没有符合您面积和分数要求的好房源，请继续在队列中等待。");
        }

        AllocationResult result = allocateHouseToApplicant(application, selectedHouse);
        log.info("====== 分房成功：\n{} ======", result.toSlip());
        return result.toSlip();
    }

    // ================== 2. 退房审批 ==================

    @CacheEvict(value = {"applications", "houses", "residents", "bills"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public String approveCheckout(String applyNo) {
        Application application = applicationMapper.selectById(applyNo);
        if (application == null) throw new RuntimeException("退房申请单不存在！");
        if (application.getApplyStatus() != 0) throw new RuntimeException("该退房申请单已被处理，请勿重复操作！");
        if (application.getApplyType() != 3) throw new RuntimeException("非法操作：该单据类型不是【退房申请】，请查证！");

        String ownerName = application.getApplicantName();
        Resident resident = getResidentByName(ownerName);
        if (resident == null) throw new RuntimeException("校验失败：系统中找不到该住户的在住信息，无法退房！");
        if (!resident.getHouseNo().equals(application.getOldHouseNo()))
            throw new RuntimeException("信息不匹配：申请退房的房号与实际居住的房号不符！");
        if (!resident.getDepartment().equals(application.getDepartment()))
            throw new RuntimeException("信息不匹配：申请人所属部门与入住登记时的部门不符！");

        String houseNo = resident.getHouseNo();
        House house = houseMapper.selectById(houseNo);
        if (house != null) {
            if (house.getStatus() == 0)
                throw new RuntimeException("该房屋当前已处于【空闲】状态，无需重复办理退房！");
            house.setStatus(0);
            houseMapper.updateById(house);
        }

        deleteBillsByNameAndHouse(ownerName, houseNo);
        removeResident(ownerName, houseNo);

        application.setApplyStatus(1);
        application.setProcessTime(LocalDateTime.now());
        applicationMapper.updateById(application);

        String msg = String.format("退房审批成功！已为 [%s] 办理退房，房屋 [%s] 已回收为空闲状态，名下账单已清理。",
                ownerName, houseNo);
        log.info("====== {} ======", msg);
        return msg;
    }

    // ================== 3. 调房审批 ==================

    @CacheEvict(value = {"applications", "houses", "residents", "bills"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public String approveTransfer(String applyNo) {
        Application application = applicationMapper.selectById(applyNo);
        if (application == null || application.getApplyStatus() != 0 || application.getApplyType() != 2) {
            throw new RuntimeException("非法的排队调房申请单！");
        }

        String ownerName = application.getApplicantName();
        Resident resident = getResidentByName(ownerName);
        if (resident == null) {
            throw new RuntimeException("校验失败：系统中找不到该住户的在住信息！");
        }
        if (!resident.getHouseNo().equals(application.getOldHouseNo())) {
            throw new RuntimeException("校验失败：实际居住房号与调房申请不匹配！");
        }

        House oldHouse = houseMapper.selectById(application.getOldHouseNo());
        if (oldHouse != null && application.getOldHouseArea() != null) {
            if (oldHouse.getArea().compareTo(application.getOldHouseArea()) != 0) {
                throw new RuntimeException("原住房面积不匹配：数据库记录为 " + oldHouse.getArea()
                        + "㎡，申请填写为 " + application.getOldHouseArea() + "㎡！");
            }
        }

        House selectedHouse = findBestMatchHouse(application.getReqArea(), application.getScore());
        if (selectedHouse == null) {
            throw new RuntimeException("当前空房库中没有符合您面积和分数要求的好房源，请继续等待调房。");
        }

        String oldHouseNo = resident.getHouseNo();
        String newHouseNo = selectedHouse.getHouseNo();

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
        application.setAllocatedHouseNo(newHouseNo);
        application.setProcessTime(LocalDateTime.now());
        applicationMapper.updateById(application);

        deleteBillsByNameAndHouse(ownerName, oldHouseNo);

        RentBill newBill = createRentBill(newHouseNo, ownerName, selectedHouse.getArea(), selectedHouse.getRentPerSqm());
        rentBillMapper.insert(newBill);

        String transferSlip = String.format(
                "【调房分配单】\n住户姓名：%s\n房号变更：[%s] -> 置换为 -> [%s]\n新房首月账单：%.2f 元",
                ownerName, oldHouseNo, newHouseNo, newBill.getRentAmount());
        log.info("====== 调房业务成功：{} ======", transferSlip);
        return transferSlip;
    }

    // ================== 4. 计算住房分数 ==================

    public Integer calculateHousingScore(String title, Integer familySize) {
        int baseScore = 40;
        if (title != null) {
            if (title.contains("教授") || title.contains("处长")) baseScore = 100;
            else if (title.contains("科长") || title.contains("研究员")) baseScore = 80;
            else if (title.contains("讲师") || title.contains("工程师")) baseScore = 60;
            else if (title.contains("实验员") || title.contains("干事")) baseScore = 50;
        }
        return baseScore + (familySize != null ? familySize : 1) * 10;
    }

    // ================== 5. 申请提交 ==================

    @CacheEvict(value = {"applications"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public String submitApplication(ApplicationSubmitDTO dto) {
        if (dto.getApplicantName() == null || dto.getDepartment() == null || dto.getApplyType() == null) {
            throw new RuntimeException("提交失败：申请人姓名、部门或申请类型不能为空！");
        }
        if (dto.getApplyType() < 1 || dto.getApplyType() > 3) {
            throw new RuntimeException("提交失败：非法的申请类型！(1-分房, 2-调房, 3-退房)");
        }

        if (dto.getApplyType() == 1) {
            Resident existing = getResidentByName(dto.getApplicantName());
            if (existing != null) {
                throw new RuntimeException("提交失败：申请人 [" + dto.getApplicantName()
                        + "] 已居住在 [" + existing.getHouseNo() + "]，请使用调房或退房申请！");
            }
        }

        Application app = new Application();
        String applyNo = "APP-" + System.currentTimeMillis();
        app.setApplyNo(applyNo);
        app.setApplicantName(dto.getApplicantName());
        app.setDepartment(dto.getDepartment());
        app.setApplyType(dto.getApplyType());
        app.setCreateTime(LocalDateTime.now());
        app.setApplyStatus(0);

        if (dto.getApplyType() == 2 || dto.getApplyType() == 3) {
            if (dto.getOldHouseNo() == null || dto.getOldHouseNo().trim().isEmpty()) {
                throw new RuntimeException("提交失败：调房或退房申请，必须提供原住房号！");
            }
            QueryWrapper<Resident> resQuery = new QueryWrapper<>();
            resQuery.eq("owner_name", dto.getApplicantName()).eq("house_no", dto.getOldHouseNo());
            if (residentMapper.selectCount(resQuery) == 0) {
                throw new RuntimeException("非法操作：系统未查到您在 [" + dto.getOldHouseNo() + "] 的入住记录！");
            }
            app.setOldHouseNo(dto.getOldHouseNo());
            app.setOldHouseArea(dto.getOldHouseArea());
        }

        if (dto.getApplyType() == 1 || dto.getApplyType() == 2) {
            if (dto.getTitle() == null || dto.getFamilySize() == null || dto.getReqArea() == null) {
                throw new RuntimeException("提交失败：分房或调房业务必须填写职称、家庭人口和期望面积！");
            }
            if (dto.getFamilySize() <= 0 || dto.getReqArea().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("提交失败：家庭人口和期望面积必须大于 0！");
            }
            app.setTitle(dto.getTitle());
            app.setFamilySize(dto.getFamilySize());
            app.setReqArea(dto.getReqArea());

            int calculatedScore = calculateHousingScore(dto.getTitle(), dto.getFamilySize());
            app.setScore(calculatedScore);

            QueryWrapper<HouseStandard> standardQuery = new QueryWrapper<>();
            standardQuery.eq("area", dto.getReqArea());
            HouseStandard standard = houseStandardMapper.selectOne(standardQuery);
            if (standard == null) {
                throw new RuntimeException("系统异常：不存在面积为 " + dto.getReqArea() + " 的住房标准，请联系管理员！");
            }
            if (calculatedScore < standard.getMinScore()) {
                throw new RuntimeException("条件不符：您的得分为 " + calculatedScore
                        + " 分，未达到该面积要求的最低阈值(" + standard.getMinScore() + "分)。系统已拒绝该申请！");
            }
        }

        applicationMapper.insert(app);
        return "申请提交成功！排队单号：" + applyNo
                + (app.getScore() != null ? "，系统评分为：" + app.getScore() : "");
    }

    // ================== 6. 月底自动批量分房 ==================

    @Scheduled(cron = "0 0 23 28-31 * ?")
    public void scheduledMonthlyAllocation() {
        log.info("========== 触发【月底自动分房活动】定时任务 ==========");
        LocalDate today = LocalDate.now();
        LocalDate lastDay = today.withDayOfMonth(today.lengthOfMonth());
        if (!today.equals(lastDay)) {
            log.info("今日 ({}) 不是本月最后一天 ({}), 跳过分房活动", today, lastDay);
            return;
        }
        distributedLockUtil.executeWithLock("lock:monthlyAllocation", 5, 120, () -> {
            String result = allocatePendingApplications();
            log.info("月底分房活动结果：\n{}", result);
            return result;
        });
    }

    @Scheduled(cron = "0 0 8 1 * ?")
    public void scheduledMonthlyBillGeneration() {
        log.info("========== 触发【月度账单生成】定时任务 ==========");
        String lastMonth = LocalDate.now().minusMonths(1).toString().substring(0, 7);
        try {
            int generated = generateMonthlyBills(lastMonth);
            log.info("已为 {} 月生成 {} 份房租账单", lastMonth, generated);
        } catch (Exception e) {
            log.error("月度账单生成失败：{}", e.getMessage());
        }
    }

    @CacheEvict(value = {"applications", "houses", "residents", "bills"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public String allocatePendingApplications() {
        List<Application> queue = getAllocationQueue();
        if (queue.isEmpty()) return "当前分房队列为空，无需进行分配。";

        int successCount = 0;
        StringBuilder reportLog = new StringBuilder("【月底自动分房活动报告】\n");
        StringBuilder allSlips = new StringBuilder("\n========== 住户分配单汇总 ==========\n");

        for (Application app : queue) {
            try {
                House selectedHouse = findBestMatchHouse(app.getReqArea(), app.getScore());
                if (selectedHouse != null) {
                    AllocationResult result = allocateHouseToApplicant(app, selectedHouse);
                    successCount++;
                    reportLog.append(String.format(" - 分配成功：入选住户 [%s], 分配房号 [%s]\n",
                            app.getApplicantName(), selectedHouse.getHouseNo()));
                    allSlips.append("\n").append(result.toSlip()).append("\n");
                    allSlips.append("----------------------------------------");
                }
            } catch (Exception e) {
                log.warn("分配失败：申请单 [{}], 原因: {}", app.getApplyNo(), e.getMessage());
                reportLog.append(String.format(" - 分配失败：申请单 [%s], 原因: %s\n", app.getApplyNo(), e.getMessage()));
            }
        }

        reportLog.append("\n💡 总结：本次活动共扫描排队者 ").append(queue.size())
                .append(" 人，成功匹配并分配房源 ").append(successCount).append(" 套。");
        reportLog.append(allSlips);
        log.info(reportLog.toString());
        return reportLog.toString();
    }

    // ================== 7. 生成月度账单 ==================

    @CacheEvict(value = {"bills"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public int generateMonthlyBills(String month) {
        int count = 0;
        QueryWrapper<House> houseQuery = new QueryWrapper<>();
        houseQuery.eq("status", 1);
        List<House> occupiedHouses = houseMapper.selectList(houseQuery);

        for (House house : occupiedHouses) {
            QueryWrapper<Resident> residentQuery = new QueryWrapper<>();
            residentQuery.eq("house_no", house.getHouseNo());
            Resident resident = residentMapper.selectOne(residentQuery);

            if (resident != null) {
                QueryWrapper<RentBill> billQuery = new QueryWrapper<>();
                billQuery.eq("house_no", house.getHouseNo()).eq("bill_month", month);
                if (rentBillMapper.selectCount(billQuery) == 0) {
                    BigDecimal rentAmount = house.getArea().multiply(house.getRentPerSqm());
                    RentBill bill = new RentBill();
                    bill.setHouseNo(house.getHouseNo());
                    bill.setOwnerName(resident.getOwnerName());
                    bill.setBillMonth(month);
                    bill.setRentAmount(rentAmount);
                    bill.setCreateTime(LocalDate.now());
                    rentBillMapper.insert(bill);
                    count++;
                }
            }
        }
        return count;
    }

    // ================== 7.5 撤回申请 ==================

    @CacheEvict(value = {"applications"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public String cancelApplication(String applyNo, String applicantName) {
        Application application = applicationMapper.selectById(applyNo);
        if (application == null) {
            throw new RuntimeException("申请单不存在！");
        }
        if (application.getApplyStatus() != 0) {
            throw new RuntimeException("该申请已被处理，无法撤回！");
        }
        if (!application.getApplicantName().equals(applicantName)) {
            throw new RuntimeException("您只能撤回自己的申请！");
        }

        application.setApplyStatus(2); // 已驳回（视为撤回）
        application.setRejectReason("申请人自行撤回");
        application.setProcessTime(LocalDateTime.now());
        applicationMapper.updateById(application);

        String typeName = application.getApplyType() == 1 ? "分房" : application.getApplyType() == 2 ? "调房" : "退房";
        log.info("====== 申请撤回成功：[{}] 撤回了 [{}] 申请（编号：{}）======",
                applicantName, typeName, applyNo);
        return "申请撤回成功！编号：" + applyNo + "（" + typeName + "申请）";
    }

    // ================== 8. 生成统计报表 ==================

    public Map<String, Object> generateStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();

        long totalHouses = houseMapper.selectCount(null);
        long emptyHouses = houseMapper.selectCount(new QueryWrapper<House>().eq("status", 0));
        long occupiedHouses = totalHouses - emptyHouses;
        stats.put("totalHouses", totalHouses);
        stats.put("emptyHouses", emptyHouses);
        stats.put("occupiedHouses", occupiedHouses);
        stats.put("occupancyRate", String.format("%.2f%%",
                totalHouses > 0 ? (double) occupiedHouses / totalHouses * 100 : 0));

        QueryWrapper<Resident> deptQuery = new QueryWrapper<>();
        deptQuery.select("department", "count(*) as count").groupBy("department");
        stats.put("departmentStats", residentMapper.selectMaps(deptQuery));

        QueryWrapper<Resident> titleQuery = new QueryWrapper<>();
        titleQuery.select("title", "count(*) as count").groupBy("title");
        stats.put("titleStats", residentMapper.selectMaps(titleQuery));

        List<Resident> allResidents = residentMapper.selectList(null);
        long smallHouse = allResidents.stream().filter(r -> r.getHouseArea().compareTo(new BigDecimal("60")) <= 0).count();
        long mediumHouse = allResidents.stream().filter(r ->
                r.getHouseArea().compareTo(new BigDecimal("60")) > 0
                        && r.getHouseArea().compareTo(new BigDecimal("100")) <= 0).count();
        long largeHouse = allResidents.stream().filter(r -> r.getHouseArea().compareTo(new BigDecimal("100")) > 0).count();
        stats.put("smallHouseCount", smallHouse);
        stats.put("mediumHouseCount", mediumHouse);
        stats.put("largeHouseCount", largeHouse);

        List<House> emptyHouseList = houseMapper.selectList(
                new QueryWrapper<House>().eq("status", 0).orderByAsc("house_no"));
        stats.put("emptyHouseList", emptyHouseList);

        long pendingApps = applicationMapper.selectCount(new QueryWrapper<Application>().eq("apply_status", 0));
        long approvedApps = applicationMapper.selectCount(new QueryWrapper<Application>().eq("apply_status", 1));
        long rejectedApps = applicationMapper.selectCount(new QueryWrapper<Application>().eq("apply_status", 2));
        stats.put("pendingApplications", pendingApps);
        stats.put("approvedApplications", approvedApps);
        stats.put("rejectedApplications", rejectedApps);

        return stats;
    }

    // ================== 私有工具方法 ==================

    private RentBill createRentBill(String houseNo, String ownerName, BigDecimal area, BigDecimal rentPerSqm) {
        RentBill bill = new RentBill();
        bill.setHouseNo(houseNo);
        bill.setOwnerName(ownerName);
        String currentMonth = java.time.YearMonth.now().toString();
        bill.setBillMonth(currentMonth);
        bill.setRentAmount(area.multiply(rentPerSqm));
        bill.setCreateTime(LocalDate.now());
        return bill;
    }

    private Resident getResidentByName(String ownerName) {
        QueryWrapper<Resident> query = new QueryWrapper<>();
        query.eq("owner_name", ownerName);
        return residentMapper.selectOne(query);
    }

    private void removeResident(String ownerName, String houseNo) {
        QueryWrapper<Resident> query = new QueryWrapper<>();
        query.eq("owner_name", ownerName).eq("house_no", houseNo);
        residentMapper.delete(query);
    }

    private void deleteBillsByNameAndHouse(String ownerName, String houseNo) {
        QueryWrapper<RentBill> query = new QueryWrapper<>();
        query.eq("owner_name", ownerName).eq("house_no", houseNo);
        rentBillMapper.delete(query);
    }
}
