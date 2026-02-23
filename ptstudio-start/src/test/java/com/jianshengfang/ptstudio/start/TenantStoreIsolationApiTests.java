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
class TenantStoreIsolationApiTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRejectWhenTenantStoreHeadersMissing() throws Exception {
        mockMvc.perform(get("/api/members"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("缺少租户门店上下文请求头"));
    }

    @Test
    void shouldIsolateDataAcrossTenantAndStore() throws Exception {
        String createReq = """
                {
                  "name": "Member Isolation",
                  "mobile": "13600000123",
                  "levelTag": "STANDARD"
                }
                """;

        String createResp = mockMvc.perform(post("/api/members")
                        .header("X-Tenant-Id", "tenant-a")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createReq))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long memberId = objectMapper.readTree(createResp).path("id").asLong();

        mockMvc.perform(get("/api/members/{id}", memberId)
                        .header("X-Tenant-Id", "tenant-b")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/members/{id}", memberId)
                        .header("X-Tenant-Id", "tenant-a")
                        .header("X-Store-Id", "store-999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
