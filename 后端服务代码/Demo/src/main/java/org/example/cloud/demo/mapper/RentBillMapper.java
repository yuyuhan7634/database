package org.example.cloud.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.cloud.demo.entity.RentBill;

@Mapper
public interface RentBillMapper extends BaseMapper<RentBill> {
}