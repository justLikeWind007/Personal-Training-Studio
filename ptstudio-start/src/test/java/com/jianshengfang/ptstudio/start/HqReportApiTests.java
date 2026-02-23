package com.jianshengfang.ptstudio.start;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class HqReportApiTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldProvideHqOverviewRankingTrendAndMetricDefinitions() throws Exception {
        String token = loginAsManager();
        createStore(token, "store-002", "龙华店");
        createMember("store-001", "13600001001");
        createMember("store-002", "13600001002");

        mockMvc.perform(get("/api/hq/reports/overview")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeCount").isNumber())
                .andExpect(jsonPath("$.totalMembers").isNumber());

        mockMvc.perform(get("/api/hq/reports/ranking")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/hq/reports/trend")
                        .param("days", "3")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(get("/api/hq/reports/stores/store-001")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value("store-001"))
                .andExpect(jsonPath("$.finance").exists());

        mockMvc.perform(get("/api/hq/metrics/definitions")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        String updateReq = """
                {
                  "displayName": "净现金",
                  "formula": "sum(paid)-sum(refund)",
                  "description": "总部净现金指标"
                }
                """;
        mockMvc.perform(put("/api/hq/metrics/definitions/net_cash")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricKey").value("net_cash"))
                .andExpect(jsonPath("$.version").isNumber());
    }

    private String loginAsManager() throws Exception {
        String loginBody = """
                {
                  "mobile": "13800000002",
                  "password": "123456"
                }
                """;
        String body = mockMvc.perform(post("/api/auth/login")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("token").asText();
    }

    private void createStore(String token, String storeId, String storeName) throws Exception {
        String req = """
                {
                  "storeId": "%s",
                  "storeName": "%s",
                  "businessHoursJson": "{\"weekdays\":\"09:00-21:00\"}"
                }
                """.formatted(storeId, storeName);
        mockMvc.perform(post("/api/admin/stores")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk());
    }

    private void createMember(String storeId, String mobile) throws Exception {
        String req = """
                {
                  "name": "HQ Member",
                  "mobile": "%s",
                  "levelTag": "STANDARD"
                }
                """.formatted(mobile);
        mockMvc.perform(post("/api/members")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", storeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk());
    }
}
