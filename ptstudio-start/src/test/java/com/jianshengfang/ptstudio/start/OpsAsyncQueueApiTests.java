package com.jianshengfang.ptstudio.start;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("regression")
class OpsAsyncQueueApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldEnqueueConsumeRetryDeadLetterAndCheckHealth() throws Exception {
        createMember("异步会员", "13600008401");
        createAndGenerateTask();

        mockMvc.perform(get("/api/ops/async-queue/health")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueSize", greaterThanOrEqualTo(1)));

        String enqueueFailReq = """
                {
                  "taskNo": "OT_FAIL_1",
                  "payload": "FAIL_DEMO_PAYLOAD",
                  "maxRetry": 2,
                  "operatorUserId": 4401
                }
                """;
        mockMvc.perform(post("/api/ops/async-queue/enqueue")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(enqueueFailReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskNo").value("OT_FAIL_1"));

        String consumeReq = """
                {
                  "batchSize": 10,
                  "operatorUserId": 4401
                }
                """;
        mockMvc.perform(post("/api/ops/async-queue/consume")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(consumeReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retryCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.deadCount", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/ops/async-queue/dead-letters")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reason").value("MAX_RETRY_REACHED"));

        mockMvc.perform(get("/api/ops/async-queue/health")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"));
    }

    private void createMember(String name, String mobile) throws Exception {
        String req = """
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
                        .content(req))
                .andExpect(status().isOk());
    }

    private void createAndGenerateTask() throws Exception {
        String saveRuleReq = """
                {
                  "triggerType": "DAILY_MEMBER_FOLLOWUP",
                  "priority": "HIGH",
                  "ownerRole": "STORE_MANAGER",
                  "titleTemplate": "回访{memberName}",
                  "generateLimit": 1,
                  "operatorUserId": 4401
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
                  "operatorUserId": 4401
                }
                """;
        mockMvc.perform(post("/api/ops/tasks/generate")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generateReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedCount").value(1));
    }
}
