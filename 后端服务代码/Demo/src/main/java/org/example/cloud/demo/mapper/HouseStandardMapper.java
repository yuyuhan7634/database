package org.example.cloud.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.cloud.demo.entity.HouseStandard;

@Mapper
public interface HouseStandardMapper extends BaseMapper<HouseStandard> {
    // 继承 BaseMapper，直接拥有全部基本增删改查方法
}