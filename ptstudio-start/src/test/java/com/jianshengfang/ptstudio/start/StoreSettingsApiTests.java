package com.jianshengfang.ptstudio.start;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StoreSettingsApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGetAndUpdateStoreSettings() throws Exception {
        mockMvc.perform(get("/api/settings/store")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").isNotEmpty());

        String updateReq = """
                {
                  "storeName": "深圳南山店",
                  "businessHoursJson": "{\"weekdays\":\"08:00-22:00\",\"weekend\":\"09:00-21:00\"}"
                }
                """;

        mockMvc.perform(put("/api/settings/store")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("深圳南山店"));

        mockMvc.perform(get("/api/settings/store")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("深圳南山店"));
    }
}
