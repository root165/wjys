package com.wjys.module.inspection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wjys.module.inspection.entity.InsAbnormal;
import com.wjys.module.inspection.mapper.InsAbnormalMapper;
import com.wjys.module.inspection.vo.AbnormalVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AbnormalService {
    private final InsAbnormalMapper abnormalMapper;

    public List<AbnormalVO> list(Integer status) { return abnormalMapper.selectList(new LambdaQueryWrapper<InsAbnormal>().eq(status != null, InsAbnormal::getStatus, status).orderByDesc(InsAbnormal::getId)).stream().map(item -> new AbnormalVO(item.getId(), item.getTaskId(), item.getTaskItemId(), item.getEquipId(), item.getPointId(), item.getSeverity(), item.getDescription(), item.getStatus(), item.getCreatedTime())).toList(); }
}
