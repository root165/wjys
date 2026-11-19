package com.wjys.module.dashboard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wjys.module.dashboard.entity.StatEquipDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface StatEquipDailyMapper extends BaseMapper<StatEquipDaily> {
    @Select("select * from stat_equip_daily where stat_date between #{startDate} and #{endDate} order by stat_date, equip_id")
    List<StatEquipDaily> selectRange(LocalDate startDate, LocalDate endDate);
}
