package com.wjys.module.equipment.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wjys.common.exception.BusinessException;
import com.wjys.module.equipment.dto.ChangeEquipmentStatusRequest;
import com.wjys.module.equipment.entity.EqEquipment;
import com.wjys.module.equipment.entity.EqStatusLog;
import com.wjys.module.equipment.enums.EquipmentStatus;
import com.wjys.module.equipment.mapper.EqEquipmentMapper;
import com.wjys.module.equipment.mapper.EqStatusLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EquipmentStatusService {
    private final EqEquipmentMapper equipmentMapper;
    private final EqStatusLogMapper statusLogMapper;
    private final EquipmentService equipmentService;

    @Transactional
    public void changeStatus(Long id, ChangeEquipmentStatusRequest request) {
        EquipmentStatus target;
        try { target = EquipmentStatus.fromCode(request.status()); } catch (IllegalArgumentException exception) { throw new BusinessException(HttpStatus.BAD_REQUEST, "设备状态无效"); }
        EqEquipment current = equipmentService.getActive(id);
        if (current.getStatus().equals(target.getCode())) return;
        if (current.getStatus().equals(EquipmentStatus.SCRAPPED.getCode())) throw new BusinessException(HttpStatus.CONFLICT, "已报废设备不可变更状态");
        int affected = equipmentMapper.update(null, new LambdaUpdateWrapper<EqEquipment>().eq(EqEquipment::getId, id).eq(EqEquipment::getStatus, current.getStatus()).eq(EqEquipment::getDeleted, 0).set(EqEquipment::getStatus, target.getCode()).setSql("version = version + 1"));
        if (affected != 1) throw new BusinessException(HttpStatus.CONFLICT, "设备状态已被其他操作更新");
        EqStatusLog log = new EqStatusLog(); log.setEquipId(id); log.setFromStatus(current.getStatus()); log.setToStatus(target.getCode()); log.setSourceType(request.sourceType()); log.setSourceId(request.sourceId()); log.setReason(request.reason()); statusLogMapper.insert(log);
    }
}
