package com.jianshengfang.ptstudio.start;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("regression")
class StrategyConfigApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldPublishRollbackAndQueryHistory() throws Exception {
        String draftV1 = """
                {
                  "refundRiskRatioThreshold": 55,
                  "lowAttendanceRateThreshold": 58,
                  "reversedConsumptionDailyThreshold": 4,
                  "metricCaliber": "CALIBER_V1",
                  "remark": "首版策略",
                  "operatorUserId": 3001
                }
                """;

        mockMvc.perform(put("/api/ops/strategies/draft")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftV1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricCaliber").value("CALIBER_V1"));

        String operatorReq = """
                {
                  "operatorUserId": 3001
                }
                """;
        mockMvc.perform(post("/api/ops/strategies/publish")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(operatorReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNo").value("v1"))
                .andExpect(jsonPath("$.action").value("PUBLISH"));

        String draftV2 = """
                {
                  "refundRiskRatioThreshold": 62,
                  "lowAttendanceRateThreshold": 52,
                  "reversedConsumptionDailyThreshold": 6,
                  "metricCaliber": "CALIBER_V2",
                  "remark": "二版策略",
                  "operatorUserId": 3001
                }
                """;
        mockMvc.perform(put("/api/ops/strategies/draft")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftV2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricCaliber").value("CALIBER_V2"));

        mockMvc.perform(post("/api/ops/strategies/publish")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(operatorReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNo").value("v2"));

        mockMvc.perform(post("/api/ops/strategies/rollback/v1")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(operatorReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNo").value("v3"))
                .andExpect(jsonPath("$.action").value("ROLLBACK"))
                .andExpect(jsonPath("$.sourceVersion").value("v1"));

        mockMvc.perform(get("/api/ops/strategies/current")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNo").value("v3"))
                .andExpect(jsonPath("$.metricCaliber").value("CALIBER_V1"));

        mockMvc.perform(get("/api/ops/strategies/history")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].versionNo").value("v3"));
    }
}
