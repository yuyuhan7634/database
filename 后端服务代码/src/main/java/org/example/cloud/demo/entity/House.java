package org.example.cloud.demo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
@Data
@TableName("house")
public class House {
    @TableId
    private String houseNo;
    private BigDecimal area;
    private Integer status;
    private BigDecimal rentPerSqm;
}