package com.wjys.module.inspection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wjys.common.exception.BusinessException;
import com.wjys.module.equipment.entity.EqEquipment;
import com.wjys.module.equipment.service.EquipmentService;
import com.wjys.module.inspection.entity.InsItem;
import com.wjys.module.inspection.entity.InsPlan;
import com.wjys.module.inspection.entity.InsPoint;
import com.wjys.module.inspection.entity.InsTask;
import com.wjys.module.inspection.entity.InsTaskItem;
import com.wjys.module.inspection.mapper.InsItemMapper;
import com.wjys.module.inspection.mapper.InsPointMapper;
import com.wjys.module.inspection.mapper.InsTaskItemMapper;
import com.wjys.module.inspection.mapper.InsTaskMapper;
import com.wjys.module.inspection.enums.InspectionAssignmentMode;
import com.wjys.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskGenerateService {
    private final PlanService planService;
    private final InsPointMapper pointMapper;
    private final InsItemMapper itemMapper;
    private final InsTaskMapper taskMapper;
    private final InsTaskItemMapper taskItemMapper;
    private final EquipmentService equipmentService;
    private final SysUserMapper userMapper;

    @Transactional
    public InsTask generate(Long planId, LocalDate planTime) {
        InsPlan plan = planService.requireActive(planId);
        if (planTime.isBefore(plan.getStartDate()) || (plan.getEndDate() != null && planTime.isAfter(plan.getEndDate()))) throw new BusinessException(HttpStatus.BAD_REQUEST, "任务日期不在计划有效期内");
        if (!planService.isScheduledDate(plan, planTime)) throw new BusinessException(HttpStatus.BAD_REQUEST, "该日期不符合巡检计划周期，不能生成任务");
        InsTask existing = taskMapper.selectByPlanTime(planId, planTime);
        if (existing != null) return existing;
        List<Long> pointIds = planService.pointIds(planId);
        List<InsPoint> points = pointMapper.selectBatchIds(pointIds).stream().filter(point -> Integer.valueOf(1).equals(point.getStatus())).toList();
        if (points.isEmpty()) throw new BusinessException(HttpStatus.CONFLICT, "计划没有可用巡检点位");
        InsTask task = new InsTask(); task.setTaskNo("INS-" + plan.getPlanCode() + "-" + planTime + "-" + UUID.randomUUID().toString().substring(0, 8)); task.setPlanId(planId); task.setPlanTime(planTime); task.setAssigneeId(resolveAssignee(plan, planTime, points)); task.setStatus(0); task.setTotalItems(0); task.setAbnormalCount(0);
        taskMapper.insert(task);
        int total = 0;
        for (InsPoint point : points) {
            EqEquipment equipment = equipmentService.getActive(point.getEquipId());
            List<InsItem> items = itemMapper.selectList(new LambdaQueryWrapper<InsItem>().eq(InsItem::getPointId, point.getId()).eq(InsItem::getStatus, 1));
            for (InsItem item : items) {
                InsTaskItem snapshot = new InsTaskItem(); snapshot.setTaskId(task.getId()); snapshot.setPointId(point.getId()); snapshot.setItemId(item.getId()); snapshot.setEquipId(equipment.getId()); snapshot.setEquipCode(equipment.getEquipCode()); snapshot.setPointCode(point.getPointCode()); snapshot.setItemCode(item.getItemCode()); snapshot.setItemName(item.getItemName()); snapshot.setValueType(item.getValueType()); snapshot.setCheckMethod(item.getCheckMethod()); snapshot.setUnit(item.getUnit()); snapshot.setMinValue(item.getMinValue()); snapshot.setMaxValue(item.getMaxValue()); snapshot.setOptionsJson(item.getOptionsJson()); snapshot.setRequiredPhoto(item.getRequiredPhoto()); taskItemMapper.insert(snapshot); total++;
            }
        }
        if (total == 0) throw new BusinessException(HttpStatus.CONFLICT, "计划没有可用检查项");
        task.setTotalItems(total); taskMapper.updateById(task); return task;
    }

    private Long resolveAssignee(InsPlan plan, LocalDate planTime, List<InsPoint> points) {
        InspectionAssignmentMode mode = InspectionAssignmentMode.from(plan.getAssignmentMode());
        if (mode == null) throw new BusinessException(HttpStatus.CONFLICT, "巡检计划派发方式无效，请重新配置");
        if (mode == InspectionAssignmentMode.SELF_CLAIM) return null;
        if (mode == InspectionAssignmentMode.FIXED_USER) {
            if (plan.getResponsibleUserId() == null || !userMapper.isActiveInspector(plan.getResponsibleUserId())) throw new BusinessException(HttpStatus.CONFLICT, "固定执行人不存在或已停用");
            return plan.getResponsibleUserId();
        }
        if (mode == InspectionAssignmentMode.TEAM_ROUND_ROBIN) {
            List<Long> candidates = userMapper.selectActiveInspectorIdsByDept(plan.getAssignmentDeptId());
            if (candidates.isEmpty()) throw new BusinessException(HttpStatus.CONFLICT, "轮询部门没有启用中的巡检员");
            return candidates.stream().min(Comparator.<Long>comparingInt(userId -> taskMapper.countActiveTasks(userId, planTime)).thenComparingLong(Long::longValue)).orElseThrow();
        }
        Long managerId = null;
        for (InsPoint point : points) {
            EqEquipment equipment = equipmentService.getActive(point.getEquipId());
            Long current = equipment.getResponsibleUserId();
            if (current == null || !userMapper.isActiveInspector(current)) throw new BusinessException(HttpStatus.CONFLICT, "设备责任人派发要求所有设备都配置启用中的巡检员责任人");
            if (managerId == null) managerId = current;
            else if (!managerId.equals(current)) throw new BusinessException(HttpStatus.CONFLICT, "同一计划包含多个设备责任人，不能按设备责任人派发；请改用固定人员或班组轮询");
        }
        return managerId;
    }
}
