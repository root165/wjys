package com.wjys.module.dashboard.service;

import com.alibaba.excel.EasyExcel;
import com.wjys.module.dashboard.vo.DashboardExportRow;
import com.wjys.module.dashboard.vo.EquipmentRankingItemVO;
import com.wjys.module.dashboard.vo.EquipmentRankingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardExportService {
    private final DashboardService dashboardService;

    public byte[] exportEquipmentRanking(LocalDate startDate, LocalDate endDate, Long categoryId, Integer topN) {
        EquipmentRankingVO ranking = dashboardService.equipmentRanking(startDate, endDate, categoryId, topN);
        List<DashboardExportRow> rows = ranking.list().stream().map(this::toRow).toList();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            EasyExcel.write(output, DashboardExportRow.class).autoCloseStream(false).sheet("设备排行").doWrite(rows);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("设备统计导出失败", exception);
        }
    }

    private DashboardExportRow toRow(EquipmentRankingItemVO item) {
        DashboardExportRow row = new DashboardExportRow();
        row.setEquipCode(item.equipCode()); row.setEquipName(item.equipName()); row.setFaultCount(item.faultCount());
        row.setDowntimeMinutes(item.downtimeMinutes()); row.setMtbf(item.mtbf()); row.setMttr(item.mttr()); row.setHealthScore(item.healthScore());
        return row;
    }
}
