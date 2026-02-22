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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AttendanceApiTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCheckinConsumeIdempotentAndReverse() throws Exception {
        long memberId = createMember();
        long coachId = createCoach();
        long slotId = createSlot(coachId);
        long reservationId = createReservation(memberId, slotId);

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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"));

        String consumeReq = """
                {
                  "reservationId": %d,
                  "sessionsDelta": 1,
                  "idemKey": "idem-consume-001",
                  "operatorUserId": 1002
                }
                """.formatted(reservationId);

        String c1 = mockMvc.perform(post("/api/consumptions")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(consumeReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONSUMED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String c2 = mockMvc.perform(post("/api/consumptions")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(consumeReq))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long consumeId1 = objectMapper.readTree(c1).path("id").asLong();
        long consumeId2 = objectMapper.readTree(c2).path("id").asLong();
        if (consumeId1 != consumeId2) {
            throw new IllegalStateException("idempotent check failed");
        }

        String reverseReq = """
                {
                  "operatorUserId": 1002
                }
                """;
        mockMvc.perform(post("/api/consumptions/{id}/reverse", consumeId1)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reverseReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVERSED"));

        mockMvc.perform(get("/api/checkins")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/consumptions")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldSubmitApproveAndRejectMakeupCheckinFlow() throws Exception {
        long memberId = createMember();
        long coachId = createCoach();
        long slotId = createSlot(coachId);
        long reservationId = createReservation(memberId, slotId);

        String submitReq = """
                {
                  "reservationId": %d,
                  "reason": "member_late_sync",
                  "submittedBy": 1002
                }
                """.formatted(reservationId);
        String submitResp = mockMvc.perform(post("/api/checkins/makeups")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long approvalId = objectMapper.readTree(submitResp).path("id").asLong();

        mockMvc.perform(get("/api/checkins/makeups")
                        .param("status", "PENDING")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        String approveReq = """
                {
                  "approvedBy": 9001
                }
                """;
        mockMvc.perform(post("/api/checkins/makeups/{id}/approve", approvalId)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approveReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/checkins")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].checkinChannel").value("MAKEUP_APPROVED"));

        long reservationId2 = createReservation(createMember(), createSlot(createCoach()));
        String submitReq2 = """
                {
                  "reservationId": %d,
                  "reason": "manual_fix",
                  "submittedBy": 1003
                }
                """.formatted(reservationId2);
        String submitResp2 = mockMvc.perform(post("/api/checkins/makeups")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitReq2))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long approvalId2 = objectMapper.readTree(submitResp2).path("id").asLong();

        String rejectReq = """
                {
                  "rejectReason": "evidence_missing",
                  "approvedBy": 9002
                }
                """;
        mockMvc.perform(post("/api/checkins/makeups/{id}/reject", approvalId2)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rejectReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectReason").value("evidence_missing"));
    }

    private long createMember() throws Exception {
        String req = """
                {
                  "name": "Member C",
                  "mobile": "13600000003",
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

    private long createCoach() throws Exception {
        String req = """
                {
                  "name": "Coach Sun",
                  "mobile": "13700000012",
                  "level": "L2",
                  "specialties": "pilates"
                }
                """;
        String response = mockMvc.perform(post("/api/coaches")
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

    private long createSlot(long coachId) throws Exception {
        String req = """
                {
                  "slotDate": "2026-02-25",
                  "startTime": "10:00:00",
                  "endTime": "11:00:00",
                  "capacity": 1
                }
                """;
        String response = mockMvc.perform(post("/api/coaches/{id}/slots", coachId)
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

    private long createReservation(long memberId, long slotId) throws Exception {
        String req = """
                {
                  "memberId": %d,
                  "slotId": %d
                }
                """.formatted(memberId, slotId);
        String response = mockMvc.perform(post("/api/reservations")
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
