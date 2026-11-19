package com.wjys.module.repair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wjys.module.inspection.entity.InsAbnormal;
import com.wjys.module.inspection.mapper.InsAbnormalMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RepairControllerTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private InsAbnormalMapper abnormalMapper;

    @Test
    void repairToWorkOrderWorkflowIsAtomicAndAudited() throws Exception {
        InsAbnormal abnormal = new InsAbnormal();
        abnormal.setTaskId(9001L); abnormal.setTaskItemId(9001L); abnormal.setEquipId(1L); abnormal.setPointId(1L); abnormal.setSeverity(1); abnormal.setDescription("轴承温度异常"); abnormal.setStatus(0);
        abnormalMapper.insert(abnormal);
        String token = loginToken();

        String repairResponse = mockMvc.perform(post("/api/repairs/from-abnormal/" + abnormal.getId()).header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(0)).andReturn().getResponse().getContentAsString();
        Long repairId = objectMapper.readTree(repairResponse).path("data").path("id").asLong();

        String orderResponse = mockMvc.perform(put("/api/repairs/" + repairId + "/accept").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(0)).andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(orderResponse).path("data").path("id").asLong();
        mockMvc.perform(put("/api/work-orders/" + orderId + "/dispatch").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"assigneeId\":1,\"remark\":\"安排维修\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(0));
        mockMvc.perform(put("/api/work-orders/" + orderId + "/start").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(1));
        mockMvc.perform(put("/api/work-orders/" + orderId + "/finish").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"finishReason\":\"更换轴承\",\"repairPlan\":\"拆检并更换\",\"finishResult\":\"试运行正常\",\"downtimeMinutes\":10}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(2));
        mockMvc.perform(put("/api/work-orders/" + orderId + "/accept").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"remark\":\"验收通过\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(3));
        mockMvc.perform(get("/api/work-orders/" + orderId + "/logs").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(5)));
        mockMvc.perform(get("/api/repairs?status=3").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)));
    }

    private String loginToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        return body.path("data").path("accessToken").asText();
    }

    private String bearer(String token) { return "Bearer " + token; }
}
