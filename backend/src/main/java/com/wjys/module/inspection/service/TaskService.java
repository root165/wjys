package com.wjys.module.inspection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wjys.common.exception.BusinessException;
import com.wjys.common.model.PageResult;
import com.wjys.module.inspection.dto.TaskQuery;
import com.wjys.module.inspection.entity.InsTask;
import com.wjys.module.inspection.entity.InsTaskItem;
import com.wjys.module.inspection.enums.InspectionTaskStatus;
import com.wjys.module.inspection.mapper.InsTaskItemMapper;
import com.wjys.module.inspection.mapper.InsTaskMapper;
import com.wjys.module.inspection.mapper.InsPlanMapper;
import com.wjys.module.inspection.vo.TaskItemVO;
import com.wjys.module.inspection.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final InsTaskMapper taskMapper;
    private final InsTaskItemMapper taskItemMapper;
    private final InsPlanMapper planMapper;

    public PageResult<TaskVO> page(TaskQuery query) {
        Page<InsTask> page = taskMapper.selectPage(new Page<>(query.pageNum(), Math.min(query.pageSize(), 100)), new LambdaQueryWrapper<InsTask>().eq(query.status() != null, InsTask::getStatus, query.status()).orderByDesc(InsTask::getPlanTime, InsTask::getId));
        return new PageResult<>(page.getTotal(), query.pageNum(), query.pageSize(), page.getRecords().stream().map(task -> toVO(task, false)).toList());
    }

    public TaskVO detail(Long id) { return toVO(require(id), true); }

    public List<TaskVO> assignedTasks(Long userId) {
        return taskMapper.selectList(new LambdaQueryWrapper<InsTask>()
                        .and(wrapper -> wrapper.eq(InsTask::getAssigneeId, userId).or().isNull(InsTask::getAssigneeId))
                        .in(InsTask::getStatus, InspectionTaskStatus.PENDING.getCode(), InspectionTaskStatus.RECEIVED.getCode(), InspectionTaskStatus.OVERDUE.getCode())
                        .orderByAsc(InsTask::getPlanTime).orderByAsc(InsTask::getId))
                .stream().map(task -> toVO(task, true)).toList();
    }

    @Transactional
    public TaskVO receive(Long id, Long userId) {
        InsTask task = require(id);
        if (task.getAssigneeId() != null && !task.getAssigneeId().equals(userId)) throw new BusinessException(HttpStatus.FORBIDDEN, "该任务已分配给其他人员");
        int affected = taskMapper.update(null, new LambdaUpdateWrapper<InsTask>().eq(InsTask::getId, id).in(InsTask::getStatus, InspectionTaskStatus.PENDING.getCode(), InspectionTaskStatus.OVERDUE.getCode()).set(InsTask::getStatus, InspectionTaskStatus.RECEIVED.getCode()).set(InsTask::getAssigneeId, userId));
        if (affected != 1) throw new BusinessException(HttpStatus.CONFLICT, "任务状态已被其他操作更新");
        return detail(id);
    }

    InsTask require(Long id) { InsTask task = taskMapper.selectById(id); if (task == null) throw new BusinessException(HttpStatus.NOT_FOUND, "巡检任务不存在"); return task; }
    TaskVO toVO(InsTask task, boolean includeItems) {
        List<TaskItemVO> items = includeItems ? taskItemMapper.selectByTaskId(task.getId()).stream().map(this::toItemVO).toList() : List.of();
        String label = switch (task.getStatus()) { case 0 -> "待领取"; case 1 -> "执行中"; case 2 -> "已完成"; case 3 -> "已逾期"; default -> "未知"; };
        var plan = planMapper.selectById(task.getPlanId());
        return new TaskVO(task.getId(), task.getTaskNo(), task.getPlanId(), plan == null ? null : plan.getPlanName(), task.getPlanTime(), task.getAssigneeId(), task.getStatus(), label, task.getTotalItems(), task.getAbnormalCount(), task.getFinishTime(), items);
    }
    private TaskItemVO toItemVO(InsTaskItem item) { return new TaskItemVO(item.getId(), item.getPointId(), item.getItemId(), item.getEquipId(), item.getEquipCode(), item.getPointCode(), item.getItemCode(), item.getItemName(), item.getValueType(), item.getCheckMethod(), item.getUnit(), item.getMinValue(), item.getMaxValue(), item.getOptionsJson(), item.getRequiredPhoto(), item.getCheckResult(), item.getCheckValue(), item.getRemark(), item.getAbnormalReason()); }
}
