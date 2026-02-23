package com.jianshengfang.ptstudio.start;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("regression")
class OpsDailySummaryApiTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldGetDailySummaryAndText() throws Exception {
        long memberId = createMember("日报会员", "13600002001", "STANDARD");
        createOrder(memberId, new BigDecimal("299.00"));
        String bizDate = LocalDate.now().toString();

        mockMvc.perform(get("/api/ops/daily-summary")
                        .param("bizDate", bizDate)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bizDate").value(bizDate))
                .andExpect(jsonPath("$.newMembers").isNumber())
                .andExpect(jsonPath("$.orderCount").isNumber())
                .andExpect(jsonPath("$.actualAmount").isNumber());

        mockMvc.perform(get("/api/ops/daily-summary/text")
                        .param("bizDate", bizDate)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bizDate").value(bizDate))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("运营日报")));
    }

    private long createMember(String name, String mobile, String levelTag) throws Exception {
        String request = """
                {
                  "name": "%s",
                  "mobile": "%s",
                  "levelTag": "%s"
                }
                """.formatted(name, mobile, levelTag);
        String response = mockMvc.perform(post("/api/members")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("id").asLong();
    }

    private void createOrder(long memberId, BigDecimal amount) throws Exception {
        String request = """
                {
                  "memberId": %d,
                  "orderType": "PACKAGE",
                  "totalAmount": %s
                }
                """.formatted(memberId, amount.toPlainString());
        mockMvc.perform(post("/api/orders")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk());
    }
}
