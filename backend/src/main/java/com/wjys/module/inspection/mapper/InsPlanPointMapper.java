package com.wjys.module.inspection.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InsPlanPointMapper {
    @Delete("delete from ins_plan_point where plan_id = #{planId}") int deleteByPlanId(Long planId);
    @Insert("<script><foreach collection='pointIds' item='pointId' separator=';'>insert into ins_plan_point (plan_id, point_id) values (#{planId}, #{pointId})</foreach></script>") int insertLinks(@Param("planId") Long planId, @Param("pointIds") List<Long> pointIds);
    @Select("select point_id from ins_plan_point where plan_id = #{planId} order by point_id") List<Long> selectPointIds(Long planId);
}
