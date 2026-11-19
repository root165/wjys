package com.wjys.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wjys.module.system.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {
    @Select("select count(*) from sys_dept where parent_id = #{deptId}")
    long countChildren(Long deptId);

    @Select("select count(*) from sys_user where dept_id = #{deptId}")
    long countUsers(Long deptId);
}
