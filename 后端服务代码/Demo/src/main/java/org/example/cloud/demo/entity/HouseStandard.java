package org.example.cloud.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("house_standard") // 对应数据库的住房标准表
public class HouseStandard {

    @TableId(value = "standard_no", type = IdType.INPUT) // 之前规划的标准号主键
    private String standardNo;

    private BigDecimal area;
    private Integer minScore;
}