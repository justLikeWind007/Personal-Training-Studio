package com.jianshengfang.ptstudio.start;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("regression")
class RiskAlertApiTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldListAckCloseAndExportRiskAlerts() throws Exception {
        long memberId = createMember();
        long orderId = createOrder(memberId, new BigDecimal("800.00"));
        markOrderPaid(orderId);
        createRefund(orderId, new BigDecimal("500.00"));

        String alertsResponse = mockMvc.perform(get("/api/ops/risk-alerts")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String alertNo = findAlertNo(alertsResponse, "REFUND_PENDING");

        String actionReq = """
                {
                  "operatorUserId": 2001
                }
                """;

        mockMvc.perform(post("/api/ops/risk-alerts/{alertNo}/ack", alertNo)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actionReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACK"))
                .andExpect(jsonPath("$.handledBy").value(2001));

        mockMvc.perform(post("/api/ops/risk-alerts/{alertNo}/close", alertNo)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actionReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(get("/api/ops/risk-alerts/export")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")));
    }

    private String findAlertNo(String alertsResponse, String type) throws Exception {
        JsonNode root = objectMapper.readTree(alertsResponse);
        for (JsonNode alert : root) {
            if (type.equals(alert.path("type").asText())) {
                return alert.path("alertNo").asText();
            }
        }
        throw new IllegalStateException("未找到类型为 " + type + " 的风险预警");
    }

    private long createMember() throws Exception {
        String request = """
                {
                  "name": "Risk Member",
                  "mobile": "13600009901",
                  "levelTag": "STANDARD"
                }
                """;
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
                  "channelTradeNo": "RISK_TRADE_%d",
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

    private void createRefund(long orderId, BigDecimal amount) throws Exception {
        String request = """
                {
                  "orderId": %d,
                  "refundAmount": %s,
                  "reason": "风险排查"
                }
                """.formatted(orderId, amount.toPlainString());
        mockMvc.perform(post("/api/refunds")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
