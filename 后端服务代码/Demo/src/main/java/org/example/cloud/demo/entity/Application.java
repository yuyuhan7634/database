package org.example.cloud.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("application")
public class Application {
    @TableId(value = "apply_no", type = IdType.INPUT) // 申请单号为主键
    private String applyNo;
    private String applicantName;
    private String department;
    private Integer applyType;   // 1-分房, 2-调房, 3-退房
    private Integer applyStatus; // 0-排队中, 1-已批准, 2-已驳回
    private BigDecimal reqArea;
    private String oldHouseNo;
    private LocalDate createTime;
}