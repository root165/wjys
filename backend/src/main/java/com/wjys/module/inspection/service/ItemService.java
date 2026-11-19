package com.wjys.module.inspection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wjys.common.exception.BusinessException;
import com.wjys.module.inspection.dto.CreateItemRequest;
import com.wjys.module.inspection.entity.InsItem;
import com.wjys.module.inspection.mapper.InsItemMapper;
import com.wjys.module.inspection.mapper.InsPointMapper;
import com.wjys.module.inspection.vo.ItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final InsItemMapper itemMapper;
    private final InsPointMapper pointMapper;

    public List<ItemVO> list(Long pointId) { return itemMapper.selectList(new LambdaQueryWrapper<InsItem>().eq(pointId != null, InsItem::getPointId, pointId).eq(InsItem::getStatus, 1).orderByAsc(InsItem::getId)).stream().map(this::toVO).toList(); }

    @Transactional
    public ItemVO create(CreateItemRequest request) {
        if (pointMapper.selectById(request.pointId()) == null) throw new BusinessException(HttpStatus.BAD_REQUEST, "巡检点位不存在");
        if (itemMapper.selectOne(new LambdaQueryWrapper<InsItem>().eq(InsItem::getItemCode, request.itemCode())) != null) throw new BusinessException(HttpStatus.CONFLICT, "检查项编码已存在");
        InsItem item = new InsItem(); item.setPointId(request.pointId()); item.setItemCode(request.itemCode()); item.setItemName(request.itemName()); item.setCheckMethod(request.checkMethod()); item.setValueType(request.valueType()); item.setMinValue(request.minValue()); item.setMaxValue(request.maxValue()); item.setUnit(request.unit()); item.setOptionsJson(request.optionsJson()); item.setRequiredPhoto(request.requiredPhoto() == null ? 0 : request.requiredPhoto()); item.setStatus(1); itemMapper.insert(item); return toVO(item);
    }

    private ItemVO toVO(InsItem item) { return new ItemVO(item.getId(), item.getPointId(), item.getItemCode(), item.getItemName(), item.getCheckMethod(), item.getValueType(), item.getMinValue(), item.getMaxValue(), item.getUnit(), item.getOptionsJson(), item.getRequiredPhoto(), item.getStatus()); }
}
