package com.wjys.module.inspection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wjys.common.exception.BusinessException;
import com.wjys.module.inspection.dto.SubmitItemRequest;
import com.wjys.module.inspection.dto.TaskSubmitRequest;
import com.wjys.module.inspection.entity.InsAbnormal;
import com.wjys.module.inspection.entity.InsTask;
import com.wjys.module.inspection.entity.InsTaskItem;
import com.wjys.module.inspection.enums.InspectionTaskStatus;
import com.wjys.module.inspection.mapper.InsAbnormalMapper;
import com.wjys.module.inspection.mapper.InsTaskItemMapper;
import com.wjys.module.inspection.mapper.InsTaskMapper;
import com.wjys.module.inspection.vo.TaskSubmitVO;
import com.wjys.support.file.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskSubmitService {
    private final InsTaskMapper taskMapper;
    private final InsTaskItemMapper taskItemMapper;
    private final InsAbnormalMapper abnormalMapper;
    private final ObjectMapper objectMapper;
    private final TaskService taskService;
    private final FileService fileService;

    @Transactional
    public TaskSubmitVO submit(Long taskId, TaskSubmitRequest request, Long userId) {
        String hash = hash(request);
        InsTask existingBySubmitNo = taskMapper.selectBySubmitNo(request.submitNo());
        if (existingBySubmitNo != null) {
            if (!userId.equals(existingBySubmitNo.getAssigneeId())) throw new BusinessException(HttpStatus.FORBIDDEN, "只有任务执行人可以重放提交");
            if (!hash.equals(existingBySubmitNo.getSubmitHash())) throw new BusinessException(HttpStatus.CONFLICT, "submitNo 已对应其他请求内容");
            return new TaskSubmitVO(existingBySubmitNo.getId(), existingBySubmitNo.getTaskNo(), existingBySubmitNo.getStatus(), existingBySubmitNo.getAbnormalCount(), true);
        }
        InsTask task = taskService.require(taskId);
        if (!userId.equals(task.getAssigneeId())) throw new BusinessException(HttpStatus.FORBIDDEN, "只有领取后的任务执行人可以提交");
        if (!Integer.valueOf(InspectionTaskStatus.RECEIVED.getCode()).equals(task.getStatus()) && !Integer.valueOf(InspectionTaskStatus.OVERDUE.getCode()).equals(task.getStatus())) throw new BusinessException(HttpStatus.CONFLICT, "当前任务状态不可提交");
        List<InsTaskItem> taskItems = taskItemMapper.selectByTaskId(taskId);
        Map<Long, InsTaskItem> itemMap = new HashMap<>(); taskItems.forEach(item -> itemMap.put(item.getId(), item));
        if (new HashSet<>(request.items().stream().map(SubmitItemRequest::taskItemId).toList()).size() != request.items().size() || itemMap.size() != request.items().size() || !itemMap.keySet().containsAll(request.items().stream().map(SubmitItemRequest::taskItemId).toList())) throw new BusinessException(HttpStatus.BAD_REQUEST, "提交明细必须完整且不可重复");
        int abnormalCount = 0;
        for (SubmitItemRequest submitted : request.items()) {
            InsTaskItem item = itemMap.get(submitted.taskItemId());
            if (submitted.checkResult() != 0 && submitted.checkResult() != 1) throw new BusinessException(HttpStatus.BAD_REQUEST, "检查结果无效");
            if (Integer.valueOf(1).equals(item.getRequiredPhoto()) && (submitted.fileIds() == null || submitted.fileIds().isEmpty())) throw new BusinessException(HttpStatus.BAD_REQUEST, "检查项必须上传照片");
            validateCheckValue(item, submitted.checkValue());
            String rangeReason = rangeReason(item, submitted.checkValue());
            boolean abnormal = submitted.checkResult() == 1 || rangeReason != null;
            if (abnormal) {
                abnormalCount++;
                if (rangeReason == null && (submitted.remark() == null || submitted.remark().isBlank())) throw new BusinessException(HttpStatus.BAD_REQUEST, "异常检查项必须填写说明");
            }
            if (submitted.fileIds() != null && !submitted.fileIds().isEmpty()) {
                fileService.bindAll(submitted.fileIds(), "INSPECTION_TASK_ITEM", item.getId());
            }
            item.setCheckResult(abnormal ? 1 : 0); item.setCheckValue(submitted.checkValue()); item.setRemark(submitted.remark()); item.setCheckTime(submitted.checkTime() == null ? LocalDateTime.now() : submitted.checkTime()); item.setPhotoFileIds(submitted.fileIds() == null ? null : submitted.fileIds().stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(null)); item.setAbnormalReason(rangeReason);
            taskItemMapper.updateById(item);
            if (abnormal) { InsAbnormal abnormalRecord = new InsAbnormal(); abnormalRecord.setTaskId(taskId); abnormalRecord.setTaskItemId(item.getId()); abnormalRecord.setEquipId(item.getEquipId()); abnormalRecord.setPointId(item.getPointId()); abnormalRecord.setSeverity(1); abnormalRecord.setDescription(rangeReason == null ? submitted.remark() : rangeReason); abnormalRecord.setStatus(0); abnormalMapper.insert(abnormalRecord); }
        }
        int affected = taskMapper.update(null, new LambdaUpdateWrapper<InsTask>().eq(InsTask::getId, taskId).in(InsTask::getStatus, InspectionTaskStatus.RECEIVED.getCode(), InspectionTaskStatus.OVERDUE.getCode()).set(InsTask::getStatus, InspectionTaskStatus.COMPLETED.getCode()).set(InsTask::getSubmitNo, request.submitNo()).set(InsTask::getSubmitHash, hash).set(InsTask::getClientSubmitTime, request.clientSubmitTime()).set(InsTask::getOfflineFlag, Boolean.TRUE.equals(request.offlineFlag()) ? 1 : 0).set(InsTask::getFinishTime, LocalDateTime.now()).set(InsTask::getAbnormalCount, abnormalCount));
        if (affected != 1) throw new BusinessException(HttpStatus.CONFLICT, "任务状态已被其他操作更新");
        return new TaskSubmitVO(taskId, task.getTaskNo(), InspectionTaskStatus.COMPLETED.getCode(), abnormalCount, false);
    }

    private void validateCheckValue(InsTaskItem item, String value) {
        String type = item.getValueType() == null ? "TEXT" : item.getValueType().trim().toUpperCase();
        if (Set.of("NUMBER", "TEXT", "SELECT").contains(type) && (value == null || value.isBlank())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "检查项“" + item.getItemName() + "”必须填写检查值");
        }
        if ("NUMBER".equals(type)) {
            try { new BigDecimal(value.trim()); }
            catch (NumberFormatException exception) { throw new BusinessException(HttpStatus.BAD_REQUEST, "检查项“" + item.getItemName() + "”必须填写数字"); }
        }
        if ("SELECT".equals(type)) {
            if (!allowedOptions(item.getOptionsJson()).contains(value.trim())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "检查项“" + item.getItemName() + "”的选择值无效");
            }
        }
    }

    private Set<String> allowedOptions(String optionsJson) {
        try {
            var node = objectMapper.readTree(optionsJson == null || optionsJson.isBlank() ? "[]" : optionsJson);
            if (!node.isArray()) return Set.of();
            Set<String> options = new HashSet<>();
            node.forEach(option -> {
                if (option.isTextual()) options.add(option.asText());
                else if (option.isObject()) {
                    if (option.hasNonNull("label")) options.add(option.get("label").asText());
                    if (option.hasNonNull("value")) options.add(option.get("value").asText());
                }
            });
            return options;
        } catch (Exception exception) {
            return Set.of();
        }
    }

    private String rangeReason(InsTaskItem item, String value) {
        if (!"NUMBER".equalsIgnoreCase(item.getValueType()) || value == null || value.isBlank()) return null;
        try {
            BigDecimal number = new BigDecimal(value);
            if (item.getMinValue() != null && number.compareTo(item.getMinValue()) < 0) return "数值低于下限 " + item.getMinValue();
            if (item.getMaxValue() != null && number.compareTo(item.getMaxValue()) > 0) return "数值超过上限 " + item.getMaxValue();
            return null;
        } catch (NumberFormatException exception) { return "数值格式无效"; }
    }

    private String hash(TaskSubmitRequest request) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8))); } catch (Exception exception) { throw new IllegalStateException("提交摘要生成失败", exception); }
    }
}
