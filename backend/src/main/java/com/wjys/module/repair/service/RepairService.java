package com.wjys.module.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wjys.common.exception.BusinessException;
import com.wjys.module.inspection.entity.InsAbnormal;
import com.wjys.module.inspection.mapper.InsAbnormalMapper;
import com.wjys.module.repair.dto.RejectRequest;
import com.wjys.module.repair.entity.WoOrder;
import com.wjys.module.repair.entity.WoOrderLog;
import com.wjys.module.repair.entity.WoRepair;
import com.wjys.module.repair.mapper.WoOrderLogMapper;
import com.wjys.module.repair.mapper.WoOrderMapper;
import com.wjys.module.repair.mapper.WoRepairMapper;
import com.wjys.module.repair.vo.RepairVO;
import com.wjys.module.repair.vo.WorkOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RepairService {
    private static final String SOURCE_TYPE = "INSPECTION_ABNORMAL";
    private static final String MAINTENANCE_SOURCE_TYPE = "MAINTENANCE_RECORD";
    private final InsAbnormalMapper abnormalMapper;
    private final WoRepairMapper repairMapper;
    private final WoOrderMapper orderMapper;
    private final WoOrderLogMapper orderLogMapper;

    public List<RepairVO> list(Integer status) {
        return repairMapper.selectList(new LambdaQueryWrapper<WoRepair>().eq(status != null, WoRepair::getStatus, status).orderByDesc(WoRepair::getId)).stream().map(this::toVO).toList();
    }

    @Transactional
    public RepairVO createFromAbnormal(Long abnormalId, Long reporterId) {
        InsAbnormal abnormal = abnormalMapper.selectById(abnormalId);
        if (abnormal == null) throw new BusinessException(HttpStatus.NOT_FOUND, "异常记录不存在");
        if (!Integer.valueOf(0).equals(abnormal.getStatus())) throw new BusinessException(HttpStatus.CONFLICT, "异常已转报修或已关闭");
        WoRepair existing = repairMapper.selectOne(new LambdaQueryWrapper<WoRepair>().eq(WoRepair::getSourceType, SOURCE_TYPE).eq(WoRepair::getSourceId, abnormalId));
        if (existing != null) throw new BusinessException(HttpStatus.CONFLICT, "该异常已生成报修单");

        WoRepair repair = new WoRepair();
        repair.setEquipId(abnormal.getEquipId()); repair.setSourceType(SOURCE_TYPE); repair.setSourceId(abnormalId);
        repair.setDescription(abnormal.getDescription()); repair.setReporterId(reporterId); repair.setStatus(0);
        try { repairMapper.insert(repair); } catch (DuplicateKeyException exception) { throw new BusinessException(HttpStatus.CONFLICT, "该异常已生成报修单"); }
        int affected = abnormalMapper.update(null, new LambdaUpdateWrapper<InsAbnormal>().eq(InsAbnormal::getId, abnormalId).eq(InsAbnormal::getStatus, 0).set(InsAbnormal::getStatus, 1));
        if (affected != 1) throw new BusinessException(HttpStatus.CONFLICT, "异常状态已被其他操作更新");
        return toVO(repair);
    }

    @Transactional
    public RepairVO createFromMaintenance(Long recordId, Long equipId, String description, Long reporterId) {
        WoRepair existing = repairMapper.selectOne(new LambdaQueryWrapper<WoRepair>()
                .eq(WoRepair::getSourceType, MAINTENANCE_SOURCE_TYPE)
                .eq(WoRepair::getSourceId, recordId));
        if (existing != null) throw new BusinessException(HttpStatus.CONFLICT, "该维保记录已生成报修单");
        WoRepair repair = new WoRepair();
        repair.setEquipId(equipId); repair.setSourceType(MAINTENANCE_SOURCE_TYPE); repair.setSourceId(recordId);
        repair.setDescription(description); repair.setReporterId(reporterId); repair.setStatus(0);
        try {
            repairMapper.insert(repair);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "该维保记录已生成报修单");
        }
        return toVO(repair);
    }

    @Transactional
    public WorkOrderVO accept(Long repairId, Long operatorId) {
        WoRepair repair = require(repairId);
        if (!Integer.valueOf(0).equals(repair.getStatus())) throw new BusinessException(HttpStatus.CONFLICT, "当前报修状态不可受理");
        if (orderMapper.selectOne(new LambdaQueryWrapper<WoOrder>().eq(WoOrder::getRepairId, repairId)) != null) throw new BusinessException(HttpStatus.CONFLICT, "该报修已存在工单");
        WoOrder order = new WoOrder(); order.setRepairId(repairId); order.setOrderNo("WO-" + LocalDate.now() + "-" + UUID.randomUUID().toString().substring(0, 8)); order.setStatus(0); order.setVersion(0);
        orderMapper.insert(order);
        int affected = repairMapper.update(null, new LambdaUpdateWrapper<WoRepair>().eq(WoRepair::getId, repairId).eq(WoRepair::getStatus, 0).set(WoRepair::getStatus, 1));
        if (affected != 1) throw new BusinessException(HttpStatus.CONFLICT, "报修状态已被其他操作更新");
        appendLog(order.getId(), "ACCEPT", operatorId, "受理报修");
        return toWorkOrderVO(order, repair.getEquipId());
    }

    @Transactional
    public void reject(Long repairId, RejectRequest request, Long operatorId) {
        require(repairId);
        int affected = repairMapper.update(null, new LambdaUpdateWrapper<WoRepair>().eq(WoRepair::getId, repairId).eq(WoRepair::getStatus, 0).set(WoRepair::getStatus, 2));
        if (affected != 1) throw new BusinessException(HttpStatus.CONFLICT, "当前报修状态不可驳回");
        WoOrder order = orderMapper.selectOne(new LambdaQueryWrapper<WoOrder>().eq(WoOrder::getRepairId, repairId));
        if (order != null) appendLog(order.getId(), "REJECT_REPAIR", operatorId, request.reason());
    }

    int closeAfterAccept(Long repairId) {
        return repairMapper.update(null, new LambdaUpdateWrapper<WoRepair>().eq(WoRepair::getId, repairId).eq(WoRepair::getStatus, 1).set(WoRepair::getStatus, 3));
    }

    public WoRepair require(Long repairId) {
        WoRepair repair = repairMapper.selectById(repairId);
        if (repair == null) throw new BusinessException(HttpStatus.NOT_FOUND, "报修单不存在");
        return repair;
    }

    void appendLog(Long orderId, String action, Long operatorId, String remark) {
        WoOrderLog log = new WoOrderLog(); log.setOrderId(orderId); log.setAction(action); log.setOperatorId(operatorId); log.setRemark(remark); orderLogMapper.insert(log);
    }

    WorkOrderVO toWorkOrderVO(WoOrder order, Long equipId) {
        return new WorkOrderVO(order.getId(), order.getRepairId(), order.getOrderNo(), equipId, order.getAssigneeId(), order.getStatus(), order.getDispatchRemark(), order.getExpectFinishTime(), order.getFinishReason(), order.getRepairPlan(), order.getFinishResult(), order.getDowntimeMinutes(), order.getAcceptImages(), order.getRejectReason(), order.getFinishedTime(), order.getAcceptedTime(), order.getSpareCost());
    }

    private RepairVO toVO(WoRepair repair) {
        return new RepairVO(repair.getId(), repair.getEquipId(), repair.getSourceType(), repair.getSourceId(), repair.getDescription(), repair.getReporterId(), repair.getStatus(), repair.getCreatedTime());
    }
}
