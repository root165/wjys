package com.wjys.module.inspection.controller;

import com.wjys.common.annotation.OperLog;
import com.wjys.common.model.ApiResponse;
import com.wjys.module.inspection.dto.ChangePlanStatusRequest;
import com.wjys.module.inspection.dto.CreatePlanRequest;
import com.wjys.module.inspection.dto.GenerateTaskRequest;
import com.wjys.module.inspection.dto.UpdatePlanRequest;
import com.wjys.module.inspection.entity.InsTask;
import com.wjys.module.inspection.service.PlanService;
import com.wjys.module.inspection.service.TaskGenerateService;
import com.wjys.module.inspection.vo.PlanVO;
import com.wjys.module.inspection.vo.TaskVO;
import com.wjys.module.inspection.vo.InspectionAssigneeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inspection/plans")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('inspection:plan:view')")
public class PlanController {
    private final PlanService planService;
    private final TaskGenerateService taskGenerateService;
    @GetMapping public ApiResponse<List<PlanVO>> list() { return ApiResponse.success(planService.list()); }
    @GetMapping("/assignees") public ApiResponse<List<InspectionAssigneeVO>> assignees() { return ApiResponse.success(planService.eligibleInspectors()); }
    @PostMapping @PreAuthorize("hasAuthority('inspection:plan:write') or hasRole('system_admin')") @OperLog("创建巡检计划") public ApiResponse<PlanVO> create(@Valid @RequestBody CreatePlanRequest request) { return ApiResponse.success("巡检计划创建成功", planService.create(request)); }
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('inspection:plan:write') or hasRole('system_admin')") @OperLog("编辑巡检计划") public ApiResponse<PlanVO> update(@PathVariable Long id, @Valid @RequestBody UpdatePlanRequest request) { return ApiResponse.success("巡检计划已更新", planService.update(id, request)); }
    @PutMapping("/{id}/status") @PreAuthorize("hasAuthority('inspection:plan:write') or hasRole('system_admin')") @OperLog("变更巡检计划状态") public ApiResponse<PlanVO> changeStatus(@PathVariable Long id, @Valid @RequestBody ChangePlanStatusRequest request) { return ApiResponse.success(planService.changeStatus(id, request)); }
    @PostMapping("/{id}/generate") @PreAuthorize("hasAuthority('inspection:plan:write') or hasRole('system_admin')") @OperLog("生成巡检任务") public ApiResponse<TaskVO> generate(@PathVariable Long id, @Valid @RequestBody GenerateTaskRequest request) { InsTask task = taskGenerateService.generate(id, request.planTime()); String label = task.getAssigneeId() == null ? "待领取" : "已分配"; return ApiResponse.success(new TaskVO(task.getId(), task.getTaskNo(), task.getPlanId(), null, task.getPlanTime(), task.getAssigneeId(), task.getStatus(), label, task.getTotalItems(), task.getAbnormalCount(), task.getFinishTime(), List.of())); }
}
