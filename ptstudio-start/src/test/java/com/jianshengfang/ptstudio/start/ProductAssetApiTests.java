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
class ProductAssetApiTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateUpdateAndQueryPackageAndMemberAssets() throws Exception {
        long memberId = createMember();

        String createReq = """
                {
                  "packageName": "私教10次卡",
                  "totalSessions": 10,
                  "validDays": 90,
                  "price": 1999.00,
                  "saleStatus": "ON"
                }
                """;
        String packageResp = mockMvc.perform(post("/api/packages")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packageName").value("私教10次卡"))
                .andReturn().getResponse().getContentAsString();

        long packageId = objectMapper.readTree(packageResp).path("id").asLong();

        String updateReq = """
                {
                  "packageName": "私教12次卡",
                  "totalSessions": 12,
                  "validDays": 120,
                  "price": 2299.00,
                  "saleStatus": "ON"
                }
                """;
        mockMvc.perform(put("/api/packages/{id}", packageId)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packageName").value("私教12次卡"));

        mockMvc.perform(get("/api/packages")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(packageId));

        mockMvc.perform(get("/api/members/{id}/packages", memberId)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].memberId").value(memberId));

        mockMvc.perform(get("/api/members/{id}/package-ledgers", memberId)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actionType").isNotEmpty());
    }

    private long createMember() throws Exception {
        String req = """
                {
                  "name": "Member P",
                  "mobile": "13600000088",
                  "levelTag": "STANDARD"
                }
                """;
        String response = mockMvc.perform(post("/api/members")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("id").asLong();
    }
}
