package com.wjys.module.equipment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wjys.module.equipment.entity.EqParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EqParamMapper extends BaseMapper<EqParam> {
    @Select("select * from eq_param where equip_id = #{equipId} order by sort_no, id")
    List<EqParam> selectByEquipId(Long equipId);
}
