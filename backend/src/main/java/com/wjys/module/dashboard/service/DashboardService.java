package com.wjys.module.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wjys.common.exception.BusinessException;
import com.wjys.module.dashboard.entity.StatEquipDaily;
import com.wjys.module.dashboard.mapper.DashboardQueryMapper;
import com.wjys.module.dashboard.mapper.StatEquipDailyMapper;
import com.wjys.module.dashboard.mapper.row.DailyFactRow;
import com.wjys.module.dashboard.mapper.row.EquipmentFaultRow;
import com.wjys.module.dashboard.mapper.row.FaultCountRow;
import com.wjys.module.dashboard.mapper.row.InspectionMetricRow;
import com.wjys.module.dashboard.mapper.row.StatusCountRow;
import com.wjys.module.dashboard.vo.DailyTrendVO;
import com.wjys.module.dashboard.vo.DashboardOverviewVO;
import com.wjys.module.dashboard.vo.EquipmentRankingItemVO;
import com.wjys.module.dashboard.vo.EquipmentRankingVO;
import com.wjys.module.dashboard.vo.FaultParetoItemVO;
import com.wjys.module.dashboard.vo.FaultParetoVO;
import com.wjys.module.dashboard.vo.InspectionSummaryVO;
import com.wjys.support.cache.DashboardCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final String CACHE_PREFIX = "wjys:dashboard:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(1);
    private final DashboardQueryMapper queryMapper;
    private final StatEquipDailyMapper statMapper;
    private final ObjectMapper objectMapper;
    private final DashboardCacheService cacheService;

    public DashboardOverviewVO overview(Long categoryId) {
        String cacheKey = CACHE_PREFIX + "overview:" + (categoryId == null ? "all" : categoryId);
        DashboardOverviewVO cached = readCache(cacheKey, DashboardOverviewVO.class);
        if (cached != null) return cached;
        LocalDate today = LocalDate.now();
        refresh(today);
        List<DailyFactRow> facts = queryMapper.selectDailyFacts(today, startOf(today), startOf(today.plusDays(1)), categoryId);
        long taskTotal = facts.stream().mapToLong(item -> safe(item.getTaskTotal())).sum();
        long taskFinish = facts.stream().mapToLong(item -> safe(item.getTaskFinish())).sum();
        Map<Integer, Long> statusDist = new LinkedHashMap<>();
        for (StatusCountRow row : queryMapper.selectStatusCounts(categoryId)) statusDist.put(row.getStatus(), row.getTotal());
        DashboardOverviewVO result = new DashboardOverviewVO((long) statusDist.values().stream().mapToLong(Long::longValue).sum(), statusDist,
                taskTotal, ratio(taskFinish, taskTotal), queryMapper.countUnhandledRepair(categoryId),
                queryMapper.countFaults(startOf(today), startOf(today.plusDays(1)), categoryId), queryMapper.countSpareAlerts());
        writeCache(cacheKey, result);
        return result;
    }

    public FaultParetoVO faultPareto(LocalDate startDate, LocalDate endDate, String dimension) {
        validateRange(startDate, endDate);
        if (!"equip".equalsIgnoreCase(dimension) && !"faultType".equalsIgnoreCase(dimension)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "故障分析维度无效");
        }
        String cacheKey = CACHE_PREFIX + "pareto:" + startDate + ":" + endDate + ":" + dimension.toLowerCase();
        FaultParetoVO cached = readCache(cacheKey, FaultParetoVO.class);
        if (cached != null) return cached;
        LocalDateTime start = startOf(startDate); LocalDateTime end = startOf(endDate.plusDays(1));
        List<FaultCountRow> rows = "equip".equalsIgnoreCase(dimension)
                ? queryMapper.selectFaultByEquipment(start, end) : queryMapper.selectFaultByType(start, end);
        long total = rows.stream().mapToLong(row -> safe(row.getTotal())).sum();
        if (total == 0) {
            FaultParetoVO empty = new FaultParetoVO(List.of(), 0L); writeCache(cacheKey, empty); return empty;
        }
        long cumulativeCount = 0; long keyCount = 0;
        List<FaultParetoItemVO> result = new ArrayList<>();
        for (FaultCountRow row : rows) {
            long count = safe(row.getTotal()); cumulativeCount += count;
            BigDecimal ratio = ratio(count, total);
            BigDecimal cumulative = ratio(cumulativeCount, total);
            result.add(new FaultParetoItemVO(row.getName(), count, ratio, cumulative));
            if (keyCount == 0 && cumulative.compareTo(new BigDecimal("0.80")) >= 0) keyCount = result.size();
        }
        FaultParetoVO response = new FaultParetoVO(result, keyCount == 0 ? (long) result.size() : keyCount);
        writeCache(cacheKey, response);
        return response;
    }

    public EquipmentRankingVO equipmentRanking(LocalDate startDate, LocalDate endDate, Long categoryId, Integer topN) {
        validateRange(startDate, endDate);
        int limit = topN == null ? 10 : topN;
        if (limit < 1 || limit > 50) throw new BusinessException(HttpStatus.BAD_REQUEST, "topN 必须在 1 到 50 之间");
        String cacheKey = CACHE_PREFIX + "ranking:" + startDate + ":" + endDate + ":" + (categoryId == null ? "all" : categoryId) + ":" + limit;
        EquipmentRankingVO cached = readCache(cacheKey, EquipmentRankingVO.class);
        if (cached != null) return cached;
        LocalDateTime start = startOf(startDate); LocalDateTime end = startOf(endDate.plusDays(1));
        List<EquipmentFaultRow> rows = queryMapper.selectEquipmentFaults(start, end, categoryId);
        List<DailyFactRow> facts = aggregateFacts(startDate, endDate, categoryId);
        Map<Long, FactAggregate> aggregates = aggregateByEquipment(facts);
        long maxFault = aggregates.values().stream().mapToLong(FactAggregate::faultCount).max().orElse(0L);
        long calendarMinutes = Math.max(1L, (endDate.toEpochDay() - startDate.toEpochDay() + 1) * 24L * 60L);
        List<EquipmentRankingItemVO> result = new ArrayList<>();
        for (EquipmentFaultRow row : rows) {
            FactAggregate aggregate = aggregates.getOrDefault(row.getEquipId(), new FactAggregate());
            BigDecimal mtbf = aggregate.faultCount == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(Math.max(0D, (calendarMinutes / 60D) - aggregate.downtimeMinutes / 60D)).divide(BigDecimal.valueOf(aggregate.faultCount), 2, RoundingMode.HALF_UP);
            BigDecimal mttr = aggregate.faultCount == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(aggregate.downtimeMinutes).divide(BigDecimal.valueOf(aggregate.faultCount), 2, RoundingMode.HALF_UP);
            BigDecimal faultScore = maxFault == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(aggregate.faultCount).divide(BigDecimal.valueOf(maxFault), 6, RoundingMode.HALF_UP).min(BigDecimal.ONE);
            BigDecimal downScore = BigDecimal.valueOf(aggregate.downtimeMinutes).divide(BigDecimal.valueOf(calendarMinutes), 6, RoundingMode.HALF_UP).min(BigDecimal.ONE);
            BigDecimal abnormalRate = ratio(aggregate.abnormalCount, aggregate.checkedItems).divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP);
            BigDecimal health = ONE_HUNDRED.subtract(faultScore.multiply(BigDecimal.valueOf(30))).subtract(downScore.multiply(BigDecimal.valueOf(25))).subtract(abnormalRate.multiply(BigDecimal.valueOf(25))).max(BigDecimal.ZERO).min(ONE_HUNDRED).setScale(2, RoundingMode.HALF_UP);
            result.add(new EquipmentRankingItemVO(row.getEquipCode(), row.getEquipName(), row.getFaultCount(), row.getDowntimeMinutes(), mtbf, mttr, health));
        }
        EquipmentRankingVO response = new EquipmentRankingVO(result.stream().sorted(Comparator.comparing(EquipmentRankingItemVO::faultCount).reversed().thenComparing(EquipmentRankingItemVO::downtimeMinutes, Comparator.reverseOrder())).limit(limit).toList());
        writeCache(cacheKey, response);
        return response;
    }

    public InspectionSummaryVO inspectionSummary(LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        String cacheKey = CACHE_PREFIX + "inspection:" + startDate + ":" + endDate;
        InspectionSummaryVO cached = readCache(cacheKey, InspectionSummaryVO.class);
        if (cached != null) return cached;
        InspectionMetricRow metric = queryMapper.selectInspectionMetrics(startDate, endDate);
        InspectionSummaryVO response = new InspectionSummaryVO(ratio(metric.getTaskFinish(), metric.getTaskTotal()), ratio(metric.getTaskTimeout(), metric.getTaskTotal()), ratio(metric.getAbnormalCount(), metric.getCheckedItems()));
        writeCache(cacheKey, response);
        return response;
    }

    public List<DailyTrendVO> trend(LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        String cacheKey = CACHE_PREFIX + "trend:" + startDate + ":" + endDate;
        List<DailyTrendVO> cached = readCache(cacheKey, new TypeReference<>() { });
        if (cached != null) return cached;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) refresh(date);
        List<DailyTrendVO> response = statMapper.selectRange(startDate, endDate).stream().collect(java.util.stream.Collectors.groupingBy(StatEquipDaily::getStatDate, LinkedHashMap::new, java.util.stream.Collectors.toList())).entrySet().stream().map(entry -> {
            List<StatEquipDaily> rows = entry.getValue();
            return new DailyTrendVO(entry.getKey(), rows.stream().mapToInt(item -> safe(item.getTaskTotal())).sum(), rows.stream().mapToInt(item -> safe(item.getTaskFinish())).sum(), rows.stream().mapToInt(item -> safe(item.getAbnormalCount())).sum(), rows.stream().mapToInt(item -> safe(item.getFaultCount())).sum(), rows.stream().mapToInt(item -> safe(item.getDowntimeMinutes())).sum(), rows.stream().map(StatEquipDaily::getHealthScore).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(Math.max(1, rows.size())), 2, RoundingMode.HALF_UP));
        }).toList();
        writeCache(cacheKey, response);
        return response;
    }

    @Transactional
    public void refresh(LocalDate statDate) {
        LocalDateTime start = startOf(statDate); LocalDateTime end = startOf(statDate.plusDays(1));
        List<DailyFactRow> facts = queryMapper.selectDailyFacts(statDate, start, end, null);
        long maxFault = facts.stream().mapToLong(item -> safe(item.getFaultCount())).max().orElse(0L);
        for (DailyFactRow fact : facts) {
            StatEquipDaily row = statMapper.selectOne(new LambdaQueryWrapper<StatEquipDaily>().eq(StatEquipDaily::getStatDate, statDate).eq(StatEquipDaily::getEquipId, fact.getEquipId()));
            if (row == null) { row = new StatEquipDaily(); row.setStatDate(statDate); row.setEquipId(fact.getEquipId()); }
            int downtime = safe(fact.getDowntimeMinutes()); int faults = safe(fact.getFaultCount());
            BigDecimal mtbf = faults == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(Math.max(0D, 24D - downtime / 60D)).divide(BigDecimal.valueOf(faults), 2, RoundingMode.HALF_UP);
            BigDecimal mttr = faults == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(downtime).divide(BigDecimal.valueOf(faults), 2, RoundingMode.HALF_UP);
            BigDecimal faultScore = maxFault == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(faults).divide(BigDecimal.valueOf(maxFault), 6, RoundingMode.HALF_UP).min(BigDecimal.ONE);
            BigDecimal downScore = BigDecimal.valueOf(downtime).divide(BigDecimal.valueOf(1440), 6, RoundingMode.HALF_UP).min(BigDecimal.ONE);
            BigDecimal abnormalRate = ratio(safe(fact.getAbnormalCount()), safe(fact.getCheckedItems())).divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP);
            BigDecimal health = ONE_HUNDRED.subtract(faultScore.multiply(BigDecimal.valueOf(30))).subtract(downScore.multiply(BigDecimal.valueOf(25))).subtract(abnormalRate.multiply(BigDecimal.valueOf(25))).max(BigDecimal.ZERO).min(ONE_HUNDRED).setScale(2, RoundingMode.HALF_UP);
            row.setTaskTotal(safe(fact.getTaskTotal())); row.setTaskFinish(safe(fact.getTaskFinish())); row.setAbnormalCount(safe(fact.getAbnormalCount())); row.setFaultCount(faults); row.setDowntimeMinutes(downtime); row.setMtbfHours(mtbf); row.setMttrMinutes(mttr); row.setHealthScore(health);
            if (row.getId() == null) statMapper.insert(row); else statMapper.updateById(row);
        }
        cacheService.evictByPrefix(CACHE_PREFIX);
    }

    private <T> T readCache(String key, Class<T> type) {
        return cacheService.get(key).map(json -> {
            try { return objectMapper.readValue(json, type); } catch (JsonProcessingException ignored) { return null; }
        }).orElse(null);
    }

    private <T> T readCache(String key, TypeReference<T> type) {
        return cacheService.get(key).map(json -> {
            try { return objectMapper.readValue(json, type); } catch (JsonProcessingException ignored) { return null; }
        }).orElse(null);
    }

    private void writeCache(String key, Object value) {
        try { cacheService.put(key, objectMapper.writeValueAsString(value), CACHE_TTL); } catch (JsonProcessingException ignored) { }
    }

    private List<DailyFactRow> aggregateFacts(LocalDate startDate, LocalDate endDate, Long categoryId) {
        List<DailyFactRow> all = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) all.addAll(queryMapper.selectDailyFacts(date, startOf(date), startOf(date.plusDays(1)), categoryId));
        return all;
    }

    private Map<Long, FactAggregate> aggregateByEquipment(List<DailyFactRow> facts) {
        Map<Long, FactAggregate> result = new LinkedHashMap<>();
        for (DailyFactRow fact : facts) {
            FactAggregate aggregate = result.computeIfAbsent(fact.getEquipId(), ignored -> new FactAggregate());
            aggregate.faultCount += safe(fact.getFaultCount()); aggregate.downtimeMinutes += safe(fact.getDowntimeMinutes()); aggregate.abnormalCount += safe(fact.getAbnormalCount()); aggregate.checkedItems += safe(fact.getCheckedItems());
        }
        return result;
    }

    private void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate) || endDate.toEpochDay() - startDate.toEpochDay() > 365) throw new BusinessException(HttpStatus.BAD_REQUEST, "统计日期范围无效，最长支持 365 天");
    }

    private LocalDateTime startOf(LocalDate date) { return date.atStartOfDay(); }
    private int safe(Integer value) { return value == null ? 0 : value; }
    private long safe(Long value) { return value == null ? 0L : value; }
    private BigDecimal ratio(long numerator, long denominator) { return denominator == 0 ? BigDecimal.ZERO.setScale(2) : BigDecimal.valueOf(numerator * 100D / denominator).setScale(2, RoundingMode.HALF_UP); }
    private static final class FactAggregate {
        private long faultCount;
        private long downtimeMinutes;
        private long abnormalCount;
        private long checkedItems;

        private long faultCount() { return faultCount; }
    }
}
