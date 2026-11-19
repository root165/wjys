package com.wjys.module.inspection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wjys.module.inspection.entity.InsTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InsTaskMapper extends BaseMapper<InsTask> {
    @Select("select * from ins_task where submit_no = #{submitNo} limit 1") InsTask selectBySubmitNo(String submitNo);
    @Select("select * from ins_task where plan_id = #{planId} and plan_time = #{planTime} limit 1") InsTask selectByPlanTime(Long planId, java.time.LocalDate planTime);

    @Select("select count(*) from ins_task where assignee_id = #{userId} and plan_time = #{planTime} and status <> 4")
    int countActiveTasks(Long userId, java.time.LocalDate planTime);
}
