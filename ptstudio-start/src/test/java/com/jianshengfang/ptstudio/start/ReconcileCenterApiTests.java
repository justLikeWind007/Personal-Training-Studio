package com.jianshengfang.ptstudio.start;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReconcileCenterApiTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldListRetryCloseAndExportIssues() throws Exception {
        long memberId = createMember();
        long orderId = createOrder(memberId, new BigDecimal("199.00"));

        mockMvc.perform(post("/api/payments/alipay/precreate")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payStatus").value("WAIT_PAY"));

        mockMvc.perform(get("/api/reconcile-center/overview")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCount").isNumber())
                .andExpect(jsonPath("$.anomalyCount").isNumber());

        String issues = mockMvc.perform(get("/api/reconcile-center/issues")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].type").value("PAYMENT_WAIT_PAY"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String issueNo = objectMapper.readTree(issues).get(0).path("issueNo").asText();

        String actionReq = """
                {
                  "operatorUserId": 1002
                }
                """;
        mockMvc.perform(post("/api/reconcile-center/issues/{issueNo}/retry", issueNo)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actionReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETRYING"));

        mockMvc.perform(post("/api/reconcile-center/issues/{issueNo}/close", issueNo)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actionReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(get("/api/reconcile-center/issues/export")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")));
    }

    private long createMember() throws Exception {
        String req = """
                {
                  "name": "Member Reconcile",
                  "mobile": "13600000111",
                  "levelTag": "STANDARD"
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
