package org.example.cloud.demo.entity; // 注意包名统一用 demo

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("house")
public class House {
    @TableId(value = "house_no", type = IdType.INPUT)
    private String houseNo;

    private BigDecimal area;
    private Integer status;
    private BigDecimal rentPerSqm;
}