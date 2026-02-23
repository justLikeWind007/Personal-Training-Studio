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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("security")
class DataScopeApiTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldApplyStoreAssignedScopeAndAuditDeniedAccess() throws Exception {
        String token = loginAsManager();

        createStore(token, "store-002", "宝安店");
        createStore(token, "store-003", "福田店");

        String assignReq = """
                {
                  "scopeType": "STORE_ASSIGNED",
                  "storeIds": ["store-001","store-002"]
                }
                """;
        mockMvc.perform(post("/api/rbac/users/1002/data-scope")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scopeType").value("STORE_ASSIGNED"));

        mockMvc.perform(get("/api/admin/stores")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.storeId=='store-002')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.storeId=='store-003')]").isEmpty());

        mockMvc.perform(get("/api/settings/store")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-003"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("无权访问该门店数据"));

        mockMvc.perform(get("/api/audit/logs")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.module=='SECURITY' && @.action=='DATA_SCOPE_DENY')]").isNotEmpty());
    }

    private String loginAsManager() throws Exception {
        String loginBody = """
                {
                  "mobile": "13800000002",
                  "password": "123456"
                }
                """;
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(loginResponse).path("token").asText();
    }

    private void createStore(String token, String storeId, String storeName) throws Exception {
        String req = """
                {
                  "storeId": "%s",
                  "storeName": "%s",
                  "businessHoursJson": "{\\\"weekdays\\\":\\\"09:00-21:00\\\"}"
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
}
