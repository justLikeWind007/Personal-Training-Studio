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
class ReservationApiTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReserveAndCancelSlotWithCapacityChecks() throws Exception {
        long memberId = createMember("Member A", "13600000001");
        long coachId = createCoach();
        long slotId = createSlot(coachId, 1);

        String createReservationReq = """
                {
                  "memberId": %d,
                  "slotId": %d
                }
                """.formatted(memberId, slotId);

        String reservationResp = mockMvc.perform(post("/api/reservations")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createReservationReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long reservationId = objectMapper.readTree(reservationResp).path("id").asLong();

        mockMvc.perform(get("/api/slots/available")
                        .param("coachId", String.valueOf(coachId))
                        .param("slotDate", "2026-02-24")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        long memberId2 = createMember("Member B", "13600000002");
        String duplicateReservationReq = """
                {
                  "memberId": %d,
                  "slotId": %d
                }
                """.formatted(memberId2, slotId);

        mockMvc.perform(post("/api/reservations")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateReservationReq))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        String cancelReq = """
                {
                  "reason": "member_plan_change"
                }
                """;

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservationId)
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        mockMvc.perform(get("/api/slots/available")
                        .param("coachId", String.valueOf(coachId))
                        .param("slotDate", "2026-02-24")
                        .header("X-Tenant-Id", "tenant-demo")
                        .header("X-Store-Id", "store-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    private long createMember(String name, String mobile) throws Exception {
        String req = """
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
                  "name": "Coach Wu",
                  "mobile": "13700000011",
                  "level": "L1",
                  "specialties": "yoga"
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

    private long createSlot(long coachId, int capacity) throws Exception {
        String req = """
                {
                  "slotDate": "2026-02-24",
                  "startTime": "09:00:00",
                  "endTime": "10:00:00",
                  "capacity": %d
                }
                """.formatted(capacity);
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
}
