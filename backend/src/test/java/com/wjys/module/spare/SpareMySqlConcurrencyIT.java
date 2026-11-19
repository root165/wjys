package com.wjys.module.spare;

import com.wjys.module.repair.entity.WoOrder;
import com.wjys.module.repair.entity.WoRepair;
import com.wjys.module.repair.mapper.WoOrderMapper;
import com.wjys.module.repair.mapper.WoRepairMapper;
import com.wjys.module.spare.dto.SpareUseRequest;
import com.wjys.module.spare.entity.WoSpare;
import com.wjys.module.spare.mapper.WoSpareMapper;
import com.wjys.module.spare.mapper.WoSpareRecordMapper;
import com.wjys.module.spare.service.SpareConsumptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class SpareMySqlConcurrencyIT {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36").withDatabaseName("wjys").withUsername("wjys").withPassword("wjys");

    @Autowired private SpareConsumptionService consumptionService;
    @Autowired private WoRepairMapper repairMapper;
    @Autowired private WoOrderMapper orderMapper;
    @Autowired private WoSpareMapper spareMapper;
    @Autowired private WoSpareRecordMapper recordMapper;

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @Test
    void onlyOneConcurrentConsumerCanTakeTheLastSpare() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        WoRepair repair = new WoRepair(); repair.setEquipId(1L); repair.setSourceType("MYSQL_CONCURRENCY"); repair.setSourceId(System.nanoTime()); repair.setDescription("并发库存测试"); repair.setReporterId(1L); repair.setStatus(1); repairMapper.insert(repair);
        WoOrder order = new WoOrder(); order.setRepairId(repair.getId()); order.setOrderNo("WO-MYSQL-" + suffix); order.setAssigneeId(1L); order.setStatus(1); order.setVersion(0); orderMapper.insert(order);
        WoSpare spare = new WoSpare(); spare.setSpareCode("SP-MYSQL-" + suffix); spare.setSpareName("并发测试备件"); spare.setUnit("件"); spare.setStockQty(1); spare.setWarningQty(0); spare.setUnitPrice(new BigDecimal("1.00")); spare.setStatus(1); spare.setVersion(0); spareMapper.insert(spare);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Boolean> first = executor.submit(() -> consume(start, order.getId(), spare.getId()));
        Future<Boolean> second = executor.submit(() -> consume(start, order.getId(), spare.getId()));
        start.countDown();
        int successCount = (first.get() ? 1 : 0) + (second.get() ? 1 : 0);
        executor.shutdownNow();

        assertEquals(1, successCount);
        assertEquals(0, spareMapper.selectById(spare.getId()).getStockQty());
        assertEquals(1, recordMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.wjys.module.spare.entity.WoSpareRecord>().eq(com.wjys.module.spare.entity.WoSpareRecord::getOrderId, order.getId()).eq(com.wjys.module.spare.entity.WoSpareRecord::getDirection, "CONSUME")).size());
    }

    private boolean consume(CountDownLatch start, Long orderId, Long spareId) throws InterruptedException {
        start.await();
        try { consumptionService.consume(orderId, List.of(new SpareUseRequest(spareId, 1, "并发测试")), 1L); return true; }
        catch (RuntimeException exception) { return false; }
    }
}
