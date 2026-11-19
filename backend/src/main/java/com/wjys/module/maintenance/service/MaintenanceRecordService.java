package com.wjys.module.maintenance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wjys.common.exception.BusinessException;
import com.wjys.module.equipment.dto.ChangeEquipmentStatusRequest;
import com.wjys.module.equipment.enums.EquipmentStatus;
import com.wjys.module.equipment.service.EquipmentService;
import com.wjys.module.equipment.service.EquipmentStatusService;
import com.wjys.module.maintenance.dto.AuditMaintenanceRecordRequest;
import com.wjys.module.maintenance.dto.SaveMaintenanceRecordRequest;
import com.wjys.module.maintenance.entity.MtPlan;
import com.wjys.module.maintenance.entity.MtRecord;
import com.wjys.module.maintenance.mapper.MtPlanMapper;
import com.wjys.module.maintenance.mapper.MtRecordMapper;
import com.wjys.module.maintenance.vo.MaintenanceRecordVO;
import com.wjys.module.repair.service.RepairService;
import com.wjys.module.repair.vo.RepairVO;
import com.wjys.support.file.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaintenanceRecordService {
    private static final String SOURCE_TYPE = "MAINTENANCE_RECORD";
    private final MtPlanMapper planMapper;
    private final MtRecordMapper recordMapper;
    private final MaintenancePlanService planService;
    private final EquipmentService equipmentService;
    private final EquipmentStatusService equipmentStatusService;
    private final RepairService repairService;
    private final FileService fileService;

    public List<MaintenanceRecordVO> list(Integer status, Long equipId) {
        return recordMapper.selectList(new LambdaQueryWrapper<MtRecord>()
                        .eq(status != null, MtRecord::getStatus, status)
                        .eq(equipId != null, MtRecord::getEquipId, equipId)
                        .orderByDesc(MtRecord::getMaintainDate)
                        .orderByDesc(MtRecord::getId))
                .stream().map(this::toVO).toList();
    }

    @Transactional
    public MaintenanceRecordVO submit(SaveMaintenanceRecordRequest request, Long maintainerId) {
        equipmentService.getActive(request.equipId());
        MtPlan plan = null;
        if (request.planId() != null) {
            plan = planService.require(request.planId());
            if (!request.equipId().equals(plan.getEquipId())) throw new BusinessException(HttpStatus.BAD_REQUEST, "维保计划与设备不匹配");
            if (!Integer.valueOf(1).equals(plan.getStatus())) throw new BusinessException(HttpStatus.CONFLICT, "保养计划已停用");
            MtRecord duplicate = recordMapper.selectOne(new LambdaQueryWrapper<MtRecord>()
                    .eq(MtRecord::getPlanId, request.planId()).eq(MtRecord::getMaintainDate, request.maintainDate()));
            if (duplicate != null) throw new BusinessException(HttpStatus.CONFLICT, "该计划当天已提交维保记录");
        }
        MtRecord record = new MtRecord();
        record.setRecordCode("MT-" + request.maintainDate() + "-" + UUID.randomUUID().toString().substring(0, 8));
        record.setPlanId(request.planId());
        record.setEquipId(request.equipId());
        record.setMaintainDate(request.maintainDate());
        record.setMaintainerId(maintainerId);
        record.setMaintainContent(request.maintainContent());
        record.setMaintainResult(request.maintainResult());
        record.setCost(request.cost());
        record.setDowntimeMinutes(request.downtimeMinutes());
        record.setImages(joinFileIds(request.fileIds()));
        record.setStatus(0);
        record.setRemark(request.remark());
        try {
            recordMapper.insert(record);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "该计划当天已提交维保记录");
        }
        fileService.bindAll(request.fileIds(), "MAINTENANCE_RECORD", record.getId());
        equipmentStatusService.changeStatus(request.equipId(), new ChangeEquipmentStatusRequest(
                EquipmentStatus.MAINTENANCE.getCode(), SOURCE_TYPE, record.getId(), "提交维保记录"));
        return toVO(record);
    }

    @Transactional
    public MaintenanceRecordVO audit(Long id, AuditMaintenanceRecordRequest request, Long auditorId) {
        MtRecord record = require(id);
        if (!Integer.valueOf(0).equals(record.getStatus())) throw new BusinessException(HttpStatus.CONFLICT,
                Integer.valueOf(1).equals(record.getStatus()) ? "归档记录不可重复审核" : "退回记录请重新提交");
        LocalDateTime auditTime = LocalDateTime.now();
        int targetStatus = Boolean.TRUE.equals(request.pass()) ? 1 : 2;
        int affected = recordMapper.update(null, new LambdaUpdateWrapper<MtRecord>()
                .eq(MtRecord::getId, id).eq(MtRecord::getStatus, 0)
                .set(MtRecord::getStatus, targetStatus).set(MtRecord::getAuditUser, auditorId)
                .set(MtRecord::getAuditTime, auditTime).set(MtRecord::getRemark, request.remark()));
        if (affected != 1) throw new BusinessException(HttpStatus.CONFLICT, "维保记录状态已被其他操作更新");
        record.setStatus(targetStatus); record.setAuditUser(auditorId); record.setAuditTime(auditTime); record.setRemark(request.remark());
        if (targetStatus == 1) {
            MtPlan plan = record.getPlanId() == null ? null : planService.require(record.getPlanId());
            if (plan != null) {
                LocalDate nextDate = nextDate(record.getMaintainDate(), plan.getCycleType(), plan.getCycleValue());
                int planAffected = planServiceAdvance(plan.getId(), plan.getNextDate(), nextDate);
                if (planAffected != 1) throw new BusinessException(HttpStatus.CONFLICT, "保养计划已被其他操作更新");
            }
            equipmentStatusService.changeStatus(record.getEquipId(), new ChangeEquipmentStatusRequest(
                    EquipmentStatus.IDLE.getCode(), SOURCE_TYPE, record.getId(), "维保记录归档"));
        }
        return toVO(record);
    }

    @Transactional
    public RepairVO toRepair(Long id, Long reporterId) {
        MtRecord record = require(id);
        if (!Integer.valueOf(1).equals(record.getStatus()) || !Integer.valueOf(1).equals(record.getMaintainResult())) {
            throw new BusinessException(HttpStatus.CONFLICT, "只有已归档的异常维保记录可以转报修");
        }
        return repairService.createFromMaintenance(record.getId(), record.getEquipId(), record.getMaintainContent(), reporterId);
    }

    public MtRecord require(Long id) {
        MtRecord record = recordMapper.selectById(id);
        if (record == null) throw new BusinessException(HttpStatus.NOT_FOUND, "维保记录不存在");
        return record;
    }

    private int planServiceAdvance(Long planId, LocalDate oldDate, LocalDate nextDate) {
        return planMapper.update(null, new LambdaUpdateWrapper<MtPlan>()
                .eq(MtPlan::getId, planId).eq(MtPlan::getNextDate, oldDate).set(MtPlan::getNextDate, nextDate).setSql("version = version + 1"));
    }

    static LocalDate nextDate(LocalDate maintainDate, Integer cycleType, Integer cycleValue) {
        return switch (cycleType) {
            case 0 -> maintainDate.plusDays(Math.max(1, cycleValue / 24));
            case 1 -> maintainDate.plusDays(cycleValue);
            case 2 -> maintainDate.plusDays(7L * cycleValue);
            case 3 -> {
                YearMonth target = YearMonth.from(maintainDate).plusMonths(cycleValue);
                yield target.atDay(Math.min(maintainDate.getDayOfMonth(), target.lengthOfMonth()));
            }
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "周期类型无效");
        };
    }

    private String joinFileIds(List<Long> fileIds) { return fileIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(null); }

    private MaintenanceRecordVO toVO(MtRecord record) {
        List<Long> fileIds = record.getImages() == null || record.getImages().isBlank() ? List.of() :
                Arrays.stream(record.getImages().split(",")).map(Long::valueOf).toList();
        return new MaintenanceRecordVO(record.getId(), record.getRecordCode(), record.getPlanId(), record.getEquipId(),
                record.getMaintainDate(), record.getMaintainerId(), record.getMaintainContent(), record.getMaintainResult(),
                record.getCost(), record.getDowntimeMinutes(), fileIds, record.getStatus(), record.getAuditUser(),
                record.getAuditTime(), record.getRemark(), record.getCreatedTime());
    }
}
