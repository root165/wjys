package com.wjys.module.dashboard.controller;

import com.wjys.common.model.ApiResponse;
import com.wjys.module.dashboard.service.DashboardExportService;
import com.wjys.module.dashboard.service.DashboardService;
import com.wjys.module.dashboard.vo.DailyTrendVO;
import com.wjys.module.dashboard.vo.DashboardOverviewVO;
import com.wjys.module.dashboard.vo.EquipmentRankingVO;
import com.wjys.module.dashboard.vo.FaultParetoVO;
import com.wjys.module.dashboard.vo.InspectionSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('dashboard:view')")
public class DashboardController {
    private final DashboardService dashboardService;
    private final DashboardExportService dashboardExportService;

    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewVO> overview(@RequestParam(required = false) Long categoryId) {
        return ApiResponse.success(dashboardService.overview(categoryId));
    }

    @GetMapping("/fault/pareto")
    public ApiResponse<FaultParetoVO> faultPareto(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate,
                                                   @RequestParam(defaultValue = "faultType") String dimension) {
        return ApiResponse.success(dashboardService.faultPareto(startDate, endDate, dimension));
    }

    @GetMapping("/equipment/ranking")
    public ApiResponse<EquipmentRankingVO> equipmentRanking(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate,
                                                             @RequestParam(required = false) Long categoryId,
                                                             @RequestParam(defaultValue = "10") Integer topN) {
        return ApiResponse.success(dashboardService.equipmentRanking(startDate, endDate, categoryId, topN));
    }

    @GetMapping("/inspection/summary")
    public ApiResponse<InspectionSummaryVO> inspectionSummary(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        return ApiResponse.success(dashboardService.inspectionSummary(startDate, endDate));
    }

    @GetMapping("/trend")
    public ApiResponse<List<DailyTrendVO>> trend(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        return ApiResponse.success(dashboardService.trend(startDate, endDate));
    }

    @GetMapping("/export/equipment")
    public ResponseEntity<byte[]> exportEquipment(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate,
                                                   @RequestParam(required = false) Long categoryId,
                                                   @RequestParam(defaultValue = "50") Integer topN) {
        byte[] content = dashboardExportService.exportEquipmentRanking(startDate, endDate, categoryId, topN);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''equipment-ranking.xlsx")
                .body(content);
    }
}
