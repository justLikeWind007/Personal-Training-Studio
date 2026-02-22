package com.jianshengfang.ptstudio.start;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReportApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGenerateOverviewAttendanceAndFinanceReports() throws Exception {
        long memberId = createMember();
        long coachId = createCoach();
        long slotId = createSlot(coachId);
        long reservationId = createReservation(memberId, slotId);
        checkinAndConsume(reservationId);
        long orderId = createOrder(memberId);
        payAndRefund(orderId);
        createCommissionStatement();

        mockMvc.perform(get("/api/reports/overview")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReservations").isNumber())
                .andExpect(jsonPath("$.totalRevenue").isNumber());

        mockMvc.perform(get("/api/reports/attendance")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCheckins").isNumber())
                .andExpect(jsonPath("$.attendanceRatePercent").isNumber());

        mockMvc.perform(get("/api/reports/finance")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paidAmount").isNumber())
                .andExpect(jsonPath("$.commissionAmount").isNumber());
    }

    private long createMember() throws Exception {
        String req = """
                {
                  "name": "Member R",
                  "mobile": "13600000031",
                  "levelTag": "STANDARD"
                }
                """;
        String body = mockMvc.perform(post("/api/members")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(body).path("id").asLong();
    }

    private long createCoach() throws Exception {
        String req = """
                {
                  "name": "Coach R",
                  "mobile": "13700000031",
                  "level": "L1",
                  "specialties": "boxing"
                }
                """;
        String body = mockMvc.perform(post("/api/coaches")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(body).path("id").asLong();
    }

    private long createSlot(long coachId) throws Exception {
        String req = """
                {
                  "slotDate": "2026-02-26",
                  "startTime": "15:00:00",
                  "endTime": "16:00:00",
                  "capacity": 1
                }
                """;
        String body = mockMvc.perform(post("/api/coaches/{id}/slots", coachId)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(body).path("id").asLong();
    }

    private long createReservation(long memberId, long slotId) throws Exception {
        String req = """
                {
                  "memberId": %d,
                  "slotId": %d
                }
                """.formatted(memberId, slotId);
        String body = mockMvc.perform(post("/api/reservations")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(body).path("id").asLong();
    }

    private void checkinAndConsume(long reservationId) throws Exception {
        String checkinReq = """
                {
                  "reservationId": %d,
                  "checkinChannel": "FRONT_DESK",
                  "operatorUserId": 1002
                }
                """.formatted(reservationId);
        mockMvc.perform(post("/api/checkins")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkinReq))
                .andExpect(status().isOk());

        String consumeReq = """
                {
                  "reservationId": %d,
                  "sessionsDelta": 1,
                  "idemKey": "report-consume-001",
                  "operatorUserId": 1002
                }
                """.formatted(reservationId);
        mockMvc.perform(post("/api/consumptions")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(consumeReq))
                .andExpect(status().isOk());
    }

    private long createOrder(long memberId) throws Exception {
        String req = """
                {
                  "memberId": %d,
                  "orderType": "PACKAGE",
                  "totalAmount": 199.00
                }
                """.formatted(memberId);
        String body = mockMvc.perform(post("/api/orders")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(body).path("id").asLong();
    }

    private void payAndRefund(long orderId) throws Exception {
        mockMvc.perform(post("/api/payments/alipay/precreate")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + "}"))
                .andExpect(status().isOk());

        String callbackReq = """
                {
                  "orderId": %d,
                  "channelTradeNo": "ALI-REPORT-001",
                  "callbackRaw": "ok"
                }
                """.formatted(orderId);
        mockMvc.perform(post("/api/payments/alipay/callback")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackReq))
                .andExpect(status().isOk());

        String refundReq = """
                {
                  "orderId": %d,
                  "refundAmount": 19.00,
                  "reason": "report_case"
                }
                """.formatted(orderId);
        String refundBody = mockMvc.perform(post("/api/refunds")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refundReq))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long refundId = com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(refundBody).path("id").asLong();

        mockMvc.perform(post("/api/refunds/{id}/approve", refundId)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operatorUserId\":1002}"))
                .andExpect(status().isOk());
    }

    private void createCommissionStatement() throws Exception {
        String ruleReq = """
                {
                  "name": "Report Rule",
                  "calcMode": "PERCENT",
                  "ratio": 0.2,
                  "effectiveFrom": "%s"
                }
                """.formatted(OffsetDateTime.now().toString());
        String ruleBody = mockMvc.perform(post("/api/commission/rules")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ruleReq))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long ruleId = com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(ruleBody).path("id").asLong();

        String genReq = """
                {
                  "statementMonth": "2026-02",
                  "ruleId": %d
                }
                """.formatted(ruleId);
        mockMvc.perform(post("/api/commission/statements/generate")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(genReq))
                .andExpect(status().isOk());
    }
}
