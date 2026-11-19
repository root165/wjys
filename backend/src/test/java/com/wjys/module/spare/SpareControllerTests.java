package com.wjys.module.spare;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wjys.module.repair.entity.WoOrder;
import com.wjys.module.repair.entity.WoRepair;
import com.wjys.module.repair.mapper.WoOrderMapper;
import com.wjys.module.repair.mapper.WoRepairMapper;
import com.wjys.module.spare.entity.WoSpare;
import com.wjys.module.spare.mapper.WoSpareMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpareControllerTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private WoRepairMapper repairMapper;
    @Autowired private WoOrderMapper orderMapper;
    @Autowired private WoSpareMapper spareMapper;

    @Test
    void stockConsumptionUsesConditionalDeductionAndSupportsReverse() throws Exception {
        WoRepair repair = new WoRepair(); repair.setEquipId(1L); repair.setSourceType("M06_TEST"); repair.setSourceId(70001L); repair.setDescription("备件测试报修"); repair.setReporterId(1L); repair.setStatus(1); repairMapper.insert(repair);
        WoOrder order = new WoOrder(); order.setRepairId(repair.getId()); order.setOrderNo("WO-M06-TEST"); order.setAssigneeId(1L); order.setStatus(1); order.setVersion(0); orderMapper.insert(order);
        String token = loginToken();
        String spareResponse = mockMvc.perform(post("/api/spares").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"spareCode\":\"SP-M06-001\",\"spareName\":\"测试轴承\",\"unit\":\"件\",\"stockQty\":5,\"warningQty\":1,\"unitPrice\":12.50,\"status\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.stockQty").value(5)).andReturn().getResponse().getContentAsString();
        Long spareId = objectMapper.readTree(spareResponse).path("data").path("id").asLong();
        String consumeResponse = mockMvc.perform(post("/api/work-orders/" + order.getId() + "/spares/consume").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"items\":[{\"spareId\":" + spareId + ",\"quantity\":3,\"remark\":\"更换\"}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].amount").value(37.50)).andReturn().getResponse().getContentAsString();
        Long recordId = objectMapper.readTree(consumeResponse).path("data").get(0).path("id").asLong();
        WoSpare consumed = spareMapper.selectById(spareId);
        WoOrder consumedOrder = orderMapper.selectById(order.getId());
        org.junit.jupiter.api.Assertions.assertEquals(2, consumed.getStockQty());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("37.50"), consumedOrder.getSpareCost());
        mockMvc.perform(post("/api/work-orders/" + order.getId() + "/spares/consume").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"items\":[{\"spareId\":" + spareId + ",\"quantity\":3}]}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/work-orders/spare-records/" + recordId + "/reverse").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"remark\":\"撤销\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.direction").value("REVERSE"));
        org.junit.jupiter.api.Assertions.assertEquals(5, spareMapper.selectById(spareId).getStockQty());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("0.00"), orderMapper.selectById(order.getId()).getSpareCost());
        mockMvc.perform(post("/api/work-orders/spare-records/" + recordId + "/reverse").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"remark\":\"重复撤销\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/spares/alerts").header("Authorization", bearer(token))).andExpect(status().isOk());
    }

    private String loginToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        return body.path("data").path("accessToken").asText();
    }

    private String bearer(String token) { return "Bearer " + token; }
}
