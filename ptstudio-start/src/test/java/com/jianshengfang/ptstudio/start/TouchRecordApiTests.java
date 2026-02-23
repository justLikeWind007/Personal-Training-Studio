package com.jianshengfang.ptstudio.start;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("regression")
class TouchRecordApiTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateListAndExportTouchRecords() throws Exception {
        long memberId = createMember("触达会员", "13600008301");
        String taskNo = createTaskAndGetTaskNo();

        String createReq = """
                {
                  "memberId": %d,
                  "taskNo": "%s",
                  "channel": "PHONE",
                  "contentSummary": "续费提醒与课程安排沟通",
                  "result": "CONTACTED",
                  "operatorUserId": 4301
                }
                """.formatted(memberId, taskNo);
        mockMvc.perform(post("/api/ops/touch-records")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskNo").value(taskNo))
                .andExpect(jsonPath("$.channel").value("PHONE"));

        mockMvc.perform(get("/api/ops/touch-records")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .param("memberId", String.valueOf(memberId))
                        .param("taskNo", taskNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].memberId").value(memberId))
                .andExpect(jsonPath("$[0].taskNo").value(taskNo));

        mockMvc.perform(get("/api/ops/touch-records/export")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .param("memberId", String.valueOf(memberId)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")));
    }

    private long createMember(String name, String mobile) throws Exception {
        String req = """
                {
                  "name": "%s",
                  "mobile": "%s",
                  "levelTag": "STANDARD"
                }
                """.formatted(name, mobile);
        String response = mockMvc.perform(post("/api/members")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("id").asLong();
    }

    private String createTaskAndGetTaskNo() throws Exception {
        String saveRuleReq = """
                {
                  "triggerType": "DAILY_MEMBER_FOLLOWUP",
                  "priority": "HIGH",
                  "ownerRole": "STORE_MANAGER",
                  "titleTemplate": "回访{memberName}",
                  "generateLimit": 1,
                  "operatorUserId": 4301
                }
                """;
        mockMvc.perform(put("/api/ops/tasks/rules")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveRuleReq))
                .andExpect(status().isOk());

        String generateReq = """
                {
                  "operatorUserId": 4301
                }
                """;
        String response = mockMvc.perform(post("/api/ops/tasks/generate")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generateReq))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("tasks").get(0).path("taskNo").asText();
    }
}
