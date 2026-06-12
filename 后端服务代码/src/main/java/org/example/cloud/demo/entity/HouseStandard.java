package org.example.cloud.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("house_standard")
public class HouseStandard {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private BigDecimal area;
    private Integer minScore;
}
