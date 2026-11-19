package com.wjys.module.spare.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wjys.common.exception.BusinessException;
import com.wjys.module.repair.entity.WoOrder;
import com.wjys.module.repair.mapper.WoOrderMapper;
import com.wjys.module.repair.service.WorkOrderService;
import com.wjys.module.spare.dto.SpareUseRequest;
import com.wjys.module.spare.entity.WoSpare;
import com.wjys.module.spare.entity.WoSpareRecord;
import com.wjys.module.spare.mapper.WoSpareMapper;
import com.wjys.module.spare.mapper.WoSpareRecordMapper;
import com.wjys.module.spare.vo.SpareRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpareConsumptionService {
    private final WoOrderMapper orderMapper;
    private final WoSpareMapper spareMapper;
    private final WoSpareRecordMapper recordMapper;
    private final WorkOrderService workOrderService;

    @Transactional
    public List<SpareRecordVO> consume(Long orderId, List<SpareUseRequest> requests, Long operatorId) {
        WoOrder order = workOrderService.require(orderId);
        if (!Integer.valueOf(1).equals(order.getStatus()) && !Integer.valueOf(2).equals(order.getStatus())) throw new BusinessException(HttpStatus.CONFLICT, "当前工单状态不可领用备件");
        if (new HashSet<>(requests.stream().map(SpareUseRequest::spareId).toList()).size() != requests.size()) throw new BusinessException(HttpStatus.BAD_REQUEST, "同一请求不可重复领用同一备件");
        BigDecimal total = BigDecimal.ZERO;
        List<SpareRecordVO> result = new java.util.ArrayList<>();
        for (SpareUseRequest request : requests) {
            WoSpare spare = requireActive(request.spareId());
            int affected = spareMapper.update(null, new LambdaUpdateWrapper<WoSpare>().eq(WoSpare::getId, request.spareId()).eq(WoSpare::getStatus, 1).ge(WoSpare::getStockQty, request.quantity()).setSql("stock_qty = stock_qty - " + request.quantity()).setSql("version = version + 1"));
            if (affected != 1) throw new BusinessException(HttpStatus.CONFLICT, "备件库存不足或已停用");
            BigDecimal amount = spare.getUnitPrice().multiply(BigDecimal.valueOf(request.quantity())); total = total.add(amount);
            WoSpareRecord record = new WoSpareRecord(); record.setOrderId(orderId); record.setSpareId(request.spareId()); record.setDirection("CONSUME"); record.setQuantity(request.quantity()); record.setUnitPrice(spare.getUnitPrice()); record.setAmount(amount); record.setOperatorId(operatorId); record.setRemark(request.remark()); recordMapper.insert(record); result.add(toVO(record));
        }
        int orderAffected = orderMapper.update(null, new LambdaUpdateWrapper<WoOrder>().eq(WoOrder::getId, orderId).in(WoOrder::getStatus, 1, 2).setSql("spare_cost = spare_cost + " + total).setSql("version = version + 1"));
        if (orderAffected != 1) throw new BusinessException(HttpStatus.CONFLICT, "工单状态已被其他操作更新");
        return result;
    }

    @Transactional
    public SpareRecordVO reverse(Long recordId, Long operatorId, String remark) {
        WoSpareRecord original = recordMapper.selectById(recordId);
        if (original == null || !"CONSUME".equals(original.getDirection())) throw new BusinessException(HttpStatus.NOT_FOUND, "可撤销的领用流水不存在");
        if (recordMapper.selectOne(new LambdaQueryWrapper<WoSpareRecord>().eq(WoSpareRecord::getSourceRecordId, recordId).eq(WoSpareRecord::getDirection, "REVERSE")) != null) throw new BusinessException(HttpStatus.CONFLICT, "该领用流水已撤销");
        WoSpare spare = requireActive(original.getSpareId());
        if (spareMapper.update(null, new LambdaUpdateWrapper<WoSpare>().eq(WoSpare::getId, spare.getId()).eq(WoSpare::getStatus, 1).setSql("stock_qty = stock_qty + " + original.getQuantity()).setSql("version = version + 1")) != 1) throw new BusinessException(HttpStatus.CONFLICT, "备件状态已变更");
        if (original.getOrderId() != null && orderMapper.update(null, new LambdaUpdateWrapper<WoOrder>().eq(WoOrder::getId, original.getOrderId()).in(WoOrder::getStatus, 1, 2).setSql("spare_cost = spare_cost - " + original.getAmount()).setSql("version = version + 1")) != 1) throw new BusinessException(HttpStatus.CONFLICT, "工单状态不可撤销领用");
        WoSpareRecord reverse = new WoSpareRecord(); reverse.setOrderId(original.getOrderId()); reverse.setSpareId(original.getSpareId()); reverse.setSourceRecordId(recordId); reverse.setDirection("REVERSE"); reverse.setQuantity(original.getQuantity()); reverse.setUnitPrice(original.getUnitPrice()); reverse.setAmount(original.getAmount().negate()); reverse.setOperatorId(operatorId); reverse.setRemark(remark); recordMapper.insert(reverse); return toVO(reverse);
    }

    public List<SpareRecordVO> records(Long orderId) {
        workOrderService.require(orderId);
        return recordMapper.selectList(new LambdaQueryWrapper<WoSpareRecord>().eq(WoSpareRecord::getOrderId, orderId).orderByAsc(WoSpareRecord::getId)).stream().map(this::toVO).toList();
    }

    private WoSpare requireActive(Long id) { WoSpare spare = spareMapper.selectById(id); if (spare == null) throw new BusinessException(HttpStatus.NOT_FOUND, "备件不存在"); if (!Integer.valueOf(1).equals(spare.getStatus())) throw new BusinessException(HttpStatus.CONFLICT, "备件已停用"); return spare; }
    private SpareRecordVO toVO(WoSpareRecord record) { return new SpareRecordVO(record.getId(), record.getOrderId(), record.getSpareId(), record.getSourceRecordId(), record.getDirection(), record.getQuantity(), record.getUnitPrice(), record.getAmount(), record.getOperatorId(), record.getRemark(), record.getCreatedTime()); }
}
