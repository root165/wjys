package com.wjys.module.equipment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wjys.module.equipment.entity.EqStatusLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EqStatusLogMapper extends BaseMapper<EqStatusLog> {
    @Select("select * from eq_status_log where equip_id = #{equipId} order by id desc")
    List<EqStatusLog> selectByEquipId(Long equipId);
}
