package com.wjys.module.inspection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wjys.common.exception.BusinessException;
import com.wjys.module.equipment.service.EquipmentService;
import com.wjys.module.inspection.dto.CreatePointRequest;
import com.wjys.module.inspection.entity.InsPoint;
import com.wjys.module.inspection.mapper.InsPointMapper;
import com.wjys.module.inspection.vo.PointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {
    private final InsPointMapper pointMapper;
    private final EquipmentService equipmentService;

    public List<PointVO> list() { return pointMapper.selectList(new LambdaQueryWrapper<InsPoint>().eq(InsPoint::getStatus, 1).orderByAsc(InsPoint::getId)).stream().map(this::toVO).toList(); }

    @Transactional
    public PointVO create(CreatePointRequest request) {
        equipmentService.getActive(request.equipId());
        if (pointMapper.selectOne(new LambdaQueryWrapper<InsPoint>().eq(InsPoint::getPointCode, request.pointCode())) != null) throw new BusinessException(HttpStatus.CONFLICT, "点位编码已存在");
        InsPoint point = new InsPoint(); point.setEquipId(request.equipId()); point.setPointCode(request.pointCode()); point.setPointName(request.pointName()); point.setLocation(request.location()); point.setStatus(1); pointMapper.insert(point); return toVO(point);
    }

    private PointVO toVO(InsPoint item) { return new PointVO(item.getId(), item.getEquipId(), item.getPointCode(), item.getPointName(), item.getLocation(), item.getStatus()); }
}
