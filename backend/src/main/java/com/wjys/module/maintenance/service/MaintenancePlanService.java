package com.wjys.module.maintenance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wjys.common.exception.BusinessException;
import com.wjys.module.equipment.service.EquipmentService;
import com.wjys.module.maintenance.dto.ChangeMaintenancePlanStatusRequest;
import com.wjys.module.maintenance.dto.SaveMaintenancePlanRequest;
import com.wjys.module.maintenance.entity.MtPlan;
import com.wjys.module.maintenance.mapper.MtPlanMapper;
import com.wjys.module.maintenance.vo.MaintenancePlanVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenancePlanService {
    private final MtPlanMapper planMapper;
    private final EquipmentService equipmentService;

    public List<MaintenancePlanVO> list(Integer status) {
        return planMapper.selectList(new LambdaQueryWrapper<MtPlan>()
                        .eq(status != null, MtPlan::getStatus, status)
                        .orderByAsc(MtPlan::getNextDate)
                        .orderByDesc(MtPlan::getId))
                .stream().map(this::toVO).toList();
    }

    @Transactional
    public MaintenancePlanVO create(SaveMaintenancePlanRequest request, Long creatorId) {
        equipmentService.getActive(request.equipId());
        if (planMapper.selectOne(new LambdaQueryWrapper<MtPlan>().eq(MtPlan::getPlanCode, request.planCode())) != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "保养计划编码已存在");
        }
        MtPlan plan = new MtPlan();
        apply(plan, request);
        plan.setStatus(1);
        plan.setCreatorId(creatorId);
        plan.setVersion(0);
        try {
            planMapper.insert(plan);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "保养计划编码已存在");
        }
        return toVO(plan);
    }

    @Transactional
    public MaintenancePlanVO update(Long id, SaveMaintenancePlanRequest request) {
        MtPlan plan = require(id);
        equipmentService.getActive(request.equipId());
        MtPlan sameCode = planMapper.selectOne(new LambdaQueryWrapper<MtPlan>()
                .eq(MtPlan::getPlanCode, request.planCode()).ne(MtPlan::getId, id));
        if (sameCode != null) throw new BusinessException(HttpStatus.CONFLICT, "保养计划编码已存在");
        apply(plan, request);
        try {
            planMapper.updateById(plan);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "保养计划编码已存在");
        }
        return toVO(plan);
    }

    @Transactional
    public MaintenancePlanVO changeStatus(Long id, ChangeMaintenancePlanStatusRequest request) {
        MtPlan plan = require(id);
        int affected = planMapper.update(null, new LambdaUpdateWrapper<MtPlan>()
                .eq(MtPlan::getId, id)
                .eq(MtPlan::getStatus, plan.getStatus())
                .set(MtPlan::getStatus, request.status())
                .setSql("version = version + 1"));
        if (affected != 1) throw new BusinessException(HttpStatus.CONFLICT, "保养计划已被其他操作更新");
        plan.setStatus(request.status());
        return toVO(plan);
    }

    public MtPlan require(Long id) {
        MtPlan plan = planMapper.selectById(id);
        if (plan == null) throw new BusinessException(HttpStatus.NOT_FOUND, "保养计划不存在");
        return plan;
    }

    private void apply(MtPlan plan, SaveMaintenancePlanRequest request) {
        plan.setPlanCode(request.planCode());
        plan.setPlanName(request.planName());
        plan.setEquipId(request.equipId());
        plan.setMaintainType(request.maintainType());
        plan.setCycleType(request.cycleType());
        plan.setCycleValue(request.cycleValue());
        plan.setContent(request.content());
        plan.setAssigneeId(request.assigneeId());
        plan.setNextDate(request.nextDate());
    }

    private MaintenancePlanVO toVO(MtPlan plan) {
        return new MaintenancePlanVO(plan.getId(), plan.getPlanCode(), plan.getPlanName(), plan.getEquipId(),
                plan.getMaintainType(), plan.getCycleType(), plan.getCycleValue(), plan.getContent(),
                plan.getAssigneeId(), plan.getNextDate(), plan.getStatus(), plan.getCreatorId());
    }
}
