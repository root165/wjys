package com.wjys.module.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.hamcrest.Matchers.greaterThan;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemFoundationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void systemAdminCanReadFoundationResourcesAndWriteAuditLog() throws Exception {
        String token = loginToken();
        mockMvc.perform(get("/api/system/users?pageNum=1&pageSize=20&total=0&keyword=")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray());
        mockMvc.perform(get("/api/system/roles").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].roleCode").value("system_admin"));
        mockMvc.perform(get("/api/system/depts").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].deptName").value("运维中心"));
        mockMvc.perform(get("/api/system/dicts/status").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(post("/api/system/users")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"field-user\",\"password\":\"Password1\",\"nickname\":\"现场用户\",\"deptId\":1}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/system/logs").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total", greaterThan(0)));
    }

    @Test
    void requestTraceIdIsReusedByResponseAndUnauthenticatedError() throws Exception {
        String token = loginToken();
        mockMvc.perform(get("/api/system/roles")
                        .header("Authorization", bearer(token))
                        .header("X-Trace-Id", "p7-trace-001"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "p7-trace-001"))
                .andExpect(jsonPath("$.traceId").value("p7-trace-001"));

        mockMvc.perform(get("/api/system/roles")
                        .header("X-Trace-Id", "p7-unauthenticated-001"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Trace-Id", "p7-unauthenticated-001"))
                .andExpect(jsonPath("$.traceId").value("p7-unauthenticated-001"));
    }

    private String loginToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        return body.path("data").path("accessToken").asText();
    }

    private String bearer(String token) { return "Bearer " + token; }
}
