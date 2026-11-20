package com.wjys.module.mobile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wjys.support.message.MessageService;
import com.wjys.module.system.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MobileSyncControllerTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MessageService messageService;
    @Autowired private SysUserMapper userMapper;

    @Test
    void pullReturnsCursorAndPushKeepsItemLevelResult() throws Exception {
        String token = loginToken("inspector");
        Long inspectorId = userMapper.selectByUsername("inspector").getId();
        var message = messageService.create(inspectorId, "MOBILE_TEST", "移动提醒", "请处理现场任务", "M08_TEST", 1L);

        String pullResponse = mockMvc.perform(get("/api/mobile/sync/pull").header("Authorization", bearer(token)).param("modules", "messages"))
                .andExpect(status().isOk()).andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store")).andExpect(jsonPath("$.data.cursor", notNullValue())).andExpect(jsonPath("$.data.tasks.length()").value(0)).andExpect(jsonPath("$.data.messages.length()", greaterThanOrEqualTo(1))).andReturn().getResponse().getContentAsString();
        String cursor = objectMapper.readTree(pullResponse).path("data").path("cursor").asText();
        messageService.create(inspectorId, "MOBILE_TEST", "移动增量提醒", "这是游标之后的新消息", "M08_TEST", 2L);
        mockMvc.perform(get("/api/mobile/sync/pull").header("Authorization", bearer(token)).param("cursor", cursor).param("modules", "messages"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.messages.length()").value(1)).andExpect(jsonPath("$.data.messages[0].title").value("移动增量提醒"));
        mockMvc.perform(put("/api/mobile/messages/" + message.getId() + "/read").header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        String submitNo = "mobile-invalid-" + UUID.randomUUID();
        String body = "{\"items\":[{\"taskId\":999999,\"payload\":{\"submitNo\":\"" + submitNo + "\",\"items\":[{\"taskItemId\":1,\"checkResult\":0}]}}]}";
        String response = mockMvc.perform(post("/api/mobile/sync/push").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.results[0].status").value("rejected")).andReturn().getResponse().getContentAsString();
        JsonNode result = objectMapper.readTree(response).path("data").path("results").get(0);
        org.junit.jupiter.api.Assertions.assertEquals("404", result.path("errorCode").asText());
    }

    @Test
    void adminCannotUseInspectorMobileSync() throws Exception {
        String token = loginToken("admin");
        mockMvc.perform(get("/api/mobile/sync/pull").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/mobile/messages/1/read").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    private String loginToken(String username) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"" + username + "\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("accessToken").asText();
    }

    private String bearer(String token) { return "Bearer " + token; }
}
