package com.wjys.module.inspection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wjys.module.inspection.entity.InsTaskItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InsTaskItemMapper extends BaseMapper<InsTaskItem> {
    @Select("select * from ins_task_item where task_id = #{taskId} order by id") List<InsTaskItem> selectByTaskId(Long taskId);
}
