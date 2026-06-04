package org.example.cloud.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.cloud.demo.entity.Resident;

@Mapper
public interface ResidentMapper extends BaseMapper<Resident> {
}