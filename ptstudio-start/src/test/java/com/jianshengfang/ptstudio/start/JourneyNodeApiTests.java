package com.jianshengfang.ptstudio.start;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("regression")
class JourneyNodeApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldScanJourneyNodesAndGenerateTasks() throws Exception {
        createMember("旅程会员", "13600008201");

        String scanReq = """
                {
                  "autoGenerateTask": true,
                  "operatorUserId": 4201
                }
                """;

        mockMvc.perform(post("/api/ops/journey-nodes/scan")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scanReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalNodes", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.generatedTaskCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.nodes[0].linkedTaskNo", notNullValue()));

        mockMvc.perform(get("/api/ops/tasks")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .param("status", "TODO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].source").value("JOURNEY_NODE"));
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
}
