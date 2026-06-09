package org.example.cloud.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("resident")
public class Resident {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String ownerName;
    private String department;
    private String title;
    private Integer familySize;
    private Integer score;
    private String houseNo;
    private BigDecimal houseArea;
}
