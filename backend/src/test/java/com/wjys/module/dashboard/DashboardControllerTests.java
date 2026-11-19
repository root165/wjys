package com.wjys.module.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wjys.module.dashboard.entity.StatEquipDaily;
import com.wjys.module.dashboard.mapper.StatEquipDailyMapper;
import com.wjys.module.repair.entity.WoRepair;
import com.wjys.module.repair.mapper.WoRepairMapper;
import com.wjys.support.message.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private WoRepairMapper repairMapper;
    @Autowired private StatEquipDailyMapper statMapper;
    @Autowired private MessageService messageService;

    @Test
    void dashboardAggregatesFactsAndMessageCenterTracksReadState() throws Exception {
        String token = loginToken();
        WoRepair repair = new WoRepair(); repair.setEquipId(1L); repair.setSourceType("M07_TEST"); repair.setSourceId(Math.abs(UUID.randomUUID().getMostSignificantBits())); repair.setDescription("压力异常"); repair.setReporterId(1L); repair.setStatus(0); repairMapper.insert(repair);

        mockMvc.perform(get("/api/dashboard/overview").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.equipTotal").value(2)).andExpect(jsonPath("$.data.unhandledRepair").isNumber()).andExpect(jsonPath("$.data.faultToday").isNumber());
        StatEquipDaily daily = statMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StatEquipDaily>().eq(StatEquipDaily::getStatDate, LocalDate.now()).eq(StatEquipDaily::getEquipId, 1L));
        assertNotNull(daily);

        mockMvc.perform(get("/api/dashboard/fault/pareto").header("Authorization", bearer(token)).param("startDate", "2026-01-01").param("endDate", "2026-12-31").param("dimension", "faultType"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.list.length()").value(1)).andExpect(jsonPath("$.data.list[0].count").value(1));
        mockMvc.perform(get("/api/dashboard/equipment/ranking").header("Authorization", bearer(token)).param("startDate", "2026-01-01").param("endDate", "2026-12-31").param("topN", "5"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.list.length()").value(1)).andExpect(jsonPath("$.data.list[0].equipCode").value("EQ-001"));
        mockMvc.perform(get("/api/dashboard/export/equipment").header("Authorization", bearer(token)).param("startDate", "2026-01-01").param("endDate", "2026-12-31").param("topN", "5"))
                .andExpect(status().isOk()).andExpect(result -> assertTrue(result.getResponse().getContentType().contains("spreadsheetml.sheet")))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsByteArray().length > 100));
        mockMvc.perform(get("/api/dashboard/inspection/summary").header("Authorization", bearer(token)).param("startDate", "2026-01-01").param("endDate", "2026-12-31"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.finishRate").isNumber());
        mockMvc.perform(get("/api/dashboard/fault/pareto").header("Authorization", bearer(token)).param("startDate", "2024-01-01").param("endDate", "2026-12-31"))
                .andExpect(status().isBadRequest());

        messageService.create(1L, "TEST", "统计提醒", "有一条未读消息", "M07_TEST", repair.getId());
        mockMvc.perform(get("/api/messages/unread-count").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(1));
        String messages = mockMvc.perform(get("/api/messages").header("Authorization", bearer(token)).param("readFlag", "0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1)).andReturn().getResponse().getContentAsString();
        Long messageId = objectMapper.readTree(messages).path("data").get(0).path("id").asLong();
        mockMvc.perform(put("/api/messages/" + messageId + "/read").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/messages/unread-count").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(0));
        assertFalse(messageId == 0L);
    }

    private String loginToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        return body.path("data").path("accessToken").asText();
    }

    private String bearer(String token) { return "Bearer " + token; }
}
