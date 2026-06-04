package org.example.cloud.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("rent_bill")
public class RentBill {
    @TableId(value = "house_no", type = IdType.INPUT) // 联合主键之一，这里作为标识
    private String houseNo;
    private String ownerName;
    private String billMonth;
    private BigDecimal rentAmount;
    private LocalDate createTime;
}