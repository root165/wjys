package com.wjys.module.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wjys.common.exception.BusinessException;
import com.wjys.module.equipment.dto.ChangeEquipmentStatusRequest;
import com.wjys.module.equipment.enums.EquipmentStatus;
import com.wjys.module.equipment.service.EquipmentStatusService;
import com.wjys.module.inspection.entity.InsAbnormal;
import com.wjys.module.inspection.mapper.InsAbnormalMapper;
import com.wjys.module.repair.dto.DispatchRequest;
import com.wjys.module.repair.dto.FinishRequest;
import com.wjys.module.repair.entity.WoOrder;
import com.wjys.module.repair.entity.WoOrderLog;
import com.wjys.module.repair.mapper.WoOrderLogMapper;
import com.wjys.module.repair.mapper.WoOrderMapper;
import com.wjys.module.repair.vo.OrderLogVO;
import com.wjys.module.repair.vo.WorkOrderVO;
import com.wjys.support.file.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkOrderService {
    private final WoOrderMapper orderMapper;
    private final WoOrderLogMapper orderLogMapper;
    private final RepairService repairService;
    private final EquipmentStatusService equipmentStatusService;
    private final InsAbnormalMapper abnormalMapper;
    private final FileService fileService;

    public List<WorkOrderVO> list(Integer status) {
        return orderMapper.selectList(new LambdaQueryWrapper<WoOrder>().eq(status != null, WoOrder::getStatus, status).orderByDesc(WoOrder::getId)).stream().map(this::toVO).toList();
    }

    public WorkOrderVO detail(Long orderId) {
        return toVO(require(orderId));
    }

    public List<OrderLogVO> logs(Long orderId) {
        require(orderId);
        return orderLogMapper.selectList(new LambdaQueryWrapper<WoOrderLog>().eq(WoOrderLog::getOrderId, orderId).orderByAsc(WoOrderLog::getId)).stream().map(log -> new OrderLogVO(log.getId(), log.getOrderId(), log.getAction(), log.getOperatorId(), log.getRemark(), log.getCreatedTime())).toList();
    }

    @Transactional
    public WorkOrderVO dispatch(Long orderId, DispatchRequest request, Long operatorId) {
        WoOrder order = require(orderId);
        if (!Integer.valueOf(0).equals(order.getStatus())) throw new BusinessException(HttpStatus.CONFLICT, "当前工单状态不可派工");
        int affected = orderMapper.update(null, new LambdaUpdateWrapper<WoOrder>().eq(WoOrder::getId, orderId).eq(WoOrder::getStatus, 0).set(WoOrder::getAssigneeId, request.assigneeId()).set(WoOrder::getDispatchRemark, request.remark()).set(WoOrder::getExpectFinishTime, request.expectFinishTime()).setSql("version = version + 1"));
        if (affected != 1) throw new BusinessException(HttpStatus.CONFLICT, "工单已被其他操作更新");
        repairService.require(order.getRepairId());
        equipmentStatusService.changeStatus(repairService.require(order.getRepairId()).getEquipId(), new ChangeEquipmentStatusRequest(EquipmentStatus.MAINTENANCE.getCode(), "REPAIR_ORDER", orderId, "工单派工"));
        repairService.appendLog(orderId, "DISPATCH", operatorId, request.remark());
        order.setAssigneeId(request.assigneeId()); order.setDispatchRemark(request.remark()); order.setExpectFinishTime(request.expectFinishTime());
        return toVO(order);
    }

    @Transactional
    public WorkOrderVO start(Long orderId, Long userId) {
        WoOrder order = require(orderId);
        if (!Integer.valueOf(0).equals(order.getStatus()) && !Integer.valueOf(4).equals(order.getStatus())) throw new BusinessException(HttpStatus.CONFLICT, "当前工单状态不可接单");
        if (order.getAssigneeId() == null || !order.getAssigneeId().equals(userId)) throw new BusinessException(HttpStatus.FORBIDDEN, "只有指定维修人可以接单");
        int affected = orderMapper.update(null, new LambdaUpdateWrapper<WoOrder>().eq(WoOrder::getId, orderId).in(WoOrder::getStatus, 0, 4).eq(WoOrder::getAssigneeId, userId).set(WoOrder::getStatus, 1).set(WoOrder::getRejectReason, null).setSql("version = version + 1"));
        if (affected != 1) throw new BusinessException(HttpStatus.CONFLICT, "工单已被其他操作更新");
        order.setStatus(1); order.setRejectReason(null); repairService.appendLog(orderId, "START", userId, "维修人接单");
        return toVO(order);
    }

    @Transactional
    public WorkOrderVO finish(Long orderId, FinishRequest request, Long userId) {
        WoOrder order = require(orderId);
        if (!Integer.valueOf(1).equals(order.getStatus())) throw new BusinessException(HttpStatus.CONFLICT, "当前工单状态不可完工");
        if (order.getAssigneeId() == null || !order.getAssigneeId().equals(userId)) throw new BusinessException(HttpStatus.FORBIDDEN, "只有指定维修人可以完工");
        if (request.fileIds() != null && !request.fileIds().isEmpty()) fileService.bindAll(request.fileIds(), "WORK_ORDER", orderId);
        String photoIds = request.fileIds() == null ? null : request.fileIds().stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(null);
        int affected = orderMapper.update(null, new LambdaUpdateWrapper<WoOrder>().eq(WoOrder::getId, orderId).eq(WoOrder::getStatus, 1).eq(WoOrder::getAssigneeId, userId).set(WoOrder::getStatus, 2).set(WoOrder::getFinishReason, request.finishReason()).set(WoOrder::getRepairPlan, request.repairPlan()).set(WoOrder::getFinishResult, request.finishResult()).set(WoOrder::getDowntimeMinutes, request.downtimeMinutes()).set(WoOrder::getAcceptImages, photoIds).set(WoOrder::getFinishedTime, LocalDateTime.now()).setSql("version = version + 1"));
        if (affected != 1) throw new BusinessException(HttpStatus.CONFLICT, "工单已被其他操作更新");
        order.setStatus(2); order.setFinishReason(request.finishReason()); order.setRepairPlan(request.repairPlan()); order.setFinishResult(request.finishResult()); order.setDowntimeMinutes(request.downtimeMinutes()); order.setAcceptImages(photoIds); order.setFinishedTime(LocalDateTime.now());
        repairService.appendLog(orderId, "FINISH", userId, request.finishResult());
        return toVO(order);
    }

    @Transactional
    public WorkOrderVO accept(Long orderId, Long operatorId, String remark) {
        WoOrder order = require(orderId);
        if (!Integer.valueOf(2).equals(order.getStatus())) throw new BusinessException(HttpStatus.CONFLICT, "当前工单状态不可验收");
        int affected = orderMapper.update(null, new LambdaUpdateWrapper<WoOrder>().eq(WoOrder::getId, orderId).eq(WoOrder::getStatus, 2).set(WoOrder::getStatus, 3).set(WoOrder::getAcceptedTime, LocalDateTime.now()).setSql("version = version + 1"));
        if (affected != 1) throw new BusinessException(HttpStatus.CONFLICT, "工单已被其他操作更新");
        var repair = repairService.require(order.getRepairId());
        // The repair and its source abnormal must close in this same transaction.
        int repairAffected = repairService.closeAfterAccept(order.getRepairId());
        if (repairAffected != 1) throw new BusinessException(HttpStatus.CONFLICT, "报修状态不可关闭");
        if ("INSPECTION_ABNORMAL".equals(repair.getSourceType())) abnormalMapper.update(null, new LambdaUpdateWrapper<InsAbnormal>().eq(InsAbnormal::getId, repair.getSourceId()).eq(InsAbnormal::getStatus, 1).set(InsAbnormal::getStatus, 2));
        equipmentStatusService.changeStatus(repair.getEquipId(), new ChangeEquipmentStatusRequest(EquipmentStatus.IDLE.getCode(), "REPAIR_ORDER", orderId, "工单验收通过"));
        order.setStatus(3); order.setAcceptedTime(LocalDateTime.now()); repairService.appendLog(orderId, "ACCEPT_ORDER", operatorId, remark);
        return toVO(order);
    }

    @Transactional
    public WorkOrderVO reject(Long orderId, String reason, Long operatorId) {
        WoOrder order = require(orderId);
        if (!Integer.valueOf(2).equals(order.getStatus())) throw new BusinessException(HttpStatus.CONFLICT, "当前工单状态不可退回");
        int affected = orderMapper.update(null, new LambdaUpdateWrapper<WoOrder>().eq(WoOrder::getId, orderId).eq(WoOrder::getStatus, 2).set(WoOrder::getStatus, 4).set(WoOrder::getRejectReason, reason).setSql("version = version + 1"));
        if (affected != 1) throw new BusinessException(HttpStatus.CONFLICT, "工单已被其他操作更新");
        order.setStatus(4); order.setRejectReason(reason); repairService.appendLog(orderId, "REJECT_ACCEPTANCE", operatorId, reason);
        return toVO(order);
    }

    public WoOrder require(Long orderId) {
        WoOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException(HttpStatus.NOT_FOUND, "工单不存在");
        return order;
    }

    private WorkOrderVO toVO(WoOrder order) {
        return repairService.toWorkOrderVO(order, repairService.require(order.getRepairId()).getEquipId());
    }
}
