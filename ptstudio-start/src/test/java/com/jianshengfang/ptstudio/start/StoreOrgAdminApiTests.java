package com.jianshengfang.ptstudio.start;

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
class StoreOrgAdminApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateListAndDeactivateStore() throws Exception {
        String createStoreReq = """
                {
                  "storeId": "store-002",
                  "storeName": "南山二店",
                  "businessHoursJson": "{\"weekdays\":\"09:00-21:00\"}"
                }
                """;
        mockMvc.perform(post("/api/admin/stores")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createStoreReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value("store-002"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/admin/stores")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.storeId=='store-002')]").isNotEmpty());

        mockMvc.perform(post("/api/admin/stores/store-002/status")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        String createMemberReq = """
                {
                  "name": "inactive store member",
                  "mobile": "13600000999",
                  "levelTag": "STANDARD"
                }
                """;
        mockMvc.perform(post("/api/members")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createMemberReq))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("门店已停用，禁止写操作"));
    }
}
