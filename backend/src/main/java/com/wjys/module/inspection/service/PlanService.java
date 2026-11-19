package com.wjys.module.inspection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wjys.common.exception.BusinessException;
import com.wjys.module.inspection.dto.ChangePlanStatusRequest;
import com.wjys.module.inspection.dto.CreatePlanRequest;
import com.wjys.module.inspection.dto.UpdatePlanRequest;
import com.wjys.module.inspection.entity.InsPlan;
import com.wjys.module.inspection.mapper.InsPlanMapper;
import com.wjys.module.inspection.mapper.InsPlanPointMapper;
import com.wjys.module.inspection.mapper.InsPointMapper;
import com.wjys.module.inspection.enums.InspectionAssignmentMode;
import com.wjys.module.inspection.vo.InspectionAssigneeVO;
import com.wjys.module.inspection.vo.PlanVO;
import com.wjys.module.system.mapper.SysUserMapper;
import com.wjys.module.equipment.entity.EqEquipment;
import com.wjys.module.equipment.service.EquipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class PlanService {
    private final InsPlanMapper planMapper;
    private final InsPlanPointMapper planPointMapper;
    private final InsPointMapper pointMapper;
    private final SysUserMapper userMapper;
    private final EquipmentService equipmentService;

    public List<PlanVO> list() { return planMapper.selectList(new LambdaQueryWrapper<InsPlan>().orderByDesc(InsPlan::getId)).stream().map(this::toVO).toList(); }

    @Transactional
    public PlanVO create(CreatePlanRequest request) {
        if (planMapper.selectOne(new LambdaQueryWrapper<InsPlan>().eq(InsPlan::getPlanCode, request.planCode())) != null) throw new BusinessException(HttpStatus.CONFLICT, "计划编码已存在");
        List<Long> pointIds = request.pointIds().stream().distinct().toList();
        if (pointMapper.selectBatchIds(pointIds).size() != pointIds.size()) throw new BusinessException(HttpStatus.BAD_REQUEST, "存在无效巡检点位");
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) throw new BusinessException(HttpStatus.BAD_REQUEST, "结束日期不能早于开始日期");
        InspectionAssignmentMode mode = parseMode(request.assignmentMode());
        validateAssignment(mode, request.responsibleUserId(), request.assignmentDeptId(), pointIds, false);
        InsPlan plan = new InsPlan(); plan.setPlanCode(request.planCode()); plan.setPlanName(request.planName()); plan.setCycleType(request.cycleType().toUpperCase(Locale.ROOT)); plan.setCycleValue(request.cycleValue()); plan.setStartDate(request.startDate()); plan.setEndDate(request.endDate()); plan.setExecuteTime(request.executeTime() == null ? LocalTime.of(9, 0) : request.executeTime()); plan.setAssignmentMode(mode.name()); plan.setAssignmentDeptId(request.assignmentDeptId()); plan.setResponsibleUserId(mode == InspectionAssignmentMode.FIXED_USER ? request.responsibleUserId() : null); plan.setStatus(0); planMapper.insert(plan); planPointMapper.insertLinks(plan.getId(), pointIds); return toVO(plan);
    }

    @Transactional
    public PlanVO update(Long id, UpdatePlanRequest request) {
        InsPlan plan = planMapper.selectById(id);
        if (plan == null) throw new BusinessException(HttpStatus.NOT_FOUND, "巡检计划不存在");
        InsPlan sameCode = planMapper.selectOne(new LambdaQueryWrapper<InsPlan>().eq(InsPlan::getPlanCode, request.planCode()).ne(InsPlan::getId, id));
        if (sameCode != null) throw new BusinessException(HttpStatus.CONFLICT, "计划编码已存在");
        List<Long> pointIds = request.pointIds().stream().distinct().toList();
        if (pointMapper.selectBatchIds(pointIds).size() != pointIds.size()) throw new BusinessException(HttpStatus.BAD_REQUEST, "存在无效巡检点位");
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) throw new BusinessException(HttpStatus.BAD_REQUEST, "结束日期不能早于开始日期");
        InspectionAssignmentMode mode = parseMode(request.assignmentMode());
        validateAssignment(mode, request.responsibleUserId(), request.assignmentDeptId(), pointIds, Integer.valueOf(1).equals(plan.getStatus()));
        plan.setPlanCode(request.planCode()); plan.setPlanName(request.planName()); plan.setCycleType(request.cycleType().toUpperCase(Locale.ROOT)); plan.setCycleValue(request.cycleValue()); plan.setStartDate(request.startDate()); plan.setEndDate(request.endDate()); plan.setExecuteTime(request.executeTime() == null ? LocalTime.of(9, 0) : request.executeTime()); plan.setAssignmentMode(mode.name()); plan.setAssignmentDeptId(request.assignmentDeptId()); plan.setResponsibleUserId(mode == InspectionAssignmentMode.FIXED_USER ? request.responsibleUserId() : null);
        planMapper.updateById(plan);
        planPointMapper.deleteByPlanId(id);
        planPointMapper.insertLinks(id, pointIds);
        return toVO(plan);
    }

    @Transactional
    public PlanVO changeStatus(Long id, ChangePlanStatusRequest request) {
        if (request.status() < 0 || request.status() > 2) throw new BusinessException(HttpStatus.BAD_REQUEST, "计划状态无效");
        InsPlan plan = planMapper.selectById(id); if (plan == null) throw new BusinessException(HttpStatus.NOT_FOUND, "巡检计划不存在");
        if (request.status() == 1) {
            if (planPointMapper.selectPointIds(id).isEmpty()) throw new BusinessException(HttpStatus.CONFLICT, "计划至少需要一个巡检点位");
            validateAssignment(parseMode(plan.getAssignmentMode()), plan.getResponsibleUserId(), plan.getAssignmentDeptId(), planPointMapper.selectPointIds(id), true);
        }
        plan.setStatus(request.status()); planMapper.updateById(plan); return toVO(plan);
    }

    InsPlan requireActive(Long id) { InsPlan plan = planMapper.selectById(id); if (plan == null) throw new BusinessException(HttpStatus.NOT_FOUND, "巡检计划不存在"); if (!Integer.valueOf(1).equals(plan.getStatus())) throw new BusinessException(HttpStatus.CONFLICT, "只有启用计划才能生成任务"); return plan; }
    List<Long> pointIds(Long planId) { return planPointMapper.selectPointIds(planId); }
    public List<InspectionAssigneeVO> eligibleInspectors() { return userMapper.selectActiveInspectors(); }

    public boolean isScheduledDate(InsPlan plan, java.time.LocalDate date) {
        if (date.isBefore(plan.getStartDate()) || (plan.getEndDate() != null && date.isAfter(plan.getEndDate()))) return false;
        String type = plan.getCycleType() == null ? "DAILY" : plan.getCycleType().toUpperCase(Locale.ROOT);
        int value = integerOrDefault(plan.getCycleValue(), 1);
        if ("DAILY".equals(type)) return java.time.temporal.ChronoUnit.DAYS.between(plan.getStartDate(), date) % Math.max(value, 1) == 0;
        if ("WEEKLY".equals(type)) return date.getDayOfWeek().getValue() == value;
        if ("MONTHLY".equals(type)) return date.getDayOfMonth() == Math.min(value, date.lengthOfMonth());
        if ("INTERVAL".equals(type)) return value > 0 && java.time.temporal.ChronoUnit.DAYS.between(plan.getStartDate(), date) % value == 0;
        return false;
    }

    private void validateAssignment(InspectionAssignmentMode mode, Long userId, Long deptId, List<Long> pointIds, boolean validateEquipmentManagers) {
        if (mode == InspectionAssignmentMode.FIXED_USER && (userId == null || !userMapper.isActiveInspector(userId))) throw new BusinessException(HttpStatus.BAD_REQUEST, "固定执行人必须选择启用中的巡检员");
        if (mode == InspectionAssignmentMode.TEAM_ROUND_ROBIN && (deptId == null || userMapper.selectActiveInspectorIdsByDept(deptId).isEmpty())) throw new BusinessException(HttpStatus.BAD_REQUEST, "班组轮询必须选择至少有一名巡检员的部门");
        if (mode != InspectionAssignmentMode.FIXED_USER && userId != null) throw new BusinessException(HttpStatus.BAD_REQUEST, "当前派发方式不需要固定执行人");
        if (mode != InspectionAssignmentMode.TEAM_ROUND_ROBIN && deptId != null) throw new BusinessException(HttpStatus.BAD_REQUEST, "当前派发方式不需要执行部门");
        if (validateEquipmentManagers && mode == InspectionAssignmentMode.EQUIPMENT_MANAGER) {
            Long managerId = null;
            for (Long pointId : pointIds) {
                var point = pointMapper.selectById(pointId);
                EqEquipment equipment = equipmentService.getActive(point.getEquipId());
                Long current = equipment.getResponsibleUserId();
                if (current == null || !userMapper.isActiveInspector(current)) throw new BusinessException(HttpStatus.CONFLICT, "设备责任人派发要求所有设备都配置启用中的巡检员责任人");
                if (managerId == null) managerId = current;
                else if (!managerId.equals(current)) throw new BusinessException(HttpStatus.CONFLICT, "同一计划包含多个设备责任人，请改用固定人员或班组轮询");
            }
        }
    }

    private InspectionAssignmentMode parseMode(String value) {
        InspectionAssignmentMode mode = InspectionAssignmentMode.from(value);
        if (mode == null) throw new BusinessException(HttpStatus.BAD_REQUEST, "巡检任务派发方式无效");
        return mode;
    }

    private int integerOrDefault(String value, int fallback) {
        try { return value == null || value.isBlank() ? fallback : Integer.parseInt(value); }
        catch (NumberFormatException exception) { return fallback; }
    }

    PlanVO toVO(InsPlan plan) { return new PlanVO(plan.getId(), plan.getPlanCode(), plan.getPlanName(), plan.getCycleType(), plan.getCycleValue(), plan.getStartDate(), plan.getEndDate(), plan.getExecuteTime(), parseMode(plan.getAssignmentMode()).name(), plan.getAssignmentDeptId(), plan.getResponsibleUserId(), plan.getStatus(), planPointMapper.selectPointIds(plan.getId())); }
}
