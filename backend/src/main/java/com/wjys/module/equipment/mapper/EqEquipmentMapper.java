package com.wjys.module.equipment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wjys.module.equipment.entity.EqEquipment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EqEquipmentMapper extends BaseMapper<EqEquipment> {
    @Select("select count(1) from ins_task_item i join ins_task t on t.id = i.task_id where i.equip_id = #{equipId} and t.status in (0, 1, 3)")
    long countActiveInspectionTasks(Long equipId);

    @Select("select count(1) from wo_repair r left join wo_order o on o.repair_id = r.id where r.equip_id = #{equipId} and (r.status in (0, 1) or o.status in (0, 1, 2, 4))")
    long countActiveRepairOrders(Long equipId);
}
