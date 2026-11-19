package com.wjys.module.maintenance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wjys.module.equipment.entity.EqEquipment;
import com.wjys.module.equipment.enums.EquipmentStatus;
import com.wjys.module.equipment.mapper.EqEquipmentMapper;
import com.wjys.module.maintenance.entity.MtPlan;
import com.wjys.module.maintenance.entity.MtRecord;
import com.wjys.module.maintenance.mapper.MtPlanMapper;
import com.wjys.module.maintenance.mapper.MtRecordMapper;
import com.wjys.module.repair.entity.WoRepair;
import com.wjys.module.repair.mapper.WoRepairMapper;
import com.wjys.module.system.entity.SysFile;
import com.wjys.module.system.mapper.SysFileMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaintenanceControllerTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SysFileMapper fileMapper;
    @Autowired private MtPlanMapper planMapper;
    @Autowired private MtRecordMapper recordMapper;
    @Autowired private EqEquipmentMapper equipmentMapper;
    @Autowired private WoRepairMapper repairMapper;

    @Test
    void maintenanceRecordAuditsArchivesAndCanCreateRepairFromAbnormalResult() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        LocalDate maintainDate = LocalDate.of(2026, 11, 17);
        String token = loginToken();
        String planResponse = mockMvc.perform(post("/api/maintenance/plans").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planCode\":\"MT-" + suffix + "\",\"planName\":\"空压机月度保养\",\"equipId\":2,\"maintainType\":1,\"cycleType\":3,\"cycleValue\":1,\"content\":\"检查滤芯与压力\",\"nextDate\":\"2026-11-01\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(1)).andReturn().getResponse().getContentAsString();
        Long planId = objectMapper.readTree(planResponse).path("data").path("id").asLong();

        SysFile file = new SysFile();
        file.setObjectKey("test/maintenance-" + suffix + ".jpg"); file.setOriginalName("现场.jpg"); file.setChecksum(suffix); file.setBindStatus(0);
        fileMapper.insert(file);
        String recordResponse = mockMvc.perform(post("/api/maintenance/records").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":" + planId + ",\"equipId\":2,\"maintainDate\":\"" + maintainDate + "\",\"maintainContent\":\"更换滤芯并完成压力测试，发现异常\",\"maintainResult\":1,\"cost\":120.50,\"downtimeMinutes\":30,\"fileIds\":[" + file.getId() + "],\"remark\":\"需要关注压力波动\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(0)).andReturn().getResponse().getContentAsString();
        Long recordId = objectMapper.readTree(recordResponse).path("data").path("id").asLong();
        assertEquals(1, fileMapper.selectById(file.getId()).getBindStatus());
        assertEquals(EquipmentStatus.MAINTENANCE.getCode(), equipmentMapper.selectById(2L).getStatus());

        mockMvc.perform(put("/api/maintenance/records/" + recordId + "/audit").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pass\":true,\"remark\":\"记录完整\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(1));
        MtPlan archivedPlan = planMapper.selectById(planId);
        assertEquals(LocalDate.of(2026, 12, 17), archivedPlan.getNextDate());
        assertEquals(EquipmentStatus.IDLE.getCode(), equipmentMapper.selectById(2L).getStatus());
        assertEquals(1, recordMapper.selectById(recordId).getStatus());

        mockMvc.perform(put("/api/maintenance/records/" + recordId + "/audit").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pass\":true}"))
                .andExpect(status().isConflict());
        String repairResponse = mockMvc.perform(post("/api/maintenance/records/" + recordId + "/to-repair").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.sourceType").value("MAINTENANCE_RECORD")).andReturn().getResponse().getContentAsString();
        Long repairId = objectMapper.readTree(repairResponse).path("data").path("id").asLong();
        WoRepair repair = repairMapper.selectById(repairId);
        assertNotNull(repair);
        assertEquals(recordId, repair.getSourceId());
        mockMvc.perform(post("/api/maintenance/records/" + recordId + "/to-repair").header("Authorization", bearer(token)))
                .andExpect(status().isConflict());
    }

    @Test
    void duplicateRecordForPlanAndDateReturnsConflict() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        LocalDate maintainDate = LocalDate.of(2026, 11, 18);
        String token = loginToken();
        String planResponse = mockMvc.perform(post("/api/maintenance/plans").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planCode\":\"MT-" + suffix + "\",\"planName\":\"日常保养\",\"equipId\":2,\"maintainType\":0,\"cycleType\":1,\"cycleValue\":1,\"nextDate\":\"2026-11-18\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Long planId = objectMapper.readTree(planResponse).path("data").path("id").asLong();
        SysFile firstFile = pendingFile(suffix + "-1");
        submitRecord(token, planId, maintainDate, firstFile.getId());
        SysFile secondFile = pendingFile(suffix + "-2");
        mockMvc.perform(post("/api/maintenance/records").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":" + planId + ",\"equipId\":2,\"maintainDate\":\"" + maintainDate + "\",\"maintainContent\":\"第二次重复提交保养内容\",\"maintainResult\":0,\"fileIds\":[" + secondFile.getId() + "]}"))
                .andExpect(status().isConflict());
    }

    private SysFile pendingFile(String suffix) {
        SysFile file = new SysFile(); file.setObjectKey("test/" + suffix + ".jpg"); file.setOriginalName("现场.jpg"); file.setChecksum(suffix); file.setBindStatus(0); fileMapper.insert(file); return file;
    }

    private void submitRecord(String token, Long planId, LocalDate date, Long fileId) throws Exception {
        mockMvc.perform(post("/api/maintenance/records").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":" + planId + ",\"equipId\":2,\"maintainDate\":\"" + date + "\",\"maintainContent\":\"完成日常润滑并检查紧固件\",\"maintainResult\":0,\"fileIds\":[" + fileId + "]}"))
                .andExpect(status().isOk());
    }

    private String loginToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        return body.path("data").path("accessToken").asText();
    }

    private String bearer(String token) { return "Bearer " + token; }
}
