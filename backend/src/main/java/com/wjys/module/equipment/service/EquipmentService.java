package com.wjys.module.equipment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wjys.common.exception.BusinessException;
import com.wjys.common.model.PageResult;
import com.wjys.module.equipment.dto.EquipmentQuery;
import com.wjys.module.equipment.dto.SaveEquipmentRequest;
import com.wjys.module.equipment.dto.SaveParamRequest;
import com.wjys.module.equipment.entity.EqEquipment;
import com.wjys.module.equipment.entity.EqParam;
import com.wjys.module.equipment.entity.EqStatusLog;
import com.wjys.module.equipment.enums.EquipmentStatus;
import com.wjys.module.equipment.mapper.EqCategoryMapper;
import com.wjys.module.equipment.mapper.EqEquipmentMapper;
import com.wjys.module.equipment.mapper.EqParamMapper;
import com.wjys.module.equipment.mapper.EqStatusLogMapper;
import com.wjys.module.equipment.vo.EquipmentDetailVO;
import com.wjys.module.equipment.vo.EquipmentListVO;
import com.wjys.module.equipment.vo.ParamVO;
import com.wjys.module.equipment.vo.StatusLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipmentService {
    private final EqEquipmentMapper equipmentMapper;
    private final EqCategoryMapper categoryMapper;
    private final EqParamMapper paramMapper;
    private final EqStatusLogMapper statusLogMapper;

    public PageResult<EquipmentListVO> page(EquipmentQuery query) {
        LambdaQueryWrapper<EqEquipment> wrapper = new LambdaQueryWrapper<EqEquipment>()
                .eq(EqEquipment::getDeleted, 0)
                .eq(query.categoryId() != null, EqEquipment::getCategoryId, query.categoryId())
                .eq(query.status() != null, EqEquipment::getStatus, query.status())
                .and(query.keyword() != null && !query.keyword().isBlank(), item -> item.like(EqEquipment::getEquipCode, query.keyword()).or().like(EqEquipment::getEquipName, query.keyword()))
                .orderByDesc(EqEquipment::getId);
        Page<EqEquipment> page = equipmentMapper.selectPage(new Page<>(query.pageNum(), Math.min(query.pageSize(), 100)), wrapper);
        return new PageResult<>(page.getTotal(), query.pageNum(), query.pageSize(), page.getRecords().stream().map(this::toListVO).toList());
    }

    public EquipmentDetailVO detail(Long id) {
        EqEquipment equipment = getActive(id);
        return new EquipmentDetailVO(equipment.getId(), equipment.getCategoryId(), equipment.getEquipCode(), equipment.getEquipName(), equipment.getModel(), equipment.getManufacturer(), equipment.getLocation(), equipment.getStatus(), EquipmentStatus.fromCode(equipment.getStatus()).getLabel(), equipment.getResponsibleUserId(), equipment.getPurchaseDate(), equipment.getWarrantyExpire(), equipment.getDescription(), paramMapper.selectByEquipId(id).stream().map(this::toParamVO).toList(), statusLogMapper.selectByEquipId(id).stream().map(this::toLogVO).toList());
    }

    @Transactional
    public ParamVO updateParam(Long equipmentId, Long paramId, SaveParamRequest request) {
        getActive(equipmentId);
        EqParam param = paramMapper.selectOne(new LambdaQueryWrapper<EqParam>().eq(EqParam::getId, paramId).eq(EqParam::getEquipId, equipmentId));
        if (param == null) throw new BusinessException(HttpStatus.NOT_FOUND, "设备参数不存在");
        param.setParamName(request.paramName()); param.setParamValue(request.paramValue()); param.setUnit(request.unit()); param.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        paramMapper.updateById(param);
        return toParamVO(param);
    }

    @Transactional
    public EquipmentListVO create(SaveEquipmentRequest request) {
        validateCategory(request.categoryId());
        if (equipmentMapper.selectOne(new LambdaQueryWrapper<EqEquipment>().eq(EqEquipment::getEquipCode, request.equipCode())) != null) throw new BusinessException(HttpStatus.CONFLICT, "设备编码已存在");
        EqEquipment equipment = new EqEquipment();
        apply(equipment, request); equipment.setStatus(EquipmentStatus.IDLE.getCode()); equipment.setDeleted(0); equipment.setVersion(0);
        equipmentMapper.insert(equipment);
        return toListVO(equipment);
    }

    @Transactional
    public EquipmentListVO update(Long id, SaveEquipmentRequest request) {
        EqEquipment equipment = getActive(id);
        validateCategory(request.categoryId());
        EqEquipment sameCode = equipmentMapper.selectOne(new LambdaQueryWrapper<EqEquipment>().eq(EqEquipment::getEquipCode, request.equipCode()).ne(EqEquipment::getId, id));
        if (sameCode != null) throw new BusinessException(HttpStatus.CONFLICT, "设备编码已存在");
        apply(equipment, request); equipmentMapper.updateById(equipment); return toListVO(equipment);
    }

    @Transactional
    public void delete(Long id) {
        EqEquipment equipment = getActive(id);
        if (!Integer.valueOf(EquipmentStatus.SCRAPPED.getCode()).equals(equipment.getStatus())) throw new BusinessException(HttpStatus.CONFLICT, "只有已报废设备才能删除");
        if (equipmentMapper.countActiveInspectionTasks(id) > 0 || equipmentMapper.countActiveRepairOrders(id) > 0) throw new BusinessException(HttpStatus.CONFLICT, "设备存在未闭环巡检任务或维修单据，不能删除");
        equipment.setDeleted(1); equipmentMapper.updateById(equipment);
    }

    public EqEquipment getActive(Long id) {
        EqEquipment equipment = equipmentMapper.selectOne(new LambdaQueryWrapper<EqEquipment>().eq(EqEquipment::getId, id).eq(EqEquipment::getDeleted, 0));
        if (equipment == null) throw new BusinessException(HttpStatus.NOT_FOUND, "设备不存在");
        return equipment;
    }

    private void validateCategory(Long categoryId) {
        if (categoryId != null && categoryMapper.selectOne(new LambdaQueryWrapper<com.wjys.module.equipment.entity.EqCategory>().eq(com.wjys.module.equipment.entity.EqCategory::getId, categoryId).eq(com.wjys.module.equipment.entity.EqCategory::getStatus, 1)) == null) throw new BusinessException(HttpStatus.BAD_REQUEST, "设备分类不存在或已停用");
    }

    private void apply(EqEquipment equipment, SaveEquipmentRequest request) {
        equipment.setCategoryId(request.categoryId()); equipment.setEquipCode(request.equipCode()); equipment.setEquipName(request.equipName()); equipment.setModel(request.model()); equipment.setManufacturer(request.manufacturer()); equipment.setLocation(request.location()); equipment.setResponsibleUserId(request.responsibleUserId()); equipment.setPurchaseDate(request.purchaseDate()); equipment.setWarrantyExpire(request.warrantyExpire()); equipment.setDescription(request.description());
    }

    private EquipmentListVO toListVO(EqEquipment item) { return new EquipmentListVO(item.getId(), item.getCategoryId(), item.getEquipCode(), item.getEquipName(), item.getModel(), item.getLocation(), item.getStatus(), EquipmentStatus.fromCode(item.getStatus()).getLabel()); }
    private ParamVO toParamVO(EqParam item) { return new ParamVO(item.getId(), item.getParamName(), item.getParamValue(), item.getUnit(), item.getSortNo()); }
    private StatusLogVO toLogVO(EqStatusLog item) { return new StatusLogVO(item.getId(), item.getFromStatus(), item.getToStatus(), item.getSourceType(), item.getSourceId(), item.getReason(), item.getCreatedTime()); }
}
