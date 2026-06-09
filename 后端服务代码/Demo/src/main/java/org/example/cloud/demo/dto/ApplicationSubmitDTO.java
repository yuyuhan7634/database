package org.example.cloud.demo.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ApplicationSubmitDTO {
    private String applicantName; // 申请人姓名
    private String department;    // 部门
    private Integer applyType;    // 1-分房申请, 2-调房申请, 3-退房申请

    // 分房/调房所需字段
    private String title;         // 职称
    private Integer familySize;   // 家庭人口
    private BigDecimal reqArea;   // 要求的住房面积

    // 调房/退房所需字段
    private String oldHouseNo;    // 原房号
    private BigDecimal oldHouseArea; // 原住房面积
}