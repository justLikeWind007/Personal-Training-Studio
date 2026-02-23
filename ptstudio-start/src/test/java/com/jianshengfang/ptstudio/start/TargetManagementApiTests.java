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
import java.time.YearMonth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("regression")
class TargetManagementApiTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldUpsertTargetAndQueryAchievement() throws Exception {
        long memberId = createMember("目标会员", "13600003001");
        long orderId = createOrder(memberId, new BigDecimal("500.00"));
        markOrderPaid(orderId);

        String month = YearMonth.now().toString();
        String upsertReq = """
                {
                  "month": "%s",
                  "revenueTarget": 1000.00,
                  "consumptionTarget": 20,
                  "newMemberTarget": 10
                }
                """.formatted(month);
        mockMvc.perform(put("/api/ops/targets")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(upsertReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(month))
                .andExpect(jsonPath("$.revenueTarget").value(1000.00));

        mockMvc.perform(get("/api/ops/targets/achievement")
                        .param("month", month)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(month))
                .andExpect(jsonPath("$.actualRevenue").isNumber())
                .andExpect(jsonPath("$.revenueRate").isNumber())
                .andExpect(jsonPath("$.target.newMemberTarget").value(10));
    }

    private long createMember(String name, String mobile) throws Exception {
        String request = """
                {
                  "name": "%s",
                  "mobile": "%s",
                  "levelTag": "STANDARD"
                }
                """.formatted(name, mobile);
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

    private long createOrder(long memberId, BigDecimal amount) throws Exception {
        String request = """
                {
                  "memberId": %d,
                  "orderType": "PACKAGE",
                  "totalAmount": %s
                }
                """.formatted(memberId, amount.toPlainString());
        String response = mockMvc.perform(post("/api/orders")
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

    private void markOrderPaid(long orderId) throws Exception {
        String precreateRequest = """
                {
                  "orderId": %d
                }
                """.formatted(orderId);
        mockMvc.perform(post("/api/payments/alipay/precreate")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(precreateRequest))
                .andExpect(status().isOk());

        String callbackRequest = """
                {
                  "orderId": %d,
                  "channelTradeNo": "TRADE_%d",
                  "callbackRaw": "SUCCESS"
                }
                """.formatted(orderId, orderId);
        mockMvc.perform(post("/api/payments/alipay/callback")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payStatus").value("PAID"));
    }
}
