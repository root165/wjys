package com.wjys.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wjys.module.system.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {
    @Select("select distinct m.* from sys_menu m join sys_role_menu rm on rm.menu_id = m.id join sys_user_role ur on ur.role_id = rm.role_id where ur.user_id = #{userId} and m.status = 1 order by m.sort_no, m.id")
    List<SysMenu> selectByUserId(Long userId);
}
