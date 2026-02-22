package com.jianshengfang.ptstudio.start;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class FinanceApiTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCompleteOrderPayRefundReconcileFlow() throws Exception {
        long memberId = createMember();

        long orderId = createOrder(memberId, new BigDecimal("299.00"));

        mockMvc.perform(post("/api/payments/alipay/precreate")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payStatus").value("WAIT_PAY"));

        String callbackReq = """
                {
                  "orderId": %d,
                  "channelTradeNo": "ALI-TRADE-0001",
                  "callbackRaw": "{\"trade_status\":\"TRADE_SUCCESS\"}"
                }
                """.formatted(orderId);
        mockMvc.perform(post("/api/payments/alipay/callback")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payStatus").value("PAID"));

        String refundReq = """
                {
                  "orderId": %d,
                  "refundAmount": 99.00,
                  "reason": "member_cancel"
                }
                """.formatted(orderId);
        String refundResp = mockMvc.perform(post("/api/refunds")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refundReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long refundId = objectMapper.readTree(refundResp).path("id").asLong();

        mockMvc.perform(post("/api/refunds/{id}/approve", refundId)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operatorUserId\":1002}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        mockMvc.perform(get("/api/reconciliations/daily")
                        .param("bizDate", LocalDate.now().toString())
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BALANCED"));
    }

    private long createMember() throws Exception {
        String req = """
                {
                  "name": "Member D",
                  "mobile": "13600000004",
                  "levelTag": "VIP"
                }
                """;
        String response = mockMvc.perform(post("/api/members")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("id").asLong();
    }

    private long createOrder(long memberId, BigDecimal amount) throws Exception {
        String req = """
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
                        .content(req))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("id").asLong();
    }
}
