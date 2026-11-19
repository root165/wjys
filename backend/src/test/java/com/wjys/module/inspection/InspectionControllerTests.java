package com.wjys.module.inspection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wjys.module.system.entity.SysFile;
import com.wjys.module.system.mapper.SysFileMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InspectionControllerTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SysFileMapper sysFileMapper;

    @Test
    void taskGenerationReceiveSubmitAndIdempotencyWork() throws Exception {
        String adminToken = loginToken("admin");
        String inspectorToken = loginToken("inspector");
        mockMvc.perform(get("/api/inspection/plans/assignees").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[?(@.username == 'inspector')].id").isNotEmpty());
        LocalDate today = LocalDate.now();
        String generateResponse = mockMvc.perform(post("/api/inspection/plans/1/generate").header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON).content("{\"planTime\":\"" + today + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(0)).andExpect(jsonPath("$.data.assigneeId").doesNotExist()).andReturn().getResponse().getContentAsString();
        Long taskId = objectMapper.readTree(generateResponse).path("data").path("id").asLong();
        String detailResponse = mockMvc.perform(get("/api/inspection/tasks/" + taskId).header("Authorization", bearer(adminToken))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Long taskItemId = objectMapper.readTree(detailResponse).path("data").path("items").get(0).path("id").asLong();

        mockMvc.perform(put("/api/inspection/tasks/" + taskId + "/receive").header("Authorization", bearer(adminToken))).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/inspection/tasks/" + taskId + "/submit").header("Authorization", bearer(inspectorToken)).contentType(MediaType.APPLICATION_JSON).content("{\"submitNo\":\"before-receive-" + taskId + "\",\"items\":[{\"taskItemId\":" + taskItemId + ",\"checkResult\":0,\"checkValue\":\"40\"}]}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/inspection/tasks/" + taskId + "/receive").header("Authorization", bearer(inspectorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(1));
        String uploadResponse = mockMvc.perform(multipart("/api/files/upload").file(new MockMultipartFile("file", "inspection.jpg", "image/jpeg", new byte[]{1, 2, 3})).header("Authorization", bearer(inspectorToken)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Long fileId = objectMapper.readTree(uploadResponse).path("data").path("fileId").asLong();
        String submitBody = "{\"submitNo\":\"submit-" + taskId + "\",\"offlineFlag\":false,\"items\":[{\"taskItemId\":" + taskItemId + ",\"checkResult\":0,\"checkValue\":\"40\",\"remark\":\"正常\",\"fileIds\":[" + fileId + "]}]}";
        mockMvc.perform(post("/api/inspection/tasks/" + taskId + "/submit").header("Authorization", bearer(inspectorToken)).contentType(MediaType.APPLICATION_JSON).content(submitBody)).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(2)).andExpect(jsonPath("$.data.abnormalCount").value(0));
        SysFile boundFile = sysFileMapper.selectById(fileId);
        org.junit.jupiter.api.Assertions.assertEquals(1, boundFile.getBindStatus());
        org.junit.jupiter.api.Assertions.assertEquals("INSPECTION_TASK_ITEM", boundFile.getBizType());
        mockMvc.perform(post("/api/inspection/tasks/" + taskId + "/submit").header("Authorization", bearer(inspectorToken)).contentType(MediaType.APPLICATION_JSON).content(submitBody)).andExpect(status().isOk()).andExpect(jsonPath("$.data.idempotentReplay").value(true));

        String tomorrowResponse = mockMvc.perform(post("/api/inspection/plans/1/generate").header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON).content("{\"planTime\":\"" + today.plusDays(1) + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Long tomorrowTaskId = objectMapper.readTree(tomorrowResponse).path("data").path("id").asLong();
        Long tomorrowItemId = objectMapper.readTree(mockMvc.perform(get("/api/inspection/tasks/" + tomorrowTaskId).header("Authorization", bearer(adminToken))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").path("items").get(0).path("id").asLong();
        mockMvc.perform(put("/api/inspection/tasks/" + tomorrowTaskId + "/receive").header("Authorization", bearer(inspectorToken))).andExpect(status().isOk());
        mockMvc.perform(post("/api/inspection/tasks/" + tomorrowTaskId + "/submit").header("Authorization", bearer(inspectorToken)).contentType(MediaType.APPLICATION_JSON).content("{\"submitNo\":\"submit-" + tomorrowTaskId + "\",\"items\":[{\"taskItemId\":" + tomorrowItemId + ",\"checkResult\":0,\"checkValue\":\"99\"}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.abnormalCount").value(1));
        String abnormalResponse = mockMvc.perform(get("/api/inspection/abnormals?status=0").header("Authorization", bearer(inspectorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1))).andReturn().getResponse().getContentAsString();
        Long abnormalId = objectMapper.readTree(abnormalResponse).path("data").get(0).path("id").asLong();
        mockMvc.perform(post("/api/inspection/abnormals/" + abnormalId + "/to-repair").header("Authorization", bearer(inspectorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.data.sourceType").value("INSPECTION_ABNORMAL"));
        mockMvc.perform(post("/api/inspection/abnormals/" + abnormalId + "/to-repair").header("Authorization", bearer(inspectorToken))).andExpect(status().isConflict());
    }

    private String loginToken(String username) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"" + username + "\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        return body.path("data").path("accessToken").asText();
    }
    private String bearer(String token) { return "Bearer " + token; }
}
