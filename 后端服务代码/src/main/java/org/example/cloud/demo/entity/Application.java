package org.example.cloud.demo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("application")
public class Application {

    @TableId
    private String applyNo;
    private String applicantName;
    private String department;
    private Integer applyType;
    private Integer applyStatus;
    private BigDecimal reqArea;
    private String oldHouseNo;
    private String allocatedHouseNo;
    private LocalDateTime createTime;
    private String title;
    private Integer familySize;
    private Integer score;
    private BigDecimal oldHouseArea;
    private String rejectReason;
    private LocalDateTime processTime;
}
