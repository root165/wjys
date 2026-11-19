package com.wjys.module.equipment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wjys.module.equipment.entity.EqParam;
import com.wjys.module.equipment.mapper.EqParamMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EquipmentControllerTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EqParamMapper paramMapper;

    @Test
    void equipmentCanChangeStatusAndGenerateQrCode() throws Exception {
        String token = loginToken();
        mockMvc.perform(get("/api/equipment/equipments").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(2));
        mockMvc.perform(put("/api/equipment/equipments/1/status").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":3,\"sourceType\":\"test\",\"reason\":\"测试故障\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/equipment/equipments/1").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(3)).andExpect(jsonPath("$.data.statusLogs[0].toStatus").value(3));
        mockMvc.perform(get("/api/equipment/equipments/1/qrcode").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    void duplicateEquipmentCodeReturnsConflict() throws Exception {
        String token = loginToken();
        mockMvc.perform(post("/api/equipment/equipments").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"categoryId\":1,\"equipCode\":\"EQ-001\",\"equipName\":\"重复设备\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void equipmentParameterCanBeEdited() throws Exception {
        EqParam param = new EqParam(); param.setEquipId(1L); param.setParamName("额定温度"); param.setParamValue("80"); param.setUnit("℃"); param.setSortNo(1); paramMapper.insert(param);
        String token = loginToken();
        mockMvc.perform(put("/api/equipment/equipments/1/params/" + param.getId()).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content("{\"paramName\":\"额定温度\",\"paramValue\":\"85\",\"unit\":\"℃\",\"sortNo\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.paramValue").value("85")).andExpect(jsonPath("$.data.sortNo").value(2));
    }

    private String loginToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        return body.path("data").path("accessToken").asText();
    }

    private String bearer(String token) { return "Bearer " + token; }
}
