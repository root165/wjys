package com.wjys.module.dashboard.mapper;

import com.wjys.module.dashboard.mapper.row.DailyFactRow;
import com.wjys.module.dashboard.mapper.row.EquipmentFaultRow;
import com.wjys.module.dashboard.mapper.row.FaultCountRow;
import com.wjys.module.dashboard.mapper.row.InspectionMetricRow;
import com.wjys.module.dashboard.mapper.row.StatusCountRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DashboardQueryMapper {
    @Select("""
            <script>
            select e.id equip_id,
              coalesce((select count(distinct t.id) from ins_task t join ins_task_item ti on ti.task_id = t.id where t.plan_time = #{statDate} and ti.equip_id = e.id), 0) task_total,
              coalesce((select count(distinct t.id) from ins_task t join ins_task_item ti on ti.task_id = t.id where t.plan_time = #{statDate} and t.status = 2 and ti.equip_id = e.id), 0) task_finish,
              coalesce((select count(*) from ins_task_item ti join ins_task t on t.id = ti.task_id where t.plan_time = #{statDate} and ti.equip_id = e.id and ti.check_result = 1), 0) abnormal_count,
              coalesce((select count(*) from ins_task_item ti join ins_task t on t.id = ti.task_id where t.plan_time = #{statDate} and ti.equip_id = e.id and ti.check_result is not null), 0) checked_items,
              coalesce((select count(*) from wo_repair r where r.created_time &gt;= #{startTime} and r.created_time &lt; #{endTime} and r.equip_id = e.id and r.status &lt;&gt; 2), 0) fault_count,
              coalesce((select sum(o.downtime_minutes) from wo_order o join wo_repair r on r.id = o.repair_id where o.status = 3 and o.finished_time &gt;= #{startTime} and o.finished_time &lt; #{endTime} and r.equip_id = e.id), 0) downtime_minutes
            from eq_equipment e
            where e.deleted = 0
            <if test="categoryId != null">and e.category_id = #{categoryId}</if>
            order by e.id
            </script>
            """)
    List<DailyFactRow> selectDailyFacts(@Param("statDate") LocalDate statDate,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime,
                                        @Param("categoryId") Long categoryId);

    @Select("<script>select status, count(*) total from eq_equipment where deleted = 0 <if test='categoryId != null'>and category_id = #{categoryId}</if> group by status</script>")
    List<StatusCountRow> selectStatusCounts(@Param("categoryId") Long categoryId);

    @Select("<script>select count(*) from wo_repair r join eq_equipment e on e.id = r.equip_id where r.status in (0,1) and e.deleted = 0 <if test='categoryId != null'>and e.category_id = #{categoryId}</if></script>")
    long countUnhandledRepair(@Param("categoryId") Long categoryId);

    @Select("<script>select count(*) from wo_repair r join eq_equipment e on e.id = r.equip_id where r.created_time &gt;= #{startTime} and r.created_time &lt; #{endTime} and r.status &lt;&gt; 2 and e.deleted = 0 <if test='categoryId != null'>and e.category_id = #{categoryId}</if></script>")
    long countFaults(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, @Param("categoryId") Long categoryId);

    @Select("select count(*) from wo_spare where status = 1 and stock_qty <= warning_qty")
    long countSpareAlerts();

    @Select("<script>select e.id equip_id, e.equip_code, e.equip_name, count(r.id) fault_count, coalesce(sum(case when o.status = 3 then o.downtime_minutes else 0 end), 0) downtime_minutes from wo_repair r join eq_equipment e on e.id = r.equip_id left join wo_order o on o.repair_id = r.id where r.created_time &gt;= #{startTime} and r.created_time &lt; #{endTime} and r.status &lt;&gt; 2 and e.deleted = 0 <if test='categoryId != null'>and e.category_id = #{categoryId}</if> group by e.id, e.equip_code, e.equip_name order by fault_count desc, downtime_minutes desc</script>")
    List<EquipmentFaultRow> selectEquipmentFaults(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, @Param("categoryId") Long categoryId);

    @Select("select e.equip_name name, count(r.id) total from wo_repair r join eq_equipment e on e.id = r.equip_id where r.created_time >= #{startTime} and r.created_time < #{endTime} and r.status <> 2 group by e.id, e.equip_name order by total desc, e.equip_name")
    List<FaultCountRow> selectFaultByEquipment(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("select coalesce(r.source_type, 'UNKNOWN') name, count(r.id) total from wo_repair r where r.created_time >= #{startTime} and r.created_time < #{endTime} and r.status <> 2 group by r.source_type order by total desc, name")
    List<FaultCountRow> selectFaultByType(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("select count(*) task_total, coalesce(sum(case when t.status = 2 then 1 else 0 end), 0) task_finish, coalesce(sum(case when t.status = 3 then 1 else 0 end), 0) task_timeout, coalesce(sum(t.abnormal_count), 0) abnormal_count, coalesce((select count(*) from ins_task_item ti join ins_task tx on tx.id = ti.task_id where tx.plan_time >= #{startDate} and tx.plan_time <= #{endDate} and ti.check_result is not null), 0) checked_items from ins_task t where t.plan_time >= #{startDate} and t.plan_time <= #{endDate}")
    InspectionMetricRow selectInspectionMetrics(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
