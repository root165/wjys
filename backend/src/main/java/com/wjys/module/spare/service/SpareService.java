package com.wjys.module.spare.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wjys.common.exception.BusinessException;
import com.wjys.module.spare.dto.AdjustStockRequest;
import com.wjys.module.spare.dto.SaveSpareRequest;
import com.wjys.module.spare.entity.WoSpare;
import com.wjys.module.spare.entity.WoSpareRecord;
import com.wjys.module.spare.mapper.WoSpareMapper;
import com.wjys.module.spare.mapper.WoSpareRecordMapper;
import com.wjys.module.spare.vo.SpareVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpareService {
    private final WoSpareMapper spareMapper;
    private final WoSpareRecordMapper recordMapper;

    public List<SpareVO> list(String keyword, Integer status, boolean alerts) {
        return spareMapper.selectList(new LambdaQueryWrapper<WoSpare>()
                .eq(status != null, WoSpare::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper.like(WoSpare::getSpareCode, keyword).or().like(WoSpare::getSpareName, keyword))
                .apply(alerts, "stock_qty <= warning_qty")
                .orderByAsc(WoSpare::getStockQty).orderByDesc(WoSpare::getId)).stream().map(this::toVO).toList();
    }

    @Transactional
    public SpareVO create(SaveSpareRequest request) {
        if (request.status() != 0 && request.status() != 1) throw new BusinessException(HttpStatus.BAD_REQUEST, "备件状态无效");
        if (spareMapper.selectOne(new LambdaQueryWrapper<WoSpare>().eq(WoSpare::getSpareCode, request.spareCode())) != null) throw new BusinessException(HttpStatus.CONFLICT, "备件编码已存在");
        WoSpare spare = new WoSpare(); apply(spare, request); spare.setVersion(0); spareMapper.insert(spare); return toVO(spare);
    }

    @Transactional
    public SpareVO update(Long id, SaveSpareRequest request) {
        WoSpare spare = require(id); apply(spare, request); spareMapper.updateById(spare); return toVO(spare);
    }

    @Transactional
    public SpareVO adjustStock(Long id, AdjustStockRequest request, Long operatorId) {
        WoSpare spare = require(id);
        if (request.delta() == 0) throw new BusinessException(HttpStatus.BAD_REQUEST, "库存调整数量不能为 0");
        LambdaUpdateWrapper<WoSpare> update = new LambdaUpdateWrapper<WoSpare>().eq(WoSpare::getId, id).eq(WoSpare::getStatus, 1);
        if (request.delta() > 0) update.setSql("stock_qty = stock_qty + " + request.delta());
        else update.ge(WoSpare::getStockQty, -request.delta()).setSql("stock_qty = stock_qty - " + (-request.delta()));
        update.setSql("version = version + 1");
        if (spareMapper.update(null, update) != 1) throw new BusinessException(HttpStatus.CONFLICT, "备件库存不足或状态已变更");
        WoSpareRecord record = new WoSpareRecord(); record.setSpareId(id); record.setDirection("ADJUST"); record.setQuantity(Math.abs(request.delta())); record.setUnitPrice(spare.getUnitPrice()); record.setAmount(BigDecimal.ZERO); record.setOperatorId(operatorId); record.setRemark(request.remark()); recordMapper.insert(record);
        spare.setStockQty(spare.getStockQty() + request.delta()); return toVO(spare);
    }

    public WoSpare require(Long id) {
        WoSpare spare = spareMapper.selectById(id);
        if (spare == null) throw new BusinessException(HttpStatus.NOT_FOUND, "备件不存在");
        return spare;
    }

    private void apply(WoSpare spare, SaveSpareRequest request) {
        if (request.status() != 0 && request.status() != 1) throw new BusinessException(HttpStatus.BAD_REQUEST, "备件状态无效");
        spare.setSpareCode(request.spareCode()); spare.setSpareName(request.spareName()); spare.setSpecification(request.specification()); spare.setUnit(request.unit()); spare.setStockQty(request.stockQty()); spare.setWarningQty(request.warningQty()); spare.setUnitPrice(request.unitPrice()); spare.setStatus(request.status());
    }

    private SpareVO toVO(WoSpare spare) { return new SpareVO(spare.getId(), spare.getSpareCode(), spare.getSpareName(), spare.getSpecification(), spare.getUnit(), spare.getStockQty(), spare.getWarningQty(), spare.getUnitPrice(), spare.getStatus(), spare.getStockQty() <= spare.getWarningQty()); }
}
