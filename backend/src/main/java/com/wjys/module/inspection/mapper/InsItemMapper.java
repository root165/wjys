package com.wjys.module.inspection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wjys.module.inspection.entity.InsItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InsItemMapper extends BaseMapper<InsItem> {
    @Select("select * from ins_item where point_id in (${pointIds}) and status = 1 order by id")
    List<InsItem> selectActiveByPointIds(String pointIds);
}
