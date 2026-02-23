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
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("regression")
class OpsReviewDashboardApiTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldQueryAndExportReviewDashboard() throws Exception {
        long memberId = createMember("复盘会员", "13600008501");
        String taskNo = createTaskAndComplete();
        createTouchRecord(memberId, taskNo);

        mockMvc.perform(get("/api/ops/review-dashboard")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks", greaterThan(0)))
                .andExpect(jsonPath("$.doneTasks", greaterThan(0)))
                .andExpect(jsonPath("$.completionRate", greaterThan(0.0)))
                .andExpect(jsonPath("$.conversionRate", greaterThan(0.0)));

        mockMvc.perform(get("/api/ops/review-dashboard/export")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
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
        String resp = mockMvc.perform(post("/api/members")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(resp).path("id").asLong();
    }

    private String createTaskAndComplete() throws Exception {
        String saveRuleReq = """
                {
                  "triggerType": "DAILY_MEMBER_FOLLOWUP",
                  "priority": "HIGH",
                  "ownerRole": "STORE_MANAGER",
                  "titleTemplate": "回访{memberName}",
                  "generateLimit": 1,
                  "operatorUserId": 4501
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
                  "operatorUserId": 4501
                }
                """;
        String genResp = mockMvc.perform(post("/api/ops/tasks/generate")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generateReq))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String taskNo = objectMapper.readTree(genResp).path("tasks").get(0).path("taskNo").asText();

        mockMvc.perform(post("/api/ops/tasks/{taskNo}/start", taskNo)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generateReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOING"));

        mockMvc.perform(post("/api/ops/tasks/{taskNo}/complete", taskNo)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generateReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        return taskNo;
    }

    private void createTouchRecord(long memberId, String taskNo) throws Exception {
        String req = """
                {
                  "memberId": %d,
                  "taskNo": "%s",
                  "channel": "PHONE",
                  "contentSummary": "复盘触达",
                  "result": "CONTACTED",
                  "operatorUserId": 4501
                }
                """.formatted(memberId, taskNo);
        mockMvc.perform(post("/api/ops/touch-records")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk());
    }
}
