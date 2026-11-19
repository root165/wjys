package com.wjys.module.equipment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wjys.module.equipment.entity.EqCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EqCategoryMapper extends BaseMapper<EqCategory> {
    @Select("select count(*) from eq_category where parent_id = #{categoryId}")
    long countChildren(Long categoryId);

    @Select("select count(*) from eq_equipment where category_id = #{categoryId} and deleted = 0")
    long countActiveEquipments(Long categoryId);
}
