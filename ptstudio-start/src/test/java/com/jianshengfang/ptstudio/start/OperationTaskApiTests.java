package com.jianshengfang.ptstudio.start;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("regression")
class OperationTaskApiTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSaveRuleGenerateTasksAndTransitStatus() throws Exception {
        createMember("任务会员A", "13600008101");
        createMember("任务会员B", "13600008102");

        String saveRuleReq = """
                {
                  "triggerType": "DAILY_MEMBER_FOLLOWUP",
                  "priority": "HIGH",
                  "ownerRole": "STORE_MANAGER",
                  "titleTemplate": "回访{memberName}",
                  "generateLimit": 2,
                  "operatorUserId": 4101
                }
                """;
        mockMvc.perform(put("/api/ops/tasks/rules")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveRuleReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.triggerType").value("DAILY_MEMBER_FOLLOWUP"));

        String operatorReq = """
                {
                  "operatorUserId": 4101
                }
                """;
        String generateResp = mockMvc.perform(post("/api/ops/tasks/generate")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(operatorReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedCount").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String taskNo = objectMapper.readTree(generateResp).path("tasks").get(0).path("taskNo").asText();

        mockMvc.perform(get("/api/ops/tasks")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("TODO"));

        mockMvc.perform(post("/api/ops/tasks/{taskNo}/start", taskNo)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(operatorReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOING"));

        mockMvc.perform(post("/api/ops/tasks/{taskNo}/complete", taskNo)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(operatorReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    private void createMember(String name, String mobile) throws Exception {
        String request = """
                {
                  "name": "%s",
                  "mobile": "%s",
                  "levelTag": "STANDARD"
                }
                """.formatted(name, mobile);
        mockMvc.perform(post("/api/members")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk());
    }
}
